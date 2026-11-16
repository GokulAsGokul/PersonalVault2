package com.gokul.personalvault.data.local

import android.content.Context
import com.gokul.personalvault.security.MasterPasswordCryptoV2

/**
 * Stores only the encrypted master-password envelope in app-private preferences.
 * The plaintext vault key is never persisted by this class.
 */
object VaultKeyLocalStoreV2 {
    private const val PREFS = "pv2_master_envelopes"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun prefix(uid: String, vaultId: String) = "${uid}_${vaultId}_"

    fun hasMasterPassword(context: Context, uid: String, vaultId: String): Boolean {
        val p = prefix(uid, vaultId)
        return prefs(context).contains(p + "ciphertext")
    }

    /**
     * First-time setup: generates a new random vault key and wraps it locally.
     * Use only when creating a brand-new vault key.
     */
    fun create(
        context: Context,
        uid: String,
        vaultId: String,
        password: CharArray
    ): Result<ByteArray> {
        if (hasMasterPassword(context, uid, vaultId)) {
            password.fill('\u0000')
            return Result.failure(
                IllegalStateException("A master password is already configured on this device.")
            )
        }

        val key = MasterPasswordCryptoV2.generateVaultKey()
        return try {
            saveEnvelope(context, uid, vaultId, key, password)
            Result.success(key)
        } catch (error: Exception) {
            key.fill(0)
            Result.failure(error)
        } finally {
            password.fill('\u0000')
        }
    }

    /**
     * Recovery/import path: stores the EXISTING 256-bit vault key under this
     * member's own master password. It deliberately does not generate a key.
     *
     * Existing local envelopes are never silently overwritten.
     */
    fun importExistingKey(
        context: Context,
        uid: String,
        vaultId: String,
        existingVaultKey: ByteArray,
        newMasterPassword: CharArray
    ): Result<Unit> {
        if (existingVaultKey.size != 32) {
            newMasterPassword.fill('\u0000')
            return Result.failure(IllegalArgumentException("The recovered vault key is invalid."))
        }
        if (hasMasterPassword(context, uid, vaultId)) {
            newMasterPassword.fill('\u0000')
            return Result.failure(
                IllegalStateException(
                    "A master password is already configured on this device. Unlock the existing vault instead."
                )
            )
        }

        return try {
            saveEnvelope(context, uid, vaultId, existingVaultKey, newMasterPassword)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            newMasterPassword.fill('\u0000')
        }
    }

    private fun saveEnvelope(
        context: Context,
        uid: String,
        vaultId: String,
        vaultKey: ByteArray,
        password: CharArray
    ) {
        require(uid.isNotBlank() && vaultId.isNotBlank()) {
            "Vault and member identity are required."
        }
        require(vaultKey.size == 32) { "Vault key must be 256 bits." }

        val envelope = MasterPasswordCryptoV2.encrypt(vaultKey, password, vaultId, uid)
        val p = prefix(uid, vaultId)
        val saved = prefs(context).edit()
            .putString(p + "version", envelope.version.toString())
            .putString(p + "salt", envelope.salt)
            .putString(p + "nonce", envelope.nonce)
            .putString(p + "ciphertext", envelope.ciphertext)
            .commit()

        if (!saved) {
            throw IllegalStateException("Unable to save encrypted vault key on this device.")
        }
    }

    fun unlock(
        context: Context,
        uid: String,
        vaultId: String,
        password: CharArray
    ): Result<ByteArray> {
        return try {
            val p = prefix(uid, vaultId)
            val all = prefs(context)
            val fields = mapOf(
                "version" to all.getString(p + "version", null),
                "salt" to all.getString(p + "salt", null),
                "nonce" to all.getString(p + "nonce", null),
                "ciphertext" to all.getString(p + "ciphertext", null)
            ).mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

            val envelope = MasterPasswordCryptoV2.Envelope.from(fields)
            Result.success(MasterPasswordCryptoV2.decrypt(envelope, password, vaultId, uid))
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            password.fill('\u0000')
        }
    }
}
