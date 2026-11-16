package com.gokul.personalvault

import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

/**
 * Firestore persistence for shared password records.
 *
 * Only the encrypted payload is stored for Website/App, Username/Email,
 * Password and Category. Ownership and timestamps remain queryable metadata.
 */
object PasswordRepository {

    data class PasswordRecord(
        val id: String,
        val createdBy: String,
        val service: String,
        val username: String,
        val password: String,
        val category: String,
        val createdAt: Long,
        val updatedAt: Long
    )

    private fun passwords(db: FirebaseFirestore, householdId: String) =
        db.collection("households")
            .document(householdId)
            .collection("passwords")

    private fun encryptFields(
        householdId: String,
        service: String,
        username: String,
        password: String,
        category: String
    ): Map<String, Any> {
        val json = JSONObject()
            .put("service", service)
            .put("username", username)
            .put("password", password)
            .put("category", category)
            .toString()

        return HouseholdCrypto.encrypt(householdId, json).toMap()
    }

    private fun decryptDocument(
        householdId: String,
        documentId: String,
        data: Map<String, Any?>
    ): PasswordRecord {
        val creator = data["createdBy"] as? String
            ?: error("Password record is missing its creator.")
        val createdAt = (data["createdAt"] as? Number)?.toLong()
            ?: error("Password record is missing its creation date.")
        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: createdAt
        val payload = data["payload"] as? Map<String, Any?>
            ?: error("Password record has no encrypted payload.")

        val jsonText = HouseholdCrypto.decrypt(
            householdId,
            HouseholdCrypto.EncryptedPayload.fromMap(payload)
        )
        val json = JSONObject(jsonText)

        return PasswordRecord(
            id = documentId,
            createdBy = creator,
            service = json.getString("service"),
            username = json.getString("username"),
            password = json.getString("password"),
            category = json.getString("category"),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun load(
        db: FirebaseFirestore,
        householdId: String,
        onResult: (Result<List<PasswordRecord>>) -> Unit
    ) {
        if (!HouseholdCrypto.isUnlocked(householdId)) {
            onResult(Result.failure(IllegalStateException(
                "Household vault is locked. Unlock it before viewing passwords."
            )))
            return
        }

        passwords(db, householdId)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val records = snapshot.documents.map { doc ->
                        val data = doc.data
                            ?: error("Password record ${doc.id} is empty.")
                        decryptDocument(householdId, doc.id, data)
                    }.sortedByDescending { it.updatedAt }
                    onResult(Result.success(records))
                } catch (e: Exception) {
                    onResult(Result.failure(e))
                }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun add(
        db: FirebaseFirestore,
        householdId: String,
        currentUid: String,
        service: String,
        username: String,
        password: String,
        category: String,
        onResult: (Result<String>) -> Unit
    ) {
        try {
            require(currentUid.isNotBlank()) { "Missing signed-in user." }
            check(HouseholdCrypto.isUnlocked(householdId)) {
                "Household vault is locked. Unlock it before saving passwords."
            }

            val now = System.currentTimeMillis()
            val data = hashMapOf<String, Any>(
                "createdBy" to currentUid,
                "createdAt" to now,
                "updatedAt" to now,
                "payload" to encryptFields(
                    householdId, service, username, password, category
                )
            )

            val ref = passwords(db, householdId).document()
            ref.set(data)
                .addOnSuccessListener { onResult(Result.success(ref.id)) }
                .addOnFailureListener { e -> onResult(Result.failure(e)) }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun update(
        db: FirebaseFirestore,
        householdId: String,
        recordId: String,
        currentUid: String,
        service: String,
        username: String,
        password: String,
        category: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        try {
            check(HouseholdCrypto.isUnlocked(householdId)) {
                "Household vault is locked. Unlock it before editing passwords."
            }

            val ref = passwords(db, householdId).document(recordId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                val creator = snapshot.getString("createdBy")
                check(creator == currentUid) {
                    "You can edit only passwords created by your account."
                }

                transaction.update(
                    ref,
                    mapOf(
                        "payload" to encryptFields(
                            householdId, service, username, password, category
                        ),
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                Unit
            }.addOnSuccessListener { onResult(Result.success(Unit)) }
                .addOnFailureListener { e -> onResult(Result.failure(e)) }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun delete(
        db: FirebaseFirestore,
        householdId: String,
        recordId: String,
        currentUid: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (!HouseholdCrypto.isUnlocked(householdId)) {
            onResult(Result.failure(IllegalStateException(
                "Household vault is locked. Unlock it before deleting passwords."
            )))
            return
        }
        val ref = passwords(db, householdId).document(recordId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            check(snapshot.getString("createdBy") == currentUid) {
                "You can delete only passwords created by your account."
            }
            transaction.delete(ref)
            Unit
        }.addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }
}
