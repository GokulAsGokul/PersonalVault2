package com.gokul.personalvault.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity

private val VaultSuccessGreen = Color(0xFF218838)
private val VaultErrorRed = Color(0xFFC62828)
private val VaultInfoBlue = Color(0xFF37474F)

enum class VaultSnackbarType { SUCCESS, ERROR, INFO }

@Composable
fun VaultSnackbarHost(
    hostState: SnackbarHostState,
    type: VaultSnackbarType,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val currentMessage = hostState.currentSnackbarData?.visuals?.message
    val currentType = currentMessage?.let(::classifyVaultSnackbar)

    LaunchedEffect(currentMessage, currentType) {
        if (currentMessage != null && currentType == VaultSnackbarType.ERROR) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val messageType = classifyVaultSnackbar(data.visuals.message)
        val background = when (messageType) {
            VaultSnackbarType.SUCCESS -> VaultSuccessGreen
            VaultSnackbarType.ERROR -> VaultErrorRed
            VaultSnackbarType.INFO -> VaultInfoBlue
        }
        Snackbar(
            snackbarData = data,
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
        listOf("unable", "failed", "failure", "error", "could not", "cannot", "can't",
            "not allowed", "not available", "denied", "invalid", "must ", "please select",
            "please enter", "enter ", "select ", "choose ", "required", "do not match", "does not match", "too large", "exceed", "cancelled", "canceled", "only an empty",
            "no files", "duplicate", "already exists", "does not", "did not")
            .any(value::contains) -> VaultSnackbarType.ERROR
        listOf("success", "saved", "created", "updated", "deleted", "restored",
            "uploaded", "moved", "copied", "ready", "approved", "unlocked", "revoked",
            "started", "queued", "replaced", "verified")
            .any(value::contains) -> VaultSnackbarType.SUCCESS
        else -> VaultSnackbarType.INFO
    }
}

@Composable
fun VaultSnackbarOverlay(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = classifyVaultSnackbar(message)
    val haptics = LocalHapticFeedback.current
    val bottomOffset = with(LocalDensity.current) { 72.dp.roundToPx() }
    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            if (type == VaultSnackbarType.ERROR) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            delay(4_000)
            onDismiss()
        }
    }
    if (message.isNotBlank()) {
        val background = when (type) {
            VaultSnackbarType.SUCCESS -> VaultSuccessGreen
            VaultSnackbarType.ERROR -> VaultErrorRed
            VaultSnackbarType.INFO -> VaultInfoBlue
        }
        Popup(
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, -bottomOffset),
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = false)
        ) {
            Snackbar(
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                containerColor = background,
                contentColor = Color.White,
                actionContentColor = Color.White,
                dismissActionContentColor = Color.White,
                content = {
                    androidx.compose.material3.Text(message, color = Color.White)
                }
            )
            }
        }
    }
}
