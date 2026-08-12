package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "emotional_expressions",
    uniqueConstraints = [UniqueConstraint(name = "uk_emotional_expressions_type", columnNames = ["type"])],
)
class EmotionalExpressionEntity(
    @Column(nullable = false, length = 100)
    var type: String,

    @Column(nullable = false, length = 1000)
    var description: String,
) : LongBaseEntity()
