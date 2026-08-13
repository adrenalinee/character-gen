package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "prompt_templates",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_prompt_templates_name", columnNames = ["name"]),
    ],
)
class PromptTemplate(
    @Column(nullable = false, length = 200)
    var name: String,

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
