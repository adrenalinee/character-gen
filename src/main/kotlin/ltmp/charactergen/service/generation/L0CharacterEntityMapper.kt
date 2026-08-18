package ltmp.charactergen.service.generation

import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.entity.CharacterBannedWordEntity
import ltmp.charactergen.data.entity.CharacterConcreteDetailEntity
import ltmp.charactergen.data.entity.CharacterDisclosureItemEntity
import ltmp.charactergen.data.entity.CharacterForbiddenBehaviorEntity
import ltmp.charactergen.data.entity.CharacterNonNegotiableEntity
import ltmp.charactergen.data.entity.CharacterRelationEntity
import ltmp.charactergen.data.entity.CharacterVerbalTicEntity
import ltmp.charactergen.data.entity.CharacterVoiceSampleEntity
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.model.L0CharacterResponse

fun L0CharacterResponse.toEntity(generationExecute: L0GenerationExecute): Character = Character(
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
    expressionNote = traits.expressionNote,
    identityNote = traits.identityNote,
    attachmentNoteEarly = behavior.attachmentNote.early,
    attachmentNoteDeepening = behavior.attachmentNote.deepening,
    attachmentNoteClose = behavior.attachmentNote.close,
    attachmentNoteOnRejection = behavior.attachmentNote.onRejection,
    emotionalExpressionNote = behavior.emotionalExpressionNote,
    avoidanceNote = behavior.avoidanceNote,
    speechArchetypeNote = voice.speechArchetypeNote,
    sentencePattern = voice.sentencePattern,
    underStress = voice.underStress,
    addressStyle = voice.addressStyle,
    recognizability = designNote.recognizability,
    longTermViability = designNote.longTermViability,
    imbalanceNote = designNote.imbalanceNote,
    concreteDetails = identity.concreteDetails.map { CharacterConcreteDetailEntity(detailText = it) },
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
    verbalTics = voice.verbalTics.map {
        CharacterVerbalTicEntity(situation = it.situation, description = it.description)
    },
    bannedWords = voice.bannedWords.map { CharacterBannedWordEntity(wordText = it) },
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
    disclosureItems = buildList {
        disclosureLadder.surface.forEach { add(CharacterDisclosureItemEntity(DisclosureLevel.SURFACE, it)) }
        disclosureLadder.mid.forEach { add(CharacterDisclosureItemEntity(DisclosureLevel.MID, it)) }
        disclosureLadder.deep.forEach { add(CharacterDisclosureItemEntity(DisclosureLevel.DEEP, it)) }
        disclosureLadder.deepest.forEach { add(CharacterDisclosureItemEntity(DisclosureLevel.DEEPEST, it)) }
    },
    voiceSamples = voiceSamples.map {
        CharacterVoiceSampleEntity(
            situation = it.situation,
            line = it.line,
            demonstrates = it.demonstrates,
        )
    },
)
