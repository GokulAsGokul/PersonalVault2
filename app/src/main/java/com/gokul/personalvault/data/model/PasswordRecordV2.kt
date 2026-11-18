package com.gokul.personalvault.data.model

data class PasswordRecordV2(
    val id: String,
    val scope: String,
    val ownerUid: String,
    val websiteApp: String,
    val username: String,
    val password: String,
    val category: String,
    val notes: String,
    val createdAt: Long,
    val editedAt: Long,
    val expiryAt: Long? = null
) {
    val isShared: Boolean get() = scope == SCOPE_SHARED

    companion object {
        const val SCOPE_PERSONAL = "PERSONAL"
        const val SCOPE_SHARED = "SHARED"
        val CATEGORIES = listOf(
            "Social", "Banking", "Shopping", "Work", "Email",
            "Entertainment", "Utilities", "Other"
        )
    }
}
