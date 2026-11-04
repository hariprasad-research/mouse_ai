package com.example.security

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList

data class NetworkAuditEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val caller: String,
    val destination: String,
    val wasBlocked: Boolean,
    val policyReason: String
)

object NetworkAuditTracker {

    private val _events = MutableStateFlow<List<NetworkAuditEvent>>(emptyList())
    val events: StateFlow<List<NetworkAuditEvent>> = _events.asStateFlow()

    private val eventHistory = CopyOnWriteArrayList<NetworkAuditEvent>()

    fun recordEvent(caller: String, destination: String, wasBlocked: Boolean, policyReason: String) {
        val event = NetworkAuditEvent(
            caller = caller,
            destination = destination,
            wasBlocked = wasBlocked,
            policyReason = policyReason
        )
        eventHistory.add(0, event)
        // Keep last 100 events
        val trimmed = eventHistory.take(100)
        _events.update { trimmed }
        try {
            Log.d("NetworkAuditTracker", "Recorded [blocked=$wasBlocked] $caller -> $destination ($policyReason)")
        } catch (_: Throwable) {
            // Ignored in JVM test environment
        }
    }

    fun getEventsSince(timestamp: Long): List<NetworkAuditEvent> {
        return eventHistory.filter { it.timestamp >= timestamp }
    }

    fun getOutgoingRequestsCountSince(timestamp: Long): Int {
        return eventHistory.count { it.timestamp >= timestamp && !it.wasBlocked }
    }

    fun getBlockedRequestsCountSince(timestamp: Long): Int {
        return eventHistory.count { it.timestamp >= timestamp && it.wasBlocked }
    }

    fun clear() {
        eventHistory.clear()
        _events.value = emptyList()
    }
}
