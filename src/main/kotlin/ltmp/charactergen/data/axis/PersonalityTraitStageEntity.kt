package ltmp.charactergen.data.axis

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "personality_trait_stages",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_personality_trait_stages_trait_stage",
            columnNames = ["trait", "stage"],
        ),
    ],
)
class PersonalityTraitStageEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var trait: BigFiveTrait,

    @Column(nullable = false)
    var stage: Int,

    @Column(nullable = false, length = 1000)
    var description: String,
) : LongBaseEntity() {
    init {
        require(stage in 1..5) { "Big Five stage must be between 1 and 5: $stage" }
    }
}
