package ltmp.charactergen.data

/**
 * 캐릭터와 사용자 사이의 관계 단계.
 *
 * 단계에 따라 열리는 개방 사다리 층위와 주입할 관계 단계 서술이 달라진다.
 * (L0 런타임 주입 설계 3절)
 *
 * 런타임은 네 단계이고 캐릭터가 써 둔 [RelationshipStageLevel]은 세 단계이므로,
 * '가까움'과 '특별함'은 같은 서술을 쓰고 열리는 개방 층위로만 갈린다.
 */
enum class RelationshipStage(
    val label: String,
    val openedLevels: Set<DisclosureLevel>,
    val stageLevel: RelationshipStageLevel,
) {
    /** 1 · 처음 */
    FIRST("처음", setOf(DisclosureLevel.SURFACE), RelationshipStageLevel.EARLY),

    /** 2 · 익숙함 */
    FAMILIAR("익숙함", setOf(DisclosureLevel.SURFACE, DisclosureLevel.MID), RelationshipStageLevel.DEEPENING),

    /** 3 · 가까움 */
    CLOSE(
        "가까움",
        setOf(DisclosureLevel.SURFACE, DisclosureLevel.MID, DisclosureLevel.DEEP),
        RelationshipStageLevel.CLOSE,
    ),

    /** 4 · 특별함 */
    SPECIAL(
        "특별함",
        setOf(DisclosureLevel.SURFACE, DisclosureLevel.MID, DisclosureLevel.DEEP, DisclosureLevel.DEEPEST),
        RelationshipStageLevel.CLOSE,
    ),
    ;

    /** 아직 열리지 않은 층위. 닫힌 주제 처리 대상이다. */
    fun closedLevels(): List<DisclosureLevel> = DisclosureLevel.entries.filterNot { it in openedLevels }

    /** 심층 층위가 열린 단계에서만 Lie의 기원이 된 상처를 주입한다. */
    fun opensWound(): Boolean = DisclosureLevel.DEEP in openedLevels
}
