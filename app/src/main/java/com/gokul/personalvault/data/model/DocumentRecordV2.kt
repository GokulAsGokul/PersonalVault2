package com.gokul.personalvault.data.model

import com.google.firebase.Timestamp

data class DocumentRecordV2(
    val id: String = "",
    val ownerUid: String = "",
    val scope: String = DocumentScopeV2.PERSONAL.name,
    val title: String = "",
    val titleLower: String = "",
    val categoryId: String = "other",
    val folderId: String? = null,
    val description: String = "",
    val originalFileName: String = "",
    val contentType: String = "application/octet-stream",
    val sizeBytes: Long = 0L,
    val storagePath: String = "",
    val uploadedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val purgeAt: Timestamp? = null,
    val favouriteUids: List<String> = emptyList()
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "schemaVersion" to 1,
        "ownerUid" to ownerUid,
        "scope" to scope,
        "title" to title,
        "titleLower" to title.trim().lowercase(),
        "categoryId" to categoryId,
        "folderId" to folderId,
        "description" to description,
        "originalFileName" to originalFileName,
        "contentType" to contentType,
        "sizeBytes" to sizeBytes,
        "storagePath" to storagePath,
        "uploadedAt" to uploadedAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "purgeAt" to purgeAt,
        "favouriteUids" to favouriteUids
    )

    companion object {
        fun fromSnapshot(id: String, data: Map<String, Any?>): DocumentRecordV2 =
            DocumentRecordV2(
                id = id,
                ownerUid = data["ownerUid"] as? String ?: "",
                scope = data["scope"] as? String ?: DocumentScopeV2.PERSONAL.name,
                title = data["title"] as? String ?: "",
                titleLower = data["titleLower"] as? String ?: "",
                categoryId = data["categoryId"] as? String ?: "other",
                folderId = data["folderId"] as? String,
                description = data["description"] as? String ?: "",
                originalFileName = data["originalFileName"] as? String ?: "",
                contentType = data["contentType"] as? String ?: "application/octet-stream",
                sizeBytes = (data["sizeBytes"] as? Number)?.toLong() ?: 0L,
                storagePath = data["storagePath"] as? String ?: "",
                uploadedAt = data["uploadedAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp,
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                deletedAt = data["deletedAt"] as? Timestamp,
                purgeAt = data["purgeAt"] as? Timestamp,
                favouriteUids = (data["favouriteUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
    }
}

data class DocumentCategoryV2(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false
)

object BuiltInDocumentCategoriesV2 {
    val all = listOf(
        DocumentCategoryV2("personal_identity", "Personal Identity", true),
        DocumentCategoryV2("banking_finance", "Banking & Finance", true),
        DocumentCategoryV2("insurance", "Insurance", true),
        DocumentCategoryV2("employment", "Employment", true),
        DocumentCategoryV2("property", "Property", true),
        DocumentCategoryV2("vehicle", "Vehicle", true),
        DocumentCategoryV2("travel_visa", "Travel & Visa", true),
        DocumentCategoryV2("medical", "Medical", true),
        DocumentCategoryV2("education", "Education", true),
        DocumentCategoryV2("legal_documents", "Legal Documents", true),
        DocumentCategoryV2("other", "Other", true)
    )
}
