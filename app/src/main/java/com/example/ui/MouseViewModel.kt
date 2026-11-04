package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MouseEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.KnowledgeEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.ResearchEntity
import com.example.security.AutonomyController
import com.example.security.KeystoreManager
import com.example.security.OperatingMode
import com.example.security.PermissionGate
import com.example.service.BackupRestoreService
import com.example.service.DeviceControlService
import com.example.service.ResearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class MouseViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val permissionGate = PermissionGate.getInstance(application)
    val keystoreManager = KeystoreManager(application)
    val autonomyController = AutonomyController.getInstance(application)

    val mouseEngine = MouseEngine(application, database, permissionGate, keystoreManager)
    val researchService = ResearchService(database.researchDao(), database.knowledgeDao(), permissionGate)
    val deviceControlService = DeviceControlService(application, database.securityDao(), autonomyController)
    val backupRestoreService = BackupRestoreService(database)

    private val _uiState = MutableStateFlow(MouseUiState())
    val uiState: StateFlow<MouseUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    init {
        // Init TTS
        try {
            textToSpeech = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                }
            }
        } catch (e: Exception) {
            // Ignore TTS init failure in headless or restricted environments
        }

        // Observe permission state
        viewModelScope.launch {
            permissionGate.state.collectLatest { pState ->
                _uiState.update { it.copy(permissionState = pState) }
            }
        }

        // Observe operating mode & pending actions
        viewModelScope.launch {
            autonomyController.mode.collectLatest { mode ->
                _uiState.update { it.copy(operatingMode = mode) }
            }
        }
        viewModelScope.launch {
            autonomyController.pendingAction.collectLatest { action ->
                _uiState.update { it.copy(pendingAction = action) }
            }
        }

        // Observe preferred provider
        viewModelScope.launch {
            mouseEngine.selectedProviderId.collectLatest { pId ->
                _uiState.update { it.copy(activeProviderId = pId) }
            }
        }

        // Observe conversations
        viewModelScope.launch {
            database.conversationDao().getAllConversations().collectLatest { convs ->
                _uiState.update { current ->
                    val activeId = if (current.activeConversationId.isNotBlank() && convs.any { it.id == current.activeConversationId }) {
                        current.activeConversationId
                    } else {
                        convs.firstOrNull()?.id ?: createNewConversationDirect()
                    }
                    current.copy(conversations = convs, activeConversationId = activeId)
                }
                observeActiveMessages()
            }
        }

        // Observe memories
        viewModelScope.launch {
            database.memoryDao().getAllMemories().collectLatest { mems ->
                _uiState.update { it.copy(memories = mems) }
            }
        }

        // Observe knowledge
        viewModelScope.launch {
            database.knowledgeDao().getAllKnowledge().collectLatest { list ->
                _uiState.update { it.copy(knowledgeList = list) }
            }
        }

        // Observe research
        viewModelScope.launch {
            database.researchDao().getAllResearch().collectLatest { res ->
                _uiState.update { it.copy(researchList = res) }
            }
        }

        // Observe audit logs
        viewModelScope.launch {
            database.securityDao().getAllAuditLogs().collectLatest { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }

        // Observe local GGUF models
        viewModelScope.launch {
            mouseEngine.localProvider.modelManager.modelsState.collectLatest { models ->
                _uiState.update { it.copy(managedModels = models, activeModelMetrics = mouseEngine.localProvider.runtime.lastMetrics) }
            }
        }

        // Observe network audit tracker events
        viewModelScope.launch {
            com.example.security.NetworkAuditTracker.events.collectLatest { events ->
                _uiState.update { it.copy(networkAuditEvents = events) }
            }
        }

        // Initial hardware audit & telemetry
        refreshHardwareAndTelemetry()

        // Initial OpenAI-compatible config from persistent storage
        _uiState.update {
            it.copy(
                openAiEndpoint = mouseEngine.openAiProvider.endpointUrl,
                openAiModel = mouseEngine.openAiProvider.modelName
            )
        }
    }

    private fun observeActiveMessages() {
        val activeId = _uiState.value.activeConversationId
        if (activeId.isBlank()) return
        viewModelScope.launch {
            database.messageDao().getMessagesForConversation(activeId).collectLatest { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    private fun createNewConversationDirect(): String {
        val newId = UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = newId,
                    title = "Session ${System.currentTimeMillis() % 10000}"
                )
            )
        }
        return newId
    }

    fun navigateTo(screen: MouseScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun startNewConversation() {
        val newId = UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = newId,
                    title = "Conversation ${System.currentTimeMillis() % 1000}"
                )
            )
            _uiState.update { it.copy(activeConversationId = newId, currentScreen = MouseScreen.CHAT) }
            observeActiveMessages()
        }
    }

    fun switchConversation(conversationId: String) {
        _uiState.update { it.copy(activeConversationId = conversationId, currentScreen = MouseScreen.CHAT) }
        observeActiveMessages()
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.conversationDao().deleteConversation(id)
            database.messageDao().deleteMessagesForConversation(id)
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isGenerating) return

        val convId = _uiState.value.activeConversationId.ifBlank {
            createNewConversationDirect()
        }

        _uiState.update { it.copy(inputText = "", isGenerating = true) }

        viewModelScope.launch {
            try {
                mouseEngine.processUserMessage(convId, text)
            } catch (e: Exception) {
                showToast("Error processing message: ${e.localizedMessage}")
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun toggleMasterInternetPermission(enabled: Boolean) {
        permissionGate.setMasterInternetPermission(enabled)
        val msg = if (enabled) "Internet Permission set to ON. Online AI & web services enabled." else "Internet Permission set to OFF. Offline-first security enforced."
        showToast(msg)
    }

    fun setSubPermission(onlineAi: Boolean? = null, webSearch: Boolean? = null, research: Boolean? = null) {
        permissionGate.setSubPermission(onlineAi, webSearch, research)
    }

    fun setPreferredProvider(providerId: String) {
        mouseEngine.setPreferredProvider(providerId)
        showToast("Active Provider set to: $providerId")
    }

    fun setOperatingMode(mode: OperatingMode) {
        autonomyController.setOperatingMode(mode)
        showToast("Operating mode set to: ${mode.name}")
    }

    fun storeApiKey(providerId: String, key: String) {
        keystoreManager.storeApiKey(providerId, key)
        showToast("API Key securely encrypted & stored in Keystore.")
    }

    fun getMaskedApiKey(providerId: String): String {
        return keystoreManager.getMaskedApiKey(providerId)
    }

    fun saveOpenAiConfig(endpoint: String, model: String, key: String? = null) {
        if (endpoint.isNotBlank()) {
            mouseEngine.openAiProvider.endpointUrl = endpoint
        }
        if (model.isNotBlank()) {
            mouseEngine.openAiProvider.modelName = model
        }
        if (!key.isNullOrBlank()) {
            keystoreManager.storeApiKey("openai_compatible", key.trim())
        }
        _uiState.update {
            it.copy(
                openAiEndpoint = mouseEngine.openAiProvider.endpointUrl,
                openAiModel = mouseEngine.openAiProvider.modelName
            )
        }
        showToast("OpenAI-compatible configuration saved.")
    }

    // Memories
    fun addMemory(category: String, key: String, content: String, importance: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().insertMemory(
                MemoryEntity(
                    category = category,
                    key = key.trim(),
                    content = content.trim(),
                    importance = importance
                )
            )
            showToast("Saved to Personal Memory Vault")
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().deleteMemory(id)
            showToast("Memory deleted from vault")
        }
    }

    // Knowledge
    fun addKnowledge(title: String, category: String, content: String, tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.knowledgeDao().insertKnowledge(
                KnowledgeEntity(
                    title = title.trim(),
                    category = category.trim(),
                    content = content.trim(),
                    tags = tags.trim()
                )
            )
            showToast("Knowledge Card added")
        }
    }

    fun deleteKnowledge(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.knowledgeDao().deleteKnowledge(id)
            showToast("Knowledge Card removed")
        }
    }

    // Research
    fun conductResearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val res = researchService.executeResearch(query)
                showToast("Research complete (${if (res.isLiveWeb) "Online Multi-Source" else "Offline Local"})")
            } catch (e: Exception) {
                showToast("Research failed: ${e.localizedMessage}")
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun toggleResearchVerification(id: String, currentStatus: String) {
        viewModelScope.launch {
            researchService.toggleVerification(id, currentStatus)
        }
    }

    // Hardware & Telemetry
    fun refreshHardwareAndTelemetry() {
        viewModelScope.launch(Dispatchers.IO) {
            val audit = mouseEngine.localProvider.auditHardware()
            val telemetry = deviceControlService.getTelemetry()
            _uiState.update { it.copy(hardwareAudit = audit, telemetry = telemetry) }
        }
    }

    // Device actions
    fun launchSettings() {
        viewModelScope.launch {
            deviceControlService.launchSystemSettings()
        }
    }

    fun launchWirelessSettings() {
        viewModelScope.launch {
            deviceControlService.launchWirelessSettings()
        }
    }

    fun confirmPendingAction() {
        val action = _uiState.value.pendingAction ?: return
        viewModelScope.launch {
            action.onConfirm()
            autonomyController.clearPendingAction()
        }
    }

    fun cancelPendingAction() {
        val action = _uiState.value.pendingAction ?: return
        viewModelScope.launch {
            action.onCancel()
            autonomyController.clearPendingAction()
        }
    }

    // TTS
    fun speakText(text: String) {
        textToSpeech?.speak(text.take(400), TextToSpeech.QUEUE_FLUSH, null, "MOUSE_SPEECH")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    // Voice recognition
    fun startVoiceInput() {
        val app = getApplication<Application>()
        if (SpeechRecognizer.isRecognitionAvailable(app)) {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _uiState.update { it.copy(isListeningVoice = true) }
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _uiState.update { it.copy(isListeningVoice = false) }
                        }
                        override fun onError(error: Int) {
                            _uiState.update { it.copy(isListeningVoice = false) }
                            showToast("Voice input ended.")
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spokenText = matches?.firstOrNull() ?: ""
                            _uiState.update { it.copy(isListeningVoice = false, inputText = spokenText) }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                showToast("Voice input not available: ${e.localizedMessage}")
            }
        } else {
            showToast("Speech recognition is not available on this device.")
        }
    }

    fun stopVoiceInput() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isListeningVoice = false) }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        return backupRestoreService.createBackupJson()
    }

    suspend fun restoreBackup(json: String, clearExisting: Boolean): Boolean {
        val success = backupRestoreService.restoreFromJson(json, clearExisting)
        if (success) {
            showToast("Archive restored successfully")
            refreshHardwareAndTelemetry()
        } else {
            showToast("Restore failed: invalid backup schema")
        }
        return success
    }

    // Local GGUF Model Management
    fun checkModelCompatibility(modelId: String): Pair<Boolean, String> {
        return mouseEngine.localProvider.modelManager.checkHardwareCompatibility(modelId)
    }

    fun checkModelStorage(modelId: String): Pair<Boolean, String> {
        return mouseEngine.localProvider.modelManager.checkStorageAvailability(modelId)
    }

    fun startModelDownload(modelId: String) {
        viewModelScope.launch {
            try {
                showToast("Initiating GGUF download...")
                mouseEngine.localProvider.modelManager.downloadModel(modelId)
                showToast("Download complete. Model ready to load.")
            } catch (e: Exception) {
                showToast("Download stopped: ${e.localizedMessage}")
            }
        }
    }

    fun cancelModelDownload(modelId: String) {
        mouseEngine.localProvider.modelManager.cancelDownload(modelId)
        showToast("Download cancelled.")
    }

    fun loadModelIntoRuntime(modelId: String) {
        viewModelScope.launch {
            showToast("Loading GGUF model into memory...")
            val res = mouseEngine.localProvider.modelManager.loadModelIntoRuntime(modelId)
            if (res.success) {
                showToast("Loaded ${res.modelName} in ${res.loadTimeMs}ms (${res.memoryUsedMb}MB RAM)")
            } else {
                showToast("Load failed: ${res.errorMessage}")
            }
        }
    }

    fun unloadActiveModel() {
        mouseEngine.localProvider.modelManager.unloadActiveModel()
        showToast("GGUF model unloaded from memory.")
    }

    fun deleteModel(modelId: String) {
        val deleted = mouseEngine.localProvider.modelManager.deleteModel(modelId)
        if (deleted) {
            showToast("Model removed from storage.")
        } else {
            showToast("Could not delete model.")
        }
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}
