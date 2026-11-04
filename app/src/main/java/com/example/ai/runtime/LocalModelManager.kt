package com.example.ai.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.example.security.NetworkAuditTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import net.ladenthin.llama.GgufInspector
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

enum class ModelRuntimeStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    LOADING,
    LOADED_ACTIVE,
    ACTIVELY_GENERATING,
    ERROR
}

data class ManagedModel(
    val id: String,
    val name: String,
    val family: String,
    val architecture: String,
    val parameterCount: String,
    val quantization: String,
    val fileSizeBytes: Long,
    val minRamMb: Long, // Estimated minimum RAM
    val downloadUrl: String,
    val expectedSha256: String? = null,
    val license: String = "Open License",
    val status: ModelRuntimeStatus = ModelRuntimeStatus.NOT_DOWNLOADED,
    val downloadedBytes: Long = 0L,
    val downloadProgressPercent: Int = 0,
    val downloadSpeedKbps: Long = 0L,
    val actualSha256: String? = null,
    val isChecksumVerified: Boolean? = null,
    val ggufVersion: Int? = null,
    val tensorCount: Long? = null,
    val contextLength: Long? = null,
    val detectedArchitecture: String? = null,
    val measuredLoadTimeMs: Long? = null,
    val measuredMemoryBeforeMb: Long? = null,
    val measuredMemoryAfterMb: Long? = null,
    val measuredMemoryIncreaseMb: Long? = null,
    val measuredPeakRamMb: Long? = null,
    val measuredTokensPerSec: Double? = null,
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
            family = "SmolLM2",
            architecture = "Llama",
            parameterCount = "360M",
            quantization = "Q4_K_M",
            fileSizeBytes = 270_590_880L, // ~258.1 MB
            minRamMb = 850L,
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
            expectedSha256 = "2fa3f013dcdd7b99f9b237717fa0b12d75bbb89984cc1274be1471a465bac9c2",
            license = "Apache-2.0"
        ),
        ManagedModel(
            id = "llama32_1b",
            name = "Llama 3.2 1B Instruct",
            family = "Llama 3.2",
            architecture = "Llama",
            parameterCount = "1.23B",
            quantization = "Q4_K_M",
            fileSizeBytes = 807_694_464L, // ~770.3 MB
            minRamMb = 1600L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            expectedSha256 = "6f85a640a97cf2bf5b8e764087b1e83da0fdb51d7c9fab7d0fece9385611df83",
            license = "Llama 3.2 Community License"
        ),
        ManagedModel(
            id = "gemma3_1b",
            name = "Gemma 3 1B IT",
            family = "Gemma 3",
            architecture = "Gemma 3",
            parameterCount = "1B",
            quantization = "Q4_K_M",
            fileSizeBytes = 806_058_240L, // ~768.7 MB
            minRamMb = 1600L,
            downloadUrl = "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf",
            expectedSha256 = "8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135",
            license = "Gemma Terms of Use"
        )
    )

    private val _modelsState = MutableStateFlow(refreshInitialCatalog())
    val modelsState: StateFlow<List<ManagedModel>> = _modelsState.asStateFlow()

    @Volatile
    private var isDownloadCancelled = false

    fun getModelFile(modelId: String): File {
        return File(modelsDir, "$modelId.gguf")
    }

    val activeModelId: String?
        get() = _modelsState.value.firstOrNull {
            it.status == ModelRuntimeStatus.LOADED_ACTIVE || it.status == ModelRuntimeStatus.ACTIVELY_GENERATING
        }?.id

    private fun refreshInitialCatalog(): List<ManagedModel> {
        return defaultCatalog.map { model ->
            val file = getModelFile(model.id)
            if (file.exists() && file.length() > 0) {
                val isLoaded = runtime.activeModelPath == file.absolutePath
                val currentStatus = when {
                    isLoaded && runtime.isActivelyGenerating -> ModelRuntimeStatus.ACTIVELY_GENERATING
                    isLoaded -> ModelRuntimeStatus.LOADED_ACTIVE
                    else -> ModelRuntimeStatus.DOWNLOADED
                }

                // Parse existing file GGUF header if not yet parsed
                val parsedHeader = inspectGgufHeader(file)

                model.copy(
                    status = currentStatus,
                    downloadedBytes = file.length(),
                    downloadProgressPercent = 100,
                    ggufVersion = parsedHeader.version,
                    tensorCount = parsedHeader.tensorCount,
                    detectedArchitecture = parsedHeader.architecture ?: model.architecture
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
            Pair(false, "Caution: Available RAM (${availRamMb} MB) is below estimated requirement (${model.minRamMb} MB).")
        } else {
            Pair(true, "Compatible: ${availRamMb} MB RAM available (Est. requirement: ${model.minRamMb} MB).")
        }
    }

    fun checkStorageAvailability(modelId: String): Pair<Boolean, String> {
        val model = _modelsState.value.firstOrNull { it.id == modelId }
            ?: return Pair(false, "Unknown model: $modelId")

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val requiredBytes = model.fileSizeBytes + (100L * 1024 * 1024) // 100MB safety buffer

        val freeMb = freeBytes / (1024 * 1024)
        val requiredMb = requiredBytes / (1024 * 1024)

        return if (freeBytes < requiredBytes) {
            Pair(false, "Insufficient Storage: Free ${freeMb} MB < Required ${requiredMb} MB.")
        } else {
            Pair(true, "Storage OK: Free ${freeMb} MB (File size: ${model.fileSizeBytes / (1024 * 1024)} MB).")
        }
    }

    suspend fun downloadModel(
        modelId: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
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

        // Record network event for audit trail
        NetworkAuditTracker.recordEvent(
            caller = "LocalModelManager (Owner Authorized Download)",
            destination = model.downloadUrl,
            wasBlocked = false,
            policyReason = "Owner Authorized Model Download"
        )

        val request = Request.Builder()
            .url(model.downloadUrl)
            .addHeader("User-Agent", "MOUSE-Android/1.0 (Linux; Android)")
            .build()

        var downloadedSoFar = 0L
        val startTime = System.currentTimeMillis()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = "Download failed with HTTP ${response.code}: ${response.message}"
                    updateModelStatus(modelId, ModelRuntimeStatus.ERROR, error = err)
                    throw IllegalStateException(err)
                }

                val body = response.body ?: throw IllegalStateException("Empty response body from repository.")
                val totalLength = if (body.contentLength() > 0) body.contentLength() else model.fileSizeBytes

                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(128 * 1024)
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
                            if (now - lastProgressUpdate > 250) {
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

            // Rename .part to .gguf
            if (partFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                partFile.renameTo(targetFile)
            }

            // Calculate actual SHA-256
            val computedHash = calculateSha256(targetFile)
            val isVerified = if (model.expectedSha256 != null) {
                computedHash.equals(model.expectedSha256, ignoreCase = true)
            } else null

            // Inspect GGUF metadata
            val parsedHeader = inspectGgufHeader(targetFile)

            _modelsState.update { list ->
                list.map { m ->
                    if (m.id == modelId) {
                        m.copy(
                            status = ModelRuntimeStatus.DOWNLOADED,
                            downloadedBytes = targetFile.length(),
                            downloadProgressPercent = 100,
                            actualSha256 = computedHash,
                            isChecksumVerified = isVerified,
                            ggufVersion = parsedHeader.version,
                            tensorCount = parsedHeader.tensorCount,
                            detectedArchitecture = parsedHeader.architecture ?: m.architecture,
                            contextLength = parsedHeader.contextLength,
                            lastError = if (isVerified == false) "Checksum mismatch with published hash" else null
                        )
                    } else m
                }
            }

            Log.i("LocalModelManager", "Model $modelId downloaded. SHA256=$computedHash, GGUF_v=${parsedHeader.version}, Tensors=${parsedHeader.tensorCount}")
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
        val targetFile = getModelFile(modelId)
        if (runtime.activeModelPath == targetFile.absolutePath) {
            runtime.unloadModel()
        }
        val deleted = if (targetFile.exists()) targetFile.delete() else true
        _modelsState.update { list ->
            list.map { m ->
                if (m.id == modelId) {
                    m.copy(
                        status = ModelRuntimeStatus.NOT_DOWNLOADED,
                        downloadedBytes = 0L,
                        downloadProgressPercent = 0,
                        actualSha256 = null,
                        isChecksumVerified = null,
                        measuredLoadTimeMs = null,
                        measuredMemoryBeforeMb = null,
                        measuredMemoryAfterMb = null,
                        measuredMemoryIncreaseMb = null,
                        measuredPeakRamMb = null,
                        measuredTokensPerSec = null,
                        lastError = null
                    )
                } else m
            }
        }
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
                        m.copy(
                            status = ModelRuntimeStatus.LOADED_ACTIVE,
                            measuredLoadTimeMs = result.loadTimeMs,
                            measuredMemoryBeforeMb = result.memoryBeforeMb,
                            measuredMemoryAfterMb = result.memoryAfterMb,
                            measuredMemoryIncreaseMb = result.memoryUsedMb,
                            lastError = null
                        )
                    } else {
                        if (m.status == ModelRuntimeStatus.LOADED_ACTIVE || m.status == ModelRuntimeStatus.ACTIVELY_GENERATING) {
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
        val currentActive = runtime.activeModelName
        runtime.unloadModel()
        _modelsState.update { list ->
            list.map { m ->
                if (m.status == ModelRuntimeStatus.LOADED_ACTIVE || m.status == ModelRuntimeStatus.ACTIVELY_GENERATING) {
                    m.copy(status = ModelRuntimeStatus.DOWNLOADED)
                } else m
            }
        }
        Log.i("LocalModelManager", "Unloaded model $currentActive. Status restored to DOWNLOADED.")
    }

    fun updateActiveGenerationMetrics(modelId: String, peakRamMb: Long, tokensPerSec: Double) {
        _modelsState.update { list ->
            list.map { m ->
                if (m.id == modelId) {
                    m.copy(
                        measuredPeakRamMb = peakRamMb,
                        measuredTokensPerSec = tokensPerSec
                    )
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

    data class GgufParsedHeader(
        val isValid: Boolean,
        val version: Int? = null,
        val tensorCount: Long? = null,
        val architecture: String? = null,
        val contextLength: Long? = null
    )

    private fun inspectGgufHeader(file: File): GgufParsedHeader {
        if (!file.exists() || file.length() < 24) {
            return GgufParsedHeader(isValid = false)
        }

        // Try GgufInspector first
        try {
            val meta = GgufInspector.read(file.toPath())
            return GgufParsedHeader(
                isValid = true,
                version = meta.version,
                tensorCount = meta.tensorCount,
                architecture = meta.architecture.orElse(null),
                contextLength = if (meta.contextLength.isPresent) meta.contextLength.asLong else null
            )
        } catch (e: Throwable) {
            Log.d("LocalModelManager", "GgufInspector read notice: ${e.message}. Using binary header fallback.")
        }

        // Binary header parser fallback
        return try {
            FileInputStream(file).use { input ->
                val headerBytes = ByteArray(24)
                val read = input.read(headerBytes)
                if (read < 24) return GgufParsedHeader(isValid = false)

                val buf = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
                val magic = ByteArray(4)
                buf.get(magic)
                val magicStr = String(magic, Charsets.US_ASCII)
                if (magicStr != "GGUF") return GgufParsedHeader(isValid = false)

                val version = buf.int
                val tensorCount = buf.long
                GgufParsedHeader(
                    isValid = true,
                    version = version,
                    tensorCount = tensorCount
                )
            }
        } catch (e: Exception) {
            GgufParsedHeader(isValid = false)
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(128 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
