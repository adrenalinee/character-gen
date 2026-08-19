package ltmp.charactergen.service

import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.BigFiveVector
import ltmp.charactergen.data.ExpressionAxis
import ltmp.charactergen.data.entity.AttachmentStyleEntity
import ltmp.charactergen.data.entity.AvoidanceStyleEntity
import ltmp.charactergen.data.entity.EmotionalExpressionEntity
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.entity.SpeechArchetypeEntity
import ltmp.charactergen.data.repo.AxisConflictRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

/**
 * 성향과 표현 축의 정합성을 판정한다.
 *
 * 불균형은 성향과 표현 사이의 거리이고, 충돌은 두 축이 같은 상황에서 반대를 지시하는 것이다.
 * 전자는 예상 Big Five 좌표와의 거리로 계산하고, 후자는 별도의 금지 매트릭스로 관리한다.
 */
@Service
class L0AxisBalanceService(
    private val axisConflictRepository: AxisConflictRepository,
) {
    @Transactional(readOnly = true)
    fun evaluate(
        opennessStage: PersonalityTraitStageEntity?,
        conscientiousnessStage: PersonalityTraitStageEntity?,
        extraversionStage: PersonalityTraitStageEntity?,
        agreeablenessStage: PersonalityTraitStageEntity?,
        neuroticismStage: PersonalityTraitStageEntity?,
        attachmentStyle: AttachmentStyleEntity?,
        emotionalExpression: EmotionalExpressionEntity?,
        avoidanceStyle: AvoidanceStyleEntity?,
        speechArchetype: SpeechArchetypeEntity?,
    ): AxisBalanceResult {
        val stages = buildMap {
            opennessStage?.let { put(BigFiveTrait.OPENNESS, it.stage) }
            conscientiousnessStage?.let { put(BigFiveTrait.CONSCIENTIOUSNESS, it.stage) }
            extraversionStage?.let { put(BigFiveTrait.EXTRAVERSION, it.stage) }
            agreeablenessStage?.let { put(BigFiveTrait.AGREEABLENESS, it.stage) }
            neuroticismStage?.let { put(BigFiveTrait.NEUROTICISM, it.stage) }
        }
        val axisValues = listOfNotNull(
            attachmentStyle?.let {
                ExpressionAxisValue(ExpressionAxis.ATTACHMENT_STYLE, it.type, it.bigFiveVector)
            },
            emotionalExpression?.let {
                ExpressionAxisValue(ExpressionAxis.EMOTIONAL_EXPRESSION, it.type, it.bigFiveVector)
            },
            avoidanceStyle?.let {
                ExpressionAxisValue(ExpressionAxis.AVOIDANCE_STYLE, it.type, it.bigFiveVector)
            },
            speechArchetype?.let {
                ExpressionAxisValue(ExpressionAxis.SPEECH_ARCHETYPE, it.name, it.bigFiveVector)
            },
        )
        return AxisBalanceResult(
            imbalances = findImbalances(stages, axisValues),
            conflict = findConflict(emotionalExpression, avoidanceStyle),
        )
    }

    /**
     * 성향 단계와 표현 축의 예상 좌표 사이 거리가 임계값 이상인 항목을 찾는다.
     *
     * 예상 좌표가 0인 성향은 그 축이 예측하지 않는 성향이므로 판정에서 제외하고,
     * 지정되지 않은 성향과 표현 축도 제외한다. 거리가 큰 순서로 반환한다.
     */
    fun findImbalances(
        stages: Map<BigFiveTrait, Int>,
        axisValues: List<ExpressionAxisValue>,
    ): List<AxisImbalance> = axisValues
        .flatMap { axisValue ->
            BigFiveTrait.entries.mapNotNull { trait ->
                val expectedOffset = axisValue.vector.expectedOffset(trait)
                if (expectedOffset == 0) return@mapNotNull null
                val stage = stages[trait] ?: return@mapNotNull null
                val distance = abs(BigFiveVector.offsetOfStage(stage) - expectedOffset)
                if (distance < IMBALANCE_DISTANCE) {
                    null
                } else {
                    AxisImbalance(
                        axis = axisValue.axis,
                        axisValueName = axisValue.name,
                        trait = trait,
                        stage = stage,
                        expectedOffset = expectedOffset,
                        distance = distance,
                    )
                }
            }
        }
        .sortedByDescending { it.distance }

    /**
     * 프롬프트의 `IMBALANCE_NOTE`에 주입할 문장. 불균형이 없으면 null이다.
     *
     * 불균형은 요청 검증에서 하나만 통과시키므로 정상 경로에서는 항상 한 건이다.
     * 여러 건이 들어오면 거리가 가장 먼 하나로 설명을 만든다.
     */
    fun buildImbalanceNote(imbalances: List<AxisImbalance>): String? {
        val imbalance = imbalances.maxByOrNull { it.distance } ?: return null
        return """
            이 인물의 ${imbalance.traitLabel()}와 ${imbalance.axisValueLabel()} 조합은 일반적으로 함께 나타나지 않는다.
            이것을 오류로 보지 말고, 왜 이 사람에게서 둘이 함께 나타나는지를
            상처와 성장 배경으로 설명하라. 그 설명이 이 인물의 핵심 특징이 된다.
            design_note.imbalance_note에 그 설명을 적어라.
        """.trimIndent()
    }

    private fun findConflict(
        emotionalExpression: EmotionalExpressionEntity?,
        avoidanceStyle: AvoidanceStyleEntity?,
    ): AxisConflict? {
        if (emotionalExpression == null || avoidanceStyle == null) return null
        val conflict = axisConflictRepository.findByEmotionalExpressionIdAndAvoidanceStyleId(
            emotionalExpression.id,
            avoidanceStyle.id,
        ) ?: return null
        return AxisConflict(
            emotionalExpressionType = emotionalExpression.type,
            avoidanceStyleType = avoidanceStyle.type,
            reason = conflict.reason,
        )
    }

    private companion object {
        /** 이 거리 이상이면 불균형으로 본다. 거리 2는 약한 불균형이라 알리지 않고 통과시킨다. */
        const val IMBALANCE_DISTANCE = 3
    }
}

data class ExpressionAxisValue(
    val axis: ExpressionAxis,
    val name: String,
    val vector: BigFiveVector,
)

data class AxisImbalance(
    val axis: ExpressionAxis,
    val axisValueName: String,
    val trait: BigFiveTrait,
    val stage: Int,
    val expectedOffset: Int,
    val distance: Int,
) {
    fun traitLabel(): String = "${trait.label} ${stage}단계"

    fun axisValueLabel(): String = "${axis.label} '$axisValueName'"

    fun describe(): String = "${traitLabel()} × ${axisValueLabel()}"
}

data class AxisConflict(
    val emotionalExpressionType: String,
    val avoidanceStyleType: String,
    val reason: String,
) {
    fun describe(): String =
        "${ExpressionAxis.EMOTIONAL_EXPRESSION.label} '$emotionalExpressionType' × " +
            "${ExpressionAxis.AVOIDANCE_STYLE.label} '$avoidanceStyleType'"
}

data class AxisBalanceResult(
    val imbalances: List<AxisImbalance>,
    val conflict: AxisConflict? = null,
) {
    /** 생성 요청이 거부되는 조합인지 여부. */
    val blocked: Boolean get() = conflict != null || imbalances.size > MAX_ALLOWED_IMBALANCES

    companion object {
        /** 불균형은 한 번에 하나까지만 허용한다. */
        const val MAX_ALLOWED_IMBALANCES = 1
    }
}
