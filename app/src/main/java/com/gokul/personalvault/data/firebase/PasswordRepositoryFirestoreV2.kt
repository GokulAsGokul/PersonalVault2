
package com.gokul.personalvault.data.firebase

import com.gokul.personalvault.HouseholdCrypto
import com.gokul.personalvault.data.local.VaultKeySessionV2
import com.gokul.personalvault.data.model.EncryptedPasswordDocumentV2
import com.gokul.personalvault.data.model.PasswordRecordV2
import com.gokul.personalvault.security.PasswordRecordCryptoV2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

object PasswordRepositoryFirestoreV2 {

    private const val VAULT_COLLECTION = "vaultsV2"
    private const val PASSWORD_COLLECTION = "passwords"

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Loads personal records owned by this member and shared records
     * encrypted with the household key.
     */
    suspend fun load(
        vaultId: String,
        memberUid: String
    ): List<PasswordRecordV2> {

        validateSignedInMember(memberUid)

        val collection = passwordCollection(vaultId)

        val sharedSnapshot = collection
            .whereEqualTo("scope", PasswordRecordV2.SCOPE_SHARED)
            .get()
            .await()

        val personalSnapshot = collection
            .whereEqualTo("scope", PasswordRecordV2.SCOPE_PERSONAL)
            .whereEqualTo("ownerUid", memberUid)
            .get()
            .await()

        val documents = sharedSnapshot.documents + personalSnapshot.documents

        if (documents.isEmpty()) {
            return emptyList()
        }

        val needsPersonalKey = personalSnapshot.documents.isNotEmpty()
        val needsHouseholdKey = sharedSnapshot.documents.isNotEmpty()

        var personalKey: ByteArray? = null
        var householdKey: ByteArray? = null

        try {
            if (needsPersonalKey) {
                personalKey = requireVaultKey(vaultId, memberUid)
            }

            if (needsHouseholdKey) {
                householdKey = requireHouseholdKey(vaultId)
            }

            val records = documents.map { snapshot ->

                val encryptedDocument =
                    EncryptedPasswordDocumentV2.fromSnapshot(snapshot)
                        ?: throw IllegalStateException(
                            "Password record ${snapshot.id} has an invalid encrypted document."
                        )

                val key = when (
                    encryptedDocument.encryptionKeySource
                ) {
                    EncryptedPasswordDocumentV2.KEY_SOURCE_MEMBER_MASTER -> {
                        require(
                            encryptedDocument.scope ==
                                    PasswordRecordV2.SCOPE_PERSONAL
                        ) {
                            "Invalid encryption key source for a personal password."
                        }

                        personalKey
                            ?: throw IllegalStateException(
                                "Personal vault key is unavailable."
                            )
                    }

                    EncryptedPasswordDocumentV2.KEY_SOURCE_HOUSEHOLD -> {
                        require(
                            encryptedDocument.scope ==
                                    PasswordRecordV2.SCOPE_SHARED
                        ) {
                            "Invalid encryption key source for a shared password."
                        }

                        householdKey
                            ?: throw IllegalStateException(
                                "Household encryption key is unavailable."
                            )
                    }

                    EncryptedPasswordDocumentV2.KEY_SOURCE_LEGACY_MEMBER_MASTER -> {
                        throw IllegalStateException(
                            "This shared password is from the old test-data format. " +
                                    "Remove the old shared test records before continuing."
                        )
                    }

                    else -> {
                        throw IllegalStateException(
                            "Unsupported password encryption key source."
                        )
                    }
                }

                val record = PasswordRecordCryptoV2.decrypt(
                    encryptedPayload = encryptedDocument.encryptedPayload,
                    vaultKey = key,
                    vaultId = vaultId,
                    recordId = snapshot.id,
                    ownerUid = encryptedDocument.ownerUid,
                    scope = encryptedDocument.scope
                )

                require(record.id == snapshot.id) {
                    "Password record ID mismatch."
                }

                require(record.ownerUid == encryptedDocument.ownerUid) {
                    "Password owner mismatch."
                }

                require(record.scope == encryptedDocument.scope) {
                    "Password scope mismatch."
                }

                if (record.scope == PasswordRecordV2.SCOPE_PERSONAL) {
                    require(record.ownerUid == memberUid) {
                        "A personal password cannot be loaded by another member."
                    }
                }

                record
            }

            return records.sortedBy { it.websiteApp.lowercase() }

        } finally {
            personalKey?.fill(0)
            householdKey?.fill(0)
        }
    }

    /**
     * Creates a password using the correct encryption key:
     * MEMBER_MASTER for personal records.
     * HOUSEHOLD for shared records.
     */
    suspend fun create(
        vaultId: String,
        memberUid: String,
        scope: String,
        websiteApp: String,
        username: String,
        password: String,
        category: String,
        notes: String
    ): PasswordRecordV2 {

        validateSignedInMember(memberUid)

        require(
            scope == PasswordRecordV2.SCOPE_PERSONAL ||
                    scope == PasswordRecordV2.SCOPE_SHARED
        ) {
            "Invalid password scope."
        }

        require(websiteApp.isNotBlank()) {
            "Website / App is required."
        }

        require(password.isNotBlank()) {
            "Password is required."
        }

        val now = System.currentTimeMillis()

        val record = PasswordRecordV2(
            id = UUID.randomUUID().toString(),
            scope = scope,
            ownerUid = memberUid,
            websiteApp = websiteApp.trim(),
            username = username.trim(),
            password = password,
            category = category,
            notes = notes.trim(),
            createdAt = now,
            editedAt = now
        )

        val keySource =
            if (scope == PasswordRecordV2.SCOPE_PERSONAL) {
                EncryptedPasswordDocumentV2.KEY_SOURCE_MEMBER_MASTER
            } else {
                EncryptedPasswordDocumentV2.KEY_SOURCE_HOUSEHOLD
            }

        val key = if (scope == PasswordRecordV2.SCOPE_PERSONAL) {
            requireVaultKey(vaultId, memberUid)
        } else {
            requireHouseholdKey(vaultId)
        }

        try {
            val encryptedPayload = PasswordRecordCryptoV2.encrypt(
                record = record,
                vaultKey = key,
                vaultId = vaultId
            )

            val document = hashMapOf<String, Any>(
                "schemaVersion" to EncryptedPasswordDocumentV2.SCHEMA_VERSION,
                "ownerUid" to record.ownerUid,
                "scope" to record.scope,
                "encryptionKeySource" to keySource,
                "payloadVersion" to EncryptedPasswordDocumentV2.PAYLOAD_VERSION,
                "algorithm" to EncryptedPasswordDocumentV2.ALGORITHM,
                "encryptedPayload" to encryptedPayload,
                "createdAt" to FieldValue.serverTimestamp(),
                "editedAt" to FieldValue.serverTimestamp()
            )

            passwordCollection(vaultId)
                .document(record.id)
                .set(document)
                .await()

            return record

        } finally {
            key.fill(0)
        }
    }


    suspend fun update(
        vaultId: String, memberUid: String, original: PasswordRecordV2,
        websiteApp: String, username: String, password: String,
        category: String, notes: String
    ): PasswordRecordV2 {
        validateSignedInMember(memberUid)
        require(original.ownerUid == memberUid) {
            "Only the original owner can edit this password."
        }
        require(websiteApp.isNotBlank()) { "Website / App is required." }
        require(password.isNotBlank()) { "Password is required." }

        val updated = original.copy(
            websiteApp = websiteApp.trim(), username = username.trim(),
            password = password, category = category, notes = notes.trim(),
            editedAt = System.currentTimeMillis()
        )
        val key = if (original.scope == PasswordRecordV2.SCOPE_PERSONAL) {
            requireVaultKey(vaultId, memberUid)
        } else {
            requireHouseholdKey(vaultId)
        }
        try {
            val encryptedPayload = PasswordRecordCryptoV2.encrypt(
                record = updated, vaultKey = key, vaultId = vaultId
            )
            passwordCollection(vaultId).document(original.id).update(
                mapOf(
                    "encryptedPayload" to encryptedPayload,
                    "editedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return updated
        } finally {
            key.fill(0)
        }
    }

    suspend fun delete(vaultId: String, memberUid: String, record: PasswordRecordV2) {
        validateSignedInMember(memberUid)
        require(record.ownerUid == memberUid) {
            "Only the original owner can delete this password."
        }
        passwordCollection(vaultId).document(record.id).delete().await()
    }

    private fun passwordCollection(vaultId: String) =
        db.collection(VAULT_COLLECTION)
            .document(vaultId)
            .collection(PASSWORD_COLLECTION)

    private fun requireVaultKey(
        vaultId: String,
        memberUid: String
    ): ByteArray {
        return VaultKeySessionV2.copyKey(vaultId, memberUid)
            ?: throw IllegalStateException(
                "Unlock your V2 vault before accessing personal passwords."
            )
    }

    private fun requireHouseholdKey(vaultId: String): ByteArray {
        return try {
            HouseholdCrypto.copyHouseholdKey(vaultId)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Shared access is locked. Open Household and select " +
                        "Check shared access before opening shared passwords.",
                e
            )
        }
    }

    private fun validateSignedInMember(memberUid: String) {
        require(memberUid.isNotBlank()) {
            "Member UID is required."
        }

        val signedInUid = FirebaseAuth.getInstance().currentUser?.uid

        check(signedInUid == memberUid) {
            "The signed-in account does not match the active vault member."
        }
    }
}
