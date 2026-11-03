package com.example.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionState(
    val isInternetEnabled: Boolean = false, // Strictly OFF by default
    val allowOnlineAi: Boolean = false,
    val allowWebSearch: Boolean = false,
    val allowResearchAggregation: Boolean = false,
    val isSafetySimulationMode: Boolean = false
)

class PermissionGate private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        PermissionState(
            isInternetEnabled = prefs.getBoolean(KEY_INTERNET_ENABLED, false),
            allowOnlineAi = prefs.getBoolean(KEY_ONLINE_AI, false),
            allowWebSearch = prefs.getBoolean(KEY_WEB_SEARCH, false),
            allowResearchAggregation = prefs.getBoolean(KEY_RESEARCH, false)
        )
    )
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    fun isInternetEnabled(): Boolean = _state.value.isInternetEnabled

    fun setMasterInternetPermission(enabled: Boolean) {
        val updated = if (enabled) {
            // When master internet is turned ON, enable online AI and web research services
            _state.value.copy(
                isInternetEnabled = true,
                allowOnlineAi = true,
                allowWebSearch = true,
                allowResearchAggregation = true
            )
        } else {
            // When master internet is turned OFF, immediately disable all network channels
            _state.value.copy(
                isInternetEnabled = false,
                allowOnlineAi = false,
                allowWebSearch = false,
                allowResearchAggregation = false
            )
        }
        _state.value = updated
        persistState(updated)
    }

    fun setSubPermission(onlineAi: Boolean? = null, webSearch: Boolean? = null, research: Boolean? = null) {
        // Can only enable sub-permissions if master internet is ON
        if (!_state.value.isInternetEnabled) return

        val updated = _state.value.copy(
            allowOnlineAi = onlineAi ?: _state.value.allowOnlineAi,
            allowWebSearch = webSearch ?: _state.value.allowWebSearch,
            allowResearchAggregation = research ?: _state.value.allowResearchAggregation
        )
        _state.value = updated
        persistState(updated)
    }

    /**
     * Strict check before any network operation. Throws SecurityException if Internet is blocked.
     */
    fun assertNetworkAllowed(feature: String) {
        if (!_state.value.isInternetEnabled) {
            throw SecurityException("MOUSE Internet Permission Gate blocked network call for '$feature'. Internet access is currently disabled by owner policy.")
        }
    }

    private fun persistState(state: PermissionState) {
        prefs.edit()
            .putBoolean(KEY_INTERNET_ENABLED, state.isInternetEnabled)
            .putBoolean(KEY_ONLINE_AI, state.allowOnlineAi)
            .putBoolean(KEY_WEB_SEARCH, state.allowWebSearch)
            .putBoolean(KEY_RESEARCH, state.allowResearchAggregation)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "mouse_permission_gate"
        private const val KEY_INTERNET_ENABLED = "internet_master_enabled"
        private const val KEY_ONLINE_AI = "sub_online_ai"
        private const val KEY_WEB_SEARCH = "sub_web_search"
        private const val KEY_RESEARCH = "sub_research"

        @Volatile
        private var INSTANCE: PermissionGate? = null

        fun getInstance(context: Context): PermissionGate {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PermissionGate(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
