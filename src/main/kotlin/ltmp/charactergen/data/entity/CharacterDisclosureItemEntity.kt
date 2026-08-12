package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(name = "character_disclosure_items")
class CharacterDisclosureItemEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val level: DisclosureLevel,

    @Lob
    @Column(name = "item_text", nullable = false)
    val itemText: String,
) : LongBaseEntity()
