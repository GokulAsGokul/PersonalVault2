package com.gokul.personalvault.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VaultSuccessGreen = Color(0xFF218838)
private val VaultErrorRed = Color(0xFFC62828)
private val VaultInfoBlue = Color(0xFF37474F)

enum class VaultSnackbarType { SUCCESS, ERROR, INFO }

data class VaultSnackbarNotice(val id: Long, val message: String, val type: VaultSnackbarType)

/** One app-wide notification channel; the dashboard owns the only visible host. */
object VaultSnackbarController {
    var notice by mutableStateOf<VaultSnackbarNotice?>(null)
        private set
    private var nextId = 0L

    fun show(message: String, type: VaultSnackbarType? = null) {
        if (message.isBlank()) return
        nextId += 1
        notice = VaultSnackbarNotice(nextId, message, type ?: classifyVaultSnackbar(message))
    }

    fun clear(id: Long) {
        if (notice?.id == id) notice = null
    }
}

@Composable
fun VaultSnackbarHost(
    hostState: SnackbarHostState,
    type: VaultSnackbarType = VaultSnackbarType.INFO,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val background = when (type) {
            VaultSnackbarType.SUCCESS -> VaultSuccessGreen
            VaultSnackbarType.ERROR -> VaultErrorRed
            VaultSnackbarType.INFO -> VaultInfoBlue
        }
        Snackbar(
            snackbarData = data,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            containerColor = background,
            contentColor = Color.White,
            actionColor = Color.White,
            dismissActionContentColor = Color.White
        )
    }
}

fun classifyVaultSnackbar(message: String): VaultSnackbarType {
    val value = message.lowercase()
    return when {
        listOf(
            "unable", "failed", "failure", "error", "could not", "cannot", "can't",
            "not allowed", "not available", "denied", "invalid", "incorrect", "wrong password",
            "bad decrypt", "decrypt", "must ", "please select", "please enter", "enter ",
            "select ", "choose ", "required", "do not match", "does not match", "too large",
            "exceed", "cancelled", "canceled", "only an empty", "no files", "duplicate",
            "already exists", "already in use", "does not", "did not", "couldn't",
            "exception", "permission"
        ).any(value::contains) -> VaultSnackbarType.ERROR
        listOf(
            "success", "saved", "created", "updated", "deleted", "restored", "uploaded",
            "moved", "copied", "ready", "approved", "unlocked", "revoked", "started",
            "queued", "replaced", "verified", "renamed", "generated", "submitted"
        ).any(value::contains) -> VaultSnackbarType.SUCCESS
        else -> VaultSnackbarType.INFO
    }
}

/** Compatibility bridge: forward screen messages to the dashboard host, never draw a popup. */
@Composable
fun VaultSnackbarOverlay(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            VaultSnackbarController.show(message)
            onDismiss()
        }
    }
}

/** Uses the Android vibrator directly instead of touch-only haptic feedback. */
fun triggerVaultErrorVibration(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(180L)
        }
    } catch (_: Exception) {
        // Keep snackbar feedback available if vibration is disabled by the device.
    }
}
