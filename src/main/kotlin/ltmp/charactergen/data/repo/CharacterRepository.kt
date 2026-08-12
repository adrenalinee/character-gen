package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.Character
import org.springframework.data.jpa.repository.JpaRepository

interface CharacterRepository : JpaRepository<Character, Long> {
    fun existsByGenerationExecuteId(generationExecuteId: Long): Boolean
}
