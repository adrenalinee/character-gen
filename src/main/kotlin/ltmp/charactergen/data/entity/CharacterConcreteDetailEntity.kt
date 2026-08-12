package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(name = "character_concrete_details")
class CharacterConcreteDetailEntity(
    @Lob
    @Column(name = "detail_text", nullable = false)
    val detailText: String,
) : LongBaseEntity()
