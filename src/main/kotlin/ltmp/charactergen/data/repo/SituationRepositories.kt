package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.VerbalTicSituationEntity
import ltmp.charactergen.data.entity.VoiceSampleSituationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface VerbalTicSituationRepository : JpaRepository<VerbalTicSituationEntity, Long> {
    fun findAllByOrderByIdAsc(): List<VerbalTicSituationEntity>
}

interface VoiceSampleSituationRepository : JpaRepository<VoiceSampleSituationEntity, Long> {
    fun findAllByOrderByIdAsc(): List<VoiceSampleSituationEntity>
}
