package ltmp.charactergen.api.character

import io.swagger.v3.oas.annotations.media.Schema
import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.RelationshipStage

@Schema(
    description = "캐릭터 런타임 인스트럭션 조립 요청. 캐릭터와의 관계 단계만 지정하면, " +
        "공개 범위와 그 밖의 조립 정책은 서버 기본값으로 결정한다.",
)
data class CreateCharacterRuntimeInstructionRequest(
    @field:Schema(
        description = "관계 단계. 캐릭터가 이 단계에 대해 작성한 거리·호칭·감정 범위·회피 강도를 인스트럭션에 반영한다.",
        example = "EARLY",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val relationshipStage: RelationshipStage,
)

@Schema(description = "캐릭터와 관계 단계로 조립한 런타임 인스트럭션")
data class CharacterRuntimeInstructionDto(
    @field:Schema(description = "대상 캐릭터 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val characterId: Long,

    @field:Schema(description = "대상 캐릭터 이름", example = "서윤", requiredMode = Schema.RequiredMode.REQUIRED)
    val characterName: String,

    @field:Schema(description = "요청한 관계 단계", example = "EARLY", requiredMode = Schema.RequiredMode.REQUIRED)
    val relationshipStage: RelationshipStage,

    @field:Schema(
        description = "관계 단계에서 파생해 실제로 열린 개인사 층위(EARLY=SURFACE, DEEPENING=MID, CLOSE=DEEP)",
        example = "SURFACE",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val openedDisclosureLevel: DisclosureLevel,

    @field:Schema(
        description = "서버 기본 CHARACTER_RUNTIME 인스트럭션 프롬프트 ref 식별자",
        example = "10",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val refId: Long,

    @field:Schema(
        description = "서버 기본 CHARACTER_RUNTIME 인스트럭션 템플릿 브랜치",
        example = "main",
        nullable = true,
    )
    val instructionBranch: String? = null,

    @field:Schema(description = "조립된 런타임 인스트럭션 전문", requiredMode = Schema.RequiredMode.REQUIRED)
    val instructions: String,

    @field:Schema(
        description = "예시 발화 주입 여부. EARLY 단계에서만 서버 정책으로 주입한다.",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val includeVoiceSamples: Boolean,

    @field:Schema(
        description = "닫힌 개인사 층위 처리 방식. 서버 정책상 TOPIC_ONLY를 사용한다.",
        example = "TOPIC_ONLY",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val closedTopicMode: ClosedTopicMode,
)
