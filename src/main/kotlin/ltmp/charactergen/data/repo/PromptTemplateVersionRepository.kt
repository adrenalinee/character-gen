package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.PromptTemplateVersion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface PromptTemplateVersionRepository : JpaRepository<PromptTemplateVersion, Long> {
    @EntityGraph(attributePaths = ["promptTemplate"])
    override fun findAll(pageable: Pageable): Page<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findByPromptTemplateId(promptTemplateId: Long, pageable: Pageable): Page<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findByPromptTemplateIdAndNameContainingIgnoreCase(
        promptTemplateId: Long,
        name: String,
        pageable: Pageable,
    ): Page<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate", "inputs"])
    fun findOneWithPromptTemplateById(id: Long): PromptTemplateVersion?

    fun existsByPromptTemplateId(promptTemplateId: Long): Boolean

    fun existsByPromptTemplateIdAndName(promptTemplateId: Long, name: String): Boolean
}
