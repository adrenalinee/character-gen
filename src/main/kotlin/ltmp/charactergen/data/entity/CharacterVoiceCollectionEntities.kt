package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.BannedWordSource
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(name = "character_verbal_tics")
class CharacterVerbalTicEntity(
    @Lob
    @Column(nullable = false)
    val situation: String,

    @Lob
    @Column(nullable = false)
    val description: String,
) : LongBaseEntity()

@Entity
@Table(name = "character_banned_words")
class CharacterBannedWordEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: BannedWordSource,

    @Lob
    @Column(name = "word_text", nullable = false)
    val wordText: String,
) : LongBaseEntity()
