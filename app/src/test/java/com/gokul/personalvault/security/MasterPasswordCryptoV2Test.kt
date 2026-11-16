package com.gokul.personalvault.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test

class MasterPasswordCryptoV2Test {
    private val password = "StrongMaster#2026".toCharArray()

    @Test fun correctPasswordRestoresSameVaultKey() {
        val key = MasterPasswordCryptoV2.generateVaultKey()
        val envelope = MasterPasswordCryptoV2.encrypt(key, password.copyOf(), "vault-1", "member-1")
        val restored = MasterPasswordCryptoV2.decrypt(envelope, password.copyOf(), "vault-1", "member-1")
        assertArrayEquals(key, restored)
        key.fill(0); restored.fill(0)
    }

    @Test fun wrongPasswordIsRejected() {
        val key = MasterPasswordCryptoV2.generateVaultKey()
        val envelope = MasterPasswordCryptoV2.encrypt(key, password.copyOf(), "vault-1", "member-1")
        try {
            MasterPasswordCryptoV2.decrypt(envelope, "WrongPassword#1".toCharArray(), "vault-1", "member-1")
            fail("Expected incorrect password to be rejected")
        } catch (_: IllegalArgumentException) { }
        key.fill(0)
    }

    @Test fun envelopeCannotBeMovedToAnotherMember() {
        val key = MasterPasswordCryptoV2.generateVaultKey()
        val envelope = MasterPasswordCryptoV2.encrypt(key, password.copyOf(), "vault-1", "member-1")
        try {
            MasterPasswordCryptoV2.decrypt(envelope, password.copyOf(), "vault-1", "member-2")
            fail("Expected member identity mismatch to be rejected")
        } catch (_: IllegalArgumentException) { }
        key.fill(0)
    }

    @Test fun generatedKeysAreRandom() {
        val first = MasterPasswordCryptoV2.generateVaultKey()
        val second = MasterPasswordCryptoV2.generateVaultKey()
        assertNotEquals(first.toList(), second.toList())
        first.fill(0); second.fill(0)
    }
}
