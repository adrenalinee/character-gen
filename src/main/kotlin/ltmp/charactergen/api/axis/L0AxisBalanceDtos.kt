package ltmp.charactergen.api.axis

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.ExpressionAxis
import ltmp.charactergen.service.AxisBalanceResult
import ltmp.charactergen.service.AxisConflict
import ltmp.charactergen.service.AxisImbalance

@Schema(
    description = "성향과 표현 축의 정합성을 미리 점검할 입력. 모든 필드가 선택 사항이며, " +
        "지정되지 않은 축은 판정에서 제외된다.",
)
data class L0AxisBalanceCheckRequest(
    @field:Positive
    @field:Schema(description = "개방성 단계 옵션 식별자", example = "3", nullable = true)
    val opennessStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "성실성 단계 옵션 식별자", example = "8", nullable = true)
    val conscientiousnessStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "외향성 단계 옵션 식별자", example = "11", nullable = true)
    val extraversionStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "우호성 단계 옵션 식별자", example = "18", nullable = true)
    val agreeablenessStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "신경성 단계 옵션 식별자", example = "23", nullable = true)
    val neuroticismStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "관계 태도 옵션 식별자", example = "1", nullable = true)
    val attachmentStyleId: Long? = null,

    @field:Positive
    @field:Schema(description = "정서 표출 옵션 식별자", example = "1", nullable = true)
    val emotionalExpressionId: Long? = null,

    @field:Positive
    @field:Schema(description = "회피 방식 옵션 식별자", example = "3", nullable = true)
    val avoidanceStyleId: Long? = null,

    @field:Positive
    @field:Schema(description = "말투 원형 옵션 식별자", example = "3", nullable = true)
    val speechArchetypeId: Long? = null,
)

@Schema(
    description = "축 정합성 점검 결과. 운영자 화면에서 경고를 표시하는 데 사용하며, " +
        "blocked가 true인 조합은 생성 요청 시 400으로 거부된다.",
)
data class L0AxisBalanceCheckDto(
    @field:Schema(
        description = "성향과 예상 좌표의 거리가 3 이상인 항목. 비어 있으면 정합이다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val imbalances: List<AxisImbalanceDto>,

    @field:Schema(description = "같은 상황에서 반대를 지시하는 축 조합. 없으면 null이다.", nullable = true)
    val conflict: AxisConflictDto? = null,

    @field:Schema(
        description = "생성 요청이 거부되는 조합인지 여부. 충돌이 있거나 불균형이 2개 이상이면 true다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val blocked: Boolean,

    @field:Schema(
        description = "이 조합으로 생성할 때 프롬프트에 주입될 불균형 안내 문장. 불균형이 없으면 null이다.",
        nullable = true,
    )
    val imbalanceNote: String? = null,
)

@Schema(description = "성향과 표현 축 사이의 불균형 한 건")
data class AxisImbalanceDto(
    @field:Schema(description = "불균형이 발생한 표현 축", requiredMode = Schema.RequiredMode.REQUIRED)
    val axis: ExpressionAxis,

    @field:Schema(description = "표현 축에 선택된 값", example = "장황형", requiredMode = Schema.RequiredMode.REQUIRED)
    val axisValueName: String,

    @field:Schema(description = "거리가 벌어진 Big Five 성향", requiredMode = Schema.RequiredMode.REQUIRED)
    val trait: BigFiveTrait,

    @field:Schema(
        description = "입력된 성향 단계",
        example = "1",
        minimum = "1",
        maximum = "5",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val stage: Int,

    @field:Schema(
        description = "표현 축이 예상하는 성향 오프셋. 3단계를 0으로 본 값이다.",
        example = "1",
        minimum = "-2",
        maximum = "2",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val expectedOffset: Int,

    @field:Schema(
        description = "입력된 성향과 예상 좌표 사이의 거리. 3 이상이면 불균형이다.",
        example = "3",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val distance: Int,

    @field:Schema(
        description = "운영자 화면에 표시할 설명",
        example = "외향성 1단계 × 말투 원형 '장황형'",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val description: String,
)

@Schema(description = "함께 지정할 수 없는 축 조합")
data class AxisConflictDto(
    @field:Schema(description = "정서 표출 유형", example = "즉시 표출", requiredMode = Schema.RequiredMode.REQUIRED)
    val emotionalExpressionType: String,

    @field:Schema(
        description = "회피 방식 유형",
        example = "짧게 답하고 침묵",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val avoidanceStyleType: String,

    @field:Schema(description = "이 조합을 막는 이유", requiredMode = Schema.RequiredMode.REQUIRED)
    val reason: String,

    @field:Schema(
        description = "운영자 화면에 표시할 설명",
        example = "정서 표출 '즉시 표출' × 회피 방식 '짧게 답하고 침묵'",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val description: String,
)

fun AxisImbalance.toDto(): AxisImbalanceDto = AxisImbalanceDto(
    axis = axis,
    axisValueName = axisValueName,
    trait = trait,
    stage = stage,
    expectedOffset = expectedOffset,
    distance = distance,
    description = describe(),
)

fun AxisConflict.toDto(): AxisConflictDto = AxisConflictDto(
    emotionalExpressionType = emotionalExpressionType,
    avoidanceStyleType = avoidanceStyleType,
    reason = reason,
    description = describe(),
)

fun AxisBalanceResult.toCheckDto(imbalanceNote: String?): L0AxisBalanceCheckDto = L0AxisBalanceCheckDto(
    imbalances = imbalances.map { it.toDto() },
    conflict = conflict?.toDto(),
    blocked = blocked,
    imbalanceNote = imbalanceNote,
)
