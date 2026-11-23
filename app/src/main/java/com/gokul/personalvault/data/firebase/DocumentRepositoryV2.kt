package com.gokul.personalvault.data.firebase

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.gokul.personalvault.data.model.DocumentRecordV2
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.io.File
import java.util.UUID

class DocumentRepositoryV2(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1")
) {
    companion object {
        const val MAX_FILE_BYTES = 50L * 1024L * 1024L
        const val DOCUMENT_PAGE_SIZE = 50L
        private const val VAULTS = "vaultsV2"
        private const val DOCUMENTS = "documents"
        private val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "txt"
        )
    }

    data class UploadResult(val documentId: String, val storagePath: String)

    fun isSupportedFileName(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return extension in SUPPORTED_EXTENSIONS
    }

    fun readDisplayName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "document"
    }

    fun readSizeBytes(resolver: ContentResolver, uri: Uri): Long {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0 && !cursor.isNull(index)) return cursor.getLong(index)
                }
            }
        return -1L
    }

    fun documentCollection(vaultId: String) =
        firestore.collection(VAULTS).document(vaultId).collection(DOCUMENTS)

    fun sharedDocumentsQuery(vaultId: String) =
        documentCollection(vaultId).whereEqualTo("scope", "SHARED")

    fun personalDocumentsQuery(vaultId: String, memberUid: String) =
        documentCollection(vaultId).whereEqualTo("scope", "PERSONAL")
            .whereEqualTo("ownerUid", memberUid)

    fun sharedDocumentsPageQuery(vaultId: String, after: DocumentSnapshot? = null): Query {
        var query = sharedDocumentsQuery(vaultId)
            .orderBy(FieldPath.documentId())
            .limit(DOCUMENT_PAGE_SIZE)
        if (after != null) query = query.startAfter(after)
        return query
    }

    fun personalDocumentsPageQuery(
        vaultId: String,
        memberUid: String,
        after: DocumentSnapshot? = null
    ): Query {
        var query = personalDocumentsQuery(vaultId, memberUid)
            .orderBy(FieldPath.documentId())
            .limit(DOCUMENT_PAGE_SIZE)
        if (after != null) query = query.startAfter(after)
        return query
    }

    fun customCategoryCollection(vaultId: String) =
        firestore.collection(VAULTS).document(vaultId).collection("documentCategories")

    fun folderCollection(vaultId: String) =
        firestore.collection(VAULTS).document(vaultId).collection("documentFolders")

    fun foldersQuery(vaultId: String, categoryId: String) =
        folderCollection(vaultId).whereEqualTo("categoryId", categoryId)

    fun sharedFoldersQuery(vaultId: String, categoryId: String) =
        folderCollection(vaultId).whereEqualTo("categoryId", categoryId)
            .whereEqualTo("scope", "SHARED")

    fun personalFoldersQuery(vaultId: String, categoryId: String, memberUid: String) =
        folderCollection(vaultId).whereEqualTo("categoryId", categoryId)
            .whereEqualTo("scope", "PERSONAL")
            .whereEqualTo("ownerUid", memberUid)

    suspend fun createFolder(
        vaultId: String,
        ownerUid: String,
        name: String,
        categoryId: String,
        parentFolderId: String?,
        scope: String
    ): String {
        val cleanName = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
        }
        require(cleanName.isNotEmpty() && cleanName.length <= 30) {
            "Folder name must be between 1 and 30 characters."
        }
        require(categoryId.isNotBlank()) { "Choose a document category." }
        require(scope == "PERSONAL" || scope == "SHARED") { "Invalid folder visibility." }

        if (parentFolderId != null) {
            val parent = folderCollection(vaultId).document(parentFolderId).get().await()
            require(parent.exists()) { "The parent folder no longer exists." }
            require(parent.getString("categoryId") == categoryId) {
                "A subfolder must remain inside its parent's category."
            }
            require(parent.getString("scope") == scope) {
                "A subfolder must use the same visibility as its parent folder."
            }
            require(scope != "PERSONAL" || parent.getString("ownerUid") == ownerUid) {
                "You cannot create a folder inside another member's personal folder."
            }
        }

        var siblingsQuery = folderCollection(vaultId)
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("parentFolderId", parentFolderId)
            .whereEqualTo("scope", scope)
        if (scope == "PERSONAL") {
            siblingsQuery = siblingsQuery.whereEqualTo("ownerUid", ownerUid)
        }
        val siblings = siblingsQuery.get().await()
        val nameLower = cleanName.lowercase(Locale.ROOT)
        require(siblings.documents.none {
            it.getString("nameLower") == nameLower
        }) { "A folder with this name already exists at this location." }

        val folderId = UUID.randomUUID().toString()
        val now = FieldValue.serverTimestamp()
        folderCollection(vaultId).document(folderId).set(
            mapOf(
                "schemaVersion" to 1,
                "name" to cleanName,
                "nameLower" to nameLower,
                "categoryId" to categoryId,
                "parentFolderId" to parentFolderId,
                "ownerUid" to ownerUid,
                "scope" to scope,
                "createdAt" to now,
                "updatedAt" to now
            )
        ).await()
        return folderId
    }

    suspend fun renameFolder(vaultId: String, folder: com.gokul.personalvault.data.model.DocumentFolderV2, name: String) {
        val cleanName = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
        }
        require(cleanName.isNotEmpty() && cleanName.length <= 30) {
            "Folder name must be between 1 and 30 characters."
        }
        var siblingsQuery = folderCollection(vaultId)
            .whereEqualTo("categoryId", folder.categoryId)
            .whereEqualTo("parentFolderId", folder.parentFolderId)
            .whereEqualTo("scope", folder.scope)
        if (folder.scope == "PERSONAL") {
            siblingsQuery = siblingsQuery.whereEqualTo("ownerUid", folder.ownerUid)
        }
        val siblings = siblingsQuery.get().await()
        val nameLower = cleanName.lowercase(Locale.ROOT)
        require(siblings.documents.none {
            it.id != folder.id && it.getString("nameLower") == nameLower
        }) { "A folder with this name already exists at this location." }
        folderCollection(vaultId).document(folder.id).update(
            mapOf(
                "name" to cleanName,
                "nameLower" to nameLower,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteEmptyFolder(vaultId: String, folderId: String) {
        functions.getHttpsCallable("deleteEmptyV2DocumentFolder")
            .call(mapOf("vaultId" to vaultId, "folderId" to folderId)).await()
    }

    suspend fun moveFolder(
        vaultId: String,
        folder: com.gokul.personalvault.data.model.DocumentFolderV2,
        newParentFolderId: String?
    ) {
        require(newParentFolderId != folder.id) { "A folder cannot be moved inside itself." }
        if (newParentFolderId != null) {
            val parent = folderCollection(vaultId).document(newParentFolderId).get().await()
            require(parent.exists()) { "The destination folder no longer exists." }
            require(parent.getString("categoryId") == folder.categoryId) {
                "Folders can only be moved within the same category."
            }
            require(parent.getString("scope") == folder.scope) {
                "The destination must have the same Personal / Shared visibility."
            }
            require(folder.scope != "PERSONAL" || parent.getString("ownerUid") == folder.ownerUid) {
                "You cannot move a personal folder into another member's folder."
            }

            // Walk the destination's ancestors to prevent moving a folder into its own subtree.
            var ancestorId: String? = newParentFolderId
            val visited = mutableSetOf<String>()
            while (ancestorId != null) {
                require(ancestorId != folder.id) { "A folder cannot be moved inside its own subfolder." }
                require(visited.add(ancestorId)) { "The destination folder hierarchy contains a cycle." }
                val ancestor = folderCollection(vaultId).document(ancestorId).get().await()
                require(ancestor.exists()) { "The destination folder hierarchy is incomplete." }
                ancestorId = ancestor.getString("parentFolderId")
            }
        }

        val siblings = folderCollection(vaultId)
            .whereEqualTo("categoryId", folder.categoryId)
            .whereEqualTo("parentFolderId", newParentFolderId)
            .whereEqualTo("scope", folder.scope)
            .get().await()
        require(siblings.documents.none {
            it.id != folder.id && it.getString("nameLower") == folder.nameLower
        }) { "A folder with this name already exists at the destination." }

        folderCollection(vaultId).document(folder.id).update(
            mapOf("parentFolderId" to newParentFolderId, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }


    suspend fun moveDocumentToFolder(
        vaultId: String,
        document: DocumentRecordV2,
        targetFolderId: String?
    ) {
        require(!document.isDeleted) { "Restore this document before moving it." }
        if (targetFolderId != null) {
            val target = folderCollection(vaultId).document(targetFolderId).get().await()
            require(target.exists()) { "The destination folder no longer exists." }
            require(target.getString("categoryId") == document.categoryId) {
                "Documents can only be moved within their existing category."
            }
            require(target.getString("scope") == document.scope) {
                "The destination folder must have the same Personal / Shared visibility."
            }
            require(document.scope != "PERSONAL" || target.getString("ownerUid") == document.ownerUid) {
                "You cannot move a personal document into another member's folder."
            }
        }
        documentCollection(vaultId).document(document.id).update(
            mapOf("folderId" to targetFolderId, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun upload(
        vaultId: String,
        ownerUid: String,
        uri: Uri,
        resolver: ContentResolver,
        title: String,
        categoryId: String,
        description: String,
        scope: String,
        folderId: String? = null,
        onProgress: (Float) -> Unit = {}
    ): UploadResult {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty() && cleanTitle.length <= 50) { "Enter a file name up to 50 characters." }
        require(description.length <= 50) { "Notes must be 50 characters or fewer." }
        require(scope == "PERSONAL" || scope == "SHARED") { "Invalid document visibility." }
        if (folderId != null) {
            val folder = folderCollection(vaultId).document(folderId).get().await()
            require(folder.exists()) { "The selected folder no longer exists." }
            require(folder.getString("categoryId") == categoryId) {
                "The selected folder belongs to a different category."
            }
            require(folder.getString("scope") == scope) {
                "The selected folder must have the same Personal / Shared visibility."
            }
            require(scope != "PERSONAL" || folder.getString("ownerUid") == ownerUid) {
                "You cannot upload a personal document into another member's folder."
            }
        }
        val fileName = readDisplayName(resolver, uri)
        val size = readSizeBytes(resolver, uri)
        require(size in 1..MAX_FILE_BYTES) { "Each file must be between 1 byte and 50 MB." }
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        require(extension in SUPPORTED_EXTENSIONS) {
            "Unsupported file type. Supported formats: PDF, JPG, PNG, DOC, DOCX, XLS, XLSX, PPT, PPTX and TXT."
        }

        val titleLower = cleanTitle.lowercase(Locale.ROOT)
        val duplicate = documentCollection(vaultId)
            .whereEqualTo("ownerUid", ownerUid)
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("titleLower", titleLower)
            .whereEqualTo("isDeleted", false)
            .limit(1).get().await()
        require(duplicate.isEmpty) {
            "A document with this title already exists in this category. Open it and choose Replace Document instead."
        }

        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val documentId = UUID.randomUUID().toString()
        val storagePath = "vaultsV2/$vaultId/documents/$ownerUid/$documentId"
        val storageRef = storage.reference.child(storagePath)
        val customMetadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("vaultId", vaultId)
            .setCustomMetadata("ownerUid", ownerUid)
            .setCustomMetadata("documentId", documentId)
            .setCustomMetadata("scope", scope)
            .setCustomMetadata("fileExtension", extension)
            .build()

        val uploadTask = storageRef.putFile(uri, customMetadata)
        uploadTask.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            if (total > 0L) onProgress(snapshot.bytesTransferred.toFloat() / total.toFloat())
        }
        uploadTask.await()

        val now = FieldValue.serverTimestamp()
        val record = hashMapOf<String, Any?>(
            "schemaVersion" to 1,
            "ownerUid" to ownerUid,
            "scope" to scope,
            "title" to cleanTitle,
            "titleLower" to titleLower,
            "categoryId" to categoryId,
            "folderId" to folderId,
            "description" to description.trim(),
            "originalFileName" to fileName,
            "contentType" to mimeType,
            "sizeBytes" to size,
            "storagePath" to storagePath,
            "uploadedAt" to now,
            "updatedAt" to now,
            "isDeleted" to false,
            "deletedAt" to null,
            "purgeAt" to null
        )
        try {
            documentCollection(vaultId).document(documentId).set(record).await()
        } catch (error: Exception) {
            throw IllegalStateException(
                "The file reached Storage but its record could not be saved. Do not retry yet; the incomplete upload needs cleanup.",
                error
            )
        }
        onProgress(1f)
        return UploadResult(documentId, storagePath)
    }

    suspend fun updateMetadata(
        vaultId: String,
        document: DocumentRecordV2,
        title: String,
        description: String,
        categoryId: String,
        scope: String
    ) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty() && cleanTitle.length <= 50) { "File name must be 1–50 characters." }
        require(description.length <= 50) { "Notes must be 50 characters or fewer." }
        require(scope == "PERSONAL" || scope == "SHARED") { "Invalid visibility." }
        val duplicate = documentCollection(vaultId)
            .whereEqualTo("ownerUid", document.ownerUid)
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("titleLower", cleanTitle.lowercase(Locale.ROOT))
            .whereEqualTo("isDeleted", false)
            .limit(2).get().await().documents.any { it.id != document.id }
        require(!duplicate) { "Another document already uses this title in that category." }
        documentCollection(vaultId).document(document.id).update(
            mapOf(
                "title" to cleanTitle,
                "titleLower" to cleanTitle.lowercase(Locale.ROOT),
                "description" to description.trim(),
                "categoryId" to categoryId,
                "scope" to scope,
                // A folder is bound to one category and one visibility.
                // Clear the folder assignment when either is changed.
                "folderId" to if (categoryId != document.categoryId || scope != document.scope) null else document.folderId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Replaces the binary at the document's existing Storage path.
     * The previous file is overwritten and is not retained as a version.
     * Document identity and all user-defined metadata remain unchanged.
     */
    suspend fun replaceFile(
        vaultId: String,
        document: DocumentRecordV2,
        uri: Uri,
        resolver: ContentResolver,
        onProgress: (Float) -> Unit = {}
    ): String {
        require(!document.isDeleted) { "Restore this document before replacing its file." }
        val expectedPath = "vaultsV2/" + vaultId + "/documents/" +
            document.ownerUid + "/" + document.id
        require(document.storagePath == expectedPath) { "Invalid document storage path." }
        val fileName = readDisplayName(resolver, uri)
        val size = readSizeBytes(resolver, uri)
        require(size in 1..MAX_FILE_BYTES) { "The replacement file must be between 1 byte and 50 MB." }
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        require(extension in SUPPORTED_EXTENSIONS) {
            "Unsupported replacement file type. Supported formats: PDF, JPG, PNG, DOC, DOCX, XLS, XLSX, PPT, PPTX and TXT."
        }

        // Derive the document title from the selected filename, excluding its extension.
        val replacementTitle = fileName.substringBeforeLast('.')
            .trim()
            .take(50)
        require(replacementTitle.isNotBlank()) { "The replacement filename must contain a valid title." }

        // Check duplicates before overwriting the existing Storage object.
        val duplicate = documentCollection(vaultId)
            .whereEqualTo("ownerUid", document.ownerUid)
            .whereEqualTo("categoryId", document.categoryId)
            .whereEqualTo("titleLower", replacementTitle.lowercase(Locale.ROOT))
            .whereEqualTo("isDeleted", false)
            .limit(2).get().await().documents.any { it.id != document.id }
        require(!duplicate) {
            "Another document already uses the title '$replacementTitle' in this category. Rename that document or choose another file."
        }

        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("vaultId", vaultId)
            .setCustomMetadata("ownerUid", document.ownerUid)
            .setCustomMetadata("documentId", document.id)
            .setCustomMetadata("scope", document.scope)
            .setCustomMetadata("fileExtension", extension)
            .build()

        val storageRef = storage.reference.child(document.storagePath)
        val uploadTask = storageRef.putFile(uri, metadata)
        uploadTask.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            if (total > 0L) onProgress(snapshot.bytesTransferred.toFloat() / total.toFloat())
        }
        uploadTask.await()

        try {
            documentCollection(vaultId).document(document.id).update(
                mapOf(
                    "title" to replacementTitle,
                    "titleLower" to replacementTitle.lowercase(Locale.ROOT),
                    "originalFileName" to fileName,
                    "contentType" to mimeType,
                    "sizeBytes" to size,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (error: Exception) {
            throw IllegalStateException(
                "The replacement file was uploaded, but its document details could not be updated. Refresh before trying again.",
                error
            )
        }
        onProgress(1f)
        return replacementTitle
    }

    suspend fun createCategory(vaultId: String, name: String): String {
        val result = functions.getHttpsCallable("createV2DocumentCategory")
            .call(mapOf("vaultId" to vaultId, "name" to name.trim())).await()
        return (result.data as? Map<*, *>)?.get("categoryId") as? String
            ?: throw IllegalStateException("Category was created but its ID was not returned.")
    }

    suspend fun renameCategory(vaultId: String, categoryId: String, name: String) {
        functions.getHttpsCallable("renameV2DocumentCategory")
            .call(mapOf("vaultId" to vaultId, "categoryId" to categoryId, "name" to name.trim())).await()
    }

    suspend fun deleteCategory(vaultId: String, categoryId: String) {
        functions.getHttpsCallable("deleteV2DocumentCategory")
            .call(mapOf("vaultId" to vaultId, "categoryId" to categoryId)).await()
    }

    suspend fun setFavourite(vaultId: String, documentId: String, memberUid: String, favourite: Boolean) {
        require(memberUid.isNotBlank()) { "A signed-in household member is required." }
        documentCollection(vaultId).document(documentId)
            .update(com.google.firebase.firestore.FieldPath.of("favouriteUids", memberUid), favourite)
            .await()
    }

    suspend fun moveToRecycleBin(vaultId: String, document: DocumentRecordV2) {
        functions.getHttpsCallable("softDeleteV2Document")
            .call(mapOf("vaultId" to vaultId, "documentId" to document.id)).await()
    }

    suspend fun restore(vaultId: String, documentId: String) {
        functions.getHttpsCallable("restoreV2Document")
            .call(mapOf("vaultId" to vaultId, "documentId" to documentId)).await()
    }

    suspend fun permanentlyDelete(vaultId: String, documentId: String) {
        functions.getHttpsCallable("permanentlyDeleteV2Document")
            .call(mapOf("vaultId" to vaultId, "documentId" to documentId)).await()
    }

    suspend fun emptyRecycleBin(vaultId: String): Int {
        val result = functions.getHttpsCallable("emptyRecycleBinV2Documents")
            .call(mapOf("vaultId" to vaultId)).await()
        val data = result.data as? Map<*, *>
        return (data?.get("deletedCount") as? Number)?.toInt() ?: 0
    }

    suspend fun downloadToCache(
        cacheDir: File,
        vaultId: String,
        document: DocumentRecordV2,
        onProgress: (transferredBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): File {
        require(!document.isDeleted) { "Restore this document before opening or sharing it." }
        val expectedPath = "vaultsV2/$vaultId/documents/" + document.ownerUid + "/" + document.id
        require(document.storagePath == expectedPath) {
            "Invalid document storage path."
        }
        require(document.ownerUid.isNotBlank() && document.id.isNotBlank()) {
            "Invalid document identity."
        }
        val extension = document.originalFileName.substringAfterLast('.', "")
            .lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }
            .take(10)
        require(extension in SUPPORTED_EXTENSIONS) { "Unsupported document file type." }
        val targetDirectory = File(cacheDir, "documents").apply { mkdirs() }
        val suffix = if (extension.isBlank()) "" else ".$extension"
        val localFile = File(targetDirectory, document.id + suffix)
        val partialFile = File(targetDirectory, document.id + suffix + ".part")
        partialFile.delete()
        try {
            val downloadTask = storage.reference.child(expectedPath).getFile(partialFile)
            downloadTask.addOnProgressListener { snapshot ->
                onProgress(snapshot.bytesTransferred, snapshot.totalByteCount)
            }
            downloadTask.await()
            require(partialFile.isFile && partialFile.length() > 0L) {
                "The downloaded document is empty."
            }
            if (document.sizeBytes > 0L) {
                require(partialFile.length() == document.sizeBytes) {
                    "The downloaded file size does not match its document record."
                }
            }
            if (localFile.exists() && !localFile.delete()) {
                throw IllegalStateException("Unable to replace the previous cached document.")
            }
            if (!partialFile.renameTo(localFile)) {
                partialFile.copyTo(localFile, overwrite = true)
                partialFile.delete()
            }
            onProgress(localFile.length(), localFile.length())
            return localFile
        } catch (error: Exception) {
            partialFile.delete()
            throw error
        }
    }

    fun cleanupDocumentCache(cacheDir: File, maxAgeMillis: Long = 7L * 24L * 60L * 60L * 1000L) {
        val directory = File(cacheDir, "documents")
        if (!directory.exists()) return
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        val abandonedTempCutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        directory.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            val abandonedCapture = file.name.startsWith("Photo_") ||
                file.name.startsWith("Scanned_Document_")
            val expiredCache = file.lastModified() < cutoff
            val abandonedCaptureExpired = abandonedCapture && file.lastModified() < abandonedTempCutoff
            if (expiredCache || abandonedCaptureExpired) file.delete()
        }
    }
}
