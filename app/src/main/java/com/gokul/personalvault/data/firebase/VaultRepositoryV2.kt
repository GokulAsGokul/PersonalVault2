
package com.gokul.personalvault.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

/**
 * Personal Vault V2 vault initialization and membership-aware lookup.
 *
 * A signed-in user can own a vault or belong to another owner's vault.
 * Cloud Functions determine the active vault using approved membership.
 */
object VaultRepositoryV2 {

    private const val COLLECTION_V2 = "vaultsV2"
    private const val SCHEMA_VERSION = 2

    private val functions: FirebaseFunctions
        get() = FirebaseFunctions.getInstance("asia-south1")

    data class Vault(
        val vaultId: String,
        val ownerUid: String,
        val memberUids: List<String>,
        val schemaVersion: Int
    )

    /**
     * Creates or loads the signed-in user's personal V2 vault.
     *
     * This preserves the existing personal vault bootstrap.
     * It does not determine the active shared vault.
     */
    fun createOrLoadOwnerVault(
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        db: FirebaseFirestore = FirebaseFirestore.getInstance(),
        onResult: (Result<Vault>) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(
                Result.failure(
                    IllegalStateException("Please sign in before creating a vault.")
                )
            )
            return
        }

        if (!user.isEmailVerified) {
            onResult(
                Result.failure(
                    IllegalStateException("Verify your email before creating a vault.")
                )
            )
            return
        }

        val uid = user.uid
        val vaultRef = db.collection(COLLECTION_V2).document(uid)

        vaultRef.get()
            .addOnSuccessListener { existing ->
                if (existing.exists()) {
                    val ownerUid = existing.getString("ownerUid").orEmpty()
                    val members = (existing.get("memberUids") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()
                    val schemaVersion =
                        existing.getLong("schemaVersion")?.toInt()

                    if (
                        ownerUid != uid ||
                        uid !in members ||
                        schemaVersion != SCHEMA_VERSION
                    ) {
                        onResult(
                            Result.failure(
                                IllegalStateException(
                                    "The existing V2 personal vault has an unexpected structure. " +
                                        "No changes were made."
                                )
                            )
                        )
                        return@addOnSuccessListener
                    }

                    onResult(
                        Result.success(
                            Vault(
                                vaultId = existing.id,
                                ownerUid = ownerUid,
                                memberUids = members,
                                schemaVersion = schemaVersion
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val newVault = hashMapOf<String, Any>(
                    "ownerUid" to uid,
                    "memberUids" to listOf(uid),
                    "schemaVersion" to SCHEMA_VERSION,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                vaultRef.set(newVault)
                    .addOnSuccessListener {
                        onResult(
                            Result.success(
                                Vault(
                                    vaultId = uid,
                                    ownerUid = uid,
                                    memberUids = listOf(uid),
                                    schemaVersion = SCHEMA_VERSION
                                )
                            )
                        )
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * Resolves the active vault for the signed-in account.
     *
     * Owners receive their own vault.
     * Approved members receive the owner's vault.
     *
     * This lookup is performed by the secured getMyV2Vault
     * Cloud Function, not by direct client-side membership writes.
     */
    fun getMyActiveVault(
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        onResult: (Result<Vault>) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(
                Result.failure(
                    IllegalStateException("Please sign in before loading your vault.")
                )
            )
            return
        }

        if (!user.isEmailVerified) {
            onResult(
                Result.failure(
                    IllegalStateException("Verify your email before loading your vault.")
                )
            )
            return
        }

        functions
            .getHttpsCallable("getMyV2Vault")
            .call(emptyMap<String, Any>())
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>

                val vaultId = data?.get("vaultId") as? String
                val ownerUid = data?.get("ownerUid") as? String
                val memberUids = (data?.get("memberUids") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val schemaVersion =
                    (data?.get("schemaVersion") as? Number)?.toInt()

                if (
                    vaultId.isNullOrBlank() ||
                    ownerUid.isNullOrBlank() ||
                    vaultId != ownerUid ||
                    user.uid !in memberUids ||
                    schemaVersion != SCHEMA_VERSION
                ) {
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "The server returned an invalid active V2 vault response."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                onResult(
                    Result.success(
                        Vault(
                            vaultId = vaultId,
                            ownerUid = ownerUid,
                            memberUids = memberUids,
                            schemaVersion = schemaVersion
                        )
                    )
                )
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }
}
