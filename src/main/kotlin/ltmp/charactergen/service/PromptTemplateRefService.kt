package ltmp.charactergen.service

import ltmp.charactergen.api.prompt.CreatePromptTemplateBranchRequest
import ltmp.charactergen.api.prompt.CreatePromptTemplateVersionRefRequest
import ltmp.charactergen.api.prompt.PromptTemplateRefDto
import ltmp.charactergen.api.prompt.UpdatePromptTemplateBranchRequest
import ltmp.charactergen.api.prompt.toDto
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PromptTemplateRefService(
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val generationExecuteRepository: L0GenerationExecuteRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        promptTemplateId: Long?,
        type: PromptTemplateRefType?,
        name: String?,
        version: Long?,
        pageable: Pageable,
    ): Page<PromptTemplateRefDto> {
        if (promptTemplateId != null && !promptTemplateRepository.existsById(promptTemplateId)) {
            throw ResourceNotFoundException("PromptTemplate not found: $promptTemplateId")
        }
        return promptTemplateRefRepository.search(promptTemplateId, type, name, version, pageable)
            .map(PromptTemplateRef::toDto)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): PromptTemplateRefDto = findRef(id).toDto()

    @Transactional
    fun createBranch(request: CreatePromptTemplateBranchRequest): PromptTemplateRefDto {
        val template = findTemplate(request.promptTemplateId)
        ensureUniqueBranchName(template.id, request.name, null)
        val source = request.fromRefId?.let { findSource(template.id, it) }
        val branch = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = request.name,
            fromRef = source,
            fromRefRevision = source?.revision,
            instructions = source?.instructions ?: "",
        )
        source?.inputs?.forEach { branch.addInput(it.role, it.inputText) }
        return promptTemplateRefRepository.saveAndFlush(branch).toDto()
    }

    @Transactional
    fun createVersion(request: CreatePromptTemplateVersionRefRequest): PromptTemplateRefDto {
        val template = promptTemplateRepository.findLockedById(request.promptTemplateId)
            ?: throw ResourceNotFoundException("PromptTemplate not found: ${request.promptTemplateId}")
        val source = findSource(template.id, request.fromRefId)
        if (source.type != PromptTemplateRefType.BRANCH) {
            throw InvalidRequestException("VERSION can only be created from a BRANCH ref: ${source.id}")
        }
        validateExecutable(source)
        template.lastVersion += 1
        val version = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.VERSION,
            version = template.lastVersion,
            fromRef = source,
            fromRefRevision = source.revision,
            instructions = source.instructions,
        )
        source.inputs.forEach { version.addInput(it.role, it.inputText) }
        promptTemplateRepository.flush()
        return promptTemplateRefRepository.saveAndFlush(version).toDto()
    }

    @Transactional
    fun updateBranch(id: Long, request: UpdatePromptTemplateBranchRequest): PromptTemplateRefDto {
        val branch = findRef(id)
        if (branch.type != PromptTemplateRefType.BRANCH) {
            throw ResourceConflictException("VERSION ref cannot be modified: $id")
        }
        if (branch.revision != request.revision) {
            throw ResourceConflictException(
                "PromptTemplateRef revision conflict. expected=${branch.revision}, actual=${request.revision}",
            )
        }
        ensureUniqueBranchName(branch.promptTemplate.id, request.name, branch.id)
        branch.name = request.name
        branch.instructions = request.instructions
        branch.replaceInputs(request.inputs.map { it.role to it.inputText })
        promptTemplateRefRepository.flush()
        return branch.toDto()
    }

    @Transactional
    fun delete(id: Long) {
        val ref = findRef(id)
        if (promptTemplateRefRepository.existsByFromRefId(id)) {
            throw ResourceConflictException("PromptTemplateRef is referenced by another ref: $id")
        }
        if (generationExecuteRepository.existsByPromptTemplateRefId(id)) {
            throw ResourceConflictException("PromptTemplateRef is referenced by a generation execute: $id")
        }
        promptTemplateRefRepository.delete(ref)
        promptTemplateRefRepository.flush()
    }

    fun validateExecutable(ref: PromptTemplateRef) {
        if (ref.instructions.isBlank()) {
            throw InvalidRequestException("PromptTemplateRef instructions must not be blank when publishing or executing")
        }
        if (ref.inputs.isEmpty()) {
            throw InvalidRequestException("PromptTemplateRef must contain at least one input when publishing or executing")
        }
        if (ref.inputs.any { it.role.isBlank() || it.inputText.isBlank() }) {
            throw InvalidRequestException("PromptTemplateRef role and inputText must not be blank when publishing or executing")
        }
    }

    @Transactional(readOnly = true)
    fun findRefEntity(id: Long): PromptTemplateRef = findRef(id)

    private fun findSource(promptTemplateId: Long, refId: Long): PromptTemplateRef {
        val source = findRef(refId)
        if (source.promptTemplate.id != promptTemplateId) {
            throw InvalidRequestException("fromRefId must belong to PromptTemplate $promptTemplateId: $refId")
        }
        return source
    }

    private fun ensureUniqueBranchName(promptTemplateId: Long, name: String, excludedId: Long?) {
        val exists = if (excludedId == null) {
            promptTemplateRefRepository.existsByPromptTemplateIdAndName(promptTemplateId, name)
        } else {
            promptTemplateRefRepository.existsByPromptTemplateIdAndNameAndIdNot(promptTemplateId, name, excludedId)
        }
        if (exists) throw ResourceConflictException("BRANCH name already exists in template $promptTemplateId: $name")
    }

    private fun findTemplate(id: Long): PromptTemplate = promptTemplateRepository.findById(id).orElseThrow {
        ResourceNotFoundException("PromptTemplate not found: $id")
    }

    private fun findRef(id: Long): PromptTemplateRef =
        promptTemplateRefRepository.findOneWithDetailsById(id)
            ?: throw ResourceNotFoundException("PromptTemplateRef not found: $id")
}
