package com.gokul.personalvault.data.firebase

import android.content.Context
import android.os.Build
import com.gokul.personalvault.HouseholdCrypto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID

/**
 * Device-level approval and shared-key envelope transport for Personal Vault 2.0.
 *
 * Each Android installation has its own Android Keystore RSA key pair and stable
 * installation ID. A household member's device remains pending until the owner
 * approves it. The shared AES key is only ever uploaded as an RSA-wrapped envelope.
 */
class SharedDeviceApprovalRepositoryV2(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-south1")
) {
    data class Device(
        val memberUid: String,
        val deviceId: String,
        val status: String,
        val algorithm: String,
        val deviceLabel: String,
        val publicKey: String,
        val createdAt: Long?
    )

    private fun memberDevices(vaultId: String, memberUid: String) =
        db.collection("vaultsV2").document(vaultId)
            .collection("members").document(memberUid)
            .collection("devices")

    private fun memberEnvelope(vaultId: String, memberUid: String, deviceId: String) =
        db.collection("vaultsV2").document(vaultId)
            .collection("members").document(memberUid)
            .collection("sharedKeyEnvelopes").document(deviceId)

    fun currentDeviceId(context: Context, vaultId: String, memberUid: String): String {
        val prefs = context.applicationContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
        val key = "device_${vaultId}_${memberUid}"
        val existing = prefs.getString(key, null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString().replace("-", "")
        check(prefs.edit().putString(key, created).commit()) {
            "Unable to save this installation's device ID."
        }
        return created
    }

    /** Registers this installation. Owner devices are approved by the server; member devices are pending. */
    fun registerThisDevice(
        context: Context,
        vaultId: String,
        memberUid: String,
        callback: (Result<String>) -> Unit
    ) {
        runCatching {
            val deviceId = currentDeviceId(context, vaultId, memberUid)
            val publicKey = HouseholdCrypto.exportPublicKey(memberUid, deviceId)
            Triple(deviceId, publicKey, mapOf(
                "vaultId" to vaultId,
                "deviceId" to deviceId,
                "publicKey" to publicKey,
                "algorithm" to RSA_ALGORITHM,
                "deviceLabel" to "${Build.MANUFACTURER} ${Build.MODEL}".take(80)
            ))
        }.onFailure {
            callback(Result.failure(it))
        }.onSuccess { (_, _, payload) ->
            functions.getHttpsCallable("registerV2SharedDevice")
                .call(payload)
                .addOnSuccessListener { result ->
                    val status = (result.data as? Map<*, *>)?.get("status") as? String
                    if (status.isNullOrBlank()) {
                        callback(Result.failure(
                            IllegalStateException("The server did not return a device status.")
                        ))
                    } else {
                        callback(Result.success(status))
                    }
                }
                .addOnFailureListener { callback(Result.failure(it)) }
        }
    }

    /** Lists all registered devices. This operation is owner-only on the server. */
    fun loadDevices(callback: (Result<List<Device>>) -> Unit) {
        functions.getHttpsCallable("listV2SharedDevices")
            .call(emptyMap<String, Any>())
            .addOnSuccessListener { result ->
                val rows = (result.data as? Map<*, *>)?.get("devices") as? List<*>
                    ?: emptyList<Any>()
                val devices = rows.mapNotNull { row ->
                    val map = row as? Map<*, *> ?: return@mapNotNull null
                    val memberUid = map["memberUid"] as? String ?: return@mapNotNull null
                    val deviceId = map["deviceId"] as? String ?: return@mapNotNull null
                    Device(
                        memberUid = memberUid,
                        deviceId = deviceId,
                        status = map["status"] as? String ?: "unknown",
                        algorithm = map["algorithm"] as? String ?: "",
                        deviceLabel = map["deviceLabel"] as? String ?: "Android device",
                        publicKey = map["publicKey"] as? String ?: "",
                        createdAt = (map["createdAt"] as? Number)?.toLong()
                    )
                }
                callback(Result.success(devices))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun approveDevice(
        memberUid: String,
        deviceId: String,
        callback: (Result<Unit>) -> Unit
    ) = ownerDeviceAction(
        "approveV2SharedDevice",
        mapOf("memberUid" to memberUid, "deviceId" to deviceId),
        callback
    )

    private fun ownerDeviceAction(
        functionName: String,
        payload: Map<String, Any>,
        callback: (Result<Unit>) -> Unit
    ) {
        functions.getHttpsCallable(functionName).call(payload)
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Unlocks the existing owner envelope, or creates the first shared key envelope. */
    fun ensureOwnerSharedKey(
        vaultId: String,
        ownerUid: String,
        deviceId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val envelopeRef = memberEnvelope(vaultId, ownerUid, deviceId)
        envelopeRef.get()
            .addOnSuccessListener { existing ->
                if (existing.exists()) {
                    unlockThisDevice(vaultId, ownerUid, deviceId, callback)
                } else {
                    initializeSharedKeyOnOwnerDevice(
                        vaultId, ownerUid, deviceId, callback
                    )
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /**
     * Creates the first shared key on an owner device and stores only an RSA
     * envelope in Firestore. The plaintext AES key remains in memory.
     */
    fun initializeSharedKeyOnOwnerDevice(
        vaultId: String,
        ownerUid: String,
        deviceId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val envelopeRef = memberEnvelope(vaultId, ownerUid, deviceId)
        envelopeRef.get()
            .addOnSuccessListener { existing ->
                if (existing.exists()) {
                    callback(Result.failure(
                        IllegalStateException("A shared key already exists for this device.")
                    ))
                    return@addOnSuccessListener
                }
                try {
                    HouseholdCrypto.createHouseholdKey(vaultId)
                    val wrapped = HouseholdCrypto.wrapHouseholdKeyForMember(
                        vaultId,
                        HouseholdCrypto.exportPublicKey(ownerUid, deviceId)
                    )
                    writeEnvelope(
                        envelopeRef,
                        vaultId,
                        ownerUid,
                        deviceId,
                        wrapped.wrappedKeyBase64,
                        callback
                    )
                } catch (error: Exception) {
                    HouseholdCrypto.clearSession()
                    callback(Result.failure(error))
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Owner distributes the in-memory shared key to one already-approved device. */
    fun createEnvelopeForApprovedDevice(
        vaultId: String,
        memberUid: String,
        deviceId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        if (!HouseholdCrypto.isUnlocked(vaultId)) {
            callback(Result.failure(
                IllegalStateException("Unlock the shared vault on the owner device first.")
            ))
            return
        }

        val deviceRef = memberDevices(vaultId, memberUid).document(deviceId)
        val envelopeRef = memberEnvelope(vaultId, memberUid, deviceId)
        deviceRef.get()
            .addOnSuccessListener { device ->
                if (!device.exists() || device.getString("status") != "approved") {
                    callback(Result.failure(
                        IllegalStateException("Only an owner-approved device can receive a shared key.")
                    ))
                    return@addOnSuccessListener
                }
                val publicKey = device.getString("publicKey")
                if (publicKey.isNullOrBlank()) {
                    callback(Result.failure(
                        IllegalStateException("The approved device has no registered public key.")
                    ))
                    return@addOnSuccessListener
                }
                envelopeRef.get()
                    .addOnSuccessListener { existing ->
                        if (existing.exists()) {
                            callback(Result.failure(
                                IllegalStateException("A key envelope already exists. Key rotation is required to replace it.")
                            ))
                            return@addOnSuccessListener
                        }
                        try {
                            val wrapped = HouseholdCrypto.wrapHouseholdKeyForMember(
                                vaultId,
                                publicKey
                            )
                            writeEnvelope(
                                envelopeRef,
                                vaultId,
                                memberUid,
                                deviceId,
                                wrapped.wrappedKeyBase64,
                                callback
                            )
                        } catch (error: Exception) {
                            callback(Result.failure(error))
                        }
                    }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Retrieves and unwraps only this installation's envelope using its Keystore private key. */
    fun unlockThisDevice(
        vaultId: String,
        memberUid: String,
        deviceId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        memberEnvelope(vaultId, memberUid, deviceId).get()
            .addOnSuccessListener { document ->
                val wrappedKey = document.getString("wrappedKey")
                if (!document.exists() || wrappedKey.isNullOrBlank()) {
                    callback(Result.failure(
                        IllegalStateException("The owner has not provisioned shared-key access for this device.")
                    ))
                    return@addOnSuccessListener
                }
                try {
                    HouseholdCrypto.unwrapHouseholdKey(
                        firebaseUid = memberUid,
                        deviceId = deviceId,
                        householdId = vaultId,
                        wrappedKeyBase64 = wrappedKey
                    )
                    callback(Result.success(Unit))
                } catch (error: Exception) {
                    callback(Result.failure(error))
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun writeEnvelope(
        reference: com.google.firebase.firestore.DocumentReference,
        vaultId: String,
        memberUid: String,
        deviceId: String,
        wrappedKey: String,
        callback: (Result<Unit>) -> Unit
    ) {
        reference.set(mapOf(
            "vaultId" to vaultId,
            "memberUid" to memberUid,
            "deviceId" to deviceId,
            "keyVersion" to 1,
            "algorithm" to RSA_ALGORITHM,
            "wrappedKey" to wrappedKey,
            "createdAt" to FieldValue.serverTimestamp()
        )).addOnSuccessListener {
            callback(Result.success(Unit))
        }.addOnFailureListener {
            HouseholdCrypto.clearSession()
            callback(Result.failure(it))
        }
    }

    companion object {
        private const val PREFS = "pv2_shared_device_identity"
        private const val RSA_ALGORITHM = "RSA-OAEP-256"
    }
}
