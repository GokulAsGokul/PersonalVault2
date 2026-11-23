package com.gokul.personalvault.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.content.ClipData
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Deselect
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
import com.gokul.personalvault.DocumentUploadForegroundService
import com.gokul.personalvault.data.firebase.DocumentRepositoryV2
import com.gokul.personalvault.ui.components.VaultSnackbarHost
import com.gokul.personalvault.ui.components.classifyVaultSnackbar
import com.gokul.personalvault.ui.components.VaultSnackbarController
import com.gokul.personalvault.ui.components.VaultSnackbarType
import com.gokul.personalvault.ui.components.triggerVaultErrorVibration
import com.gokul.personalvault.data.model.BuiltInDocumentCategoriesV2
import com.gokul.personalvault.data.model.DocumentRecordV2
import com.gokul.personalvault.data.model.DocumentFolderV2
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CancellationException
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
    val categoryId: String = "other",
    val scope: String = "PERSONAL",
    val folderId: String? = null,
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
    var sharedDocumentsLoaded by remember { mutableStateOf(false) }
    var personalDocumentsLoaded by remember { mutableStateOf(false) }
    var sharedPageDocuments by remember { mutableStateOf<Map<String, DocumentRecordV2>>(emptyMap()) }
    var personalPageDocuments by remember { mutableStateOf<Map<String, DocumentRecordV2>>(emptyMap()) }
    var sharedLastDocument by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var personalLastDocument by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var sharedHasMore by remember { mutableStateOf(false) }
    var personalHasMore by remember { mutableStateOf(false) }
    var sharedHasLoadedMore by remember { mutableStateOf(false) }
    var personalHasLoadedMore by remember { mutableStateOf(false) }
    var loadingMoreDocuments by remember { mutableStateOf(false) }
    val documentsLoaded = sharedDocumentsLoaded && personalDocumentsLoaded
    var folders by remember { mutableStateOf<List<DocumentFolderV2>>(emptyList()) }
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var createFolderError by remember { mutableStateOf<String?>(null) }
    var newFolderCategory by remember { mutableStateOf("other") }
    var folderCategoryMenuExpanded by remember { mutableStateOf(false) }
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
    var deleteFolderError by remember { mutableStateOf<String?>(null) }
    var permanentDeleteTarget by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var showEmptyRecycleBinConfirm by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var advancedSearchExpanded by remember { mutableStateOf(false) }
    var searchFileNameOnly by remember { mutableStateOf(false) }
    var showFavouritesOnly by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedDocumentIds = remember { mutableStateListOf<String>() }
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
    var isDownloadingFile by remember { mutableStateOf(false) }
    var downloadingFileName by remember { mutableStateOf("") }
    var downloadTransferredBytes by remember { mutableLongStateOf(0L) }
    var downloadTotalBytes by remember { mutableLongStateOf(0L) }
    var isSavingDownloadedFile by remember { mutableStateOf(false) }
    var showDocumentPreview by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var previewFileDocumentId by remember { mutableStateOf<String?>(null) }
    var previewTitle by remember { mutableStateOf("") }
    var previewPageIndex by remember { mutableIntStateOf(0) }
    var previewPageCount by remember { mutableIntStateOf(1) }
    val pendingUploads = remember { mutableStateListOf<PendingDocumentUploadV2New>() }
    val uploadFormScrollState = rememberScrollState()
    var showAddDocuments by remember { mutableStateOf(false) }
    var showAddOptions by remember { mutableStateOf(false) }
    var showDocumentDetails by remember { mutableStateOf(false) }
    var replacementTarget by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var pendingReplacement by remember { mutableStateOf<PendingDocumentReplacementV2New?>(null) }
    var replacingDocument by remember { mutableStateOf(false) }
    var replacementProgress by remember { mutableFloatStateOf(0f) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingScanFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPreview by remember { mutableStateOf(false) }

    LaunchedEffect(vaultId) {
        withContext(Dispatchers.IO) {
            repository.cleanupDocumentCache(context.cacheDir)
        }
    }

    LaunchedEffect(showAddDocuments) {
        if (showAddDocuments) uploadFormScrollState.scrollTo(0)
    }

    // Status messages are shown as snackbars. While an upload is running the
    // progress banner at the top of the list shows the live message instead.
    LaunchedEffect(message, uploading) {
        if (message.isNotBlank() && !uploading) {
            VaultSnackbarController.show(message, classifyVaultSnackbar(message))
            message = ""
        }
    }

    DisposableEffect(vaultId, memberUid) {
        documents = emptyList()
        sharedDocumentsLoaded = false
        personalDocumentsLoaded = false
        loadingMoreDocuments = false
        var sharedFirstPageIds = mutableSetOf<String>()
        var personalFirstPageIds = mutableSetOf<String>()
        sharedPageDocuments = emptyMap()
        personalPageDocuments = emptyMap()
        sharedLastDocument = null
        personalLastDocument = null
        sharedHasMore = false
        personalHasMore = false
        sharedHasLoadedMore = false
        personalHasLoadedMore = false
        fun publishDocuments() {
            documents = (sharedPageDocuments.values + personalPageDocuments.values).distinctBy { it.id }
        }
        val sharedListener: ListenerRegistration = repository.sharedDocumentsPageQuery(vaultId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val updated = sharedPageDocuments.toMutableMap()
                    sharedFirstPageIds.forEach(updated::remove)
                    sharedFirstPageIds.clear()
                    snapshot.documents.forEach { doc ->
                        doc.data?.let {
                            updated[doc.id] = DocumentRecordV2.fromSnapshot(doc.id, it)
                            sharedFirstPageIds.add(doc.id)
                        }
                    }
                    sharedPageDocuments = updated
                    if (!sharedHasLoadedMore) {
                        sharedLastDocument = snapshot.documents.lastOrNull()
                        sharedHasMore = snapshot.documents.size.toLong() == DocumentRepositoryV2.DOCUMENT_PAGE_SIZE
                    }
                    publishDocuments()
                    sharedDocumentsLoaded = true
                } else if (error != null) {
                    sharedDocumentsLoaded = true
                    message = "Shared documents: ${error.localizedMessage ?: "Unable to load documents."}"
                }
            }
        val personalListener: ListenerRegistration = repository.personalDocumentsPageQuery(vaultId, memberUid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val updated = personalPageDocuments.toMutableMap()
                    personalFirstPageIds.forEach(updated::remove)
                    personalFirstPageIds.clear()
                    snapshot.documents.forEach { doc ->
                        doc.data?.let {
                            updated[doc.id] = DocumentRecordV2.fromSnapshot(doc.id, it)
                            personalFirstPageIds.add(doc.id)
                        }
                    }
                    personalPageDocuments = updated
                    if (!personalHasLoadedMore) {
                        personalLastDocument = snapshot.documents.lastOrNull()
                        personalHasMore = snapshot.documents.size.toLong() == DocumentRepositoryV2.DOCUMENT_PAGE_SIZE
                    }
                    publishDocuments()
                    personalDocumentsLoaded = true
                } else if (error != null) {
                    personalDocumentsLoaded = true
                    message = "Personal documents: ${error.localizedMessage ?: "Unable to load documents."}"
                }
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
            val sharedFolders = mutableMapOf<String, DocumentFolderV2>()
            val personalFolders = mutableMapOf<String, DocumentFolderV2>()
            fun publishFolders() {
                folders = (sharedFolders.values + personalFolders.values).distinctBy { it.id }
            }
            val sharedFolderListener = repository.folderCollection(vaultId)
                .whereEqualTo("scope", "SHARED")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        sharedFolders.clear()
                        snapshot.documents.forEach { doc ->
                            doc.data?.let { sharedFolders[doc.id] = DocumentFolderV2.fromSnapshot(doc.id, it) }
                        }
                        publishFolders()
                    } else if (error != null) message = "Shared folders: ${error.localizedMessage ?: "Unable to load folders."}"
                }
            val personalFolderListener = repository.folderCollection(vaultId)
                .whereEqualTo("scope", "PERSONAL")
                .whereEqualTo("ownerUid", memberUid)
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
            val parentFolder = folders.firstOrNull { it.id == currentFolderId }
            val defaultCategory = parentFolder?.categoryId ?: if (category == "all") "other" else category
            val defaultScope = parentFolder?.scope ?: if (visibility == "SHARED") "SHARED" else "PERSONAL"
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
                    title = fileName.substringBeforeLast('.', fileName).take(50),
                    categoryId = defaultCategory,
                    scope = defaultScope,
                    folderId = parentFolder?.id
                )
            }
            val unsupported = selectedFiles.filterNot { repository.isSupportedFileName(it.fileName) }
            if (unsupported.isNotEmpty()) {
                message = "Unsupported file type: " + unsupported.joinToString { it.fileName } +
                    ". Supported formats: PDF, JPG, PNG, DOC, DOCX, XLS, XLSX, PPT, PPTX and TXT."
                return@rememberLauncherForActivityResult
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
                                        notes = if (pageCount > 1) "${pageCount} pages scanned" else "",
                                        categoryId = folders.firstOrNull { it.id == currentFolderId }?.categoryId
                                            ?: if (category == "all") "other" else category,
                                        scope = folders.firstOrNull { it.id == currentFolderId }?.scope
                                            ?: if (visibility == "SHARED") "SHARED" else "PERSONAL",
                                        folderId = currentFolderId
                                    )
                                )
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
                title = fileName.substringBeforeLast('.').take(50),
                categoryId = folders.firstOrNull { it.id == currentFolderId }?.categoryId
                    ?: if (category == "all") "other" else category,
                scope = folders.firstOrNull { it.id == currentFolderId }?.scope
                    ?: if (visibility == "SHARED") "SHARED" else "PERSONAL",
                folderId = currentFolderId
            )
        )
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
            q.isEmpty() || if (searchFileNameOnly) {
                it.originalFileName.lowercase(Locale.ROOT).contains(q)
            } else {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                    it.originalFileName.lowercase(Locale.ROOT).contains(q) ||
                    it.description.lowercase(Locale.ROOT).contains(q) ||
                    it.categoryId.replace('_', ' ').lowercase(Locale.ROOT).contains(q)
            }
        }
        .filter { !showFavouritesOnly || it.favouriteUids[memberUid] == true }
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

    fun downloadDocument(doc: DocumentRecordV2) {
        scope.launch {
            busyDocument = true
            isDownloadingFile = true
            downloadingFileName = doc.title.ifBlank { doc.originalFileName }
            downloadTransferredBytes = 0L
            downloadTotalBytes = doc.sizeBytes.coerceAtLeast(0L)
            try {
                val file = repository.downloadToCache(context.cacheDir, vaultId, doc) { transferred, total ->
                    downloadTransferredBytes = transferred.coerceAtLeast(0L)
                    if (total > 0L) downloadTotalBytes = total
                }
                isSavingDownloadedFile = true
                withContext(Dispatchers.IO) {
                    saveDocumentToDownloadsNew(
                        context = context,
                        source = file,
                        displayName = doc.originalFileName.ifBlank { doc.title },
                        mimeType = doc.contentType
                    )
                }
                message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "Downloaded to Downloads/Personal Vault."
                } else {
                    "Downloaded to the app's Downloads folder."
                }
                selectedDocument = null
            } catch (error: Exception) {
                message = error.localizedMessage ?: error.message ?: "Download failed."
            } finally {
                busyDocument = false
                isSavingDownloadedFile = false
                isDownloadingFile = false
            }
        }
    }

    fun loadMoreDocuments() {
        if (loadingMoreDocuments) return
        scope.launch {
            loadingMoreDocuments = true
            try {
                if (sharedHasMore) {
                    val page = repository.sharedDocumentsPageQuery(vaultId, sharedLastDocument).get().await()
                    val updated = sharedPageDocuments.toMutableMap()
                    page.documents.forEach { doc ->
                        doc.data?.let { updated[doc.id] = DocumentRecordV2.fromSnapshot(doc.id, it) }
                    }
                    sharedPageDocuments = updated
                    documents = (updated.values + personalPageDocuments.values).distinctBy { it.id }
                    sharedLastDocument = page.documents.lastOrNull() ?: sharedLastDocument
                    sharedHasLoadedMore = true
                    sharedHasMore = page.documents.size.toLong() == DocumentRepositoryV2.DOCUMENT_PAGE_SIZE
                }
                if (personalHasMore) {
                    val page = repository.personalDocumentsPageQuery(vaultId, memberUid, personalLastDocument).get().await()
                    val updated = personalPageDocuments.toMutableMap()
                    page.documents.forEach { doc ->
                        doc.data?.let { updated[doc.id] = DocumentRecordV2.fromSnapshot(doc.id, it) }
                    }
                    personalPageDocuments = updated
                    documents = (sharedPageDocuments.values + updated.values).distinctBy { it.id }
                    personalLastDocument = page.documents.lastOrNull() ?: personalLastDocument
                    personalHasLoadedMore = true
                    personalHasMore = page.documents.size.toLong() == DocumentRepositoryV2.DOCUMENT_PAGE_SIZE
                }
                documents = (sharedPageDocuments.values + personalPageDocuments.values)
                    .distinctBy { it.id }
            } catch (error: Exception) {
                message = error.localizedMessage ?: error.message ?: "Unable to load more documents."
            } finally {
                loadingMoreDocuments = false
            }
        }
    }

    fun openDocument(doc: DocumentRecordV2) {
        if (doc.isDeleted) {
            selectedDocument = doc
            return
        }
        scope.launch {
            busyDocument = true
            isDownloadingFile = true
            downloadingFileName = doc.title.ifBlank { doc.originalFileName }
            downloadTransferredBytes = 0L
            downloadTotalBytes = doc.sizeBytes.coerceAtLeast(0L)
            try {
                val file = repository.downloadToCache(context.cacheDir, vaultId, doc) { transferred, total ->
                    downloadTransferredBytes = transferred.coerceAtLeast(0L)
                    if (total > 0L) downloadTotalBytes = total
                }
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
                VaultExternalActivityState.documentViewerActive = true
                context.startActivity(Intent.createChooser(viewIntent, "Open document"))
            } catch (error: Exception) {
                VaultExternalActivityState.documentViewerActive = false
                message = error.localizedMessage ?: error.message ?: "Unable to open document."
            } finally {
                busyDocument = false
                isDownloadingFile = false
            }
        }
    }

    suspend fun renderPreviewPage(file: File, doc: DocumentRecordV2, pageIndex: Int): Pair<androidx.compose.ui.graphics.ImageBitmap, Int> =
        withContext(Dispatchers.IO) {
            if (doc.contentType.startsWith("image/")) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val sampleSize = maxOf(
                    1,
                    (bounds.outWidth / 1600).coerceAtLeast(1),
                    (bounds.outHeight / 2000).coerceAtLeast(1)
                )
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    ?: throw IllegalStateException("This image cannot be previewed.")
                bitmap.asImageBitmap() to 1
            } else if (doc.contentType.contains("pdf", ignoreCase = true) ||
                doc.originalFileName.endsWith(".pdf", ignoreCase = true)
            ) {
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(descriptor).use { renderer ->
                    require(renderer.pageCount > 0) { "This PDF has no pages." }
                    val index = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(index).use { page ->
                        val scale = (1400f / page.width).coerceAtMost(2f)
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap.asImageBitmap() to renderer.pageCount
                    }
                }
            } else {
                throw IllegalArgumentException("Preview is available for PDF and image files. Use Open for this file type.")
            }
        }

    fun previewDocument(doc: DocumentRecordV2, pageIndex: Int = 0) {
        scope.launch {
            busyDocument = true
            val cachedPreview = previewFile?.takeIf { previewFileDocumentId == doc.id }
            isDownloadingFile = cachedPreview == null
            downloadingFileName = doc.title.ifBlank { doc.originalFileName }
            downloadTransferredBytes = 0L
            downloadTotalBytes = doc.sizeBytes.coerceAtLeast(0L)
            try {
                val file = cachedPreview
                    ?: repository.downloadToCache(context.cacheDir, vaultId, doc) { transferred, total ->
                    downloadTransferredBytes = transferred.coerceAtLeast(0L)
                    if (total > 0L) downloadTotalBytes = total
                }
                val (bitmap, count) = renderPreviewPage(file, doc, pageIndex)
                previewFile = file
                previewFileDocumentId = doc.id
                previewBitmap = bitmap
                previewPageCount = count
                previewPageIndex = pageIndex.coerceIn(0, count - 1)
                previewTitle = doc.title.ifBlank { doc.originalFileName }
                showDocumentPreview = true
            } catch (error: Exception) {
                message = error.localizedMessage ?: error.message ?: "Unable to preview this document."
            } finally {
                isDownloadingFile = false
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

    fun toggleFavourite(doc: DocumentRecordV2) {
        scope.launch {
            try {
                val wasFavourite = doc.favouriteUids[memberUid] == true
                repository.setFavourite(vaultId, doc.id, memberUid, !wasFavourite)
                message = if (wasFavourite) "Removed from Favourites." else "Added to Favourites."
            } catch (error: Exception) {
                message = error.localizedMessage ?: "Unable to update Favourite."
            }
        }
    }

    fun toggleSelection(doc: DocumentRecordV2) {
        if (doc.id in selectedDocumentIds) selectedDocumentIds.remove(doc.id)
        else selectedDocumentIds.add(doc.id)
        if (selectedDocumentIds.isEmpty()) selectionMode = false
    }

    fun runBulkFavourite() {
        val targets = visible.filter { it.id in selectedDocumentIds }
        scope.launch {
            busyDocument = true
            var succeeded = 0
            try {
                targets.forEach { doc ->
                    repository.setFavourite(vaultId, doc.id, memberUid, doc.favouriteUids[memberUid] != true)
                    succeeded++
                }
                message = "$succeeded document(s) updated in Favourites."
            } catch (error: Exception) {
                message = "$succeeded updated. ${error.localizedMessage ?: "Some favourites could not be updated."}"
            } finally {
                selectedDocumentIds.clear()
                selectionMode = false
                busyDocument = false
            }
        }
    }

    fun runBulkRestore() {
        val targets = visible.filter { it.id in selectedDocumentIds && it.ownerUid == memberUid }
        val skipped = selectedDocumentIds.size - targets.size
        scope.launch {
            busyDocument = true
            var succeeded = 0
            try {
                targets.forEach { doc ->
                    repository.restore(vaultId, doc.id)
                    succeeded++
                }
                message = "$succeeded document(s) restored." +
                    if (skipped > 0) " $skipped skipped: only the owner can restore a document." else ""
            } catch (error: Exception) {
                message = "$succeeded restored. ${error.localizedMessage ?: "Some documents could not be restored."}"
            } finally {
                selectedDocumentIds.clear()
                selectionMode = false
                busyDocument = false
            }
        }
    }

    fun runBulkDelete() {
        val targets = visible.filter { it.id in selectedDocumentIds && it.ownerUid == memberUid }
        val skipped = selectedDocumentIds.size - targets.size
        scope.launch {
            busyDocument = true
            var succeeded = 0
            try {
                targets.forEach { doc ->
                    repository.moveToRecycleBin(vaultId, doc)
                    succeeded++
                }
                message = "$succeeded document(s) moved to Recycle Bin." +
                    if (skipped > 0) " $skipped skipped: only the owner can delete a document." else ""
            } catch (error: Exception) {
                message = "$succeeded moved. ${error.localizedMessage ?: "Some documents could not be deleted."}"
            } finally {
                selectedDocumentIds.clear()
                selectionMode = false
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
        createFolderError = null
        newFolderCategory = if (category == "all") "other" else category
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { advancedSearchExpanded = !advancedSearchExpanded }) {
                                Icon(Icons.Outlined.Tune, "Advanced search")
                            }
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { search = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
                            }
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

            if (advancedSearchExpanded) {
                item(key = "advanced_search_options") {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.surface)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Advanced Search", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = searchFileNameOnly, onCheckedChange = { searchFileNameOnly = it })
                                Text("Search filename only", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Search matches title, filename, notes and category.",
                                color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
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

            if (!documentsLoaded) {
                item(key = "documents_initial_loading") {
                    Card(
                        Modifier.fillMaxWidth().padding(top = 36.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.surface)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = documentAccent)
                            Text("Loading documents…", fontWeight = FontWeight.SemiBold)
                            Text("Your personal and shared files will appear here.",
                                color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (isHome) {
                // ── Home: summary, recent documents and categories ──
                val recent = active.filter { visibility == "ALL" || it.scope == visibility }
                    .sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }.take(3)
                item(key = "document_summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DocumentSummaryTileV2New("Documents", active.size.toString(), Icons.Outlined.Description, palette, Modifier.weight(1f))
                            DocumentSummaryTileV2New("Storage", formatDocumentBytesNew(bytes), Icons.Outlined.Storage, palette, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DocumentSummaryTileV2New(
                                "Personal", active.count { it.scope == "PERSONAL" }.toString(),
                                Icons.Outlined.Person, palette, Modifier.weight(1f), isShared = false
                            )
                            DocumentSummaryTileV2New(
                                "Shared", active.count { it.scope == "SHARED" }.toString(),
                                Icons.Outlined.Groups, palette, Modifier.weight(1f), isShared = true
                            )
                        }
                    }
                }
                item(key = "recent_header") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAllDocuments = true }) { Text("See all", color = documentAccent) }
                    }
                }
                if (recent.isEmpty()) {
                    item(key = "recent_empty") {
                        EmptyStateV2New(
                            palette,
                            "No documents yet",
                            "Upload documents, scan a paper or capture a photo to get started.",
                            actionLabel = "Add Documents",
                            onAction = { showAddOptions = true }
                        )
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
                items(allCategories.chunked(3)) { categoryRow ->
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
                        repeat(3 - categoryRow.size) { Spacer(Modifier.weight(1f)) }
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
                            repeat(2 - slotRow.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                item(key = "files_header") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    recycleBin -> "Deleted Documents"
                                    category == "all" -> "All files"
                                    else -> "Files"
                                },
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                            )
                            Text("${visible.size} files", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                        }
                        Box {
                            TextButton(onClick = { sortMenu = true }) {
                                Text(sortLabelV2New(sort), color = documentAccent)
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                DocumentSortV2New.values().forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(sortLabelV2New(value)) },
                                        onClick = { sort = value; sortMenu = false }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            if (selectionMode) {
                                selectionMode = false
                                selectedDocumentIds.clear()
                            } else {
                                selectionMode = true
                                selectedDocumentIds.clear()
                            }
                        }) {
                            Icon(if (selectionMode) Icons.Outlined.Close else Icons.Outlined.SelectAll,
                                if (selectionMode) "Exit selection" else "Select documents")
                        }
                        IconButton(onClick = { grid = !grid }) {
                            Icon(if (grid) Icons.Outlined.List else Icons.Outlined.GridView, "Toggle view")
                        }
                    }
                }
                if (recycleBin) {
                    item(key = "empty_recycle_bin") {
                        OutlinedButton(
                            onClick = { showEmptyRecycleBinConfirm = true },
                            enabled = !busyDocument,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Empty Recycle Bin")
                        }
                    }
                }
                if (selectionMode) {
                    item(key = "bulk_actions") {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.container)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${selectedDocumentIds.size} selected", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = {
                                        if (selectedDocumentIds.size == visible.size) selectedDocumentIds.clear()
                                        else {
                                            selectedDocumentIds.clear()
                                            selectedDocumentIds.addAll(visible.map { it.id })
                                        }
                                    }) { Text(if (selectedDocumentIds.size == visible.size) "Clear" else "Select all") }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!recycleBin) {
                                        OutlinedButton(
                                            onClick = { runBulkFavourite() },
                                            enabled = selectedDocumentIds.isNotEmpty() && !busyDocument,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Outlined.Favorite, null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Favourite")
                                        }
                                    }
                                    if (!recycleBin && visible.any { it.id in selectedDocumentIds && it.ownerUid == memberUid }) {
                                        Button(
                                            onClick = { runBulkDelete() },
                                            enabled = selectedDocumentIds.isNotEmpty() && !busyDocument,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Outlined.Delete, null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Delete")
                                        }
                                    }
                                }
                                if (recycleBin && visible.any { it.id in selectedDocumentIds && it.ownerUid == memberUid }) {
                                    Button(
                                        onClick = { runBulkRestore() },
                                        enabled = selectedDocumentIds.isNotEmpty() && !busyDocument,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Outlined.Restore, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Restore")
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "favourites_filter") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FilterChip(
                            selected = showFavouritesOnly,
                            enabled = !recycleBin,
                            onClick = { showFavouritesOnly = !showFavouritesOnly },
                            label = { Text("Favourites") },
                            leadingIcon = { Icon(Icons.Outlined.Favorite, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                item(key = "type_chips") {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL" to "All", "PDF" to "Pdf", "IMAGE" to "Image", "OFFICE" to "Office", "OTHER" to "Other")
                            .forEach { (value, label) ->
                                FilterChip(
                                    selected = fileType == value,
                                    onClick = { fileType = value },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(50),
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = documentAccent,
                                        selectedLabelColor = palette.onAccent,
                                        containerColor = palette.container,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                    }
                }
                if (visible.isEmpty()) {
                    item(key = "empty") {
                        EmptyStateV2New(
                            palette,
                            if (recycleBin) "Recycle Bin is empty" else if (search.isNotBlank()) "No matching documents" else "No documents found",
                            if (recycleBin) "Deleted files will appear here."
                            else if (search.isNotBlank()) "Try another name or clear your search."
                            else "Upload a file to get started.",
                            actionLabel = when {
                                recycleBin -> null
                                search.isNotBlank() -> "Clear Search"
                                else -> "Add Documents"
                            },
                            onAction = {
                                if (search.isNotBlank()) search = "" else showAddOptions = true
                            }
                        )
                    }
                } else if (grid) {
                    items(visible.chunked(2)) { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { doc ->
                                DocumentGridCardV2New(
                                    doc, recycleBin, memberUid, Modifier.weight(1f),
                                    onClick = {
                                        if (selectionMode) toggleSelection(doc)
                                        else if (recycleBin) selectedDocument = doc else openDocument(doc)
                                    },
                                    onMore = { selectedDocument = doc },
                                    selectionMode = selectionMode,
                                    selected = doc.id in selectedDocumentIds
                                )
                            }
                            repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } else {
                    items(visible, key = { it.id }) { doc ->
                        DocumentRowV2New(
                            doc, recycleBin, memberUid,
                            onClick = {
                                if (selectionMode) toggleSelection(doc)
                                else if (recycleBin) selectedDocument = doc else openDocument(doc)
                            },
                            onMore = { selectedDocument = doc },
                            selectionMode = selectionMode,
                            selected = doc.id in selectedDocumentIds
                        )
                    }
                }
                if (sharedHasMore || personalHasMore) {
                    item(key = "load_more_documents") {
                        OutlinedButton(
                            onClick = { loadMoreDocuments() },
                            enabled = !loadingMoreDocuments,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            if (loadingMoreDocuments) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading…")
                            } else {
                                Text("Load More Documents")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddOptions) {
        ModalBottomSheet(
            onDismissRequest = { showAddOptions = false },
            containerColor = documentSurface,
            tonalElevation = 8.dp
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
                        startCreateFolder()
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
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, bounds)
                        }
                        var sampleSize = 1
                        while (bounds.outWidth / sampleSize > 1200 ||
                            bounds.outHeight / sampleSize > 1200
                        ) {
                            sampleSize *= 2
                        }
                        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
            }
        }
        AlertDialog(
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            onDismissRequest = {
                if (!uploading) {
                    showPhotoPreview = false
                    pendingPhotoFile?.delete()
                    pendingPhotoFile = null
                    pendingPhotoUri = null
                    capturedPhotoUri = null
                    pendingScanFile?.delete()
                    pendingScanFile = null
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
                        pendingScanFile?.delete()
                        pendingScanFile = null
                    }) { Text("Cancel") }
                }
            }
        )
    }

    if (showAddDocuments) {
        AlertDialog(
            modifier = Modifier.fillMaxSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false),

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
            onDismissRequest = {
                if (!uploading) {
                    showAddDocuments = false
                    pendingUploads.clear()
                    pendingPhotoFile?.delete()
                    pendingPhotoFile = null
                    pendingPhotoUri = null
                    capturedPhotoUri = null
                    pendingScanFile?.delete()
                    pendingScanFile = null
                }
            },
            title = { Text("Add Documents") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(uploadFormScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("${pendingUploads.size} of $MAX_DOCUMENT_BATCH_COUNT_NEW files selected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Limits: 50 MB per file · 200 MB per batch",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Total selected: ${formatDocumentBytesNew(pendingUploads.sumOf { it.sizeBytes })}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (!uploading) {
                    if (pendingUploads.isEmpty()) {
                        OutlinedButton(
                            onClick = {
                                VaultExternalActivityState.documentPickerActive = true
                                picker.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Files")
                        }
                    }
                    Divider()
                    pendingUploads.forEachIndexed { index, pending ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = documentCardSurface)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DocumentTypeIconNew(
                                    pending.fileName.substringAfterLast('.', "").let {
                                        if (it.equals("pdf", true)) "application/pdf" else "application/octet-stream"
                                    },
                                    Modifier.size(38.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(pending.fileName, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Text(formatDocumentBytesNew(pending.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    pending.errorMessage?.let { uploadError ->
                                        Text(
                                            if (pending.retryBlocked) "Needs cleanup: $uploadError" else "Upload failed: $uploadError",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                IconButton(
                                    enabled = !uploading,
                                    onClick = { pendingUploads.removeAt(index) }
                                ) { Icon(Icons.Outlined.Close, contentDescription = "Remove file") }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            Text("Access", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = pending.scope == "PERSONAL",
                                    onClick = { pendingUploads[index] = pending.copy(scope = "PERSONAL", folderId = null) },
                                    label = { Text("Personal") },
                                    enabled = !uploading
                                )
                                FilterChip(
                                    selected = pending.scope == "SHARED",
                                    onClick = { pendingUploads[index] = pending.copy(scope = "SHARED", folderId = null) },
                                    label = { Text("Shared") },
                                    enabled = !uploading
                                )
                            }
                            var categoryMenuExpanded by remember(pending.uri) { mutableStateOf(false) }
                            Text("Category", style = MaterialTheme.typography.labelMedium)
                            Box {
                                OutlinedButton(
                                    onClick = { categoryMenuExpanded = true },
                                    enabled = !uploading,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(allCategories.firstOrNull { it.first == pending.categoryId }?.second ?: "Other")
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Choose category")
                                }
                                DropdownMenu(
                                    expanded = categoryMenuExpanded,
                                    onDismissRequest = { categoryMenuExpanded = false }
                                ) {
                                    allCategories.forEach { (id, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                pendingUploads[index] = pending.copy(categoryId = id, folderId = null)
                                                categoryMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            val eligibleFolders = folders.filter {
                                it.categoryId == pending.categoryId && it.scope == pending.scope &&
                                    (pending.scope == "SHARED" || it.ownerUid == memberUid)
                            }.sortedBy { it.name.lowercase(Locale.ROOT) }
                            var folderMenuExpanded by remember(pending.uri) { mutableStateOf(false) }
                            Text("Destination Folder", style = MaterialTheme.typography.labelMedium)
                            Box {
                                OutlinedButton(
                                    onClick = { folderMenuExpanded = true },
                                    enabled = !uploading,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        pending.folderId?.let { id -> eligibleFolders.firstOrNull { it.id == id }?.name }
                                            ?: "Category root",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Choose folder")
                                }
                                DropdownMenu(
                                    expanded = folderMenuExpanded,
                                    onDismissRequest = { folderMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Category root") },
                                        onClick = {
                                            pendingUploads[index] = pending.copy(folderId = null)
                                            folderMenuExpanded = false
                                        }
                                    )
                                    eligibleFolders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder.name) },
                                            onClick = {
                                                pendingUploads[index] = pending.copy(folderId = folder.id)
                                                folderMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (index != pendingUploads.lastIndex) Divider()
                    }
                    }
                    if (uploading) {
                        Text("Uploading Files", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        pendingUploads.forEachIndexed { index, pending ->
                            val itemProgress = when {
                                index < currentUploadIndex -> 1f
                                index == currentUploadIndex -> currentFileProgress
                                else -> 0f
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DocumentTypeIconNew(
                                        if (pending.fileName.endsWith(".pdf", true)) "application/pdf" else "application/octet-stream",
                                        Modifier.size(34.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(pending.fileName, style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(
                                            when {
                                                index < currentUploadIndex -> "${formatDocumentBytesNew(pending.sizeBytes)} · 100%"
                                                index == currentUploadIndex -> "${formatDocumentBytesNew((pending.sizeBytes * itemProgress).toLong())} · ${(itemProgress * 100).toInt()}%"
                                                else -> "${formatDocumentBytesNew(pending.sizeBytes)} · Pending…"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (index < currentUploadIndex) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Completed",
                                            tint = Color(0xFF31C56B))
                                    } else if (index == currentUploadIndex) {
                                        CircularProgressIndicator(progress = { itemProgress },
                                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { itemProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = documentAccent
                                )
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = documentCardSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Uploading…", fontWeight = FontWeight.Bold)
                                Text(
                                    if (currentUploadIndex >= 0) "Uploading file ${currentUploadIndex + 1} of ${pendingUploads.size}"
                                    else "Preparing upload…",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(),
                                    color = documentAccent)
                                Text("Uploads continue while you use other apps or lock your screen. Closing the app cancels the upload.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uploading && pendingUploads.any { !it.retryBlocked } &&
                        pendingUploads.filter { !it.retryBlocked }.all { it.title.isNotBlank() },
                    onClick = {
                        DocumentUploadForegroundService.start(context, "Uploading documents")
                        scope.launch {
                            val uploadJob = currentCoroutineContext()[Job]
                            DocumentUploadForegroundService.setCancelAction { uploadJob?.cancel() }
                            uploading = true
                            progress = 0f
                            currentFileProgress = 0f
                            var completed = 0
                            val retryableItems = pendingUploads.filter { !it.retryBlocked }
                            val total = retryableItems.size
                            try {
                                retryableItems.forEachIndexed { index, pending ->
                                    message = "Uploading ${index + 1} of $total: ${pending.fileName}"
                                    currentFileProgress = 0f
                                    currentUploadIndex = index
                                    try {
                                        repository.upload(
                                            vaultId, memberUid, pending.uri, context.contentResolver,
                                            pending.title.trim(), pending.categoryId, pending.notes.trim(), pending.scope,
                                            pending.folderId
                                        ) { value ->
                                            currentFileProgress = value
                                            progress = (completed + value) / total.toFloat()
                                            DocumentUploadForegroundService.updateProgress((progress * 100).toInt())
                                        }
                                        completed++
                                        pendingUploads.remove(pending)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        val errorText = error.localizedMessage ?: error.message ?: "Upload failed"
                                        val requiresCleanup = error.message?.contains("Do not retry yet", ignoreCase = true) == true
                                        val itemIndex = pendingUploads.indexOfFirst { it.uri == pending.uri }
                                        if (itemIndex >= 0) {
                                            pendingUploads[itemIndex] = pending.copy(
                                                errorMessage = errorText,
                                                retryBlocked = requiresCleanup
                                            )
                                        }
                                    }
                                }
                                progress = 1f
                                if (pendingUploads.isEmpty()) {
                                    message = "$completed file(s) uploaded."
                                    showAddDocuments = false
                                    pendingPhotoFile?.delete()
                                    pendingPhotoFile = null
                                    pendingPhotoUri = null
                                    capturedPhotoUri = null
                                    pendingScanFile?.delete()
                                    pendingScanFile = null
                                } else {
                                    val blockedCount = pendingUploads.count { it.retryBlocked }
                                    val retryCount = pendingUploads.size - blockedCount
                                    message = buildString {
                                        append("$completed file(s) uploaded. ")
                                        if (retryCount > 0) append("$retryCount file(s) can be retried. ")
                                        if (blockedCount > 0) append("$blockedCount file(s) need cleanup before retry; do not upload them again yet.")
                                    }
                                }
                            } catch (error: CancellationException) {
                                message = "Upload cancelled because the app was closed."
                            } catch (error: Exception) {
                                message = "Upload failed: ${error.localizedMessage ?: error.message ?: "Unknown error"}"
                            } finally {
                                uploading = false
                                DocumentUploadForegroundService.setCancelAction(null)
                                DocumentUploadForegroundService.stop(context)
                            }
                        }
                    }
                ) { Text(if (uploading) "Uploading…" else "Upload") }
            },
            dismissButton = {
                TextButton(
                    enabled = !uploading,
                    onClick = {
                        showAddDocuments = false
                        pendingUploads.clear()
                        pendingPhotoFile?.delete()
                        pendingPhotoFile = null
                        pendingPhotoUri = null
                        capturedPhotoUri = null
                        pendingScanFile?.delete()
                        pendingScanFile = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    if (categoryDialog) {
        AlertDialog(

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
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

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
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

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
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
            ModalBottomSheet(onDismissRequest = { showFolderActions = false }, containerColor = documentSurface) {
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
                            deleteFolderError = null
                            showDeleteFolderConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (showRenameFolder) {
        AlertDialog(

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
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
                            showRenameFolder = false
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
            }
            AlertDialog(

                shape = RoundedCornerShape(24.dp),

                containerColor = MaterialTheme.colorScheme.surface,

                tonalElevation = 8.dp,
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

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
            onDismissRequest = { showDeleteFolderConfirm = false; deleteFolderError = null },
            title = { Text("Delete Empty Folder?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Only an empty folder can be deleted. Documents and subfolders must be moved first.")
                    deleteFolderError?.let { errorText ->
                        Text(
                            errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
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
                            val failure = error.localizedMessage ?: error.message ?: "Folder could not be deleted."
                            showDeleteFolderConfirm = false
                            deleteFolderError = null
                            VaultSnackbarController.show(failure, VaultSnackbarType.ERROR)
                        } finally { busyDocument = false }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteFolderConfirm = false; deleteFolderError = null }) { Text("Cancel") } }
        )
    }

    if (showCreateFolder) {
        AlertDialog(

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
            onDismissRequest = { showCreateFolder = false },
            title = { Text("Create Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it.take(30); createFolderError = null },
                        label = { Text("Folder name") },
                        supportingText = { Text(newFolderName.length.toString() + "/30") },
                        singleLine = true
                    )
                    val parentScope = currentFolderId?.let { id ->
                        folders.firstOrNull { it.id == id }?.scope
                    }
                    if (category == "all" && currentFolderId == null) {
                        Text("Category", style = MaterialTheme.typography.labelMedium)
                        Box {
                            OutlinedButton(onClick = { folderCategoryMenuExpanded = true }) {
                                Text(allCategories.firstOrNull { it.first == newFolderCategory }?.second ?: "Other")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Choose category")
                            }
                            DropdownMenu(expanded = folderCategoryMenuExpanded, onDismissRequest = { folderCategoryMenuExpanded = false }) {
                                allCategories.forEach { (id, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        newFolderCategory = id
                                        folderCategoryMenuExpanded = false
                                    })
                                }
                            }
                        }
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
                    createFolderError?.let { errorText ->
                        Snackbar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color(0xFFC62828),
                            contentColor = Color.White
                        ) { Text(errorText) }
                    }
                    Text(
                        if (currentFolderId == null) "Folder will be created inside " +
                            (allCategories.firstOrNull { it.first == (if (category == "all") newFolderCategory else category) }?.second ?: "this category") + "."
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
                                    vaultId, memberUid, newFolderName,
                                    if (category == "all") newFolderCategory else category,
                                    currentFolderId, newFolderScope
                                )
                                showCreateFolder = false
                                createFolderError = null
                                message = "Folder created."
                            } catch (error: Exception) {
                                val failure = error.localizedMessage ?: error.message ?: "Unable to create folder."
                                createFolderError = failure
                                triggerVaultErrorVibration(context)
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
            }
            val orderedEligibleFolders = buildList {
                val visited = mutableSetOf<String>()
                fun appendChildren(parentId: String?) {
                    eligibleFolders.filter { it.parentFolderId == parentId }
                        .sortedBy { it.name.lowercase(Locale.ROOT) }
                        .forEach { folder ->
                            if (visited.add(folder.id)) {
                                add(folder)
                                appendChildren(folder.id)
                            }
                        }
                }
                appendChildren(null)
            }
            AlertDialog(

                shape = RoundedCornerShape(24.dp),

                containerColor = MaterialTheme.colorScheme.surface,

                tonalElevation = 8.dp,
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
                        orderedEligibleFolders.forEach { folder ->
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
                            "${formatDocumentBytesNew(doc.sizeBytes)} · ${doc.contentType.substringAfter('/') .uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { selectedDocument = null }) {
                        Icon(Icons.Outlined.Close, "Close")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DocumentActionIconNew(
                        icon = { Icon(Icons.Outlined.Visibility, null, tint = documentAccent) },
                        label = "Preview",
                        enabled = !busyDocument && !doc.isDeleted &&
                            (doc.contentType.startsWith("image/") || doc.contentType.contains("pdf", true) ||
                                doc.originalFileName.endsWith(".pdf", true)),
                        onClick = { previewDocument(doc) }
                    )
                    DocumentActionIconNew(
                        icon = { Icon(Icons.Outlined.Download, null, tint = documentAccent) },
                        label = "Download",
                        enabled = !busyDocument && !doc.isDeleted,
                        onClick = { downloadDocument(doc) }
                    )
                    if (doc.ownerUid == memberUid && !doc.isDeleted) {
                        DocumentActionIconNew(
                            icon = { Icon(Icons.Outlined.Edit, null, tint = documentAccent) },
                            label = "Edit",
                            enabled = !busyDocument,
                            onClick = {
                                editTitle = doc.title
                                editDescription = doc.description
                                editCategory = doc.categoryId
                                editScope = doc.scope
                                editingDocument = doc
                                selectedDocument = null
                            }
                        )
                    }
                    DocumentActionIconNew(
                        icon = { Icon(Icons.Outlined.Share, null, tint = documentAccent) },
                        label = "Share",
                        enabled = !busyDocument && !doc.isDeleted,
                        onClick = { shareDocument(doc) }
                    )
                    DocumentActionIconNew(
                        icon = { Icon(Icons.Outlined.Info, null, tint = documentAccent) },
                        label = "Details",
                        enabled = !busyDocument,
                        onClick = { showDocumentDetails = true }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (!doc.isDeleted) {
                    ListItem(
                        headlineContent = { Text(if (doc.favouriteUids[memberUid] == true) "Remove from Favourites" else "Add to Favourites") },
                        supportingContent = { Text("Only your account's Favourite list is changed") },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable(enabled = !busyDocument) {
                            toggleFavourite(doc)
                            selectedDocument = null
                        }
                    )
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
                    if (doc.ownerUid == memberUid) {
                        ListItem(
                            headlineContent = { Text("Move to Recycle Bin", color = MaterialTheme.colorScheme.error) },
                            supportingContent = { Text("Keep it recoverable for 30 days") },
                            leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable(enabled = !busyDocument) {
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
                    }
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

    if (showDocumentPreview && previewBitmap != null) {
        AlertDialog(
            modifier = Modifier.fillMaxSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDocumentPreview = false },
            title = {
                Text(previewTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            text = {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                        bitmap = previewBitmap!!,
                        contentDescription = "Document preview",
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                    if (previewPageCount > 1) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                enabled = previewPageIndex > 0,
                                onClick = { selectedDocument?.let { previewDocument(it, previewPageIndex - 1) } }
                            ) { Text("Previous") }
                            Text("Page ${previewPageIndex + 1} of $previewPageCount")
                            TextButton(
                                enabled = previewPageIndex < previewPageCount - 1,
                                onClick = { selectedDocument?.let { previewDocument(it, previewPageIndex + 1) } }
                            ) { Text("Next") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocumentPreview = false }) { Text("Close") }
            }
        )
    }

    if (isDownloadingFile) {
        val total = downloadTotalBytes
        val progressValue = if (total > 0L) {
            (downloadTransferredBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else 0f
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Downloading document", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(downloadingFileName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(progress = { progressValue }, modifier = Modifier.fillMaxWidth(),
                        color = documentAccent)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDocumentBytesNew(downloadTransferredBytes), style = MaterialTheme.typography.bodySmall)
                        Text(if (total > 0L) "of ${formatDocumentBytesNew(total)}" else "Calculating total…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        when {
                            isSavingDownloadedFile -> "Saving to Downloads/Personal Vault…"
                            total > 0L -> "${(progressValue * 100).toInt()}% downloaded"
                            else -> "Downloading…"
                        },
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {}
        )
    }

    if (showEmptyRecycleBinConfirm) {
        AlertDialog(
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            onDismissRequest = { if (!busyDocument) showEmptyRecycleBinConfirm = false },
            title = { Text("Empty Recycle Bin?") },
            text = {
                Text("All deleted documents owned by you will be permanently removed, including their stored files. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    enabled = !busyDocument,
                    onClick = {
                        scope.launch {
                            busyDocument = true
                            try {
                                val count = repository.emptyRecycleBin(vaultId)
                                showEmptyRecycleBinConfirm = false
                                selectedDocument = null
                                selectedDocumentIds.clear()
                                selectionMode = false
                                message = if (count == 0) "Recycle Bin is already empty." else "$count document(s) permanently deleted."
                            } catch (error: Exception) {
                                message = error.localizedMessage ?: error.message ?: "Unable to empty Recycle Bin."
                            } finally {
                                busyDocument = false
                            }
                        }
                    }
                ) { Text("Empty Bin", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !busyDocument, onClick = { showEmptyRecycleBinConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    permanentDeleteTarget?.let { doc ->
        AlertDialog(

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
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
                modifier = Modifier.fillMaxSize(),
                properties = DialogProperties(usePlatformDefaultWidth = false),
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                onDismissRequest = { showDocumentDetails = false },
                title = {
                    Text(
                        "File Details",
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal
                    )
                },
                text = {
                    Column(
                        Modifier.fillMaxWidth().fillMaxHeight(0.94f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(12.dp))
                            DocumentTypeIconNew(doc.contentType, Modifier.size(72.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                doc.title.ifBlank { doc.originalFileName },
                                modifier = Modifier.fillMaxWidth(),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                doc.originalFileName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            ScopeLabelV2New(doc.scope, palette)
                            Spacer(Modifier.height(18.dp))
                            HorizontalDivider(color = palette.line)
                            Spacer(Modifier.height(8.dp))
                            DocumentDetailRowV2New(
                                "Type",
                                doc.contentType.substringAfter('/').uppercase().ifBlank { "File" },
                                Icons.Outlined.Description
                            )
                            DocumentDetailRowV2New("Size", formatDocumentBytesNew(doc.sizeBytes), Icons.Outlined.Storage)
                            DocumentDetailRowV2New(
                                "Location",
                                "My Documents / ${allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other"}",
                                Icons.Outlined.FolderOpen
                            )
                            DocumentDetailRowV2New(
                                "Added",
                                doc.uploadedAt?.toDate()?.let {
                                    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(it)
                                } ?: "—",
                                Icons.Outlined.CalendarToday
                            )
                            DocumentDetailRowV2New(
                                "Modified",
                                doc.updatedAt?.toDate()?.let {
                                    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(it)
                                } ?: "—",
                                Icons.Outlined.History
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showDocumentDetails = false }) {
                                Text("Close", color = palette.accent)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }

    editingDocument?.let { doc ->
        AlertDialog(
            modifier = Modifier.fillMaxSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false),

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
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
                                    ?.let { "Selected: $it" } ?: formatDocumentBytesNew(doc.sizeBytes),
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
                                "${formatDocumentBytesNew((selected.sizeBytes * replacementProgress).toLong())} / ${formatDocumentBytesNew(selected.sizeBytes)}",
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
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    var editCategoryMenuExpanded by remember(doc.id) { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { editCategoryMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                allCategories.firstOrNull { it.first == editCategory }?.second ?: "Other",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Choose category")
                        }
                        DropdownMenu(
                            expanded = editCategoryMenuExpanded,
                            onDismissRequest = { editCategoryMenuExpanded = false }
                        ) {
                            allCategories.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = { editCategory = id; editCategoryMenuExpanded = false }
                                )
                            }
                        }
                    }
                    Text("Access", style = MaterialTheme.typography.labelLarge)
                    Row(
                        Modifier.fillMaxWidth().clickable { editScope = "PERSONAL" }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = editScope == "PERSONAL", onClick = { editScope = "PERSONAL" })
                        Column {
                            Text("Personal", fontWeight = FontWeight.SemiBold)
                            Text("Only you can access", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { editScope = "SHARED" }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = editScope == "SHARED", onClick = { editScope = "SHARED" })
                        Column {
                            Text("Shared", fontWeight = FontWeight.SemiBold)
                            Text("Both household members can access", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                    val selectedReplacement = pendingReplacement
                        ?.takeIf { replacementTarget?.id == doc.id }
                    if (selectedReplacement != null) {
                        DocumentUploadForegroundService.start(context, "Replacing document")
                    }
                    scope.launch {
                        val replacementJob = currentCoroutineContext()[Job]
                        if (selectedReplacement != null) {
                            DocumentUploadForegroundService.setCancelAction { replacementJob?.cancel() }
                        }
                        busyDocument = true
                        try {
                            if (selectedReplacement != null) {
                                replacingDocument = true
                                replacementProgress = 0f
                                repository.replaceFile(
                                    vaultId, doc, selectedReplacement.uri, context.contentResolver
                                ) { value ->
                                    replacementProgress = value
                                    DocumentUploadForegroundService.updateProgress((value * 100).toInt())
                                }
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
                        } catch (error: CancellationException) {
                            message = "Replacement cancelled because the app was closed."
                            throw error
                        } catch (error: Exception) {
                            editingDocument = null
                            replacementTarget = null
                            message = error.localizedMessage ?: error.message ?: "Save failed."
                        } finally {
                            replacingDocument = false
                            busyDocument = false
                            if (selectedReplacement != null) {
                                DocumentUploadForegroundService.setCancelAction(null)
                                DocumentUploadForegroundService.stop(context)
                            }
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

// ───────────────────────── Redesign: palette & helpers ─────────────────────────

private data class DocPaletteV2New(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val surface: Color,
    val surface2: Color,
    val muted: Color,
    val line: Color
)

private fun documentPaletteV2New(dark: Boolean): DocPaletteV2New =
    if (dark) DocPaletteV2New(
        accent = Color(0xFFCBA6FF), onAccent = Color(0xFF1B0B33), container = Color(0xFF2E2145),
        surface = Color(0xFF1D1925), surface2 = Color(0xFF272131), muted = Color(0xFFB0A8C2), line = Color(0xFF2C2536)
    ) else DocPaletteV2New(
        accent = Color(0xFF6F35D5), onAccent = Color.White, container = Color(0xFFF0E8FD),
        surface = Color.White, surface2 = Color(0xFFF1EEF7), muted = Color(0xFF625B75), line = Color(0xFFE8E2F3)
    )

private fun categoryStyleV2New(id: String, dark: Boolean): Pair<Color, Color> = if (dark) when (id) {
    "banking_finance" -> Color(0xFF12372A) to Color(0xFF8FE3B9)
    "property" -> Color(0xFF45290F) to Color(0xFFFFC38A)
    "insurance" -> Color(0xFF16324F) to Color(0xFF9CCBFF)
    "employment" -> Color(0xFF32224F) to Color(0xFFD9C2FF)
    "vehicle" -> Color(0xFF1B2D52) to Color(0xFFB3C8FF)
    "travel_visa" -> Color(0xFF0F3534) to Color(0xFF8FE0DC)
    "medical" -> Color(0xFF4A1E2A) to Color(0xFFFFA9BC)
    "education" -> Color(0xFF443410) to Color(0xFFFFD98A)
    "legal_documents" -> Color(0xFF222A55) to Color(0xFFB7C1FF)
    else -> Color(0xFF2B2142) to Color(0xFFCDB5FF)
} else when (id) {
    "banking_finance" -> Color(0xFFE0F5EA) to Color(0xFF087747)
    "property" -> Color(0xFFFFEBD8) to Color(0xFFB85A00)
    "insurance" -> Color(0xFFE1F0FF) to Color(0xFF1767B1)
    "employment" -> Color(0xFFF0E5FF) to Color(0xFF7134BD)
    "vehicle" -> Color(0xFFE3ECFF) to Color(0xFF2457B5)
    "travel_visa" -> Color(0xFFDFF4F3) to Color(0xFF087579)
    "medical" -> Color(0xFFFFE7EC) to Color(0xFFC0304E)
    "education" -> Color(0xFFFFF2D8) to Color(0xFF8A5A0A)
    "legal_documents" -> Color(0xFFE8EBFF) to Color(0xFF394B9B)
    else -> Color(0xFFEDE6FA) to Color(0xFF5B2FB5)
}

private fun categoryIconV2New(id: String): ImageVector = when (id) {
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

private fun sortLabelV2New(sort: DocumentSortV2New): String = when (sort) {
    DocumentSortV2New.NAME_ASC -> "Name A–Z"
    DocumentSortV2New.NAME_DESC -> "Name Z–A"
    DocumentSortV2New.NEWEST -> "Newest first"
    DocumentSortV2New.OLDEST -> "Oldest first"
    DocumentSortV2New.LARGEST -> "Largest first"
    DocumentSortV2New.SMALLEST -> "Smallest first"
}

private fun documentMetaV2New(document: DocumentRecordV2): String {
    val extension = document.originalFileName.substringAfterLast('.', "")
        .uppercase(Locale.ROOT).take(5)
        .ifBlank { document.contentType.substringAfter('/', "").uppercase(Locale.ROOT).take(5) }
    val date = document.uploadedAt?.toDate()?.let {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(it)
    }
    return listOfNotNull(
        extension.takeIf { it.isNotBlank() },
        formatDocumentBytesNew(document.sizeBytes),
        date
    ).joinToString(", ")
}

private fun Modifier.dashedBorderV2New(color: Color, radius: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        )
    }

// ───────────────────────── Redesign: components ─────────────────────────

@Composable
private fun DocumentSummaryTileV2New(
    label: String,
    value: String,
    icon: ImageVector,
    palette: DocPaletteV2New,
    modifier: Modifier,
    isShared: Boolean? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tileBackground = when (isShared) {
        true -> if (isDark) Color(0xFF352347) else Color(0xFFF0E5FF)
        false -> if (isDark) Color(0xFF1D3A2C) else Color(0xFFE4F5E9)
        null -> palette.container
    }
    val tileAccent = when (isShared) {
        true -> if (isDark) Color(0xFFD2A8FF) else Color(0xFF7136B8)
        false -> if (isDark) Color(0xFF8FE0A7) else Color(0xFF21834A)
        null -> palette.accent
    }
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tileBackground)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = tileAccent, modifier = Modifier.size(21.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = if (isShared != null) tileAccent else MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = if (isShared != null) tileAccent else palette.muted, maxLines = 1)
        }
    }
}

@Composable
private fun ScopeLabelV2New(scope: String, palette: DocPaletteV2New) {
    val shared = scope == "SHARED"
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (shared) {
        if (isDark) Color(0xFF352347) else Color(0xFFF0E5FF)
    } else {
        if (isDark) Color(0xFF1D3A2C) else Color(0xFFE4F5E9)
    }
    val foreground = if (shared) {
        if (isDark) Color(0xFFD2A8FF) else Color(0xFF7136B8)
    } else {
        if (isDark) Color(0xFF8FE0A7) else Color(0xFF21834A)
    }
    Surface(shape = RoundedCornerShape(10.dp), color = background) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                if (shared) Icons.Outlined.Groups else Icons.Outlined.Person,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(14.dp)
            )
            Text(if (shared) "Shared" else "Personal",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = foreground, maxLines = 1)
        }
    }
}

@Composable
private fun DocumentDetailRowV2New(label: String, value: String, icon: ImageVector) {
    ListItem(
        modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        supportingContent = {
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        },
        leadingContent = {
            Icon(icon, contentDescription = null,
                tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
                    Color(0xFFD2A8FF) else Color(0xFF7136B8))
        }
    )
}

@Composable
private fun ScopePillV2New(document: DocumentRecordV2, mine: Boolean, palette: DocPaletteV2New) {
    ScopeLabelV2New(document.scope, palette)
}

@Composable
private fun DocumentRowV2New(
    document: DocumentRecordV2,
    isRecycleBin: Boolean,
    currentMemberUid: String,
    onClick: () -> Unit,
    onMore: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = documentPaletteV2New(dark)
    val mine = document.ownerUid == currentMemberUid
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            }
            DocumentTypeIconNew(document.contentType, Modifier.size(46.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    document.title.ifBlank { document.originalFileName }.take(50),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    documentMetaV2New(document),
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isRecycleBin) {
                    Text("Deleted", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (!isRecycleBin) {
                if (document.favouriteUids[currentMemberUid] == true) {
                    Icon(Icons.Outlined.Favorite, contentDescription = "Favourite",
                        tint = Color(0xFFB45BCE), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.width(6.dp))
                ScopePillV2New(document, mine, palette)
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More document options", tint = palette.muted)
            }
        }
    }
}

@Composable
private fun DocumentGridCardV2New(
    document: DocumentRecordV2,
    isRecycleBin: Boolean,
    currentMemberUid: String,
    modifier: Modifier,
    onClick: () -> Unit,
    onMore: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = documentPaletteV2New(dark)
    val mine = document.ownerUid == currentMemberUid
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(checked = selected, onCheckedChange = { onClick() })
                }
                DocumentTypeIconNew(document.contentType, Modifier.size(44.dp))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onMore) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More document options", tint = palette.muted)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                document.title.ifBlank { document.originalFileName }.take(50),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                documentMetaV2New(document),
                color = palette.muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            if (!isRecycleBin && document.favouriteUids[currentMemberUid] == true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Favorite, contentDescription = "Favourite",
                        tint = Color(0xFFB45BCE), modifier = Modifier.size(14.dp))
                    Text("Favourite", color = Color(0xFFB45BCE), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isRecycleBin) {
                Text("Deleted", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            } else {
                ScopePillV2New(document, mine, palette)
            }
        }
    }
}

@Composable
private fun CategoryCardV2New(
    name: String,
    icon: ImageVector,
    count: Int,
    tileBg: Color,
    tileFg: Color,
    palette: DocPaletteV2New,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = tileBg) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tileFg, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("$count files", color = palette.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FolderCardV2New(
    folder: DocumentFolderV2,
    isOwner: Boolean,
    palette: DocPaletteV2New,
    modifier: Modifier,
    onOpen: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = if (isOwner) 0.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.FolderOpen, null, tint = palette.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    folder.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                ScopeLabelV2New(folder.scope, palette)
            }
            if (isOwner) {
                IconButton(onClick = onMore, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.MoreVert, "Folder actions", tint = palette.muted)
                }
            }
        }
    }
}

@Composable
private fun NewFolderCardV2New(palette: DocPaletteV2New, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .dashedBorderV2New(palette.muted, 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, null, tint = palette.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("New folder", color = palette.accent, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyStateV2New(
    palette: DocPaletteV2New,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(Modifier.size(56.dp), shape = RoundedCornerShape(28.dp), color = palette.container) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Description, null, tint = palette.accent)
                }
            }
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DocumentTypeIconNew(contentType: String, modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (tile, tint, image) = when {
        contentType.contains("pdf", true) ->
            Triple(if (isDark) Color(0xFF4A2020) else Color(0xFFFFE3E3), if (isDark) Color(0xFFFF9A9A) else Color(0xFFC42020), Icons.Outlined.PictureAsPdf)
        contentType.startsWith("image/") ->
            Triple(if (isDark) Color(0xFF1E3350) else Color(0xFFDDEBFF), if (isDark) Color(0xFF9CC4FF) else Color(0xFF1D63C2), Icons.Outlined.Image)
        contentType.contains("spreadsheet", true) || contentType.contains("excel", true) ||
            contentType.contains("sheet", true) ->
            Triple(if (isDark) Color(0xFF1B3A2A) else Color(0xFFDDF4E5), if (isDark) Color(0xFF9BE3BA) else Color(0xFF13733F), Icons.Outlined.Description)
        else ->
            Triple(if (isDark) Color(0xFF302447) else Color(0xFFEBE2FF), if (isDark) Color(0xFFCDB5FF) else Color(0xFF5B2FB5), Icons.Outlined.InsertDriveFile)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = tile) {
        Box(contentAlignment = Alignment.Center) {
            Icon(image, null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DocumentActionIconNew(
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
            color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF30243D) else Color(0xFFF0E8FF)
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { icon() }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}


/**
 * Publishes a completed document download into Downloads/Personal Vault without
 * opening the system Save As picker.
 */
private fun saveDocumentToDownloadsNew(
    context: Context,
    source: File,
    displayName: String,
    mimeType: String
) {
    require(source.isFile && source.length() > 0L) { "The downloaded document is missing or empty." }
    val safeDisplayName = displayName.substringAfterLast('/').substringAfterLast('\\')
        .ifBlank { "document" }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeDisplayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Personal Vault")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Unable to create the file in Downloads/Personal Vault.")
        try {
            val output = resolver.openOutputStream(destination, "w")
                ?: throw IllegalStateException("Unable to open the destination file.")
            output.use { stream ->
                source.inputStream().use { input -> input.copyTo(stream) }
            }
            val completedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            check(resolver.update(destination, completedValues, null, null) > 0) {
                "The file was copied but could not be published in Downloads."
            }
        } catch (error: Exception) {
            resolver.delete(destination, null, null)
            throw error
        }
    } else {
        val externalDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val downloadsDirectory = File(externalDownloads, "Personal Vault")
        if (!downloadsDirectory.exists() && !downloadsDirectory.mkdirs()) {
            throw IllegalStateException("Unable to create the app Downloads folder.")
        }
        source.copyTo(File(downloadsDirectory, safeDisplayName), overwrite = true)
    }
}


private fun formatDocumentBytesNew(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}