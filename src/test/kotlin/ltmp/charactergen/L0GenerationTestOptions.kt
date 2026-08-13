package ltmp.charactergen

import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.data.repo.SpeechRegisterRepository

data class L0GenerationTestOptions(
    val domainId: Long,
    val lieId: Long,
    val opennessStageId: Long,
    val conscientiousnessStageId: Long,
    val extraversionStageId: Long,
    val agreeablenessStageId: Long,
    val neuroticismStageId: Long,
    val attachmentStyleId: Long,
    val emotionalExpressionId: Long,
    val avoidanceStyleId: Long,
    val speechArchetypeId: Long,
    val speechRegisterId: Long,
)

fun l0GenerationTestOptions(
    domainRepository: DomainRepository,
    lieRepository: LieRepository,
    personalityTraitStageRepository: PersonalityTraitStageRepository,
    attachmentStyleRepository: AttachmentStyleRepository,
    emotionalExpressionRepository: EmotionalExpressionRepository,
    avoidanceStyleRepository: AvoidanceStyleRepository,
    speechArchetypeRepository: SpeechArchetypeRepository,
    speechRegisterRepository: SpeechRegisterRepository,
): L0GenerationTestOptions = L0GenerationTestOptions(
    domainId = domainRepository.findAll().single { it.name == "웹툰" }.id,
    lieId = requireNotNull(lieRepository.findByType("대체 가능성 공포형")).id,
    opennessStageId = requireNotNull(
        personalityTraitStageRepository.findByTraitAndStage(BigFiveTrait.OPENNESS, 3),
    ).id,
    conscientiousnessStageId = requireNotNull(
        personalityTraitStageRepository.findByTraitAndStage(BigFiveTrait.CONSCIENTIOUSNESS, 3),
    ).id,
    extraversionStageId = requireNotNull(
        personalityTraitStageRepository.findByTraitAndStage(BigFiveTrait.EXTRAVERSION, 3),
    ).id,
    agreeablenessStageId = requireNotNull(
        personalityTraitStageRepository.findByTraitAndStage(BigFiveTrait.AGREEABLENESS, 3),
    ).id,
    neuroticismStageId = requireNotNull(
        personalityTraitStageRepository.findByTraitAndStage(BigFiveTrait.NEUROTICISM, 3),
    ).id,
    attachmentStyleId = requireNotNull(attachmentStyleRepository.findByType("일정 거리 유지")).id,
    emotionalExpressionId = requireNotNull(emotionalExpressionRepository.findByType("사실 치환")).id,
    avoidanceStyleId = requireNotNull(avoidanceStyleRepository.findByType("짧게 답하고 침묵")).id,
    speechArchetypeId = requireNotNull(speechArchetypeRepository.findByName("비유형")).id,
    speechRegisterId = requireNotNull(speechRegisterRepository.findByName("존댓말 기본")).id,
)
