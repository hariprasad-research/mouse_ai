package com.example.ai.providers

import android.content.Context
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

class OpenAiCompatibleProvider(
    context: Context,
    private val keystoreManager: KeystoreManager,
    private val permissionGate: PermissionGate
) : AiProvider {

    override val id: String = "openai_compatible"
    override val name: String = "OpenAI-Compatible Endpoint"
    override val isOnline: Boolean = true

    private val prefs = context.getSharedPreferences("mouse_openai_prefs", Context.MODE_PRIVATE)

    var endpointUrl: String
        get() = prefs.getString("endpoint_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
        set(value) = prefs.edit().putString("endpoint_url", value.trimEnd('/')).apply()

    var modelName: String
        get() = prefs.getString("model_name", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString("model_name", value.trim()).apply()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    override suspend fun checkAvailability(): ProviderAvailability = withContext(Dispatchers.IO) {
        if (!permissionGate.isInternetEnabled()) {
            return@withContext ProviderAvailability(
                isAvailable = false,
                reason = "Blocked by Internet Permission Gate (Offline Policy active)"
            )
        }
        val key = keystoreManager.getApiKey(id)
        if (key.isBlank() && !endpointUrl.contains("localhost") && !endpointUrl.contains("10.0.2.2")) {
            return@withContext ProviderAvailability(
                isAvailable = false,
                reason = "API Key not configured in Keystore"
            )
        }
        ProviderAvailability(
            isAvailable = true,
            reason = "Configured: $modelName @ $endpointUrl"
        )
    }

    override suspend fun generate(request: GenerationRequest): GenerationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            permissionGate.assertNetworkAllowed("OpenAI-Compatible Provider")
        } catch (e: SecurityException) {
            return@withContext GenerationResult(
                content = "Request blocked: Internet permission is OFF.",
                providerName = name,
                isOnline = true,
                error = e.message
            )
        }

        val apiKey = keystoreManager.getApiKey(id)
        val messagesArray = JSONArray()

        // System message with MOUSE identity and local retrieved context
        val systemPrompt = buildString {
            append("You are MOUSE (My Own Ultimate Smart Entity) — a personalized, honest, and security-conscious AI assistant.\n")
            if (request.contextInfo.isNotBlank()) {
                append("\n[RECALLED LOCAL CONTEXT]:\n")
                append(request.contextInfo)
            }
        }
        messagesArray.put(JSONObject().put("role", "system").put("content", systemPrompt))

        for (m in request.messages) {
            messagesArray.put(JSONObject().put("role", m.role).put("content", m.content))
        }

        val payload = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesArray)
            put("temperature", request.temperature)
        }

        val targetUrl = "$endpointUrl/chat/completions"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toString().toRequestBody(mediaType)

        val reqBuilder = Request.Builder()
            .url(targetUrl)
            .post(requestBody)

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        try {
            val response = client.newCall(reqBuilder.build()).execute()
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GenerationResult(
                    content = "Server returned error ${response.code}.",
                    providerName = name,
                    isOnline = true,
                    latencyMs = latency,
                    error = "HTTP_${response.code}: $bodyString"
                )
            }

            val respJson = JSONObject(bodyString)
            val choices = respJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext GenerationResult(
                    content = "Empty response received from endpoint.",
                    providerName = name,
                    isOnline = true,
                    latencyMs = latency,
                    error = "NO_CHOICES"
                )
            }

            val messageObj = choices.getJSONObject(0).getJSONObject("message")
            val text = messageObj.getString("content")

            GenerationResult(
                content = text.trim(),
                providerName = "$name ($modelName)",
                isOnline = true,
                sourcesUsed = listOf("Custom Endpoint: $endpointUrl"),
                confidence = 0.95f,
                latencyMs = latency
            )
        } catch (e: Exception) {
            GenerationResult(
                content = "Error contacting OpenAI-compatible endpoint: ${e.localizedMessage}",
                providerName = name,
                isOnline = true,
                latencyMs = System.currentTimeMillis() - startTime,
                error = e.localizedMessage
            )
        }
    }
}
