
package com.gokul.personalvault.data.firebase

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
     * Loads only the personal records owned by this member
     * and the shared records visible to approved vault members.
     */
    suspend fun load(
        vaultId: String,
        memberUid: String
    ): List<PasswordRecordV2> {

        validateSignedInMember(memberUid)

        val key = requireVaultKey(vaultId, memberUid)

        try {
            val collection = passwordCollection(vaultId)

            val sharedSnapshot = collection
                .whereEqualTo(
                    "scope",
                    PasswordRecordV2.SCOPE_SHARED
                )
                .get()
                .await()

            val personalSnapshot = collection
                .whereEqualTo(
                    "scope",
                    PasswordRecordV2.SCOPE_PERSONAL
                )
                .whereEqualTo("ownerUid", memberUid)
                .get()
                .await()

            val documents =
                sharedSnapshot.documents + personalSnapshot.documents

            val records = documents.map { snapshot ->

                val encryptedDocument =
                    EncryptedPasswordDocumentV2.fromSnapshot(snapshot)
                        ?: throw IllegalStateException(
                            "Password record ${snapshot.id} has an invalid encrypted document."
                        )

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
            key.fill(0)
        }
    }

    /**
     * Creates a new encrypted password document.
     *
     * Only the authenticated member can create a record
     * owned by their own UID.
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

        val key = requireVaultKey(vaultId, memberUid)

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
                "Unlock your V2 vault before accessing passwords."
            )
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