package com.gokul.personalvault.data.firebase

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.gokul.personalvault.data.model.DocumentRecordV2
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class DocumentRepositoryV2(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    companion object {
        const val MAX_FILE_BYTES = 50L * 1024L * 1024L
        private const val VAULTS = "vaultsV2"
        private const val DOCUMENTS = "documents"
    }

    data class UploadResult(
        val documentId: String,
        val storagePath: String
    )

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

    suspend fun upload(
        vaultId: String,
        ownerUid: String,
        uri: Uri,
        resolver: ContentResolver,
        title: String,
        categoryId: String,
        description: String,
        scope: String,
        onProgress: (Float) -> Unit = {}
    ): UploadResult {
        require(title.isNotBlank() && title.length <= 160) { "Enter a title up to 160 characters." }
        require(description.length <= 50) { "Description must be 50 characters or fewer." }
        require(scope == "PERSONAL" || scope == "SHARED") { "Invalid document visibility." }

        val fileName = readDisplayName(resolver, uri)
        val size = readSizeBytes(resolver, uri)
        require(size in 1..MAX_FILE_BYTES) { "Each file must be between 1 byte and 50 MB." }

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
            "title" to title.trim(),
            "titleLower" to title.trim().lowercase(Locale.ROOT),
            "categoryId" to categoryId,
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
            firestore.collection(VAULTS).document(vaultId)
                .collection(DOCUMENTS).document(documentId).set(record).await()
        } catch (error: Exception) {
            // The Storage object is intentionally retained if metadata creation
            // fails because client delete is denied by Storage Rules. A trusted
            // backend orphan-cleanup process should remove unreferenced objects.
            throw IllegalStateException(
                "File uploaded but its document record could not be saved. Contact support before retrying.",
                error
            )
        }
        onProgress(1f)
        return UploadResult(documentId, storagePath)
    }

    suspend fun moveToRecycleBin(vaultId: String, document: DocumentRecordV2) {
        require(document.ownerUid.isNotBlank()) { "Document owner is missing." }
        val deletedAt = Timestamp.now()
        val calendar = Calendar.getInstance().apply { time = deletedAt.toDate() }
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val purgeAt = Timestamp(calendar.time)

        firestore.collection(VAULTS).document(vaultId)
            .collection(DOCUMENTS).document(document.id)
            .update(
                mapOf(
                    "isDeleted" to true,
                    "deletedAt" to FieldValue.serverTimestamp(),
                    "purgeAt" to purgeAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun restore(vaultId: String, documentId: String) {
        firestore.collection(VAULTS).document(vaultId)
            .collection(DOCUMENTS).document(documentId)
            .update(
                mapOf(
                    "isDeleted" to false,
                    "deletedAt" to null,
                    "purgeAt" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun downloadUrl(vaultId: String, document: DocumentRecordV2): String {
        require(document.storagePath.startsWith("vaultsV2/$vaultId/documents/")) {
            "Invalid document storage path."
        }
        return storage.reference.child(document.storagePath).downloadUrl.await().toString()
    }

    fun documentCollection(vaultId: String) =
        firestore.collection(VAULTS).document(vaultId).collection(DOCUMENTS)

    fun customCategoryCollection(vaultId: String) =
        firestore.collection(VAULTS).document(vaultId).collection("documentCategories")
}
