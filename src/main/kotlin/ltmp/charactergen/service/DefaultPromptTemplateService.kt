package ltmp.charactergen.service

import ltmp.charactergen.api.prompt.DefaultPromptTemplateDto
import ltmp.charactergen.api.prompt.UpdateDefaultPromptTemplateRequest
import ltmp.charactergen.api.prompt.toDto
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.DefaultPromptTemplate
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.DefaultPromptTemplateRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 용도별 기본 프롬프트 템플릿을 조회하고 변경한다.
 *
 * 프롬프트를 지정하지 않은 실행은 여기서 결정한 템플릿을 쓴다.
 * 지정한 실행은 [PromptTemplate.type]이 용도와 일치하는지만 검증하므로,
 * 같은 용도 안에서는 다른 프롬프트로 자유롭게 시험할 수 있다.
 */
@Service
class DefaultPromptTemplateService(
    private val defaultPromptTemplateRepository: DefaultPromptTemplateRepository,
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
) {
    @Transactional(readOnly = true)
    fun getDefaults(): List<DefaultPromptTemplateDto> =
        defaultPromptTemplateRepository.findAllByOrderByTypeAsc().map(DefaultPromptTemplate::toDto)

    @Transactional(readOnly = true)
    fun getDefault(type: PromptTemplateType): DefaultPromptTemplateDto = findDefault(type).toDto()

    @Transactional
    fun updateDefault(type: PromptTemplateType, request: UpdateDefaultPromptTemplateRequest): DefaultPromptTemplateDto {
        val template = promptTemplateRepository.findById(request.promptTemplateId).orElseThrow {
            ResourceNotFoundException("PromptTemplate not found: ${request.promptTemplateId}")
        }
        if (template.type != type) {
            throw InvalidRequestException(
                "PromptTemplate type mismatch. expected=$type, actual=${template.type}, templateId=${template.id}",
            )
        }
        val current = defaultPromptTemplateRepository.findOneWithTemplateByType(type)
        val updated = if (current == null) {
            defaultPromptTemplateRepository.save(DefaultPromptTemplate(type = type, promptTemplate = template))
        } else {
            current.promptTemplate = template
            current
        }
        defaultPromptTemplateRepository.flush()
        return updated.toDto()
    }

    /**
     * 해당 용도의 기본 프롬프트에서 실제로 실행할 ref를 찾는다.
     *
     * 기본 템플릿, 기본 BRANCH, BRANCH ref 중 하나라도 없으면 무엇이 없는지 알리고 실패한다.
     * 임의의 다른 프롬프트로 대체하지 않는다.
     */
    @Transactional(readOnly = true)
    fun resolveDefaultRef(type: PromptTemplateType): PromptTemplateRef {
        val template = findDefault(type).promptTemplate
        val branchName = template.defaultBranch
            ?: throw InvalidRequestException(
                "기본 프롬프트 템플릿에 기본 BRANCH가 없습니다. type=$type, templateId=${template.id}",
            )
        return promptTemplateRefRepository.findOneWithDetailsByPromptTemplateIdAndName(template.id, branchName)
            ?: throw ResourceNotFoundException(
                "PromptTemplateRef branch not found: templateId=${template.id}, branchName=$branchName",
            )
    }

    @Transactional(readOnly = true)
    fun resolveDefaultTemplate(type: PromptTemplateType): PromptTemplate = findDefault(type).promptTemplate

    private fun findDefault(type: PromptTemplateType): DefaultPromptTemplate =
        defaultPromptTemplateRepository.findOneWithTemplateByType(type)
            ?: throw ResourceNotFoundException("DefaultPromptTemplate not found: $type")
}
