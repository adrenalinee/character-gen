package ltmp.charactergen.service

import ltmp.charactergen.api.character.CharacterDto
import ltmp.charactergen.api.character.CharacterSimpleDto
import ltmp.charactergen.api.character.toDto
import ltmp.charactergen.api.character.toSimpleDto
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.repo.CharacterRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CharacterService(
    private val characterRepository: CharacterRepository,
) {
    @Transactional(readOnly = true)
    fun searchCharacters(
        name: String?,
        generationExecuteId: Long?,
        pageable: Pageable,
    ): Page<CharacterSimpleDto> =
        characterRepository.search(name, generationExecuteId, pageable).map(Character::toSimpleDto)

    @Transactional(readOnly = true)
    fun getCharacter(characterId: Long): CharacterDto = findCharacter(characterId).toDto()

    @Transactional
    fun deleteCharacter(characterId: Long) {
        val character = findCharacter(characterId)
        characterRepository.delete(character)
        characterRepository.flush()
    }

    private fun findCharacter(characterId: Long): Character =
        characterRepository.findOneWithGenerationById(characterId)
            ?: throw ResourceNotFoundException("Character not found: $characterId")
}
