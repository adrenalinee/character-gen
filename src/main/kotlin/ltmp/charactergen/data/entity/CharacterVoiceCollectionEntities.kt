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

    /**
     * 런타임 대화 프롬프트에 넣을 항목인지. (L0 캐릭터 생성 프롬프트 v4.2 B-13)
     *
     * 판정은 `RuntimeDuplicateSelector`가 캐릭터 저장 시점에 한 번 하고, 조립기는 이 값을 필터로만 쓴다.
     */
    @Column(name = "runtime_included", nullable = false)
    val runtimeIncluded: Boolean,
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
