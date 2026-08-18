package ltmp.charactergen.api.axis

import io.swagger.v3.oas.annotations.media.Schema
import ltmp.charactergen.data.BigFiveTrait

@Schema(description = "L0 캐릭터 생성 화면에서 선택할 수 있는 전체 축 정보")
data class L0CharacterGenerationOptionsDto(
    @field:Schema(description = "활동 분야 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val domains: List<DomainOptionDto>,

    @field:Schema(description = "Lie 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val lies: List<LieOptionDto>,

    @field:Schema(description = "상처가 생긴 시기 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val woundTimings: List<WoundOptionDto>,

    @field:Schema(description = "상처가 생긴 장소 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val woundPlaces: List<WoundOptionDto>,

    @field:Schema(description = "상처를 준 사람 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val woundAgents: List<WoundOptionDto>,

    @field:Schema(description = "상처 형태 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val woundForms: List<WoundOptionDto>,

    @field:Schema(description = "상처 지속 시간 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val woundDurations: List<WoundOptionDto>,

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

    @field:Schema(description = "존댓말/반말 기본 정책 선택지", requiredMode = Schema.RequiredMode.REQUIRED)
    val speechRegisters: List<SpeechRegisterOptionDto>,
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

@Schema(description = "상처 조건 축 선택지")
data class WoundOptionDto(
    @field:Schema(description = "식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "프롬프트에 주입할 값", requiredMode = Schema.RequiredMode.REQUIRED)
    val value: String,
    @field:Schema(description = "값의 설계 의도 또는 주의 사항", nullable = true)
    val description: String? = null,
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

@Schema(description = "존댓말/반말 기본 정책 선택지")
data class SpeechRegisterOptionDto(
    @field:Schema(description = "식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "프롬프트에 사용할 정책 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
)
