package ltmp.charactergen.data.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import ltmp.charactergen.data.LongBaseEntity
import ltmp.charactergen.data.PromptTemplateRefType
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Check

@Entity
@Table(
    name = "prompt_template_refs",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_prompt_template_refs_template_name", columnNames = ["prompt_template_id", "name"]),
        UniqueConstraint(name = "uk_prompt_template_refs_template_version", columnNames = ["prompt_template_id", "version"]),
    ],
    indexes = [
        Index(name = "idx_prompt_template_refs_template_type", columnList = "prompt_template_id,type"),
        Index(name = "idx_prompt_template_refs_from_ref_id", columnList = "from_ref_id"),
    ],
)
@Check(
    constraints = "(type = 'BRANCH' and name is not null and version is null) or " +
        "(type = 'VERSION' and name is null and version is not null and from_ref_id is not null)",
)
class PromptTemplateRef(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_template_id", nullable = false)
    var promptTemplate: PromptTemplate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: PromptTemplateRefType,

    @Column(length = 200)
    var name: String? = null,

    @Column
    val version: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_ref_id")
    val fromRef: PromptTemplateRef? = null,

    @Column(name = "from_ref_revision")
    val fromRefRevision: Long? = null,

    @Lob
    @Column(nullable = false)
    var instructions: String,
) : LongBaseEntity() {
    // JPA가 관리하는 낙관적 잠금 값이므로 0으로 초기화한다.
    @Version
    @Column(nullable = false)
    var revision: Long = 0

    @OneToMany(mappedBy = "promptTemplateRef", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderColumn(name = "input_order")
    @BatchSize(size = 50)
    val inputs: MutableList<PromptTemplateInput> = mutableListOf()

    fun replaceInputs(newInputs: List<Pair<String, String>>) {
        inputs.clear()
        newInputs.forEach { (role, inputText) -> addInput(role, inputText) }
    }

    fun addInput(role: String, inputText: String) {
        inputs.add(
            PromptTemplateInput(
                promptTemplateRef = this,
                role = role,
                inputText = inputText,
            ),
        )
    }
}
