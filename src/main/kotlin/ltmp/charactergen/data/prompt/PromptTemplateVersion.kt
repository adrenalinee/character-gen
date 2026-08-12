package ltmp.charactergen.data.prompt

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

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
    var prompt: String,
) : LongBaseEntity()
