package ltmp.charactergen.client.openai

import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.service.generation.GenerationPrompt

interface L0CharacterLlmClient {
    fun generate(prompt: GenerationPrompt): L0CharacterLlmResult
}

data class L0CharacterLlmResult(
    val character: L0CharacterResponse,
    val rawJson: String,
)
