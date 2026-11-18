
package com.gokul.personalvault.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class EncryptedPasswordDocumentV2(
    val encryptionKeySource: String = "MEMBER_MASTER",
    val ownerUid: String,
    val scope: String,
    val payloadVersion: Long,
    val algorithm: String,
    val encryptedPayload: String,
    val createdAt: Long,
    val editedAt: Long

) {
    companion object {
        const val SCHEMA_VERSION = 2L
        const val PAYLOAD_VERSION = 2L
        const val ALGORITHM = "AES-256-GCM"
        const val KEY_SOURCE_MEMBER_MASTER = "MEMBER_MASTER"
        const val KEY_SOURCE_HOUSEHOLD = "HOUSEHOLD"
        const val KEY_SOURCE_LEGACY_MEMBER_MASTER = "LEGACY_MEMBER_MASTER"

        fun fromSnapshot(
            snapshot: DocumentSnapshot
        ): EncryptedPasswordDocumentV2? {
            return try {
                val schemaVersion =
                    snapshot.getLong("schemaVersion") ?: return null

                if (schemaVersion != SCHEMA_VERSION) return null

                val ownerUid =
                    snapshot.getString("ownerUid") ?: return null

                val scope =
                    snapshot.getString("scope") ?: return null

                val encryptionKeySource =
                    snapshot.getString("encryptionKeySource")
                        ?: if (scope == PasswordRecordV2.SCOPE_PERSONAL) {
                            KEY_SOURCE_MEMBER_MASTER
                        } else {
                            KEY_SOURCE_LEGACY_MEMBER_MASTER
                        }

                val payloadVersion =
                    snapshot.getLong("payloadVersion") ?: return null

                val algorithm =
                    snapshot.getString("algorithm") ?: return null

                val encryptedPayload =
                    snapshot.getString("encryptedPayload") ?: return null

                val createdAt =
                    snapshot.getTimestamp("createdAt") ?: return null

                val editedAt =
                    snapshot.getTimestamp("editedAt") ?: return null

                if (ownerUid.isBlank()) return null

                if (
                    scope != PasswordRecordV2.SCOPE_PERSONAL &&
                    scope != PasswordRecordV2.SCOPE_SHARED
                ) return null

                if (payloadVersion != PAYLOAD_VERSION) return null
                if (algorithm != ALGORITHM) return null

                if (
                    encryptedPayload.length < 40 ||
                    encryptedPayload.length > 100_000
                ) return null

                if (
                    encryptionKeySource != KEY_SOURCE_MEMBER_MASTER &&
                    encryptionKeySource != KEY_SOURCE_HOUSEHOLD &&
                    encryptionKeySource != KEY_SOURCE_LEGACY_MEMBER_MASTER
                ) return null

                EncryptedPasswordDocumentV2(
                    encryptionKeySource = encryptionKeySource,
                    ownerUid = ownerUid,
                    scope = scope,
                    payloadVersion = payloadVersion,
                    algorithm = algorithm,
                    encryptedPayload = encryptedPayload,
                    createdAt = createdAt.toDate().time,
                    editedAt = editedAt.toDate().time
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}