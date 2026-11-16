package com.gokul.personalvault.data.local

import android.content.Context
import com.gokul.personalvault.data.model.PasswordRecordV2
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted, device-local V2 password repository for the first Password Manager milestone.
 *
 * All credential fields are inside one AES-256-GCM encrypted payload. The vault key is
 * obtained only from the active V2 in-memory session and is never persisted by this class.
 *
 * This milestone is intentionally local-only. It does not claim to sync Shared records
 * to another member/device. Cloud sync and server-side ownership rules are a later milestone.
 */
object PasswordRepositoryV2 {
    private const val PREFS = "pv2_password_records_encrypted"
    private const val VERSION = 1
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    private fun storageKey(uid: String, vaultId: String) = "records_${uid}_$vaultId"
    private fun aad(uid: String, vaultId: String) =
        "PersonalVault2|password-records|v$VERSION|$vaultId|$uid".toByteArray(StandardCharsets.UTF_8)

    fun load(context: Context, uid: String, vaultId: String): Result<List<PasswordRecordV2>> =
        runCatching {
            val key = requireSessionKey(vaultId, uid)
            try {
                val encoded = context.applicationContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(storageKey(uid, vaultId), null) ?: return@runCatching emptyList()
                decodeRecords(decrypt(encoded, key, aad(uid, vaultId)))
            } finally {
                key.fill(0)
            }
        }

    fun save(
        context: Context,
        uid: String,
        vaultId: String,
        records: List<PasswordRecordV2>
    ): Result<Unit> = runCatching {
        val key = requireSessionKey(vaultId, uid)
        try {
            val plain = encodeRecords(records)
            val encrypted = encrypt(plain, key, aad(uid, vaultId))
            val saved = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(storageKey(uid, vaultId), encrypted)
                .commit()
            check(saved) { "Unable to save encrypted password records on this device." }
        } finally {
            key.fill(0)
        }
    }

    fun newRecord(
        uid: String,
        scope: String,
        websiteApp: String,
        username: String,
        password: String,
        category: String,
        notes: String,
        existing: PasswordRecordV2? = null
    ): PasswordRecordV2 {
        val now = System.currentTimeMillis()
        return PasswordRecordV2(
            id = existing?.id ?: UUID.randomUUID().toString(),
            scope = scope,
            ownerUid = existing?.ownerUid ?: uid,
            websiteApp = websiteApp.trim(),
            username = username.trim(),
            password = password,
            category = category,
            notes = notes.trim(),
            createdAt = existing?.createdAt ?: now,
            editedAt = now
        )
    }

    private fun requireSessionKey(vaultId: String, uid: String): ByteArray {
        return VaultKeySessionV2.copyKey(vaultId, uid)
            ?: throw IllegalStateException("Unlock your V2 vault before accessing passwords.")
    }

    private fun encrypt(plain: ByteArray, key: ByteArray, associatedData: ByteArray): String {
        val nonce = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(associatedData)
            val ciphertext = cipher.doFinal(plain)
            Base64.getEncoder().encodeToString(byteArrayOf(VERSION.toByte()) + nonce + ciphertext)
        } finally {
            nonce.fill(0)
            plain.fill(0)
        }
    }

    private fun decrypt(encoded: String, key: ByteArray, associatedData: ByteArray): ByteArray {
        val packed = Base64.getDecoder().decode(encoded)
        require(packed.size > 1 + NONCE_BYTES + 16) { "Encrypted password data is invalid." }
        require(packed[0].toInt() == VERSION) { "Unsupported password data version." }
        val nonce = packed.copyOfRange(1, 1 + NONCE_BYTES)
        val ciphertext = packed.copyOfRange(1 + NONCE_BYTES, packed.size)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(associatedData)
            cipher.doFinal(ciphertext)
        } finally {
            packed.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }

    private fun encodeRecords(records: List<PasswordRecordV2>): ByteArray {
        val array = JSONArray()
        records.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id)
                put("scope", r.scope)
                put("ownerUid", r.ownerUid)
                put("websiteApp", r.websiteApp)
                put("username", r.username)
                put("password", r.password)
                put("category", r.category)
                put("notes", r.notes)
                put("createdAt", r.createdAt)
                put("editedAt", r.editedAt)
            })
        }
        return array.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun decodeRecords(bytes: ByteArray): List<PasswordRecordV2> {
        val array = JSONArray(String(bytes, StandardCharsets.UTF_8))
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    PasswordRecordV2(
                        id = o.getString("id"),
                        scope = o.getString("scope"),
                        ownerUid = o.getString("ownerUid"),
                        websiteApp = o.getString("websiteApp"),
                        username = o.getString("username"),
                        password = o.getString("password"),
                        category = o.optString("category"),
                        notes = o.optString("notes"),
                        createdAt = o.optLong("createdAt"),
                        editedAt = o.optLong("editedAt")
                    )
                )
            }
        }.also { bytes.fill(0) }
    }
}
