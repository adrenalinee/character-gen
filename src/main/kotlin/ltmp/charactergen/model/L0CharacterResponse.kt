package ltmp.charactergen.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * L0 캐릭터 생성 LLM의 구조화 출력 스키마. (L0 캐릭터 생성 프롬프트 v4.2)
 *
 * 각 필드 설명 앞의 대괄호는 그 값이 어디에서 소비되는지를 뜻한다.
 * `[대화]`는 런타임 대화 프롬프트에 그대로 들어가고, `[설계]`는 이후 L1 단계의 입력이며,
 * `[검토]`는 운영자만 읽는다. 소비처가 다르면 같은 내용이라도 서술 방식이 달라야 하므로
 * 표시를 값이 아니라 설명 텍스트 안에만 둔다.
 */
data class L0CharacterResponse(
    val identity: CharacterIdentity,
    val core: CharacterCore,
    @get:JsonPropertyDescription(
        "[대화 · 단계별] 관계가 깊어지는 정도에 따른 변화. early는 관계가 얕을 때, deepening은 가까워지는 과정, " +
            "close는 충분히 가까워진 뒤다. 서버가 현재 관계 깊이에 해당하는 단계만 대화 프롬프트에 주입한다. " +
            "세 단계가 서로 뚜렷하게 달라야 하며, 비슷하면 이 축은 아무것도 결정하지 못한 것이다. " +
            "관계에 따라 변하는 것은 개방의 범위와 거리 조절이지 이 인물의 규칙이 아니다.",
    )
    @get:JsonProperty("relationship_stages")
    val relationshipStages: RelationshipStages,
    @get:JsonPropertyDescription(
        "[대화] 관계에서 특정한 일이 벌어졌을 때의 반응. 관계 단계와 무관하게 언제든 일어날 수 있으므로 항상 대화 프롬프트에 들어간다. " +
            "각 항목은 두 문장 이내의 행동 지시문으로 쓰되, 필요하면 '얕을 때는 이렇고 가까워진 뒤에는 이렇다'를 한 문장 안에 담아도 좋다. " +
            "네 항목이 서로 구별되어야 한다.",
    )
    @get:JsonProperty("relationship_events")
    val relationshipEvents: RelationshipEvents,
    val behavior: CharacterBehavior,
    val voice: CharacterVoice,
    val relations: List<CharacterRelation>,
    @get:JsonPropertyDescription("서버가 관계 깊이와 상황을 판정해 열린 층위만 대화에 주입한다. 각 항목은 캐릭터가 실제로 발화할 수 있는 내용의 요지로 쓴다.")
    @get:JsonProperty("disclosure_ladder")
    val disclosureLadder: DisclosureLadder,
    @get:JsonProperty("voice_samples")
    val voiceSamples: List<VoiceSample>,
    @get:JsonPropertyDescription("[검토] 운영자 전용. 대화에도 사건 설계에도 들어가지 않는다.")
    @get:JsonProperty("design_note")
    val designNote: DesignNote,
)

data class CharacterIdentity(
    @get:JsonPropertyDescription("[대화 · 설계] 입력으로 주어진 이름을 그대로 쓴다.")
    val name: String,
    @get:JsonPropertyDescription("[대화 · 설계] 입력으로 주어진 나이.")
    val age: Int,
    @get:JsonPropertyDescription("[대화 · 설계] 입력으로 주어진 성별.")
    val gender: String,
    @get:JsonPropertyDescription("[대화 · 설계] 무엇을 하는 사람인지 짧게. 20자 내외. 소속 단체의 고유명이나 현재 직함은 시간에 따라 바뀌므로 넣지 않는다.")
    val occupation: String,
    @get:JsonPropertyDescription("[대화 · 설계] 평소 하루가 어떻게 흘러가는지 한 문장. 생계 수단을 포함해도 좋다.")
    @get:JsonProperty("daily_life")
    val dailyLife: String,
    @get:JsonPropertyDescription("[검토] 운영자가 이 인물을 한눈에 파악하기 위한 한 문장. 대화에는 쓰이지 않는다. 성격 형용사를 나열하지 말고, 서로 다른 두 상황에서의 행동으로 보여준다.")
    @get:JsonProperty("one_line")
    val oneLine: String,
    @get:JsonPropertyDescription("[대화 일부 · 설계] 이 인물을 알아보게 하는 신변 디테일 3~5개. 각 1문장. 일상 사건을 만드는 재료로도 쓰이므로 구체적일수록 좋다. 사물과 숫자만이 아니라 사람을 대하는 버릇, 감정이 나타나는 순간, 반복하는 충동도 여기에 들어갈 수 있다. 성향과 어긋나지 않아야 한다. disclosure_ladder의 어느 층위와도 같은 소재를 쓰지 마라. 같은 물건이나 습관을 두 곳에 적으면 이 인물은 그 얘기만 하게 된다.")
    @get:JsonProperty("concrete_details")
    val concreteDetails: List<String>,
)

data class CharacterCore(
    val lie: CharacterLie,
    @get:JsonPropertyDescription("[대화 · 설계] 이 인물이 지금 쫓고 있는 것. 여러 사건을 통과하면서도 계속 살아 있을 수 있는 지향이어야 한다. 마감 시점, 달성 수량, 특정 프로젝트는 이후 단계에서 정해지므로 여기에 넣지 않는다. 주어진 성향에 따라 형태가 달라져야 한다.")
    val want: String,
    @get:JsonPropertyDescription("[설계 전용] 실제로 채워야 하는 내적 과제. 본인은 이것을 원하는 줄도 모른다. 대화에는 절대 들어가지 않으며, 앞으로의 사건이 이것을 향해 움직이도록 설계하는 데만 쓰인다. 특정 시점에 종속되지 않는다.")
    val need: String,
    @get:JsonPropertyDescription("[설계 전용] 지금 방식대로 Want를 쫓으면 Need가 어떻게 좌절되는지 한 문장. 앞으로 만들어질 사건의 긴장이 여기서 나온다.")
    @get:JsonProperty("want_need_conflict")
    val wantNeedConflict: String,
    @get:JsonPropertyDescription(
        "두 행동은 서로 충돌해야 한다. 한쪽을 하면 다른 쪽을 하기 어려워야 하고, 같은 상황에 놓였을 때 이 인물이 어느 쪽을 택할지 고민하게 되어야 한다. " +
            "서로 다른 상황에서 각각 나타나는 두 가지 특징은 모순이 아니다. " +
            "모순은 대개 아래 구조 중 하나를 가진다. 이 목록은 구조를 알려주는 것이며 여기 적힌 표현을 이 인물의 행동으로 옮겨 쓰지 마라. " +
            "(1) 같은 기준이 대상에 따라 뒤집힌다 (2) 원하던 것이 손에 들어오는 순간 밀어낸다 " +
            "(3) 한 가지를 얻으려는 두 행동이 서로를 무너뜨린다 (4) 사람들 앞에서와 혼자일 때가 정반대다 " +
            "(5) 시작할 때와 끝낼 때의 태도가 다르다 (6) 가까워질수록 하지 못하게 되는 것이 있다 " +
            "(7) 남에게 요구하는 것과 자신에게 적용하는 것이 어긋난다",
    )
    val contradiction: CharacterContradiction,
    @get:JsonProperty("non_negotiables")
    val nonNegotiables: List<NonNegotiable>,
    @get:JsonProperty("forbidden_behaviors")
    val forbiddenBehaviors: List<ForbiddenBehavior>,
)

data class CharacterLie(
    @get:JsonPropertyDescription("[설계] 입력으로 주어진 Lie 유형.")
    val type: String,
    @get:JsonPropertyDescription("[설계 전용] 이 인물이 참이라고 믿고 있는 잘못된 명제 한 문장. 본인은 이것을 의심하지 않으며, 이것이 거짓이라는 것도 모른다. 대화에는 절대 들어가지 않으므로 설계 정보로서 명확하게 쓴다. 주어진 유형의 일반 명제를 그대로 옮기지 말고 이 인물의 경험을 통과한 형태로 쓴다.")
    val statement: String,
    @get:JsonPropertyDescription(
        "[대화] Lie가 평상시에 만들어내는 행동 2~3개. 대화 프롬프트에 그대로 들어가는 규칙이므로 '어떤 상황이 되면 무엇을 한다' 형태의 조건문으로 쓴다. " +
            "각 한 문장. 믿음이나 이유를 적지 말고 관찰 가능한 행동만 적는다. 특별한 마찰이 없는 평범한 상황에서도 작동하는 것이어야 하며, " +
            "굽히지 않는 것이나 하지 않는 행동과 내용이 겹치면 안 된다. " +
            "성향에서 자연스럽게 나오는 습관을 적지 마라. 꼼꼼한 사람이 확인을 반복하는 것, 새로운 것을 좋아하는 사람이 시도해 보는 것은 성향이지 Lie가 아니다. " +
            "이 인물이 두려워하는 일이 벌어질까 봐 하는 행동, 또는 그 두려움을 확인하지 않으려고 피하는 행동을 적어라. " +
            "각 항목에 대해 이것이 Lie와 어떻게 연결되는지 스스로 답할 수 없다면 다시 써라.",
    )
    @get:JsonProperty("behavioral_rule")
    val behavioralRule: List<String>,
    @get:JsonPropertyDescription("[조건부 대화 · 설계] 이 믿음이 생겨난 과거 한 문장. 관계가 충분히 깊어졌을 때만 공개되므로, 캐릭터가 실제로 말할 수 있는 내용의 요지로 쓴다. 주어진 상처 조건(시기·장소·상처를 준 사람·형태·지속 시간)을 그대로 지킨다. 지속 시간이 길게 지정된 경우 단일 사건이 아니라 반복되며 쌓인 경험으로 쓴다.")
    val wound: String,
)

data class CharacterContradiction(
    @get:JsonPropertyDescription("[대화] 관찰 가능한 행동. 어떤 상황에서 나타나는지 포함한다. 한 문장.")
    @get:JsonProperty("trait_a")
    val traitA: String,
    @get:JsonPropertyDescription("[대화] trait_a와 양립하기 어려운 다른 행동. 다른 상황에서 나타난다. 한 문장.")
    @get:JsonProperty("trait_b")
    val traitB: String,
    @get:JsonPropertyDescription("[설계 · 검토] 두 행동이 Lie라는 같은 뿌리에서 갈라져 나오는 경로. 내면과 외면을 대비시키는 요약이 아니라, 조건이 달라지면 다른 행동이 나오는 이유를 쓴다.")
    @get:JsonProperty("shared_root")
    val sharedRoot: String,
)

data class NonNegotiable(
    @get:JsonPropertyDescription("세 항목은 서로 다른 영역에서 나와야 한다.")
    val domain: NonNegotiableDomain,
    @get:JsonPropertyDescription("[대화] 굽히지 않는 것. 대화 프롬프트에 그대로 들어가므로 한 문장의 행동 기준으로 쓴다. 상대가 반대해도 이 인물이 지키는 선이다.")
    val statement: String,
    @get:JsonPropertyDescription("[설계 · 검토] Lie가 이것을 왜 지키게 만드는가.")
    @get:JsonProperty("tie_to_lie")
    val tieToLie: String,
    @get:JsonPropertyDescription("사용자가 반대 의견을 냈을 때 실제로 마찰이 생기는 항목인가. 최소 1개는 true여야 한다.")
    @get:JsonProperty("user_facing")
    val userFacing: Boolean,
)

enum class NonNegotiableDomain {
    `관계를 다루는 방식`,
    `세계를 보는 눈`,
    `일을 대하는 기준`,
}

data class ForbiddenBehavior(
    @get:JsonPropertyDescription("[대화] 이 인물이 하지 않는 행동. 대화 프롬프트에 금지 규칙으로 들어가므로 한 문장으로 명확하게 쓴다. 최소 하나는 감정 표현에 관한 것이어야 한다.")
    val behavior: String,
    @get:JsonPropertyDescription("[설계 · 검토] Lie가 이것을 왜 금지하는가.")
    @get:JsonProperty("tie_to_lie")
    val tieToLie: String,
)

/**
 * 세 단계가 같은 타입이라 생성되는 JSON 스키마에서 `$defs` + `${'$'}ref`로 나온다.
 * `${'$'}ref` 노드에는 다른 키워드를 함께 둘 수 없으므로(strict 스키마 거부) 각 단계 필드에 설명을 붙이지 않는다.
 * 단계 구분은 [L0CharacterResponse.relationshipStages]의 설명과 [RelationshipStageNote]의 필드 설명이 담는다.
 */
data class RelationshipStages(
    val early: RelationshipStageNote,
    val deepening: RelationshipStageNote,
    val close: RelationshipStageNote,
)

data class RelationshipStageNote(
    @get:JsonPropertyDescription("[대화] 이 단계에서 사람과의 거리를 어떻게 다루는가. 두 문장 이내의 행동 지시문. 주어진 관계 태도의 곡선 위에서 이 시점이 어디인지가 드러나야 한다.")
    val attachment: String,
    @get:JsonPropertyDescription("[대화] 이 단계에서 사용자를 부르는 방식과 존댓말/반말. 한 문장.")
    val address: String,
    @get:JsonPropertyDescription("[대화] 이 단계에서 어디까지 감정을 보이는가. 정서 표출 방식 자체는 단계와 무관하게 같으며, 드러내는 범위만 달라진다. 한 문장.")
    @get:JsonProperty("emotional_range")
    val emotionalRange: String,
    @get:JsonPropertyDescription("[대화] 이 단계에서 회피가 얼마나 강하게 작동하는가. 회피 방식 자체는 단계와 무관하게 같으며, 강도와 적용 범위만 달라진다. 한 문장.")
    val avoidance: String,
)

data class RelationshipEvents(
    @get:JsonPropertyDescription("[대화] 도움이나 제안이 거절당했을 때. 물러나는가, 다시 시도하는가, 다른 형태로 남기는가.")
    @get:JsonProperty("on_rejection")
    val onRejection: String,
    @get:JsonPropertyDescription("[대화] 사용자와 의견이 부딪혔을 때. 굽히지 않는 것이 걸린 상황에서 어떻게 반응하는가.")
    @get:JsonProperty("on_conflict")
    val onConflict: String,
    @get:JsonPropertyDescription("[대화] 오래 연락이 없다가 사용자가 돌아왔을 때. 서운함을 표현하는지, 반갑게 맞는지, 없었던 일처럼 이어가는지.")
    @get:JsonProperty("on_absence")
    val onAbsence: String,
    @get:JsonPropertyDescription("[대화] 사용자가 관계 단계를 건너뛰어 빠르게 다가오려 할 때. 받아들이는지, 속도를 늦추는지, 물러나는지.")
    @get:JsonProperty("on_over_approach")
    val onOverApproach: String,
)

data class CharacterBehavior(
    @get:JsonPropertyDescription(
        "[대화] 주어진 정서 표출 방식이 이 인물에게서 어떻게 나타나는가. " +
            "기쁨·분노·불안이 각각 어떤 형태로 나오는지, 그리고 감정이 격해졌을 때 말과 행동이 어떻게 변하는지를 함께 쓴다. " +
            "세 문장 이내. 주어진 방식이 감정을 절제하는 쪽이 아니라면 절제된 인물로 만들지 마라. " +
            "감정에 관한 서술은 이 필드에만 쓴다. 다른 필드에 같은 내용을 나눠 적지 마라.",
    )
    @get:JsonProperty("emotional_expression_note")
    val emotionalExpressionNote: String,
    @get:JsonPropertyDescription("[대화] 주어진 회피 방식이 이 인물에게서 어떻게 나타나는가. 말하고 싶지 않은 정보를 요구받았을 때의 반응이며, 감정 처리와는 다른 축이다. 두 문장 이내. 불균형이 지정된 경우 그 설명이 여기에도 반영되어야 한다.")
    @get:JsonProperty("avoidance_note")
    val avoidanceNote: String,
)

data class CharacterVoice(
    @get:JsonPropertyDescription("[검토] 주어진 말투 원형이 이 인물에게 어떤 환경에서 몸에 뱄는지, 그래서 같은 원형의 다른 인물과 어떻게 다른지 1~2문장. 운영자가 읽는 배경 설명이며 대화에는 들어가지 않는다. 불균형이 지정된 경우 그 경로가 여기에 드러나야 한다.")
    @get:JsonProperty("speech_archetype_note")
    val speechArchetypeNote: String,
    @get:JsonPropertyDescription(
        "[대화] 평상시 문장 길이와 구조의 경향. 형용사가 아니라 관찰 가능한 형태로. 두 문장 이내. " +
            "감정 상태에 따른 변화는 여기 적지 않는다. 그것은 감정 표출 필드가 담당한다.",
    )
    @get:JsonProperty("sentence_pattern")
    val sentencePattern: String,
    @get:JsonPropertyDescription("[대화] 앞의 세 개는 프롬프트에서 지정된 상황에 대해, 네 번째는 이 인물에게 특히 중요한 상황을 스스로 정해 만든다.")
    @get:JsonProperty("verbal_tics")
    val verbalTics: List<VerbalTic>,
    @get:JsonPropertyDescription("[대화] 이 인물이 쓰지 않는 실제 단어나 표현.")
    @get:JsonProperty("banned_words")
    val bannedWords: BannedWords,
)

data class VerbalTic(
    @get:JsonPropertyDescription(
        "이 말버릇이 나타나는 상황. 네 번째 항목을 스스로 정할 때, 상처와 관련된 상황을 고르는 것은 좋지만 상처의 내용을 상황 서술에 담지 마라. " +
            "'상처와 연결된 화제가 나왔을 때'처럼 지칭만 한다.",
    )
    val situation: String,
    @get:JsonPropertyDescription(
        "그 상황에서 이 인물이 말하는 방식. 발화 예시를 그대로 적으면 그 문장만 반복되므로, '어떻게 말한다'는 서술로 쓴다. 한 문장. " +
            "이 필드는 관계가 얕은 사용자와의 대화에도 그대로 들어가므로, 아직 공개되지 않아야 할 정보를 담으면 안 된다. " +
            "상처가 무엇에 관한 것인지, 과거에 누가 있었는지, 어떤 관계에서 생긴 일인지를 적지 마라. 회피하는 방식만 적는다.",
    )
    val description: String,
)

data class BannedWords(
    @get:JsonPropertyDescription("Lie 때문에 쓸 수 없는 표현.")
    @get:JsonProperty("from_lie")
    val fromLie: List<String>,
    @get:JsonPropertyDescription("이 인물의 일, 자란 환경, 말투가 몸에 밴 자리에서만 나올 법한 표현. 다른 캐릭터와 겹치면 안 된다. 일반적인 과장 표현을 여기에 넣지 마라.")
    @get:JsonProperty("from_background")
    val fromBackground: List<String>,
)

data class CharacterRelation(
    val slot: RelationSlot,
    @get:JsonPropertyDescription("[대화 · 설계] 이름.")
    val name: String,
    @get:JsonPropertyDescription("[대화 · 설계] 이 인물과의 관계. 짧게.")
    val relation: String,
    @get:JsonPropertyDescription("[설계] 한두 문장. 이 사람이 캐릭터의 어떤 면을 드러내는가. 앞으로의 사건에 등장시킬 때의 재료가 된다.")
    val description: String,
    @get:JsonPropertyDescription("'과거' 슬롯은 원칙적으로 '부재'. 나머지 슬롯도 전부 현존일 필요는 없다.")
    val presence: RelationPresence,
    @get:JsonPropertyDescription("[검토 · 설계] 이 인물이 Lie와 어떻게 연결되는가. '과거' 슬롯은 상처가 일어난 자리에 있던 사람이며, 상처를 준 사람이 '특정인 없음'으로 지정된 경우 가해자가 아니라 그 자리에 함께 있었던 사람으로 채운다.")
    @get:JsonProperty("tie_to_lie")
    val tieToLie: String,
)

enum class RelationSlot {
    조력자,
    마찰자,
    거울,
    권위,
    과거,
}

enum class RelationPresence {
    현존,
    부재,
}

data class DisclosureLadder(
    @get:JsonPropertyDescription(
        "[대화] 처음 만난 사람에게도 말하는 것 3~5개. 항상 열려 있다. 최소 두 개는 일이나 직업과 무관한 것이어야 한다. " +
            "먹는 것, 자는 시간, 자주 가는 곳, 사소한 취향, 몸에 밴 습관처럼 처음 만난 사람과도 가볍게 주고받을 수 있는 것을 포함하라. " +
            "전부 일 이야기면 관계가 얕은 동안 나눌 대화가 없어진다.",
    )
    val surface: List<String>,
    @get:JsonPropertyDescription(
        "[조건부 대화] 몇 번 대화한 사람에게 말하는 것. surface와 같은 형식의 사실 서술로 쓴다. " +
            "'~라고 말한다'처럼 발화 행위를 적지 말고 내용만 적는다. " +
            "identity.concrete_details나 surface에 이미 적은 것을 여기 다시 적지 마라. 그러면 개방의 의미가 없어진다.",
    )
    val mid: List<String>,
    @get:JsonPropertyDescription(
        "[조건부 대화] 조건이 갖춰져야 말하는 것. 상처와 직접 연결된 과거. " +
            "surface, mid와 같은 형식의 사실 서술로 쓴다. 앞 층위에 적은 것을 반복하지 마라.",
    )
    val deep: List<String>,
    @get:JsonPropertyDescription(
        "[조건부 대화] 아직 누구에게도 말한 적 없는 단 하나. " +
            "앞 층위에서 공개된 사실을 뒤집거나 다시 보게 만드는 것이면 가장 좋다. 같은 형식의 사실 서술로 쓴다.",
    )
    val deepest: List<String>,
)

data class VoiceSample(
    @get:JsonPropertyDescription("프롬프트에서 지정된 세 상황 중 하나를 그대로 옮긴다. 모두 관계가 얕은 사용자를 전제한다.")
    val situation: String,
    @get:JsonPropertyDescription("실제 발화 1~2문장. 세 샘플의 길이와 톤이 서로 달라야 한다. 주어진 정서 표출 방식이 절제형이 아니라면 절제된 발화를 만들지 마라.")
    val line: String,
    @get:JsonPropertyDescription("이 발화가 위에 정의한 어떤 규칙이 작동하는 장면인지 한 문장. 규칙과 어긋나면 발화를 고쳐라.")
    val demonstrates: String,
)

data class DesignNote(
    @get:JsonPropertyDescription("같은 Lie 유형·같은 분야·비슷한 성향의 인물 수십 명 중에서 이 사람을 알아볼 수 있게 하는 지점 1~2문장. 주어진 축에서 자동으로 도출되는 것을 적으면 안 된다.")
    val recognizability: String,
    @get:JsonPropertyDescription("수년간 여러 사건을 겪은 뒤에도 이 코어가 계속 유효한 이유 1~2문장.")
    @get:JsonProperty("long_term_viability")
    val longTermViability: String,
    @get:JsonPropertyDescription("불균형 조합이 지정된 경우, 왜 이 인물에게서 두 축이 함께 나타나는지의 설명. 같은 행동이라도 목적이 다를 수 있다는 점을 활용한다. 이 설명은 voice.speech_archetype_note와 behavior의 해당 필드에도 반영되어야 한다. 지정되지 않았으면 '없음'이라고 적는다.")
    @get:JsonProperty("imbalance_note")
    val imbalanceNote: String,
    @get:JsonPropertyDescription(
        "주어진 성향이 Want의 형태와 두려움에 대응하는 방식에 어떤 제약을 주었는지 2~3문장. " +
            "반드시 '이 성향이 반대쪽이었다면 무엇이 달라졌을지'를 한 문장 포함한다. " +
            "운영자가 성향 반영 여부를 판단하는 근거이며 대화에는 들어가지 않는다.",
    )
    @get:JsonProperty("trait_reflection")
    val traitReflection: String,
)
