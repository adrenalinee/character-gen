package ltmp.charactergen.service.generation

data class GenerationPrompt(
    val instructions: String,
    val inputs: List<GenerationPromptInput>,
)

data class GenerationPromptInput(
    val role: String,
    val inputText: String,
)

data class PromptRefSnapshot(
    val instructions: String,
    val inputs: List<PromptRefInputSnapshot>,
)

data class PromptRefInputSnapshot(
    val role: String,
    val inputText: String,
)
