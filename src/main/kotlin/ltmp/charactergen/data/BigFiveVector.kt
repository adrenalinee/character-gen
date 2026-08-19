package ltmp.charactergen.data

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 표현 축의 각 값이 어떤 성향에서 나오기 쉬운지를 나타내는 Big Five 예상 좌표.
 *
 * 각 값은 3단계(중립)를 0으로 본 오프셋이며 -2~+2 범위다.
 * 0은 "이 축이 해당 성향을 예측하지 않는다"는 뜻이고 불균형 판정에서 제외된다.
 */
@Embeddable
class BigFiveVector(
    @Column(name = "openness_vector", nullable = false)
    var openness: Int,

    @Column(name = "conscientiousness_vector", nullable = false)
    var conscientiousness: Int,

    @Column(name = "extraversion_vector", nullable = false)
    var extraversion: Int,

    @Column(name = "agreeableness_vector", nullable = false)
    var agreeableness: Int,

    @Column(name = "neuroticism_vector", nullable = false)
    var neuroticism: Int,
) {
    init {
        listOf(openness, conscientiousness, extraversion, agreeableness, neuroticism).forEach {
            require(it in MIN_OFFSET..MAX_OFFSET) { "Big Five vector offset must be between $MIN_OFFSET and $MAX_OFFSET: $it" }
        }
    }

    fun expectedOffset(trait: BigFiveTrait): Int = when (trait) {
        BigFiveTrait.OPENNESS -> openness
        BigFiveTrait.CONSCIENTIOUSNESS -> conscientiousness
        BigFiveTrait.EXTRAVERSION -> extraversion
        BigFiveTrait.AGREEABLENESS -> agreeableness
        BigFiveTrait.NEUROTICISM -> neuroticism
    }

    companion object {
        /** Big Five 단계를 오프셋으로 바꾸는 기준. 3단계가 중립이다. */
        const val NEUTRAL_STAGE = 3
        const val MIN_OFFSET = -2
        const val MAX_OFFSET = 2

        fun offsetOfStage(stage: Int): Int = stage - NEUTRAL_STAGE
    }
}
