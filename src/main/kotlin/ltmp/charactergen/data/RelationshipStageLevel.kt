package ltmp.charactergen.data

/**
 * L0 스키마 `relationship_stages`의 세 단계. (L0 캐릭터 생성 프롬프트 v4 §2)
 *
 * 런타임 관계 단계인 [RelationshipStage]와는 층이 다르다.
 * 전자는 대화 세션이 놓인 위치이고, 이것은 캐릭터가 그 위치별로 미리 써 둔 행동 지시문의 단위다.
 */
enum class RelationshipStageLevel {
    /** 관계가 얕을 때 */
    EARLY,

    /** 가까워지는 과정 */
    DEEPENING,

    /** 충분히 가까워진 뒤 */
    CLOSE,
}
