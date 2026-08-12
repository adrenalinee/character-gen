package ltmp.charactergen.service.generation

import ltmp.charactergen.client.openai.L0CharacterLlmClient
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class L0GenerationExecutor(
    private val stateService: L0GenerationExecutionStateService,
    private val promptRenderer: L0CharacterPromptRenderer,
    private val llmClient: L0CharacterLlmClient,
) {
    fun executeNext(): Boolean {
        val generationId = stateService.claimNext() ?: return false
        val startedAt = System.nanoTime()
        try {
            val work = stateService.loadWork(generationId)
            val prompt = promptRenderer.render(work.prompt, work.request)
            val result = llmClient.generate(prompt)
            stateService.complete(
                generationId = generationId,
                response = result.character,
                rawJson = result.rawJson,
                elapsedMillis = elapsedSince(startedAt),
            )
        } catch (failure: Exception) {
            stateService.fail(generationId, failure, elapsedSince(startedAt))
        }
        return true
    }

    private fun elapsedSince(startedAt: Long): Long = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
}
