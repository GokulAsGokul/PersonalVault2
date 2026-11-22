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
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.File
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private enum class DocumentSortV2New { NAME_ASC, NAME_DESC, NEWEST, OLDEST, LARGEST, SMALLEST }

private data class PendingDocumentUploadV2New(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val title: String,
    val notes: String = "",
    val errorMessage: String? = null,
    val retryBlocked: Boolean = false
)

private data class PendingDocumentReplacementV2New(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long
)

private const val MAX_DOCUMENT_BATCH_COUNT_NEW = 10
private const val MAX_DOCUMENT_BATCH_BYTES_NEW = 200L * 1024L * 1024L
private const val MAX_DOCUMENT_FILE_BYTES_NEW = 50L * 1024L * 1024L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsManagerV2ScreenNew(vaultId: String, memberUid: String) {
    val context = LocalContext.current
    val isDarkUi = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = documentPaletteV2New(isDarkUi)
    val documentAccent = palette.accent
    val documentSurface = if (isDarkUi) Color(0xFF17151D) else MaterialTheme.colorScheme.surface
    val documentCardSurface = if (isDarkUi) Color(0xFF211E29) else MaterialTheme.colorScheme.surface
    val repository = remember { DocumentRepositoryV2() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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
    var sort by remember { mutableStateOf(DocumentSortV2New.NEWEST) }
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
    val pendingUploads = remember { mutableStateListOf<PendingDocumentUploadV2New>() }
    val uploadFormScrollState = rememberScrollState()
    var showAddDocuments by remember { mutableStateOf(false) }
    var showAddOptions by remember { mutableStateOf(false) }
    var showDocumentDetails by remember { mutableStateOf(false) }
    var uploadCategory by remember { mutableStateOf("other") }
    var uploadScope by remember { mutableStateOf("PERSONAL") }
    var uploadCategoryMenu by remember { mutableStateOf(false) }
    var uploadFolderId by remember { mutableStateOf<String?>(null) }
    var uploadFolderMenu by remember { mutableStateOf(false) }
    var replacementTarget by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var pendingReplacement by remember { mutableStateOf<PendingDocumentReplacementV2New?>(null) }
    var replacingDocument by remember { mutableStateOf(false) }
    var replacementProgress by remember { mutableFloatStateOf(0f) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingScanFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPreview by remember { mutableStateOf(false) }

    LaunchedEffect(showAddDocuments) {
        if (showAddDocuments) uploadFormScrollState.scrollTo(0)
    }

    // Status messages are shown as snackbars. While an upload is running the
    // progress banner at the top of the list shows the live message instead.
    LaunchedEffect(message, uploading) {
        if (message.isNotBlank() && !uploading) {
            snackbarHostState.showSnackbar(message)
            message = ""
        }
    }

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
            if (uris.size > MAX_DOCUMENT_BATCH_COUNT_NEW) {
                message = "Select up to $MAX_DOCUMENT_BATCH_COUNT_NEW files per batch. You selected ${uris.size}."
                return@rememberLauncherForActivityResult
            }
            val selectedFiles = uris.map { uri ->
                // OpenMultipleDocuments uses ACTION_OPEN_DOCUMENT; retain read access so a
                // queued upload can still read the selected URI after activity recreation.
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val fileName = runCatching {
                    repository.readDisplayName(context.contentResolver, uri)
                }.getOrDefault("document")
                val sizeBytes = repository.readSizeBytes(context.contentResolver, uri)
                PendingDocumentUploadV2New(
                    uri = uri,
                    fileName = fileName,
                    sizeBytes = sizeBytes,
                    title = fileName.substringBeforeLast('.', fileName).take(50)
                )
            }
            val oversized = selectedFiles.filter { it.sizeBytes > MAX_DOCUMENT_FILE_BYTES_NEW }
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
            if (totalBytes > MAX_DOCUMENT_BATCH_BYTES_NEW) {
                message = "The selected files total ${formatDocumentBytesNew(totalBytes)}. The batch limit is 200 MB."
                return@rememberLauncherForActivityResult
            }
            pendingUploads.clear()
            pendingUploads.addAll(selectedFiles)
            pendingPhotoFile?.delete()
            pendingPhotoFile = null
            pendingPhotoUri = null
            capturedPhotoUri = null
            pendingScanFile?.delete()
            pendingScanFile = null
            val parentFolder = folders.firstOrNull { it.id == currentFolderId }
            uploadCategory = parentFolder?.categoryId ?: if (category == "all") "other" else category
            uploadScope = parentFolder?.scope ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
            uploadFolderId = parentFolder?.id
            showAddDocuments = true
        } else {
            message = "No files selected."
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(MAX_DOCUMENT_BATCH_COUNT_NEW)
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
                scope.launch(Dispatchers.IO) {
                    val timestamp = System.currentTimeMillis()
                    val fileName = "Scanned_Document_${timestamp}.pdf"
                    val directory = File(context.cacheDir, "documents").apply { mkdirs() }
                    val scannedFile = File(directory, fileName)
                    try {
                        val input = context.contentResolver.openInputStream(pdfUri)
                            ?: throw IllegalStateException("Unable to read the scanned PDF.")
                        input.use { source ->
                            scannedFile.outputStream().use { output -> source.copyTo(output) }
                        }
                        val sizeBytes = scannedFile.length()
                        withContext(Dispatchers.Main) {
                            if (sizeBytes in 1..MAX_DOCUMENT_FILE_BYTES_NEW) {
                                val cachedUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    scannedFile
                                )
                                pendingScanFile?.delete()
                                pendingScanFile = scannedFile
                                pendingUploads.clear()
                                pendingUploads.add(
                                    PendingDocumentUploadV2New(
                                        uri = cachedUri,
                                        fileName = fileName,
                                        sizeBytes = sizeBytes,
                                        title = "Scanned Document ${timestamp}".take(50),
                                        notes = if (pageCount > 1) "${pageCount} pages scanned" else ""
                                    )
                                )
                                val parentFolder = folders.firstOrNull { it.id == currentFolderId }
                                uploadCategory = parentFolder?.categoryId
                                    ?: if (category == "all") "other" else category
                                uploadScope = parentFolder?.scope
                                    ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
                                uploadFolderId = parentFolder?.id
                                showAddDocuments = true
                            } else {
                                scannedFile.delete()
                                message = "The scanned PDF must be between 1 byte and 50 MB."
                            }
                        }
                    } catch (error: Exception) {
                        scannedFile.delete()
                        withContext(Dispatchers.Main) {
                            message = error.localizedMessage ?: "Unable to prepare the scanned PDF."
                        }
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
            capturedPhotoUri = null
            showPhotoPreview = false
            message = if (success) "The camera did not save a photo. Please try again." else "Photo capture cancelled."
        }
    }

    val replacementPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        VaultExternalActivityState.documentPickerActive = false
        if (uri != null) {
            // OpenDocument grants persistable read access for a replacement queued in the editor.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val fileName = runCatching {
                repository.readDisplayName(context.contentResolver, uri)
            }.getOrDefault("document")
            val sizeBytes = repository.readSizeBytes(context.contentResolver, uri)
            when {
                sizeBytes !in 1..MAX_DOCUMENT_FILE_BYTES_NEW ->
                    message = "The replacement file must be between 1 byte and 50 MB."
                fileName.substringAfterLast('.', "").isBlank() ->
                    message = "Choose a file with a valid extension."
                else -> {
                    pendingReplacement = PendingDocumentReplacementV2New(uri, fileName, sizeBytes)
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
            pendingPhotoFile?.delete()
            pendingPhotoFile = null
            pendingPhotoUri = null
            capturedPhotoUri = null
            showPhotoPreview = false
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
        if (sizeBytes !in 1..MAX_DOCUMENT_FILE_BYTES_NEW) {
            message = "The photo must be between 1 byte and 50 MB."
            return
        }
        val fileName = photoFile.name
        pendingUploads.clear()
        pendingUploads.add(
            PendingDocumentUploadV2New(
                uri = photoUri,
                fileName = fileName,
                sizeBytes = sizeBytes,
                title = fileName.substringBeforeLast('.').take(50)
            )
        )
        val parentFolder = folders.firstOrNull { it.id == currentFolderId }
        uploadCategory = parentFolder?.categoryId ?: if (category == "all") "other" else category
        uploadScope = parentFolder?.scope ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
        uploadFolderId = parentFolder?.id
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
                DocumentSortV2New.NAME_ASC -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
                DocumentSortV2New.NAME_DESC -> list.sortedByDescending { it.title.lowercase(Locale.ROOT) }
                DocumentSortV2New.NEWEST -> list.sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }
                DocumentSortV2New.OLDEST -> list.sortedBy { it.uploadedAt?.toDate()?.time ?: 0L }
                DocumentSortV2New.LARGEST -> list.sortedByDescending { it.sizeBytes }
                DocumentSortV2New.SMALLEST -> list.sortedBy { it.sizeBytes }
            }
        }

    val bytes = active.sumOf { it.sizeBytes }
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

    // ───────── Redesign: navigation state helpers ─────────
    val isHome = !recycleBin && category == "all" && currentFolderId == null && !showAllDocuments
    val inCategory = !recycleBin && category != "all"
    val canGoBack = !isHome

    fun goBack() {
        when {
            recycleBin -> {
                recycleBin = false
                category = "all"
                currentFolderId = null
                showAllDocuments = false
            }
            currentFolderId != null ->
                currentFolderId = folders.firstOrNull { it.id == currentFolderId }?.parentFolderId
            category != "all" -> {
                category = "all"
                fileType = "ALL"
            }
            showAllDocuments -> showAllDocuments = false
        }
    }
    BackHandler(enabled = canGoBack) { goBack() }

    fun startCreateFolder() {
        newFolderName = ""
        newFolderScope = currentFolderId?.let { id ->
            folders.firstOrNull { it.id == id }?.scope
        } ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
        showCreateFolder = true
    }

    val categoryTitle = allCategories.firstOrNull { it.first == category }?.second ?: "Documents"
    val childFolders = folders.filter {
        it.parentFolderId == currentFolderId && (visibility == "ALL" || it.scope == visibility)
    }.sortedBy { it.name.lowercase(Locale.ROOT) }
    val headerTitle = when {
        recycleBin -> "Recycle Bin"
        category != "all" -> categoryTitle
        showAllDocuments -> "All files"
        else -> "Documents"
    }
    val headerSubtitle = when {
        recycleBin -> "Deleted files are kept for 30 days"
        category != "all" ->
            "${folders.size} folders, ${active.count { it.categoryId == category }} files"
        else -> "${active.size} files, ${formatDocumentBytesNew(bytes)}"
    }
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val mutedColor = palette.muted

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!recycleBin) ExtendedFloatingActionButton(
                onClick = { showAddOptions = true },
                expanded = fabExpanded,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Add") },
                containerColor = documentAccent,
                contentColor = palette.onAccent,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Header ──
            item(key = "header") {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (canGoBack) {
                        IconButton(onClick = { goBack() }) {
                            Icon(Icons.Outlined.ChevronRight, "Back", Modifier.rotate(180f))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = if (canGoBack) 0.dp else 4.dp)) {
                        Text(
                            headerTitle,
                            style = if (isHome) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(headerSubtitle, style = MaterialTheme.typography.bodySmall, color = mutedColor)
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (recycleBin) "All Documents" else "Recycle Bin") },
                                leadingIcon = { Icon(if (recycleBin) Icons.Outlined.Description else Icons.Outlined.Delete, null) },
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
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                onClick = { manageCategories = true; menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("New Category") },
                                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                                onClick = { newCategory = ""; categoryDialog = true; menuExpanded = false }
                            )
                        }
                    }
                }
            }

            // ── Upload progress banner (replaces the old inline progress) ──
            if (uploading) {
                item(key = "uploading") {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.container)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(message, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth(), color = documentAccent)
                        }
                    }
                }
            }

            // ── Search ──
            item(key = "search") {
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search documents") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    )
                )
            }

            // ── Scope switch ──
            item(key = "scope") {
                val scopes = listOf("ALL" to "All", "PERSONAL" to "Personal", "SHARED" to "Shared")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    scopes.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = visibility == value,
                            onClick = {
                                visibility = value
                                if (!recycleBin) {
                                    category = "all"
                                    currentFolderId = null
                                    showAllDocuments = false
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = scopes.size),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = documentAccent,
                                activeContentColor = palette.onAccent,
                                inactiveContainerColor = palette.container,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text(label) }
                    }
                }
            }

            if (isHome) {
                // ── Home: Recent + Categories ──
                val recent = active.filter { visibility == "ALL" || it.scope == visibility }
                    .sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }.take(3)
                item(key = "recent_header") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAllDocuments = true }) { Text("See all", color = documentAccent) }
                    }
                }
                if (recent.isEmpty()) {
                    item(key = "recent_empty") {
                        EmptyStateV2New(palette, "No documents yet", "Tap Add to upload, scan or photograph your first file.")
                    }
                } else {
                    items(recent, key = { "recent-${it.id}" }) { doc ->
                        DocumentRowV2New(
                            doc, false, memberUid,
                            onClick = { openDocument(doc) },
                            onMore = { selectedDocument = doc }
                        )
                    }
                }
                item(key = "cat_header") {
                    Text(
                        "Categories",
                        Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(allCategories.chunked(2)) { categoryRow ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryRow.forEach { (id, name) ->
                            val count = active.count {
                                it.categoryId == id && (visibility == "ALL" || it.scope == visibility)
                            }
                            val (tileBg, tileFg) = categoryStyleV2New(id, isDarkUi)
                            CategoryCardV2New(
                                name = name,
                                icon = categoryIconV2New(id),
                                count = count,
                                tileBg = tileBg,
                                tileFg = tileFg,
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    category = id
                                    currentFolderId = null
                                    showAllDocuments = false
                                }
                            )
                        }
                        repeat(2 - categoryRow.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item(key = "recycle_row") {
                    Card(
                        Modifier.fillMaxWidth().padding(top = 4.dp).clickable {
                            recycleBin = true
                            category = "all"
                            showAllDocuments = false
                            currentFolderId = null
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.surface)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Delete, null, tint = mutedColor)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Recycle Bin", fontWeight = FontWeight.SemiBold)
                                Text("${deleted.size} deleted files", style = MaterialTheme.typography.bodySmall, color = mutedColor)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = mutedColor)
                        }
                    }
                }
            } else {
                // ── Category / folder / all / recycle bin ──
                if (inCategory) {
                    val folderPath = mutableListOf<DocumentFolderV2>()
                    var folderCursor = currentFolderId
                    val visitedFolderIds = mutableSetOf<String>()
                    while (folderCursor != null && visitedFolderIds.add(folderCursor)) {
                        val pathFolder = folders.firstOrNull { it.id == folderCursor } ?: break
                        folderPath.add(0, pathFolder)
                        folderCursor = pathFolder.parentFolderId
                    }
                    if (folderPath.isNotEmpty()) {
                        item(key = "breadcrumbs") {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = { currentFolderId = null },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) { Text(categoryTitle, color = documentAccent) }
                                folderPath.forEach { crumb ->
                                    Text("›", color = mutedColor)
                                    TextButton(
                                        onClick = { currentFolderId = crumb.id },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            crumb.name,
                                            color = if (crumb.id == currentFolderId) documentAccent
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "folders_header") {
                        Text(
                            "Folders",
                            Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val folderSlots: List<DocumentFolderV2?> = childFolders + listOf<DocumentFolderV2?>(null)
                    items(folderSlots.chunked(2)) { slotRow ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            slotRow.forEach { folder ->
                                if (folder == null) {
                                    NewFolderCardV2New(palette, Modifier.weight(1f)) { startCreateFolder() }
                                } else {
                                    FolderCardV2New(
                                        folder = folder,
                                        isOwner = folder.ownerUid == memberUid,
                                        palette = palette,
                                        modifier = Modifier.weight(1f),
                                        onOpen = { currentFolderId = folder.id },
                                        onMore = { selectedFolder = folder; showFolderActions = true }
                                    )
                                }
                            }