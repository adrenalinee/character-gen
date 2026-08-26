package ltmp.charactergen.data.repo

import ltmp.charactergen.data.entity.Character
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CharacterRepository : JpaRepository<Character, Long> {
    @EntityGraph(attributePaths = ["generationExecute"])
    @Query(
        """
        select character
        from Character character
        where (:name is null or lower(character.name) like lower(concat('%', :name, '%')))
          and (:generationExecuteId is null or character.generationExecute.id = :generationExecuteId)
        """,
    )
    fun search(
        @Param("name") name: String?,
        @Param("generationExecuteId") generationExecuteId: Long?,
        pageable: Pageable,
    ): Page<Character>

    @EntityGraph(attributePaths = ["generationExecute"])
    fun findOneWithGenerationById(id: Long): Character?

    /** 생성 프롬프트에 "이미 존재하는 캐릭터"로 알려줄 최근 캐릭터. */
    @EntityGraph(attributePaths = ["generationExecute", "generationExecute.domain"])
    @Query("select character from Character character order by character.id desc")
    fun findRecent(pageable: Pageable): List<Character>

    fun existsByGenerationExecuteId(generationExecuteId: Long): Boolean
}
