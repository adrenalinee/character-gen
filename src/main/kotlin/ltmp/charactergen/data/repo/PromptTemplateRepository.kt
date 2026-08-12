package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.PromptTemplate
import org.springframework.data.jpa.repository.JpaRepository

interface PromptTemplateRepository : JpaRepository<PromptTemplate, Long> {
    fun findAllByOrderByNameAsc(): List<PromptTemplate>

    fun findByNameContainingIgnoreCaseOrderByNameAsc(name: String): List<PromptTemplate>

    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(name: String, id: Long): Boolean
}
