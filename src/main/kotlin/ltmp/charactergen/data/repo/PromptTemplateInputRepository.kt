package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.PromptTemplateInput
import org.springframework.data.jpa.repository.JpaRepository

interface PromptTemplateInputRepository : JpaRepository<PromptTemplateInput, Long>
