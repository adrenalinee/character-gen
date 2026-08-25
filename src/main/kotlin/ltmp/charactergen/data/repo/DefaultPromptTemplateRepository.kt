package ltmp.charactergen.data.repo

import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.DefaultPromptTemplate
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface DefaultPromptTemplateRepository : JpaRepository<DefaultPromptTemplate, Long> {
    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findOneWithTemplateByType(type: PromptTemplateType): DefaultPromptTemplate?

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findAllByOrderByTypeAsc(): List<DefaultPromptTemplate>

    fun existsByPromptTemplateId(promptTemplateId: Long): Boolean
}
