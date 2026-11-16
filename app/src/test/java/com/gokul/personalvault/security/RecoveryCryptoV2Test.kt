package com.gokul.personalvault.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecoveryCryptoV2Test {

    private val vaultId = "test-vault-v2"
    private val user1Uid = "test-user-1"
    private val user2Uid = "test-user-2"

    @Test
    fun userCanRestoreVaultKeyWithTheirRecoveryPassword() {
        val originalVaultKey = RecoveryCryptoV2.generateVaultKey()
        val password = "UserOne-Recovery-2026!".toCharArray()

        try {
            val envelope = RecoveryCryptoV2.createRecoveryEnvelope(
                vaultKey = originalVaultKey,
                recoveryPassword = password,
                vaultId = vaultId,
                memberUid = user1Uid
            )

            val restoredVaultKey = RecoveryCryptoV2.restoreVaultKey(
                envelope = envelope,
                recoveryPassword = password,
                vaultId = vaultId,
                memberUid = user1Uid
            )

            assertArrayEquals(originalVaultKey, restoredVaultKey)
            restoredVaultKey.fill(0)
        } finally {
            originalVaultKey.fill(0)
            password.fill('\u0000')
        }
    }

    @Test
    fun twoMembersCanHaveDifferentBackupsForTheSameVaultKey() {
        val originalVaultKey = RecoveryCryptoV2.generateVaultKey()
        val user1Password = "UserOne-Recovery-2026!".toCharArray()
        val user2Password = "UserTwo-Recovery-2026!".toCharArray()

        try {
            val user1Envelope = RecoveryCryptoV2.createRecoveryEnvelope(
                originalVaultKey, user1Password, vaultId, user1Uid
            )
            val user2Envelope = RecoveryCryptoV2.createRecoveryEnvelope(
                originalVaultKey, user2Password, vaultId, user2Uid
            )

            // Different salts/nonces and member-bound AAD should produce distinct envelopes.
            assertNotEquals(user1Envelope, user2Envelope)

            val restoredByUser1 = RecoveryCryptoV2.restoreVaultKey(
                user1Envelope, user1Password, vaultId, user1Uid
            )
            val restoredByUser2 = RecoveryCryptoV2.restoreVaultKey(
                user2Envelope, user2Password, vaultId, user2Uid
            )

            assertArrayEquals(originalVaultKey, restoredByUser1)
            assertArrayEquals(originalVaultKey, restoredByUser2)

            restoredByUser1.fill(0)
            restoredByUser2.fill(0)
        } finally {
            originalVaultKey.fill(0)
            user1Password.fill('\u0000')
            user2Password.fill('\u0000')
        }
    }

    @Test
    fun incorrectRecoveryPasswordIsRejected() {
        val vaultKey = RecoveryCryptoV2.generateVaultKey()
        val correctPassword = "Correct-Recovery-2026!".toCharArray()
        val incorrectPassword = "Incorrect-Recovery-2026!".toCharArray()

        try {
            val envelope = RecoveryCryptoV2.createRecoveryEnvelope(
                vaultKey, correctPassword, vaultId, user1Uid
            )

            assertThrows(IllegalArgumentException::class.java) {
                RecoveryCryptoV2.restoreVaultKey(
                    envelope, incorrectPassword, vaultId, user1Uid
                )
            }
        } finally {
            vaultKey.fill(0)
            correctPassword.fill('\u0000')
            incorrectPassword.fill('\u0000')
        }
    }

    @Test
    fun recoveryEnvelopeCannotBeUsedForAnotherMemberOrVault() {
        val vaultKey = RecoveryCryptoV2.generateVaultKey()
        val password = "Member-Recovery-Password-2026!".toCharArray()

        try {
            val envelope = RecoveryCryptoV2.createRecoveryEnvelope(
                vaultKey, password, vaultId, user1Uid
            )

            assertThrows(IllegalArgumentException::class.java) {
                RecoveryCryptoV2.restoreVaultKey(
                    envelope, password, vaultId, user2Uid
                )
            }

            assertThrows(IllegalArgumentException::class.java) {
                RecoveryCryptoV2.restoreVaultKey(
                    envelope, password, "different-vault", user1Uid
                )
            }
        } finally {
            vaultKey.fill(0)
            password.fill('\u0000')
        }
    }

    @Test
    fun recoveryEnvelopeCanBeSerializedAndParsed() {
        val vaultKey = RecoveryCryptoV2.generateVaultKey()
        val password = "Serialize-Recovery-2026!".toCharArray()

        try {
            val original = RecoveryCryptoV2.createRecoveryEnvelope(
                vaultKey, password, vaultId, user1Uid
            )

            val parsed = RecoveryCryptoV2.RecoveryEnvelope.fromMap(original.toMap())

            assertEquals(original, parsed)

            val restored = RecoveryCryptoV2.restoreVaultKey(
                parsed, password, vaultId, user1Uid
            )
            assertArrayEquals(vaultKey, restored)
            restored.fill(0)
        } finally {
            vaultKey.fill(0)
            password.fill('\u0000')
        }
    }
}
