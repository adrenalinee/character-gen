package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.BigFiveVector
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "speech_archetypes",
    uniqueConstraints = [UniqueConstraint(name = "uk_speech_archetypes_name", columnNames = ["name"])],
)
class SpeechArchetypeEntity(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 1000)
    var description: String,

    @Embedded
    var bigFiveVector: BigFiveVector,
) : LongBaseEntity()
