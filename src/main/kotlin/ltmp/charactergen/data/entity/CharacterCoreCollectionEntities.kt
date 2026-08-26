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

/**
 * Lie가 평상시에 만들어내는 조건부 행동. `lie.statement`는 대화에 들어가지 않고 이 규칙만 들어간다.
 */
@Entity
@Table(name = "character_behavioral_rules")
class CharacterBehavioralRuleEntity(
    @Lob
    @Column(name = "rule_text", nullable = false)
    val ruleText: String,
) : LongBaseEntity()
