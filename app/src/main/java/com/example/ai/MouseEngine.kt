package com.example.ai

import android.content.Context
import com.example.ai.providers.GeminiProvider
import com.example.ai.providers.LocalInferenceProvider
import com.example.ai.providers.OpenAiCompatibleProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.security.KeystoreManager
import com.example.security.PermissionGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

class MouseEngine(
    private val context: Context,
    val database: AppDatabase,
    val permissionGate: PermissionGate,
    val keystoreManager: KeystoreManager
) {
    val localProvider = LocalInferenceProvider(
        context,
        database.memoryDao(),
        database.knowledgeDao(),
        database.researchDao()
    )

    val geminiProvider = GeminiProvider(
        keystoreManager,
        permissionGate
    )

    val openAiProvider = OpenAiCompatibleProvider(
        context,
        keystoreManager,
        permissionGate
    )

    private val prefs = context.getSharedPreferences("mouse_engine_prefs", Context.MODE_PRIVATE)

    private val _selectedProviderId = MutableStateFlow(
        prefs.getString("preferred_provider", "local_core") ?: "local_core"
    )
    val selectedProviderId: StateFlow<String> = _selectedProviderId.asStateFlow()

    fun setPreferredProvider(providerId: String) {
        _selectedProviderId.value = providerId
        prefs.edit().putString("preferred_provider", providerId).apply()
    }

    fun getActiveProvider(): AiProvider {
        // If Internet is OFF, strictly enforce Local Core provider
        if (!permissionGate.isInternetEnabled()) {
            return localProvider
        }
        return when (_selectedProviderId.value) {
            "gemini" -> geminiProvider
            "openai_compatible" -> openAiProvider
            else -> localProvider
        }
    }

    /**
     * Executes the central intelligence pipeline:
     * User Input -> Intent Understanding -> Permission Check -> Memory Retrieval ->
     * Knowledge Retrieval -> Provider Selection -> Response Generation ->
     * Response Validation -> Memory/History Update -> User Response
     */
    suspend fun processUserMessage(
        conversationId: String,
        userText: String
    ): MessageEntity = withContext(Dispatchers.IO) {
        val trimmed = userText.trim()
        val now = System.currentTimeMillis()

        // 1. Record User Message
        val userMessage = MessageEntity(
            conversationId = conversationId,
            role = "user",
            content = trimmed,
            timestamp = now
        )
        database.messageDao().insertMessage(userMessage)

        // 2. Intent Understanding (Direct memory commands or system operations)
        val lower = trimmed.lowercase(Locale.ROOT)

        // Memory Command: "Remember that [fact]"
        if (lower.startsWith("remember that ") || lower.startsWith("remember: ")) {
            val fact = trimmed.substringAfter("remember that ").substringAfter("remember: ").trim()
            val key = fact.take(24).replace(Regex("[^a-zA-Z0-9 ]"), "").trim().replace(" ", "_").lowercase()
            val newMemory = MemoryEntity(
                category = "PERSONAL_FACT",
                key = key.ifBlank { "user_note_${System.currentTimeMillis() % 1000}" },
                content = fact,
                importance = 5
            )
            database.memoryDao().insertMemory(newMemory)

            val reply = "Affirmative. I have saved this permanently to your **Personal Memory Vault**:\n\n" +
                    "• **Key:** `${newMemory.key}`\n" +
                    "• **Content:** \"$fact\"\n\n" +
                    "This is stored strictly on-device in your local encrypted Room database."
            val assistantMsg = MessageEntity(
                conversationId = conversationId,
                role = "assistant",
                content = reply,
                providerUsed = "MOUSE Memory Subsystem",
                isOnline = false,
                timestamp = System.currentTimeMillis(),
                sourcesJson = "[\"Personal Memory Store\"]",
                confidence = 1.0f
            )
            database.messageDao().insertMessage(assistantMsg)
            return@withContext assistantMsg
        }

        // Memory Command: "Forget [fact/key]"
        if (lower.startsWith("forget ") || lower.startsWith("delete memory ")) {
            val target = trimmed.substringAfter("forget ").substringAfter("delete memory ").trim()
            val matched = database.memoryDao().searchMemories(target)
            if (matched.isNotEmpty()) {
                for (m in matched) {
                    database.memoryDao().deleteMemory(m.id)
                }
                val reply = "Confirmed. I have removed ${matched.size} matching record(s) from your memory vault regarding \"$target\"."
                val assistantMsg = MessageEntity(
                    conversationId = conversationId,
                    role = "assistant",
                    content = reply,
                    providerUsed = "MOUSE Memory Subsystem",
                    isOnline = false,
                    timestamp = System.currentTimeMillis(),
                    sourcesJson = "[\"Memory Removal Confirmation\"]",
                    confidence = 1.0f
                )
                database.messageDao().insertMessage(assistantMsg)
                return@withContext assistantMsg
            }
        }

        // 3. Memory & Knowledge Retrieval
        val relevantMemories = try {
            database.memoryDao().searchMemories(trimmed.take(35)).take(4)
        } catch (e: Exception) {
            emptyList()
        }

        val relevantKnowledge = try {
            database.knowledgeDao().searchKnowledge(trimmed.take(35)).take(3)
        } catch (e: Exception) {
            emptyList()
        }

        val contextInfo = buildString {
            if (relevantMemories.isNotEmpty()) {
                append("User Personal Memories:\n")
                for (m in relevantMemories) {
                    append("- [${m.category}] ${m.key}: ${m.content}\n")
                }
            }
            if (relevantKnowledge.isNotEmpty()) {
                append("Local Knowledge Base:\n")
                for (k in relevantKnowledge) {
                    append("- ${k.title}: ${k.content}\n")
                }
            }
        }

        // 4. Provider Selection & Fallback Handling
        val activeProvider = getActiveProvider()
        val recentMessages = database.messageDao().getMessagesList(conversationId).takeLast(10).map {
            ChatMessage(role = it.role, content = it.content)
        }

        val genRequest = GenerationRequest(
            messages = recentMessages,
            contextInfo = contextInfo
        )

        var genResult = activeProvider.generate(genRequest)

        // If online provider encountered an error or failed, cleanly fall back to Local Core
        var providerDisplayName = activeProvider.name
        var isOnlineUsed = activeProvider.isOnline
        if (genResult.error != null && activeProvider.isOnline) {
            val fallbackResult = localProvider.generate(genRequest)
            genResult = fallbackResult.copy(
                content = "*(Note: Online provider ${activeProvider.name} failed: ${genResult.error}. Fell back to MOUSE Local Core.)*\n\n${fallbackResult.content}"
            )
            providerDisplayName = "MOUSE Local Core (Fallback)"
            isOnlineUsed = false
        }

        // 5. Response Validation (Strict Honesty Check)
        var validatedContent = genResult.content
        if (!permissionGate.isInternetEnabled() && isOnlineUsed) {
            // Safety guard: network should never have been reported as online if internet was disabled
            isOnlineUsed = false
            providerDisplayName = localProvider.name
        }

        // 6. Record and Save Assistant Response
        val assistantMessage = MessageEntity(
            conversationId = conversationId,
            role = "assistant",
            content = validatedContent,
            providerUsed = providerDisplayName,
            isOnline = isOnlineUsed,
            timestamp = System.currentTimeMillis(),
            sourcesJson = "[${genResult.sourcesUsed.joinToString(",") { "\"$it\"" }}]",
            confidence = genResult.confidence
        )
        database.messageDao().insertMessage(assistantMessage)

        // Update conversation timestamp
        val conv = database.conversationDao().getConversationById(conversationId)
        if (conv != null) {
            database.conversationDao().updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }

        assistantMessage
    }
}
