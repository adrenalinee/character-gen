package ltmp.charactergen.service

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.BigFiveVector
import ltmp.charactergen.data.ExpressionAxis
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.AxisConflictRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.service.generation.GenerationPrompt
import ltmp.charactergen.service.generation.GenerationPromptInput
import ltmp.charactergen.service.generation.L0CharacterPromptRenderer
import ltmp.charactergen.service.generation.L0GenerationOptions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Transactional
class L0AxisBalanceIntegrationTests(
    private val mockMvc: MockMvc,
    private val axisBalanceService: L0AxisBalanceService,
    private val generationService: L0CharacterGenerationService,
    private val promptRenderer: L0CharacterPromptRenderer,
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
    private val axisConflictRepository: AxisConflictRepository,
) {
    @Test
    fun `표현 축 카탈로그와 충돌 매트릭스가 초기화된다`() {
        assertEquals(1, speechArchetype("장황형").bigFiveVector.extraversion)
        assertEquals(1, speechArchetype("장황형").bigFiveVector.conscientiousness)
        assertEquals(-2, avoidanceStyle("짧게 답하고 침묵").bigFiveVector.extraversion)
        assertEquals(2, attachmentStyle("기울어짐").bigFiveVector.agreeableness)
        assertEquals(3, axisConflictRepository.findAllWithAxisValues().size)
    }

    @Test
    fun `성향과 예상 좌표의 거리가 3 이상이면 불균형으로 판정한다`() {
        val imbalances = axisBalanceService.findImbalances(
            stages = mapOf(BigFiveTrait.EXTRAVERSION to 1),
            axisValues = listOf(expressionAxisValue(ExpressionAxis.SPEECH_ARCHETYPE, "장황형", 0, 1, 1, 0, 0)),
        )

        val imbalance = imbalances.single()
        assertEquals(BigFiveTrait.EXTRAVERSION, imbalance.trait)
        assertEquals(3, imbalance.distance)
        assertEquals("외향성 1단계 × 말투 원형 '장황형'", imbalance.describe())
    }

    @Test
    fun `거리가 2면 약한 불균형이므로 알리지 않는다`() {
        val imbalances = axisBalanceService.findImbalances(
            stages = mapOf(BigFiveTrait.EXTRAVERSION to 3),
            axisValues = listOf(expressionAxisValue(ExpressionAxis.AVOIDANCE_STYLE, "짧게 답하고 침묵", 0, 0, -2, 0, 0)),
        )

        assertTrue(imbalances.isEmpty())
    }

    @Test
    fun `예상 좌표가 0인 성향은 판정에서 제외한다`() {
        val imbalances = axisBalanceService.findImbalances(
            stages = mapOf(BigFiveTrait.EXTRAVERSION to 1),
            axisValues = listOf(expressionAxisValue(ExpressionAxis.SPEECH_ARCHETYPE, "비유형", 2, 0, 0, 0, 0)),
        )

        assertTrue(imbalances.isEmpty())
    }

    @Test
    fun `지정되지 않은 성향은 판정에서 제외한다`() {
        val imbalances = axisBalanceService.findImbalances(
            stages = emptyMap(),
            axisValues = listOf(expressionAxisValue(ExpressionAxis.SPEECH_ARCHETYPE, "장황형", 0, 1, 1, 0, 0)),
        )

        assertTrue(imbalances.isEmpty())
    }

    @Test
    fun `정서 표출과 회피 방식이 충돌하는 조합을 찾아낸다`() {
        val result = axisBalanceService.evaluate(
            opennessStage = null,
            conscientiousnessStage = null,
            extraversionStage = null,
            agreeablenessStage = null,
            neuroticismStage = null,
            attachmentStyle = null,
            emotionalExpression = emotionalExpression("즉시 표출"),
            avoidanceStyle = avoidanceStyle("짧게 답하고 침묵"),
            speechArchetype = null,
        )

        val conflict = assertNotNull(result.conflict)
        assertEquals("정서 표출 '즉시 표출' × 회피 방식 '짧게 답하고 침묵'", conflict.describe())
        assertTrue(result.blocked)
    }

    @Test
    fun `충돌 매트릭스에 없는 조합은 통과시킨다`() {
        val result = axisBalanceService.evaluate(
            opennessStage = null,
            conscientiousnessStage = null,
            extraversionStage = null,
            agreeablenessStage = null,
            neuroticismStage = null,
            attachmentStyle = null,
            emotionalExpression = emotionalExpression("사실 치환"),
            avoidanceStyle = avoidanceStyle("짧게 답하고 침묵"),
            speechArchetype = null,
        )

        assertNull(result.conflict)
        assertTrue(result.imbalances.isEmpty())
    }

    @Test
    fun `충돌 조합으로 생성을 요청하면 거부한다`() {
        val refId = executableRefId()

        val failure = assertFailsWith<InvalidRequestException> {
            generationService.requestGeneration(
                generationRequest(
                    refId = refId,
                    extraversionStage = 3,
                    agreeablenessStage = 3,
                    attachmentType = "일정 거리 유지",
                    emotionalExpressionType = "즉시 표출",
                    avoidanceStyleType = "짧게 답하고 침묵",
                    speechArchetypeName = "실무형",
                ),
            )
        }

        assertTrue(failure.detail.contains("함께 지정할 수 없다"), failure.detail)
    }

    @Test
    fun `불균형이 두 개 이상이면 생성을 거부한다`() {
        val refId = executableRefId()

        val failure = assertFailsWith<InvalidRequestException> {
            generationService.requestGeneration(
                generationRequest(
                    refId = refId,
                    extraversionStage = 1,
                    agreeablenessStage = 5,
                    attachmentType = "일정 거리 유지",
                    emotionalExpressionType = "사실 치환",
                    avoidanceStyleType = "거절 명시",
                    speechArchetypeName = "장황형",
                ),
            )
        }

        assertTrue(failure.detail.contains("한 번에 하나만 허용한다"), failure.detail)
        assertTrue(failure.detail.contains("말투 원형 '장황형'"), failure.detail)
        assertTrue(failure.detail.contains("회피 방식 '거절 명시'"), failure.detail)
    }

    @Test
    fun `불균형이 하나면 생성을 수락한다`() {
        val refId = executableRefId()

        val execute = generationService.requestGeneration(
            generationRequest(
                refId = refId,
                extraversionStage = 1,
                agreeablenessStage = 3,
                attachmentType = "일정 거리 유지",
                emotionalExpressionType = "사실 치환",
                avoidanceStyleType = "짧게 답하고 침묵",
                speechArchetypeName = "장황형",
            ),
        )

        assertEquals(refId, execute.refId)
    }

    @Test
    fun `불균형이 있으면 렌더 시점에 IMBALANCE_NOTE를 주입한다`() {
        val rendered = promptRenderer.render(
            prompt = imbalancePlaceholderPrompt(),
            request = renderOnlyRequest(),
            options = L0GenerationOptions(
                extraversionStage = 1,
                speechArchetypeName = "장황형",
                speechArchetypeVector = BigFiveVector(
                    openness = 0,
                    conscientiousness = 1,
                    extraversion = 1,
                    agreeableness = 0,
                    neuroticism = 0,
                ),
            ),
        )

        val inputText = rendered.inputs.single().inputText
        assertTrue(inputText.contains("외향성 1단계와 말투 원형 '장황형' 조합은"), inputText)
        assertTrue(inputText.contains("design_note.imbalance_note에 그 설명을 적어라."), inputText)
    }

    @Test
    fun `불균형이 없으면 IMBALANCE_NOTE를 없음으로 채운다`() {
        val rendered = promptRenderer.render(
            prompt = imbalancePlaceholderPrompt(),
            request = renderOnlyRequest(),
            options = L0GenerationOptions(
                extraversionStage = 3,
                speechArchetypeName = "장황형",
                speechArchetypeVector = BigFiveVector(
                    openness = 0,
                    conscientiousness = 1,
                    extraversion = 1,
                    agreeableness = 0,
                    neuroticism = 0,
                ),
            ),
        )

        assertEquals("## 불균형 지정\n없음", rendered.inputs.single().inputText)
    }

    @Test
    fun `사전 점검 API가 충돌과 불균형을 함께 알려준다`() {
        val requestBody = """
        {
          "extraversionStageId":${stageId(BigFiveTrait.EXTRAVERSION, 1)},
          "emotionalExpressionId":${emotionalExpression("즉시 표출").id},
          "avoidanceStyleId":${avoidanceStyle("짧게 답하고 침묵").id},
          "speechArchetypeId":${speechArchetype("장황형").id}
        }
        """.trimIndent()

        mockMvc.post("/api/v1/l0-characters/axis-balance-checks") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.blocked") { value(true) }
            jsonPath("$.conflict.avoidanceStyleType") { value("짧게 답하고 침묵") }
            // 외향성 1단계는 "즉시 표출"(E+1)과 "장황형"(E+1) 양쪽에서 거리 3이 된다.
            jsonPath("$.imbalances.length()") { value(2) }
            jsonPath("$.imbalances[0].axis") { value("EMOTIONAL_EXPRESSION") }
            jsonPath("$.imbalances[0].distance") { value(3) }
            jsonPath("$.imbalances[1].axis") { value("SPEECH_ARCHETYPE") }
            jsonPath("$.imbalances[1].distance") { value(3) }
            jsonPath("$.imbalanceNote") { exists() }
        }
    }

    @Test
    fun `OpenAPI 문서에 점검 API와 예상 좌표를 노출한다`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.paths['/api/v1/l0-characters/axis-balance-checks'].post") { exists() }
            jsonPath("$.components.schemas.L0AxisBalanceCheckRequest.properties.speechArchetypeId") { exists() }
            jsonPath("$.components.schemas.L0AxisBalanceCheckDto.properties.blocked") { exists() }
            jsonPath("$.components.schemas.AxisImbalanceDto.properties.distance") { exists() }
            jsonPath("$.components.schemas.AxisConflictDto.properties.reason") { exists() }
            jsonPath("$.components.schemas.TypeDescriptionOptionDto.properties.bigFiveVector") { exists() }
            jsonPath("$.components.schemas.SpeechArchetypeOptionDto.properties.bigFiveVector") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationOptionsDto.properties.axisConflicts") { exists() }
        }
    }

    private fun imbalancePlaceholderPrompt(): GenerationPrompt = GenerationPrompt(
        instructions = "system instruction",
        inputs = listOf(GenerationPromptInput(role = "user", inputText = "## 불균형 지정\n{{IMBALANCE_NOTE}}")),
    )

    private fun executableRefId(): Long {
        val template = promptTemplateRepository.saveAndFlush(
            PromptTemplate(name = "axis-balance-${UUID.randomUUID()}", type = PromptTemplateType.CHARACTER_GENERATION),
        )
        val ref = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = "main",
            instructions = "system instruction",
        ).also { it.addInput(role = "user", inputText = "## 불균형 지정\n{{IMBALANCE_NOTE}}") }
        return promptTemplateRefRepository.saveAndFlush(ref).id
    }

    private fun generationRequest(
        refId: Long,
        extraversionStage: Int,
        agreeablenessStage: Int,
        attachmentType: String,
        emotionalExpressionType: String,
        avoidanceStyleType: String,
        speechArchetypeName: String,
    ): L0CharacterGenerationRequest = L0CharacterGenerationRequest(
        refId = refId,
        opennessStageId = stageId(BigFiveTrait.OPENNESS, 3),
        conscientiousnessStageId = stageId(BigFiveTrait.CONSCIENTIOUSNESS, 3),
        extraversionStageId = stageId(BigFiveTrait.EXTRAVERSION, extraversionStage),
        agreeablenessStageId = stageId(BigFiveTrait.AGREEABLENESS, agreeablenessStage),
        neuroticismStageId = stageId(BigFiveTrait.NEUROTICISM, 3),
        attachmentStyleId = attachmentStyle(attachmentType).id,
        emotionalExpressionId = emotionalExpression(emotionalExpressionType).id,
        avoidanceStyleId = avoidanceStyle(avoidanceStyleType).id,
        speechArchetypeId = speechArchetype(speechArchetypeName).id,
    )

    private fun expressionAxisValue(
        axis: ExpressionAxis,
        name: String,
        openness: Int,
        conscientiousness: Int,
        extraversion: Int,
        agreeableness: Int,
        neuroticism: Int,
    ): ExpressionAxisValue = ExpressionAxisValue(
        axis = axis,
        name = name,
        vector = BigFiveVector(
            openness = openness,
            conscientiousness = conscientiousness,
            extraversion = extraversion,
            agreeableness = agreeableness,
            neuroticism = neuroticism,
        ),
    )

    private fun stageId(trait: BigFiveTrait, stage: Int): Long =
        assertNotNull(personalityTraitStageRepository.findByTraitAndStage(trait, stage)).id

    private fun attachmentStyle(type: String) = assertNotNull(attachmentStyleRepository.findByType(type))

    private fun emotionalExpression(type: String) = assertNotNull(emotionalExpressionRepository.findByType(type))

    private fun avoidanceStyle(type: String) = assertNotNull(avoidanceStyleRepository.findByType(type))

    private fun speechArchetype(name: String) = assertNotNull(speechArchetypeRepository.findByName(name))

    /**
     * 렌더러는 요청에서 축 식별자를 읽지 않고 options만 본다. 필수 항목을 채우기 위한 최소 요청이다.
     */
    private fun renderOnlyRequest() = L0CharacterGenerationRequest(
        refId = 1,
        attachmentStyleId = 1,
        emotionalExpressionId = 1,
        avoidanceStyleId = 1,
        speechArchetypeId = 1,
    )
}
