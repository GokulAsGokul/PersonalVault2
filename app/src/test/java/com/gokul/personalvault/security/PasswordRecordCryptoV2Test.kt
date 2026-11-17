
package com.gokul.personalvault.security

import com.gokul.personalvault.data.model.PasswordRecordV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.GeneralSecurityException
import java.util.Base64

class PasswordRecordCryptoV2Test {

    private val vaultKey = ByteArray(32) { it.toByte() }
    private val vaultId = "test-vault-123"

    private fun sampleRecord() = PasswordRecordV2(
        id = "password-record-001",
        scope = PasswordRecordV2.SCOPE_SHARED,
        ownerUid = "wife-member-uid",
        websiteApp = "Example Banking",
        username = "test@example.com",
        password = "MyStrongPassword@123",
        category = "Banking",
        notes = "Test secure note",
        createdAt = 1_800_000_000_000L,
        editedAt = 1_800_000_000_000L
    )

    @Test
    fun encryptAndDecryptReturnsOriginalRecord() {
        val original = sampleRecord()

        val encrypted = PasswordRecordCryptoV2.encrypt(
            record = original,
            vaultKey = vaultKey,
            vaultId = vaultId
        )

        val decrypted = PasswordRecordCryptoV2.decrypt(
            encryptedPayload = encrypted,
            vaultKey = vaultKey,
            vaultId = vaultId,
            recordId = original.id,
            ownerUid = original.ownerUid,
            scope = original.scope
        )

        assertEquals(original, decrypted)
    }

    @Test
    fun sharedRecordCanBeDecryptedWithoutMemberUid() {
        val original = sampleRecord()

        val encrypted = PasswordRecordCryptoV2.encrypt(
            original,
            vaultKey,
            vaultId
        )

        // The decrypt API deliberately does not accept a member UID.
        // Another approved member with the same vault key can decrypt it.
        val decrypted = PasswordRecordCryptoV2.decrypt(
            encrypted,
            vaultKey,
            vaultId,
            original.id,
            original.ownerUid,
            original.scope
        )

        assertEquals(original.password, decrypted.password)
        assertEquals(original.username, decrypted.username)
    }

    @Test
    fun modifiedCiphertextIsRejected() {
        val original = sampleRecord()

        val encrypted = PasswordRecordCryptoV2.encrypt(
            original,
            vaultKey,
            vaultId
        )

        val bytes = Base64.getDecoder().decode(encrypted)
        bytes[bytes.lastIndex] =
            (bytes[bytes.lastIndex].toInt() xor 1).toByte()

        val tampered = Base64.getEncoder().encodeToString(bytes)

        assertThrows(GeneralSecurityException::class.java) {
            PasswordRecordCryptoV2.decrypt(
                tampered,
                vaultKey,
                vaultId,
                original.id,
                original.ownerUid,
                original.scope
            )
        }
    }

    @Test
    fun wrongVaultIdIsRejected() {
        val original = sampleRecord()

        val encrypted = PasswordRecordCryptoV2.encrypt(
            original,
            vaultKey,
            vaultId
        )

        assertThrows(GeneralSecurityException::class.java) {
            PasswordRecordCryptoV2.decrypt(
                encrypted,
                vaultKey,
                "another-vault",
                original.id,
                original.ownerUid,
                original.scope
            )
        }
    }

    @Test
    fun wrongRecordIdIsRejected() {
        val original = sampleRecord()

        val encrypted = PasswordRecordCryptoV2.encrypt(
            original,
            vaultKey,
            vaultId
        )

        assertThrows(GeneralSecurityException::class.java) {
            PasswordRecordCryptoV2.decrypt(
                encrypted,
                vaultKey,
                vaultId,
                "different-record",
                original.ownerUid,
                original.scope
            )
        }
    }

    @Test
    fun wrongScopeIsRejected() {
        val original = sampleRecord()

        val encrypted = PasswordRecordCryptoV2.encrypt(
            original,
            vaultKey,
            vaultId
        )

        assertThrows(GeneralSecurityException::class.java) {
            PasswordRecordCryptoV2.decrypt(
                encrypted,
                vaultKey,
                vaultId,
                original.id,
                original.ownerUid,
                PasswordRecordV2.SCOPE_PERSONAL
            )
        }
    }

    @Test
    fun invalidVaultKeyLengthIsRejected() {
        val original = sampleRecord()

        assertThrows(IllegalArgumentException::class.java) {
            PasswordRecordCryptoV2.encrypt(
                original,
                ByteArray(16),
                vaultId
            )
        }
    }
}