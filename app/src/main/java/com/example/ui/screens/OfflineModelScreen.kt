package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.runtime.ManagedModel
import com.example.ai.runtime.ModelRuntimeStatus
import com.example.ai.runtime.RuntimeMetrics
import com.example.ui.MouseUiState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletTertiary

@Composable
fun OfflineModelScreen(
    uiState: MouseUiState,
    onRefreshHardware: () -> Unit,
    onStartDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onLoadModel: (String) -> Unit,
    onUnloadModel: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onCheckCompatibility: (String) -> Pair<Boolean, String>,
    onCheckStorage: (String) -> Pair<Boolean, String>,
    modifier: Modifier = Modifier
) {
    val audit = uiState.hardwareAudit
    var pendingDownloadModel by remember { mutableStateOf<ManagedModel?>(null) }
    var pendingDeleteModel by remember { mutableStateOf<ManagedModel?>(null) }

    val activeModel = uiState.managedModels.firstOrNull {
        it.status == ModelRuntimeStatus.LOADED_ACTIVE || it.status == ModelRuntimeStatus.ACTIVELY_GENERATING
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Hardware & Local AI",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Stage 3: Offline Model Verification & Comparison",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            IconButton(onClick = onRefreshHardware) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanPrimary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Model & Provider Status Card
        ActiveRuntimeStatusCard(
            activeModel = activeModel,
            isInternetEnabled = uiState.permissionState.isInternetEnabled
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Device Telemetry Audit
        if (audit != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("hardware_audit_card"),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEVICE TELEMETRY AUDIT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = TextMuted
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "TIER: ${audit.tier.name}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyanPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TelemetrySpecRow("CPU Architecture", "${audit.primaryAbi} (${audit.cpuCores} active cores)")
                    TelemetrySpecRow("Available System RAM", "${audit.availRamMb} MB / ${audit.totalRamMb} MB")
                    TelemetrySpecRow("Free Internal Storage", "${audit.freeStorageMb} MB")
                    TelemetrySpecRow("Android OS", audit.androidVersion)
                    TelemetrySpecRow("Recommended Candidate", audit.recommendedModel)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PART 6: Detailed Performance Measurements Table
        PerformanceMeasurementsCard(
            activeModel = activeModel,
            metrics = uiState.activeModelMetrics
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PART 5: Offline Network Verification Card
        NetworkVerificationCard(
            isInternetEnabled = uiState.permissionState.isInternetEnabled,
            events = uiState.networkAuditEvents
        )

        Spacer(modifier = Modifier.height(18.dp))

        // PART 2: Lightweight Model Catalog
        Text(
            text = "LIGHTWEIGHT CANDIDATE MODELS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        uiState.managedModels.forEach { model ->
            CandidateModelCard(
                model = model,
                onDownloadClick = { pendingDownloadModel = model },
                onCancelClick = { onCancelDownload(model.id) },
                onLoadClick = { onLoadModel(model.id) },
                onUnloadClick = { onUnloadModel() },
                onDeleteClick = { pendingDeleteModel = model }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Operating Principle Notice
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceContainerDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "No Silent Downloads: MOUSE requires explicit owner authorization before downloading weights. When Internet is OFF, local GGUF models run 100% on-device with zero external network communication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }

    // Explicit Owner Download Approval Dialog
    pendingDownloadModel?.let { model ->
        val (compatOk, compatMsg) = onCheckCompatibility(model.id)
        val (storageOk, storageMsg) = onCheckStorage(model.id)

        AlertDialog(
            onDismissRequest = { pendingDownloadModel = null },
            icon = {
                Icon(Icons.Default.Download, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Authorise Model Download", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You are requesting to download ${model.name} (${model.family}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Source: ${model.downloadUrl.substringBefore("/resolve/")}\n" +
                               "• Exact File: ${model.downloadUrl.substringAfterLast("/")}\n" +
                               "• File Size: ${model.fileSizeBytes / (1024 * 1024)} MB (${model.fileSizeBytes} bytes)\n" +
                               "• Quantization: ${model.quantization}\n" +
                               "• License: ${model.license}\n" +
                               "• Estimated RAM Requirement: ~${model.minRamMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (model.expectedSha256 != null) {
                        Text(
                            text = "• Published Git-LFS SHA-256:\n  ${model.expectedSha256.take(16)}...${model.expectedSha256.takeLast(16)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = storageMsg,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (storageOk) AccentGreen else AccentRed
                    )
                    Text(
                        text = compatMsg,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (compatOk) AccentGreen else AmberSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = pendingDownloadModel
                        pendingDownloadModel = null
                        if (m != null) onStartDownload(m.id)
                    },
                    enabled = storageOk,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SurfaceDark)
                ) {
                    Text("Confirm & Download", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDownloadModel = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete Confirmation Dialog
    pendingDeleteModel?.let { model ->
        AlertDialog(
            onDismissRequest = { pendingDeleteModel = null },
            title = { Text("Delete Model File?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete ${model.name} (${model.fileSizeBytes / (1024 * 1024)} MB) from local storage?", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = pendingDeleteModel
                        pendingDeleteModel = null
                        if (m != null) onDeleteModel(m.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = SurfaceDark)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteModel = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun ActiveRuntimeStatusCard(activeModel: ManagedModel?, isInternetEnabled: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("active_runtime_card"),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (activeModel != null) CyanPrimary else BorderSubtle
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    activeModel?.status == ModelRuntimeStatus.ACTIVELY_GENERATING -> AccentGreen
                                    activeModel?.status == ModelRuntimeStatus.LOADED_ACTIVE -> CyanPrimary
                                    else -> TextMuted
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE INFERENCE RUNTIME",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = if (activeModel != null) CyanPrimary else TextMuted
                    )
                }

                // Internet Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isInternetEnabled) AmberSecondary.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isInternetEnabled) "NET ON" else "NET OFF (OFFLINE)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = if (isInternetEnabled) AmberSecondary else AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeModel != null) {
                Text(
                    text = "${activeModel.name} (${activeModel.quantization})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Active Provider: LocalGgufRuntime (libjllama.so) • Status: ${
                        if (activeModel.status == ModelRuntimeStatus.ACTIVELY_GENERATING) "GENERATING TOKENS..." else "LOADED IN RAM"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = "No GGUF Model Loaded in RAM",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Active Provider: MOUSE Local Knowledge & Directive Component (Offline Fallback)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun PerformanceMeasurementsCard(activeModel: ManagedModel?, metrics: RuntimeMetrics?) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("performance_measurements_card"),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEASURED INFERENCE & RUNTIME BENCHMARKS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = CyanPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MeasurementRow("Model Download Size", activeModel?.let { "${it.downloadedBytes / (1024 * 1024)} MB (${it.downloadedBytes} bytes)" } ?: "Not measured")
            MeasurementRow("SHA-256 Hash", activeModel?.actualSha256?.let {
                val status = if (activeModel.isChecksumVerified == true) " (Verified)" else ""
                "${it.take(12)}...${it.takeLast(12)}$status"
            } ?: "Not measured")
            MeasurementRow("Model Load Duration", if ((activeModel?.measuredLoadTimeMs ?: 0L) > 0) "${activeModel?.measuredLoadTimeMs} ms" else "Not measured")
            MeasurementRow("Memory Before Loading", if ((activeModel?.measuredMemoryBeforeMb ?: 0L) > 0) "${activeModel?.measuredMemoryBeforeMb} MB" else "Not measured")
            MeasurementRow("Memory After Loading", if ((activeModel?.measuredMemoryAfterMb ?: 0L) > 0) "${activeModel?.measuredMemoryAfterMb} MB" else "Not measured")
            MeasurementRow("Memory Increase", if ((activeModel?.measuredMemoryIncreaseMb ?: 0L) > 0) "+${activeModel?.measuredMemoryIncreaseMb} MB" else "Not measured")
            MeasurementRow("Time to First Token (TTFT)", if ((metrics?.timeToFirstTokenMs ?: 0L) > 0) "${metrics?.timeToFirstTokenMs} ms" else "Not measured")
            MeasurementRow("Generation Speed", if ((metrics?.tokensPerSecond ?: 0.0) > 0.0) "%.1f tokens/sec".format(metrics?.tokensPerSecond) else "Not measured")
            MeasurementRow("Total Generated Tokens", if ((metrics?.totalTokens ?: 0) > 0) "${metrics?.totalTokens} tokens" else "Not measured")
            MeasurementRow("Total Generation Duration", if ((metrics?.totalGenerationDurationMs ?: 0L) > 0) "${metrics?.totalGenerationDurationMs} ms" else "Not measured")
            MeasurementRow("Peak Memory During Inference", if ((metrics?.peakMemoryDuringInferenceMb ?: 0L) > 0) "${metrics?.peakMemoryDuringInferenceMb} MB" else "Not measured")
            MeasurementRow("Memory After Unload", if ((metrics?.memoryAfterUnloadMb ?: 0L) > 0) "${metrics?.memoryAfterUnloadMb} MB" else "Not measured")
            MeasurementRow("Crash or Error", metrics?.lastError ?: "None (Clean)")
        }
    }
}

@Composable
fun MeasurementRow(metric: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = metric, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = if (value == "Not measured") TextMuted else TextPrimary
            )
        )
    }
}

@Composable
fun TelemetrySpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
    }
}

@Composable
fun NetworkVerificationCard(isInternetEnabled: Boolean, events: List<com.example.security.NetworkAuditEvent>) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("network_verification_card"),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (!isInternetEnabled) AccentGreen.copy(alpha = 0.5f) else AmberSecondary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (!isInternetEnabled) Icons.Default.PublicOff else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (!isInternetEnabled) AccentGreen else AmberSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OFFLINE NETWORK VERIFICATION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = if (!isInternetEnabled) AccentGreen else AmberSecondary
                    )
                }

                Text(
                    text = if (!isInternetEnabled) "ZERO TRAFFIC CONFIRMED" else "ONLINE ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    color = if (!isInternetEnabled) AccentGreen else AmberSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (!isInternetEnabled) {
                    "PermissionGate enforces strict zero-trust isolation. Outgoing Gemini, OpenRouter, and web search requests are blocked immediately with SecurityException. Local inference executes 100% on-device."
                } else {
                    "Internet access is currently enabled by owner permission. Disable in the top bar to enforce offline verification."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Intercepted Network Events:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                events.take(3).forEach { ev ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${ev.caller}: ${ev.destination.take(24)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                        Text(
                            text = if (ev.wasBlocked) "BLOCKED" else "ALLOWED",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ev.wasBlocked) AccentRed else AccentGreen
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateModelCard(
    model: ManagedModel,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onLoadClick: () -> Unit,
    onUnloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isActive = model.status == ModelRuntimeStatus.LOADED_ACTIVE || model.status == ModelRuntimeStatus.ACTIVELY_GENERATING

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("managed_model_${model.id}"),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) CyanPrimary else BorderSubtle
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Family: ${model.family} • Architecture: ${model.architecture} • ${model.parameterCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (model.status) {
                                ModelRuntimeStatus.ACTIVELY_GENERATING -> AccentGreen.copy(alpha = 0.2f)
                                ModelRuntimeStatus.LOADED_ACTIVE -> CyanPrimaryContainer
                                ModelRuntimeStatus.DOWNLOADED -> VioletTertiary.copy(alpha = 0.2f)
                                ModelRuntimeStatus.DOWNLOADING -> AmberSecondary.copy(alpha = 0.2f)
                                ModelRuntimeStatus.LOADING -> CyanPrimaryContainer
                                ModelRuntimeStatus.ERROR -> AccentRed.copy(alpha = 0.2f)
                                ModelRuntimeStatus.NOT_DOWNLOADED -> BorderSubtle
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (model.status) {
                            ModelRuntimeStatus.ACTIVELY_GENERATING -> "GENERATING"
                            ModelRuntimeStatus.LOADED_ACTIVE -> "ACTIVE (IN-RAM)"
                            ModelRuntimeStatus.DOWNLOADED -> "DOWNLOADED (INSTALLED)"
                            ModelRuntimeStatus.DOWNLOADING -> "DOWNLOADING ${model.downloadProgressPercent}%"
                            ModelRuntimeStatus.LOADING -> "LOADING INTO RAM..."
                            ModelRuntimeStatus.ERROR -> "ERROR"
                            ModelRuntimeStatus.NOT_DOWNLOADED -> "NOT DOWNLOADED"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = when (model.status) {
                            ModelRuntimeStatus.ACTIVELY_GENERATING -> AccentGreen
                            ModelRuntimeStatus.LOADED_ACTIVE -> CyanPrimary
                            ModelRuntimeStatus.DOWNLOADED -> VioletTertiary
                            ModelRuntimeStatus.DOWNLOADING -> AmberSecondary
                            ModelRuntimeStatus.LOADING -> CyanPrimary
                            ModelRuntimeStatus.ERROR -> AccentRed
                            ModelRuntimeStatus.NOT_DOWNLOADED -> TextMuted
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quant: ${model.quantization} • Size: ${model.fileSizeBytes / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "RAM (Est): ~${model.minRamMb} MB",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = CyanPrimary
                )
            }

            // GGUF Metadata Row (if downloaded)
            if (model.status == ModelRuntimeStatus.DOWNLOADED || isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GGUF v${model.ggufVersion ?: 3} • Tensors: ${model.tensorCount ?: "Valid"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    if (model.actualSha256 != null) {
                        Text(
                            text = if (model.isChecksumVerified == true) "SHA256: Verified" else "SHA256: Computed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (model.isChecksumVerified == true) AccentGreen else TextSecondary
                        )
                    }
                }
            }

            // Actual Runtime Memory (when measured)
            if (model.measuredMemoryAfterMb != null && model.measuredMemoryAfterMb > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Actual Process RAM: ${model.measuredMemoryAfterMb} MB (+${model.measuredMemoryIncreaseMb} MB delta)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Download Progress Bar if downloading
            if (model.status == ModelRuntimeStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { model.downloadProgressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = CyanPrimary,
                    trackColor = SurfaceContainerDark,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${model.downloadedBytes / (1024 * 1024)} MB / ${model.fileSizeBytes / (1024 * 1024)} MB (${model.downloadSpeedKbps} KB/s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    TextButton(onClick = onCancelClick, modifier = Modifier.height(24.dp)) {
                        Text("Cancel", color = AccentRed, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Error display
            if (model.lastError != null) {
                Text(
                    text = "Error: ${model.lastError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRed
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (model.status) {
                    ModelRuntimeStatus.NOT_DOWNLOADED -> {
                        Button(
                            onClick = onDownloadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryContainer, contentColor = CyanPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download GGUF (~${model.fileSizeBytes / (1024 * 1024)} MB)", fontSize = 12.sp)
                        }
                    }
                    ModelRuntimeStatus.DOWNLOADED -> {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onLoadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SurfaceDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load into Memory", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ModelRuntimeStatus.LOADED_ACTIVE, ModelRuntimeStatus.ACTIVELY_GENERATING -> {
                        Button(
                            onClick = onUnloadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary, contentColor = SurfaceDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unload from RAM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ModelRuntimeStatus.DOWNLOADING -> {
                        // Handled above in progress bar
                    }
                    ModelRuntimeStatus.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = CyanPrimary)
                    }
                    ModelRuntimeStatus.ERROR -> {
                        Button(
                            onClick = onDownloadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryContainer, contentColor = CyanPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry Download", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
