package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "character_lies",
    uniqueConstraints = [UniqueConstraint(name = "uk_character_lies_type", columnNames = ["type"])],
)
class LieEntity(
    @Column(nullable = false, length = 100)
    var type: String,

    @Column(name = "core_statement", nullable = false, length = 1000)
    var coreStatement: String,
) : LongBaseEntity()
