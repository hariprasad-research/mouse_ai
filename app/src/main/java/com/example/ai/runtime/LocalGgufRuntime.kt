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
    val modelLoadDurationMs: Long = 0L,
    val memoryBeforeLoadMb: Long = 0L,
    val memoryAfterLoadMb: Long = 0L,
    val memoryIncreaseMb: Long = 0L,
    val timeToFirstTokenMs: Long = 0L,
    val tokensPerSecond: Double = 0.0,
    val totalTokens: Int = 0,
    val totalGenerationDurationMs: Long = 0L,
    val peakMemoryDuringInferenceMb: Long = 0L,
    val memoryAfterUnloadMb: Long = 0L,
    val lastError: String? = null
) {
    // Backward compatibility aliases
    val loadTimeMs: Long get() = modelLoadDurationMs
    val peakMemoryMb: Long get() = peakMemoryDuringInferenceMb
}

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
    val memoryBeforeMb: Long = 0L,
    val memoryAfterMb: Long = 0L,
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

    @Volatile
    var isActivelyGenerating: Boolean = false
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

            lastMetrics = lastMetrics.copy(
                modelLoadDurationMs = loadTime,
                memoryBeforeLoadMb = memBefore,
                memoryAfterLoadMb = memAfter,
                memoryIncreaseMb = memDelta,
                lastError = null
            )

            Log.i("LocalGgufRuntime", "Model ${modelFile.name} loaded in ${loadTime}ms (RAM: ${memBefore}MB -> ${memAfter}MB, +${memDelta}MB)")

            ModelLoadResult(
                success = true,
                modelName = modelFile.name,
                loadTimeMs = loadTime,
                memoryUsedMb = memDelta,
                memoryBeforeMb = memBefore,
                memoryAfterMb = memAfter
            )
        } catch (e: Throwable) {
            llamaModel = null
            activeModelPath = null
            activeModelName = null
            val loadErr = e.localizedMessage ?: "Native model load failed"
            lastMetrics = lastMetrics.copy(
                modelLoadDurationMs = System.currentTimeMillis() - startTime,
                memoryBeforeLoadMb = memBefore,
                memoryAfterLoadMb = getProcessMemoryMb(),
                lastError = loadErr
            )
            Log.e("LocalGgufRuntime", "Failed to load GGUF model: $loadErr", e)
            ModelLoadResult(
                success = false,
                modelName = modelFile.name,
                loadTimeMs = System.currentTimeMillis() - startTime,
                memoryUsedMb = 0L,
                errorMessage = loadErr
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
            isActivelyGenerating = false
            System.gc()
            val memAfterUnload = getProcessMemoryMb()
            lastMetrics = lastMetrics.copy(memoryAfterUnloadMb = memAfterUnload)
            Log.i("LocalGgufRuntime", "Model unloaded. Process RAM after GC: ${memAfterUnload}MB")
        }
    }

    suspend fun generateSync(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Pair<String, RuntimeMetrics> = withContext(Dispatchers.IO) {
        val model = llamaModel ?: throw IllegalStateException("No GGUF model is loaded into runtime.")

        isActivelyGenerating = true
        val startTime = System.currentTimeMillis()
        var firstTokenMs = 0L
        var tokenCount = 0
        val sb = StringBuilder()
        val memBefore = getProcessMemoryMb()
        var peakMem = memBefore

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

                // Sample peak memory every 10 tokens
                if (tokenCount % 10 == 0) {
                    val currentMem = getProcessMemoryMb()
                    if (currentMem > peakMem) peakMem = currentMem
                }

                if (output.stop) break
            }
        } catch (e: Exception) {
            Log.e("LocalGgufRuntime", "Inference error: ${e.message}", e)
            lastMetrics = lastMetrics.copy(lastError = e.localizedMessage)
            throw e
        } finally {
            isActivelyGenerating = false
        }

        val totalTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
        val tokensPerSec = if (totalTime > 0) (tokenCount.toDouble() * 1000.0) / totalTime else 0.0
        val currentMem = getProcessMemoryMb()
        if (currentMem > peakMem) peakMem = currentMem

        val metrics = lastMetrics.copy(
            timeToFirstTokenMs = firstTokenMs,
            tokensPerSecond = tokensPerSec,
            totalTokens = tokenCount,
            totalGenerationDurationMs = totalTime,
            peakMemoryDuringInferenceMb = peakMem,
            lastError = null
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

        isActivelyGenerating = true
        val startTime = System.currentTimeMillis()
        var firstTokenMs = 0L
        var tokenCount = 0
        val memBefore = getProcessMemoryMb()
        var peakMem = memBefore

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
                emit(GenerationChunk(tokenText = output.text, isComplete = false))

                if (tokenCount % 10 == 0) {
                    val currentMem = getProcessMemoryMb()
                    if (currentMem > peakMem) peakMem = currentMem
                }

                if (output.stop) break
            }
        } finally {
            isActivelyGenerating = false
        }

        val totalTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
        val tokensPerSec = if (totalTime > 0) (tokenCount.toDouble() * 1000.0) / totalTime else 0.0
        val currentMem = getProcessMemoryMb()
        if (currentMem > peakMem) peakMem = currentMem

        val finalMetrics = lastMetrics.copy(
            timeToFirstTokenMs = firstTokenMs,
            tokensPerSecond = tokensPerSec,
            totalTokens = tokenCount,
            totalGenerationDurationMs = totalTime,
            peakMemoryDuringInferenceMb = peakMem,
            lastError = null
        )
        lastMetrics = finalMetrics

        emit(GenerationChunk(tokenText = "", isComplete = true, metrics = finalMetrics))
    }.flowOn(Dispatchers.IO)

    fun getProcessMemoryMb(): Long {
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
