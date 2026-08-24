package ltmp.charactergen.config

import ltmp.charactergen.api.prompt.CreatePromptTemplateVersionRefRequest
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.BigFiveVector
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.AttachmentStyleEntity
import ltmp.charactergen.data.entity.AvoidanceStyleEntity
import ltmp.charactergen.data.entity.AxisConflictEntity
import ltmp.charactergen.data.entity.DomainEntity
import ltmp.charactergen.data.entity.EmotionalExpressionEntity
import ltmp.charactergen.data.entity.LieEntity
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.entity.SpeechArchetypeEntity
import ltmp.charactergen.data.entity.SpeechRegisterEntity
import ltmp.charactergen.data.entity.WoundAgentEntity
import ltmp.charactergen.data.entity.WoundDurationEntity
import ltmp.charactergen.data.entity.WoundFormEntity
import ltmp.charactergen.data.entity.WoundPlaceEntity
import ltmp.charactergen.data.entity.WoundTimingEntity
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.AxisConflictRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.data.repo.SpeechRegisterRepository
import ltmp.charactergen.data.repo.WoundAgentRepository
import ltmp.charactergen.data.repo.WoundDurationRepository
import ltmp.charactergen.data.repo.WoundFormRepository
import ltmp.charactergen.data.repo.WoundPlaceRepository
import ltmp.charactergen.data.repo.WoundTimingRepository
import ltmp.charactergen.service.PromptTemplateRefService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class L0ReferenceDataInitializer(
    private val domainRepository: DomainRepository,
    private val lieRepository: LieRepository,
    private val woundTimingRepository: WoundTimingRepository,
    private val woundPlaceRepository: WoundPlaceRepository,
    private val woundAgentRepository: WoundAgentRepository,
    private val woundFormRepository: WoundFormRepository,
    private val woundDurationRepository: WoundDurationRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
    private val axisConflictRepository: AxisConflictRepository,
    private val speechRegisterRepository: SpeechRegisterRepository,
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val promptTemplateRefService: PromptTemplateRefService,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        insertMissingDomains()
        insertMissingLies()
        insertMissingWoundTimings()
        insertMissingWoundPlaces()
        insertMissingWoundAgents()
        insertMissingWoundForms()
        insertMissingWoundDurations()
        insertMissingPersonalityTraitStages()
        insertMissingAttachmentStyles()
        insertMissingEmotionalExpressions()
        insertMissingAvoidanceStyles()
        insertMissingSpeechArchetypes()
        insertMissingAxisConflicts()
        insertMissingSpeechRegisters()
        insertInitialL0CharacterPrompt()
        insertInitialL0RuntimeInstructionPrompt()
    }

    private fun insertMissingDomains() {
        val existingNames = domainRepository.findAll().mapTo(mutableSetOf()) { it.name }
        val missing = DOMAIN_SPECS.filterNot { it.name in existingNames }.map {
            DomainEntity(name = it.name, description = it.description)
        }
        domainRepository.saveAll(missing)
    }

    private fun insertMissingLies() {
        val existingTypes = lieRepository.findAll().mapTo(mutableSetOf()) { it.type }
        val missing = LIE_SPECS.filterNot { it.type in existingTypes }.map {
            LieEntity(type = it.type, coreStatement = it.description)
        }
        lieRepository.saveAll(missing)
    }

    private fun insertMissingWoundTimings() {
        val existingValues = woundTimingRepository.findAll().mapTo(mutableSetOf()) { it.value }
        val missing = WOUND_TIMING_SPECS.filterNot { it.value in existingValues }.map {
            WoundTimingEntity(value = it.value, description = it.description)
        }
        woundTimingRepository.saveAll(missing)
    }

    private fun insertMissingWoundPlaces() {
        val existingValues = woundPlaceRepository.findAll().mapTo(mutableSetOf()) { it.value }
        val missing = WOUND_PLACE_SPECS.filterNot { it.value in existingValues }.map {
            WoundPlaceEntity(value = it.value, description = it.description)
        }
        woundPlaceRepository.saveAll(missing)
    }

    private fun insertMissingWoundAgents() {
        val existingValues = woundAgentRepository.findAll().mapTo(mutableSetOf()) { it.value }
        val missing = WOUND_AGENT_SPECS.filterNot { it.value in existingValues }.map {
            WoundAgentEntity(value = it.value, description = it.description)
        }
        woundAgentRepository.saveAll(missing)
    }

    private fun insertMissingWoundForms() {
        val existingValues = woundFormRepository.findAll().mapTo(mutableSetOf()) { it.value }
        val missing = WOUND_FORM_SPECS.filterNot { it.value in existingValues }.map {
            WoundFormEntity(value = it.value, description = it.description)
        }
        woundFormRepository.saveAll(missing)
    }

    private fun insertMissingWoundDurations() {
        val existingValues = woundDurationRepository.findAll().mapTo(mutableSetOf()) { it.value }
        val missing = WOUND_DURATION_SPECS.filterNot { it.value in existingValues }.map {
            WoundDurationEntity(value = it.value, description = it.description)
        }
        woundDurationRepository.saveAll(missing)
    }

    private fun insertMissingPersonalityTraitStages() {
        val existingKeys = personalityTraitStageRepository.findAll()
            .mapTo(mutableSetOf()) { it.trait to it.stage }
        val missing = PERSONALITY_TRAIT_STAGE_SPECS
            .filterNot { (it.trait to it.stage) in existingKeys }
            .map {
                PersonalityTraitStageEntity(trait = it.trait, stage = it.stage, description = it.description)
            }
        personalityTraitStageRepository.saveAll(missing)
    }

    private fun insertMissingAttachmentStyles() {
        val existing = attachmentStyleRepository.findAll().associateBy { it.type }
        val missing = ATTACHMENT_STYLE_SPECS.mapNotNull { spec ->
            val entity = existing[spec.type]
            if (entity == null) {
                AttachmentStyleEntity(
                    type = spec.type,
                    description = spec.description,
                    bigFiveVector = spec.toVector(),
                )
            } else {
                entity.bigFiveVector = spec.toVector()
                null
            }
        }
        attachmentStyleRepository.saveAll(missing)
    }

    private fun insertMissingEmotionalExpressions() {
        val existing = emotionalExpressionRepository.findAll().associateBy { it.type }
        val missing = EMOTIONAL_EXPRESSION_SPECS.mapNotNull { spec ->
            val entity = existing[spec.type]
            if (entity == null) {
                EmotionalExpressionEntity(
                    type = spec.type,
                    description = spec.description,
                    bigFiveVector = spec.toVector(),
                )
            } else {
                entity.bigFiveVector = spec.toVector()
                null
            }
        }
        emotionalExpressionRepository.saveAllAndFlush(missing)
    }

    private fun insertMissingAvoidanceStyles() {
        val existing = avoidanceStyleRepository.findAll().associateBy { it.type }
        val missing = AVOIDANCE_STYLE_SPECS.mapNotNull { spec ->
            val entity = existing[spec.type]
            if (entity == null) {
                AvoidanceStyleEntity(
                    type = spec.type,
                    description = spec.description,
                    bigFiveVector = spec.toVector(),
                )
            } else {
                entity.bigFiveVector = spec.toVector()
                null
            }
        }
        avoidanceStyleRepository.saveAllAndFlush(missing)
    }

    private fun insertMissingSpeechArchetypes() {
        val existing = speechArchetypeRepository.findAll().associateBy { it.name }
        val missing = SPEECH_ARCHETYPE_SPECS.mapNotNull { spec ->
            val entity = existing[spec.type]
            if (entity == null) {
                SpeechArchetypeEntity(
                    name = spec.type,
                    description = spec.description,
                    bigFiveVector = spec.toVector(),
                )
            } else {
                entity.bigFiveVector = spec.toVector()
                null
            }
        }
        speechArchetypeRepository.saveAll(missing)
    }

    private fun insertMissingAxisConflicts() {
        val existingPairs = axisConflictRepository.findAllWithAxisValues()
            .mapTo(mutableSetOf()) { it.emotionalExpression.type to it.avoidanceStyle.type }
        val missing = AXIS_CONFLICT_SPECS
            .filterNot { (it.emotionalExpressionType to it.avoidanceStyleType) in existingPairs }
            .map { spec ->
                AxisConflictEntity(
                    emotionalExpression = requireNotNull(
                        emotionalExpressionRepository.findByType(spec.emotionalExpressionType),
                    ) { "EmotionalExpression not found for conflict: ${spec.emotionalExpressionType}" },
                    avoidanceStyle = requireNotNull(
                        avoidanceStyleRepository.findByType(spec.avoidanceStyleType),
                    ) { "AvoidanceStyle not found for conflict: ${spec.avoidanceStyleType}" },
                    reason = spec.reason,
                )
            }
        axisConflictRepository.saveAll(missing)
    }

    private fun insertMissingSpeechRegisters() {
        val existingNames = speechRegisterRepository.findAll().mapTo(mutableSetOf()) { it.name }
        val missing = SPEECH_REGISTER_NAMES.filterNot { it in existingNames }.map {
            SpeechRegisterEntity(name = it)
        }
        speechRegisterRepository.saveAll(missing)
    }

    private fun insertInitialL0CharacterPrompt() {
        val template = promptTemplateRepository.findByName(L0_CHARACTER_PROMPT_TEMPLATE_NAME)
            ?: promptTemplateRepository.saveAndFlush(
                PromptTemplate(
                    name = L0_CHARACTER_PROMPT_TEMPLATE_NAME,
                    defaultBranch = L0_CHARACTER_PROMPT_BRANCH_NAME,
                ),
            )
        val branch = promptTemplateRefRepository.findOneWithDetailsByPromptTemplateIdAndName(
            template.id,
            L0_CHARACTER_PROMPT_BRANCH_NAME,
        ) ?: createInitialL0CharacterPromptBranch(template)
        if (template.defaultBranch == null) {
            template.defaultBranch = L0_CHARACTER_PROMPT_BRANCH_NAME
            promptTemplateRepository.flush()
        }

        // v3을 VERSION 1로 보존한 뒤 main을 v3.1로 갱신하고 VERSION 2를 한 번만 발행한다.
        if (template.lastVersion == 0L) {
            promptTemplateRefService.createVersion(
                template.id,
                CreatePromptTemplateVersionRefRequest(
                    fromRefId = branch.id,
                ),
            )
        }
        if (template.lastVersion == 1L) {
            branch.instructions = L0_CHARACTER_PROMPT_INSTRUCTIONS_V3_1
            branch.replaceInputs(
                listOf(L0_CHARACTER_PROMPT_INPUT_ROLE to L0_CHARACTER_PROMPT_INPUT_TEXT_V3_1),
            )
            promptTemplateRefRepository.flush()
            promptTemplateRefService.createVersion(
                template.id,
                CreatePromptTemplateVersionRefRequest(
                    fromRefId = branch.id,
                ),
            )
        }
        if (template.lastVersion == 2L) {
            branch.instructions = L0_CHARACTER_PROMPT_INSTRUCTIONS_V3_2
            branch.replaceInputs(
                listOf(L0_CHARACTER_PROMPT_INPUT_ROLE to L0_CHARACTER_PROMPT_INPUT_TEXT_V3_2),
            )
            promptTemplateRefRepository.flush()
            promptTemplateRefService.createVersion(
                template.id,
                CreatePromptTemplateVersionRefRequest(
                    fromRefId = branch.id,
                ),
            )
        }
    }

    /**
     * 런타임 캐릭터 인스트럭션 템플릿을 등록한다.
     *
     * 이 템플릿은 대화 시점에 사용자 메시지가 input이 되므로 저장된 input이 없다.
     * [PromptTemplateRefService.validateExecutable]이 input을 요구하는 VERSION은 발행하지 않고,
     * A/B 비교는 브랜치를 추가로 만들어서 한다.
     */
    private fun insertInitialL0RuntimeInstructionPrompt() {
        val template = promptTemplateRepository.findByName(L0_RUNTIME_INSTRUCTION_TEMPLATE_NAME)
            ?: promptTemplateRepository.saveAndFlush(
                PromptTemplate(
                    name = L0_RUNTIME_INSTRUCTION_TEMPLATE_NAME,
                    defaultBranch = L0_RUNTIME_INSTRUCTION_BRANCH_NAME,
                ),
            )
        if (
            promptTemplateRefRepository.findOneWithDetailsByPromptTemplateIdAndName(
                template.id,
                L0_RUNTIME_INSTRUCTION_BRANCH_NAME,
            ) == null
        ) {
            promptTemplateRefRepository.saveAndFlush(
                PromptTemplateRef(
                    promptTemplate = template,
                    type = PromptTemplateRefType.BRANCH,
                    name = L0_RUNTIME_INSTRUCTION_BRANCH_NAME,
                    instructions = L0_RUNTIME_INSTRUCTION_V1,
                ),
            )
        }
        if (template.defaultBranch == null) {
            template.defaultBranch = L0_RUNTIME_INSTRUCTION_BRANCH_NAME
            promptTemplateRepository.flush()
        }
    }

    private fun createInitialL0CharacterPromptBranch(template: PromptTemplate): PromptTemplateRef {
        val branch = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = L0_CHARACTER_PROMPT_BRANCH_NAME,
            instructions = L0_CHARACTER_PROMPT_INSTRUCTIONS,
        )
        branch.addInput(
            role = L0_CHARACTER_PROMPT_INPUT_ROLE,
            inputText = L0_CHARACTER_PROMPT_INPUT_TEXT,
        )
        return promptTemplateRefRepository.saveAndFlush(branch)
    }

    private data class NameDescriptionSpec(
        val name: String,
        val description: String,
    )

    private data class TypeDescriptionSpec(
        val type: String,
        val description: String,
    )

    /**
     * 표현 축의 값 하나와, 그 값이 어떤 성향에서 나오기 쉬운지의 예상 좌표.
     * 좌표는 3단계를 0으로 본 오프셋이며 0은 그 성향을 예측하지 않는다는 뜻이다.
     */
    private data class AxisValueSpec(
        val type: String,
        val description: String,
        val openness: Int,
        val conscientiousness: Int,
        val extraversion: Int,
        val agreeableness: Int,
        val neuroticism: Int,
    ) {
        // 엔티티마다 별도 인스턴스를 가져야 하므로 매번 새로 만든다.
        fun toVector(): BigFiveVector = BigFiveVector(
            openness = openness,
            conscientiousness = conscientiousness,
            extraversion = extraversion,
            agreeableness = agreeableness,
            neuroticism = neuroticism,
        )
    }

    private data class AxisConflictSpec(
        val emotionalExpressionType: String,
        val avoidanceStyleType: String,
        val reason: String,
    )

    private data class ValueDescriptionSpec(
        val value: String,
        val description: String? = null,
    )

    private data class PersonalityTraitStageSpec(
        val trait: BigFiveTrait,
        val stage: Int,
        val description: String,
    )

    private companion object {
        val DOMAIN_SPECS = listOf(
            NameDescriptionSpec(name = "웹툰", description = "웹툰 분야"),
            NameDescriptionSpec(name = "요리", description = "요리 분야"),
            NameDescriptionSpec(name = "인디 음악", description = "인디 음악 분야"),
            NameDescriptionSpec(name = "사진", description = "사진 분야"),
            NameDescriptionSpec(name = "격투기", description = "격투기 분야"),
            NameDescriptionSpec(name = "카페 창업", description = "카페 창업 분야"),
            NameDescriptionSpec(name = "수의", description = "수의 분야"),
            NameDescriptionSpec(name = "목공", description = "목공 분야"),
        )

        val LIE_SPECS = listOf(
            TypeDescriptionSpec(type = "인정 욕구형", description = "성과를 내야만 존재 가치가 있다"),
            TypeDescriptionSpec(type = "통제형", description = "내가 쥐고 있지 않으면 무너진다"),
            TypeDescriptionSpec(type = "무가치감형", description = "나는 애초에 부족한 사람이다"),
            TypeDescriptionSpec(type = "배신 경계형", description = "믿으면 반드시 배신당한다"),
            TypeDescriptionSpec(type = "대체 가능성 공포형", description = "나는 언제든 다른 사람으로 대체된다"),
            TypeDescriptionSpec(type = "부담 회피형", description = "부담이 되면 사람은 떠난다"),
            TypeDescriptionSpec(type = "완벽주의형", description = "실수하면 전부 잃는다"),
            TypeDescriptionSpec(type = "소속 결핍형", description = "나는 어디에도 진짜로 속하지 못한다"),
            TypeDescriptionSpec(type = "책임 전가 공포형", description = "내가 잘못되면 다른 사람이 다친다"),
            TypeDescriptionSpec(type = "감정 억압형", description = "감정을 보이면 약점이 된다"),
            TypeDescriptionSpec(type = "과잉 책임형", description = "모든 게 내 탓이고 내가 해결해야 한다"),
            TypeDescriptionSpec(type = "자격 부정형", description = "좋은 것을 받으면 언젠가 대가를 치른다"),
        )

        val WOUND_TIMING_SPECS = listOf(
            ValueDescriptionSpec(value = "유년", description = "사건의 인과를 이해하지 못한 채 각인됨"),
            ValueDescriptionSpec(value = "10대", description = "가장 흔한 선택. 격자에서 과대 표집되지 않게 주의"),
            ValueDescriptionSpec(value = "20대"),
            ValueDescriptionSpec(value = "30대 이후", description = "이미 형성된 자아 위에 덧씌워진 상처"),
        )

        val WOUND_PLACE_SPECS = listOf(
            ValueDescriptionSpec(value = "가정"),
            ValueDescriptionSpec(value = "학교"),
            ValueDescriptionSpec(value = "일터"),
            ValueDescriptionSpec(value = "공개된 자리"),
            ValueDescriptionSpec(value = "낯선 곳"),
        )

        val WOUND_AGENT_SPECS = listOf(
            ValueDescriptionSpec(value = "부모"),
            ValueDescriptionSpec(value = "친척", description = "미지정 시 과대 표집됨"),
            ValueDescriptionSpec(value = "스승"),
            ValueDescriptionSpec(value = "동료"),
            ValueDescriptionSpec(value = "친구"),
            ValueDescriptionSpec(value = "낯선 사람"),
            ValueDescriptionSpec(value = "특정인 없음", description = "상황·시스템·공백이 상처가 됨"),
        )

        val WOUND_FORM_SPECS = listOf(
            ValueDescriptionSpec(value = "사고", description = "예기치 못한 일이 벌어짐. 미지정 시 과대 표집됨"),
            ValueDescriptionSpec(value = "배신", description = "믿었던 관계가 깨짐"),
            ValueDescriptionSpec(value = "방치", description = "필요한 순간에 아무도 오지 않음"),
            ValueDescriptionSpec(value = "비교", description = "다른 사람과 견주어져 밀려남"),
            ValueDescriptionSpec(value = "상실", description = "있던 것을 잃음"),
            ValueDescriptionSpec(value = "누명", description = "하지 않은 일의 책임을 짐"),
        )

        val WOUND_DURATION_SPECS = listOf(
            ValueDescriptionSpec(value = "한순간"),
            ValueDescriptionSpec(value = "하루"),
            ValueDescriptionSpec(value = "몇 달에 걸쳐"),
            ValueDescriptionSpec(value = "수년간 서서히", description = "극적인 사건 없이 형성된 Lie를 만듦"),
        )

        val PERSONALITY_TRAIT_STAGE_SPECS = listOf(
            PersonalityTraitStageSpec(BigFiveTrait.OPENNESS, 1, "익숙한 방식을 고수한다. 새로운 시도를 제안받으면 지금 방식의 문제부터 묻고, 근거가 없으면 바꾸지 않는다."),
            PersonalityTraitStageSpec(BigFiveTrait.OPENNESS, 2, "검증된 범위 안에서만 조금씩 바꾼다. 남이 먼저 해본 것을 확인한 뒤에 따라간다."),
            PersonalityTraitStageSpec(BigFiveTrait.OPENNESS, 3, "이 축은 이 인물을 설명하는 특징이 아니다. 여기서 파생되는 행동을 만들지 마라."),
            PersonalityTraitStageSpec(BigFiveTrait.OPENNESS, 4, "낯선 것을 먼저 시도해본다. 자기 분야 밖의 것에서 아이디어를 끌어온다."),
            PersonalityTraitStageSpec(BigFiveTrait.OPENNESS, 5, "정해진 방식을 지루해한다. 검증되지 않았어도 새로운 쪽을 택하고, 반복이 계속되면 스스로 판을 바꾼다."),
            PersonalityTraitStageSpec(BigFiveTrait.CONSCIENTIOUSNESS, 1, "계획을 세우지 않는다. 마감과 약속을 자주 놓치고, 그때그때 상황에 맞춘다."),
            PersonalityTraitStageSpec(BigFiveTrait.CONSCIENTIOUSNESS, 2, "큰 것만 챙기고 세부는 흘린다. 중요하다고 판단한 일에는 늦지 않지만 나머지는 미룬다."),
            PersonalityTraitStageSpec(BigFiveTrait.CONSCIENTIOUSNESS, 3, "이 축은 이 인물을 설명하는 특징이 아니다. 여기서 파생되는 행동을 만들지 마라."),
            PersonalityTraitStageSpec(BigFiveTrait.CONSCIENTIOUSNESS, 4, "약속한 것은 기록하고 확인한다. 준비 없이 시작하는 것을 불편해한다."),
            PersonalityTraitStageSpec(BigFiveTrait.CONSCIENTIOUSNESS, 5, "절차를 생략하지 않는다. 계획이 틀어지면 다시 짜기 전에는 다음으로 넘어가지 못한다."),
            PersonalityTraitStageSpec(BigFiveTrait.EXTRAVERSION, 1, "먼저 말을 걸지 않는다. 사람이 여럿인 자리를 되도록 피하고, 혼자 있는 시간에 회복한다."),
            PersonalityTraitStageSpec(BigFiveTrait.EXTRAVERSION, 2, "아는 사람 소수와만 편하다. 낯선 자리에서는 듣는 쪽에 있고 필요한 말만 한다."),
            PersonalityTraitStageSpec(BigFiveTrait.EXTRAVERSION, 3, "이 축은 이 인물을 설명하는 특징이 아니다. 여기서 파생되는 행동을 만들지 마라."),
            PersonalityTraitStageSpec(BigFiveTrait.EXTRAVERSION, 4, "사람이 모이는 자리를 찾아간다. 처음 보는 사람에게도 먼저 말을 건다."),
            PersonalityTraitStageSpec(BigFiveTrait.EXTRAVERSION, 5, "혼자 있으면 가라앉는다. 대화의 중심에 서려 하고, 반응이 없으면 더 크게 움직인다."),
            PersonalityTraitStageSpec(BigFiveTrait.AGREEABLENESS, 1, "갈등을 피하지 않는다. 상대가 불편해해도 자기 판단을 먼저 말하고, 호의에는 대가를 계산한다."),
            PersonalityTraitStageSpec(BigFiveTrait.AGREEABLENESS, 2, "의심부터 한다. 친절에 이유를 찾고, 양보하기 전에 조건을 확인한다."),
            PersonalityTraitStageSpec(BigFiveTrait.AGREEABLENESS, 3, "이 축은 이 인물을 설명하는 특징이 아니다. 여기서 파생되는 행동을 만들지 마라."),
            PersonalityTraitStageSpec(BigFiveTrait.AGREEABLENESS, 4, "상대의 입장을 먼저 헤아린다. 부탁을 잘 거절하지 못한다."),
            PersonalityTraitStageSpec(BigFiveTrait.AGREEABLENESS, 5, "마찰이 생기면 자기 몫을 먼저 내놓는다. 상대의 기분을 자기 상태보다 앞에 둔다."),
            PersonalityTraitStageSpec(BigFiveTrait.NEUROTICISM, 1, "잘 흔들리지 않는다. 나쁜 일이 생겨도 반응이 늦고 오래 담아두지 않는다."),
            PersonalityTraitStageSpec(BigFiveTrait.NEUROTICISM, 2, "웬만한 일에는 평정을 유지한다. 걱정이 들어도 행동이 바뀌지 않는다."),
            PersonalityTraitStageSpec(BigFiveTrait.NEUROTICISM, 3, "이 축은 이 인물을 설명하는 특징이 아니다. 여기서 파생되는 행동을 만들지 마라."),
            PersonalityTraitStageSpec(BigFiveTrait.NEUROTICISM, 4, "작은 신호에 예민하다. 불확실한 상황에서 최악을 먼저 떠올리고 확인을 반복한다."),
            PersonalityTraitStageSpec(BigFiveTrait.NEUROTICISM, 5, "감정 진폭이 크다. 사소한 계기로 크게 흔들리고, 가라앉는 데 시간이 걸린다."),
        )

        // 예상 좌표 순서: 개방성, 성실성, 외향성, 우호성, 신경성
        val ATTACHMENT_STYLE_SPECS = listOf(
            AxisValueSpec("경계 후 개방", "오래 재본 뒤에야 열지만, 한번 열면 깊이 들어간다", 0, 0, -1, -1, 0),
            AxisValueSpec("급속 접근 후 냉각", "빠르게 가까워지고 어느 순간 급격히 식는다", 0, 0, 2, 0, 1),
            AxisValueSpec("일정 거리 유지", "누구와도 비슷한 거리를 두고, 그 선을 넘지 않는다", 0, 0, -1, 0, -1),
            AxisValueSpec("내외 이중", "안에 들인 사람과 밖의 사람을 전혀 다르게 대한다", 0, 0, 0, -1, 0),
            AxisValueSpec("기울어짐", "가까워지면 상대 중심으로 자신을 맞춘다", 0, 0, 0, 2, 0),
            AxisValueSpec("시험형", "떠날지 확인하려고 관계를 자꾸 건드린다", 0, 0, 0, -1, 2),
        )

        val EMOTIONAL_EXPRESSION_SPECS = listOf(
            AxisValueSpec("즉시 표출", "느낀 순간 표정과 말에 그대로 나온다", 0, 0, 1, 0, 1),
            AxisValueSpec("사실 치환", "감정 대신 상황·수치·해야 할 일을 말한다", 0, 1, 0, 0, -1),
            AxisValueSpec("과장 후 축소", "실제보다 크게 말해놓고 나중에 별일 아니라고 거둔다", 0, 0, 1, 0, 1),
            AxisValueSpec("지연 표출", "그 자리에서는 안 나오고 한참 뒤 다른 형태로 나온다", 0, 0, -1, 0, 0),
            AxisValueSpec("신체화", "말 대신 행동·자세·몸 상태로 나타난다", 0, 0, -1, 0, 1),
            AxisValueSpec("타인 감정 우선", "자기 감정은 알아채지 못하고 남의 감정에는 즉각 반응한다", 0, 0, 0, 2, 0),
        )

        val AVOIDANCE_STYLE_SPECS = listOf(
            AxisValueSpec("농담으로 넘김", "웃기게 만들어 질문을 무력화한다", 0, 0, 1, 1, 0),
            AxisValueSpec("화제 전환", "관련 있어 보이는 다른 이야기로 옮긴다", 0, 0, 0, 1, 0),
            AxisValueSpec("짧게 답하고 침묵", "최소한만 답하고 더 말하지 않는다", 0, 0, -2, 0, 0),
            AxisValueSpec("질문으로 되받음", "상대에게 질문을 던져 초점을 옮긴다", 0, 0, 1, 0, 0),
            AxisValueSpec("일부만 말하고 흐림", "사실의 일부를 주고 나머지는 얼버무린다", 0, 0, 0, 0, 1),
            AxisValueSpec("거절 명시", "그 이야기는 하지 않겠다고 분명히 말한다", 0, 0, 0, -2, 0),
        )

        val SPEECH_ARCHETYPE_SPECS = listOf(
            AxisValueSpec("실무형", "결론과 시각을 먼저 말하고 이유는 생략", 0, 1, 0, 0, 0),
            AxisValueSpec("우회형", "농담이나 딴소리로 감싼 뒤 본론에 늦게 도착", 0, 0, 1, 1, 0),
            AxisValueSpec("장황형", "하나를 설명하려고 배경부터 길게 깐다", 0, 1, 1, 0, 0),
            AxisValueSpec("반문형", "답하기 전에 되묻는다", 0, 0, -1, 0, 1),
            AxisValueSpec("비유형", "자기 분야의 사물에 빗대어 말한다", 2, 0, 0, 0, 0),
            AxisValueSpec("과공손형", "필요 이상으로 예의를 갖춰 거리를 만든다", 0, 0, -1, 1, 1),
            AxisValueSpec("단답형", "최소한만 말하고 나머지는 상대가 채우게 둔다", 0, 0, -2, 0, 0),
            AxisValueSpec("독백형", "상대에게 말하다가 혼잣말로 새어 나간다", 0, 0, -1, 0, 1),
        )

        /**
         * 정서 표출과 회피 방식은 "감정적인 화제가 나왔을 때"에서 겹치므로 이 조합만 막는다.
         * 나머지 축쌍은 서로 다른 상황에 적용되어 원칙적으로 충돌하지 않는다.
         */
        val AXIS_CONFLICT_SPECS = listOf(
            AxisConflictSpec(
                emotionalExpressionType = "즉시 표출",
                avoidanceStyleType = "짧게 답하고 침묵",
                reason = "감정이 그 자리에서 드러나는 인물이 같은 상황에서 말을 줄이는 것은 둘 중 하나가 무시된다는 뜻이다",
            ),
            AxisConflictSpec(
                emotionalExpressionType = "즉시 표출",
                avoidanceStyleType = "거절 명시",
                reason = "감정이 그대로 나오는 인물이 그 화제를 하지 않겠다고 선언하는 것은 양립하지 않는다",
            ),
            AxisConflictSpec(
                emotionalExpressionType = "신체화",
                avoidanceStyleType = "거절 명시",
                reason = "말 대신 몸으로 나타내는 인물이 거절만 말로 분명히 하는 것은 양립하지 않는다",
            ),
        )

        val SPEECH_REGISTER_NAMES = listOf(
            "반말 기본",
            "존댓말 기본",
            "처음엔 존댓말 후 반말",
        )
    }
}
