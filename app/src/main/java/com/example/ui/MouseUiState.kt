package com.example.ui

import com.example.ai.providers.HardwareAudit
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.KnowledgeEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ResearchEntity
import com.example.data.local.entities.SecurityAuditEntity
import com.example.security.OperatingMode
import com.example.security.PendingAction
import com.example.security.PermissionState
import com.example.service.DeviceTelemetry

enum class MouseScreen {
    CHAT,
    MEMORY,
    KNOWLEDGE,
    RESEARCH,
    PROVIDERS,
    PERMISSIONS,
    OFFLINE_HARDWARE,
    AUTONOMY,
    BACKUP_RESTORE,
    ABOUT
}

data class MouseUiState(
    val currentScreen: MouseScreen = MouseScreen.CHAT,
    val activeConversationId: String = "",
    val conversations: List<ConversationEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val inputText: String = "",
    val isListeningVoice: Boolean = false,
    val permissionState: PermissionState = PermissionState(),
    val activeProviderId: String = "local_core",
    val operatingMode: OperatingMode = OperatingMode.INTELLIGENT,
    val pendingAction: PendingAction? = null,
    val hardwareAudit: HardwareAudit? = null,
    val telemetry: DeviceTelemetry? = null,
    val memories: List<MemoryEntity> = emptyList(),
    val knowledgeList: List<KnowledgeEntity> = emptyList(),
    val researchList: List<ResearchEntity> = emptyList(),
    val auditLogs: List<SecurityAuditEntity> = emptyList(),
    val managedModels: List<com.example.ai.runtime.ManagedModel> = emptyList(),
    val activeModelMetrics: com.example.ai.runtime.RuntimeMetrics? = null,
    val networkAuditEvents: List<com.example.security.NetworkAuditEvent> = emptyList(),
    val openAiEndpoint: String = "https://api.openai.com/v1",
    val openAiModel: String = "gpt-4o-mini",
    val toastMessage: String? = null
)
