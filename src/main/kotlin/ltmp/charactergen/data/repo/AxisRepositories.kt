package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.AttachmentStyleEntity
import ltmp.charactergen.data.entity.AvoidanceStyleEntity
import ltmp.charactergen.data.entity.DomainEntity
import ltmp.charactergen.data.entity.EmotionalExpressionEntity
import ltmp.charactergen.data.entity.LieEntity
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.entity.SpeechArchetypeEntity
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
