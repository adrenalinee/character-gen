package ltmp.charactergen.service.generation

import ltmp.charactergen.data.BannedWordSource
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.entity.CharacterBannedWordEntity
import ltmp.charactergen.data.entity.CharacterBehavioralRuleEntity
import ltmp.charactergen.data.entity.CharacterConcreteDetailEntity
import ltmp.charactergen.data.entity.CharacterDisclosureItemEntity
import ltmp.charactergen.data.entity.CharacterForbiddenBehaviorEntity
import ltmp.charactergen.data.entity.CharacterNonNegotiableEntity
import ltmp.charactergen.data.entity.CharacterRelationEntity
import ltmp.charactergen.data.entity.CharacterRelationshipStageEntity
import ltmp.charactergen.data.entity.CharacterVerbalTicEntity
import ltmp.charactergen.data.entity.CharacterVoiceSampleEntity
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.model.RelationshipStageNote

/**
 * @param runtimeInclusion 런타임 대화 프롬프트에 넣을 항목.
 *   `RuntimeDuplicateSelector`가 저장 직전에 판정한 결과다. (v4.2 B-13)
 */
fun L0CharacterResponse.toEntity(
    generationExecute: L0GenerationExecute,
    runtimeInclusion: RuntimeInclusion,
): Character = Character(
    generationExecute = generationExecute,
    name = identity.name,
    age = identity.age,
    gender = identity.gender,
    occupation = identity.occupation,
    dailyLife = identity.dailyLife,
    oneLine = identity.oneLine,
    lieType = core.lie.type,
    lieStatement = core.lie.statement,
    lieWound = core.lie.wound,
    want = core.want,
    need = core.need,
    wantNeedConflict = core.wantNeedConflict,
    contradictionTraitA = core.contradiction.traitA,
    contradictionTraitB = core.contradiction.traitB,
    contradictionSharedRoot = core.contradiction.sharedRoot,
    onRejection = relationshipEvents.onRejection,
    onConflict = relationshipEvents.onConflict,
    onAbsence = relationshipEvents.onAbsence,
    onOverApproach = relationshipEvents.onOverApproach,
    emotionalExpressionNote = behavior.emotionalExpressionNote,
    avoidanceNote = behavior.avoidanceNote,
    speechArchetypeNote = voice.speechArchetypeNote,
    sentencePattern = voice.sentencePattern,
    sentencePatternRuntimeIncluded = runtimeInclusion.sentencePatternIncluded,
    recognizability = designNote.recognizability,
    longTermViability = designNote.longTermViability,
    imbalanceNote = designNote.imbalanceNote,
    traitReflection = designNote.traitReflection,
    concreteDetails = identity.concreteDetails.mapIndexed { index, detail ->
        CharacterConcreteDetailEntity(
            detailText = detail,
            runtimeIncluded = index in runtimeInclusion.concreteDetailIndexes,
        )
    },
    behavioralRules = core.lie.behavioralRule.map { CharacterBehavioralRuleEntity(ruleText = it) },
    relationshipStages = listOf(
        relationshipStages.early.toEntity(RelationshipStage.EARLY),
        relationshipStages.deepening.toEntity(RelationshipStage.DEEPENING),
        relationshipStages.close.toEntity(RelationshipStage.CLOSE),
    ),
    nonNegotiables = core.nonNegotiables.map {
        CharacterNonNegotiableEntity(
            domain = it.domain,
            statement = it.statement,
            tieToLie = it.tieToLie,
            userFacing = it.userFacing,
        )
    },
    forbiddenBehaviors = core.forbiddenBehaviors.map {
        CharacterForbiddenBehaviorEntity(behavior = it.behavior, tieToLie = it.tieToLie)
    },
    verbalTics = voice.verbalTics.mapIndexed { index, tic ->
        CharacterVerbalTicEntity(
            situation = tic.situation,
            description = tic.description,
            runtimeIncluded = index in runtimeInclusion.verbalTicIndexes,
        )
    },
    bannedWords = buildList {
        voice.bannedWords.fromLie.forEach { add(CharacterBannedWordEntity(BannedWordSource.FROM_LIE, it)) }
        voice.bannedWords.fromBackground.forEach { add(CharacterBannedWordEntity(BannedWordSource.FROM_BACKGROUND, it)) }
    },
    relations = relations.map {
        CharacterRelationEntity(
            slot = it.slot,
            name = it.name,
            relation = it.relation,
            description = it.description,
            presence = it.presence,
            tieToLie = it.tieToLie,
        )
    },
    disclosureItems = disclosureLadder.runtimeOrderedItems().mapIndexed { index, (level, text) ->
        CharacterDisclosureItemEntity(
            level = level,
            itemText = text,
            runtimeIncluded = index in runtimeInclusion.disclosureItemIndexes,
        )
    },
    voiceSamples = voiceSamples.map {
        CharacterVoiceSampleEntity(
            situation = it.situation,
            line = it.line,
            demonstrates = it.demonstrates,
        )
    },
)

private fun RelationshipStageNote.toEntity(stage: RelationshipStage) = CharacterRelationshipStageEntity(
    stage = stage,
    attachment = attachment,
    address = address,
    emotionalRange = emotionalRange,
    avoidance = avoidance,
)
