package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.AttachmentStyleEntity
import ltmp.charactergen.data.entity.AvoidanceStyleEntity
import ltmp.charactergen.data.entity.DomainEntity
import ltmp.charactergen.data.entity.EmotionalExpressionEntity
import ltmp.charactergen.data.entity.LieEntity
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.entity.SpeechArchetypeEntity
import ltmp.charactergen.data.entity.SpeechRegisterEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DomainRepository : JpaRepository<DomainEntity, Long> {
    fun findAllByOrderByNameAsc(): List<DomainEntity>
}

interface LieRepository : JpaRepository<LieEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<LieEntity>

    fun findByType(type: String): LieEntity?
}

interface PersonalityTraitStageRepository : JpaRepository<PersonalityTraitStageEntity, Long> {
    fun findAllByOrderByTraitAscStageAsc(): List<PersonalityTraitStageEntity>

    fun findByTraitAndStage(trait: ltmp.charactergen.data.BigFiveTrait, stage: Int): PersonalityTraitStageEntity?
}

interface AttachmentStyleRepository : JpaRepository<AttachmentStyleEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<AttachmentStyleEntity>

    fun findByType(type: String): AttachmentStyleEntity?
}

interface EmotionalExpressionRepository : JpaRepository<EmotionalExpressionEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<EmotionalExpressionEntity>

    fun findByType(type: String): EmotionalExpressionEntity?
}

interface AvoidanceStyleRepository : JpaRepository<AvoidanceStyleEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<AvoidanceStyleEntity>

    fun findByType(type: String): AvoidanceStyleEntity?
}

interface SpeechArchetypeRepository : JpaRepository<SpeechArchetypeEntity, Long> {
    fun findAllByOrderByNameAsc(): List<SpeechArchetypeEntity>

    fun findByName(name: String): SpeechArchetypeEntity?
}

interface SpeechRegisterRepository : JpaRepository<SpeechRegisterEntity, Long> {
    fun findAllByOrderByIdAsc(): List<SpeechRegisterEntity>

    fun findByName(name: String): SpeechRegisterEntity?
}
