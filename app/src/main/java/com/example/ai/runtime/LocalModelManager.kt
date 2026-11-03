package com.example.ai.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

enum class ModelRuntimeStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    LOADING,
    LOADED_ACTIVE,
    ERROR
}

data class ManagedModel(
    val id: String,
    val name: String,
    val architecture: String,
    val parameterCount: String,
    val quantization: String,
    val fileSizeBytes: Long,
    val minRamMb: Long,
    val downloadUrl: String,
    val expectedSha256: String? = null,
    val status: ModelRuntimeStatus = ModelRuntimeStatus.NOT_DOWNLOADED,
    val downloadedBytes: Long = 0L,
    val downloadProgressPercent: Int = 0,
    val downloadSpeedKbps: Long = 0L,
    val lastError: String? = null
)

class LocalModelManager(
    private val context: Context,
    val runtime: LocalGgufRuntime = LocalGgufRuntime.getInstance()
) {
    private val modelsDir: File = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val defaultCatalog = listOf(
        ManagedModel(
            id = "smollm2_360m",
            name = "SmolLM2 360M Instruct",
            architecture = "Llama / SmolLM2",
            parameterCount = "360M",
            quantization = "Q4_K_M",
            fileSizeBytes = 245_000_000L, // ~245 MB
            minRamMb = 850L,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
            expectedSha256 = null
        ),
        ManagedModel(
            id = "qwen25_05b",
            name = "Qwen 2.5 0.5B Instruct",
            architecture = "Qwen2",
            parameterCount = "490M",
            quantization = "Q4_K_M",
            fileSizeBytes = 398_000_000L, // ~398 MB
            minRamMb = 1400L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2_5-0_5b-instruct-q4_k_m.gguf",
            expectedSha256 = null
        )
    )

    private val _modelsState = MutableStateFlow(refreshInitialCatalog())
    val modelsState: StateFlow<List<ManagedModel>> = _modelsState.asStateFlow()

    private var activeDownloadJob: Job? = null
    @Volatile
    private var isDownloadCancelled = false

    private fun getModelFile(modelId: String): File {
        return File(modelsDir, "$modelId.gguf")
    }

    private fun refreshInitialCatalog(): List<ManagedModel> {
        return defaultCatalog.map { model ->
            val file = getModelFile(model.id)
            if (file.exists() && file.length() > 0) {
                val isLoaded = runtime.activeModelPath == file.absolutePath
                model.copy(
                    status = if (isLoaded) ModelRuntimeStatus.LOADED_ACTIVE else ModelRuntimeStatus.DOWNLOADED,
                    downloadedBytes = file.length(),
                    downloadProgressPercent = 100
                )
            } else {
                model.copy(status = ModelRuntimeStatus.NOT_DOWNLOADED)
            }
        }
    }

    fun checkHardwareCompatibility(modelId: String): Pair<Boolean, String> {
        val model = _modelsState.value.firstOrNull { it.id == modelId }
            ?: return Pair(false, "Unknown model: $modelId")

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        return if (availRamMb < model.minRamMb) {
            Pair(false, "Insufficient RAM: Available ${availRamMb}MB < Required ${model.minRamMb}MB. Running this model may cause system memory termination.")
        } else {
            Pair(true, "Compatible: ${availRamMb}MB RAM available (Requires ${model.minRamMb}MB).")
        }
    }

    fun checkStorageAvailability(modelId: String): Pair<Boolean, String> {
        val model = _modelsState.value.firstOrNull { it.id == modelId }
            ?: return Pair(false, "Unknown model: $modelId")

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val requiredBytes = model.fileSizeBytes + (100L * 1024 * 1024) // 100MB margin

        val freeMb = freeBytes / (1024 * 1024)
        val requiredMb = requiredBytes / (1024 * 1024)

        return if (freeBytes < requiredBytes) {
            Pair(false, "Insufficient Disk Space: Free ${freeMb}MB < Required ${requiredMb}MB.")
        } else {
            Pair(true, "Storage OK: Free ${freeMb}MB (Requires ${requiredMb}MB).")
        }
    }

    suspend fun downloadModel(modelId: String, onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val model = _modelsState.value.firstOrNull { it.id == modelId }
            ?: throw IllegalArgumentException("Model $modelId not in catalog.")

        val (storageOk, storageMsg) = checkStorageAvailability(modelId)
        if (!storageOk) {
            updateModelStatus(modelId, ModelRuntimeStatus.ERROR, error = storageMsg)
            throw IllegalStateException(storageMsg)
        }

        isDownloadCancelled = false
        val targetFile = getModelFile(modelId)
        val partFile = File(modelsDir, "$modelId.gguf.part")

        updateModelStatus(modelId, ModelRuntimeStatus.DOWNLOADING)

        val request = Request.Builder()
            .url(model.downloadUrl)
            .build()

        var downloadedSoFar = 0L
        val startTime = System.currentTimeMillis()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = "Download failed with HTTP ${response.code}"
                    updateModelStatus(modelId, ModelRuntimeStatus.ERROR, error = err)
                    throw IllegalStateException(err)
                }

                val body = response.body ?: throw IllegalStateException("Empty response body from model repository.")
                val totalLength = if (body.contentLength() > 0) body.contentLength() else model.fileSizeBytes

                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var lastProgressUpdate = System.currentTimeMillis()

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isDownloadCancelled) {
                                partFile.delete()
                                updateModelStatus(modelId, ModelRuntimeStatus.NOT_DOWNLOADED)
                                throw CancellationException("Download cancelled by owner.")
                            }

                            output.write(buffer, 0, bytesRead)
                            downloadedSoFar += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 300) {
                                val percent = ((downloadedSoFar.toDouble() / totalLength.toDouble()) * 100).toInt().coerceIn(0, 100)
                                val elapsedSec = (now - startTime).coerceAtLeast(1L) / 1000.0
                                val speedKbps = if (elapsedSec > 0) ((downloadedSoFar / 1024) / elapsedSec).toLong() else 0L

                                _modelsState.update { list ->
                                    list.map { m ->
                                        if (m.id == modelId) {
                                            m.copy(
                                                status = ModelRuntimeStatus.DOWNLOADING,
                                                downloadedBytes = downloadedSoFar,
                                                downloadProgressPercent = percent,
                                                downloadSpeedKbps = speedKbps
                                            )
                                        } else m
                                    }
                                }
                                onProgress(downloadedSoFar, totalLength)
                                lastProgressUpdate = now
                            }
                        }
                    }
                }
            }

            // Rename part file to actual .gguf
            if (partFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                partFile.renameTo(targetFile)
            }

            // Verify checksum if available
            if (model.expectedSha256 != null) {
                val computedHash = calculateSha256(targetFile)
                if (!computedHash.equals(model.expectedSha256, ignoreCase = true)) {
                    targetFile.delete()
                    val err = "Checksum mismatch: Model file may be corrupted."
                    updateModelStatus(modelId, ModelRuntimeStatus.ERROR, error = err)
                    throw IllegalStateException(err)
                }
            }

            updateModelStatus(modelId, ModelRuntimeStatus.DOWNLOADED, downloadedBytes = targetFile.length(), percent = 100)
            Log.i("LocalModelManager", "Model $modelId downloaded and installed successfully (${targetFile.length()} bytes)")
        } catch (e: Exception) {
            partFile.delete()
            if (e !is CancellationException) {
                updateModelStatus(modelId, ModelRuntimeStatus.ERROR, error = e.localizedMessage)
            }
            throw e
        }
    }

    fun cancelDownload(modelId: String) {
        isDownloadCancelled = true
        val partFile = File(modelsDir, "$modelId.gguf.part")
        if (partFile.exists()) partFile.delete()
        updateModelStatus(modelId, ModelRuntimeStatus.NOT_DOWNLOADED)
    }

    fun deleteModel(modelId: String): Boolean {
        // If loaded, unload first
        val targetFile = getModelFile(modelId)
        if (runtime.activeModelPath == targetFile.absolutePath) {
            runtime.unloadModel()
        }
        val deleted = if (targetFile.exists()) targetFile.delete() else true
        updateModelStatus(modelId, ModelRuntimeStatus.NOT_DOWNLOADED, downloadedBytes = 0L, percent = 0)
        return deleted
    }

    suspend fun loadModelIntoRuntime(modelId: String): ModelLoadResult = withContext(Dispatchers.IO) {
        val file = getModelFile(modelId)
        if (!file.exists()) {
            return@withContext ModelLoadResult(
                success = false,
                modelName = modelId,
                loadTimeMs = 0L,
                memoryUsedMb = 0L,
                errorMessage = "Model file does not exist. Please download it first."
            )
        }

        updateModelStatus(modelId, ModelRuntimeStatus.LOADING)
        val result = runtime.loadModel(file)

        if (result.success) {
            _modelsState.update { list ->
                list.map { m ->
                    if (m.id == modelId) {
                        m.copy(status = ModelRuntimeStatus.LOADED_ACTIVE, lastError = null)
                    } else {
                        // All other downloaded models remain downloaded
                        if (m.status == ModelRuntimeStatus.LOADED_ACTIVE) {
                            m.copy(status = ModelRuntimeStatus.DOWNLOADED)
                        } else m
                    }
                }
            }
        } else {
            updateModelStatus(modelId, ModelRuntimeStatus.ERROR, error = result.errorMessage)
        }
        result
    }

    fun unloadActiveModel() {
        runtime.unloadModel()
        _modelsState.update { list ->
            list.map { m ->
                if (m.status == ModelRuntimeStatus.LOADED_ACTIVE) {
                    m.copy(status = ModelRuntimeStatus.DOWNLOADED)
                } else m
            }
        }
    }

    private fun updateModelStatus(
        modelId: String,
        status: ModelRuntimeStatus,
        downloadedBytes: Long = 0L,
        percent: Int = 0,
        error: String? = null
    ) {
        _modelsState.update { list ->
            list.map { m ->
                if (m.id == modelId) {
                    m.copy(
                        status = status,
                        downloadedBytes = if (downloadedBytes > 0) downloadedBytes else m.downloadedBytes,
                        downloadProgressPercent = if (percent > 0) percent else m.downloadProgressPercent,
                        lastError = error
                    )
                } else m
            }
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
