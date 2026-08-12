package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(name = "prompt_template_inputs")
class PromptTemplateInput(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_template_ref_id", nullable = false)
    var promptTemplateRef: PromptTemplateRef,

    @Column(nullable = false, length = 50)
    var role: String,

    @Lob
    @Column(name = "input_text", nullable = false)
    var inputText: String,
) : LongBaseEntity()
