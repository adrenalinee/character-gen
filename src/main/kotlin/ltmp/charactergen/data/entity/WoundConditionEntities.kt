package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "wound_timings",
    uniqueConstraints = [UniqueConstraint(name = "uk_wound_timings_value", columnNames = ["option_value"])],
)
class WoundTimingEntity(
    @Column(name = "option_value", nullable = false, length = 100)
    var value: String,

    @Column(nullable = true, length = 1000)
    var description: String? = null,
) : LongBaseEntity()

@Entity
@Table(
    name = "wound_places",
    uniqueConstraints = [UniqueConstraint(name = "uk_wound_places_value", columnNames = ["option_value"])],
)
class WoundPlaceEntity(
    @Column(name = "option_value", nullable = false, length = 100)
    var value: String,

    @Column(nullable = true, length = 1000)
    var description: String? = null,
) : LongBaseEntity()

@Entity
@Table(
    name = "wound_agents",
    uniqueConstraints = [UniqueConstraint(name = "uk_wound_agents_value", columnNames = ["option_value"])],
)
class WoundAgentEntity(
    @Column(name = "option_value", nullable = false, length = 100)
    var value: String,

    @Column(nullable = true, length = 1000)
    var description: String? = null,
) : LongBaseEntity()

@Entity
@Table(
    name = "wound_forms",
    uniqueConstraints = [UniqueConstraint(name = "uk_wound_forms_value", columnNames = ["option_value"])],
)
class WoundFormEntity(
    @Column(name = "option_value", nullable = false, length = 100)
    var value: String,

    @Column(nullable = true, length = 1000)
    var description: String? = null,
) : LongBaseEntity()

@Entity
@Table(
    name = "wound_durations",
    uniqueConstraints = [UniqueConstraint(name = "uk_wound_durations_value", columnNames = ["option_value"])],
)
class WoundDurationEntity(
    @Column(name = "option_value", nullable = false, length = 100)
    var value: String,

    @Column(nullable = true, length = 1000)
    var description: String? = null,
) : LongBaseEntity()
