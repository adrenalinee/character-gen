package ltmp.charactergen.service.generation

import ltmp.charactergen.model.BannedWords
import ltmp.charactergen.model.CharacterBehavior
import ltmp.charactergen.model.CharacterContradiction
import ltmp.charactergen.model.CharacterCore
import ltmp.charactergen.model.CharacterIdentity
import ltmp.charactergen.model.CharacterLie
import ltmp.charactergen.model.CharacterRelation
import ltmp.charactergen.model.CharacterVoice
import ltmp.charactergen.model.DesignNote
import ltmp.charactergen.model.DisclosureLadder
import ltmp.charactergen.model.ForbiddenBehavior
import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.model.NonNegotiable
import ltmp.charactergen.model.NonNegotiableDomain
import ltmp.charactergen.model.RelationPresence
import ltmp.charactergen.model.RelationSlot
import ltmp.charactergen.model.RelationshipEvents
import ltmp.charactergen.model.RelationshipStageNote
import ltmp.charactergen.model.RelationshipStages
import ltmp.charactergen.model.VerbalTic
import ltmp.charactergen.model.VoiceSample

/**
 * 생성 결과 한 벌. 개수 규정과 겹침 판정을 함께 보는 테스트가 같은 캐릭터를 쓰도록 한 곳에 둔다.
 *
 * 기본값은 스키마 규정을 만족하는 개수다. 규정을 벗어난 상황은 인자로 개수를 줄이거나 늘려서 만든다.
 */
fun l0CharacterResponse(
    concreteDetails: List<String> = listOf("묘사1", "묘사2", "묘사3"),
    surface: List<String> = listOf("surface1", "surface2", "surface3"),
    behavioralRules: List<String> = listOf("평소 행동 규칙", "평소 행동 규칙2"),
    nonNegotiableCount: Int = 3,
    forbiddenBehaviorCount: Int = 3,
    verbalTicCount: Int = 4,
    wound: String = "상처",
    deep: List<String> = listOf("deep1"),
    stageEmotionalRange: String? = null,
    stageAvoidance: String? = null,
): L0CharacterResponse = L0CharacterResponse(
    identity = CharacterIdentity(
        name = "서윤",
        age = 29,
        gender = "여성",
        occupation = "웹툰 작가",
        dailyLife = "새벽에 작업하고 낮에 잔다",
        oneLine = "한 줄 요약",
        concreteDetails = concreteDetails,
    ),
    core = CharacterCore(
        lie = CharacterLie(
            type = "대체 가능성 공포형",
            statement = "믿음",
            behavioralRule = behavioralRules,
            wound = wound,
        ),
        want = "쫓는 것",
        need = "필요한 것",
        wantNeedConflict = "충돌",
        contradiction = CharacterContradiction("특성 A", "특성 B", "공통 뿌리"),
        nonNegotiables = (1..nonNegotiableCount).map {
            NonNegotiable(NonNegotiableDomain.`관계를 다루는 방식`, "굽히지 않는 것$it", "연결", true)
        },
        forbiddenBehaviors = (1..forbiddenBehaviorCount).map {
            ForbiddenBehavior("하지 않는 것$it", "연결")
        },
    ),
    relationshipStages = RelationshipStages(
        early = stageNote("관계 단계 애착", stageEmotionalRange, stageAvoidance),
        deepening = stageNote("깊어질 때 애착", stageEmotionalRange, stageAvoidance),
        close = stageNote("가까울 때 애착", stageEmotionalRange, stageAvoidance),
    ),
    relationshipEvents = RelationshipEvents(
        onRejection = "거절당했을 때",
        onConflict = "부딪혔을 때",
        onAbsence = "오랜만일 때",
        onOverApproach = "너무 다가올 때",
    ),
    behavior = CharacterBehavior(
        emotionalExpressionNote = "정서 표출 서술",
        avoidanceNote = "회피 서술",
    ),
    voice = CharacterVoice(
        speechArchetypeNote = "말투 원형 설명",
        sentencePattern = "문장 패턴",
        verbalTics = (1..verbalTicCount).map { VerbalTic("상황$it", "말버릇$it") },
        bannedWords = BannedWords(fromLie = listOf("무조건"), fromBackground = listOf("당연히")),
    ),
    relations = listOf(
        CharacterRelation(RelationSlot.과거, "이름", "관계", "설명", RelationPresence.부재, "연결"),
    ),
    disclosureLadder = DisclosureLadder(
        surface = surface,
        mid = listOf("mid1", "mid2"),
        deep = deep,
        deepest = emptyList(),
    ),
    voiceSamples = listOf(VoiceSample("상황", "발화", "무엇을 보여주는가")),
    designNote = DesignNote("판별", "장기", "없음", "성향 반영"),
)

private fun stageNote(
    attachment: String,
    emotionalRange: String?,
    avoidance: String?,
) = RelationshipStageNote(
    attachment = attachment,
    address = "$attachment 호칭",
    emotionalRange = emotionalRange ?: "$attachment 감정",
    avoidance = avoidance ?: "$attachment 회피",
)
