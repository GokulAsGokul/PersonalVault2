
package com.gokul.personalvault.ui.screens

import com.gokul.personalvault.ui.components.VaultSnackbarOverlay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gokul.personalvault.data.firebase.HouseholdInvitationRepositoryV2
import java.text.DateFormat
import java.util.Date

/**
 * Household join-code centre.
 *
 * Owner:
 * - Generates a one-time join code.
 * - Reviews membership requests.
 * - Approves or rejects requests.
 *
 * Joining member:
 * - Enters the code using their own signed-in account.
 */
@Composable
fun HouseholdInvitationV2Screen(
    onBack: () -> Unit,
    vaultId: String,
    memberUid: String,
    repository: HouseholdInvitationRepositoryV2 = remember {
        HouseholdInvitationRepositoryV2()
    }
) {
    var joinCodeInput by remember { mutableStateOf("") }
    var generatedCode by remember {
        mutableStateOf<HouseholdInvitationRepositoryV2.JoinCode?>(null)
    }

    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val requests = remember {
        mutableStateListOf<HouseholdInvitationRepositoryV2.MembershipRequest>()
    }

    fun refreshRequests() {
        loading = true

        repository.loadMembershipRequests { result ->
            loading = false

            result.fold(
                onSuccess = {
                    requests.clear()
                    requests.addAll(it)
                },
                onFailure = {
                    message = it.localizedMessage
                        ?: "Unable to load membership requests."
                    isError = true
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshRequests()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Household",
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }

            Text(
                text = "Connect two separate accounts using a one-time join code. " +
                    "The household owner must approve the joining request.",
                style = MaterialTheme.typography.bodyMedium
            )

            SharedDeviceApprovalPanelV2(vaultId = vaultId, memberUid = memberUid)

            // OWNER: GENERATE JOIN CODE
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Generate household join code",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Generate a code and share it privately with your " +
                            "household member. It expires after 15 minutes and can " +
                            "only be used once.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            busy = true
                            message = ""
                            generatedCode = null

                            repository.createJoinCode { result ->
                                busy = false

                                result.fold(
                                    onSuccess = {
                                        generatedCode = it
                                        message = "Join code generated successfully."
                                        isError = false
                                    },
                                    onFailure = {
                                        message = it.localizedMessage
                                            ?: "Unable to generate join code."
                                        isError = true
                                    }
                                )
                            }
                        },
                        enabled = !busy && !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate join code")
                    }

                    generatedCode?.let { item ->
                        HorizontalDivider()

                        Text(
                            text = "Your join code",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(
                            text = item.code,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Expires: ${
                                DateFormat.getDateTimeInstance().format(
                                    Date(item.expiresAt)
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Share this code only with the intended household member.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        TextButton(
                            onClick = {
                                busy = true

                                repository.cancelJoinCode { result ->
                                    busy = false

                                    result.fold(
                                        onSuccess = {
                                            generatedCode = null
                                            message = "Active join code revoked."
                                            isError = false
                                        },
                                        onFailure = {
                                            message = it.localizedMessage
                                                ?: "Unable to revoke join code."
                                            isError = true
                                        }
                                    )
                                }
                            },
                            enabled = !busy
                        ) {
                            Text("Revoke active join code")
                        }
                    }
                }
            }

            // MEMBER: SUBMIT JOIN CODE
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Join a household",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Sign in using your own account, enter the code " +
                            "provided by the household owner, and submit your request.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = {
                            joinCodeInput = it
                                .uppercase()
                                .filter { character ->
                                    character.isLetterOrDigit()
                                }
                                .take(10)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("10-character join code") },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (joinCodeInput.length != 10) {
                                message = "Enter the complete 10-character join code."
                                isError = true
                            } else {
                                busy = true
                                message = ""

                                repository.submitJoinCode(joinCodeInput) { result ->
                                    busy = false

                                    result.fold(
                                        onSuccess = { status ->
                                            joinCodeInput = ""

                                            message = if (
                                                status == "awaiting_owner_approval"
                                            ) {
                                                "Request submitted. Waiting for the household owner to approve."
                                            } else {
                                                "Join request status: $status"
                                            }

                                            isError = false
                                        },
                                        onFailure = {
                                            message = it.localizedMessage
                                                ?: "Unable to submit join code."
                                            isError = true
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !busy && !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit join request")
                    }
                }
            }

            // OWNER: APPROVE OR REJECT
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Membership requests",
                            style = MaterialTheme.typography.titleMedium
                        )

                        TextButton(
                            onClick = { refreshRequests() },
                            enabled = !loading && !busy
                        ) {
                            Text("Refresh")
                        }
                    }

                    if (requests.isEmpty()) {
                        Text(
                            text = "No requests awaiting approval.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    requests.forEach { item ->
                        HorizontalDivider()

                        Text(
                            text = "New household member request",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Account reference: ${
                                item.inviteeUid.takeLast(6)
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Status: ${item.status}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        item.createdAt?.let { timestamp ->
                            Text(
                                text = "Requested: ${
                                    DateFormat.getDateTimeInstance().format(
                                        Date(timestamp)
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    busy = true
                                    message = ""

                                    repository.approve(item.requestId) { result ->
                                        busy = false

                                        result.fold(
                                            onSuccess = { status ->
                                                message = if (status == "approved") {
                                                    "Membership approved. The member is now connected to the owner's V2 vault."
                                                } else {
                                                    "Approval status: $status"
                                                }

                                                isError = false
                                                refreshRequests()
                                            },
                                            onFailure = {
                                                message = it.localizedMessage
                                                    ?: "Unable to approve membership."
                                                isError = true
                                            }
                                        )
                                    }
                                },
                                enabled = !busy
                            ) {
                                Text("Approve")
                            }

                            TextButton(
                                onClick = {
                                    busy = true
                                    message = ""

                                    repository.reject(item.requestId) { result ->
                                        busy = false

                                        result.fold(
                                            onSuccess = {
                                                message = "Membership request rejected."
                                                isError = false
                                                refreshRequests()
                                            },
                                            onFailure = {
                                                message = it.localizedMessage
                                                    ?: "Unable to reject request."
                                                isError = true
                                            }
                                        )
                                    }
                                },
                                enabled = !busy
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }

            if (loading || busy) {
                CircularProgressIndicator()
            }

            VaultSnackbarOverlay(message = message, onDismiss = { message = "" })

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Joining a household connects membership only. " +
                    "Password sharing and encrypted key exchange must be completed " +
                    "separately before shared vault data is enabled.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
