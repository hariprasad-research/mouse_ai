package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MouseScreen
import com.example.ui.MouseViewModel
import com.example.ui.components.ActionConfirmDialog
import com.example.ui.components.MouseAppBar
import com.example.ui.components.MouseDrawer
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AutonomyScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.KnowledgeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OfflineModelScreen
import com.example.ui.screens.PermissionGateScreen
import com.example.ui.screens.ProviderSettingsScreen
import com.example.ui.screens.ResearchScreen
import com.example.ui.theme.MouseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MouseTheme {
                val viewModel: MouseViewModel = viewModel()
                MouseMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MouseMainApp(viewModel: MouseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast / Snackbar listener
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Back handling: drawer first, then return to Chat screen
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && uiState.currentScreen != MouseScreen.CHAT) {
        viewModel.navigateTo(MouseScreen.CHAT)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MouseDrawer(
                uiState = uiState,
                onNavigate = { screen -> viewModel.navigateTo(screen) },
                onNewConversation = { viewModel.startNewConversation() },
                onSelectConversation = { id -> viewModel.switchConversation(id) },
                onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MouseAppBar(
                    uiState = uiState,
                    onMenuClick = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onToggleInternet = { enabled ->
                        viewModel.toggleMasterInternetPermission(enabled)
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (uiState.currentScreen) {
                    MouseScreen.CHAT -> {
                        ChatScreen(
                            uiState = uiState,
                            onInputChange = { viewModel.setInputText(it) },
                            onSendMessage = { viewModel.sendMessage() },
                            onStartVoice = { viewModel.startVoiceInput() },
                            onStopVoice = { viewModel.stopVoiceInput() },
                            onSpeakText = { viewModel.speakText(it) }
                        )
                    }
                    MouseScreen.MEMORY -> {
                        MemoryScreen(
                            memories = uiState.memories,
                            onAddMemory = { cat, key, content, imp ->
                                viewModel.addMemory(cat, key, content, imp)
                            },
                            onDeleteMemory = { id -> viewModel.deleteMemory(id) }
                        )
                    }
                    MouseScreen.KNOWLEDGE -> {
                        KnowledgeScreen(
                            knowledgeList = uiState.knowledgeList,
                            onAddKnowledge = { title, cat, content, tags ->
                                viewModel.addKnowledge(title, cat, content, tags)
                            },
                            onDeleteKnowledge = { id -> viewModel.deleteKnowledge(id) }
                        )
                    }
                    MouseScreen.RESEARCH -> {
                        ResearchScreen(
                            uiState = uiState,
                            onConductResearch = { query -> viewModel.conductResearch(query) },
                            onToggleVerification = { id, currentStatus ->
                                viewModel.toggleResearchVerification(id, currentStatus)
                            }
                        )
                    }
                    MouseScreen.PROVIDERS -> {
                        ProviderSettingsScreen(
                            uiState = uiState,
                            onSelectProvider = { pId -> viewModel.setPreferredProvider(pId) },
                            onSaveKey = { pId, key -> viewModel.storeApiKey(pId, key) },
                            getMaskedKey = { pId -> viewModel.getMaskedApiKey(pId) }
                        )
                    }
                    MouseScreen.PERMISSIONS -> {
                        PermissionGateScreen(
                            uiState = uiState,
                            onToggleMaster = { enabled -> viewModel.toggleMasterInternetPermission(enabled) },
                            onToggleSub = { onlineAi, webSearch, research ->
                                viewModel.setSubPermission(onlineAi, webSearch, research)
                            }
                        )
                    }
                    MouseScreen.OFFLINE_HARDWARE -> {
                        OfflineModelScreen(
                            uiState = uiState,
                            onRefreshHardware = { viewModel.refreshHardwareAndTelemetry() },
                            onStartDownload = { modelId -> viewModel.startModelDownload(modelId) },
                            onCancelDownload = { modelId -> viewModel.cancelModelDownload(modelId) },
                            onLoadModel = { modelId -> viewModel.loadModelIntoRuntime(modelId) },
                            onUnloadModel = { viewModel.unloadActiveModel() },
                            onDeleteModel = { modelId -> viewModel.deleteModel(modelId) },
                            onCheckCompatibility = { modelId -> viewModel.checkModelCompatibility(modelId) },
                            onCheckStorage = { modelId -> viewModel.checkModelStorage(modelId) }
                        )
                    }
                    MouseScreen.AUTONOMY -> {
                        AutonomyScreen(
                            uiState = uiState,
                            onSelectMode = { mode -> viewModel.setOperatingMode(mode) },
                            onLaunchSettings = { viewModel.launchSettings() },
                            onLaunchWirelessSettings = { viewModel.launchWirelessSettings() }
                        )
                    }
                    MouseScreen.BACKUP_RESTORE -> {
                        BackupRestoreScreen(
                            uiState = uiState,
                            onExportBackup = { viewModel.exportBackupJson() },
                            onRestoreBackup = { json, clearExisting ->
                                viewModel.restoreBackup(json, clearExisting)
                            }
                        )
                    }
                    MouseScreen.ABOUT -> {
                        AboutScreen()
                    }
                }

                // Action Authorization Dialog for Controlled / Sensitive tasks
                uiState.pendingAction?.let { pending ->
                    ActionConfirmDialog(
                        pendingAction = pending,
                        onConfirm = { viewModel.confirmPendingAction() },
                        onCancel = { viewModel.cancelPendingAction() }
                    )
                }
            }
        }
    }
}
