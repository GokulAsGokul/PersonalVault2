
package com.gokul.personalvault.data.firebase

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Android client for the V2 one-time household join-code workflow.
 *
 * All membership changes are performed by secured Cloud Functions.
 * The Android client never writes membership documents directly.
 */
class HouseholdInvitationRepositoryV2(
    private val functions: FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-south1")
) {

    data class JoinCode(
        val code: String,
        val status: String,
        val expiresAt: Long,
        val expiresInSeconds: Long
    )

    data class MembershipRequest(
        val requestId: String,
        val ownerUid: String,
        val inviteeUid: String,
        val status: String,
        val createdAt: Long?
    )

    fun createJoinCode(callback: (Result<JoinCode>) -> Unit) {
        call("createV2JoinCode", emptyMap())
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>

                val code = data?.get("code") as? String
                val status = data?.get("status") as? String ?: "active"
                val expiresAt = (data?.get("expiresAt") as? Number)?.toLong()
                val expiresInSeconds =
                    (data?.get("expiresInSeconds") as? Number)?.toLong() ?: 900L

                if (code.isNullOrBlank() || expiresAt == null) {
                    callback(
                        Result.failure(
                            IllegalStateException(
                                "The server did not return a valid join code."
                            )
                        )
                    )
                } else {
                    callback(
                        Result.success(
                            JoinCode(
                                code = code,
                                status = status,
                                expiresAt = expiresAt,
                                expiresInSeconds = expiresInSeconds
                            )
                        )
                    )
                }
            }
            .addOnFailureListener {
                callback(Result.failure(it.toFriendlyException()))
            }
    }

    fun submitJoinCode(
        code: String,
        callback: (Result<String>) -> Unit
    ) {
        call(
            "submitV2JoinCode",
            mapOf("code" to code.trim().uppercase())
        )
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val status = data?.get("status") as? String ?: "submitted"
                callback(Result.success(status))
            }
            .addOnFailureListener {
                callback(Result.failure(it.toFriendlyException()))
            }
    }

    fun loadMembershipRequests(
        callback: (Result<List<MembershipRequest>>) -> Unit
    ) {
        call("listMyV2MembershipRequests", emptyMap())
            .addOnSuccessListener { result ->
                val rows = (result.data as? Map<*, *>)
                    ?.get("requests") as? List<*> ?: emptyList<Any>()

                callback(
                    Result.success(rows.mapNotNull { parseRequest(it) })
                )
            }
            .addOnFailureListener {
                callback(Result.failure(it.toFriendlyException()))
            }
    }

    fun approve(
        requestId: String,
        callback: (Result<String>) -> Unit
    ) {
        ownerAction("approveV2Membership", requestId, callback)
    }

    fun reject(
        requestId: String,
        callback: (Result<String>) -> Unit
    ) {
        ownerAction("rejectV2Membership", requestId, callback)
    }

    fun cancelJoinCode(callback: (Result<String>) -> Unit) {
        call("cancelV2JoinCode", emptyMap())
            .addOnSuccessListener { result ->
                val status = (result.data as? Map<*, *>)
                    ?.get("status") as? String ?: "revoked"

                callback(Result.success(status))
            }
            .addOnFailureListener {
                callback(Result.failure(it.toFriendlyException()))
            }
    }

    private fun ownerAction(
        functionName: String,
        requestId: String,
        callback: (Result<String>) -> Unit
    ) {
        call(
            functionName,
            mapOf("requestId" to requestId)
        )
            .addOnSuccessListener { result ->
                val status = (result.data as? Map<*, *>)
                    ?.get("status") as? String ?: "updated"

                callback(Result.success(status))
            }
            .addOnFailureListener {
                callback(Result.failure(it.toFriendlyException()))
            }
    }

    private fun call(
        functionName: String,
        data: Map<String, Any>
    ) = functions.getHttpsCallable(functionName).call(data)

    private fun parseRequest(value: Any?): MembershipRequest? {
        val map = value as? Map<*, *> ?: return null

        val requestId = map["requestId"] as? String ?: return null

        return MembershipRequest(
            requestId = requestId,
            ownerUid = map["ownerUid"] as? String ?: "",
            inviteeUid = map["inviteeUid"] as? String ?: "",
            status = map["status"] as? String ?: "unknown",
            createdAt = (map["createdAt"] as? Number)?.toLong()
        )
    }

    private fun Throwable.toFriendlyException(): Exception {
        val firebaseError = this as? FirebaseFunctionsException

        val message = firebaseError?.message
            ?: localizedMessage
            ?: "The household request could not be completed."

        return IllegalStateException(message, this)
    }
}
