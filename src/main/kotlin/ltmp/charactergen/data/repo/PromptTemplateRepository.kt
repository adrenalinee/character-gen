package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.PromptTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import jakarta.persistence.LockModeType

interface PromptTemplateRepository : JpaRepository<PromptTemplate, Long> {
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<PromptTemplate>

    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(name: String, id: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLockedById(id: Long): PromptTemplate?
}
