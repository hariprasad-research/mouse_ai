package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.runtime.ManagedModel
import com.example.ai.runtime.ModelRuntimeStatus
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
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
                        text = "Genuine GGUF llama.cpp runtime & model manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            IconButton(onClick = onRefreshHardware) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Audit Card
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

                    TelemetrySpecRow("CPU Architecture", "${audit.primaryAbi} (${audit.cpuCores} cores)")
                    TelemetrySpecRow("Available System RAM", "${audit.availRamMb} MB / ${audit.totalRamMb} MB")
                    TelemetrySpecRow("Free Disk Space", "${audit.freeStorageMb} MB")
                    TelemetrySpecRow("Android OS", audit.androidVersion)
                    TelemetrySpecRow("Recommended Model", audit.recommendedModel)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Real-Time Inference Performance Benchmarks
        val metrics = uiState.activeModelMetrics
        if (metrics != null && (metrics.totalTokens > 0 || metrics.tokensPerSecond > 0)) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("inference_benchmark_card"),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LAST INFERENCE BENCHMARK",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BenchmarkMetricItem("TTFT", "${metrics.timeToFirstTokenMs}ms")
                        BenchmarkMetricItem("Speed", "%.1f t/s".format(metrics.tokensPerSecond))
                        BenchmarkMetricItem("Tokens", "${metrics.totalTokens}")
                        BenchmarkMetricItem("Peak RAM", "${metrics.peakMemoryMb}MB")
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Managed Models Section
        Text(
            text = "GGUF COMPATIBLE ON-DEVICE MODELS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        uiState.managedModels.forEach { model ->
            ManagedModelCard(
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
                    text = "No Large Model Silent Downloads: MOUSE requires explicit owner authorization before downloading weights. When no model is loaded, MOUSE uses local knowledge and clearly communicates that no LLM is in memory.",
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
                        text = "You are requesting to download ${model.name} (${model.architecture}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Download Size: ~${model.fileSizeBytes / (1024 * 1024)} MB\n• Quantization: ${model.quantization}\n• Min Required RAM: ${model.minRamMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
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
fun BenchmarkMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = CyanPrimary
        )
    }
}

@Composable
fun ManagedModelCard(
    model: ManagedModel,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onLoadClick: () -> Unit,
    onUnloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("managed_model_${model.id}"),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (model.status == ModelRuntimeStatus.LOADED_ACTIVE) CyanPrimary else BorderSubtle
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
                        text = "${model.architecture} • ${model.parameterCount} • ${model.quantization}",
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
                                ModelRuntimeStatus.LOADED_ACTIVE -> AccentGreen.copy(alpha = 0.2f)
                                ModelRuntimeStatus.DOWNLOADED -> CyanPrimaryContainer
                                ModelRuntimeStatus.DOWNLOADING -> AmberSecondary.copy(alpha = 0.2f)
                                ModelRuntimeStatus.LOADING -> VioletTertiary.copy(alpha = 0.2f)
                                ModelRuntimeStatus.ERROR -> AccentRed.copy(alpha = 0.2f)
                                ModelRuntimeStatus.NOT_DOWNLOADED -> BorderSubtle
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (model.status) {
                            ModelRuntimeStatus.LOADED_ACTIVE -> "ACTIVE (IN-RAM)"
                            ModelRuntimeStatus.DOWNLOADED -> "DOWNLOADED"
                            ModelRuntimeStatus.DOWNLOADING -> "DOWNLOADING ${model.downloadProgressPercent}%"
                            ModelRuntimeStatus.LOADING -> "LOADING..."
                            ModelRuntimeStatus.ERROR -> "ERROR"
                            ModelRuntimeStatus.NOT_DOWNLOADED -> "READY TO DOWNLOAD"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = when (model.status) {
                            ModelRuntimeStatus.LOADED_ACTIVE -> AccentGreen
                            ModelRuntimeStatus.DOWNLOADED -> CyanPrimary
                            ModelRuntimeStatus.DOWNLOADING -> AmberSecondary
                            ModelRuntimeStatus.LOADING -> VioletTertiary
                            ModelRuntimeStatus.ERROR -> AccentRed
                            ModelRuntimeStatus.NOT_DOWNLOADED -> TextMuted
                        }
                    )
                }
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
                        text = "${model.downloadedBytes / (1024 * 1024)}MB / ${model.fileSizeBytes / (1024 * 1024)}MB (${model.downloadSpeedKbps} KB/s)",
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
                    ModelRuntimeStatus.LOADED_ACTIVE -> {
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
