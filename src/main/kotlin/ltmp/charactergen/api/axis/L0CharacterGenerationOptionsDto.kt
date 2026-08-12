package ltmp.charactergen.api.axis

import io.swagger.v3.oas.annotations.media.Schema
import ltmp.charactergen.data.axis.BigFiveTrait

@Schema(description = "L0 캐릭터 생성 화면에서 선택할 수 있는 전체 축 정보")
data class L0CharacterGenerationOptionsDto(
    @field:Schema(description = "활동 분야 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val domains: List<DomainOptionDto>,

    @field:Schema(description = "Lie 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val lies: List<LieOptionDto>,

    @field:Schema(description = "Big Five 성향별 단계 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val personalityTraitStages: List<PersonalityTraitStageOptionDto>,

    @field:Schema(description = "관계 태도 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val attachments: List<TypeDescriptionOptionDto>,

    @field:Schema(description = "정서 표출 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val emotionalExpressions: List<TypeDescriptionOptionDto>,

    @field:Schema(description = "회피 방식 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val avoidanceStyles: List<TypeDescriptionOptionDto>,

    @field:Schema(description = "말투 원형 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val speechArchetypes: List<SpeechArchetypeOptionDto>,
)

@Schema(description = "활동 분야 선택지")
data class DomainOptionDto(
    @field:Schema(description = "식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "분야 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "분야 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val description: String,
)

@Schema(description = "Lie 선택지")
data class LieOptionDto(
    @field:Schema(description = "식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "Lie 유형", requiredMode = Schema.RequiredMode.REQUIRED)
    val type: String,
    @field:Schema(description = "Lie 핵심 명제", requiredMode = Schema.RequiredMode.REQUIRED)
    val coreStatement: String,
)

@Schema(description = "Big Five 성향 단계 선택지")
data class PersonalityTraitStageOptionDto(
    @field:Schema(description = "식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "Big Five 성향", requiredMode = Schema.RequiredMode.REQUIRED)
    val trait: BigFiveTrait,
    @field:Schema(description = "단계(1~5)", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    val stage: Int,
    @field:Schema(description = "해당 성향 단계 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val description: String,
)

@Schema(description = "유형과 설명으로 구성된 축 선택지")
data class TypeDescriptionOptionDto(
    @field:Schema(description = "식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "축 유형", requiredMode = Schema.RequiredMode.REQUIRED)
    val type: String,
    @field:Schema(description = "유형 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val description: String,
)

@Schema(description = "말투 원형 선택지")
data class SpeechArchetypeOptionDto(
    @field:Schema(description = "식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "말투 원형 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "말투 원형 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val description: String,
)
