package ltmp.charactergen.api.character

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import ltmp.charactergen.api.ApiErrorResponse
import ltmp.charactergen.service.CharacterService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/l0-characters")
@Tag(name = "L0 Characters", description = "생성 완료된 L0 캐릭터 조회 및 삭제 API")
class CharacterController(
    private val characterService: CharacterService,
) {
    @Operation(
        summary = "L0 캐릭터 검색",
        description = "캐릭터 이름과 생성 실행 식별자를 조건으로 저장된 캐릭터를 검색한다. 조건을 생략하면 전체를 반환한다.",
    )
    @ApiResponse(responseCode = "200", description = "캐릭터 검색 성공")
    @GetMapping
    fun searchCharacters(
        @Parameter(description = "부분 일치로 검색할 캐릭터 이름", example = "서윤")
        @RequestParam(required = false)
        name: String?,
        @Parameter(description = "캐릭터를 생성한 실행 식별자", example = "10")
        @RequestParam(required = false)
        generationExecuteId: Long?,
        @ParameterObject @PageableDefault(sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PagedModel<CharacterSimpleDto> =
        PagedModel(characterService.searchCharacters(name, generationExecuteId, pageable))

    @Operation(summary = "L0 캐릭터 단일 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "캐릭터 상세 조회 성공",
                content = [Content(schema = Schema(implementation = CharacterDto::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "캐릭터 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/{characterId}")
    fun getCharacter(
        @Parameter(description = "캐릭터 식별자", example = "1", required = true)
        @PathVariable
        characterId: Long,
    ): CharacterDto = characterService.getCharacter(characterId)

    @Operation(
        summary = "L0 캐릭터 삭제",
        description = "저장된 캐릭터와 모든 하위 데이터를 삭제한다. 캐릭터를 생성한 실행 이력은 유지한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "캐릭터 삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "캐릭터 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @DeleteMapping("/{characterId}")
    fun deleteCharacter(
        @Parameter(description = "캐릭터 식별자", example = "1", required = true)
        @PathVariable
        characterId: Long,
    ): ResponseEntity<Void> {
        characterService.deleteCharacter(characterId)
        return ResponseEntity.noContent().build()
    }
}
