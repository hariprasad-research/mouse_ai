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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.OperatingMode
import com.example.ui.MouseUiState
import com.example.ui.theme.AccentGreen
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
fun AutonomyScreen(
    uiState: MouseUiState,
    onSelectMode: (OperatingMode) -> Unit,
    onLaunchSettings: () -> Unit,
    onLaunchWirelessSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry = uiState.telemetry

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Device Control & Autonomy",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Permission-gated device actions & operating modes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Operating Modes Section
        Text(
            text = "OPERATING SECURITY MODES",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        ModeCard(
            mode = OperatingMode.CONTROLLED,
            title = "Controlled Mode",
            desc = "Executes only direct owner commands. Asks for confirmation before every external action.",
            color = AmberSecondary,
            isSelected = uiState.operatingMode == OperatingMode.CONTROLLED,
            onSelect = { onSelectMode(OperatingMode.CONTROLLED) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ModeCard(
            mode = OperatingMode.INTELLIGENT,
            title = "Intelligent Mode (Recommended)",
            desc = "Plans tasks and performs safe local reads automatically. Always asks before sensitive actions.",
            color = CyanPrimary,
            isSelected = uiState.operatingMode == OperatingMode.INTELLIGENT,
            onSelect = { onSelectMode(OperatingMode.INTELLIGENT) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ModeCard(
            mode = OperatingMode.AUTONOMOUS,
            title = "Autonomous Mode",
            desc = "Executes pre-approved actions within defined boundaries. All actions logged to security audit.",
            color = VioletTertiary,
            isSelected = uiState.operatingMode == OperatingMode.AUTONOMOUS,
            onSelect = { onSelectMode(OperatingMode.AUTONOMOUS) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Device Actions & Telemetry
        Text(
            text = "DEVICE TELEMETRY & SAFE ACTIONS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceVariantDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (telemetry != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Battery Level:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Text("${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "Discharging"})", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Internal Free Storage:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("${telemetry.freeStorageMb} MB", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onLaunchSettings,
                        modifier = Modifier.weight(1f).testTag("launch_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryContainer, contentColor = CyanPrimary)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Android Settings", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onLaunchWirelessSettings,
                        modifier = Modifier.weight(1f).testTag("launch_wireless_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryContainer, contentColor = CyanPrimary)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Network Settings", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    mode: OperatingMode,
    title: String,
    desc: String,
    color: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("mode_${mode.name.lowercase()}"),
        color = if (isSelected) color.copy(alpha = 0.15f) else SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else BorderSubtle)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) color else TextMuted
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (isSelected) color else TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
