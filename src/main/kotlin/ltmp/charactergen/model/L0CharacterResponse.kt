package ltmp.charactergen.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class L0CharacterResponse(
    val identity: CharacterIdentity,
    val core: CharacterCore,
    val traits: CharacterTraits,
    val behavior: CharacterBehavior,
    val voice: CharacterVoice,
    val relations: List<CharacterRelation>,
    @get:JsonProperty("disclosure_ladder")
    val disclosureLadder: DisclosureLadder,
    @get:JsonProperty("voice_samples")
    val voiceSamples: List<VoiceSample>,
    @get:JsonProperty("design_note")
    val designNote: DesignNote,
)

data class CharacterIdentity(
    @get:JsonPropertyDescription("입력으로 주어진 이름을 그대로 쓴다.")
    val name: String,
    val age: Int,
    val gender: String,
    @get:JsonPropertyDescription("무엇을 하는 사람인지 짧게. 20자 내외. 소속 단체의 고유명이나 현재 직함은 시간에 따라 바뀌므로 넣지 않는다.")
    val occupation: String,
    @get:JsonPropertyDescription("평소 하루가 어떻게 흘러가는지 한 문장.")
    @get:JsonProperty("daily_life")
    val dailyLife: String,
    @get:JsonPropertyDescription("이 인물을 한 문장으로. 성격 형용사를 나열하지 말고, 서로 다른 두 상황에서의 행동으로 보여준다.")
    @get:JsonProperty("one_line")
    val oneLine: String,
    @get:JsonPropertyDescription("이 인물을 알아보게 하는 신변 디테일. 각 1문장. 사물과 숫자만이 아니라 사람을 대하는 버릇, 감정이 나타나는 순간, 반복하는 충동도 여기에 들어갈 수 있다. 성향과 어긋나지 않아야 한다.")
    @get:JsonProperty("concrete_details")
    val concreteDetails: List<String>,
)

data class CharacterCore(
    val lie: CharacterLie,
    @get:JsonPropertyDescription("이 인물이 지금 쫓고 있는 것. 여러 사건을 통과하면서도 계속 살아 있을 수 있는 지향이어야 한다. 마감 시점, 달성 수량, 특정 프로젝트는 이후 단계에서 정해지므로 여기에 넣지 않는다. 주어진 성향에 따라 형태가 달라져야 한다.")
    val want: String,
    @get:JsonPropertyDescription("실제로 채워야 하는 내적 과제. 본인은 이것을 모른다. 특정 시점에 종속되지 않는다.")
    val need: String,
    @get:JsonPropertyDescription("지금 방식대로 Want를 쫓으면 Need가 어떻게 좌절되는지 한 문장.")
    @get:JsonProperty("want_need_conflict")
    val wantNeedConflict: String,
    val contradiction: CharacterContradiction,
    @get:JsonProperty("non_negotiables")
    val nonNegotiables: List<NonNegotiable>,
    @get:JsonProperty("forbidden_behaviors")
    val forbiddenBehaviors: List<ForbiddenBehavior>,
)

data class CharacterLie(
    @get:JsonPropertyDescription("입력으로 주어진 Lie 유형.")
    val type: String,
    @get:JsonPropertyDescription("이 인물이 참이라고 믿고 있는 잘못된 명제 한 문장. 주어진 유형의 일반 명제를 그대로 옮기지 말고, 이 인물의 경험을 통과한 형태로 쓴다.")
    val statement: String,
    @get:JsonPropertyDescription("이 믿음이 생겨난 과거 한 문장. 주어진 상처 조건(시기·장소·상처를 준 사람·형태·지속 시간)을 그대로 지킨다. 지속 시간이 길게 지정된 경우 단일 사건이 아니라 반복되며 쌓인 경험으로 쓴다.")
    val wound: String,
)

data class CharacterContradiction(
    @get:JsonPropertyDescription("관찰 가능한 행동. 어떤 상황에서 나타나는지 포함한다.")
    @get:JsonProperty("trait_a")
    val traitA: String,
    @get:JsonPropertyDescription("trait_a와 양립하기 어려운 다른 행동. 다른 상황에서 나타난다.")
    @get:JsonProperty("trait_b")
    val traitB: String,
    @get:JsonPropertyDescription("두 행동이 Lie라는 같은 뿌리에서 갈라져 나오는 경로. 내면과 외면을 대비시키는 요약이 아니라, 조건이 달라지면 다른 행동이 나오는 이유를 쓴다.")
    @get:JsonProperty("shared_root")
    val sharedRoot: String,
)

data class NonNegotiable(
    @get:JsonPropertyDescription("세 항목은 서로 다른 영역에서 나와야 한다.")
    val domain: NonNegotiableDomain,
    @get:JsonPropertyDescription("굽히지 않는 것. 행동 기준으로 쓴다.")
    val statement: String,
    @get:JsonPropertyDescription("Lie가 이것을 왜 지키게 만드는가.")
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
    @get:JsonPropertyDescription("이 인물이 하지 않는 행동. 최소 하나는 감정 표현에 관한 것이어야 한다.")
    val behavior: String,
    @get:JsonPropertyDescription("Lie가 이것을 왜 금지하는가.")
    @get:JsonProperty("tie_to_lie")
    val tieToLie: String,
)

data class CharacterTraits(
    @get:JsonPropertyDescription("주어진 성향 단계가 이 인물의 말과 행동에 어떻게 나타나는지 2~3문장. 3단계로 지정된 축은 언급하지 않는다.")
    @get:JsonProperty("expression_note")
    val expressionNote: String,
    @get:JsonPropertyDescription("주어진 성향이 Want의 형태와 두려움에 대응하는 방식에 어떤 제약을 주었는지 2~3문장. 반드시 '이 성향이 반대쪽이었다면 무엇이 달라졌을지'를 한 문장 포함한다. 주어진 값을 나열하기만 하면 안 된다.")
    @get:JsonProperty("identity_note")
    val identityNote: String,
)

data class CharacterBehavior(
    @get:JsonPropertyDescription("관계 태도는 상태가 아니라 시간에 따른 곡선이다. 네 시점이 서로 달라야 하며, 비슷하면 이 축은 아무것도 결정하지 못한 것이다.")
    @get:JsonProperty("attachment_note")
    val attachmentNote: AttachmentNote,
    @get:JsonPropertyDescription("주어진 정서 표출 방식이 이 인물에게서 어떻게 나타나는가. 기쁨·분노·불안이 각각 어떤 형태로 나오는지 관찰 가능하게 쓴다. 주어진 방식이 감정을 절제하는 쪽이 아니라면 절제된 인물로 만들지 마라.")
    @get:JsonProperty("emotional_expression_note")
    val emotionalExpressionNote: String,
    @get:JsonPropertyDescription("주어진 회피 방식이 이 인물에게서 어떻게 나타나는가. 말하고 싶지 않은 정보를 요구받았을 때의 반응이며, 감정 처리와는 다른 축이다. 불균형이 지정된 경우 그 설명이 여기에도 반영되어야 한다.")
    @get:JsonProperty("avoidance_note")
    val avoidanceNote: String,
)

data class AttachmentNote(
    @get:JsonPropertyDescription("관계가 얕을 때의 행동. 대화를 어떻게 시작하고 어디까지 여는가.")
    val early: String,
    @get:JsonPropertyDescription("가까워지는 과정에서 달라지는 것. 무엇이 새로 열리고 무엇이 여전히 닫혀 있는가.")
    val deepening: String,
    @get:JsonPropertyDescription("충분히 가까워졌을 때의 행동. 이 시점에도 관계 태도가 유지되는 방식이 드러나야 한다.")
    val close: String,
    @get:JsonPropertyDescription("도움이 거절되거나 상대가 밀어냈을 때의 반응. 물러나는가, 다시 시도하는가, 관계를 정리하는가.")
    @get:JsonProperty("on_rejection")
    val onRejection: String,
)

data class CharacterVoice(
    @get:JsonPropertyDescription("주어진 말투 원형이 이 인물에게 어떤 환경에서 몸에 뱄는지, 그래서 같은 원형의 다른 인물과 어떻게 다른지 1~2문장. 불균형이 지정된 경우 그 설명이 여기에도 반영되어야 한다.")
    @get:JsonProperty("speech_archetype_note")
    val speechArchetypeNote: String,
    @get:JsonPropertyDescription("문장 길이와 구조의 경향. 형용사가 아니라 관찰 가능한 형태로.")
    @get:JsonProperty("sentence_pattern")
    val sentencePattern: String,
    @get:JsonPropertyDescription("앞의 세 개는 프롬프트에서 지정된 상황에 대해, 네 번째는 이 인물에게 특히 중요한 상황을 스스로 정해 만든다.")
    @get:JsonProperty("verbal_tics")
    val verbalTics: List<VerbalTic>,
    @get:JsonProperty("banned_words")
//    val bannedWords: List<String>,
    val bannedWords: BannedWords,
    @get:JsonPropertyDescription("감정이 격해졌을 때 말과 행동이 어떻게 변하는지. 주어진 정서 표출 방식과 일치해야 한다.")
    @get:JsonProperty("under_stress")
    val underStress: String,
    @get:JsonPropertyDescription("사용자를 부르는 방식과 존댓말/반말 정책. 관계가 깊어졌을 때 어떻게 달라지는지 포함. 주어진 관계 태도의 곡선과 일치해야 한다.")
    @get:JsonProperty("address_style")
    val addressStyle: String,
)

data class VerbalTic(
    @get:JsonPropertyDescription("이 말버릇이 나타나는 상황.")
    val situation: String,
    @get:JsonPropertyDescription("그 상황에서 이 인물이 말하는 방식. 발화 예시를 그대로 적으면 그 문장만 반복되므로, '어떻게 말한다'는 서술로 쓴다.")
    val description: String,
)

data class BannedWords(
    @get:JsonPropertyDescription("Lie 때문에 쓸 수 없는 표현. 이 유형의 인물이라면 대체로 피할 만한 것.")
    val fromLie: List<String>,
    @get:JsonPropertyDescription("이 인물의 일, 자란 환경, 말투가 몸에 밴 자리에서만 나올 법한 표현. 다른 캐릭터와 겹치면 안 된다. 일반적인 과장 표현을 여기에 넣지 마라.")
    val fromBackground: List<String>
)

data class CharacterRelation(
    val slot: RelationSlot,
    val name: String,
    @get:JsonPropertyDescription("이 인물과의 관계.")
    val relation: String,
    @get:JsonPropertyDescription("한두 문장. 이 사람이 캐릭터의 어떤 면을 드러내는가.")
    val description: String,
    @get:JsonPropertyDescription("'과거' 슬롯은 원칙적으로 '부재'. 나머지 슬롯도 전부 현존일 필요는 없다.")
    val presence: RelationPresence,
    @get:JsonPropertyDescription("이 인물이 Lie와 어떻게 연결되는가. '과거' 슬롯은 상처가 일어난 자리에 있던 사람이며, 상처를 준 사람이 '특정인 없음'으로 지정된 경우 가해자가 아니라 그 자리에 함께 있었던 사람으로 채운다.")
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
    @get:JsonPropertyDescription("처음 만난 사람에게도 말하는 것.")
    val surface: List<String>,
    @get:JsonPropertyDescription("몇 번 대화한 사람에게 말하는 것.")
    val mid: List<String>,
    @get:JsonPropertyDescription("조건이 갖춰져야 말하는 것. 상처와 직접 연결된 과거, Lie의 내용.")
    val deep: List<String>,
    @get:JsonPropertyDescription("아직 누구에게도 말한 적 없는 단 하나. 앞 층위에서 공개된 사실을 뒤집거나 다시 보게 만드는 것이면 가장 좋다.")
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
    @get:JsonPropertyDescription("불균형 조합이 지정된 경우, 왜 이 인물에게서 두 축이 함께 나타나는지의 설명. 같은 행동이라도 목적이 다를 수 있다는 점을 활용한다. 이 설명은 voice와 behavior의 해당 필드에도 반영되어야 한다. 지정되지 않았으면 '없음'이라고 적는다.")
    @get:JsonProperty("imbalance_note")
    val imbalanceNote: String,
)
