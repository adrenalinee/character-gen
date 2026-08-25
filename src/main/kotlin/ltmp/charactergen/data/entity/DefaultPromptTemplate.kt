package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity
import ltmp.charactergen.data.PromptTemplateType

/**
 * 용도별로 기본 선택될 프롬프트 템플릿이다.
 *
 * 타입당 한 행만 존재한다. 실제로 실행할 ref는 [PromptTemplate.defaultBranch]가 결정하므로
 * 여기서 ref를 다시 가리키지 않는다.
 */
@Entity
@Table(
    name = "default_prompt_templates",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_default_prompt_templates_type", columnNames = ["type"]),
    ],
)
class DefaultPromptTemplate(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: PromptTemplateType,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_template_id", nullable = false)
    var promptTemplate: PromptTemplate,
) : LongBaseEntity()
