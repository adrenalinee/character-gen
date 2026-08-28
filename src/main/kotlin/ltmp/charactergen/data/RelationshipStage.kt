package ltmp.charactergen.data

/**
 * 캐릭터가 관계 깊이별로 미리 써 둔 행동 지시문의 단위. (L0 캐릭터 생성 프롬프트 v4 `relationship_stages`)
 *
 * v4에서 관계 단계는 이 세 개로 확정됐다. 서버는 현재 단계에 해당하는 한 벌만 대화 프롬프트에 주입한다.
 *
 * [label]은 운영 화면과 API에서 쓰는 표시명이고, [promptLabel]은 런타임 프롬프트에 들어가는 이름이다.
 * early의 프롬프트 이름에만 "아직"을 붙인다. 관계가 바뀔 수 있다는 것이 함축되지 않으면
 * 캐릭터가 지금의 거리를 고정된 것으로 취급한다. (런타임 프롬프트 조립 스펙 v1 3-6)
 *
 * 어디까지 개인사를 여는지는 이 단계가 정하지 않는다. v4는 개방 판정을 별도의 서버 게이팅으로 두므로
 * [DisclosureLevel]로 따로 지정하고, 지정이 없을 때만 [defaultOpenedLevel]로 파생한다.
 * 특히 `deepest`는 "아직 누구에게도 말한 적 없는 단 하나"이므로 어느 단계에서도 저절로 열리지 않는다.
 */
enum class RelationshipStage(
    val label: String,
    val promptLabel: String,
    val defaultOpenedLevel: DisclosureLevel,
) {
    /** 관계가 얕을 때 */
    EARLY("얕은 관계", "아직 얕은 관계", DisclosureLevel.SURFACE),

    /** 가까워지는 과정 */
    DEEPENING("가까워지는 중", "가까워지는 중", DisclosureLevel.MID),

    /** 충분히 가까워진 뒤 */
    CLOSE("가까운 관계", "가까운 관계", DisclosureLevel.DEEP),
}
