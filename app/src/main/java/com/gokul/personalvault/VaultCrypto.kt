package com.gokul.personalvault

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Phase 1 local cryptography:
 * - A random 256-bit data-encryption key (DEK) encrypts vault values.
 * - The user's Master Password derives a wrapping key using PBKDF2-HMAC-SHA256.
 * - The wrapping key encrypts the DEK with AES-256-GCM.
 *
 * Only the wrapped DEK is persisted. The unwrapped DEK is held in memory
 * while the vault session is unlocked.
 */
object VaultCrypto {
    private const val PREFS = "personal_vault_local_security"
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val NONCE_BYTES = 12
    private const val SALT_BYTES = 16
    private const val DEK_BYTES = 32
    private const val FORMAT_VERSION = 1

    @Volatile
    private var sessionKey: ByteArray? = null

    fun isSetupComplete(context: android.content.Context, userUid: String): Boolean {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        return p.contains("salt_$userUid") &&
            p.contains("wrapped_key_$userUid") &&
            p.contains("wrap_nonce_$userUid")
    }

    @Synchronized
    fun createVault(context: android.content.Context, userUid: String, masterPassword: CharArray) {
        require(userUid.isNotBlank()) { "Missing user account." }
        require(masterPassword.size >= 10) { "Use at least 10 characters." }
        check(!isSetupComplete(context, userUid)) { "Vault security is already configured." }

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val dataKey = ByteArray(DEK_BYTES).also { SecureRandom().nextBytes(it) }
        val wrappingBytes = deriveWrappingKey(masterPassword, salt)
        val nonce = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(wrappingBytes, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.updateAAD(userUid.toByteArray(StandardCharsets.UTF_8))
            val wrappedKey = cipher.doFinal(dataKey)

            val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            val saved = p.edit()
                .putInt("format_$userUid", FORMAT_VERSION)
                .putString("salt_$userUid", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("wrapped_key_$userUid", Base64.encodeToString(wrappedKey, Base64.NO_WRAP))
                .putString("wrap_nonce_$userUid", Base64.encodeToString(nonce, Base64.NO_WRAP))
                .commit()

            check(saved) { "Could not save vault security settings." }
            setSessionKey(dataKey)
        } finally {
            wrappingBytes.fill(0)
            salt.fill(0)
            nonce.fill(0)
            dataKey.fill(0)
        }
    }

    @Synchronized
    fun unlockVault(context: android.content.Context, userUid: String, masterPassword: CharArray) {
        require(userUid.isNotBlank()) { "Missing user account." }
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val version = p.getInt("format_$userUid", -1)
        check(version == FORMAT_VERSION) { "Vault security setup is missing or unsupported." }

        val saltText = p.getString("salt_$userUid", null)
            ?: throw IllegalStateException("Vault security setup is incomplete.")
        val wrappedText = p.getString("wrapped_key_$userUid", null)
            ?: throw IllegalStateException("Vault security setup is incomplete.")
        val nonceText = p.getString("wrap_nonce_$userUid", null)
            ?: throw IllegalStateException("Vault security setup is incomplete.")

        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        val wrappedKey = Base64.decode(wrappedText, Base64.NO_WRAP)
        val nonce = Base64.decode(nonceText, Base64.NO_WRAP)
        val wrappingBytes = deriveWrappingKey(masterPassword, salt)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(wrappingBytes, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.updateAAD(userUid.toByteArray(StandardCharsets.UTF_8))
            val dataKey = cipher.doFinal(wrappedKey)
            if (dataKey.size != DEK_BYTES) {
                dataKey.fill(0)
                throw IllegalStateException("Invalid vault key.")
            }
            try {
                setSessionKey(dataKey)
            } finally {
                dataKey.fill(0)
            }
        } catch (e: AEADBadTagException) {
            throw IllegalArgumentException("Incorrect Master Password.", e)
        } finally {
            salt.fill(0)
            wrappedKey.fill(0)
            nonce.fill(0)
            wrappingBytes.fill(0)
        }
    }

    @Synchronized
    fun encrypt(plainText: String, userUid: String): String {
        val key = requireSessionKey()
        val nonce = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.updateAAD(userUid.toByteArray(StandardCharsets.UTF_8))
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return "1." +
            Base64.encodeToString(nonce, Base64.NO_WRAP) + "." +
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
    }

    @Synchronized
    fun decrypt(payload: String, userUid: String): String {
        val key = requireSessionKey()
        val parts = payload.split(".")
        require(parts.size == 3 && parts[0] == "1") { "Unsupported encrypted data format." }
        val nonce = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[2], Base64.NO_WRAP)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.updateAAD(userUid.toByteArray(StandardCharsets.UTF_8))
            return String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        } finally {
            nonce.fill(0)
            cipherText.fill(0)
        }
    }

    @Synchronized
    fun clearSession() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    private fun setSessionKey(key: ByteArray) {
        sessionKey?.fill(0)
        sessionKey = key.copyOf()
    }

    private fun requireSessionKey(): ByteArray =
        sessionKey ?: throw IllegalStateException("Vault is locked. Unlock it before using encrypted data.")

    private fun deriveWrappingKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
