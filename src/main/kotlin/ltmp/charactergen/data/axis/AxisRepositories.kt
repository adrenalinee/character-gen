package ltmp.charactergen.data.axis

import org.springframework.data.jpa.repository.JpaRepository

interface DomainRepository : JpaRepository<DomainEntity, Long> {
    fun findAllByOrderByNameAsc(): List<DomainEntity>
}

interface LieRepository : JpaRepository<LieEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<LieEntity>
}

interface PersonalityTraitStageRepository : JpaRepository<PersonalityTraitStageEntity, Long> {
    fun findAllByOrderByTraitAscStageAsc(): List<PersonalityTraitStageEntity>
}

interface AttachmentStyleRepository : JpaRepository<AttachmentStyleEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<AttachmentStyleEntity>
}

interface EmotionalExpressionRepository : JpaRepository<EmotionalExpressionEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<EmotionalExpressionEntity>
}

interface AvoidanceStyleRepository : JpaRepository<AvoidanceStyleEntity, Long> {
    fun findAllByOrderByTypeAsc(): List<AvoidanceStyleEntity>
}

interface SpeechArchetypeRepository : JpaRepository<SpeechArchetypeEntity, Long> {
    fun findAllByOrderByNameAsc(): List<SpeechArchetypeEntity>
}
