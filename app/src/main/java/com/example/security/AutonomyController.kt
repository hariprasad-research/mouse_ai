package com.example.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OperatingMode {
    CONTROLLED,
    INTELLIGENT,
    AUTONOMOUS
}

data class PendingAction(
    val id: String,
    val title: String,
    val description: String,
    val actionType: String,
    val isSensitive: Boolean,
    val onConfirm: suspend () -> Unit,
    val onCancel: suspend () -> Unit = {}
)

class AutonomyController private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        OperatingMode.valueOf(prefs.getString(KEY_MODE, OperatingMode.INTELLIGENT.name) ?: OperatingMode.INTELLIGENT.name)
    )
    val mode: StateFlow<OperatingMode> = _mode.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    fun setOperatingMode(newMode: OperatingMode) {
        _mode.value = newMode
        prefs.edit().putString(KEY_MODE, newMode.name).apply()
    }

    /**
     * Determines whether an action requires explicit user authorization dialog.
     */
    fun requiresUserConfirmation(isSensitive: Boolean): Boolean {
        return when (_mode.value) {
            OperatingMode.CONTROLLED -> true // Always asks
            OperatingMode.INTELLIGENT -> isSensitive // Asks for sensitive actions
            OperatingMode.AUTONOMOUS -> false // Auto-executes within boundary unless strictly destructive
        }
    }

    fun requestActionAuthorization(action: PendingAction) {
        _pendingAction.value = action
    }

    fun clearPendingAction() {
        _pendingAction.value = null
    }

    companion object {
        private const val PREFS_NAME = "mouse_autonomy"
        private const val KEY_MODE = "operating_mode"

        @Volatile
        private var INSTANCE: AutonomyController? = null

        fun getInstance(context: Context): AutonomyController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutonomyController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
