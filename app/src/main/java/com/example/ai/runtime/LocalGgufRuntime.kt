package com.example.ai.runtime

import android.os.Debug
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.ladenthin.llama.LlamaModel
import net.ladenthin.llama.parameters.InferenceParameters
import net.ladenthin.llama.parameters.ModelParameters
import java.io.File

data class RuntimeMetrics(
    val loadTimeMs: Long = 0L,
    val timeToFirstTokenMs: Long = 0L,
    val tokensPerSecond: Double = 0.0,
    val totalTokens: Int = 0,
    val peakMemoryMb: Long = 0L
)

data class GenerationChunk(
    val tokenText: String,
    val isComplete: Boolean,
    val metrics: RuntimeMetrics? = null
)

data class ModelLoadResult(
    val success: Boolean,
    val modelName: String,
    val loadTimeMs: Long,
    val memoryUsedMb: Long,
    val errorMessage: String? = null
)

class LocalGgufRuntime {

    @Volatile
    private var llamaModel: LlamaModel? = null

    @Volatile
    var activeModelPath: String? = null
        private set

    @Volatile
    var activeModelName: String? = null
        private set

    val isModelLoaded: Boolean
        get() = llamaModel != null

    var lastMetrics: RuntimeMetrics = RuntimeMetrics()
        private set

    @Synchronized
    fun loadModel(modelFile: File, threads: Int = 4, contextSize: Int = 2048): ModelLoadResult {
        if (!modelFile.exists()) {
            return ModelLoadResult(
                success = false,
                modelName = modelFile.name,
                loadTimeMs = 0L,
                memoryUsedMb = 0L,
                errorMessage = "GGUF model file not found at ${modelFile.absolutePath}"
            )
        }

        // Unload any existing model first
        unloadModel()

        val startTime = System.currentTimeMillis()
        val memBefore = getProcessMemoryMb()

        return try {
            val params = ModelParameters()
                .setModel(modelFile.absolutePath)
                .setThreads(threads)
                .setCtxSize(contextSize)
                .setOffline(true)

            val model = LlamaModel(params)
            llamaModel = model
            activeModelPath = modelFile.absolutePath
            activeModelName = modelFile.name

            val loadTime = System.currentTimeMillis() - startTime
            val memAfter = getProcessMemoryMb()
            val memDelta = (memAfter - memBefore).coerceAtLeast(0L)

            Log.i("LocalGgufRuntime", "Model ${modelFile.name} loaded successfully in ${loadTime}ms (Mem: ${memDelta}MB)")

            ModelLoadResult(
                success = true,
                modelName = modelFile.name,
                loadTimeMs = loadTime,
                memoryUsedMb = memDelta
            )
        } catch (e: Throwable) {
            llamaModel = null
            activeModelPath = null
            activeModelName = null
            Log.e("LocalGgufRuntime", "Failed to load GGUF model: ${e.message}", e)
            ModelLoadResult(
                success = false,
                modelName = modelFile.name,
                loadTimeMs = System.currentTimeMillis() - startTime,
                memoryUsedMb = 0L,
                errorMessage = e.localizedMessage ?: "Unknown load error"
            )
        }
    }

    @Synchronized
    fun unloadModel() {
        try {
            llamaModel?.close()
        } catch (e: Exception) {
            Log.w("LocalGgufRuntime", "Error closing LlamaModel: ${e.message}")
        } finally {
            llamaModel = null
            activeModelPath = null
            activeModelName = null
            System.gc()
        }
    }

    suspend fun generateSync(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Pair<String, RuntimeMetrics> = withContext(Dispatchers.IO) {
        val model = llamaModel ?: throw IllegalStateException("No GGUF model is loaded into runtime.")

        val startTime = System.currentTimeMillis()
        var firstTokenMs = 0L
        var tokenCount = 0
        val sb = StringBuilder()
        val memBefore = getProcessMemoryMb()

        val inferenceParams = InferenceParameters.of(prompt)
            .withNPredict(maxTokens)
            .withTemperature(temperature)
            .withTopP(topP)

        try {
            val iterable = model.generate(inferenceParams)
            for (output in iterable) {
                if (tokenCount == 0) {
                    firstTokenMs = System.currentTimeMillis() - startTime
                }
                tokenCount++
                sb.append(output.text)
                if (output.stop) break
            }
        } catch (e: Exception) {
            Log.e("LocalGgufRuntime", "Inference error: ${e.message}", e)
            throw e
        }

        val totalTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
        val tokensPerSec = if (totalTime > 0) (tokenCount.toDouble() * 1000.0) / totalTime else 0.0
        val peakMem = getProcessMemoryMb().coerceAtLeast(memBefore)

        val metrics = RuntimeMetrics(
            loadTimeMs = 0L,
            timeToFirstTokenMs = firstTokenMs,
            tokensPerSecond = tokensPerSec,
            totalTokens = tokenCount,
            peakMemoryMb = peakMem
        )
        lastMetrics = metrics
        Pair(sb.toString().trim(), metrics)
    }

    fun generateStreaming(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Flow<GenerationChunk> = flow {
        val model = llamaModel ?: throw IllegalStateException("No GGUF model is loaded into runtime.")

        val startTime = System.currentTimeMillis()
        var firstTokenMs = 0L
        var tokenCount = 0
        val memBefore = getProcessMemoryMb()

        val inferenceParams = InferenceParameters.of(prompt)
            .withNPredict(maxTokens)
            .withTemperature(temperature)
            .withTopP(topP)

        val iterable = model.generate(inferenceParams)
        for (output in iterable) {
            if (tokenCount == 0) {
                firstTokenMs = System.currentTimeMillis() - startTime
            }
            tokenCount++
            emit(GenerationChunk(tokenText = output.text, isComplete = false))
            if (output.stop) break
        }

        val totalTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
        val tokensPerSec = if (totalTime > 0) (tokenCount.toDouble() * 1000.0) / totalTime else 0.0
        val peakMem = getProcessMemoryMb().coerceAtLeast(memBefore)

        val finalMetrics = RuntimeMetrics(
            timeToFirstTokenMs = firstTokenMs,
            tokensPerSecond = tokensPerSec,
            totalTokens = tokenCount,
            peakMemoryMb = peakMem
        )
        lastMetrics = finalMetrics

        emit(GenerationChunk(tokenText = "", isComplete = true, metrics = finalMetrics))
    }.flowOn(Dispatchers.IO)

    private fun getProcessMemoryMb(): Long {
        val pssKb = Debug.getPss()
        return if (pssKb > 0) pssKb / 1024L else (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L)
    }

    companion object {
        @Volatile
        private var INSTANCE: LocalGgufRuntime? = null

        fun getInstance(): LocalGgufRuntime {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalGgufRuntime().also { INSTANCE = it }
            }
        }
    }
}
