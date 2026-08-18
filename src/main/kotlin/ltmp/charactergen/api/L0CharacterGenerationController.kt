package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ltmp.charactergen.api.axis.L0CharacterGenerationOptionsDto
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.service.L0CharacterGenerationOptionsService
import ltmp.charactergen.service.L0CharacterGenerationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/l0-characters")
@Tag(name = "L0 Character Generation", description = "L0 캐릭터 코어 생성 API")
class L0CharacterGenerationController(
    private val generationOptionsService: L0CharacterGenerationOptionsService,
    private val generationService: L0CharacterGenerationService,
) {
    @Operation(
        summary = "L0 캐릭터 생성 축 조회",
        description = "캐릭터 생성 입력 UI에서 선택할 분야, Lie, 상처 조건 5개 축, Big Five, 관계 태도, 정서 표출, 회피 방식, 말투 원형, 존댓말/반말 기본 정책을 모두 반환한다.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "축 정보 조회 성공",
        content = [Content(schema = Schema(implementation = L0CharacterGenerationOptionsDto::class))],
    )
    @GetMapping("/generation-options")
    fun getGenerationOptions(): L0CharacterGenerationOptionsDto = generationOptionsService.getOptions()

    @Operation(
        summary = "L0 캐릭터 생성 실행 검색",
        description = "프롬프트 ref와 실행 상태를 조건으로 생성 실행 이력을 검색한다. 조건을 생략하면 전체 이력을 반환한다.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "실행 이력 검색 성공",
    )
    @GetMapping("/generations")
    fun searchGenerations(
        @RequestParam(required = false) refId: Long?,
        @RequestParam(required = false) status: L0GenerationExecuteStatus?,
        @ParameterObject @PageableDefault(sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PagedModel<L0GenerationExecuteSimpleDto> =
        PagedModel(generationService.searchGenerations(refId, status, pageable))

    @Operation(summary = "L0 캐릭터 생성 실행 단일 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "실행 상세 조회 성공",
                content = [Content(schema = Schema(implementation = L0GenerationExecuteDto::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "생성 실행 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/generations/{generationId}")
    fun getGeneration(@PathVariable generationId: Long): L0GenerationExecuteDto =
        generationService.getGeneration(generationId)

    @Operation(
        summary = "L0 캐릭터 생성",
        description = "refId를 제외한 생성 입력은 선택 사항이며, 생략한 값은 프롬프트에서 빈 문자열로 처리한다. 실행 레코드를 PREPARING 상태로 생성하고 즉시 반환한 뒤, 트랜잭션 커밋 후 전용 executor가 프롬프트 생성과 OpenAI Responses API 호출을 비동기로 수행한다.",
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "생성 요청을 수락함",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = L0GenerationExecuteDto::class))],
            ),
            ApiResponse(responseCode = "400", description = "실행 불가능한 ref 또는 Big Five 축과 맞지 않는 단계 옵션", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "프롬프트 ref 또는 생성 옵션 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "429", description = "동시 생성 실행 한도 초과", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/generations")
    fun generate(
        @Valid @RequestBody request: L0CharacterGenerationRequest,
    ): ResponseEntity<L0GenerationExecuteDto> = ResponseEntity.accepted().body(generationService.requestGeneration(request))
}
