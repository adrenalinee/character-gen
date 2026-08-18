package ltmp.charactergen.model

import com.fasterxml.jackson.annotation.JsonProperty

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
    val name: String,
    val age: Int,
    val gender: String,
    val occupation: String,
    @get:JsonProperty("daily_life")
    val dailyLife: String,
    @get:JsonProperty("one_line")
    val oneLine: String,
    @get:JsonProperty("concrete_details")
    val concreteDetails: List<String>,
)

data class CharacterCore(
    val lie: CharacterLie,
    val want: String,
    val need: String,
    @get:JsonProperty("want_need_conflict")
    val wantNeedConflict: String,
    val contradiction: CharacterContradiction,
    @get:JsonProperty("non_negotiables")
    val nonNegotiables: List<NonNegotiable>,
    @get:JsonProperty("forbidden_behaviors")
    val forbiddenBehaviors: List<ForbiddenBehavior>,
)

data class CharacterLie(
    val type: String,
    val statement: String,
    val wound: String,
)

data class CharacterContradiction(
    @get:JsonProperty("trait_a")
    val traitA: String,
    @get:JsonProperty("trait_b")
    val traitB: String,
    @get:JsonProperty("shared_root")
    val sharedRoot: String,
)

data class NonNegotiable(
    val domain: NonNegotiableDomain,
    val statement: String,
    @get:JsonProperty("tie_to_lie")
    val tieToLie: String,
    @get:JsonProperty("user_facing")
    val userFacing: Boolean,
)

enum class NonNegotiableDomain {
    `관계를 다루는 방식`,
    `세계를 보는 눈`,
    `일을 대하는 기준`,
}

data class ForbiddenBehavior(
    val behavior: String,
    @get:JsonProperty("tie_to_lie")
    val tieToLie: String,
)

data class CharacterTraits(
    @get:JsonProperty("expression_note")
    val expressionNote: String,
    @get:JsonProperty("identity_note")
    val identityNote: String,
)

data class CharacterBehavior(
    @get:JsonProperty("attachment_note")
    val attachmentNote: AttachmentNote,
    @get:JsonProperty("emotional_expression_note")
    val emotionalExpressionNote: String,
    @get:JsonProperty("avoidance_note")
    val avoidanceNote: String,
)

data class AttachmentNote(
    val early: String,
    val deepening: String,
    val close: String,
    @get:JsonProperty("on_rejection")
    val onRejection: String,
)

data class CharacterVoice(
    @get:JsonProperty("speech_archetype_note")
    val speechArchetypeNote: String,
    @get:JsonProperty("sentence_pattern")
    val sentencePattern: String,
    @get:JsonProperty("verbal_tics")
    val verbalTics: List<VerbalTic>,
    @get:JsonProperty("banned_words")
    val bannedWords: List<String>,
    @get:JsonProperty("under_stress")
    val underStress: String,
    @get:JsonProperty("address_style")
    val addressStyle: String,
)

data class VerbalTic(
    val situation: String,
    val description: String,
)

data class CharacterRelation(
    val slot: RelationSlot,
    val name: String,
    val relation: String,
    val description: String,
    val presence: RelationPresence,
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
    val surface: List<String>,
    val mid: List<String>,
    val deep: List<String>,
    val deepest: List<String>,
)

data class VoiceSample(
    val situation: String,
    val line: String,
    val demonstrates: String,
)

data class DesignNote(
    val recognizability: String,
    @get:JsonProperty("long_term_viability")
    val longTermViability: String,
    @get:JsonProperty("imbalance_note")
    val imbalanceNote: String,
)
