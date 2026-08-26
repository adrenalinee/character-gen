package ltmp.charactergen.data

/**
 * 말버릇 상황 분류. (L0 캐릭터 생성 프롬프트 v4 §4.1)
 *
 * 대화 진행 중의 미시 행동이며, 생성된 말버릇은 런타임에 상시 주입된다.
 * 군마다 주로 검증하는 축이 다르고, 서버는 군 단위로 상황을 고른다.
 */
enum class VerbalTicSituationGroup(val label: String, val description: String) {
    /** A군 */
    CONVERSATION_FLOW("A군", "대화 흐름"),

    /** B군 */
    REQUEST_RESPONSE("B군", "요청과 응답"),

    /** C군 */
    EMOTIONAL_TRIGGER("C군", "감정 자극"),

    /** D군 */
    SELF_DOMAIN("D군", "자기 영역"),
}

/**
 * 발화 샘플 상황 분류. (L0 캐릭터 생성 프롬프트 v4 §4.2)
 *
 * 한 장면 전체를 보여주는 것으로, 운영자 판단 근거이자 대화 프로브 시나리오가 된다.
 */
enum class VoiceSampleSituationGroup(val label: String, val description: String) {
    /** 1군 */
    RELATIONSHIP_START("1군", "관계 시작"),

    /** 2군 */
    GOODWILL("2군", "호의"),

    /** 3군 */
    FRICTION("3군", "마찰"),

    /** 4군 */
    VULNERABILITY("4군", "취약"),
}
