package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity
import org.hibernate.annotations.BatchSize

@Entity
@Table(
    name = "prompt_template_versions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_prompt_template_versions_template_name",
            columnNames = ["prompt_template_id", "name"],
        ),
    ],
)
class PromptTemplateVersion(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_template_id", nullable = false)
    var promptTemplate: PromptTemplate,

    @Column(nullable = false, length = 200)
    var name: String,

    @Lob
    @Column(nullable = false)
    var instructions: String,
) : LongBaseEntity() {
    // LLM 입력은 선언 순서가 의미를 가지므로 별도 순서 칼럼으로 보존한다.
    @OneToMany(mappedBy = "promptTemplateVersion", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderColumn(name = "input_order")
    @BatchSize(size = 50)
    val inputs: MutableList<PromptTemplateInput> = mutableListOf()

    fun addInput(role: String, inputText: String) {
        inputs.add(
            PromptTemplateInput(
                promptTemplateVersion = this,
                role = role,
                inputText = inputText,
            ),
        )
    }
}
