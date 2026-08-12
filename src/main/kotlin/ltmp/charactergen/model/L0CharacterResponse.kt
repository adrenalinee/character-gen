package ltmp.charactergen.model

import com.fasterxml.jackson.annotation.JsonProperty

data class L0CharacterResponse(
    val identity: CharacterIdentity,
    val core: CharacterCore,
    val traits: CharacterTraits,
    val behavior: CharacterBehavior,
    val voice: CharacterVoice,
    val relations: List<CharacterRelation>,
    @param:JsonProperty("disclosure_ladder")
    val disclosureLadder: DisclosureLadder,
    @param:JsonProperty("voice_samples")
    val voiceSamples: List<VoiceSample>,
    @param:JsonProperty("design_note")
    val designNote: DesignNote,
)

data class CharacterIdentity(
    val name: String,
    val age: Int,
    val gender: String,
    val occupation: String,
    @param:JsonProperty("daily_life")
    val dailyLife: String,
    @param:JsonProperty("one_line")
    val oneLine: String,
    @param:JsonProperty("concrete_details")
    val concreteDetails: List<String>,
)

data class CharacterCore(
    val lie: CharacterLie,
    val want: String,
    val need: String,
    @param:JsonProperty("want_need_conflict")
    val wantNeedConflict: String,
    val contradiction: CharacterContradiction,
    @param:JsonProperty("non_negotiables")
    val nonNegotiables: List<NonNegotiable>,
    @param:JsonProperty("forbidden_behaviors")
    val forbiddenBehaviors: List<ForbiddenBehavior>,
)

data class CharacterLie(
    val type: String,
    val statement: String,
    val wound: String,
)

data class CharacterContradiction(
    @param:JsonProperty("trait_a")
    val traitA: String,
    @param:JsonProperty("trait_b")
    val traitB: String,
    @param:JsonProperty("shared_root")
    val sharedRoot: String,
)

data class NonNegotiable(
    val domain: NonNegotiableDomain,
    val statement: String,
    @param:JsonProperty("tie_to_lie")
    val tieToLie: String,
    @param:JsonProperty("user_facing")
    val userFacing: Boolean,
)

enum class NonNegotiableDomain {
    @JsonProperty("관계를 다루는 방식")
    RELATIONSHIP,

    @JsonProperty("세계를 보는 눈")
    WORLD_VIEW,

    @JsonProperty("일을 대하는 기준")
    WORK_STANDARD,
}

data class ForbiddenBehavior(
    val behavior: String,
    @param:JsonProperty("tie_to_lie")
    val tieToLie: String,
)

data class CharacterTraits(
    @param:JsonProperty("expression_note")
    val expressionNote: String,
    @param:JsonProperty("identity_note")
    val identityNote: String,
)

data class CharacterBehavior(
    @param:JsonProperty("attachment_note")
    val attachmentNote: String,
    @param:JsonProperty("emotional_expression_note")
    val emotionalExpressionNote: String,
    @param:JsonProperty("avoidance_note")
    val avoidanceNote: String,
)

data class CharacterVoice(
    @param:JsonProperty("speech_archetype_note")
    val speechArchetypeNote: String,
    @param:JsonProperty("sentence_pattern")
    val sentencePattern: String,
    @param:JsonProperty("verbal_tics")
    val verbalTics: List<VerbalTic>,
    @param:JsonProperty("banned_words")
    val bannedWords: List<String>,
    @param:JsonProperty("under_stress")
    val underStress: String,
    @param:JsonProperty("address_style")
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
    @param:JsonProperty("tie_to_lie")
    val tieToLie: String,
)

enum class RelationSlot {
    @JsonProperty("조력자")
    ALLY,

    @JsonProperty("마찰자")
    FRICTION,

    @JsonProperty("거울")
    MIRROR,

    @JsonProperty("권위")
    AUTHORITY,

    @JsonProperty("과거")
    PAST,
}

enum class RelationPresence {
    @JsonProperty("현존")
    PRESENT,

    @JsonProperty("부재")
    ABSENT,
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
    @param:JsonProperty("long_term_viability")
    val longTermViability: String,
    @param:JsonProperty("imbalance_note")
    val imbalanceNote: String,
)
