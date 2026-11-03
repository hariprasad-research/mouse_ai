package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.SecurityAuditEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PermissionGateScreen(
    uiState: MouseUiState,
    onToggleMaster: (Boolean) -> Unit,
    onToggleSub: (onlineAi: Boolean?, webSearch: Boolean?, research: Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isInternetOn = uiState.permissionState.isInternetEnabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = if (isInternetOn) AccentGreen else AmberSecondary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Internet Permission Gate",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Offline-First Security & Network Authority",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Master Switch Card
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("master_internet_card"),
            shape = RoundedCornerShape(14.dp),
            color = if (isInternetOn) AccentGreen.copy(alpha = 0.1f) else SurfaceVariantDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isInternetOn) AccentGreen else AmberSecondary)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isInternetOn) Icons.Default.Public else Icons.Default.PublicOff,
                        contentDescription = null,
                        tint = if (isInternetOn) AccentGreen else AmberSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isInternetOn) "MASTER INTERNET: ENABLED" else "MASTER INTERNET: DISABLED",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isInternetOn) AccentGreen else AmberSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isInternetOn) "Approved online AI and web searches are permitted." else "Strict offline mode. All outgoing network calls blocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = isInternetOn,
                    onCheckedChange = onToggleMaster,
                    modifier = Modifier.testTag("master_internet_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentGreen,
                        checkedTrackColor = AccentGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = AmberSecondary,
                        uncheckedTrackColor = SurfaceContainerDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Granular Sub-Permissions
        Text(
            text = "GRANULAR SUBSYSTEM PERMISSIONS",
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
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                SubPermissionRow(
                    title = "Online AI Queries (Gemini / OpenAI)",
                    enabled = isInternetOn && uiState.permissionState.allowOnlineAi,
                    isMasterOn = isInternetOn,
                    onToggle = { onToggleSub(!uiState.permissionState.allowOnlineAi, null, null) },
                    testTag = "sub_online_ai_switch"
                )
                HorizontalDivider(color = BorderSubtle)
                SubPermissionRow(
                    title = "Live Web Searches",
                    enabled = isInternetOn && uiState.permissionState.allowWebSearch,
                    isMasterOn = isInternetOn,
                    onToggle = { onToggleSub(null, !uiState.permissionState.allowWebSearch, null) },
                    testTag = "sub_web_search_switch"
                )
                HorizontalDivider(color = BorderSubtle)
                SubPermissionRow(
                    title = "Multi-Source Research Aggregation",
                    enabled = isInternetOn && uiState.permissionState.allowResearchAggregation,
                    isMasterOn = isInternetOn,
                    onToggle = { onToggleSub(null, null, !uiState.permissionState.allowResearchAggregation) },
                    testTag = "sub_research_switch"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Audit Trail
        Text(
            text = "SECURITY AUDIT LOGS (${uiState.auditLogs.size})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(uiState.auditLogs.take(15), key = { it.id }) { log ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceContainerDark
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = CyanPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${log.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (log.status == "AUTHORIZED" || log.status == "CONFIRMED_BY_USER") AccentGreen else AccentRed
                                )
                            }
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubPermissionRow(
    title: String,
    enabled: Boolean,
    isMasterOn: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = if (isMasterOn) TextPrimary else TextMuted)
            if (!isMasterOn) {
                Text(text = "Requires Master Internet ON", style = MaterialTheme.typography.labelSmall, color = AmberSecondary)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            enabled = isMasterOn,
            modifier = Modifier.testTag(testTag)
        )
    }
}
