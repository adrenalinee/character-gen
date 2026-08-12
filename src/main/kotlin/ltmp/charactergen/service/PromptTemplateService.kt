package ltmp.charactergen.service

import ltmp.charactergen.api.prompt.CreatePromptTemplateRequest
import ltmp.charactergen.api.prompt.PromptTemplateDto
import ltmp.charactergen.api.prompt.PromptTemplateSimpleDto
import ltmp.charactergen.api.prompt.UpdatePromptTemplateRequest
import ltmp.charactergen.api.prompt.toDto
import ltmp.charactergen.api.prompt.toSimpleDto
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PromptTemplateService(
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
) {
    @Transactional(readOnly = true)
    fun searchTemplates(name: String?, pageable: Pageable): Page<PromptTemplateSimpleDto> {
        val templates = if (name == null) {
            promptTemplateRepository.findAll(pageable)
        } else {
            promptTemplateRepository.findByNameContainingIgnoreCase(name, pageable)
        }
        return templates.map(PromptTemplate::toSimpleDto)
    }

    @Transactional
    fun createTemplate(request: CreatePromptTemplateRequest): PromptTemplateDto {
        if (promptTemplateRepository.existsByName(request.name)) {
            throw ResourceConflictException("PromptTemplate name already exists: ${request.name}")
        }
        return promptTemplateRepository.saveAndFlush(PromptTemplate(name = request.name)).toDto()
    }

    @Transactional(readOnly = true)
    fun getTemplate(id: Long): PromptTemplateDto = findTemplate(id).toDto()

    @Transactional
    fun updateTemplate(id: Long, request: UpdatePromptTemplateRequest): PromptTemplateDto {
        val template = findTemplate(id)
        if (promptTemplateRepository.existsByNameAndIdNot(request.name, id)) {
            throw ResourceConflictException("PromptTemplate name already exists: ${request.name}")
        }
        template.name = request.name
        promptTemplateRepository.flush()
        return template.toDto()
    }

    @Transactional
    fun deleteTemplate(id: Long) {
        val template = findTemplate(id)
        if (promptTemplateRefRepository.existsByPromptTemplateId(id)) {
            throw ResourceConflictException("PromptTemplate with refs cannot be deleted: $id")
        }
        promptTemplateRepository.delete(template)
        promptTemplateRepository.flush()
    }

    private fun findTemplate(id: Long): PromptTemplate =
        promptTemplateRepository.findById(id).orElseThrow {
            ResourceNotFoundException("PromptTemplate not found: $id")
        }
}
