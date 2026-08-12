package ltmp.charactergen.data.prompt

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
) : LongBaseEntity() {
    // 새 aggregate에서도 버전을 추가할 수 있도록 JPA 연관 컬렉션은 빈 컬렉션으로 초기화한다.
    @OneToMany(mappedBy = "promptTemplate")
    val versions: MutableList<PromptTemplateVersion> = mutableListOf()
}
