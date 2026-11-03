package com.example.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.example.data.local.dao.SecurityDao
import com.example.data.local.entities.SecurityAuditEntity
import com.example.security.AutonomyController
import com.example.security.OperatingMode
import com.example.security.PendingAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val freeStorageMb: Long,
    val networkConnected: Boolean
)

class DeviceControlService(
    private val context: Context,
    private val securityDao: SecurityDao,
    private val autonomyController: AutonomyController
) {
    fun getTelemetry(): DeviceTelemetry {
        // Battery status
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 100
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)

        return DeviceTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            freeStorageMb = freeMb,
            networkConnected = false // Will be updated by network status
        )
    }

    suspend fun executeAction(
        title: String,
        description: String,
        actionType: String,
        isSensitive: Boolean,
        action: () -> Unit
    ) = withContext(Dispatchers.Main) {
        val mode = autonomyController.mode.value
        val needsConfirm = autonomyController.requiresUserConfirmation(isSensitive)

        if (needsConfirm) {
            autonomyController.requestActionAuthorization(
                PendingAction(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    description = description,
                    actionType = actionType,
                    isSensitive = isSensitive,
                    onConfirm = {
                        action()
                        logSecurityAction(actionType, mode, "CONFIRMED_BY_USER", "$title executed after owner confirmation.")
                    },
                    onCancel = {
                        logSecurityAction(actionType, mode, "BLOCKED", "$title cancelled by owner.")
                    }
                )
            )
        } else {
            // Auto execute in autonomous mode or non-sensitive intelligent mode
            action()
            logSecurityAction(actionType, mode, "AUTHORIZED", "$title executed automatically under $mode mode.")
        }
    }

    suspend fun launchSystemSettings() {
        executeAction(
            title = "Open Android Settings",
            description = "Launch the device system settings application.",
            actionType = "LAUNCH_SETTINGS",
            isSensitive = true
        ) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun launchWirelessSettings() {
        executeAction(
            title = "Open Wireless & Network Settings",
            description = "Inspect WiFi and network connection preferences.",
            actionType = "LAUNCH_WIRELESS_SETTINGS",
            isSensitive = true
        ) {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private suspend fun logSecurityAction(actionType: String, mode: OperatingMode, status: String, details: String) = withContext(Dispatchers.IO) {
        securityDao.insertAuditLog(
            SecurityAuditEntity(
                action = actionType,
                operatingMode = mode.name,
                status = status,
                details = details
            )
        )
    }
}
