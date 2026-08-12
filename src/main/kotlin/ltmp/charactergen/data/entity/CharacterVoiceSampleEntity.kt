package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(name = "character_voice_samples")
class CharacterVoiceSampleEntity(
    @Lob
    @Column(nullable = false)
    val situation: String,

    @Lob
    @Column(name = "sample_line", nullable = false)
    val line: String,

    @Lob
    @Column(nullable = false)
    val demonstrates: String,
) : LongBaseEntity()
