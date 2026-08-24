package ltmp.charactergen.api.chat

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.service.chat.CharacterChatMessage
import ltmp.charactergen.service.chat.CharacterChatSession
import ltmp.charactergen.service.chat.ChatRole
import java.time.OffsetDateTime

@Schema(
    description = "캐릭터 대화 세션 생성 요청. 선택 항목을 생략하면 서버 기본 설정을 따른다.",
)
data class CreateCharacterChatSessionRequest(
    @field:Schema(
        description = "대화할 캐릭터 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Positive
    val characterId: Long,

    @field:Schema(
        description = "관계 단계. 열리는 개인사 층위와 주입할 애착 서술이 달라진다.",
        example = "FIRST",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val relationshipStage: RelationshipStage,

    @field:Schema(
        description = "캐릭터에게 부여할 현재 상황. 프로브 P6처럼 특정 상황에서의 반응을 볼 때 사용한다.",
        example = "오늘 마감을 앞두고 담당자에게 원고를 크게 반려당했다.",
        nullable = true,
    )
    @field:Size(max = 2000)
    val situationNote: String? = null,

    @field:Schema(
        description = "대화에 사용할 모델. 생략하면 openai.chat.model 설정값을 사용한다.",
        example = "gpt-5.6-terra",
        nullable = true,
    )
    @field:Size(max = 200)
    val model: String? = null,

    @field:Schema(
        description = "인스트럭션 템플릿 브랜치 이름. 생략하면 기본 브랜치를 사용한다. A/B 비교에 사용한다.",
        example = "main",
        nullable = true,
    )
    @field:Size(max = 200)
    val instructionBranch: String? = null,

    @field:Schema(
        description = "예시 발화를 톤 참조로 주입할지 여부. 생략하면 character-chat.include-voice-samples 설정값을 사용한다.",
        example = "true",
        nullable = true,
    )
    val includeVoiceSamples: Boolean? = null,

    @field:Schema(
        description = "아직 열리지 않은 개인사 층위 처리 방식. 생략하면 TOPIC_ONLY를 사용한다. " +
            "OMIT은 닫힌 주제를 아예 넣지 않고, TOPIC_ONLY는 주제 이름만 넣어 회피할 수 있게 한다.",
        example = "TOPIC_ONLY",
        nullable = true,
    )
    val closedTopicMode: ClosedTopicMode? = null,
)

@Schema(description = "캐릭터에게 보낼 사용자 메시지")
data class CharacterChatMessageRequest(
    @field:Schema(
        description = "사용자 발화",
        example = "안녕하세요",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank
    @field:Size(max = 4000)
    val content: String,
)

@Schema(description = "캐릭터 대화 세션 상태와 지금까지의 대화 기록")
data class CharacterChatSessionDto(
    @field:Schema(description = "세션 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val sessionId: String,

    @field:Schema(description = "대화 중인 캐릭터 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val characterId: Long,

    @field:Schema(description = "대화 중인 캐릭터 이름", example = "서윤", requiredMode = Schema.RequiredMode.REQUIRED)
    val characterName: String,

    @field:Schema(description = "관계 단계", example = "FIRST", requiredMode = Schema.RequiredMode.REQUIRED)
    val relationshipStage: RelationshipStage,

    @field:Schema(description = "사용한 인스트럭션 템플릿 브랜치", example = "main", requiredMode = Schema.RequiredMode.REQUIRED)
    val instructionBranch: String,

    @field:Schema(
        description = "이 세션에서 실제로 주입되는 캐릭터 인스트럭션 전문. 무엇이 주입됐는지 검토하는 용도다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val instructions: String,

    @field:Schema(description = "대화에 사용하는 모델", example = "gpt-5.6-terra", requiredMode = Schema.RequiredMode.REQUIRED)
    val model: String,

    @field:Schema(description = "캐릭터에게 부여한 현재 상황", nullable = true)
    val situationNote: String?,

    @field:Schema(description = "예시 발화 주입 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    val includeVoiceSamples: Boolean,

    @field:Schema(description = "닫힌 개인사 층위 처리 방식", example = "TOPIC_ONLY", requiredMode = Schema.RequiredMode.REQUIRED)
    val closedTopicMode: ClosedTopicMode,

    @field:Schema(description = "세션 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,

    @field:Schema(
        description = "이 시각까지 대화가 없으면 세션이 삭제된다. 대화할 때마다 뒤로 밀린다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val expiresAt: OffsetDateTime,

    @field:Schema(description = "지금까지 오간 대화. 시간 순서다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val messages: List<CharacterChatMessageDto>,
)

@Schema(description = "대화 기록 한 건")
data class CharacterChatMessageDto(
    @field:Schema(description = "발화 주체", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    val role: ChatRole,

    @field:Schema(description = "발화 내용", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,

    @field:Schema(description = "발화 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
)

@Schema(description = "SSE `delta` 이벤트 payload. 응답 텍스트 조각이 이어서 도착한다.")
data class CharacterChatDeltaEventDto(
    @field:Schema(description = "응답 텍스트 조각", example = "안녕", requiredMode = Schema.RequiredMode.REQUIRED)
    val text: String,
)

@Schema(description = "SSE `done` 이벤트 payload. 응답이 정상적으로 끝났음을 알린다.")
data class CharacterChatDoneEventDto(
    @field:Schema(description = "이어붙인 전체 응답", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,

    @field:Schema(
        description = "대화 기록에서 이 응답의 위치(0부터)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val messageIndex: Int,
)

fun CharacterChatSession.toDto(expiresAt: OffsetDateTime): CharacterChatSessionDto = CharacterChatSessionDto(
    sessionId = id,
    characterId = characterId,
    characterName = characterName,
    relationshipStage = relationshipStage,
    instructionBranch = instructionBranch,
    instructions = instructions,
    model = model,
    situationNote = situationNote,
    includeVoiceSamples = includeVoiceSamples,
    closedTopicMode = closedTopicMode,
    createdAt = createdAt,
    expiresAt = expiresAt,
    messages = messages().map { it.toDto() },
)

fun CharacterChatMessage.toDto(): CharacterChatMessageDto = CharacterChatMessageDto(
    role = role,
    content = content,
    createdAt = createdAt,
)
