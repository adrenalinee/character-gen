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

    /**
     * 런타임 대화 프롬프트에 넣을 항목인지. (L0 캐릭터 생성 프롬프트 v4.2 B-13)
     *
     * 이 층위가 열렸더라도 더 우선순위가 높은 [대화] 필드와 내용이 겹치면 false가 된다.
     * 판정은 `RuntimeDuplicateSelector`가 캐릭터 저장 시점에 한 번 하고, 조립기는 이 값을 필터로만 쓴다.
     */
    @Column(name = "runtime_included", nullable = false)
    val runtimeIncluded: Boolean,
) : LongBaseEntity()
