package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MouseScreen
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

data class DrawerItem(
    val screen: MouseScreen,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun MouseDrawer(
    uiState: MouseUiState,
    onNavigate: (MouseScreen) -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val items = listOf(
        DrawerItem(MouseScreen.CHAT, "Chat / Terminal", Icons.AutoMirrored.Filled.Chat, "nav_chat"),
        DrawerItem(MouseScreen.MEMORY, "Personal Memory", Icons.Default.Psychology, "nav_memory"),
        DrawerItem(MouseScreen.KNOWLEDGE, "Knowledge Base", Icons.AutoMirrored.Filled.MenuBook, "nav_knowledge"),
        DrawerItem(MouseScreen.RESEARCH, "Independent Research", Icons.Default.TravelExplore, "nav_research"),
        DrawerItem(MouseScreen.PROVIDERS, "AI Provider Settings", Icons.Default.Tune, "nav_providers"),
        DrawerItem(MouseScreen.PERMISSIONS, "Internet Permission Gate", Icons.Default.Shield, "nav_permissions"),
        DrawerItem(MouseScreen.OFFLINE_HARDWARE, "Hardware & Local Models", Icons.Default.Memory, "nav_hardware"),
        DrawerItem(MouseScreen.AUTONOMY, "Device Actions & Autonomy", Icons.Default.Security, "nav_autonomy"),
        DrawerItem(MouseScreen.BACKUP_RESTORE, "Backup & Restore", Icons.Default.Backup, "nav_backup"),
        DrawerItem(MouseScreen.ABOUT, "About MOUSE", Icons.Default.Info, "nav_about")
    )

    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = SurfaceDark,
        drawerContentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyanPrimaryContainer)
                        .border(1.dp, CyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = CyanPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MOUSE OS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Mode: ${uiState.operatingMode.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = when (uiState.operatingMode.name) {
                            "CONTROLLED" -> AmberSecondary
                            "INTELLIGENT" -> CyanPrimary
                            else -> VioletTertiary
                        }
                    )
                }
            }

            // New Conversation Action
            Button(
                onClick = {
                    onNewConversation()
                    onCloseDrawer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_conversation_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimaryContainer,
                    contentColor = CyanPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Session", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "New Conversation", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Items
            items.forEach { item ->
                val selected = uiState.currentScreen == item.screen
                NavigationDrawerItem(
                    label = { Text(text = item.title, fontSize = 14.sp) },
                    selected = selected,
                    onClick = {
                        onNavigate(item.screen)
                        onCloseDrawer()
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (selected) CyanPrimary else TextSecondary
                        )
                    },
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag(item.testTag),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = SurfaceVariantDark,
                        selectedTextColor = CyanPrimary,
                        unselectedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(8.dp))

            // Recent Sessions
            Text(
                text = "CONVERSATIONS (${uiState.conversations.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            uiState.conversations.take(6).forEach { conv ->
                val isActive = conv.id == uiState.activeConversationId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSelectConversation(conv.id)
                            onCloseDrawer()
                        },
                    color = if (isActive) SurfaceVariantDark else SurfaceContainerDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = conv.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActive) CyanPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.conversations.size > 1) {
                            IconButton(
                                onClick = { onDeleteConversation(conv.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerDark)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Security: Strictly Enforced",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (uiState.permissionState.isInternetEnabled) "Net: ON" else "Net: OFF",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.permissionState.isInternetEnabled) AccentGreen else AmberSecondary
                    )
                }
            }
        }
    }
}
