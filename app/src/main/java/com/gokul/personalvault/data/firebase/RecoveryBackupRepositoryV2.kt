package com.gokul.personalvault.data.firebase

import com.gokul.personalvault.security.RecoveryCryptoV2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Stores and restores encrypted recovery envelopes for Personal Vault 2.0.
 * Firestore receives only the encrypted envelope and metadata.
 */
object RecoveryBackupRepositoryV2 {
    private const val ROOT = "vaultsV2"
    private const val MEMBERS = "members"
    private const val BACKUPS = "recoveryBackups"
    private const val RECOVERY_STATE = "recoveryState"
    private const val CURRENT = "current"

    data class SavedBackup(
        val backupId: String,
        val vaultId: String,
        val memberUid: String
    )

    fun createAndActivateBackup(
        vaultId: String,
        vaultKey: ByteArray,
        recoveryPassword: CharArray,
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        db: FirebaseFirestore = FirebaseFirestore.getInstance(),
        onResult: (Result<SavedBackup>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null || !user.isEmailVerified) {
            recoveryPassword.fill('\u0000')
            onResult(Result.failure(IllegalStateException("Please sign in with a verified email account.")))
            return
        }

        val memberUid = user.uid
        if (vaultId.isBlank() || vaultKey.size != 32) {
            recoveryPassword.fill('\u0000')
            onResult(Result.failure(IllegalArgumentException("A valid vault ID and 256-bit vault key are required.")))
            return
        }

        val envelope = try {
            RecoveryCryptoV2.createRecoveryEnvelope(
                vaultKey = vaultKey,
                recoveryPassword = recoveryPassword,
                vaultId = vaultId,
                memberUid = memberUid
            )
        } catch (error: Exception) {
            onResult(Result.failure(error))
            return
        } finally {
            recoveryPassword.fill('\u0000')
        }

        val memberRef = db.collection(ROOT)
            .document(vaultId)
            .collection(MEMBERS)
            .document(memberUid)

        val backupId = UUID.randomUUID().toString()
        val backupRef = memberRef.collection(BACKUPS).document(backupId)
        val currentRef = memberRef.collection(RECOVERY_STATE).document(CURRENT)

        val backupData = envelope.toMap().toMutableMap().apply {
            put("createdAt", FieldValue.serverTimestamp())
        }

        backupRef.set(backupData)
            .addOnSuccessListener {
                backupRef.get()
                    .addOnSuccessListener { savedSnapshot ->
                        if (!savedSnapshot.exists()) {
                            onResult(Result.failure(IllegalStateException("The new recovery backup could not be verified.")))
                            return@addOnSuccessListener
                        }

                        val savedData = savedSnapshot.data ?: emptyMap()

                        // Firestore numeric values may be Long while envelope.version is Int.
                        // Compare numeric values, not their boxed Kotlin types.
                        val savedVersion = (savedData["version"] as? Number)?.toInt()
                        val matches = savedVersion == envelope.version &&
                            savedData["kdf"] == envelope.kdf &&
                            savedData["cipher"] == envelope.cipher &&
                            savedData["salt"] == envelope.saltBase64 &&
                            savedData["nonce"] == envelope.nonceBase64 &&
                            savedData["ciphertext"] == envelope.ciphertextBase64

                        if (!matches) {
                            onResult(
                                Result.failure(
                                    IllegalStateException(
                                        "Recovery backup verification failed. The active backup was not changed."
                                    )
                                )
                            )
                            return@addOnSuccessListener
                        }

                        val pointerData = mapOf(
                            "activeBackupId" to backupId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )

                        currentRef.set(pointerData)
                            .addOnSuccessListener {
                                onResult(Result.success(SavedBackup(backupId, vaultId, memberUid)))
                            }
                            .addOnFailureListener { error -> onResult(Result.failure(error)) }
                    }
                    .addOnFailureListener { error -> onResult(Result.failure(error)) }
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun restoreActiveBackup(
        vaultId: String,
        recoveryPassword: CharArray,
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        db: FirebaseFirestore = FirebaseFirestore.getInstance(),
        onResult: (Result<ByteArray>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null || !user.isEmailVerified) {
            recoveryPassword.fill('\u0000')
            onResult(Result.failure(IllegalStateException("Please sign in with a verified email account.")))
            return
        }

        if (vaultId.isBlank()) {
            recoveryPassword.fill('\u0000')
            onResult(Result.failure(IllegalArgumentException("Vault ID is missing.")))
            return
        }

        val memberUid = user.uid
        val memberRef = db.collection(ROOT)
            .document(vaultId)
            .collection(MEMBERS)
            .document(memberUid)
        val currentRef = memberRef.collection(RECOVERY_STATE).document(CURRENT)

        currentRef.get()
            .addOnSuccessListener { pointerSnapshot ->
                val backupId = pointerSnapshot.getString("activeBackupId")
                if (!pointerSnapshot.exists() || backupId.isNullOrBlank()) {
                    recoveryPassword.fill('\u0000')
                    onResult(Result.failure(IllegalStateException("No active online recovery backup was found.")))
                    return@addOnSuccessListener
                }

                memberRef.collection(BACKUPS).document(backupId).get()
                    .addOnSuccessListener { backupSnapshot ->
                        if (!backupSnapshot.exists()) {
                            recoveryPassword.fill('\u0000')
                            onResult(Result.failure(IllegalStateException("The active recovery backup is missing.")))
                            return@addOnSuccessListener
                        }

                        try {
                            val envelope = RecoveryCryptoV2.RecoveryEnvelope.fromMap(
                                backupSnapshot.data ?: emptyMap()
                            )
                            val restoredKey = RecoveryCryptoV2.restoreVaultKey(
                                envelope = envelope,
                                recoveryPassword = recoveryPassword,
                                vaultId = vaultId,
                                memberUid = memberUid
                            )
                            onResult(Result.success(restoredKey))
                        } catch (error: Exception) {
                            onResult(Result.failure(error))
                        } finally {
                            recoveryPassword.fill('\u0000')
                        }
                    }
                    .addOnFailureListener { error ->
                        recoveryPassword.fill('\u0000')
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                recoveryPassword.fill('\u0000')
                onResult(Result.failure(error))
            }
    }
}
