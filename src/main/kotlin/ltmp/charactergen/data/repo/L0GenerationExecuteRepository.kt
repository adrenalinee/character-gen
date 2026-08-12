package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.L0GenerationExecute
import org.springframework.data.jpa.repository.JpaRepository

interface L0GenerationExecuteRepository : JpaRepository<L0GenerationExecute, Long> {
    fun existsByPromptTemplateRefId(promptTemplateRefId: Long): Boolean
}
