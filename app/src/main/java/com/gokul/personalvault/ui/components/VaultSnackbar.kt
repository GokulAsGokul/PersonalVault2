package com.gokul.personalvault.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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

    LaunchedEffect(currentMessage, type) {
        if (currentMessage != null && type == VaultSnackbarType.ERROR) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val background = when (type) {
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
            "please enter", "too large", "exceed", "cancelled", "canceled", "only an empty",
            "no files", "duplicate", "already exists", "does not", "did not")
            .any(value::contains) -> VaultSnackbarType.ERROR
        listOf("success", "saved", "created", "updated", "deleted", "restored",
            "uploaded", "moved", "copied", "ready", "approved", "unlocked",
            "started", "queued", "replaced", "verified")
            .any(value::contains) -> VaultSnackbarType.SUCCESS
        else -> VaultSnackbarType.INFO
    }
}
