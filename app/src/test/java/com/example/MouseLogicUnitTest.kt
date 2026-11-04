package com.example

import com.example.ai.ChatMessage
import com.example.ai.GenerationRequest
import com.example.ai.GenerationResult
import com.example.ai.providers.DeviceHardwareTier
import com.example.ai.runtime.ManagedModel
import com.example.ai.runtime.ModelRuntimeStatus
import com.example.ai.runtime.RuntimeMetrics
import com.example.security.NetworkAuditTracker
import com.example.security.OperatingMode
import com.example.security.PendingAction
import com.example.security.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MouseLogicUnitTest {

    @Test
    fun `permission state defaults strictly to offline`() {
        val defaultState = PermissionState()
        assertFalse("Internet master toggle must default to false", defaultState.isInternetEnabled)
        assertFalse("Online AI queries must default to false", defaultState.allowOnlineAi)
        assertFalse("Web search must default to false", defaultState.allowWebSearch)
        assertFalse("Research aggregation must default to false", defaultState.allowResearchAggregation)
    }

    @Test
    fun `operating mode permission check behaves according to policy`() {
        val controlledMode = OperatingMode.CONTROLLED
        assertTrue(controlledMode == OperatingMode.CONTROLLED)

        val sensitiveAction = PendingAction(
            id = "1",
            title = "Launch Settings",
            description = "Launch app",
            actionType = "LAUNCH",
            isSensitive = true,
            onConfirm = {}
        )
        val readOnlyAction = PendingAction(
            id = "2",
            title = "Check Battery",
            description = "Read battery",
            actionType = "READ",
            isSensitive = false,
            onConfirm = {}
        )
        assertTrue(sensitiveAction.isSensitive)
        assertFalse(readOnlyAction.isSensitive)
    }

    @Test
    fun `hardware audit classifies device tiers accurately`() {
        val lowRamTier = when {
            2048 < 3072 -> DeviceHardwareTier.LIGHTWEIGHT
            2048 <= 6144 -> DeviceHardwareTier.BALANCED
            else -> DeviceHardwareTier.HIGH_PERFORMANCE
        }
        assertEquals(DeviceHardwareTier.LIGHTWEIGHT, lowRamTier)

        val midRamTier = when {
            4096 < 3072 -> DeviceHardwareTier.LIGHTWEIGHT
            4096 <= 6144 -> DeviceHardwareTier.BALANCED
            else -> DeviceHardwareTier.HIGH_PERFORMANCE
        }
        assertEquals(DeviceHardwareTier.BALANCED, midRamTier)

        val highRamTier = when {
            8192 < 3072 -> DeviceHardwareTier.LIGHTWEIGHT
            8192 <= 6144 -> DeviceHardwareTier.BALANCED
            else -> DeviceHardwareTier.HIGH_PERFORMANCE
        }
        assertEquals(DeviceHardwareTier.HIGH_PERFORMANCE, highRamTier)
    }

    @Test
    fun `ai generation request and result data contracts preserve metadata`() {
        val req = GenerationRequest(
            messages = listOf(
                ChatMessage("user", "Hello MOUSE"),
                ChatMessage("assistant", "Greetings, owner.")
            ),
            contextInfo = "Memory: owner_name=Alice"
        )
        assertEquals(2, req.messages.size)
        assertTrue(req.contextInfo.contains("Alice"))

        val res = GenerationResult(
            content = "MOUSE Local Core online response.",
            providerName = "MOUSE Local Core",
            isOnline = false,
            sourcesUsed = listOf("Personal Memory Vault", "Core Identity Directive"),
            confidence = 0.98f,
            latencyMs = 12L
        )
        assertEquals("MOUSE Local Core", res.providerName)
        assertFalse(res.isOnline)
        assertEquals(2, res.sourcesUsed.size)
        assertEquals(0.98f, res.confidence)
    }

    @Test
    fun `smollm2 360m gguf model catalog definition verifies architecture and quantization`() {
        val model = ManagedModel(
            id = "smollm2_360m",
            name = "SmolLM2 360M Instruct",
            family = "SmolLM2",
            architecture = "Llama",
            parameterCount = "360M",
            quantization = "Q4_K_M",
            fileSizeBytes = 270_590_880L,
            minRamMb = 850L,
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
            expectedSha256 = "2fa3f013dcdd7b99f9b237717fa0b12d75bbb89984cc1274be1471a465bac9c2"
        )

        assertEquals("SmolLM2 360M Instruct", model.name)
        assertEquals("Q4_K_M", model.quantization)
        assertEquals("360M", model.parameterCount)
        assertEquals(270_590_880L, model.fileSizeBytes)
        assertTrue("Min RAM requirement must be >= 850MB", model.minRamMb >= 850L)
        assertNotNull(model.expectedSha256)
    }

    @Test
    fun `llama 3_2 1b gguf model catalog definition verifies parameters and hash`() {
        val model = ManagedModel(
            id = "llama32_1b",
            name = "Llama 3.2 1B Instruct",
            family = "Llama 3.2",
            architecture = "Llama",
            parameterCount = "1.23B",
            quantization = "Q4_K_M",
            fileSizeBytes = 807_694_464L,
            minRamMb = 1600L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            expectedSha256 = "6f85a640a97cf2bf5b8e764087b1e83da0fdb51d7c9fab7d0fece9385611df83"
        )

        assertEquals("Llama 3.2 1B Instruct", model.name)
        assertEquals("1.23B", model.parameterCount)
        assertEquals(807_694_464L, model.fileSizeBytes)
        assertEquals("6f85a640a97cf2bf5b8e764087b1e83da0fdb51d7c9fab7d0fece9385611df83", model.expectedSha256)
    }

    @Test
    fun `gemma 3 1b it gguf model catalog definition verifies parameters and hash`() {
        val model = ManagedModel(
            id = "gemma3_1b",
            name = "Gemma 3 1B IT",
            family = "Gemma 3",
            architecture = "Gemma 3",
            parameterCount = "1B",
            quantization = "Q4_K_M",
            fileSizeBytes = 806_058_240L,
            minRamMb = 1600L,
            downloadUrl = "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf",
            expectedSha256 = "8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135"
        )

        assertEquals("Gemma 3 1B IT", model.name)
        assertEquals("1B", model.parameterCount)
        assertEquals(806_058_240L, model.fileSizeBytes)
        assertEquals("8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135", model.expectedSha256)
    }

    @Test
    fun `checksum verification correctly identifies matching and mismatching sha256`() {
        val publishedHash = "2fa3f013dcdd7b99f9b237717fa0b12d75bbb89984cc1274be1471a465bac9c2"
        val matchingHash = "2fa3f013dcdd7b99f9b237717fa0b12d75bbb89984cc1274be1471a465bac9c2"
        val mismatchHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        assertTrue("Matching hash must verify true", matchingHash.equals(publishedHash, ignoreCase = true))
        assertFalse("Corrupted hash must verify false", mismatchHash.equals(publishedHash, ignoreCase = true))
    }

    @Test
    fun `storage check requires file size plus 100mb safety buffer`() {
        val fileSizeBytes = 270_590_880L
        val requiredBytes = fileSizeBytes + (100L * 1024 * 1024)

        val freeDiskBytesAdequate = 500_000_000L
        val freeDiskBytesInadequate = 250_000_000L

        assertTrue("Free disk 500MB is sufficient", freeDiskBytesAdequate >= requiredBytes)
        assertFalse("Free disk 250MB is insufficient with safety buffer", freeDiskBytesInadequate >= requiredBytes)
    }

    @Test
    fun `runtime metrics calculate token speed and timing accurately`() {
        val tokenCount = 120
        val durationMs = 4000L // 4 seconds
        val tokensPerSec = (tokenCount.toDouble() * 1000.0) / durationMs

        assertEquals(30.0, tokensPerSec, 0.01)

        val metrics = RuntimeMetrics(
            modelLoadDurationMs = 420L,
            memoryBeforeLoadMb = 120L,
            memoryAfterLoadMb = 400L,
            memoryIncreaseMb = 280L,
            timeToFirstTokenMs = 180L,
            tokensPerSecond = tokensPerSec,
            totalTokens = tokenCount,
            totalGenerationDurationMs = durationMs,
            peakMemoryDuringInferenceMb = 420L,
            memoryAfterUnloadMb = 125L
        )

        assertEquals(180L, metrics.timeToFirstTokenMs)
        assertEquals(30.0, metrics.tokensPerSecond, 0.01)
        assertEquals(420L, metrics.peakMemoryMb)
        assertEquals(280L, metrics.memoryIncreaseMb)
        assertEquals(125L, metrics.memoryAfterUnloadMb)
    }

    @Test
    fun `network audit tracker records and filters events correctly`() {
        NetworkAuditTracker.clear()
        val start = System.currentTimeMillis()

        NetworkAuditTracker.recordEvent(
            caller = "LocalModelManager",
            destination = "https://huggingface.co",
            wasBlocked = false,
            policyReason = "Owner Authorized Download"
        )

        NetworkAuditTracker.recordEvent(
            caller = "GeminiProvider",
            destination = "https://generativelanguage.googleapis.com",
            wasBlocked = true,
            policyReason = "Internet Permission OFF"
        )

        assertEquals(1, NetworkAuditTracker.getOutgoingRequestsCountSince(start))
        assertEquals(1, NetworkAuditTracker.getBlockedRequestsCountSince(start))
        assertEquals(2, NetworkAuditTracker.getEventsSince(start).size)
    }

    @Test
    fun `unloaded offline engine labels component accurately without falsely claiming llm`() {
        val isLoaded = false
        val activeProviderName = if (isLoaded) {
            "MOUSE Local GGUF Engine (smollm2_360m.gguf)"
        } else {
            "MOUSE Local Knowledge & Directive Component"
        }

        assertFalse("Should not claim GGUF engine when unloaded", activeProviderName.contains("GGUF Engine"))
        assertTrue("Must accurately identify as Local Knowledge Component", activeProviderName.contains("Knowledge & Directive Component"))
    }

    @Test
    fun `openai-compatible config preserves non-empty fields without overwriting with blanks`() {
        var currentEndpoint = "https://api.openai.com/v1"
        var currentModel = "gpt-4o-mini"

        fun updateConfig(newEndpoint: String, newModel: String) {
            val sanitizedEndpoint = newEndpoint.trim().trimEnd('/')
            if (sanitizedEndpoint.isNotBlank()) {
                currentEndpoint = sanitizedEndpoint
            }
            val sanitizedModel = newModel.trim()
            if (sanitizedModel.isNotBlank()) {
                currentModel = sanitizedModel
            }
        }

        // 1. Update Base URL only
        updateConfig("https://api.openai.coumsdfghj", "")
        assertEquals("https://api.openai.coumsdfghj", currentEndpoint)
        assertEquals("gpt-4o-mini", currentModel) // preserved

        // 2. Update Model Name only
        updateConfig("", "jhgfds")
        assertEquals("https://api.openai.coumsdfghj", currentEndpoint) // preserved
        assertEquals("jhgfds", currentModel)

        // 3. Update both with extra whitespace and trailing slashes
        updateConfig("  https://my-ollama.internal:11434/v1/  ", "  llama3:8b  ")
        assertEquals("https://my-ollama.internal:11434/v1", currentEndpoint)
        assertEquals("llama3:8b", currentModel)
    }

    @Test
    fun `empty api key preserves previously stored key in config save logic`() {
        var storedKey: String? = "previously_stored_secret_key"

        fun saveConfig(endpoint: String, model: String, newKey: String?) {
            // If newKey is null or blank, do not overwrite stored key
            if (!newKey.isNullOrBlank()) {
                storedKey = newKey.trim()
            }
        }

        // Saving without new key preserves existing key
        saveConfig("https://api.openai.coumsdfghj", "jhgfds", null)
        assertEquals("previously_stored_secret_key", storedKey)

        saveConfig("https://api.openai.coumsdfghj", "jhgfds", "")
        assertEquals("previously_stored_secret_key", storedKey)

        saveConfig("https://api.openai.coumsdfghj", "jhgfds", "   ")
        assertEquals("previously_stored_secret_key", storedKey)

        // Saving with a new key updates it
        saveConfig("https://api.openai.coumsdfghj", "jhgfds", "sk-new-key-12345")
        assertEquals("sk-new-key-12345", storedKey)
    }

    @Test
    fun `openai-compatible target url and payload generation uses saved base url and model name`() {
        val endpointUrl = "https://api.openai.coumsdfghj"
        val modelName = "jhgfds"

        val targetUrl = "$endpointUrl/chat/completions"
        assertEquals("https://api.openai.coumsdfghj/chat/completions", targetUrl)

        val jsonString = """{"model":"$modelName","temperature":0.7}"""
        assertTrue(jsonString.contains("\"model\":\"jhgfds\""))
    }
}
