package ltmp.charactergen.data.prompt

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface PromptTemplateVersionRepository : JpaRepository<PromptTemplateVersion, Long> {
    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findAllByOrderByIdDesc(): List<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findByNameContainingIgnoreCaseOrderByIdDesc(name: String): List<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findByPromptTemplateIdOrderByIdDesc(promptTemplateId: Long): List<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findByPromptTemplateIdAndNameContainingIgnoreCaseOrderByIdDesc(
        promptTemplateId: Long,
        name: String,
    ): List<PromptTemplateVersion>

    @EntityGraph(attributePaths = ["promptTemplate"])
    fun findOneWithPromptTemplateById(id: Long): PromptTemplateVersion?

    fun existsByPromptTemplateId(promptTemplateId: Long): Boolean

    fun existsByPromptTemplateIdAndName(promptTemplateId: Long, name: String): Boolean
}
