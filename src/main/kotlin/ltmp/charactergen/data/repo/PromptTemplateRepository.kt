package ltmp.charactergen.data.repo

import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.PromptTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType

interface PromptTemplateRepository : JpaRepository<PromptTemplate, Long> {
    fun findByName(name: String): PromptTemplate?

    @Query(
        """
        select template from PromptTemplate template
        where (:name is null or lower(template.name) like lower(concat('%', :name, '%')))
          and (:type is null or template.type = :type)
        """,
    )
    fun search(
        @Param("name") name: String?,
        @Param("type") type: PromptTemplateType?,
        pageable: Pageable,
    ): Page<PromptTemplate>

    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(name: String, id: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLockedById(id: Long): PromptTemplate?
}
