package com.example.ai.providers

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.example.ai.AiProvider
import com.example.ai.ChatMessage
import com.example.ai.GenerationRequest
import com.example.ai.GenerationResult
import com.example.ai.ProviderAvailability
import com.example.ai.runtime.LocalGgufRuntime
import com.example.ai.runtime.LocalModelManager
import com.example.ai.runtime.ModelRuntimeStatus
import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.ResearchDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class HardwareAudit(
    val totalRamMb: Long,
    val availRamMb: Long,
    val cpuCores: Int,
    val primaryAbi: String,
    val freeStorageMb: Long,
    val androidVersion: String,
    val tier: DeviceHardwareTier,
    val recommendedModel: String
)

enum class DeviceHardwareTier {
    LIGHTWEIGHT,    // < 3GB RAM -> 0.3B - 0.5B models (e.g. SmolLM2 360M Q4)
    BALANCED,       // 3GB - 6GB RAM -> 1B - 1.5B models (e.g. Llama 3.2 1B / Gemma 3 1B)
    HIGH_PERFORMANCE// > 6GB RAM -> 3B+ models
}

class LocalInferenceProvider(
    private val context: Context,
    private val memoryDao: MemoryDao,
    private val knowledgeDao: KnowledgeDao,
    private val researchDao: ResearchDao,
    val modelManager: LocalModelManager = LocalModelManager(context),
    val runtime: LocalGgufRuntime = modelManager.runtime
) : AiProvider {

    override val id: String = "local_core"
    override val name: String
        get() = if (runtime.isModelLoaded) {
            "MOUSE Local GGUF Engine (${runtime.activeModelName})"
        } else {
            "MOUSE Local Knowledge & Directive Component"
        }

    override val isOnline: Boolean = false

    fun auditHardware(): HardwareAudit {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)

        val tier = when {
            totalRamMb < 3072 -> DeviceHardwareTier.LIGHTWEIGHT
            totalRamMb <= 6144 -> DeviceHardwareTier.BALANCED
            else -> DeviceHardwareTier.HIGH_PERFORMANCE
        }

        val recommendedModel = when (tier) {
            DeviceHardwareTier.LIGHTWEIGHT -> "SmolLM2 360M Instruct"
            DeviceHardwareTier.BALANCED -> "Llama 3.2 1B Instruct / Gemma 3 1B IT"
            DeviceHardwareTier.HIGH_PERFORMANCE -> "Llama 3.2 1B Instruct"
        }

        return HardwareAudit(
            totalRamMb = totalRamMb,
            availRamMb = availRamMb,
            cpuCores = cpuCores,
            primaryAbi = primaryAbi,
            freeStorageMb = freeStorageMb,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            tier = tier,
            recommendedModel = recommendedModel
        )
    }

    override suspend fun checkAvailability(): ProviderAvailability {
        val isLoaded = runtime.isModelLoaded
        val audit = auditHardware()
        return ProviderAvailability(
            isAvailable = true,
            reason = if (isLoaded) {
                "Active GGUF Model: ${runtime.activeModelName} (${audit.availRamMb} MB RAM free)"
            } else {
                "Local Knowledge Component Active (${audit.cpuCores} cores, ${audit.availRamMb} MB RAM free)"
            },
            latencyMs = if (isLoaded) 15L else 5L
        )
    }

    override suspend fun generate(request: GenerationRequest): GenerationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val userQuery = request.messages.lastOrNull { it.role == "user" }?.content?.trim() ?: ""

        val sourcesUsed = mutableListOf<String>()

        // Layer A: Search local knowledge, memory, and research databases
        val relevantMemories = try {
            memoryDao.searchMemories(userQuery.take(30)).take(3)
        } catch (e: Exception) {
            emptyList()
        }

        val relevantKnowledge = try {
            knowledgeDao.searchKnowledge(userQuery.take(30)).take(3)
        } catch (e: Exception) {
            emptyList()
        }

        val relevantResearch = try {
            researchDao.searchResearch(userQuery.take(30)).take(2)
        } catch (e: Exception) {
            emptyList()
        }

        for (m in relevantMemories) sourcesUsed.add("Memory: ${m.key}")
        for (k in relevantKnowledge) sourcesUsed.add("Knowledge: ${k.title}")
        for (r in relevantResearch) sourcesUsed.add("Research: ${r.question}")

        // Check if a genuine GGUF Neural Model is currently loaded
        if (runtime.isModelLoaded) {
            val activeModel = modelManager.modelsState.value.firstOrNull {
                it.status == ModelRuntimeStatus.LOADED_ACTIVE || it.status == ModelRuntimeStatus.ACTIVELY_GENERATING
            }
            val activeModelName = runtime.activeModelName ?: "GGUF Model"
            sourcesUsed.add("Local GGUF Neural Model: $activeModelName")

            val systemDirective = buildSystemDirective(relevantMemories, relevantKnowledge)
            val prompt = formatChatPrompt(activeModel?.id, systemDirective, request.messages)

            return@withContext try {
                val (rawText, metrics) = runtime.generateSync(
                    prompt = prompt,
                    maxTokens = 256,
                    temperature = request.temperature
                )

                // Clean family-specific stop tokens
                val cleanText = cleanStopTokens(rawText, activeModel?.id)
                val latency = System.currentTimeMillis() - startTime

                // Update active model benchmark metrics
                activeModel?.id?.let { modelId ->
                    modelManager.updateActiveGenerationMetrics(modelId, metrics.peakMemoryMb, metrics.tokensPerSecond)
                }

                GenerationResult(
                    content = cleanText,
                    providerName = name,
                    isOnline = false,
                    sourcesUsed = sourcesUsed,
                    confidence = 0.98f,
                    latencyMs = latency
                )
            } catch (e: Exception) {
                Log.e("LocalInferenceProvider", "Neural generation failed, falling back to local reasoning: ${e.message}")
                fallbackKnowledgeGeneration(userQuery, sourcesUsed, startTime)
            }
        }

        // When no GGUF binary is loaded into memory, use local knowledge retrieval and reasoning component
        fallbackKnowledgeGeneration(userQuery, sourcesUsed, startTime)
    }

    private fun buildSystemDirective(
        memories: List<com.example.data.local.entities.MemoryEntity>,
        knowledge: List<com.example.data.local.entities.KnowledgeEntity>
    ): String {
        return buildString {
            append("You are MOUSE (My Own Ultimate Smart Entity), a personalized, honest, and security-conscious AI assistant running 100% on-device on Android.\n")
            if (memories.isNotEmpty()) {
                append("RECALLED OWNER MEMORIES:\n")
                for (m in memories) {
                    append("- [${m.key}]: ${m.content}\n")
                }
            }
            if (knowledge.isNotEmpty()) {
                append("LOCAL KNOWLEDGE CONTEXT:\n")
                for (k in knowledge) {
                    append("- [${k.title}]: ${k.content}\n")
                }
            }
        }
    }

    private fun formatChatPrompt(modelId: String?, systemDirective: String, messages: List<ChatMessage>): String {
        val historyTurns = messages.takeLast(8)
        return when (modelId) {
            "llama32_1b" -> {
                // Llama 3 prompt format
                buildString {
                    append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n")
                    append(systemDirective)
                    append("<|eot_id|>")
                    for (msg in historyTurns) {
                        append("<|start_header_id|>${msg.role}<|end_header_id|>\n\n")
                        append(msg.content)
                        append("<|eot_id|>")
                    }
                    append("<|start_header_id|>assistant<|end_header_id|>\n\n")
                }
            }
            "gemma3_1b" -> {
                // Gemma 3 prompt format
                buildString {
                    append("<start_of_turn>user\n")
                    append("[SYSTEM INSTRUCTION]\n")
                    append(systemDirective)
                    append("\n[END SYSTEM INSTRUCTION]\n\n")
                    for ((index, msg) in historyTurns.withIndex()) {
                        if (index == 0 && msg.role == "user") {
                            append(msg.content)
                            append("<end_of_turn>\n")
                        } else {
                            val roleTag = if (msg.role == "user") "user" else "model"
                            append("<start_of_turn>$roleTag\n${msg.content}<end_of_turn>\n")
                        }
                    }
                    append("<start_of_turn>model\n")
                }
            }
            else -> {
                // Default ChatML format (SmolLM2 / Qwen)
                buildString {
                    append("<|im_start|>system\n")
                    append(systemDirective)
                    append("<|im_end|>\n")
                    for (msg in historyTurns) {
                        append("<|im_start|>${msg.role}\n${msg.content}<|im_end|>\n")
                    }
                    append("<|im_start|>assistant\n")
                }
            }
        }
    }

    private fun cleanStopTokens(rawText: String, modelId: String?): String {
        var text = rawText
        val stopTokens = listOf("<|im_end|>", "<|eot_id|>", "<|end_of_text|>", "<end_of_turn>", "</s>")
        for (token in stopTokens) {
            text = text.substringBefore(token)
        }
        return text.trim()
    }

    private fun fallbackKnowledgeGeneration(
        userQuery: String,
        sourcesUsed: MutableList<String>,
        startTime: Long
    ): GenerationResult {
        val lower = userQuery.lowercase(Locale.ROOT)
        val audit = auditHardware()

        val responseText = when {
            // Identity and core purpose
            lower.contains("who are you") || lower.contains("what is mouse") || lower.contains("what are you") -> {
                sourcesUsed.add("Identity Directive")
                "I am **MOUSE** (My Own Ultimate Smart Entity) — your personal, offline-first digital assistant.\n\n" +
                "• **Single Central Identity:** Consistent personality, reasoning, and memory across all internal models.\n" +
                "• **Offline-First:** All network access is OFF by default. I prioritize your privacy and local autonomy.\n" +
                "• **Layered Intelligence:** I combine persistent personal memory, local structured knowledge, and on-device inference.\n" +
                "• **Owner Controlled:** You control the Permission Gate, operating mode, and credentials."
            }

            // Hardware & System Audit
            lower.contains("hardware") || lower.contains("device specs") || lower.contains("system info") -> {
                sourcesUsed.add("Device Hardware Telemetry")
                "**Device Hardware Evaluation:**\n\n" +
                "• **Platform:** ${audit.androidVersion}\n" +
                "• **CPU Cores:** ${audit.cpuCores} active execution cores\n" +
                "• **Primary ABI:** ${audit.primaryAbi}\n" +
                "• **Total RAM:** ${audit.totalRamMb} MB\n" +
                "• **Available RAM:** ${audit.availRamMb} MB\n" +
                "• **Free Internal Storage:** ${audit.freeStorageMb} MB\n" +
                "• **Hardware Tier:** ${audit.tier.name}\n" +
                "• **Recommended Quantized Model:** `${audit.recommendedModel}`"
            }

            // Greeting
            lower.matches(Regex("^(hi|hello|hey|greetings|good morning|good evening).*")) -> {
                "Hello. MOUSE is active and operational in offline-first mode.\n\n" +
                "I am ready to query your memory vault, retrieve local knowledge, assist with device tasks, or run on-device GGUF inference when a model is loaded."
            }

            // Honest fallback when no local LLM is loaded
            else -> {
                "I have processed your query: *\"$userQuery\"* using **MOUSE Local Knowledge Component**.\n\n" +
                "• **Neural LLM Status:** No GGUF model is currently loaded in memory.\n" +
                "• **Available Candidates:** You can download and load a model (**SmolLM2 360M**, **Llama 3.2 1B**, or **Gemma 3 1B**) from the **Hardware & Local AI** screen for full on-device neural text generation.\n" +
                "• **Network Status:** Internet access is currently **OFF** (enforced by owner security policy)."
            }
        }

        val latency = System.currentTimeMillis() - startTime
        return GenerationResult(
            content = responseText,
            providerName = "MOUSE Local Knowledge & Directive Component (Offline)",
            isOnline = false,
            sourcesUsed = sourcesUsed,
            confidence = if (sourcesUsed.isNotEmpty()) 0.95f else 0.85f,
            latencyMs = latency
        )
    }
}
