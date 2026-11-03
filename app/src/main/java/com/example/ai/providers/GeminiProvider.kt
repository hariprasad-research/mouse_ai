package com.example.ai.providers

import com.example.BuildConfig
import com.example.ai.AiProvider
import com.example.ai.GenerationRequest
import com.example.ai.GenerationResult
import com.example.ai.ProviderAvailability
import com.example.security.KeystoreManager
import com.example.security.PermissionGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val keystoreManager: KeystoreManager,
    private val permissionGate: PermissionGate
) : AiProvider {

    override val id: String = "gemini"
    override val name: String = "Google Gemini 1.5 Flash"
    override val isOnline: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun resolveApiKey(): String {
        val storedKey = keystoreManager.getApiKey(id)
        if (storedKey.isNotBlank()) return storedKey
        // Fallback to BuildConfig if provided by environment
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val buildKey = field.get(null) as? String ?: ""
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun checkAvailability(): ProviderAvailability = withContext(Dispatchers.IO) {
        if (!permissionGate.isInternetEnabled()) {
            return@withContext ProviderAvailability(
                isAvailable = false,
                reason = "Blocked by Internet Permission Gate (Offline Policy active)"
            )
        }
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            return@withContext ProviderAvailability(
                isAvailable = false,
                reason = "Gemini API key not configured in Keystore"
            )
        }
        ProviderAvailability(
            isAvailable = true,
            reason = "Online & Configured"
        )
    }

    override suspend fun generate(request: GenerationRequest): GenerationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Strict Owner Permission Check
        try {
            permissionGate.assertNetworkAllowed("Gemini AI Provider")
        } catch (e: SecurityException) {
            return@withContext GenerationResult(
                content = "Request blocked: Internet permission is OFF.",
                providerName = name,
                isOnline = true,
                latencyMs = 0L,
                error = e.message
            )
        }

        // 2. Resolve Key
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            return@withContext GenerationResult(
                content = "Gemini API Key missing. Please configure your key in Provider Settings.",
                providerName = name,
                isOnline = true,
                error = "API_KEY_MISSING"
            )
        }

        // 3. Build JSON payload
        val contentsArray = JSONArray()

        // Include system prompt & context
        val contextPrompt = buildString {
            append("You are MOUSE (My Own Ultimate Smart Entity) — a personalized, honest, and security-conscious AI assistant.\n")
            if (request.contextInfo.isNotBlank()) {
                append("\n[RECALLED LOCAL MEMORY & KNOWLEDGE]:\n")
                append(request.contextInfo)
                append("\n")
            }
        }

        val promptBuilder = StringBuilder()
        promptBuilder.append(contextPrompt).append("\n\n")

        for (msg in request.messages) {
            promptBuilder.append("${msg.role.uppercase()}: ${msg.content}\n")
        }
        promptBuilder.append("ASSISTANT: ")

        val textPart = JSONObject().put("text", promptBuilder.toString())
        val partsArray = JSONArray().put(textPart)
        val contentObj = JSONObject().put("parts", partsArray)
        contentsArray.put(contentObj)

        val requestJson = JSONObject().put("contents", contentsArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GenerationResult(
                    content = "Gemini API returned error code ${response.code}.",
                    providerName = name,
                    isOnline = true,
                    latencyMs = latency,
                    error = "HTTP_${response.code}: $responseBody"
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GenerationResult(
                    content = "No response generated by Gemini model.",
                    providerName = name,
                    isOnline = true,
                    latencyMs = latency,
                    error = "NO_CANDIDATE"
                )
            }

            val candidate = candidates.getJSONObject(0)
            val contentParts = candidate.getJSONObject("content").getJSONArray("parts")
            val generatedText = contentParts.getJSONObject(0).getString("text")

            GenerationResult(
                content = generatedText.trim(),
                providerName = name,
                isOnline = true,
                sourcesUsed = listOf("Google Gemini 1.5 Flash Cloud"),
                confidence = 0.95f,
                latencyMs = latency
            )
        } catch (e: Exception) {
            GenerationResult(
                content = "Failed to communicate with Gemini API: ${e.localizedMessage}",
                providerName = name,
                isOnline = true,
                latencyMs = System.currentTimeMillis() - startTime,
                error = e.localizedMessage
            )
        }
    }
}
