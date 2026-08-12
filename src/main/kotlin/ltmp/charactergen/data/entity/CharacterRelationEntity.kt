package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity
import ltmp.charactergen.model.RelationPresence
import ltmp.charactergen.model.RelationSlot

@Entity
@Table(name = "character_relations")
class CharacterRelationEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val slot: RelationSlot,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(nullable = false, length = 200)
    val relation: String,

    @Lob
    @Column(nullable = false)
    val description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val presence: RelationPresence,

    @Lob
    @Column(name = "tie_to_lie", nullable = false)
    val tieToLie: String,
) : LongBaseEntity()
