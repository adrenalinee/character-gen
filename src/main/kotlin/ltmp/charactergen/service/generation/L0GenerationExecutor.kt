package ltmp.charactergen.service.generation

import ltmp.charactergen.client.openai.L0CharacterLlmClient
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class L0GenerationExecutor(
    private val stateService: L0GenerationExecutionStateService,
    private val promptRenderer: L0CharacterPromptRenderer,
    private val llmClient: L0CharacterLlmClient,
    private val detailSelector: RuntimeConcreteDetailSelector,
) {
    fun execute(generationId: Long) {
        val startedAt = System.nanoTime()
        try {
            if (!stateService.markRunning(generationId)) return
            val work = stateService.loadWork(generationId)
            val prompt = promptRenderer.render(work.prompt, work.request, work.options)
            val result = llmClient.generate(prompt)
            // 임베딩 호출은 complete() 밖에서 한다. complete()는 트랜잭션이라 그 안에서 네트워크를 타면
            // 응답을 기다리는 동안 DB 커넥션을 붙잡게 된다. (v4.1 B-10)
            val runtimeDetailIndexes = detailSelector.select(result.character)
            stateService.complete(
                generationId = generationId,
                response = result.character,
                rawJson = result.rawJson,
                runtimeDetailIndexes = runtimeDetailIndexes,
                elapsedMillis = elapsedSince(startedAt),
            )
        } catch (failure: Exception) {
            stateService.fail(generationId, failure, elapsedSince(startedAt))
        }
    }

    fun failBeforeExecution(generationId: Long, failure: Exception) =
        stateService.fail(generationId, failure, 0)

    private fun elapsedSince(startedAt: Long): Long = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
}
