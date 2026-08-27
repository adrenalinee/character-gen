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

    /**
     * 런타임 대화 프롬프트에 넣을 항목인지. (L0 캐릭터 생성 프롬프트 v4.1 B-10)
     *
     * 다른 [대화] 필드와 내용이 겹치는 항목을 빼기 위해 캐릭터 저장 시점에 한 번 판정해서 저장한다.
     * 판정은 `RuntimeConcreteDetailSelector`가 하고, 조립기는 이 값을 필터로만 쓴다.
     */
    @Column(name = "runtime_included", nullable = false)
    val runtimeIncluded: Boolean,
) : LongBaseEntity()
