package com.gokul.personalvault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * Household-level encryption helper.
 *
 * - Each app installation has an RSA key pair in Android Keystore, keyed by Firebase UID + device ID.
 * - The household AES-256 key is wrapped separately for each member's public key.
 * - The household AES key is kept in memory only after unwrap/create.
 * - Password record payloads are encrypted with AES-256-GCM and householdId as AAD.
 *
 * This helper does not perform Firestore reads/writes. The caller must store each
 * member's public key and wrappedKey envelope in Firestore, under authenticated rules.
 */
object HouseholdCrypto {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val RSA_ALIAS_PREFIX = "personal_vault_household_rsa_"
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val AES_KEY_SIZE_BITS = 256
    private const val GCM_NONCE_SIZE_BYTES = 12
    private const val GCM_TAG_SIZE_BITS = 128

    @Volatile
    private var sessionHouseholdId: String? = null

    @Volatile
    private var sessionHouseholdKey: ByteArray? = null

    data class WrappedHouseholdKey(
        val version: Int = 1,
        val algorithm: String = "RSA-OAEP-256",
        val wrappedKeyBase64: String
    )

    data class EncryptedPayload(
        val version: Int = 1,
        val algorithm: String = "AES-256-GCM",
        val nonceBase64: String,
        val ciphertextBase64: String
    ) {
        fun toMap(): Map<String, Any> = mapOf(
            "version" to version,
            "algorithm" to algorithm,
            "nonce" to nonceBase64,
            "ciphertext" to ciphertextBase64
        )

        companion object {
            fun fromMap(map: Map<String, Any?>): EncryptedPayload {
                val version = (map["version"] as? Number)?.toInt()
                    ?: error("Encrypted payload version is missing.")
                require(version == 1) { "Unsupported encrypted payload version: $version" }
                return EncryptedPayload(
                    version = version,
                    algorithm = map["algorithm"] as? String ?: "AES-256-GCM",
                    nonceBase64 = map["nonce"] as? String
                        ?: error("Encrypted payload nonce is missing."),
                    ciphertextBase64 = map["ciphertext"] as? String
                        ?: error("Encrypted payload ciphertext is missing.")
                )
            }
        }
    }

    /** Creates an RSA key pair once per Firebase UID + installation device ID. */
    @Synchronized
    fun ensureDeviceKeyPair(firebaseUid: String, deviceId: String): KeyPair {
        require(firebaseUid.isNotBlank()) { "Firebase UID is required." }
        require(deviceId.isNotBlank()) { "Device ID is required." }
        val alias = aliasFor(firebaseUid, deviceId)
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (keyStore.containsAlias(alias)) {
            val privateKey = keyStore.getKey(alias, null) as java.security.PrivateKey
            val publicKey = keyStore.getCertificate(alias).publicKey
            return KeyPair(publicKey, privateKey)
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(3072)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setUserAuthenticationRequired(false)
            .build()

        generator.initialize(spec)
        return generator.generateKeyPair()
    }

    /** Public key string suitable for publishing in this installation's device document. */
    fun exportPublicKey(firebaseUid: String, deviceId: String): String {
        val publicKey = ensureDeviceKeyPair(firebaseUid, deviceId).public
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /** Owner-only operation: generate a fresh random AES-256 household key. */
    @Synchronized
    fun createHouseholdKey(householdId: String) {
        require(householdId.isNotBlank()) { "Household ID is required." }
        val generator = KeyGenerator.getInstance("AES")
        generator.init(AES_KEY_SIZE_BITS)
        val key = generator.generateKey().encoded
        setSessionKey(householdId, key)
    }

    /** Wrap the current household AES key for a member's published RSA public key. */
    fun wrapHouseholdKeyForMember(
        householdId: String,
        memberPublicKeyBase64: String
    ): WrappedHouseholdKey {
        val keyBytes = requireSessionKey(householdId)
        val publicBytes = Base64.decode(memberPublicKeyBase64, Base64.DEFAULT)
        val publicKey = java.security.KeyFactory.getInstance("RSA")
            .generatePublic(java.security.spec.X509EncodedKeySpec(publicBytes))

        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParameters())
        val wrapped = cipher.doFinal(keyBytes)
        return WrappedHouseholdKey(
            wrappedKeyBase64 = Base64.encodeToString(wrapped, Base64.NO_WRAP)
        )
    }

    /** Unwraps an envelope using the private key belonging to this exact installation. */
    @Synchronized
    fun unwrapHouseholdKey(
        firebaseUid: String,
        deviceId: String,
        householdId: String,
        wrappedKeyBase64: String
    ) {
        require(firebaseUid.isNotBlank()) { "Firebase UID is required." }
        require(deviceId.isNotBlank()) { "Device ID is required." }
        require(householdId.isNotBlank()) { "Household ID is required." }

        val keyPair = ensureDeviceKeyPair(firebaseUid, deviceId)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private, oaepParameters())
        val keyBytes = cipher.doFinal(Base64.decode(wrappedKeyBase64, Base64.DEFAULT))
        require(keyBytes.size == 32) { "Invalid household key length." }
        setSessionKey(householdId, keyBytes)
    }

    /** Encrypt a JSON/string record payload. Returns fields ready for Firestore. */
    fun encrypt(householdId: String, plaintext: String): EncryptedPayload {
        require(plaintext.isNotEmpty()) { "Cannot encrypt an empty payload." }
        val key = SecretKeySpec(requireSessionKey(householdId), "AES")
        val nonce = ByteArray(GCM_NONCE_SIZE_BYTES).also {
            java.security.SecureRandom().nextBytes(it)
        }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE_BITS, nonce))
        cipher.updateAAD(householdId.toByteArray(StandardCharsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        return EncryptedPayload(
            nonceBase64 = Base64.encodeToString(nonce, Base64.NO_WRAP),
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    /** Decrypt an encrypted record only after this account has unwrapped the household key. */
    fun decrypt(householdId: String, payload: EncryptedPayload): String {
        require(payload.version == 1) { "Unsupported encrypted payload version." }
        require(payload.algorithm == "AES-256-GCM") { "Unsupported encryption algorithm." }

        val nonce = Base64.decode(payload.nonceBase64, Base64.DEFAULT)
        require(nonce.size == GCM_NONCE_SIZE_BYTES) { "Invalid AES-GCM nonce." }

        val key = SecretKeySpec(requireSessionKey(householdId), "AES")
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE_BITS, nonce))
        cipher.updateAAD(householdId.toByteArray(StandardCharsets.UTF_8))
        val plaintext = cipher.doFinal(
            Base64.decode(payload.ciphertextBase64, Base64.DEFAULT)
        )
        return String(plaintext, StandardCharsets.UTF_8)
    }

    @Synchronized
    fun clearSession() {
        sessionHouseholdKey?.fill(0)
        sessionHouseholdKey = null
        sessionHouseholdId = null
    }

    fun isUnlocked(householdId: String): Boolean =
        sessionHouseholdId == householdId && sessionHouseholdKey?.size == 32

    /**
     * Returns a defensive copy of the unlocked household AES-256 key.
     * The caller is responsible for clearing the returned bytes.
     */
    @Synchronized
    fun copyHouseholdKey(householdId: String): ByteArray {
        return requireSessionKey(householdId)
    }

    private fun requireSessionKey(householdId: String): ByteArray {
        require(sessionHouseholdId == householdId) {
            "Household vault is locked or a different household is active."
        }
        val key = sessionHouseholdKey
            ?: error("Household vault is locked. Unlock it before continuing.")
        require(key.size == 32) { "Invalid household key." }
        return key.copyOf()
    }

    @Synchronized
    private fun setSessionKey(householdId: String, keyBytes: ByteArray) {
        require(keyBytes.size == 32) { "Household key must be 256 bits." }
        sessionHouseholdKey?.fill(0)
        sessionHouseholdId = householdId
        sessionHouseholdKey = keyBytes.copyOf()
    }

    /** Hash both identifiers; never put account or installation IDs in alias names. */
    private fun aliasFor(firebaseUid: String, deviceId: String): String {
        val identity = "$firebaseUid:$deviceId"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(StandardCharsets.UTF_8))
        val suffix = Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return RSA_ALIAS_PREFIX + suffix
    }

    private fun oaepParameters() = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA1,
        PSource.PSpecified.DEFAULT
    )
}
