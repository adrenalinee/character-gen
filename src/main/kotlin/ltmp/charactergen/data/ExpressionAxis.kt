package ltmp.charactergen.data

/**
 * 성향과 달리 표현 방식을 결정하는 축. 각 축은 서로 다른 상황에 적용된다.
 */
enum class ExpressionAxis(val label: String) {
    ATTACHMENT_STYLE("관계 태도"),
    EMOTIONAL_EXPRESSION("정서 표출"),
    AVOIDANCE_STYLE("회피 방식"),
    SPEECH_ARCHETYPE("말투 원형"),
}
