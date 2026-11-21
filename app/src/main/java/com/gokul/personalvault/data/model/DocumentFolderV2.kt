package com.gokul.personalvault.data.model

import com.google.firebase.Timestamp

enum class DocumentScopeV2 { PERSONAL, SHARED }

/**
 * A folder belongs to one existing document category.
 * A null parentFolderId means the folder is directly under that category.
 */
data class DocumentFolderV2(
    val id: String = "",
    val name: String = "",
    val nameLower: String = "",
    val categoryId: String = "other",
    val parentFolderId: String? = null,
    val ownerUid: String = "",
    val scope: String = DocumentScopeV2.PERSONAL.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun fromSnapshot(id: String, data: Map<String, Any?>) = DocumentFolderV2(
            id = id,
            name = data["name"] as? String ?: "",
            nameLower = data["nameLower"] as? String ?: "",
            categoryId = data["categoryId"] as? String ?: "other",
            parentFolderId = data["parentFolderId"] as? String,
            ownerUid = data["ownerUid"] as? String ?: "",
            scope = data["scope"] as? String ?: DocumentScopeV2.PERSONAL.name,
            createdAt = data["createdAt"] as? Timestamp,
            updatedAt = data["updatedAt"] as? Timestamp
        )
    }
}
