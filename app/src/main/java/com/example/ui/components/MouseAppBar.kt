package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MouseUiState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouseAppBar(
    uiState: MouseUiState,
    onMenuClick: () -> Unit,
    onToggleInternet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmToggleDialog by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier.testTag("mouse_app_bar"),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MOUSE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = CyanPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BorderSubtle)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (uiState.permissionState.isInternetEnabled) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.permissionState.isInternetEnabled) AccentGreen else AmberSecondary
                        )
                    }
                }
                Text(
                    text = when (uiState.activeProviderId) {
                        "gemini" -> if (uiState.permissionState.isInternetEnabled) "Gemini 1.5 Flash" else "Local Core (Net Blocked)"
                        "openai_compatible" -> if (uiState.permissionState.isInternetEnabled) "OpenAI-Compatible" else "Local Core (Net Blocked)"
                        else -> "MOUSE Local Core"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            // Internet Master Quick Switch button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showConfirmToggleDialog = true }
                    .testTag("internet_quick_toggle"),
                color = if (uiState.permissionState.isInternetEnabled) AccentGreen.copy(alpha = 0.15f) else AmberSecondary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.permissionState.isInternetEnabled) AccentGreen else AmberSecondary
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.permissionState.isInternetEnabled) Icons.Default.Public else Icons.Default.PublicOff,
                        contentDescription = "Internet Toggle",
                        tint = if (uiState.permissionState.isInternetEnabled) AccentGreen else AmberSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.permissionState.isInternetEnabled) "NET ON" else "NET OFF",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.permissionState.isInternetEnabled) AccentGreen else AmberSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary
        )
    )

    if (showConfirmToggleDialog) {
        val willEnable = !uiState.permissionState.isInternetEnabled
        AlertDialog(
            onDismissRequest = { showConfirmToggleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security Shield",
                    tint = if (willEnable) AmberSecondary else CyanPrimary
                )
            },
            title = {
                Text(text = if (willEnable) "Enable Internet Access?" else "Enforce Offline Security?")
            },
            text = {
                Text(
                    text = if (willEnable) {
                        "Enabling Internet allows MOUSE to reach configured online AI endpoints (Gemini/OpenAI) and conduct live web research.\n\n" +
                                "All requests remain authenticated and logged. You can revoke this anytime."
                    } else {
                        "Disabling Internet cuts all outgoing network calls immediately. MOUSE will operate strictly on-device using local knowledge, memory, and local AI reasoning."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleInternet(willEnable)
                        showConfirmToggleDialog = false
                    },
                    modifier = Modifier.testTag("confirm_internet_toggle")
                ) {
                    Text(text = if (willEnable) "Authorize Internet" else "Go Offline", color = CyanPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmToggleDialog = false }) {
                    Text(text = "Cancel", color = TextSecondary)
                }
            }
        )
    }
}
