package com.example.ai

data class ChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String
)

data class GenerationRequest(
    val messages: List<ChatMessage>,
    val contextInfo: String = "",
    val systemPromptOverride: String? = null,
    val temperature: Float = 0.7f
)

data class GenerationResult(
    val content: String,
    val providerName: String,
    val isOnline: Boolean,
    val sourcesUsed: List<String> = emptyList(),
    val confidence: Float = 1.0f,
    val latencyMs: Long = 0L,
    val error: String? = null
)

data class ProviderAvailability(
    val isAvailable: Boolean,
    val reason: String,
    val latencyMs: Long = 0L
)

interface AiProvider {
    val id: String
    val name: String
    val isOnline: Boolean

    suspend fun checkAvailability(): ProviderAvailability
    suspend fun generate(request: GenerationRequest): GenerationResult
}
