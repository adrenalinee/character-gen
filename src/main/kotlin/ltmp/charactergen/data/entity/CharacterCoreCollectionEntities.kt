package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity
import ltmp.charactergen.model.NonNegotiableDomain

@Entity
@Table(name = "character_non_negotiables")
class CharacterNonNegotiableEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val domain: NonNegotiableDomain,

    @Lob
    @Column(nullable = false)
    val statement: String,

    @Lob
    @Column(name = "tie_to_lie", nullable = false)
    val tieToLie: String,

    @Column(name = "user_facing", nullable = false)
    val userFacing: Boolean,
) : LongBaseEntity()

@Entity
@Table(name = "character_forbidden_behaviors")
class CharacterForbiddenBehaviorEntity(
    @Lob
    @Column(nullable = false)
    val behavior: String,

    @Lob
    @Column(name = "tie_to_lie", nullable = false)
    val tieToLie: String,
) : LongBaseEntity()
