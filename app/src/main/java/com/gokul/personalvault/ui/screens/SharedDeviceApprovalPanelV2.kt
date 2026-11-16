package com.gokul.personalvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gokul.personalvault.data.firebase.SharedDeviceApprovalRepositoryV2

@Composable
fun SharedDeviceApprovalPanelV2(
    vaultId: String,
    memberUid: String,
    repository: SharedDeviceApprovalRepositoryV2 = remember {
        SharedDeviceApprovalRepositoryV2()
    }
) {
    val context = LocalContext.current
    val isOwner = vaultId == memberUid
    var deviceId by remember(vaultId, memberUid) { mutableStateOf("") }
    var deviceStatus by remember(vaultId, memberUid) { mutableStateOf("Registering this device…") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val devices = remember { mutableStateListOf<SharedDeviceApprovalRepositoryV2.Device>() }

    fun refreshDevices() {
        if (!isOwner) return
        busy = true
        repository.loadDevices { result ->
            busy = false
            result.fold(
                onSuccess = {
                    devices.clear()
                    devices.addAll(it)
                },
                onFailure = {
                    message = it.localizedMessage ?: "Unable to load registered devices."
                }
            )
        }
    }

    LaunchedEffect(vaultId, memberUid) {
        if (vaultId.isBlank() || memberUid.isBlank()) {
            deviceStatus = "Your vault is still loading."
            return@LaunchedEffect
        }
        val thisDeviceId = repository.currentDeviceId(context, vaultId, memberUid)
        deviceId = thisDeviceId
        repository.registerThisDevice(context, vaultId, memberUid) { result ->
            result.fold(
                onSuccess = { status ->
                    deviceStatus = when (status) {
                        "approved" -> "This device is approved for shared-key access."
                        "pending" -> "This device is registered. Waiting for owner approval."
                        else -> "Device status: $status"
                    }
                    if (isOwner) {
                        repository.ensureOwnerSharedKey(
                            vaultId, memberUid, thisDeviceId
                        ) { keyResult ->
                            keyResult.fold(
                                onSuccess = { message = "Shared vault key is ready on this owner device." },
                                onFailure = { message = it.localizedMessage ?: "Unable to unlock the owner shared key." }
                            )
                        }
                        refreshDevices()
                    }
                },
                onFailure = {
                    deviceStatus = it.localizedMessage ?: "Unable to register this device."
                }
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Shared password device access", style = MaterialTheme.typography.titleMedium)
            Text(
                "Household membership and shared-password access are separate. " +
                    "Each phone must be registered and explicitly approved.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(deviceStatus, style = MaterialTheme.typography.bodyMedium)

            if (!isOwner && deviceId.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        busy = true
                        repository.unlockThisDevice(vaultId, memberUid, deviceId) { result ->
                            busy = false
                            result.fold(
                                onSuccess = { message = "Shared passwords are unlocked on this device." },
                                onFailure = { message = it.localizedMessage ?: "Shared key is not available yet." }
                            )
                        }
                    },
                    enabled = !busy
                ) { Text("Check shared access") }
            }

            if (isOwner) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Registered devices", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(onClick = { refreshDevices() }, enabled = !busy) {
                        Text("Refresh")
                    }
                }
                if (devices.isEmpty() && !busy) {
                    Text("No registered devices found yet.", style = MaterialTheme.typography.bodySmall)
                }
                devices.forEach { device ->
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (device.memberUid == memberUid) "Your device" else "Household member device",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("Device: ${device.deviceId.take(8)}…", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${device.status}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (device.status == "pending") {
                                Button(
                                    onClick = {
                                        busy = true
                                        repository.approveDevice(device.memberUid, device.deviceId) { result ->
                                            busy = false
                                            result.fold(
                                                onSuccess = {
                                                    message = "Device approved. You can now provision its shared key."
                                                    refreshDevices()
                                                },
                                                onFailure = { message = it.localizedMessage ?: "Unable to approve device." }
                                            )
                                        }
                                    },
                                    enabled = !busy
                                ) { Text("Approve device") }
                            } else if (device.status == "approved" && device.memberUid != memberUid) {
                                Button(
                                    onClick = {
                                        busy = true
                                        repository.createEnvelopeForApprovedDevice(
                                            vaultId, device.memberUid, device.deviceId
                                        ) { result ->
                                            busy = false
                                            result.fold(
                                                onSuccess = { message = "Shared key provisioned for this device." },
                                                onFailure = { message = it.localizedMessage ?: "Unable to provision shared key." }
                                            )
                                        }
                                    },
                                    enabled = !busy
                                ) { Text("Grant shared access") }
                            }
                        }
                    }
                }
            }

            if (busy) CircularProgressIndicator()
            if (message.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
