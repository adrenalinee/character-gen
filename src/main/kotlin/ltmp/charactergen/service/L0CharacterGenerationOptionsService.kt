package ltmp.charactergen.service

import ltmp.charactergen.api.axis.DomainOptionDto
import ltmp.charactergen.api.axis.L0CharacterGenerationOptionsDto
import ltmp.charactergen.api.axis.LieOptionDto
import ltmp.charactergen.api.axis.PersonalityTraitStageOptionDto
import ltmp.charactergen.api.axis.SpeechArchetypeOptionDto
import ltmp.charactergen.api.axis.SpeechRegisterOptionDto
import ltmp.charactergen.api.axis.TypeDescriptionOptionDto
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.data.repo.SpeechRegisterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class L0CharacterGenerationOptionsService(
    private val domainRepository: DomainRepository,
    private val lieRepository: LieRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
    private val speechRegisterRepository: SpeechRegisterRepository,
) {
    @Transactional(readOnly = true)
    fun getOptions() = L0CharacterGenerationOptionsDto(
        domains = domainRepository.findAllByOrderByNameAsc().map {
            DomainOptionDto(id = it.id, name = it.name, description = it.description)
        },
        lies = lieRepository.findAllByOrderByTypeAsc().map {
            LieOptionDto(id = it.id, type = it.type, coreStatement = it.coreStatement)
        },
        personalityTraitStages = personalityTraitStageRepository.findAllByOrderByTraitAscStageAsc().map {
            PersonalityTraitStageOptionDto(id = it.id, trait = it.trait, stage = it.stage, description = it.description)
        },
        attachments = attachmentStyleRepository.findAllByOrderByTypeAsc().map {
            TypeDescriptionOptionDto(id = it.id, type = it.type, description = it.description)
        },
        emotionalExpressions = emotionalExpressionRepository.findAllByOrderByTypeAsc().map {
            TypeDescriptionOptionDto(id = it.id, type = it.type, description = it.description)
        },
        avoidanceStyles = avoidanceStyleRepository.findAllByOrderByTypeAsc().map {
            TypeDescriptionOptionDto(id = it.id, type = it.type, description = it.description)
        },
        speechArchetypes = speechArchetypeRepository.findAllByOrderByNameAsc().map {
            SpeechArchetypeOptionDto(id = it.id, name = it.name, description = it.description)
        },
        speechRegisters = speechRegisterRepository.findAllByOrderByIdAsc().map {
            SpeechRegisterOptionDto(id = it.id, name = it.name)
        },
    )
}
