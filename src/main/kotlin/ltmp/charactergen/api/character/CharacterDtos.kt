package ltmp.charactergen.api.character

import io.swagger.v3.oas.annotations.media.Schema
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.model.NonNegotiableDomain
import ltmp.charactergen.model.RelationPresence
import ltmp.charactergen.model.RelationSlot
import java.time.OffsetDateTime

@Schema(description = "Character 테이블의 컬럼과 하위 테이블 행을 그대로 표현한 L0 캐릭터 상세 정보")
data class CharacterDto(
    @field:Schema(description = "캐릭터 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,

    @field:Schema(description = "캐릭터를 생성한 실행 식별자", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    val generationExecuteId: Long,

    @field:Schema(description = "캐릭터 이름", example = "서윤", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,

    @field:Schema(description = "캐릭터 나이", example = "29", requiredMode = Schema.RequiredMode.REQUIRED)
    val age: Int,

    @field:Schema(description = "캐릭터 성별", example = "여성", requiredMode = Schema.RequiredMode.REQUIRED)
    val gender: String,

    @field:Schema(description = "캐릭터 직업", example = "웹툰 작가", requiredMode = Schema.RequiredMode.REQUIRED)
    val occupation: String,

    @field:Schema(description = "캐릭터의 일상", requiredMode = Schema.RequiredMode.REQUIRED)
    val dailyLife: String,

    @field:Schema(description = "캐릭터 한 줄 소개", requiredMode = Schema.RequiredMode.REQUIRED)
    val oneLine: String,

    @field:Schema(description = "Lie 유형", requiredMode = Schema.RequiredMode.REQUIRED)
    val lieType: String,

    @field:Schema(description = "Lie 문장", requiredMode = Schema.RequiredMode.REQUIRED)
    val lieStatement: String,

    @field:Schema(description = "Lie의 기원이 된 상처", requiredMode = Schema.RequiredMode.REQUIRED)
    val lieWound: String,

    @field:Schema(description = "캐릭터가 원하는 것", requiredMode = Schema.RequiredMode.REQUIRED)
    val want: String,

    @field:Schema(description = "캐릭터에게 필요한 것", requiredMode = Schema.RequiredMode.REQUIRED)
    val need: String,

    @field:Schema(description = "Want와 Need의 충돌", requiredMode = Schema.RequiredMode.REQUIRED)
    val wantNeedConflict: String,

    @field:Schema(description = "모순을 구성하는 첫 번째 특성", requiredMode = Schema.RequiredMode.REQUIRED)
    val contradictionTraitA: String,

    @field:Schema(description = "모순을 구성하는 두 번째 특성", requiredMode = Schema.RequiredMode.REQUIRED)
    val contradictionTraitB: String,

    @field:Schema(description = "두 모순 특성의 공통 뿌리", requiredMode = Schema.RequiredMode.REQUIRED)
    val contradictionSharedRoot: String,

    @field:Schema(description = "성향 표출 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val expressionNote: String,

    @field:Schema(description = "정체성 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val identityNote: String,

    @field:Schema(description = "관계 태도 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val attachmentNote: String,

    @field:Schema(description = "정서 표출 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val emotionalExpressionNote: String,

    @field:Schema(description = "회피 방식 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val avoidanceNote: String,

    @field:Schema(description = "말투 원형 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val speechArchetypeNote: String,

    @field:Schema(description = "문장 패턴", requiredMode = Schema.RequiredMode.REQUIRED)
    val sentencePattern: String,

    @field:Schema(description = "스트레스 상황에서의 말투", requiredMode = Schema.RequiredMode.REQUIRED)
    val underStress: String,

    @field:Schema(description = "상대를 부르는 방식", requiredMode = Schema.RequiredMode.REQUIRED)
    val addressStyle: String,

    @field:Schema(description = "캐릭터의 인식 가능성 평가", requiredMode = Schema.RequiredMode.REQUIRED)
    val recognizability: String,

    @field:Schema(description = "캐릭터의 장기 활용 가능성", requiredMode = Schema.RequiredMode.REQUIRED)
    val longTermViability: String,

    @field:Schema(description = "캐릭터 설계 불균형 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val imbalanceNote: String,

    @field:Schema(description = "구체적 디테일 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val concreteDetails: List<CharacterConcreteDetailDto>,

    @field:Schema(description = "타협 불가능한 기준 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val nonNegotiables: List<CharacterNonNegotiableDto>,

    @field:Schema(description = "금지 행동 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val forbiddenBehaviors: List<CharacterForbiddenBehaviorDto>,

    @field:Schema(description = "언어 습관 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val verbalTics: List<CharacterVerbalTicDto>,

    @field:Schema(description = "금지 단어 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val bannedWords: List<CharacterBannedWordDto>,

    @field:Schema(description = "관계 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val relations: List<CharacterRelationDto>,

    @field:Schema(description = "공개 단계 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val disclosureItems: List<CharacterDisclosureItemDto>,

    @field:Schema(description = "대사 예시 하위 테이블 행", requiredMode = Schema.RequiredMode.REQUIRED)
    val voiceSamples: List<CharacterVoiceSampleDto>,

    @field:Schema(description = "캐릭터 저장 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,

    @field:Schema(description = "캐릭터 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터 검색 목록 정보. 자식 객체와 크기가 큰 서술 필드는 상세 조회에서만 제공한다.")
data class CharacterSimpleDto(
    @field:Schema(description = "캐릭터 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,

    @field:Schema(description = "캐릭터를 생성한 실행 식별자", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    val generationExecuteId: Long,

    @field:Schema(description = "캐릭터 이름", example = "서윤", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,

    @field:Schema(description = "캐릭터 나이", example = "29", requiredMode = Schema.RequiredMode.REQUIRED)
    val age: Int,

    @field:Schema(description = "캐릭터 성별", example = "여성", requiredMode = Schema.RequiredMode.REQUIRED)
    val gender: String,

    @field:Schema(description = "캐릭터 직업", example = "웹툰 작가", requiredMode = Schema.RequiredMode.REQUIRED)
    val occupation: String,

    @field:Schema(description = "캐릭터 저장 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,

    @field:Schema(description = "캐릭터 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 구체적 디테일 테이블 행")
data class CharacterConcreteDetailDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "구체적 디테일", requiredMode = Schema.RequiredMode.REQUIRED)
    val detailText: String,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 타협 불가능한 기준 테이블 행")
data class CharacterNonNegotiableDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "기준 분야", requiredMode = Schema.RequiredMode.REQUIRED)
    val domain: NonNegotiableDomain,
    @field:Schema(description = "기준 문장", requiredMode = Schema.RequiredMode.REQUIRED)
    val statement: String,
    @field:Schema(description = "Lie와의 연결", requiredMode = Schema.RequiredMode.REQUIRED)
    val tieToLie: String,
    @field:Schema(description = "사용자에게 노출되는 기준인지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    val userFacing: Boolean,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 금지 행동 테이블 행")
data class CharacterForbiddenBehaviorDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "금지 행동", requiredMode = Schema.RequiredMode.REQUIRED)
    val behavior: String,
    @field:Schema(description = "Lie와의 연결", requiredMode = Schema.RequiredMode.REQUIRED)
    val tieToLie: String,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 언어 습관 테이블 행")
data class CharacterVerbalTicDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "언어 습관이 나타나는 상황", requiredMode = Schema.RequiredMode.REQUIRED)
    val situation: String,
    @field:Schema(description = "언어 습관 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val description: String,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 금지 단어 테이블 행")
data class CharacterBannedWordDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "금지 단어", requiredMode = Schema.RequiredMode.REQUIRED)
    val wordText: String,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 관계 테이블 행")
data class CharacterRelationDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "관계 슬롯", requiredMode = Schema.RequiredMode.REQUIRED)
    val slot: RelationSlot,
    @field:Schema(description = "관계 인물 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "캐릭터와의 관계", requiredMode = Schema.RequiredMode.REQUIRED)
    val relation: String,
    @field:Schema(description = "관계 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val description: String,
    @field:Schema(description = "관계 인물의 현존 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    val presence: RelationPresence,
    @field:Schema(description = "Lie와의 연결", requiredMode = Schema.RequiredMode.REQUIRED)
    val tieToLie: String,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 공개 단계 테이블 행")
data class CharacterDisclosureItemDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "공개 단계", requiredMode = Schema.RequiredMode.REQUIRED)
    val level: DisclosureLevel,
    @field:Schema(description = "공개 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    val itemText: String,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "캐릭터의 대사 예시 테이블 행")
data class CharacterVoiceSampleDto(
    @field:Schema(description = "행 식별자", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "대사 상황", requiredMode = Schema.RequiredMode.REQUIRED)
    val situation: String,
    @field:Schema(description = "대사", requiredMode = Schema.RequiredMode.REQUIRED)
    val line: String,
    @field:Schema(description = "대사가 보여주는 캐릭터 특성", requiredMode = Schema.RequiredMode.REQUIRED)
    val demonstrates: String,
    @field:Schema(description = "행 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "행 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

fun Character.toDto() = CharacterDto(
    id = id,
    generationExecuteId = generationExecute.id,
    name = name,
    age = age,
    gender = gender,
    occupation = occupation,
    dailyLife = dailyLife,
    oneLine = oneLine,
    lieType = lieType,
    lieStatement = lieStatement,
    lieWound = lieWound,
    want = want,
    need = need,
    wantNeedConflict = wantNeedConflict,
    contradictionTraitA = contradictionTraitA,
    contradictionTraitB = contradictionTraitB,
    contradictionSharedRoot = contradictionSharedRoot,
    expressionNote = expressionNote,
    identityNote = identityNote,
    attachmentNote = attachmentNote,
    emotionalExpressionNote = emotionalExpressionNote,
    avoidanceNote = avoidanceNote,
    speechArchetypeNote = speechArchetypeNote,
    sentencePattern = sentencePattern,
    underStress = underStress,
    addressStyle = addressStyle,
    recognizability = recognizability,
    longTermViability = longTermViability,
    imbalanceNote = imbalanceNote,
    concreteDetails = concreteDetails.map {
        CharacterConcreteDetailDto(
            id = it.id,
            detailText = it.detailText,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    nonNegotiables = nonNegotiables.map {
        CharacterNonNegotiableDto(
            id = it.id,
            domain = it.domain,
            statement = it.statement,
            tieToLie = it.tieToLie,
            userFacing = it.userFacing,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    forbiddenBehaviors = forbiddenBehaviors.map {
        CharacterForbiddenBehaviorDto(
            id = it.id,
            behavior = it.behavior,
            tieToLie = it.tieToLie,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    verbalTics = verbalTics.map {
        CharacterVerbalTicDto(
            id = it.id,
            situation = it.situation,
            description = it.description,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    bannedWords = bannedWords.map {
        CharacterBannedWordDto(
            id = it.id,
            wordText = it.wordText,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    relations = relations.map {
        CharacterRelationDto(
            id = it.id,
            slot = it.slot,
            name = it.name,
            relation = it.relation,
            description = it.description,
            presence = it.presence,
            tieToLie = it.tieToLie,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    disclosureItems = disclosureItems.map {
        CharacterDisclosureItemDto(
            id = it.id,
            level = it.level,
            itemText = it.itemText,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    voiceSamples = voiceSamples.map {
        CharacterVoiceSampleDto(
            id = it.id,
            situation = it.situation,
            line = it.line,
            demonstrates = it.demonstrates,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Character.toSimpleDto() = CharacterSimpleDto(
    id = id,
    generationExecuteId = generationExecute.id,
    name = name,
    age = age,
    gender = gender,
    occupation = occupation,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
