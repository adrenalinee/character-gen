package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.data.L0GenerationExecuteStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock

interface L0GenerationExecuteRepository : JpaRepository<L0GenerationExecute, Long> {
    @Query(
        """
        select execute
        from L0GenerationExecute execute
        where (:refId is null or execute.promptTemplateRef.id = :refId)
          and (:status is null or execute.status = :status)
        """,
    )
    fun search(
        @Param("refId") refId: Long?,
        @Param("status") status: L0GenerationExecuteStatus?,
        pageable: Pageable,
    ): Page<L0GenerationExecute>

    fun existsByPromptTemplateRefId(promptTemplateRefId: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findFirstByStatusOrderByCreatedAtAsc(status: L0GenerationExecuteStatus): L0GenerationExecute?

    @EntityGraph(attributePaths = ["promptTemplateRef", "promptTemplateRef.inputs"])
    fun findOneWithPromptById(id: Long): L0GenerationExecute?
}
