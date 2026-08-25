package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity
import ltmp.charactergen.data.PromptTemplateType

@Entity
@Table(
    name = "prompt_templates",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_prompt_templates_name", columnNames = ["name"]),
    ],
    indexes = [
        Index(name = "idx_prompt_templates_type", columnList = "type"),
    ],
)
class PromptTemplate(
    @Column(nullable = false, length = 200)
    var name: String,

    // 타입을 바꾸면 이 템플릿을 기본값으로 쓰던 용도가 조용히 깨지므로 생성 후 변경하지 않는다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: PromptTemplateType,

    @Column(name = "default_branch", length = 200)
    var defaultBranch: String? = null,

    // VERSION 번호를 삭제 후에도 재사용하지 않기 위한 템플릿별 발급 카운터다.
    @Column(name = "last_version", nullable = false)
    var lastVersion: Long = 0,
) : LongBaseEntity() {
    // 새 aggregate에서도 ref를 추가할 수 있도록 JPA 연관 컬렉션은 빈 컬렉션으로 초기화한다.
    @OneToMany(mappedBy = "promptTemplate")
    val refs: MutableList<PromptTemplateRef> = mutableListOf()
}
