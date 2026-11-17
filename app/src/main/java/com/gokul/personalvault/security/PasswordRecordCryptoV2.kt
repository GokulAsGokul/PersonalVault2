
package com.gokul.personalvault.security

import com.gokul.personalvault.data.model.PasswordRecordV2
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts individual V2 password records using the shared vault key.
 *
 * Only the encryptedPayload is stored in Firestore.
 * The vault key must be 32 bytes (AES-256).
 */
object PasswordRecordCryptoV2 {

    private const val VERSION = 2
    private const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val MAX_PLAINTEXT_BYTES = 70_000
    private const val MAX_FIELD_BYTES = 32_768
    private const val MAX_ENCODED_LENGTH = 100_000

    private val secureRandom = SecureRandom()

    fun encrypt(
        record: PasswordRecordV2,
        vaultKey: ByteArray,
        vaultId: String
    ): String {
        validateKey(vaultKey)
        validateIdentity(vaultId, record.id, record.ownerUid, record.scope)

        val plaintext = encodeRecord(record)
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) {
            "Password record is too large."
        }

        val nonce = ByteArray(NONCE_BYTES)
        secureRandom.nextBytes(nonce)

        var ciphertext = ByteArray(0)
        var packed = ByteArray(0)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(vaultKey, "AES"),
                GCMParameterSpec(TAG_BITS, nonce)
            )

            cipher.updateAAD(
                associatedData(
                    vaultId,
                    record.id,
                    record.ownerUid,
                    record.scope
                )
            )

            ciphertext = cipher.doFinal(plaintext)

            packed = ByteArray(1 + NONCE_BYTES + ciphertext.size)
            packed[0] = VERSION.toByte()

            System.arraycopy(
                nonce, 0, packed, 1, NONCE_BYTES
            )

            System.arraycopy(
                ciphertext, 0, packed,
                1 + NONCE_BYTES, ciphertext.size
            )

            val encoded = Base64.getEncoder().encodeToString(packed)

            require(encoded.length <= MAX_ENCODED_LENGTH) {
                "Encrypted password record is too large."
            }

            return encoded
        } finally {
            plaintext.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
            packed.fill(0)
        }
    }

    fun decrypt(
        encryptedPayload: String,
        vaultKey: ByteArray,
        vaultId: String,
        recordId: String,
        ownerUid: String,
        scope: String
    ): PasswordRecordV2 {
        validateKey(vaultKey)
        validateIdentity(vaultId, recordId, ownerUid, scope)

        require(encryptedPayload.isNotBlank()) {
            "Encrypted password data is missing."
        }

        require(encryptedPayload.length <= MAX_ENCODED_LENGTH) {
            "Encrypted password data is too large."
        }

        val packed = Base64.getDecoder().decode(encryptedPayload)

        require(packed.size > 1 + NONCE_BYTES + 16) {
            "Encrypted password data is invalid."
        }

        require(packed[0].toInt() == VERSION) {
            "Unsupported password data version."
        }

        val nonce = packed.copyOfRange(1, 1 + NONCE_BYTES)
        val ciphertext = packed.copyOfRange(1 + NONCE_BYTES, packed.size)

        var plaintext = ByteArray(0)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(vaultKey, "AES"),
                GCMParameterSpec(TAG_BITS, nonce)
            )

            cipher.updateAAD(
                associatedData(vaultId, recordId, ownerUid, scope)
            )

            plaintext = cipher.doFinal(ciphertext)

            val record = decodeRecord(plaintext)

            require(record.id == recordId) {
                "Password record identity mismatch."
            }

            require(record.ownerUid == ownerUid) {
                "Password record owner mismatch."
            }

            require(record.scope == scope) {
                "Password record scope mismatch."
            }

            return record
        } finally {
            packed.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
            plaintext.fill(0)
        }
    }

    private fun encodeRecord(record: PasswordRecordV2): ByteArray {
        val output = ByteArrayOutputStream()

        DataOutputStream(output).use { data ->
            data.writeStringValue(record.id)
            data.writeStringValue(record.scope)
            data.writeStringValue(record.ownerUid)
            data.writeStringValue(record.websiteApp)
            data.writeStringValue(record.username)
            data.writeStringValue(record.password)
            data.writeStringValue(record.category)
            data.writeStringValue(record.notes)
            data.writeLong(record.createdAt)
            data.writeLong(record.editedAt)
        }

        return output.toByteArray()
    }

    private fun decodeRecord(bytes: ByteArray): PasswordRecordV2 {
        require(bytes.size <= MAX_PLAINTEXT_BYTES) {
            "Password record is too large."
        }

        DataInputStream(ByteArrayInputStream(bytes)).use { data ->
            val record = PasswordRecordV2(
                id = data.readStringValue(),
                scope = data.readStringValue(),
                ownerUid = data.readStringValue(),
                websiteApp = data.readStringValue(),
                username = data.readStringValue(),
                password = data.readStringValue(),
                category = data.readStringValue(),
                notes = data.readStringValue(),
                createdAt = data.readLong(),
                editedAt = data.readLong()
            )

            require(data.available() == 0) {
                "Password record contains invalid trailing data."
            }

            return record
        }
    }

    private fun DataOutputStream.writeStringValue(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)

        require(bytes.size <= MAX_FIELD_BYTES) {
            "A password field is too large."
        }

        writeInt(bytes.size)
        write(bytes)
        bytes.fill(0)
    }

    private fun DataInputStream.readStringValue(): String {
        val length = readInt()

        require(length in 0..MAX_FIELD_BYTES) {
            "Password field length is invalid."
        }

        val bytes = ByteArray(length)

        try {
            readFully(bytes)
            return String(bytes, StandardCharsets.UTF_8)
        } finally {
            bytes.fill(0)
        }
    }

    private fun associatedData(
        vaultId: String,
        recordId: String,
        ownerUid: String,
        scope: String
    ): ByteArray {
        val value =
            "PersonalVault2|password-record|v2|" +
                    "$vaultId|$recordId|$ownerUid|$scope"

        return value.toByteArray(StandardCharsets.UTF_8)
    }

    private fun validateKey(key: ByteArray) {
        require(key.size == KEY_BYTES) {
            "Vault key must be exactly 32 bytes."
        }
    }

    private fun validateIdentity(
        vaultId: String,
        recordId: String,
        ownerUid: String,
        scope: String
    ) {
        require(vaultId.isNotBlank()) { "Vault ID is required." }
        require(recordId.isNotBlank()) { "Password record ID is required." }
        require(ownerUid.isNotBlank()) { "Password owner is required." }

        require(
            scope == PasswordRecordV2.SCOPE_PERSONAL ||
                    scope == PasswordRecordV2.SCOPE_SHARED
        ) {
            "Invalid password scope."
        }
    }
}
