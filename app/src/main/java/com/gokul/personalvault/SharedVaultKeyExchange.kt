package com.gokul.personalvault

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Firestore transport for the HouseholdCrypto key exchange.
 *
 * Public RSA keys are visible to vault members.
 * Each member's AES household-key envelope is readable only by that member
 * (and the vault owner), and can be created only by the owner.
 */
object SharedVaultKeyExchange {

    private fun deviceKeys(db: FirebaseFirestore, vaultId: String) =
        db.collection("sharedVaults").document(vaultId).collection("deviceKeys")

    private fun envelopes(db: FirebaseFirestore, vaultId: String) =
        db.collection("sharedVaults").document(vaultId).collection("keyEnvelopes")

    fun registerThisDevice(
        db: FirebaseFirestore,
        vaultId: String,
        firebaseUid: String,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val publicKey = HouseholdCrypto.exportPublicKey(firebaseUid)
            val data = mapOf(
                "uid" to firebaseUid,
                "publicKey" to publicKey,
                "version" to 1,
                "algorithm" to "RSA-OAEP-256",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            deviceKeys(db, vaultId).document(firebaseUid)
                .set(data)
                .addOnSuccessListener {
                    onResult(true, "This device's public key is registered.")
                }
                .addOnFailureListener { e ->
                    onResult(false, "Could not register this device: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Could not create this device's key pair.")
        }
    }

    /**
     * Owner-only: fetch vault membership and all public keys, create one
     * random AES shared vault key, wrap it for every member, then write all
     * envelopes atomically. Existing envelopes are never overwritten.
     */
    fun createAndDistributeVaultKey(
        db: FirebaseFirestore,
        vaultId: String,
        ownerUid: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val householdRef = db.collection("sharedVaults").document(vaultId)

        householdRef.get()
            .addOnSuccessListener { household ->
                if (!household.exists() || household.getString("ownerUid") != ownerUid) {
                    onResult(false, "Only the vault owner can create the shared vault key.")
                    return@addOnSuccessListener
                }

                val memberUids = (household.get("memberUids") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.distinct()
                    .orEmpty()

                if (memberUids.isEmpty() || ownerUid !in memberUids) {
                    onResult(false, "Shared vault membership is incomplete.")
                    return@addOnSuccessListener
                }

                deviceKeys(db, vaultId).get()
                    .addOnSuccessListener { keyDocs ->
                        val publicKeys = memberUids.associateWith { uid ->
                            keyDocs.documents.firstOrNull { it.id == uid }
                                ?.getString("publicKey")
                        }

                        val missing = publicKeys.filterValues { it.isNullOrBlank() }.keys
                        if (missing.isNotEmpty()) {
                            onResult(
                                false,
                                "Register every vault member's device first. Missing: ${missing.joinToString()}"
                            )
                            return@addOnSuccessListener
                        }

                        envelopes(db, vaultId).get()
                            .addOnSuccessListener { existingEnvelopes ->
                                if (!existingEnvelopes.isEmpty) {
                                    onResult(
                                        false,
                                        "A shared vault key envelope already exists. Key rotation must be handled separately; no key was replaced."
                                    )
                                    return@addOnSuccessListener
                                }

                                try {
                                    HouseholdCrypto.createHouseholdKey(vaultId)

                                    val batch = db.batch()
                                    memberUids.forEach { uid ->
                                        val wrapped = HouseholdCrypto.wrapHouseholdKeyForMember(
                                            vaultId,
                                            publicKeys.getValue(uid)!!
                                        )
                                        val envelopeRef = envelopes(db, vaultId).document(uid)
                                        batch.set(
                                            envelopeRef,
                                            mapOf(
                                                "uid" to uid,
                                                "version" to wrapped.version,
                                                "algorithm" to wrapped.algorithm,
                                                "wrappedKey" to wrapped.wrappedKeyBase64,
                                                "createdAt" to FieldValue.serverTimestamp()
                                            )
                                        )
                                    }

                                    batch.commit()
                                        .addOnSuccessListener {
                                            onResult(
                                                true,
                                                "Shared vault key created and securely shared with all registered members. This device is unlocked."
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            HouseholdCrypto.clearSession()
                                            onResult(
                                                false,
                                                "Could not save the key envelopes: ${e.localizedMessage}"
                                            )
                                        }
                                } catch (e: Exception) {
                                    HouseholdCrypto.clearSession()
                                    onResult(
                                        false,
                                        e.localizedMessage ?: "Could not create the shared vault key."
                                    )
                                }
                            }
                            .addOnFailureListener { e ->
                                onResult(false, "Could not check existing key envelopes: ${e.localizedMessage}")
                            }
                    }
                    .addOnFailureListener { e ->
                        onResult(false, "Could not load device public keys: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, "Could not read shared vault details: ${e.localizedMessage}")
            }
    }

    /** Member operation: retrieve this account's envelope and unwrap locally. */
    fun unlockForThisMember(
        db: FirebaseFirestore,
        vaultId: String,
        firebaseUid: String,
        onResult: (Boolean, String) -> Unit
    ) {
        envelopes(db, vaultId).document(firebaseUid).get()
            .addOnSuccessListener { document ->
                val wrappedKey = document.getString("wrappedKey")
                if (!document.exists() || wrappedKey.isNullOrBlank()) {
                    onResult(false, "The vault owner has not shared a key with this account yet.")
                    return@addOnSuccessListener
                }

                try {
                    HouseholdCrypto.unwrapHouseholdKey(
                        firebaseUid = firebaseUid,
                        householdId = vaultId,
                        wrappedKeyBase64 = wrappedKey
                    )
                    onResult(true, "Household vault unlocked on this device.")
                } catch (e: Exception) {
                    onResult(
                        false,
                        e.localizedMessage ?: "Could not unlock the shared vault key on this device."
                    )
                }
            }
            .addOnFailureListener { e ->
                onResult(false, "Could not retrieve this account's key envelope: ${e.localizedMessage}")
            }
    }
}
