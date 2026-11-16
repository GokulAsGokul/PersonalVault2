package com.gokul.personalvault.security

import java.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Recovery-key cryptography for Personal Vault 2.0.
 *
 * This class only wraps/unwraps the random shared vault key. It does not access
 * Firebase, Android storage, or the UI.
 *
 * Online and offline recovery must store the exact same RecoveryEnvelope format.
 * Never store the recovery password or the unwrapped vault key in Firebase.
 */
object RecoveryCryptoV2 {
    private const val FORMAT_VERSION = 2
    private const val KDF_NAME = "Argon2id"
    private const val CIPHER_NAME = "AES-256-GCM"

    private const val VAULT_KEY_BYTES = 32
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val GCM_TAG_BITS = 128

    // Fixed parameters are deliberately not read from an untrusted envelope.
    // 64 MiB, 3 passes, 1 lane; benchmark on the minimum supported device.
    private const val ARGON_MEMORY_KIB = 64 * 1024
    private const val ARGON_ITERATIONS = 3
    private const val ARGON_PARALLELISM = 1
    private const val ARGON_OUTPUT_BYTES = 32

    private val secureRandom = SecureRandom()

    data class RecoveryEnvelope(
        val version: Int,
        val kdf: String,
        val cipher: String,
        val saltBase64: String,
        val nonceBase64: String,
        val ciphertextBase64: String
    ) {
        fun toMap(): Map<String, Any> = mapOf(
            "version" to version,
            "kdf" to kdf,
            "cipher" to cipher,
            "salt" to saltBase64,
            "nonce" to nonceBase64,
            "ciphertext" to ciphertextBase64
        )

        companion object {
            fun fromMap(map: Map<String, Any?>): RecoveryEnvelope {
                return RecoveryEnvelope(
                    version = (map["version"] as? Number)?.toInt()
                        ?: error("Recovery format version is missing."),
                    kdf = map["kdf"] as? String
                        ?: error("Recovery KDF is missing."),
                    cipher = map["cipher"] as? String
                        ?: error("Recovery cipher is missing."),
                    saltBase64 = map["salt"] as? String
                        ?: error("Recovery salt is missing."),
                    nonceBase64 = map["nonce"] as? String
                        ?: error("Recovery nonce is missing."),
                    ciphertextBase64 = map["ciphertext"] as? String
                        ?: error("Recovery ciphertext is missing.")
                )
            }
        }
    }

    /** Creates a cryptographically random 256-bit shared vault key. */
    fun generateVaultKey(): ByteArray =
        ByteArray(VAULT_KEY_BYTES).also(secureRandom::nextBytes)

    /**
     * Encrypts the shared vault key using a member-specific recovery password.
     *
     * vaultId and memberUid are authenticated as AES-GCM AAD. This prevents an
     * envelope from being silently moved to a different vault or member record.
     *
     * The caller retains responsibility for clearing the supplied password
     * CharArray after this call.
     */
    fun createRecoveryEnvelope(
        vaultKey: ByteArray,
        recoveryPassword: CharArray,
        vaultId: String,
        memberUid: String
    ): RecoveryEnvelope {
        require(vaultKey.size == VAULT_KEY_BYTES) { "Vault key must be 256 bits." }
        validateIdentity(vaultId, memberUid)
        validatePassword(recoveryPassword)

        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
        val passwordBytes = passwordToUtf8(recoveryPassword)
        val wrappingKey = deriveArgon2id(passwordBytes, salt)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(wrappingKey, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, nonce)
            )
            cipher.updateAAD(aad(vaultId, memberUid))
            val ciphertext = cipher.doFinal(vaultKey)

            return RecoveryEnvelope(
                version = FORMAT_VERSION,
                kdf = KDF_NAME,
                cipher = CIPHER_NAME,
                saltBase64 = Base64.getEncoder().encodeToString(salt),
                nonceBase64 = Base64.getEncoder().encodeToString(nonce),
                ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext)
            )
        } finally {
            passwordBytes.fill(0)
            wrappingKey.fill(0)
            salt.fill(0)
            nonce.fill(0)
        }
    }

    /**
     * Decrypts a member's recovery envelope and returns a new 32-byte vault key.
     * The caller must clear the returned ByteArray after securely importing it.
     */
    fun restoreVaultKey(
        envelope: RecoveryEnvelope,
        recoveryPassword: CharArray,
        vaultId: String,
        memberUid: String
    ): ByteArray {
        validateIdentity(vaultId, memberUid)
        validatePassword(recoveryPassword)

        require(envelope.version == FORMAT_VERSION) {
            "Unsupported recovery format version."
        }
        require(envelope.kdf == KDF_NAME && envelope.cipher == CIPHER_NAME) {
            "Unsupported recovery cryptography format."
        }

        val salt = Base64.getDecoder().decode(envelope.saltBase64)
        val nonce = Base64.getDecoder().decode(envelope.nonceBase64)
        val ciphertext = Base64.getDecoder().decode(envelope.ciphertextBase64)

        require(salt.size == SALT_BYTES) { "Invalid recovery salt." }
        require(nonce.size == NONCE_BYTES) { "Invalid recovery nonce." }
        require(ciphertext.size == VAULT_KEY_BYTES + (GCM_TAG_BITS / 8)) {
            "Invalid recovery ciphertext length."
        }

        val passwordBytes = passwordToUtf8(recoveryPassword)
        val wrappingKey = deriveArgon2id(passwordBytes, salt)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(wrappingKey, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, nonce)
            )
            cipher.updateAAD(aad(vaultId, memberUid))

            val vaultKey = try {
                cipher.doFinal(ciphertext)
            } catch (error: AEADBadTagException) {
                throw IllegalArgumentException(
                    "Recovery password is incorrect or this backup belongs to another account/vault.",
                    error
                )
            }

            if (vaultKey.size != VAULT_KEY_BYTES) {
                vaultKey.fill(0)
                throw IllegalStateException("Recovered vault key has an invalid length.")
            }
            return vaultKey
        } finally {
            passwordBytes.fill(0)
            wrappingKey.fill(0)
            salt.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }

    private fun deriveArgon2id(passwordBytes: ByteArray, salt: ByteArray): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(ARGON_MEMORY_KIB)
            .withIterations(ARGON_ITERATIONS)
            .withParallelism(ARGON_PARALLELISM)
            .build()

        return ByteArray(ARGON_OUTPUT_BYTES).also { output ->
            Argon2BytesGenerator().apply { init(parameters) }
                .generateBytes(passwordBytes, output)
        }
    }

    private fun passwordToUtf8(password: CharArray): ByteArray {
        val encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(password))
        return ByteArray(encoded.remaining()).also(encoded::get)
    }

    private fun aad(vaultId: String, memberUid: String): ByteArray =
        "PersonalVault2|recovery|v$FORMAT_VERSION|$vaultId|$memberUid"
            .toByteArray(StandardCharsets.UTF_8)

    private fun validateIdentity(vaultId: String, memberUid: String) {
        require(vaultId.isNotBlank()) { "Vault ID is required." }
        require(memberUid.isNotBlank()) { "Member account ID is required." }
    }

    private fun validatePassword(password: CharArray) {
        require(password.size >= 12) {
            "Recovery password must contain at least 12 characters."
        }
    }
}
