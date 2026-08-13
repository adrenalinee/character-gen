package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "speech_registers",
    uniqueConstraints = [UniqueConstraint(name = "uk_speech_registers_name", columnNames = ["name"])],
)
class SpeechRegisterEntity(
    @Column(nullable = false, length = 100)
    var name: String,
) : LongBaseEntity()
