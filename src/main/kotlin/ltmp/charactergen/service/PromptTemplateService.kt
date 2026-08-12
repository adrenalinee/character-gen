package ltmp.charactergen.service

import ltmp.charactergen.api.prompt.CreatePromptTemplateRequest
import ltmp.charactergen.api.prompt.CreatePromptTemplateVersionRequest
import ltmp.charactergen.api.prompt.PromptTemplateDto
import ltmp.charactergen.api.prompt.PromptTemplateVersionDto
import ltmp.charactergen.api.prompt.UpdatePromptTemplateRequest
import ltmp.charactergen.api.prompt.toDto
import ltmp.charactergen.data.prompt.PromptTemplate
import ltmp.charactergen.data.prompt.PromptTemplateRepository
import ltmp.charactergen.data.prompt.PromptTemplateVersion
import ltmp.charactergen.data.prompt.PromptTemplateVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PromptTemplateService(
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateVersionRepository: PromptTemplateVersionRepository,
) {
    @Transactional(readOnly = true)
    fun searchTemplates(name: String?): List<PromptTemplateDto> {
        val templates = if (name == null) {
            promptTemplateRepository.findAllByOrderByNameAsc()
        } else {
            promptTemplateRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name)
        }
        return templates.map(PromptTemplate::toDto)
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
        if (promptTemplateVersionRepository.existsByPromptTemplateId(id)) {
            throw ResourceConflictException("PromptTemplate with versions cannot be deleted: $id")
        }
        promptTemplateRepository.delete(template)
        promptTemplateRepository.flush()
    }

    @Transactional(readOnly = true)
    fun searchVersions(promptTemplateId: Long?, name: String?): List<PromptTemplateVersionDto> {
        if (promptTemplateId != null && !promptTemplateRepository.existsById(promptTemplateId)) {
            throw ResourceNotFoundException("PromptTemplate not found: $promptTemplateId")
        }
        val versions = when {
            promptTemplateId != null && name != null ->
                promptTemplateVersionRepository.findByPromptTemplateIdAndNameContainingIgnoreCaseOrderByIdDesc(
                    promptTemplateId,
                    name,
                )
            promptTemplateId != null -> promptTemplateVersionRepository.findByPromptTemplateIdOrderByIdDesc(promptTemplateId)
            name != null -> promptTemplateVersionRepository.findByNameContainingIgnoreCaseOrderByIdDesc(name)
            else -> promptTemplateVersionRepository.findAllByOrderByIdDesc()
        }
        return versions.map(PromptTemplateVersion::toDto)
    }

    @Transactional
    fun createVersion(request: CreatePromptTemplateVersionRequest): PromptTemplateVersionDto {
        val template = findTemplate(request.promptTemplateId)
        if (promptTemplateVersionRepository.existsByPromptTemplateIdAndName(template.id, request.name)) {
            throw ResourceConflictException(
                "PromptTemplateVersion name already exists in template ${template.id}: ${request.name}",
            )
        }
        val version = PromptTemplateVersion(
            promptTemplate = template,
            name = request.name,
            prompt = request.prompt,
        )
        return promptTemplateVersionRepository.saveAndFlush(version).toDto()
    }

    @Transactional(readOnly = true)
    fun getVersion(id: Long): PromptTemplateVersionDto =
        promptTemplateVersionRepository.findOneWithPromptTemplateById(id)?.toDto()
            ?: throw ResourceNotFoundException("PromptTemplateVersion not found: $id")

    private fun findTemplate(id: Long): PromptTemplate =
        promptTemplateRepository.findById(id).orElseThrow {
            ResourceNotFoundException("PromptTemplate not found: $id")
        }
}
