package ltmp.charactergen.service

import ltmp.charactergen.api.axis.AxisConflictOptionDto
import ltmp.charactergen.api.axis.DomainOptionDto
import ltmp.charactergen.api.axis.L0CharacterGenerationOptionsDto
import ltmp.charactergen.api.axis.LieOptionDto
import ltmp.charactergen.api.axis.PersonalityTraitStageOptionDto
import ltmp.charactergen.api.axis.SpeechArchetypeOptionDto
import ltmp.charactergen.api.axis.SpeechRegisterOptionDto
import ltmp.charactergen.api.axis.TypeDescriptionOptionDto
import ltmp.charactergen.api.axis.WoundOptionDto
import ltmp.charactergen.api.axis.toDto
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.AxisConflictRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.data.repo.SpeechRegisterRepository
import ltmp.charactergen.data.repo.WoundAgentRepository
import ltmp.charactergen.data.repo.WoundDurationRepository
import ltmp.charactergen.data.repo.WoundFormRepository
import ltmp.charactergen.data.repo.WoundPlaceRepository
import ltmp.charactergen.data.repo.WoundTimingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class L0CharacterGenerationOptionsService(
    private val domainRepository: DomainRepository,
    private val lieRepository: LieRepository,
    private val woundTimingRepository: WoundTimingRepository,
    private val woundPlaceRepository: WoundPlaceRepository,
    private val woundAgentRepository: WoundAgentRepository,
    private val woundFormRepository: WoundFormRepository,
    private val woundDurationRepository: WoundDurationRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
    private val speechRegisterRepository: SpeechRegisterRepository,
    private val axisConflictRepository: AxisConflictRepository,
) {
    @Transactional(readOnly = true)
    fun getOptions() = L0CharacterGenerationOptionsDto(
        domains = domainRepository.findAllByOrderByNameAsc().map {
            DomainOptionDto(id = it.id, name = it.name, description = it.description)
        },
        lies = lieRepository.findAllByOrderByTypeAsc().map {
            LieOptionDto(id = it.id, type = it.type, coreStatement = it.coreStatement)
        },
        woundTimings = woundTimingRepository.findAllByOrderByIdAsc().map {
            WoundOptionDto(id = it.id, value = it.value, description = it.description)
        },
        woundPlaces = woundPlaceRepository.findAllByOrderByIdAsc().map {
            WoundOptionDto(id = it.id, value = it.value, description = it.description)
        },
        woundAgents = woundAgentRepository.findAllByOrderByIdAsc().map {
            WoundOptionDto(id = it.id, value = it.value, description = it.description)
        },
        woundForms = woundFormRepository.findAllByOrderByIdAsc().map {
            WoundOptionDto(id = it.id, value = it.value, description = it.description)
        },
        woundDurations = woundDurationRepository.findAllByOrderByIdAsc().map {
            WoundOptionDto(id = it.id, value = it.value, description = it.description)
        },
        personalityTraitStages = personalityTraitStageRepository.findAllByOrderByTraitAscStageAsc().map {
            PersonalityTraitStageOptionDto(id = it.id, trait = it.trait, stage = it.stage, description = it.description)
        },
        attachments = attachmentStyleRepository.findAllByOrderByTypeAsc().map {
            TypeDescriptionOptionDto(
                id = it.id,
                type = it.type,
                description = it.description,
                bigFiveVector = it.bigFiveVector.toDto(),
            )
        },
        emotionalExpressions = emotionalExpressionRepository.findAllByOrderByTypeAsc().map {
            TypeDescriptionOptionDto(
                id = it.id,
                type = it.type,
                description = it.description,
                bigFiveVector = it.bigFiveVector.toDto(),
            )
        },
        avoidanceStyles = avoidanceStyleRepository.findAllByOrderByTypeAsc().map {
            TypeDescriptionOptionDto(
                id = it.id,
                type = it.type,
                description = it.description,
                bigFiveVector = it.bigFiveVector.toDto(),
            )
        },
        speechArchetypes = speechArchetypeRepository.findAllByOrderByNameAsc().map {
            SpeechArchetypeOptionDto(
                id = it.id,
                name = it.name,
                description = it.description,
                bigFiveVector = it.bigFiveVector.toDto(),
            )
        },
        speechRegisters = speechRegisterRepository.findAllByOrderByIdAsc().map {
            SpeechRegisterOptionDto(id = it.id, name = it.name)
        },
        axisConflicts = axisConflictRepository.findAllWithAxisValues().map {
            AxisConflictOptionDto(
                emotionalExpressionId = it.emotionalExpression.id,
                emotionalExpressionType = it.emotionalExpression.type,
                avoidanceStyleId = it.avoidanceStyle.id,
                avoidanceStyleType = it.avoidanceStyle.type,
                reason = it.reason,
            )
        },
    )
}
