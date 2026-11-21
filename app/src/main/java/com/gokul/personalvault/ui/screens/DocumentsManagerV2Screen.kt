package com.gokul.personalvault.ui.screens

import android.app.DownloadManager
import android.app.Activity
import android.graphics.BitmapFactory
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokul.personalvault.VaultExternalActivityState
import com.gokul.personalvault.data.firebase.DocumentRepositoryV2
import com.gokul.personalvault.data.model.BuiltInDocumentCategoriesV2
import com.gokul.personalvault.data.model.DocumentRecordV2
import com.gokul.personalvault.data.model.DocumentFolderV2
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.io.File
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private enum class DocumentSortV2 { NAME_ASC, NAME_DESC, NEWEST, OLDEST, LARGEST, SMALLEST }

private data class PendingDocumentUploadV2(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val title: String,
    val notes: String = ""
)

private data class PendingDocumentReplacementV2(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long
)

private const val MAX_DOCUMENT_BATCH_COUNT = 10
private const val MAX_DOCUMENT_BATCH_BYTES = 200L * 1024L * 1024L
private const val MAX_DOCUMENT_FILE_BYTES = 50L * 1024L * 1024L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsManagerV2Screen(vaultId: String, memberUid: String) {
    val context = LocalContext.current
    val repository = remember { DocumentRepositoryV2() }
    val scope = rememberCoroutineScope()
    var documents by remember { mutableStateOf<List<DocumentRecordV2>>(emptyList()) }
    var folders by remember { mutableStateOf<List<DocumentFolderV2>>(emptyList()) }
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderScope by remember { mutableStateOf("PERSONAL") }
    var selectedFolder by remember { mutableStateOf<DocumentFolderV2?>(null) }
    var showFolderActions by remember { mutableStateOf(false) }
    var showRenameFolder by remember { mutableStateOf(false) }
    var renameFolderName by remember { mutableStateOf("") }
    var showMoveFolder by remember { mutableStateOf(false) }
    var moveParentFolderId by remember { mutableStateOf<String?>(null) }
    var moveTargetChosen by remember { mutableStateOf(false) }
    var showMoveDocument by remember { mutableStateOf(false) }
    var documentToMove by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var moveDocumentTargetId by remember { mutableStateOf<String?>(null) }
    var moveDocumentTargetChosen by remember { mutableStateOf(false) }
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }
    var permanentDeleteTarget by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var visibility by remember { mutableStateOf("ALL") }
    var fileType by remember { mutableStateOf("ALL") }
    var sort by remember { mutableStateOf(DocumentSortV2.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(false) }
    var recycleBin by remember { mutableStateOf(false) }
    var showAllDocuments by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentFileProgress by remember { mutableFloatStateOf(0f) }
    var currentUploadIndex by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf("") }
    var categoryDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    var manageCategories by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Pair<String, String>?>(null) }
    var categoryEditName by remember { mutableStateOf("") }
    var customCategories by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedDocument by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var editingDocument by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("other") }
    var editScope by remember { mutableStateOf("PERSONAL") }
    var busyDocument by remember { mutableStateOf(false) }
    val pendingUploads = remember { mutableStateListOf<PendingDocumentUploadV2>() }
    var showAddDocuments by remember { mutableStateOf(false) }
    var showAddOptions by remember { mutableStateOf(false) }
    var showDocumentDetails by remember { mutableStateOf(false) }
    var uploadCategory by remember { mutableStateOf("other") }
    var uploadScope by remember { mutableStateOf("PERSONAL") }
    var uploadCategoryMenu by remember { mutableStateOf(false) }
    var replacementTarget by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var pendingReplacement by remember { mutableStateOf<PendingDocumentReplacementV2?>(null) }
    var replacingDocument by remember { mutableStateOf(false) }
    var replacementProgress by remember { mutableFloatStateOf(0f) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPreview by remember { mutableStateOf(false) }

    DisposableEffect(vaultId) {
        val sharedDocs = mutableMapOf<String, DocumentRecordV2>()
        val personalDocs = mutableMapOf<String, DocumentRecordV2>()
        fun publishDocuments() {
            documents = (sharedDocs.values + personalDocs.values).distinctBy { it.id }
        }
        val sharedListener: ListenerRegistration = repository.sharedDocumentsQuery(vaultId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    sharedDocs.clear()
                    snapshot.documents.forEach { doc ->
                        doc.data?.let { sharedDocs[doc.id] = DocumentRecordV2.fromSnapshot(doc.id, it) }
                    }
                    publishDocuments()
                } else if (error != null) message = "Shared documents: ${error.localizedMessage ?: "Unable to load documents."}"
            }
        val personalListener: ListenerRegistration = repository.personalDocumentsQuery(vaultId, memberUid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    personalDocs.clear()
                    snapshot.documents.forEach { doc ->
                        doc.data?.let { personalDocs[doc.id] = DocumentRecordV2.fromSnapshot(doc.id, it) }
                    }
                    publishDocuments()
                } else if (error != null) message = "Personal documents: ${error.localizedMessage ?: "Unable to load documents."}"
            }
        val categoriesListener = repository.customCategoryCollection(vaultId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    customCategories = snapshot.documents.mapNotNull { doc ->
                        doc.getString("name")?.let { doc.id to it }
                    }
                }
            }
        onDispose { sharedListener.remove(); personalListener.remove(); categoriesListener.remove() }
    }

    DisposableEffect(vaultId, category, documentToMove?.categoryId) {
        val folderCategory = if (category == "all") documentToMove?.categoryId else category
        if (folderCategory == null) {
            folders = emptyList()
            currentFolderId = null
            onDispose { }
        } else {
            val sharedFolders = mutableMapOf<String, DocumentFolderV2>()
            val personalFolders = mutableMapOf<String, DocumentFolderV2>()
            fun publishFolders() {
                folders = (sharedFolders.values + personalFolders.values).distinctBy { it.id }
            }
            val sharedFolderListener = repository.sharedFoldersQuery(vaultId, folderCategory)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        sharedFolders.clear()
                        snapshot.documents.forEach { doc ->
                            doc.data?.let { sharedFolders[doc.id] = DocumentFolderV2.fromSnapshot(doc.id, it) }
                        }
                        publishFolders()
                    } else if (error != null) message = "Shared folders: ${error.localizedMessage ?: "Unable to load folders."}"
                }
            val personalFolderListener = repository.personalFoldersQuery(vaultId, folderCategory, memberUid)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        personalFolders.clear()
                        snapshot.documents.forEach { doc ->
                            doc.data?.let { personalFolders[doc.id] = DocumentFolderV2.fromSnapshot(doc.id, it) }
                        }
                        publishFolders()
                    } else if (error != null) message = "Personal folders: ${error.localizedMessage ?: "Unable to load folders."}"
                }
            onDispose {
                sharedFolderListener.remove()
                personalFolderListener.remove()
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        VaultExternalActivityState.documentPickerActive = false
        if (uris.isNotEmpty()) {
            if (uris.size > MAX_DOCUMENT_BATCH_COUNT) {
                message = "Select up to $MAX_DOCUMENT_BATCH_COUNT files per batch. You selected ${uris.size}."
                return@rememberLauncherForActivityResult
            }
            val selectedFiles = uris.map { uri ->
                val fileName = runCatching {
                    repository.readDisplayName(context.contentResolver, uri)
                }.getOrDefault("document")
                val sizeBytes = repository.readSizeBytes(context.contentResolver, uri)
                PendingDocumentUploadV2(
                    uri = uri,
                    fileName = fileName,
                    sizeBytes = sizeBytes,
                    title = fileName.substringBeforeLast('.', fileName).take(50)
                )
            }
            val oversized = selectedFiles.filter { it.sizeBytes > MAX_DOCUMENT_FILE_BYTES }
            if (oversized.isNotEmpty()) {
                message = "Each file must be 50 MB or smaller. Too large: " +
                    oversized.joinToString { it.fileName }
                return@rememberLauncherForActivityResult
            }
            val unknownSize = selectedFiles.filter { it.sizeBytes < 0L }
            if (unknownSize.isNotEmpty()) {
                message = "Could not determine file size for: " +
                    unknownSize.joinToString { it.fileName } + ". Choose files from a provider that reports file size."
                return@rememberLauncherForActivityResult
            }
            val totalBytes = selectedFiles.sumOf { it.sizeBytes }
            if (totalBytes > MAX_DOCUMENT_BATCH_BYTES) {
                message = "The selected files total ${formatDocumentBytes(totalBytes)}. The batch limit is 200 MB."
                return@rememberLauncherForActivityResult
            }
            pendingUploads.clear()
            pendingUploads.addAll(selectedFiles)
            uploadCategory = if (category == "all") "other" else category
            uploadScope = if (visibility == "SHARED") "SHARED" else "PERSONAL"
            showAddDocuments = true
        } else {
            message = "No files selected."
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(MAX_DOCUMENT_BATCH_COUNT)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        VaultExternalActivityState.documentPickerActive = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdfUri = scanResult?.pdf?.uri
            if (pdfUri != null) {
                val pageCount = scanResult.pdf?.pageCount ?: 1
                val fileName = "Scanned_Document_${System.currentTimeMillis()}.pdf"
                val sizeBytes = repository.readSizeBytes(context.contentResolver, pdfUri)
                if (sizeBytes in 1..MAX_DOCUMENT_FILE_BYTES) {
                    pendingUploads.clear()
                    pendingUploads.add(
                        PendingDocumentUploadV2(
                            uri = pdfUri,
                            fileName = fileName,
                            sizeBytes = sizeBytes,
                            title = "Scanned Document".take(50),
                            notes = if (pageCount > 1) "${pageCount} pages scanned" else ""
                        )
                    )
                    val parentFolder = folders.firstOrNull { it.id == currentFolderId }
                    uploadCategory = parentFolder?.categoryId ?: if (category == "all") "other" else category
                    uploadScope = parentFolder?.scope ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
                    showAddDocuments = true
                } else {
                    message = if (sizeBytes < 0L) {
                        "Unable to read the scanned PDF size. Please scan again."
                    } else {
                        "The scanned PDF exceeds the 50 MB file limit."
                    }
                }
            } else {
                message = "The scanner did not return a PDF. Please scan again."
            }
        } else if (result.resultCode != android.app.Activity.RESULT_CANCELED) {
            message = "Document scanning was not completed."
        }
    }

    val photoCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        VaultExternalActivityState.documentPickerActive = false
        val photoFile = pendingPhotoFile
        val photoUri = pendingPhotoUri
        if (success && photoFile != null && photoUri != null && photoFile.exists() && photoFile.length() > 0L) {
            capturedPhotoUri = photoUri
            showPhotoPreview = true
        } else {
            photoFile?.delete()
            pendingPhotoFile = null
            pendingPhotoUri = null
            message = if (success) "The camera did not save a photo. Please try again." else "Photo capture cancelled."
        }
    }

    val replacementPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        VaultExternalActivityState.documentPickerActive = false
        if (uri != null) {
            val fileName = runCatching {
                repository.readDisplayName(context.contentResolver, uri)
            }.getOrDefault("document")
            val sizeBytes = repository.readSizeBytes(context.contentResolver, uri)
            when {
                sizeBytes !in 1..MAX_DOCUMENT_FILE_BYTES ->
                    message = "The replacement file must be between 1 byte and 50 MB."
                fileName.substringAfterLast('.', "").isBlank() ->
                    message = "Choose a file with a valid extension."
                else -> {
                    pendingReplacement = PendingDocumentReplacementV2(uri, fileName, sizeBytes)
                    val derivedTitle = fileName.substringBeforeLast('.').trim().take(50)
                    if (derivedTitle.isNotBlank()) editTitle = derivedTitle
                }
            }
        } else {
            pendingReplacement = null
            replacementTarget = null
            editTitle = editingDocument?.title ?: editTitle
        }
    }

    fun launchPhotoCapture() {
        try {
            val directory = File(context.cacheDir, "documents").apply { mkdirs() }
            val photoFile = File(directory, "Photo_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingPhotoFile = photoFile
            pendingPhotoUri = photoUri
            VaultExternalActivityState.documentPickerActive = true
            photoCaptureLauncher.launch(photoUri)
        } catch (error: Exception) {
            VaultExternalActivityState.documentPickerActive = false
            message = error.localizedMessage ?: "Unable to open the camera."
        }
    }

    fun acceptCapturedPhoto() {
        val photoFile = pendingPhotoFile
        val photoUri = capturedPhotoUri
        if (photoFile == null || photoUri == null || !photoFile.exists()) {
            message = "The captured photo is no longer available. Please take it again."
            showPhotoPreview = false
            return
        }
        val sizeBytes = photoFile.length()
        if (sizeBytes !in 1..MAX_DOCUMENT_FILE_BYTES) {
            message = "The photo must be between 1 byte and 50 MB."
            return
        }
        val fileName = photoFile.name
        pendingUploads.clear()
        pendingUploads.add(
            PendingDocumentUploadV2(
                uri = photoUri,
                fileName = fileName,
                sizeBytes = sizeBytes,
                title = fileName.substringBeforeLast('.').take(50)
            )
        )
        val parentFolder = folders.firstOrNull { it.id == currentFolderId }
        uploadCategory = parentFolder?.categoryId ?: if (category == "all") "other" else category
        uploadScope = parentFolder?.scope ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
        showPhotoPreview = false
        showAddDocuments = true
    }

    val active = documents.filter { !it.isDeleted }
    val deleted = documents.filter { it.isDeleted }
    val source = if (recycleBin) deleted else active
    val visible = source
        .filter { visibility == "ALL" || it.scope == visibility }
        .filter { category == "all" || it.categoryId == category }
        .filter { it.folderId == currentFolderId }
        .filter {
            val q = search.trim().lowercase(Locale.ROOT)
            q.isEmpty() || it.title.lowercase(Locale.ROOT).contains(q) ||
                it.description.lowercase(Locale.ROOT).contains(q)
        }
        .filter {
            when (fileType) {
                "PDF" -> it.contentType.contains("pdf", true)
                "IMAGE" -> it.contentType.startsWith("image/")
                "OFFICE" -> listOf("msword", "wordprocessing", "spreadsheet", "presentation", "excel", "powerpoint")
                    .any { token -> it.contentType.contains(token, true) }
                "OTHER" -> !it.contentType.contains("pdf", true) &&
                    !it.contentType.startsWith("image/") &&
                    !listOf("msword", "wordprocessing", "spreadsheet", "presentation", "excel", "powerpoint")
                        .any { token -> it.contentType.contains(token, true) }
                else -> true
            }
        }.let { list ->
            when (sort) {
                DocumentSortV2.NAME_ASC -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
                DocumentSortV2.NAME_DESC -> list.sortedByDescending { it.title.lowercase(Locale.ROOT) }
                DocumentSortV2.NEWEST -> list.sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }
                DocumentSortV2.OLDEST -> list.sortedBy { it.uploadedAt?.toDate()?.time ?: 0L }
                DocumentSortV2.LARGEST -> list.sortedByDescending { it.sizeBytes }
                DocumentSortV2.SMALLEST -> list.sortedBy { it.sizeBytes }
            }
        }

    val bytes = active.sumOf { it.sizeBytes }
    val pdfs = active.count { it.contentType.contains("pdf", true) }
    val images = active.count { it.contentType.startsWith("image/") }
    val others = active.size - pdfs - images
    val allCategories = BuiltInDocumentCategoriesV2.all.map { it.id to it.name } + customCategories

    fun editDocument(doc: DocumentRecordV2) {
        editTitle = doc.title
        editDescription = doc.description
        editCategory = doc.categoryId
        editScope = doc.scope
        editingDocument = doc
    }

    fun openDocument(doc: DocumentRecordV2) {
        if (doc.isDeleted) {
            selectedDocument = doc
            return
        }
        scope.launch {
            busyDocument = true
            try {
                val file = repository.downloadToCache(context.cacheDir, vaultId, doc)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val mimeType = doc.contentType.takeIf {
                    it.isNotBlank() && it != "application/octet-stream"
                } ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    doc.originalFileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                ) ?: "*/*"
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newUri(context.contentResolver, doc.originalFileName, uri)
                }
                context.startActivity(Intent.createChooser(viewIntent, "Open document"))
            } catch (error: Exception) {
                message = error.localizedMessage ?: error.message ?: "Unable to open document."
            } finally {
                busyDocument = false
            }
        }
    }

    fun shareDocument(doc: DocumentRecordV2) {
        scope.launch {
            busyDocument = true
            try {
                val file = repository.downloadToCache(context.cacheDir, vaultId, doc)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val mimeType = doc.contentType.takeIf {
                    it.isNotBlank() && it != "application/octet-stream"
                } ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    doc.originalFileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                ) ?: "*/*"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, doc.originalFileName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share document"))
                selectedDocument = null
            } catch (error: Exception) {
                message = error.localizedMessage ?: error.message ?: "Unable to share document."
            } finally {
                busyDocument = false
            }
        }
    }

    fun deleteDocument(doc: DocumentRecordV2) {
        scope.launch {
            busyDocument = true
            try {
                repository.moveToRecycleBin(vaultId, doc)
                message = "Moved To Recycle Bin. Retained For 30 Days."
            } catch (error: Exception) {
                message = error.message ?: "Delete Failed."
            } finally {
                busyDocument = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!recycleBin) SmallFloatingActionButton(
                onClick = { showAddOptions = true },
                containerColor = Color(0xFF6F35D5),
                contentColor = Color.White,
                shape = RoundedCornerShape(15.dp)
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add documents") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF6F35D5)
                    ) {
                        Icon(Icons.Filled.FolderOpen, null,
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp).size(22.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (recycleBin) "Recycle Bin" else if (category == "all") "Documents"
                                else allCategories.firstOrNull { it.first == category }?.second ?: "Documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (recycleBin) "Deleted files · 30-day retention"
                            else "${active.size} files · Personal Vault",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { search = if (search.isBlank()) " " else "" }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Search, "Search")
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.MoreVert, "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (recycleBin) "All Documents" else "Recycle Bin") },
                                onClick = {
                                    recycleBin = !recycleBin
                                    category = "all"
                                    showAllDocuments = false
                                    visibility = "ALL"
                                    currentFolderId = null
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manage Categories") },
                                onClick = { manageCategories = true; menuExpanded = false }
                            )
                        }
                    }
                }
            }
            if (search.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = search.trimStart(),
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search documents") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("ALL" to "All", "PERSONAL" to "Personal", "SHARED" to "Shared").forEach { (value, label) ->
                        FilterChip(
                            selected = visibility == value,
                            onClick = {
                                visibility = value
                                if (value == "ALL") {
                                    category = "all"
                                    currentFolderId = null
                                    showAllDocuments = true
                                } else {
                                    category = "all"
                                    currentFolderId = null
                                    showAllDocuments = false
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(29.dp),
                            shape = RoundedCornerShape(50),
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6F35D5),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = Color(0xFFF1EAFB),
                                labelColor = Color(0xFF34264B)
                            )
                        )
                    }
                    FilterChip(
                        modifier = Modifier.height(29.dp),
                        shape = RoundedCornerShape(50),
                        border = null,
                        selected = recycleBin,
                        onClick = {
                            recycleBin = !recycleBin
                            category = "all"
                            showAllDocuments = false
                            currentFolderId = null
                        },
                        label = { Text("Recycle Bin", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6F35D5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1EAFB),
                            labelColor = Color(0xFF34264B)
                        )
                    )
                }
            }
            if (!recycleBin && category == "all" && currentFolderId == null && !showAllDocuments) {
                items(allCategories.chunked(3)) { categoryRow ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        categoryRow.forEachIndexed { index, (id, name) ->
                            val count = active.count { it.categoryId == id }
                            val icon = when (id) {
                                "banking_finance" -> Icons.Filled.AccountBalance
                                "property" -> Icons.Filled.Home
                                "insurance" -> Icons.Filled.HealthAndSafety
                                "employment" -> Icons.Filled.Work
                                "vehicle" -> Icons.Filled.DirectionsCar
                                "travel_visa" -> Icons.Filled.Flight
                                "medical" -> Icons.Filled.MedicalServices
                                "education" -> Icons.Filled.School
                                "legal_documents" -> Icons.Filled.Gavel
                                else -> Icons.Filled.Description
                            }
                            val accent = when (id) {
                                "banking_finance" -> Color(0xFF087747)
                                "property" -> Color(0xFFD66A08)
                                "insurance" -> Color(0xFF1767B1)
                                "employment" -> Color(0xFF7134BD)
                                "vehicle" -> Color(0xFF155C9F)
                                "travel_visa" -> Color(0xFF087579)
                                "medical" -> Color(0xFFC93655)
                                "education" -> Color(0xFF8A5A0A)
                                "legal_documents" -> Color(0xFF394B9B)
                                "other" -> Color(0xFF7134BD)
                                else -> Color(0xFF7652C8)
                            }
                            val iconTile = when (id) {
                                "banking_finance" -> Color(0xFFE0F5EA)
                                "property" -> Color(0xFFFFEBD8)
                                "insurance" -> Color(0xFFE1F0FF)
                                "employment" -> Color(0xFFF0E5FF)
                                "vehicle" -> Color(0xFFE3F0FF)
                                "travel_visa" -> Color(0xFFDFF4F3)
                                "medical" -> Color(0xFFFFE7EC)
                                "education" -> Color(0xFFFFF2D8)
                                "legal_documents" -> Color(0xFFE8EBFF)
                                "other" -> Color(0xFFF0E5FF)
                                else -> Color(0xFFE9DFFF)
                            }
                            Card(
                                Modifier.weight(1f).clickable {
                                    category = id
                                    currentFolderId = null
                                    showAllDocuments = false
                                },
                                shape = RoundedCornerShape(15.dp),
                                border = if (category == id) androidx.compose.foundation.BorderStroke(
                                    1.5.dp, MaterialTheme.colorScheme.primary
                                ) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = iconTile.copy(alpha = 0.92f)
                                )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().heightIn(min = 78.dp).padding(vertical = 8.dp, horizontal = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(23.dp)
                                    )
                                    Text(name, style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("$count files", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        repeat(3 - categoryRow.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent Files", Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAllDocuments = true }) { Text("See all") }
                    }
                }
                items(active.filter { visibility == "ALL" || it.scope == visibility }
                    .sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }.take(2), key = { "recent-${it.id}" }) { doc ->
                    DocumentCardV2(
                        doc, Modifier.fillMaxWidth(), false, memberUid,
                        categoryName = allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other",
                        onClick = { openDocument(doc) },
                        onMore = { selectedDocument = doc }
                    )
                }
            } else if (!recycleBin) {
                if (category != "all") {
                    val folderPath = mutableListOf<DocumentFolderV2>()
                    var folderCursor = currentFolderId
                    val visitedFolderIds = mutableSetOf<String>()
                    while (folderCursor != null && visitedFolderIds.add(folderCursor)) {
                        val pathFolder = folders.firstOrNull { it.id == folderCursor } ?: break
                        folderPath.add(0, pathFolder)
                        folderCursor = pathFolder.parentFolderId
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { currentFolderId = null },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text(allCategories.firstOrNull { it.first == category }?.second ?: "Category") }
                            folderPath.forEach { crumb ->
                                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(
                                    onClick = { currentFolderId = crumb.id },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(crumb.name, color = if (crumb.id == currentFolderId)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
                if (category != "all") {
                    val childFolders = folders.filter {
                        it.parentFolderId == currentFolderId && (visibility == "ALL" || it.scope == visibility)
                    }.sortedBy { it.name.lowercase(Locale.ROOT) }
                    if (childFolders.isNotEmpty()) {
                        items(childFolders, key = { "folder-${it.id}" }) { folder ->
                            ListItem(
                                headlineContent = { Text(folder.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(folder.scope.lowercase().replaceFirstChar { it.uppercase() }) },
                                leadingContent = { Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary) },
                                trailingContent = {
                                    if (folder.ownerUid == memberUid) {
                                        IconButton(onClick = { selectedFolder = folder; showFolderActions = true }) {
                                            Icon(Icons.Outlined.MoreVert, "Folder actions")
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { currentFolderId = folder.id },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                    item {
                        TextButton(onClick = {
                            newFolderName = ""
                            newFolderScope = currentFolderId?.let { id ->
                                folders.firstOrNull { it.id == id }?.scope
                            } ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
                            showCreateFolder = true
                        }) {
                            Icon(Icons.Outlined.Add, null)
                            Text("Create Folder")
                        }
                    }
                }
            }
            if (recycleBin || category != "all" || currentFolderId != null || showAllDocuments) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (recycleBin) "Deleted Documents" else if (category == "all") "Documents"
                                else "Files",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${visible.size} files", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Box {
                            TextButton(onClick = { sortMenu = true }) { Text("Sort By") }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                listOf(
                                    DocumentSortV2.NAME_ASC to "Name A–Z",
                                    DocumentSortV2.NAME_DESC to "Name Z–A",
                                    DocumentSortV2.NEWEST to "Newest first",
                                    DocumentSortV2.OLDEST to "Oldest first",
                                    DocumentSortV2.LARGEST to "Largest first",
                                    DocumentSortV2.SMALLEST to "Smallest first"
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { sort = value; sortMenu = false })
                                }
                            }
                        }
                        IconButton(onClick = { grid = !grid }) {
                            Icon(if (grid) Icons.Outlined.List else Icons.Outlined.GridView, "Toggle view")
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("ALL" to "All", "PDF" to "Pdf", "IMAGE" to "Image", "OFFICE" to "Office", "OTHER" to "Other")
                            .forEach { (value, label) ->
                                FilterChip(fileType == value, { fileType = value }, label = { Text(label) })
                            }
                    }
                }
                if (visible.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary)
                                Text(if (recycleBin) "Recycle Bin is empty" else "No documents found",
                                    fontWeight = FontWeight.SemiBold)
                                Text(if (recycleBin) "Deleted files will appear here." else "Upload a file to get started.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(visible, key = { it.id }) { doc ->
                        DocumentCardV2(
                            doc, Modifier.fillMaxWidth(), recycleBin, memberUid,
                            categoryName = allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other",
                            onClick = { if (recycleBin) selectedDocument = doc else openDocument(doc) },
                            onMore = { selectedDocument = doc }
                        )
                    }
                }
    
            }
            if (uploading || message.isNotBlank()) {
                item {
                    if (uploading) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(message, style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                        }
                    } else {
                        Text(message, color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAddOptions) {
        ModalBottomSheet(
            onDismissRequest = { showAddOptions = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Add to Documents", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                ListItem(
                    headlineContent = { Text("Upload Files", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("From device (PDF, images, documents, etc.)") },
                    leadingContent = { Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        showAddOptions = false
                        val parentFolder = folders.firstOrNull { it.id == currentFolderId }
                        uploadCategory = if (category == "all") "other" else category
                        uploadScope = parentFolder?.scope
                            ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
                        VaultExternalActivityState.documentPickerActive = true
                        picker.launch(arrayOf("*/*"))
                    }
                )
                ListItem(
                    headlineContent = { Text("Create Folder", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Organise your documents") },
                    leadingContent = { Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        showAddOptions = false
                        if (category == "all") {
                            message = "Choose a category before creating a folder."
                        } else {
                            newFolderName = ""
                            newFolderScope = if (visibility == "SHARED") "SHARED" else "PERSONAL"
                            showCreateFolder = true
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("Scan Document", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Use camera to scan (PDF)") },
                    leadingContent = { Icon(Icons.Outlined.DocumentScanner, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        showAddOptions = false
                        val activity = context as? Activity
                        if (activity == null) {
                            message = "Unable to open the document scanner from this screen."
                        } else {
                            VaultExternalActivityState.documentPickerActive = true
                            GmsDocumentScanning.getClient(scannerOptions)
                                .getStartScanIntent(activity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                                .addOnFailureListener { error ->
                                    VaultExternalActivityState.documentPickerActive = false
                                    message = error.localizedMessage
                                        ?: "Unable to start document scanning. Check Google Play services and try again."
                                }
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("Take Photo", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Capture and save as document") },
                    leadingContent = { Icon(Icons.Outlined.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        showAddOptions = false
                        launchPhotoCapture()
                    }
                )
            }
        }
    }

    if (showPhotoPreview) {
        val photoUri = capturedPhotoUri
        val previewBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
            initialValue = null,
            key1 = photoUri
        ) {
            value = withContext(Dispatchers.IO) {
                photoUri?.let { uri ->
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
            }
        }
        AlertDialog(
            onDismissRequest = {
                if (!uploading) {
                    showPhotoPreview = false
                    pendingPhotoFile?.delete()
                    pendingPhotoFile = null
                    pendingPhotoUri = null
                    capturedPhotoUri = null
                }
            },
            title = { Text("Photo Preview", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!,
                            contentDescription = "Captured document photo",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    Text(
                        pendingPhotoFile?.name ?: "Captured photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Review the photo before adding it to your Documents.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = previewBitmap != null,
                    onClick = { acceptCapturedPhoto() }
                ) { Text("Use Photo") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { launchPhotoCapture() }) { Text("Retake") }
                    TextButton(onClick = {
                        showPhotoPreview = false
                        pendingPhotoFile?.delete()
                        pendingPhotoFile = null
                        pendingPhotoUri = null
                        capturedPhotoUri = null
                    }) { Text("Cancel") }
                }
            }
        )
    }

    if (showAddDocuments) {
        AlertDialog(
            onDismissRequest = { if (!uploading) { showAddDocuments = false; pendingUploads.clear() } },
            title = { Text("Add Documents") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("${pendingUploads.size} of $MAX_DOCUMENT_BATCH_COUNT files selected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Limits: 50 MB per file · 200 MB per batch",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Total selected: ${formatDocumentBytes(pendingUploads.sumOf { it.sizeBytes })}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text("Save to", fontWeight = FontWeight.SemiBold)
                    val uploadParentFolder = folders.firstOrNull { it.id == currentFolderId }
                    if (uploadParentFolder != null) {
                        Text(
                            "Saving inside ${uploadParentFolder.name} · ${uploadParentFolder.scope.lowercase().replaceFirstChar { it.uppercase() }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uploadScope == "PERSONAL",
                                onClick = { uploadScope = "PERSONAL" },
                                label = { Text("Personal") },
                                enabled = !uploading
                            )
                            FilterChip(
                                selected = uploadScope == "SHARED",
                                onClick = { uploadScope = "SHARED" },
                                label = { Text("Shared") },
                                enabled = !uploading
                            )
                        }
                    }
                    Text("Category", fontWeight = FontWeight.SemiBold)
                    if (uploadParentFolder != null) {
                        Text(
                            allCategories.firstOrNull { it.first == uploadParentFolder.categoryId }?.second ?: "Category",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Box {
                            OutlinedButton(
                                onClick = { uploadCategoryMenu = true },
                                enabled = !uploading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(allCategories.firstOrNull { it.first == uploadCategory }?.second ?: "Other")
                                Spacer(Modifier.weight(1f))
                                Text("⌄")
                            }
                            DropdownMenu(
                                expanded = uploadCategoryMenu,
                                onDismissRequest = { uploadCategoryMenu = false }
                            ) {
                                allCategories.forEach { (id, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = { uploadCategory = id; uploadCategoryMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Divider()
                    pendingUploads.forEachIndexed { index, pending ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(pending.fileName, fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            OutlinedTextField(
                                value = pending.title,
                                onValueChange = { value ->
                                    pendingUploads[index] = pending.copy(title = value.take(50))
                                },
                                label = { Text("File name (max 50)") },
                                supportingText = { Text("${pending.title.length}/50") },
                                singleLine = true,
                                enabled = !uploading,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pending.notes,
                                onValueChange = { value ->
                                    pendingUploads[index] = pending.copy(notes = value.take(50))
                                },
                                label = { Text("Notes (max 50)") },
                                supportingText = { Text("${pending.notes.length}/50") },
                                singleLine = true,
                                enabled = !uploading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (index != pendingUploads.lastIndex) Divider()
                    }
                    if (uploading) {
                        Text(message, style = MaterialTheme.typography.bodySmall)
                        val activeUpload = pendingUploads.getOrNull(currentUploadIndex)
                        if (activeUpload != null) {
                            Text(
                                "${formatDocumentBytes((activeUpload.sizeBytes * currentFileProgress).toLong())} / ${formatDocumentBytes(activeUpload.sizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { currentFileProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text("Batch progress", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uploading && pendingUploads.isNotEmpty() &&
                        pendingUploads.all { it.title.isNotBlank() },
                    onClick = {
                        scope.launch {
                            uploading = true
                            progress = 0f
                            currentFileProgress = 0f
                            var completed = 0
                            val failures = mutableListOf<String>()
                            val total = pendingUploads.size
                            try {
                                pendingUploads.toList().forEachIndexed { index, pending ->
                                    message = "Uploading ${index + 1} of $total: ${pending.fileName}"
                                    currentFileProgress = 0f
                                    currentUploadIndex = index
                                    try {
                                        repository.upload(
                                            vaultId, memberUid, pending.uri, context.contentResolver,
                                            pending.title.trim(), uploadCategory, pending.notes.trim(), uploadScope,
                                            currentFolderId.takeIf { uploadCategory == category }
                                        ) { value ->
                                            currentFileProgress = value
                                            progress = (completed + value) / total.toFloat()
                                        }
                                        completed++
                                    } catch (error: Exception) {
                                        failures.add("${pending.fileName}: ${error.localizedMessage ?: error.message ?: "Upload failed"}")
                                    }
                                }
                                progress = 1f
                                message = if (failures.isEmpty()) "$completed file(s) uploaded."
                                    else "$completed uploaded; ${failures.size} failed. " + failures.joinToString(" | ")
                                showAddDocuments = false
                                pendingUploads.clear()
                            } catch (error: Exception) {
                                message = "Upload failed: ${error.localizedMessage ?: error.message ?: "Unknown error"}"
                            } finally {
                                uploading = false
                            }
                        }
                    }
                ) { Text(if (uploading) "Uploading…" else "Upload") }
            },
            dismissButton = {
                TextButton(
                    enabled = !uploading,
                    onClick = { showAddDocuments = false; pendingUploads.clear() }
                ) { Text("Cancel") }
            }
        )
    }

    if (categoryDialog) {
        AlertDialog(
            onDismissRequest = { categoryDialog = false },
            title = { Text("New category") },
            text = { OutlinedTextField(newCategory, { newCategory = it.take(30) },
                label = { Text("Category name (max 30)") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val clean = newCategory.trim()
                    val duplicate = customCategories.any { it.second.equals(clean, true) } ||
                        BuiltInDocumentCategoriesV2.all.any { it.name.equals(clean, true) }
                    if (clean.isNotEmpty() && !duplicate) {
                        scope.launch {
                            try {
                                repository.createCategory(vaultId, clean)
                                newCategory = ""; categoryDialog = false
                            } catch (error: Exception) {
                                message = error.message ?: "Unable to create category."
                            }
                        }
                    } else message = "Enter a unique category name."
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { categoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (manageCategories) {
        AlertDialog(
            onDismissRequest = { manageCategories = false; categoryToEdit = null },
            title = { Text("Manage custom categories") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (customCategories.isEmpty()) Text("No custom categories yet.")
                    customCategories.forEach { (id, name) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(1f))
                            TextButton(onClick = { categoryToEdit = id to name; categoryEditName = name }) { Text("Rename") }
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        repository.deleteCategory(vaultId, id)
                                        if (category == id) category = "all"
                                        message = "Category deleted."
                                    } catch (error: Exception) {
                                        message = error.message ?: "Reassign documents before deleting this category."
                                    }
                                }
                            }) { Text("Delete") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { manageCategories = false }) { Text("Done") } }
        )
    }

    categoryToEdit?.let { (id, oldName) ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Rename category") },
            text = { OutlinedTextField(categoryEditName, { categoryEditName = it.take(30) },
                label = { Text("Category name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val clean = categoryEditName.trim()
                    val duplicate = customCategories.any { it.first != id && it.second.equals(clean, true) } ||
                        BuiltInDocumentCategoriesV2.all.any { it.name.equals(clean, true) }
                    if (clean.isNotEmpty() && !duplicate) scope.launch {
                        try {
                            repository.renameCategory(vaultId, id, clean)
                            categoryToEdit = null
                            message = "Category renamed."
                        } catch (error: Exception) { message = error.message ?: "Rename failed." }
                    } else message = "Enter a unique category name."
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { categoryToEdit = null }) { Text("Cancel") } }
        )
    }

    if (showFolderActions) {
        val folder = selectedFolder
        if (folder != null) {
            ModalBottomSheet(onDismissRequest = { showFolderActions = false }) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(folder.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Folder actions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ListItem(
                        headlineContent = { Text("Rename Folder") },
                        leadingContent = { Icon(Icons.Outlined.Edit, null) },
                        modifier = Modifier.clickable {
                            renameFolderName = folder.name
                            showFolderActions = false
                            showRenameFolder = true
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Move Folder") },
                        supportingContent = { Text("Move this folder with all its subfolders and files") },
                        leadingContent = { Icon(Icons.Outlined.DriveFileMove, null) },
                        modifier = Modifier.clickable {
                            moveParentFolderId = folder.parentFolderId
                            moveTargetChosen = false
                            showFolderActions = false
                            showMoveFolder = true
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Delete Empty Folder", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("Available only when the folder has no files or subfolders") },
                        leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            showFolderActions = false
                            showDeleteFolderConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (showRenameFolder) {
        AlertDialog(
            onDismissRequest = { showRenameFolder = false },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it.take(30) },
                    label = { Text("Folder name") },
                    supportingText = { Text(renameFolderName.length.toString() + "/30") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(enabled = renameFolderName.trim().isNotEmpty() && !busyDocument, onClick = {
                    val folder = selectedFolder ?: return@TextButton
                    scope.launch {
                        busyDocument = true
                        try {
                            repository.renameFolder(vaultId, folder, renameFolderName)
                            showRenameFolder = false
                            message = "Folder renamed."
                        } catch (error: Exception) {
                            message = error.localizedMessage ?: error.message ?: "Unable to rename folder."
                        } finally { busyDocument = false }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRenameFolder = false }) { Text("Cancel") } }
        )
    }

    if (showMoveFolder) {
        val folder = selectedFolder
        if (folder != null) {
            val descendants = remember(folders, folder.id) {
                val result = mutableSetOf<String>()
                var changed = true
                while (changed) {
                    changed = false
                    folders.filter { it.parentFolderId == folder.id || (it.parentFolderId != null && it.parentFolderId in result) }
                        .forEach { if (result.add(it.id)) changed = true }
                }
                result
            }
            val destinations = folders.filter {
                it.id != folder.id && it.id !in descendants &&
                    it.categoryId == folder.categoryId && it.scope == folder.scope &&
                    (folder.scope == "SHARED" || it.ownerUid == folder.ownerUid) &&
                    it.id != folder.parentFolderId
            }.sortedBy { it.name.lowercase(Locale.ROOT) }
            AlertDialog(
                onDismissRequest = { showMoveFolder = false },
                title = { Text("Move Folder") },
                text = {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Choose a destination. All subfolders and documents will move with this folder.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val rootLabel = allCategories.firstOrNull { it.first == folder.categoryId }?.second ?: "Category"
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                moveParentFolderId = null
                                moveTargetChosen = true
                            }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = moveTargetChosen && moveParentFolderId == null, onClick = {
                                moveParentFolderId = null
                                moveTargetChosen = true
                            })
                            Text(rootLabel + " (Root)")
                        }
                        destinations.forEach { destination ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    moveParentFolderId = destination.id
                                    moveTargetChosen = true
                                }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = moveTargetChosen && moveParentFolderId == destination.id, onClick = {
                                    moveParentFolderId = destination.id
                                    moveTargetChosen = true
                                })
                                Text(destination.name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = moveTargetChosen && !busyDocument, onClick = {
                        scope.launch {
                            busyDocument = true
                            try {
                                repository.moveFolder(vaultId, folder, moveParentFolderId)
                                showMoveFolder = false
                                if (currentFolderId == folder.id) currentFolderId = folder.parentFolderId
                                message = "Folder moved."
                            } catch (error: Exception) {
                                message = error.localizedMessage ?: error.message ?: "Unable to move folder."
                            } finally { busyDocument = false }
                        }
                    }) { Text("Move") }
                },
                dismissButton = { TextButton(onClick = { showMoveFolder = false }) { Text("Cancel") } }
            )
        }
    }

    if (showDeleteFolderConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderConfirm = false },
            title = { Text("Delete Empty Folder?") },
            text = { Text("Only an empty folder can be deleted. Documents and subfolders must be moved first.") },
            confirmButton = {
                TextButton(enabled = !busyDocument, onClick = {
                    val folder = selectedFolder ?: return@TextButton
                    scope.launch {
                        busyDocument = true
                        try {
                            repository.deleteEmptyFolder(vaultId, folder.id)
                            showDeleteFolderConfirm = false
                            if (currentFolderId == folder.id) currentFolderId = folder.parentFolderId
                            message = "Folder deleted."
                        } catch (error: Exception) {
                            message = error.localizedMessage ?: error.message ?: "Folder could not be deleted."
                        } finally { busyDocument = false }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteFolderConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showCreateFolder) {
        AlertDialog(
            onDismissRequest = { showCreateFolder = false },
            title = { Text("Create Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it.take(30) },
                        label = { Text("Folder name") },
                        supportingText = { Text(newFolderName.length.toString() + "/30") },
                        singleLine = true
                    )
                    val parentScope = currentFolderId?.let { id ->
                        folders.firstOrNull { it.id == id }?.scope
                    }
                    Text("Visibility", style = MaterialTheme.typography.labelMedium)
                    if (parentScope != null) {
                        Text(
                            if (parentScope == "SHARED") "Shared (inherited from parent folder)" else "Personal (inherited from parent folder)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(newFolderScope == "PERSONAL", { newFolderScope = "PERSONAL" }, label = { Text("Personal") })
                            FilterChip(newFolderScope == "SHARED", { newFolderScope = "SHARED" }, label = { Text("Shared") })
                        }
                    }
                    Text(
                        if (currentFolderId == null) "Folder will be created inside " +
                            (allCategories.firstOrNull { it.first == category }?.second ?: "this category") + "."
                        else "Folder will be created inside " +
                            (folders.firstOrNull { it.id == currentFolderId }?.name ?: "the selected folder") + ".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newFolderName.trim().isNotEmpty() && !busyDocument,
                    onClick = {
                        scope.launch {
                            busyDocument = true
                            try {
                                repository.createFolder(
                                    vaultId, memberUid, newFolderName, category, currentFolderId, newFolderScope
                                )
                                showCreateFolder = false
                                message = "Folder created."
                            } catch (error: Exception) {
                                message = error.localizedMessage ?: error.message ?: "Unable to create folder."
                            } finally {
                                busyDocument = false
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateFolder = false }) { Text("Cancel") } }
        )
    }


    if (showMoveDocument) {
        val doc = documentToMove
        if (doc != null) {
            val eligibleFolders = folders.filter { folder ->
                folder.categoryId == doc.categoryId &&
                    folder.scope == doc.scope &&
                    (doc.scope == "SHARED" || folder.ownerUid == doc.ownerUid)
            }.sortedBy { it.name.lowercase(Locale.ROOT) }
            AlertDialog(
                onDismissRequest = { showMoveDocument = false; documentToMove = null },
                title = { Text("Move Document") },
                text = {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Choose a folder in ${allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "this category"}. Personal and Shared access cannot be mixed inside a folder.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                moveDocumentTargetId = null
                                moveDocumentTargetChosen = true
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = moveDocumentTargetChosen && moveDocumentTargetId == null,
                                onClick = { moveDocumentTargetId = null; moveDocumentTargetChosen = true }
                            )
                            Column {
                                Text("Category root", fontWeight = FontWeight.SemiBold)
                                Text("Move out of the current folder", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        eligibleFolders.forEach { folder ->
                            val depth = generateSequence(folder.parentFolderId) { parentId ->
                                eligibleFolders.firstOrNull { it.id == parentId }?.parentFolderId
                            }.take(8).count()
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    moveDocumentTargetId = folder.id
                                    moveDocumentTargetChosen = true
                                }.padding(
    start = (depth * 14).dp,
    top = 7.dp,
    bottom = 7.dp
),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = moveDocumentTargetChosen && moveDocumentTargetId == folder.id,
                                    onClick = { moveDocumentTargetId = folder.id; moveDocumentTargetChosen = true }
                                )
                                Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(folder.name)
                            }
                        }
                        if (eligibleFolders.isEmpty()) {
                            Text("No eligible folders. Create a folder in this category first.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = moveDocumentTargetChosen && !busyDocument, onClick = {
                        scope.launch {
                            busyDocument = true
                            try {
                                repository.moveDocumentToFolder(vaultId, doc, moveDocumentTargetId)
                                showMoveDocument = false
                                documentToMove = null
                                message = if (moveDocumentTargetId == null) "Document moved to category root."
                                    else "Document moved to folder."
                            } catch (error: Exception) {
                                message = error.localizedMessage ?: error.message ?: "Unable to move document."
                            } finally { busyDocument = false }
                        }
                    }) { Text("Move") }
                },
                dismissButton = {
                    TextButton(onClick = { showMoveDocument = false; documentToMove = null }) { Text("Cancel") }
                }
            )
        }
    }

    selectedDocument?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = { if (!busyDocument) selectedDocument = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(
                            "${formatDocumentBytes(doc.sizeBytes)} · ${doc.contentType.substringAfter('/') .uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { selectedDocument = null }) {
                        Icon(Icons.Outlined.Close, "Close")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DocumentActionIcon(
                        icon = { Icon(Icons.Outlined.Download, null, tint = Color(0xFF7652C8)) },
                        label = "Download",
                        enabled = !busyDocument && !doc.isDeleted,
                        onClick = {
                            scope.launch {
                                busyDocument = true
                                try {
                                    val url = repository.downloadUrl(vaultId, doc)
                                    enqueueDocumentDownload(context, url, doc.originalFileName)
                                    message = "Download started. Check your Downloads folder."
                                    selectedDocument = null
                                } catch (error: Exception) {
                                    message = error.localizedMessage ?: error.message ?: "Download failed."
                                } finally { busyDocument = false }
                            }
                        }
                    )
                    DocumentActionIcon(
                        icon = { Icon(Icons.Outlined.Edit, null, tint = Color(0xFF7652C8)) },
                        label = "Edit",
                        enabled = !busyDocument && !doc.isDeleted && doc.ownerUid == memberUid,
                        onClick = {
                            editTitle = doc.title
                            editDescription = doc.description
                            editCategory = doc.categoryId
                            editScope = doc.scope
                            editingDocument = doc
                            selectedDocument = null
                        }
                    )
                    DocumentActionIcon(
                        icon = { Icon(Icons.Outlined.Share, null, tint = Color(0xFF7652C8)) },
                        label = "Share",
                        enabled = !busyDocument && !doc.isDeleted,
                        onClick = { shareDocument(doc) }
                    )
                    DocumentActionIcon(
                        icon = { Icon(Icons.Outlined.Info, null, tint = Color(0xFF7652C8)) },
                        label = "Details",
                        enabled = !busyDocument,
                        onClick = { showDocumentDetails = true }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (!doc.isDeleted) {
                    if (doc.ownerUid == memberUid) {
                        ListItem(
                            headlineContent = { Text("Change Access") },
                            supportingContent = { Text(if (doc.scope == "SHARED") "Shared with household members" else "Only you can access this document") },
                            leadingContent = { Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                            modifier = Modifier.clickable {
                                editTitle = doc.title
                                editDescription = doc.description
                                editCategory = doc.categoryId
                                editScope = doc.scope
                                editingDocument = doc
                                selectedDocument = null
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Move to Folder") },
                            supportingContent = { Text("Organise this file inside a folder in the same category") },
                            leadingContent = { Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                            modifier = Modifier.clickable {
                                documentToMove = doc
                                moveDocumentTargetId = doc.folderId
                                moveDocumentTargetChosen = false
                                selectedDocument = null
                                showMoveDocument = true
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Move to Recycle Bin", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("Keep it recoverable for 30 days") },
                        leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable(enabled = !busyDocument && doc.ownerUid == memberUid) {
                            scope.launch {
                                busyDocument = true
                                try {
                                    repository.moveToRecycleBin(vaultId, doc)
                                    selectedDocument = null
                                    message = "Moved to Recycle Bin. Retained for 30 days."
                                } catch (error: Exception) {
                                    message = error.localizedMessage ?: error.message ?: "Delete failed."
                                } finally { busyDocument = false }
                            }
                        }
                    )
                } else if (doc.ownerUid == memberUid) {
                    ListItem(
                        headlineContent = { Text("Restore Document") },
                        leadingContent = { Icon(Icons.Outlined.Restore, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable(enabled = !busyDocument) {
                            scope.launch {
                                busyDocument = true
                                try {
                                    repository.restore(vaultId, doc.id)
                                    selectedDocument = null
                                    message = "Document restored."
                                } catch (error: Exception) {
                                    message = error.localizedMessage ?: error.message ?: "Restore failed."
                                } finally { busyDocument = false }
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("This action cannot be undone") },
                        leadingContent = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable(enabled = !busyDocument) {
                            permanentDeleteTarget = doc
                        }
                    )
                }
            }
        }
    }

    permanentDeleteTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { if (!busyDocument) permanentDeleteTarget = null },
            title = { Text("Permanently Delete Document?") },
            text = { Text("'"+doc.title+"' and its stored file will be deleted permanently. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !busyDocument,
                    onClick = {
                        scope.launch {
                            busyDocument = true
                            try {
                                repository.permanentlyDelete(vaultId, doc.id)
                                selectedDocument = null
                                permanentDeleteTarget = null
                                message = "Document permanently deleted."
                            } catch (error: Exception) {
                                message = error.localizedMessage ?: error.message ?: "Permanent deletion failed."
                            } finally {
                                busyDocument = false
                            }
                        }
                    }
                ) { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !busyDocument, onClick = { permanentDeleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDocumentDetails) {
        val doc = selectedDocument
        if (doc != null) {
            AlertDialog(
                onDismissRequest = { showDocumentDetails = false },
                title = { Text("File Details") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(doc.originalFileName, fontWeight = FontWeight.Bold)
                        HorizontalDivider()
                        ListItem(headlineContent = { Text("Location") },
                            supportingContent = { Text("Documents / ${allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other"}") },
                            leadingContent = { Icon(Icons.Outlined.FolderOpen, null) })
                        ListItem(headlineContent = { Text("Access") },
                            supportingContent = { Text(if (doc.scope == "SHARED") "Shared Vault" else "Personal Vault") },
                            leadingContent = { Icon(Icons.Outlined.Groups, null) })
                        ListItem(headlineContent = { Text("Added") },
                            supportingContent = { Text(doc.uploadedAt?.toDate()?.toString() ?: "—") },
                            leadingContent = { Icon(Icons.Outlined.CalendarToday, null) })
                        ListItem(headlineContent = { Text("Modified") },
                            supportingContent = { Text(doc.updatedAt?.toDate()?.toString() ?: "—") },
                            leadingContent = { Icon(Icons.Outlined.History, null) })
                        ListItem(headlineContent = { Text("Size") },
                            supportingContent = { Text(formatDocumentBytes(doc.sizeBytes)) },
                            leadingContent = { Icon(Icons.Outlined.Storage, null) })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDocumentDetails = false }) { Text("Close") }
                }
            )
        }
    }

    editingDocument?.let { doc ->
        AlertDialog(
            onDismissRequest = {
                if (!replacingDocument && !busyDocument) {
                    if (replacementTarget?.id == doc.id) {
                        pendingReplacement = null
                        replacementTarget = null
                        editTitle = doc.title
                    }
                    editingDocument = null
                }
            },
            title = { Text("Edit document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Document file", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(doc.originalFileName, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            Text(
                                pendingReplacement?.takeIf { replacementTarget?.id == doc.id }?.fileName
                                    ?.let { "Selected: $it" } ?: formatDocumentBytes(doc.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            enabled = !busyDocument && !replacingDocument,
                            onClick = {
                                replacementTarget = doc
                                VaultExternalActivityState.documentPickerActive = true
                                replacementPicker.launch(arrayOf("*/*"))
                            }
                        ) {
                            Icon(Icons.Outlined.Edit, null)
                            Text("Change File")
                        }
                    }
                    pendingReplacement?.takeIf { replacementTarget?.id == doc.id }?.let { selected ->
                        Text(
                            "Ready to upload: ${selected.fileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (replacingDocument) {
                            Text(
                                "${formatDocumentBytes((selected.sizeBytes * replacementProgress).toLong())} / ${formatDocumentBytes(selected.sizeBytes)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            LinearProgressIndicator(
                                progress = { replacementProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    OutlinedTextField(editTitle, { editTitle = it.take(50) }, label = { Text("File name (max 50)") }, singleLine = true)
                    OutlinedTextField(editDescription, { editDescription = it.take(50) },
                        label = { Text("Notes (max 50)") }, singleLine = true)
                    Text("Category")
                    allCategories.forEach { (id, name) ->
                        if (editCategory == id) Text("✓ $name")
                        else TextButton(onClick = { editCategory = id }) { Text(name) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(editScope == "PERSONAL", { editScope = "PERSONAL" }, label = { Text("Personal") })
                        FilterChip(editScope == "SHARED", { editScope = "SHARED" }, label = { Text("Shared") })
                    }
                    if (doc.folderId != null &&
                        (editCategory != doc.categoryId || editScope != doc.scope)
                    ) {
                        Text(
                            "Changing the category or access will move this document to the category root because folders keep a fixed category and visibility.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        busyDocument = true
                        try {
                            val selectedReplacement = pendingReplacement
                                ?.takeIf { replacementTarget?.id == doc.id }
                            if (selectedReplacement != null) {
                                replacingDocument = true
                                replacementProgress = 0f
                                repository.replaceFile(
                                    vaultId, doc, selectedReplacement.uri, context.contentResolver
                                ) { value -> replacementProgress = value }
                            }
                            repository.updateMetadata(
                                vaultId, doc, editTitle, editDescription, editCategory, editScope
                            )
                            if (selectedReplacement != null) {
                                message = "Document replaced and saved successfully."
                                pendingReplacement = null
                                replacementTarget = null
                            } else {
                                message = "Document details updated."
                            }
                            editingDocument = null
                        } catch (error: Exception) {
                            message = error.localizedMessage ?: error.message ?: "Save failed."
                        } finally {
                            replacingDocument = false
                            busyDocument = false
                        }
                    }
                }, enabled = !busyDocument && !replacingDocument) {
                    Text(if (replacingDocument) "Uploading…" else "Save")
                }
            },
            dismissButton = {
                TextButton(enabled = !busyDocument && !replacingDocument, onClick = {
                    if (replacementTarget?.id == doc.id) {
                        pendingReplacement = null
                        replacementTarget = null
                        editTitle = doc.title
                    }
                    editingDocument = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DocumentSummaryTile(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(9.dp)) {
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DocumentCategoryChip(label: String, id: String, selected: String, onSelect: (String) -> Unit) {
    FilterChip(selected == id, { onSelect(id) }, label = { Text(label) })
}

@Composable
private fun DocumentCardV2(
    document: DocumentRecordV2,
    modifier: Modifier,
    isRecycleBin: Boolean,
    currentMemberUid: String,
    categoryName: String,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentTypeIcon(document.contentType, Modifier.size(42.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    document.originalFileName.ifBlank { document.title }.take(50),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(formatDocumentBytes(document.sizeBytes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
                Text(
                    if (document.ownerUid == currentMemberUid) "Personal" else "Shared by Spouse",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                if (isRecycleBin) Text("Deleted", color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More document options")
            }
        }
    }
}

@Composable
private fun DocumentTypeIcon(contentType: String, modifier: Modifier = Modifier) {
    val (tile, tint, image) = when {
        contentType.contains("pdf", true) ->
            Triple(Color(0xFFFFE1E1), Color(0xFFE52222), Icons.Outlined.PictureAsPdf)
        contentType.startsWith("image/") ->
            Triple(Color(0xFFDCEBFF), Color(0xFF2377D8), Icons.Outlined.Image)
        contentType.contains("spreadsheet", true) || contentType.contains("excel", true) ||
            contentType.contains("sheet", true) ->
            Triple(Color(0xFFDDF4E5), Color(0xFF16834A), Icons.Outlined.Description)
        else ->
            Triple(Color(0xFFE9DFFF), Color(0xFF7652C8), Icons.Outlined.InsertDriveFile)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = tile) {
        Box(contentAlignment = Alignment.Center) {
            Icon(image, null, tint = tint, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun DocumentActionIcon(
    icon: @Composable () -> Unit,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(66.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF0E8FF)
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { icon() }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

private fun enqueueDocumentDownload(context: Context, url: String, originalName: String) {
    val base = originalName.substringBeforeLast('.', originalName).replace(Regex("[^A-Za-z0-9._ -]"), "_")
    val extension = originalName.substringAfterLast('.', "")
    val suffix = System.currentTimeMillis().toString()
    val safeName = if (extension.isBlank()) "$base ($suffix)" else "$base ($suffix).$extension"
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(originalName)
        .setDescription("Downloading document from Personal Vault")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}

private fun formatDocumentBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
