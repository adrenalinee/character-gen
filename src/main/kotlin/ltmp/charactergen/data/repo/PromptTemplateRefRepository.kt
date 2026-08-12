package ltmp.charactergen.data.repo

import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.PromptTemplateRef
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PromptTemplateRefRepository : JpaRepository<PromptTemplateRef, Long> {
    @Query(
        """
        select ref from PromptTemplateRef ref
        where (:promptTemplateId is null or ref.promptTemplate.id = :promptTemplateId)
          and (:type is null or ref.type = :type)
          and (:name is null or lower(ref.name) like lower(concat('%', :name, '%')))
          and (:version is null or ref.version = :version)
        """,
    )
    @EntityGraph(attributePaths = ["promptTemplate"])
    fun search(
        @Param("promptTemplateId") promptTemplateId: Long?,
        @Param("type") type: PromptTemplateRefType?,
        @Param("name") name: String?,
        @Param("version") version: Long?,
        pageable: Pageable,
    ): Page<PromptTemplateRef>

    @EntityGraph(attributePaths = ["promptTemplate", "inputs", "fromRef"])
    fun findOneWithDetailsById(id: Long): PromptTemplateRef?

    fun existsByPromptTemplateId(promptTemplateId: Long): Boolean

    fun existsByPromptTemplateIdAndName(promptTemplateId: Long, name: String): Boolean

    fun existsByPromptTemplateIdAndNameAndIdNot(promptTemplateId: Long, name: String, id: Long): Boolean

    fun existsByFromRefId(fromRefId: Long): Boolean
}
