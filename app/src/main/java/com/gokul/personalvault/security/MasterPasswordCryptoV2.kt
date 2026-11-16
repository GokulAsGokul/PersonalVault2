package com.gokul.personalvault.security

import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/** Password-based encryption for a member's local copy of the shared V2 vault key. */
object MasterPasswordCryptoV2 {
    private const val VERSION = 2
    private const val KEY_BYTES = 32
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val MEMORY_KIB = 64 * 1024
    private const val ITERATIONS = 3
    private const val PARALLELISM = 1
    private val random = SecureRandom()

    data class Envelope(
        val version: Int,
        val salt: String,
        val nonce: String,
        val ciphertext: String
    ) {
        fun toJsonFields(): Map<String, String> = mapOf(
            "version" to version.toString(), "salt" to salt,
            "nonce" to nonce, "ciphertext" to ciphertext
        )

        companion object {
            fun from(fields: Map<String, String>): Envelope = Envelope(
                version = fields["version"]?.toIntOrNull() ?: error("Master-password data is incomplete."),
                salt = fields["salt"] ?: error("Master-password salt is missing."),
                nonce = fields["nonce"] ?: error("Master-password nonce is missing."),
                ciphertext = fields["ciphertext"] ?: error("Master-password data is incomplete.")
            )
        }
    }

    fun generateVaultKey(): ByteArray = ByteArray(KEY_BYTES).also(random::nextBytes)

    fun encrypt(vaultKey: ByteArray, password: CharArray, vaultId: String, memberUid: String): Envelope {
        require(vaultKey.size == KEY_BYTES) { "Vault key must be 256 bits." }
        validateIdentity(vaultId, memberUid)
        require(password.size >= 12) { "Master password must contain at least 12 characters." }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val passwordBytes = passwordBytes(password)
        val wrappingKey = derive(passwordBytes, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(wrappingKey, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad(vaultId, memberUid))
            return Envelope(VERSION, Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(nonce),
                Base64.getEncoder().encodeToString(cipher.doFinal(vaultKey)))
        } finally {
            passwordBytes.fill(0); wrappingKey.fill(0); salt.fill(0); nonce.fill(0)
        }
    }

    fun decrypt(envelope: Envelope, password: CharArray, vaultId: String, memberUid: String): ByteArray {
        validateIdentity(vaultId, memberUid)
        require(envelope.version == VERSION) { "Unsupported master-password data version." }
        val salt = Base64.getDecoder().decode(envelope.salt)
        val nonce = Base64.getDecoder().decode(envelope.nonce)
        val ciphertext = Base64.getDecoder().decode(envelope.ciphertext)
        require(salt.size == SALT_BYTES && nonce.size == NONCE_BYTES && ciphertext.size == KEY_BYTES + 16) {
            "Master-password data is invalid."
        }
        val passwordBytes = passwordBytes(password)
        val wrappingKey = derive(passwordBytes, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(wrappingKey, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad(vaultId, memberUid))
            return try { cipher.doFinal(ciphertext) } catch (e: AEADBadTagException) {
                throw IllegalArgumentException("Incorrect master password.", e)
            }
        } finally {
            passwordBytes.fill(0); wrappingKey.fill(0); salt.fill(0); nonce.fill(0); ciphertext.fill(0)
        }
    }

    private fun derive(password: ByteArray, salt: ByteArray): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt).withMemoryAsKB(MEMORY_KIB)
            .withIterations(ITERATIONS).withParallelism(PARALLELISM).build()
        return ByteArray(KEY_BYTES).also { Argon2BytesGenerator().apply { init(params) }.generateBytes(password, it) }
    }

    private fun passwordBytes(password: CharArray): ByteArray {
        val buffer = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(password))
        return ByteArray(buffer.remaining()).also(buffer::get)
    }

    private fun aad(vaultId: String, uid: String) = "PersonalVault2|master|v$VERSION|$vaultId|$uid".toByteArray(StandardCharsets.UTF_8)
    private fun validateIdentity(vaultId: String, uid: String) {
        require(vaultId.isNotBlank() && uid.isNotBlank()) { "Vault and member identity are required." }
    }
}
