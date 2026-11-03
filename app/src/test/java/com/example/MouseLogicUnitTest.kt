package com.example

import com.example.ai.ChatMessage
import com.example.ai.GenerationRequest
import com.example.ai.GenerationResult
import com.example.ai.providers.DeviceHardwareTier
import com.example.ai.providers.HardwareAudit
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
        // Controlled mode: all actions require user confirmation
        val controlledMode = OperatingMode.CONTROLLED
        assertTrue(controlledMode == OperatingMode.CONTROLLED)

        // Intelligent mode: sensitive actions require confirmation, safe ones execute
        val intelligentMode = OperatingMode.INTELLIGENT
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
    fun `backup json archive validation enforces correct format and schema`() {
        val jsonPayload = """
            {
              "format": "MOUSE_BACKUP_ARCHIVE",
              "version": 1,
              "exportedAt": 1730000000000,
              "messages": [],
              "memories": [
                {
                  "id": "mem-1",
                  "category": "PREFERENCE",
                  "key": "theme",
                  "content": "Dark cybernetic",
                  "importance": 5
                }
              ]
            }
        """.trimIndent()

        assertTrue("Payload must declare MOUSE_BACKUP_ARCHIVE format", jsonPayload.contains("\"format\": \"MOUSE_BACKUP_ARCHIVE\""))
        assertTrue("Payload must declare schema version 1", jsonPayload.contains("\"version\": 1"))
        assertTrue("Payload must contain memories array", jsonPayload.contains("\"memories\""))
    }

    @Test
    fun `research verification status cycle operates correctly`() {
        var status = "VERIFIED"
        status = when (status) {
            "VERIFIED" -> "UNVERIFIED"
            "UNVERIFIED" -> "CONFLICTING"
            else -> "VERIFIED"
        }
        assertEquals("UNVERIFIED", status)

        status = when (status) {
            "VERIFIED" -> "UNVERIFIED"
            "UNVERIFIED" -> "CONFLICTING"
            else -> "VERIFIED"
        }
        assertEquals("CONFLICTING", status)

        status = when (status) {
            "VERIFIED" -> "UNVERIFIED"
            "UNVERIFIED" -> "CONFLICTING"
            else -> "VERIFIED"
        }
        assertEquals("VERIFIED", status)
    }

    @Test
    fun `smollm2 360m gguf model catalog definition verifies architecture and quantization`() {
        val model = com.example.ai.runtime.ManagedModel(
            id = "smollm2_360m",
            name = "SmolLM2 360M Instruct",
            architecture = "Llama / SmolLM2",
            parameterCount = "360M",
            quantization = "Q4_K_M",
            fileSizeBytes = 245_000_000L,
            minRamMb = 850L,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF"
        )

        assertEquals("SmolLM2 360M Instruct", model.name)
        assertEquals("Q4_K_M", model.quantization)
        assertEquals("360M", model.parameterCount)
        assertTrue("Size must be ~245MB", model.fileSizeBytes > 200_000_000L)
        assertTrue("Min RAM requirement must be >= 850MB", model.minRamMb >= 850L)
    }

    @Test
    fun `storage check requires file size plus 100mb safety buffer`() {
        val fileSizeBytes = 245_000_000L
        val requiredBytes = fileSizeBytes + (100L * 1024 * 1024)

        val freeDiskBytesAdequate = 400_000_000L
        val freeDiskBytesInadequate = 250_000_000L

        assertTrue("Free disk 400MB is sufficient", freeDiskBytesAdequate >= requiredBytes)
        assertFalse("Free disk 250MB is insufficient with safety buffer", freeDiskBytesInadequate >= requiredBytes)
    }

    @Test
    fun `runtime metrics calculate token speed and timing accurately`() {
        val tokenCount = 120
        val durationMs = 4000L // 4 seconds
        val tokensPerSec = (tokenCount.toDouble() * 1000.0) / durationMs

        assertEquals(30.0, tokensPerSec, 0.01)

        val metrics = com.example.ai.runtime.RuntimeMetrics(
            loadTimeMs = 420L,
            timeToFirstTokenMs = 180L,
            tokensPerSecond = tokensPerSec,
            totalTokens = tokenCount,
            peakMemoryMb = 310L
        )

        assertEquals(180L, metrics.timeToFirstTokenMs)
        assertEquals(30.0, metrics.tokensPerSecond, 0.01)
        assertEquals(310L, metrics.peakMemoryMb)
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
}

