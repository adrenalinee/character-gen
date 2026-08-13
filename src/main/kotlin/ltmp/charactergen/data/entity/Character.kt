package ltmp.charactergen.data.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity
import org.hibernate.annotations.BatchSize

@Entity
@Table(name = "characters")
class Character(
    @OneToOne(mappedBy = "character", fetch = FetchType.LAZY, optional = false)
    val generationExecute: L0GenerationExecute,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(nullable = false)
    val age: Int,

    @Column(nullable = false, length = 100)
    val gender: String,

    @Column(nullable = false, length = 200)
    val occupation: String,

    @Lob
    @Column(name = "daily_life", nullable = false)
    val dailyLife: String,

    @Lob
    @Column(name = "one_line", nullable = false)
    val oneLine: String,

    @Column(name = "lie_type", nullable = false, length = 200)
    val lieType: String,

    @Lob
    @Column(name = "lie_statement", nullable = false)
    val lieStatement: String,

    @Lob
    @Column(name = "lie_wound", nullable = false)
    val lieWound: String,

    @Lob
    @Column(nullable = false)
    val want: String,

    @Lob
    @Column(nullable = false)
    val need: String,

    @Lob
    @Column(name = "want_need_conflict", nullable = false)
    val wantNeedConflict: String,

    @Lob
    @Column(name = "contradiction_trait_a", nullable = false)
    val contradictionTraitA: String,

    @Lob
    @Column(name = "contradiction_trait_b", nullable = false)
    val contradictionTraitB: String,

    @Lob
    @Column(name = "contradiction_shared_root", nullable = false)
    val contradictionSharedRoot: String,

    @Lob
    @Column(name = "expression_note", nullable = false)
    val expressionNote: String,

    @Lob
    @Column(name = "identity_note", nullable = false)
    val identityNote: String,

    @Lob
    @Column(name = "attachment_note", nullable = false)
    val attachmentNote: String,

    @Lob
    @Column(name = "emotional_expression_note", nullable = false)
    val emotionalExpressionNote: String,

    @Lob
    @Column(name = "avoidance_note", nullable = false)
    val avoidanceNote: String,

    @Lob
    @Column(name = "speech_archetype_note", nullable = false)
    val speechArchetypeNote: String,

    @Lob
    @Column(name = "sentence_pattern", nullable = false)
    val sentencePattern: String,

    @Lob
    @Column(name = "under_stress", nullable = false)
    val underStress: String,

    @Lob
    @Column(name = "address_style", nullable = false)
    val addressStyle: String,

    @Lob
    @Column(nullable = false)
    val recognizability: String,

    @Lob
    @Column(name = "long_term_viability", nullable = false)
    val longTermViability: String,

    @Lob
    @Column(name = "imbalance_note", nullable = false)
    val imbalanceNote: String,

    concreteDetails: List<CharacterConcreteDetailEntity>,
    nonNegotiables: List<CharacterNonNegotiableEntity>,
    forbiddenBehaviors: List<CharacterForbiddenBehaviorEntity>,
    verbalTics: List<CharacterVerbalTicEntity>,
    bannedWords: List<CharacterBannedWordEntity>,
    relations: List<CharacterRelationEntity>,
    disclosureItems: List<CharacterDisclosureItemEntity>,
    voiceSamples: List<CharacterVoiceSampleEntity>,
) : LongBaseEntity() {
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "detail_order")
    @BatchSize(size = 50)
    val concreteDetails: MutableList<CharacterConcreteDetailEntity> = concreteDetails.toMutableList()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "non_negotiable_order")
    @BatchSize(size = 50)
    val nonNegotiables: MutableList<CharacterNonNegotiableEntity> = nonNegotiables.toMutableList()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "forbidden_behavior_order")
    @BatchSize(size = 50)
    val forbiddenBehaviors: MutableList<CharacterForbiddenBehaviorEntity> = forbiddenBehaviors.toMutableList()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "tic_order")
    @BatchSize(size = 50)
    val verbalTics: MutableList<CharacterVerbalTicEntity> = verbalTics.toMutableList()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "word_order")
    @BatchSize(size = 50)
    val bannedWords: MutableList<CharacterBannedWordEntity> = bannedWords.toMutableList()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "relation_order")
    @BatchSize(size = 50)
    val relations: MutableList<CharacterRelationEntity> = relations.toMutableList()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "disclosure_order")
    @BatchSize(size = 50)
    val disclosureItems: MutableList<CharacterDisclosureItemEntity> = disclosureItems.toMutableList()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "character_id", nullable = false)
    @OrderColumn(name = "sample_order")
    @BatchSize(size = 50)
    val voiceSamples: MutableList<CharacterVoiceSampleEntity> = voiceSamples.toMutableList()
}
