package ltmp.charactergen.data

/**
 * 개방 사다리의 층위. 아래로 갈수록 깊고, 서버가 어디까지 열지 판정한다.
 * (L0 캐릭터 생성 프롬프트 v4 §2 — `disclosure_ladder`는 GATE + 조건부 대화 필드다)
 */
enum class DisclosureLevel {
    SURFACE,
    MID,
    DEEP,
    DEEPEST,
    ;

    /** 이 층위까지 열렸을 때 함께 열려 있는 층위. */
    fun openedThrough(): Set<DisclosureLevel> = entries.filterTo(mutableSetOf()) { it.ordinal <= ordinal }

    /** 이 층위까지만 열렸을 때 아직 닫혀 있는 층위. 닫힌 주제 처리 대상이다. */
    fun closedAbove(): List<DisclosureLevel> = entries.filter { it.ordinal > ordinal }

    /** 상처는 심층 층위가 열린 뒤에만 꺼낼 수 있다. */
    fun opensWound(): Boolean = ordinal >= DEEP.ordinal
}
