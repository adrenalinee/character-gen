package ltmp.charactergen.api.chat

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ltmp.charactergen.api.ApiErrorResponse
import ltmp.charactergen.service.chat.CharacterChatService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/character-chat")
@Tag(
    name = "Character Chat",
    description = "생성된 L0 캐릭터와 직접 대화해 런타임 인스트럭션을 검증하는 API. " +
        "세션은 서버 메모리에만 존재하며 일정 시간 대화가 없으면 삭제된다.",
)
class CharacterChatController(
    private val characterChatService: CharacterChatService,
) {
    @Operation(
        summary = "캐릭터 대화 세션 생성",
        description = "캐릭터와 관계 단계로 런타임 인스트럭션을 조립해 메모리에 세션을 만든다. " +
            "refId와 instructionBranch를 모두 생략하면 CHARACTER_RUNTIME 기본 프롬프트를 사용한다. " +
            "응답에는 실제로 주입되는 인스트럭션 전문이 포함되므로 무엇이 들어갔는지 바로 확인할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "세션 생성 성공",
                content = [Content(schema = Schema(implementation = CharacterChatSessionDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 값이 올바르지 않거나, 용도가 CHARACTER_RUNTIME이 아닌 ref이거나, refId와 instructionBranch를 동시에 지정함",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "캐릭터, 프롬프트 ref, 인스트럭션 템플릿 브랜치, 또는 기본 프롬프트 지정 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "429",
                description = "유지 가능한 세션 수 초과",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSession(
        @Valid @RequestBody request: CreateCharacterChatSessionRequest,
    ): CharacterChatSessionDto = characterChatService.createSession(request)

    @Operation(
        summary = "런타임 인스트럭션 조립 결과만 조회",
        description = "세션을 만들지 않고, 세션 생성과 똑같은 요청으로 조립되는 런타임 인스트럭션 전문만 돌려준다. " +
            "세션을 남기지 않으므로 프롬프트나 브랜치를 바꿔가며 조립 결과만 비교할 때 쓴다. " +
            "요청 본문은 세션 생성과 같으며 model은 조립에 관여하지 않으므로 무시한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조립 성공",
                content = [Content(schema = Schema(implementation = CharacterRuntimeInstructionPreviewDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 값이 올바르지 않거나, 용도가 CHARACTER_RUNTIME이 아닌 ref이거나, refId와 instructionBranch를 동시에 지정함",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "캐릭터, 프롬프트 ref, 인스트럭션 템플릿 브랜치, 또는 기본 프롬프트 지정 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/runtime-instructions")
    fun previewRuntimeInstruction(
        @Valid @RequestBody request: CreateCharacterChatSessionRequest,
    ): CharacterRuntimeInstructionPreviewDto = characterChatService.previewRuntimeInstruction(request)

    @Operation(
        summary = "캐릭터와 대화 (SSE 스트리밍)",
        description = """
            사용자 메시지를 보내고 캐릭터 응답을 SSE로 받는다. 세션에 쌓인 대화 기록이 매 턴 함께 전달된다.

            이벤트는 세 가지다.
            - `delta` : 응답 텍스트 조각. `{"text": "..."}` 형태로 여러 번 도착한다. LLM의 나머지 이벤트는 전달하지 않는다.
            - `done`  : 응답 완료. 이어붙인 전체 응답과 대화 기록에서의 위치를 담는다.
            - `error` : 스트림 도중 실패. 본문은 일반 오류 응답과 같은 형태다. 이 경우 이번 턴은 대화 기록에 남지 않는다.

            브라우저 EventSource는 GET만 지원하므로 fetch의 스트림 읽기로 소비한다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스트리밍 시작. delta 이벤트가 이어지고 done 또는 error로 끝난다.",
                content = [
                    Content(
                        mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                        schema = Schema(
                            description = "이벤트 이름에 따라 payload가 달라진다. delta/done/error 순서로 대응한다.",
                            oneOf = [
                                CharacterChatDeltaEventDto::class,
                                CharacterChatDoneEventDto::class,
                                ApiErrorResponse::class,
                            ],
                        ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 값이 올바르지 않음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ApiErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "세션이 없거나 만료됨",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ApiErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "같은 세션에서 이미 응답을 생성 중",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ApiErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "429",
                description = "동시 대화 수 초과",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ApiErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    // SSE는 UTF-8로 읽히는 게 규격이지만, 한글 응답이 잘못 디코딩되지 않게 charset을 명시한다.
    @PostMapping("/sessions/{sessionId}/messages", produces = [SSE_UTF8])
    fun sendMessage(
        @Parameter(description = "대화 세션 식별자", required = true)
        @PathVariable
        sessionId: String,
        @Valid @RequestBody request: CharacterChatMessageRequest,
    ): SseEmitter = characterChatService.sendMessage(sessionId, request)

    @Operation(
        summary = "대화 세션과 대화 기록 조회",
        description = "세션 설정과 지금까지 오간 대화 전체를 반환한다. 프로브 검증에 넣을 로그를 여기서 가져간다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = CharacterChatSessionDto::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "세션이 없거나 만료됨",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/sessions/{sessionId}")
    fun getSession(
        @Parameter(description = "대화 세션 식별자", required = true)
        @PathVariable
        sessionId: String,
    ): CharacterChatSessionDto = characterChatService.getSession(sessionId)

    private companion object {
        const val SSE_UTF8 = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8"
    }
}
