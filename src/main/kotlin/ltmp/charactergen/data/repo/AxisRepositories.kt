package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.AttachmentStyleEntity
import ltmp.charactergen.data.entity.AvoidanceStyleEntity
import ltmp.charactergen.data.entity.AxisConflictEntity
import ltmp.charactergen.data.entity.DomainEntity
import ltmp.charactergen.data.entity.EmotionalExpressionEntity
import ltmp.charactergen.data.entity.LieEntity
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.entity.SpeechArchetypeEntity
import ltmp.charactergen.data.entity.SpeechRegisterEntity
import ltmp.charactergen.data.entity.WoundAgentEntity
import ltmp.charactergen.data.entity.WoundDurationEntity
import ltmp.charactergen.data.entity.WoundFormEntity
import ltmp.charactergen.data.entity.WoundPlaceEntity
import ltmp.charactergen.data.entity.WoundTimingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DomainRepository : JpaRepository<DomainEntity, Long> {
    fun findAllByOrderByNameAsc(): List<DomainEntity>
}

interface LieRepository : JpaRepository<LieEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<LieEntity>

    fun findByType(type: String): LieEntity?
}

interface WoundTimingRepository : JpaRepository<WoundTimingEntity, Long> {
    fun findAllByOrderByIdAsc(): List<WoundTimingEntity>
}

interface WoundPlaceRepository : JpaRepository<WoundPlaceEntity, Long> {
    fun findAllByOrderByIdAsc(): List<WoundPlaceEntity>
}

interface WoundAgentRepository : JpaRepository<WoundAgentEntity, Long> {
    fun findAllByOrderByIdAsc(): List<WoundAgentEntity>
}

interface WoundFormRepository : JpaRepository<WoundFormEntity, Long> {
    fun findAllByOrderByIdAsc(): List<WoundFormEntity>
}

interface WoundDurationRepository : JpaRepository<WoundDurationEntity, Long> {
    fun findAllByOrderByIdAsc(): List<WoundDurationEntity>
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

interface AxisConflictRepository : JpaRepository<AxisConflictEntity, Long> {
    @Query(
        """
        select conflict from AxisConflictEntity conflict
        join fetch conflict.emotionalExpression
        join fetch conflict.avoidanceStyle
        order by conflict.id asc
        """,
    )
    fun findAllWithAxisValues(): List<AxisConflictEntity>

    fun findByEmotionalExpressionIdAndAvoidanceStyleId(
        emotionalExpressionId: Long,
        avoidanceStyleId: Long,
    ): AxisConflictEntity?
}

interface SpeechRegisterRepository : JpaRepository<SpeechRegisterEntity, Long> {
    fun findAllByOrderByIdAsc(): List<SpeechRegisterEntity>

    fun findByName(name: String): SpeechRegisterEntity?
}
