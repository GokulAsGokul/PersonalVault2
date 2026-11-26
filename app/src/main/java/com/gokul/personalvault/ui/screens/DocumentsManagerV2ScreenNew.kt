package com.gokul.personalvault.ui.screens

import com.gokul.personalvault.ui.components.VaultCompactHeader

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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.People
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
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gokul.personalvault.VaultExternalActivityState
import com.gokul.personalvault.DocumentUploadForegroundService
import com.gokul.personalvault.DocumentUploadUiState
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
import com.google.firebase.functions.FirebaseFunctionsException
import org.json.JSONArray
import org.json.JSONObject
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
    var showBulkPermanentDeleteConfirm by remember { mutableStateOf(false) }
    var showEmptyRecycleBinConfirm by remember { mutableStateOf(false) }
    var showRecycleBinSearch by remember { mutableStateOf(false) }
    var showDocumentSearch by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var advancedSearchExpanded by remember { mutableStateOf(false) }
    var searchFileNameOnly by remember { mutableStateOf(false) }
    var showFavouritesOnly by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedDocumentIds = remember { mutableStateListOf<String>() }
    var category by remember { mutableStateOf("all") }
    var visibility by remember { mutableStateOf("ALL") }
    var dashboardFilter by remember { mutableStateOf("ALL") }
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
    var categoryCreateError by remember { mutableStateOf<String?>(null) }
    var categoryCreateSaving by remember { mutableStateOf(false) }
    var categoryCreateValidationAttempt by remember { mutableIntStateOf(0) }
    val categoryCreateShake = remember { Animatable(0f) }
    var manageCategories by remember { mutableStateOf(false) }
    val manageCategoriesScrollState = rememberScrollState()
    var categoryToEdit by remember { mutableStateOf<Pair<String, String>?>(null) }
    var categoryEditName by remember { mutableStateOf("") }
    var categoryRenameError by remember { mutableStateOf<String?>(null) }
    var categoryRenameSaving by remember { mutableStateOf(false) }
    var categoryRenameValidationAttempt by remember { mutableIntStateOf(0) }
    val categoryRenameShake = remember { Animatable(0f) }
    var categoryDeletingId by remember { mutableStateOf<String?>(null) }
    val addOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    var downloadingFileType by remember { mutableStateOf("DOCUMENT") }
    var downloadTransferredBytes by remember { mutableLongStateOf(0L) }
    var downloadTotalBytes by remember { mutableLongStateOf(0L) }
    var isSavingDownloadedFile by remember { mutableStateOf(false) }
    var activeDocumentTransferJob by remember { mutableStateOf<Job?>(null) }
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

    // The foreground service owns the actual Firebase upload. This collector only
    // mirrors its progress into the screen when the Documents screen is visible.
    LaunchedEffect(vaultId, memberUid) {
        DocumentUploadForegroundService.uploadState.collect { uploadState ->
            when (uploadState) {
                is DocumentUploadUiState.Idle -> Unit
                is DocumentUploadUiState.Running -> {
                    uploading = true
                    progress = uploadState.overallProgress
                    currentFileProgress = uploadState.fileProgress
                    currentUploadIndex = uploadState.fileIndex - 1
                    message = "Uploading ${uploadState.fileIndex} of ${uploadState.totalFiles}: ${uploadState.fileName}"
                }
                is DocumentUploadUiState.Completed -> {
                    uploading = false
                    progress = 1f
                    val previouslyBlocked = pendingUploads.filter { it.retryBlocked }
                    val failedItems = uploadState.failures.map { failure ->
                        PendingDocumentUploadV2New(
                            uri = Uri.parse(failure.uri),
                            fileName = failure.fileName,
                            sizeBytes = failure.sizeBytes,
                            title = failure.title,
                            notes = failure.notes,
                            categoryId = failure.categoryId,
                            scope = failure.scope,
                            folderId = failure.folderId,
                            errorMessage = failure.errorMessage,
                            retryBlocked = failure.retryBlocked
                        )
                    }
                    pendingUploads.clear()
                    pendingUploads.addAll(previouslyBlocked + failedItems)
                    if (pendingUploads.isEmpty()) {
                        message = "${uploadState.uploadedCount} file(s) uploaded successfully."
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
                            append("${uploadState.uploadedCount} file(s) uploaded. ")
                            if (retryCount > 0) append("$retryCount file(s) can be retried. ")
                            if (blockedCount > 0) append("$blockedCount file(s) need cleanup before retry; do not upload them again yet.")
                        }
                    }
                    DocumentUploadForegroundService.acknowledgeResult()
                }
                is DocumentUploadUiState.Failed -> {
                    uploading = false
                    message = "Upload failed: ${uploadState.message}"
                    DocumentUploadForegroundService.acknowledgeResult()
                }
                is DocumentUploadUiState.Cancelled -> {
                    uploading = false
                    message = "Upload cancelled because the app was closed."
                    DocumentUploadForegroundService.acknowledgeResult()
                }
            }
        }
    }

    LaunchedEffect(vaultId) {
        withContext(Dispatchers.IO) {
            repository.cleanupDocumentCache(context.cacheDir)
        }
    }

    LaunchedEffect(showAddDocuments) {
        if (showAddDocuments) uploadFormScrollState.scrollTo(0)
    }

    LaunchedEffect(categoryCreateValidationAttempt) {
        if (categoryCreateValidationAttempt > 0) {
            triggerVaultErrorVibration(context)
            categoryCreateShake.snapTo(0f)
            listOf(-9f, 9f, -7f, 7f, -4f, 4f, 0f).forEach {
                categoryCreateShake.animateTo(it, tween(35))
            }
        }
    }

    LaunchedEffect(categoryRenameValidationAttempt) {
        if (categoryRenameValidationAttempt > 0) {
            triggerVaultErrorVibration(context)
            categoryRenameShake.snapTo(0f)
            listOf(-9f, 9f, -7f, 7f, -4f, 4f, 0f).forEach {
                categoryRenameShake.animateTo(it, tween(35))
            }
        }
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
            // Returning from the camera without taking a photo is a normal cancellation.
            if (success) message = "The camera did not save a photo. Please try again."
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
    val dashboardScoped: (DocumentRecordV2) -> Boolean = { doc ->
        when (dashboardFilter) {
            "ONLY_ME" -> doc.scope != "SHARED" && doc.ownerUid == memberUid
            "SHARED" -> doc.scope == "SHARED" && doc.ownerUid == memberUid
            "SPOUSE" -> doc.scope == "SHARED" && doc.ownerUid != memberUid
            else -> true
        }
    }
    val scopedActive = active.filter { visibility == "ALL" || it.scope == visibility }
    val deleted = documents.filter { it.isDeleted }
    val source = if (recycleBin) deleted else active
    val visible = source
        .filter { recycleBin || dashboardScoped(it) }
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
        .filter { recycleBin || !showFavouritesOnly || it.favouriteUids[memberUid] == true }
.let { list ->
            when (sort) {
                DocumentSortV2New.NAME_ASC -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
                DocumentSortV2New.NAME_DESC -> list.sortedByDescending { it.title.lowercase(Locale.ROOT) }
                DocumentSortV2New.NEWEST -> list.sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }
                DocumentSortV2New.OLDEST -> list.sortedBy { it.uploadedAt?.toDate()?.time ?: 0L }
                DocumentSortV2New.LARGEST -> list.sortedByDescending { it.sizeBytes }
                DocumentSortV2New.SMALLEST -> list.sortedBy { it.sizeBytes }
            }
        }

    val allCategories = BuiltInDocumentCategoriesV2.all.map { it.id to it.name } + customCategories

    fun editDocument(doc: DocumentRecordV2) {
        editTitle = doc.title
        editDescription = doc.description
        editCategory = doc.categoryId
        editScope = doc.scope
        editingDocument = doc
    }

    fun downloadDocument(doc: DocumentRecordV2) {
        activeDocumentTransferJob = scope.launch {
            busyDocument = true
            isDownloadingFile = true
            downloadingFileName = doc.originalFileName.ifBlank { doc.title }
            downloadingFileType = doc.originalFileName.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
                ?: when {
                    doc.contentType.contains("pdf", true) -> "PDF"
                    doc.contentType.startsWith("image/jpeg", true) -> "JPG"
                    doc.contentType.startsWith("image/png", true) -> "PNG"
                    doc.contentType.startsWith("image/", true) -> "IMAGE"
                    doc.contentType.contains("spreadsheet", true) || doc.contentType.contains("excel", true) -> "XLSX"
                    doc.contentType.contains("word", true) -> "DOCX"
                    else -> "DOCUMENT"
                }
            downloadTransferredBytes = 0L
            downloadTotalBytes = doc.sizeBytes.coerceAtLeast(0L)
            try {
                val file = repository.downloadToCache(context.cacheDir, vaultId, doc) { transferred, total ->
                    downloadTransferredBytes = transferred.coerceAtLeast(0L)
                    if (total > 0L) downloadTotalBytes = total
                }
                isSavingDownloadedFile = true
                withContext<Unit>(Dispatchers.IO) {
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
            } catch (error: CancellationException) {
                message = "Download cancelled."
            } catch (error: Exception) {
                message = error.localizedMessage ?: error.message ?: "Download failed."
            } finally {
                busyDocument = false
                isSavingDownloadedFile = false
                isDownloadingFile = false
                activeDocumentTransferJob = null
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
        activeDocumentTransferJob = scope.launch {
            busyDocument = true
            isDownloadingFile = true
            downloadingFileName = doc.originalFileName.ifBlank { doc.title }
            downloadingFileType = doc.originalFileName.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
                ?: when {
                    doc.contentType.contains("pdf", true) -> "PDF"
                    doc.contentType.startsWith("image/jpeg", true) -> "JPG"
                    doc.contentType.startsWith("image/png", true) -> "PNG"
                    doc.contentType.startsWith("image/", true) -> "IMAGE"
                    doc.contentType.contains("spreadsheet", true) || doc.contentType.contains("excel", true) -> "XLSX"
                    doc.contentType.contains("word", true) -> "DOCX"
                    else -> "DOCUMENT"
                }
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
            } catch (error: CancellationException) {
                message = "Download cancelled."
                VaultExternalActivityState.documentViewerActive = false
            } catch (error: Exception) {
                VaultExternalActivityState.documentViewerActive = false
                message = error.localizedMessage ?: error.message ?: "Unable to open document."
            } finally {
                busyDocument = false
                isDownloadingFile = false
                activeDocumentTransferJob = null
            }
        }
    }

    suspend fun renderPreviewPage(file: File, doc: DocumentRecordV2, pageIndex: Int): Pair<androidx.compose.ui.graphics.ImageBitmap, Int> =
        withContext(Dispatchers.IO) {
            if (doc.contentType.startsWith("image/")) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val sampleSize = maxOf(                    1,
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
        activeDocumentTransferJob = scope.launch {
            busyDocument = true
            val cachedPreview = previewFile?.takeIf { previewFileDocumentId == doc.id }
            isDownloadingFile = cachedPreview == null
            downloadingFileName = doc.originalFileName.ifBlank { doc.title }
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
            } catch (error: CancellationException) {
                message = "Download cancelled."
            } catch (error: Exception) {
                message = error.localizedMessage ?: error.message ?: "Unable to preview this document."
            } finally {
                isDownloadingFile = false
                busyDocument = false
                activeDocumentTransferJob = null
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

    fun runBulkPermanentDelete() {
        val targets = visible.filter {
            it.id in selectedDocumentIds && it.ownerUid == memberUid && it.scope != "SHARED"
        }
        val skipped = selectedDocumentIds.size - targets.size
        scope.launch {
            busyDocument = true
            var succeeded = 0
            try {
                targets.forEach { doc ->
                    repository.permanentlyDelete(vaultId, doc.id)
                    succeeded++
                }
                message = "$succeeded document(s) permanently deleted." +
                    if (skipped > 0) " $skipped skipped: only personal documents owned by you can be permanently deleted." else ""
            } catch (error: Exception) {
                message = "$succeeded deleted. ${error.localizedMessage ?: "Some documents could not be permanently deleted."}"
            } finally {
                selectedDocumentIds.clear()
                selectionMode = false
                busyDocument = false
            }
        }
    }

    fun runBulkRestore() {
        val targets = visible.filter {
            it.id in selectedDocumentIds && it.ownerUid == memberUid && it.scope != "SHARED"
        }
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

    // Return to the default, unselected state whenever the Documents dashboard is shown.
    LaunchedEffect(isHome) {
        if (isHome) {
            selectedDocumentIds.clear()
            selectionMode = false
        }
    }

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
                dashboardFilter = "ALL"
            }
            showAllDocuments -> {
                showAllDocuments = false
                dashboardFilter = "ALL"
            }
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
        !recycleBin && it.parentFolderId == currentFolderId &&
            (visibility == "ALL" || it.scope == visibility)
    }.sortedBy { it.name.lowercase(Locale.ROOT) }
    val headerTitle = when {
        recycleBin -> "Recycle Bin"
        category != "all" -> categoryTitle
        showAllDocuments -> "All files"
        else -> "Documents"
    }
    val headerSubtitle = null
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    LaunchedEffect(category, currentFolderId, showAllDocuments, recycleBin) { listState.scrollToItem(0) }
    val mutedColor = palette.muted

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }
            VaultCompactHeader(
                title = headerTitle,
                subtitle = headerSubtitle,
                onBack = if (canGoBack) ({ goBack() }) else null,
                titleIcon = if (headerTitle == "Documents") Icons.Outlined.Description else null,
                actions = {
                    if (recycleBin) {
                        IconButton(onClick = {
                            showRecycleBinSearch = !showRecycleBinSearch
                            if (!showRecycleBinSearch) search = ""
                        }) {
                            Icon(
                                if (showRecycleBinSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = if (showRecycleBinSearch) "Close Recycle Bin search" else "Search Recycle Bin"
                            )
                        }
                    } else {
                        IconButton(onClick = { showDocumentSearch = !showDocumentSearch; if (!showDocumentSearch) { search = ""; advancedSearchExpanded = false } }) {
                            Icon(if (showDocumentSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = if (showDocumentSearch) "Close search" else "Search documents")
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More options") }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("New folder") }, leadingIcon = { Icon(Icons.Outlined.FolderOpen, null) }, onClick = { menuExpanded = false; startCreateFolder() })
                            }
                        }
                    }
                }
            )
        },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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

            // Unified Documents Overview: scope filters and live statistics.
            if (isHome) {
                item(key = "documents_overview") {
                    val dashboardOverviewDocs = if (recycleBin) deleted else active.filter(dashboardScoped)

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            documentAccent.copy(alpha = 0.28f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val filters = listOf(
                                Triple("ALL", "All", Icons.Outlined.SelectAll),
                                Triple("ONLY_ME", "Only Me", Icons.Outlined.Person),
                                Triple("SHARED", "Shared", Icons.Outlined.People),
                                Triple("SPOUSE", "Spouse", Icons.Outlined.People),
                                Triple("RECYCLE_BIN", "Recycle Bin", Icons.Outlined.Delete)
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                filters.forEach { (value, label, icon) ->
                                    val selected = if (value == "RECYCLE_BIN") recycleBin
                                        else !recycleBin && dashboardFilter == value
                                    val tileBackground = when (value) {
                                        "ALL" -> Color(0xFFF0E5FF)
                                        "ONLY_ME" -> Color(0xFFE7F7E9)
                                        "SHARED" -> Color(0xFFFFEEE5)
                                        "SPOUSE" -> Color(0xFFFCE8F5)
                                        else -> Color(0xFFE3F0FF)
                                    }
                                    val tileAccent = when (value) {
                                        "ALL" -> Color(0xFF7136B8)
                                        "ONLY_ME" -> Color(0xFF269B52)
                                        "SHARED" -> Color(0xFFEF7B35)
                                        "SPOUSE" -> Color(0xFFD63384)
                                        else -> Color(0xFF2875D0)
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f)
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable(
                                                indication = ripple(bounded = true, color = tileAccent),
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) {
                                            if (value == "RECYCLE_BIN") {
                                                recycleBin = true
                                                category = "all"
                                                currentFolderId = null
                                                showAllDocuments = false
                                                visibility = "ALL"
                                            } else {
                                                recycleBin = false
                                                dashboardFilter = value
                                                visibility = "ALL"
                                                category = "all"
                                                currentFolderId = null
                                                showAllDocuments = false
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = tileBackground,
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) tileAccent else tileAccent.copy(alpha = 0.18f)
                                        ),
                                        shadowElevation = if (selected) 3.dp else 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 1.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(icon, contentDescription = null,
                                                tint = tileAccent,
                                                modifier = Modifier.size(23.dp))
                                            Text(label,
                                                color = Color(0xFF211536),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                            val personalCount = dashboardOverviewDocs.count { it.scope != "SHARED" && it.ownerUid == memberUid }
                            val sharedCount = dashboardOverviewDocs.count { it.scope == "SHARED" && it.ownerUid == memberUid }
                            val spouseCount = dashboardOverviewDocs.count { it.scope == "SHARED" && it.ownerUid != memberUid }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DocumentDashboardStatCardV2New("Document", dashboardOverviewDocs.size.toString(),
                                    Icons.Outlined.Description, Modifier.weight(1f), Color(0xFFF0E5FF), Color(0xFF7136B8))
                                DocumentDashboardStatCardV2New("Storage", formatDocumentBytesNew(dashboardOverviewDocs.sumOf { it.sizeBytes }),
                                    Icons.Outlined.Storage, Modifier.weight(1f), Color(0xFFE3F0FF), Color(0xFF2875D0))
                                DocumentDashboardStatCardV2New("Only Me", personalCount.toString(),
                                    Icons.Outlined.Person, Modifier.weight(1f), Color(0xFFE7F7E9), Color(0xFF269B52))
                                DocumentDashboardStatCardV2New("Shared", sharedCount.toString(),
                                    Icons.Outlined.People, Modifier.weight(1f), Color(0xFFFFEEE5), Color(0xFFEF7B35))
                                DocumentDashboardStatCardV2New("Spouse", spouseCount.toString(),
                                    Icons.Outlined.People, Modifier.weight(1f), Color(0xFFFCE8F5), Color(0xFFD63384))
                            }
                        }
                    }
                }
            } else if (!recycleBin) {

                item(key = "ownership_filters") {
                    val filters = listOf(
                        Triple("ALL", "All", Icons.Outlined.SelectAll),
                        Triple("ONLY_ME", "Only Me", Icons.Outlined.Person),
                        Triple("SHARED", "Shared", Icons.Outlined.People),
                        Triple("SPOUSE", "Spouse", Icons.Outlined.People)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        filters.forEach { (value, label, icon) ->
                            val selected = dashboardFilter == value
                            val tileBackground = when (value) {
                                "ALL" -> Color(0xFFF0E5FF)
                                "ONLY_ME" -> Color(0xFFE7F7E9)
                                "SHARED" -> Color(0xFFFFEEE5)
                                "SPOUSE" -> Color(0xFFFCE8F5)
                                else -> Color(0xFFE3F0FF)
                            }
                            val tileAccent = when (value) {
                                "ALL" -> Color(0xFF7136B8)
                                "ONLY_ME" -> Color(0xFF269B52)
                                "SHARED" -> Color(0xFFEF7B35)
                                "SPOUSE" -> Color(0xFFD63384)
                                else -> Color(0xFF2875D0)
                            }
                            Surface(
                                modifier = Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(16.dp)).clickable(
                                    indication = ripple(bounded = true, color = tileAccent),
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    recycleBin = false
                                    dashboardFilter = value
                                    visibility = "ALL"
                                    selectionMode = false; selectedDocumentIds.clear()
                                },
                                shape = RoundedCornerShape(16.dp), color = tileBackground,
                                border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) tileAccent else tileAccent.copy(alpha = 0.18f)),
                                shadowElevation = if (selected) 3.dp else 0.dp
                            ) {
                                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(icon, null, tint = tileAccent, modifier = Modifier.size(23.dp))
                                    Text(label, color = Color(0xFF211536), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
            if (showDocumentSearch && !recycleBin) item(key = "search_spacing") { Spacer(Modifier.height(12.dp)) }
            if (showDocumentSearch && !recycleBin) item(key = "search") {
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search documents") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { advancedSearchExpanded = !advancedSearchExpanded }) { Icon(Icons.Outlined.Tune, "Advanced search") }
                            if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = palette.surface, unfocusedContainerColor = palette.surface,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent
                    )
                )
            }
            if (advancedSearchExpanded && showDocumentSearch && !recycleBin) {
                item(key = "advanced_search_options") {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = palette.surface)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Advanced Search", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = searchFileNameOnly, onCheckedChange = { searchFileNameOnly = it })
                                Text("Search filename only", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Search matches title, filename, notes and category.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        }
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
                val recent = active.filter(dashboardScoped)
                    .sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }.take(5)
                item(key = "cat_header") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Categories",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                newCategory = ""
                                categoryCreateError = null
                                categoryDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Category", color = documentAccent)
                        }
                    }
                }
                items(allCategories.chunked(2)) { categoryRow ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryRow.forEach { (id, name) ->
                            val count = active.count {
                                it.categoryId == id && dashboardScoped(it)
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
                        if (categoryRow.size == 1) Spacer(Modifier.weight(1f))
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
            } else {
                // ── Category / folder / all / recycle bin ──
                if (recycleBin) {
                    item(key = "recycle_bin_overview") {
                        val totalBytes = deleted.sumOf { it.sizeBytes }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, documentAccent.copy(alpha = 0.22f))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(Modifier.size(58.dp), shape = RoundedCornerShape(18.dp),
                                        color = if (isDarkUi) Color(0xFF40252D) else Color(0xFFFFE8E8)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.Delete, null, tint = Color(0xFFE36A75),
                                                modifier = Modifier.size(30.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Recycle Bin", style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold)
                                        Text("Deleted files stay here for 30 days",
                                            color = palette.muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(deleted.size.toString(), style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold)
                                        Text("Deleted items", color = palette.muted, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(formatDocumentBytesNew(totalBytes), style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold)
                                        Text("Storage used", color = palette.muted, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text("Items are automatically and permanently deleted 30 days after they were moved here.",
                                    color = palette.muted, style = MaterialTheme.typography.bodySmall)
                            }
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
                    if (showRecycleBinSearch) item(key = "recycle_bin_search") {
                        TextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Search deleted files") },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                            trailingIcon = {
                                if (search.isNotEmpty()) {
                                    IconButton(onClick = { search = "" }) {
                                        Icon(Icons.Outlined.Close, "Clear search")
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
                }
                if (inCategory && !recycleBin) {
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
                    if (childFolders.isNotEmpty()) item(key = "folders_header") {
                        Text(
                            "Folders",
                            Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val folderSlots = childFolders
                    items(folderSlots.chunked(2)) { slotRow ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            slotRow.forEach { folder ->
                                FolderCardV2New(
                                        folder = folder,
                                        palette = palette,
                                        fileCount = active.count { it.folderId == folder.id },
                                        subfolderCount = folders.count { it.parentFolderId == folder.id },
                                        modifier = Modifier.weight(1f),
                                        onOpen = { currentFolderId = folder.id },
                                        onMore = { selectedFolder = folder; showFolderActions = true }
                                    )
                            }
                            repeat(2 - slotRow.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                item(key = "files_header") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (!recycleBin) Text(
                                if (category == "all") "All files" else "Files",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                            )
                            Text("${visible.size} ${if (recycleBin) "items" else "files"}",
                                color = mutedColor, style = MaterialTheme.typography.bodySmall)
                        }
                        Box {
                            TextButton(onClick = { sortMenu = true }) {
                                Text(sortLabelV2New(sort), color = documentAccent)
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Show sorting options", tint = documentAccent)
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

                        IconButton(onClick = { grid = !grid }) {
                            Icon(if (grid) Icons.Outlined.List else Icons.Outlined.GridView, "Toggle view")
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
                                    if (recycleBin) {
                                        TextButton(onClick = {
                                            selectionMode = false
                                            selectedDocumentIds.clear()
                                        }) { Text("Done") }
                                    }
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
                                if (recycleBin && visible.any {
                                    it.id in selectedDocumentIds && it.ownerUid == memberUid && it.scope != "SHARED"
                                }) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { runBulkRestore() },
                                            enabled = selectedDocumentIds.isNotEmpty() && !busyDocument,
                                            modifier = Modifier.weight(1f).heightIn(min = 58.dp)
                                        ) {
                                            Icon(Icons.Outlined.Restore, null)
                                            Spacer(Modifier.width(7.dp))
                                            Text("Restore", fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { showBulkPermanentDeleteConfirm = true },
                                            enabled = selectedDocumentIds.isNotEmpty() && !busyDocument,
                                            modifier = Modifier.weight(1.35f).heightIn(min = 58.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDarkUi) Color(0xFF5B203B) else Color(0xFFFFE3EA),
                                                contentColor = if (isDarkUi) Color(0xFFFF9BB2) else Color(0xFFB42345)
                                            )
                                        ) {
                                            Icon(Icons.Outlined.DeleteForever, null)
                                            Spacer(Modifier.width(7.dp))
                                            Text("Delete Permanently", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!recycleBin) item(key = "favourites_filter") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FilterChip(
                            selected = showFavouritesOnly,
                            onClick = { showFavouritesOnly = !showFavouritesOnly },
                            label = { Text("Favourites") },
                            leadingIcon = { Icon(Icons.Outlined.Favorite, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                if (visible.isEmpty()) {
                    item(key = "empty") {
                        EmptyStateV2New(
                            palette,
                            if (recycleBin) {
                                if (currentFolderId == null) "No deleted files" else "No deleted files in this folder"
                            } else if (search.isNotBlank()) "No matching documents" else "No documents found",
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
                                    onLongClick = {
                                        if (!selectionMode) selectionMode = true
                                        toggleSelection(doc)
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
                            onLongClick = {
                                if (!selectionMode) selectionMode = true
                                toggleSelection(doc)
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
            sheetState = addOptionsSheetState,
            containerColor = documentSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Add to Documents", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                ListItem(
                    headlineContent = { Text("Upload Files", fontWeight = FontWeight.SemiBold) },                    supportingContent = { Text("From device (PDF, images, documents, etc.)") },
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
        FullScreenDocumentForm(
            dark = isDarkUi,
            fullScreenPage = true,
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
            header = {
                VaultCompactHeader(
                    title = "Add Documents",
                    subtitle = null,
                    onBack = {
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
                    titleIcon = null,
                    actions = {}
                )
            },
            contentFillsSpace = pendingUploads.isNotEmpty() || uploading,
            content = {
                if (pendingUploads.isEmpty() && !uploading) {
                    val addPalette = documentPaletteV2New(isDarkUi)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = addPalette.container)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.Info, null, tint = addPalette.accent,
                                            modifier = Modifier.size(22.dp))
                                        Text(
                                            "0 of $MAX_DOCUMENT_BATCH_COUNT_NEW files selected",
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDarkUi) Color(0xFFF7F1FF) else Color(0xFF281638)
                                        )
                                    }
                                    Text("Total · 0 B", color = addPalette.accent, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Limits: 50 MB per file · 200 MB per batch",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = addPalette.muted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    VaultExternalActivityState.documentPickerActive = true
                                    picker.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = addPalette.accent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, addPalette.line)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add New File")
                            }
                        }
                    }
                } else {
                Column(Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(uploadFormScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val addPalette = documentPaletteV2New(isDarkUi)
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = addPalette.container)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Info, null, tint = addPalette.accent, modifier = Modifier.size(26.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${pendingUploads.size} of $MAX_DOCUMENT_BATCH_COUNT_NEW files selected", fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkUi) Color(0xFFF7F1FF) else Color(0xFF281638))
                                Text("Limits: 50 MB per file · 200 MB per batch", style = MaterialTheme.typography.labelSmall,
                                    color = addPalette.muted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total", style = MaterialTheme.typography.labelSmall, color = addPalette.muted)
                                Text(formatDocumentBytesNew(pendingUploads.sumOf { it.sizeBytes }),
                                    color = addPalette.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (!uploading) {
                    if (pendingUploads.size < MAX_DOCUMENT_BATCH_COUNT_NEW) {
                        OutlinedButton(
                            onClick = {
                                VaultExternalActivityState.documentPickerActive = true
                                picker.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = addPalette.accent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, addPalette.line)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (pendingUploads.isEmpty()) "Add Files" else "Add New File")
                        }
                    }
                    pendingUploads.forEachIndexed { index, pending ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, addPalette.line),
                            colors = CardDefaults.cardColors(containerColor = addPalette.surface2)
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
                                        style = MaterialTheme.typography.bodySmall, maxLines = 1,
                                        color = if (isDarkUi) Color(0xFFF7F1FF) else Color(0xFF281638))
                                    Text(formatDocumentBytesNew(pending.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = addPalette.muted)
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
                                trailingIcon = {
                                    if (pending.title.isNotEmpty()) IconButton(enabled = !uploading, onClick = {
                                        pendingUploads[index] = pending.copy(title = "")
                                    }) { Icon(Icons.Outlined.Close, contentDescription = "Clear file name") }
                                },
                                singleLine = true,
                                enabled = !uploading,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
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
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                            )
                            Text("Access", style = MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("PERSONAL" to "Personal", "SHARED" to "Shared").forEach { (value, label) ->
                                    val selected = pending.scope == value
                                    val selectorShape = RoundedCornerShape(16.dp)
                                    Row(Modifier.weight(1f).height(56.dp).clip(selectorShape)
                                        .background(if (selected) addPalette.container else addPalette.surface2)
                                        .border(1.dp, if (selected) addPalette.accent else addPalette.line, selectorShape)
                                        .clickable(enabled = !uploading,
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = Color(0xFFD9B5FF))) {
                                            pendingUploads[index] = pending.copy(scope = value, folderId = null)
                                        }.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(if (value == "PERSONAL") Icons.Outlined.Person else Icons.Outlined.People,
                                            null, tint = addPalette.accent, modifier = Modifier.size(19.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text(label, color = if (isDarkUi) Color(0xFFF7F1FF) else Color(0xFF281638),
                                            fontSize = 14.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                }
                            }
                            var categoryMenuExpanded by remember(pending.uri) { mutableStateOf(false) }
                            Text("Category", style = MaterialTheme.typography.labelMedium)
                            Box {
                                OutlinedButton(
                                    onClick = { categoryMenuExpanded = true },
                                    enabled = !uploading,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = addPalette.muted)
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
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = addPalette.muted)
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
                        Text(
                            "Uploading files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        pendingUploads.forEachIndexed { index, pending ->
                            val itemProgress = when {
                                index < currentUploadIndex -> 1f
                                index == currentUploadIndex -> currentFileProgress.coerceIn(0f, 1f)
                                else -> 0f
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, addPalette.line),
                                colors = CardDefaults.cardColors(containerColor = addPalette.surface)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DocumentTypeIconNew(
                                            if (pending.fileName.endsWith(".pdf", true)) "application/pdf" else "application/octet-stream",
                                            Modifier.size(42.dp)
                                        )
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                pending.fileName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isDarkUi) Color(0xFFF7F1FF) else Color(0xFF281638)
                                            )
                                            Text(
                                                when {
                                                    index < currentUploadIndex -> "${formatDocumentBytesNew(pending.sizeBytes)} · Completed"
                                                    index == currentUploadIndex -> "${formatDocumentBytesNew((pending.sizeBytes * itemProgress).toLong())} · ${(itemProgress * 100).toInt()}%"
                                                    else -> "${formatDocumentBytesNew(pending.sizeBytes)} · Pending…"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = addPalette.muted
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { itemProgress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = addPalette.accent,
                                        trackColor = addPalette.container
                                    )
                                }
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
                }
            },
            primaryAction = {
                if (pendingUploads.isNotEmpty() || uploading) Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9137D7), contentColor = Color.White),
                    enabled = !uploading && pendingUploads.any { !it.retryBlocked } &&
                        pendingUploads.filter { !it.retryBlocked }.all { it.title.isNotBlank() },
                    onClick = {
                        val retryableItems = pendingUploads.filter { !it.retryBlocked }
                        val payload = JSONArray().apply {
                            retryableItems.forEach { pending ->
                                put(
                                    JSONObject()
                                        .put("uri", pending.uri.toString())
                                        .put("fileName", pending.fileName)
                                        .put("sizeBytes", pending.sizeBytes)
                                        .put("title", pending.title.trim())
                                        .put("notes", pending.notes.trim())
                                        .put("categoryId", pending.categoryId)
                                        .put("scope", pending.scope)
                                        .put("folderId", pending.folderId)
                                )
                            }
                        }
                        uploading = true
                        progress = 0f
                        currentFileProgress = 0f
                        currentUploadIndex = 0
                        DocumentUploadForegroundService.startUploads(
                            context = context,
                            vaultId = vaultId,
                            ownerUid = memberUid,
                            uploads = payload
                        )
                    }
                ) { Text(if (uploading) "Uploading…" else "Upload", fontWeight = FontWeight.SemiBold) }
            },
            secondaryAction = {
                if (pendingUploads.isNotEmpty() || uploading) Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCC0F5), contentColor = Color(0xFF30203F)),
                    enabled = true,
                    onClick = {
                        if (uploading) {
                            DocumentUploadForegroundService.cancelAndStop(context)
                        }
                        showAddDocuments = false
                        pendingUploads.clear()
                        pendingPhotoFile?.delete()
                        pendingPhotoFile = null
                        pendingPhotoUri = null
                        capturedPhotoUri = null
                        pendingScanFile?.delete()
                        pendingScanFile = null
                    }
                ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
            }
        )
    }

    if (categoryDialog) {
        AlertDialog(

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
            onDismissRequest = { if (!categoryCreateSaving) categoryDialog = false },
            title = { Text("Add New Category", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it.take(30); categoryCreateError = null },
                        modifier = Modifier.fillMaxWidth().offset(x = categoryCreateShake.value.dp),
                        label = { Text("New Category Name") },
                        singleLine = true,
                        isError = categoryCreateError != null,
                        enabled = !categoryCreateSaving,
                        trailingIcon = {
                            if (newCategory.isNotEmpty()) {
                                IconButton(onClick = { newCategory = ""; categoryCreateError = null }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear category name")
                                }
                            }
                        },
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    categoryCreateError.orEmpty(),
                                    color = if (categoryCreateError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("${newCategory.length}/30", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                    if (categoryCreateSaving) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = documentAccent)
                        Text("Saving category…", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !categoryCreateSaving,
                    onClick = {
                        val clean = newCategory.trim()
                        val duplicate = customCategories.any { it.second.equals(clean, true) } ||
                            BuiltInDocumentCategoriesV2.all.any { it.name.equals(clean, true) }
                        val validation = when {
                            clean.isEmpty() -> "Category Name is mandatory"
                            duplicate -> "Category Name is already available"
                            else -> null
                        }
                        if (validation != null) {
                            categoryCreateError = validation
                            categoryCreateValidationAttempt++
                        } else {
                            scope.launch {
                                categoryCreateSaving = true
                                try {
                                    repository.createCategory(vaultId, clean)
                                    newCategory = ""
                                    categoryCreateError = null
                                    categoryDialog = false
                                    message = "Category created successfully."
                                } catch (error: Exception) {
                                    if (error is FirebaseFunctionsException &&
                                        error.code == FirebaseFunctionsException.Code.ALREADY_EXISTS
                                    ) {
                                        categoryCreateError = "Category Name is already available"
                                        categoryCreateValidationAttempt++
                                    } else {
                                        message = error.localizedMessage ?: error.message ?: "Unable to create category."
                                    }
                                } finally {
                                    categoryCreateSaving = false
                                }
                            }
                        }
                    }
                ) {
                    if (categoryCreateSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Create")
                }
            },
            dismissButton = {
                TextButton(enabled = !categoryCreateSaving, onClick = { categoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (manageCategories) {
        AlertDialog(

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
            onDismissRequest = {
                if (categoryDeletingId == null) {
                    manageCategories = false
                    categoryToEdit = null
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = documentAccent.copy(alpha = 0.18f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = documentAccent,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "Manage Custom Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Organise your documents your way",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (customCategories.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = RoundedCornerShape(21.dp),
                                    color = documentAccent.copy(alpha = 0.14f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.FolderOpen,
                                            contentDescription = null,
                                            tint = documentAccent,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                Text(
                                    "No custom categories yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Create your first category to organise your documents.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                        .heightIn(min = 44.dp),
                                    enabled = categoryDeletingId == null,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = documentAccent,
                                        contentColor = palette.onAccent
                                    ),
                                    onClick = {
                                        manageCategories = false
                                        newCategory = ""
                                        categoryCreateError = null
                                        categoryDialog = true
                                    }
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create Category", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${customCategories.size} ${if (customCategories.size == 1) "category" else "categories"}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                enabled = categoryDeletingId == null,
                                onClick = {
                                    manageCategories = false
                                    newCategory = ""
                                    categoryCreateError = null
                                    categoryDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Category", color = documentAccent)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 10.dp)
                                    .verticalScroll(manageCategoriesScrollState),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                            customCategories.forEachIndexed { index, (id, name) ->
                                val categoryIconColors = listOf(
                                    Color(0xFFB88AF5),
                                    Color(0xFF83D7A1),
                                    Color(0xFFF2C76C),
                                    Color(0xFF82B8F5),
                                    Color(0xFFE59AD8)
                                )
                                val categoryTint = categoryIconColors[index % categoryIconColors.size]
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(36.dp),
                                            shape = RoundedCornerShape(13.dp),
                                            color = categoryTint.copy(alpha = 0.16f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.FolderOpen,
                                                    contentDescription = null,
                                                    tint = categoryTint,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            name,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 4,
                                            overflow = TextOverflow.Clip
                                        )
                                        IconButton(
                                            modifier = Modifier.size(36.dp),
                                            enabled = categoryDeletingId == null,
                                            onClick = {
                                                categoryToEdit = id to name
                                                categoryEditName = name
                                                categoryRenameError = null
                                            }
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "Rename $name",
                                                tint = documentAccent
                                            )
                                        }
                                        IconButton(
                                            modifier = Modifier.size(36.dp),
                                            enabled = categoryDeletingId == null,
                                            onClick = {
                                                scope.launch {
                                                    categoryDeletingId = id
                                                    try {
                                                        repository.deleteCategory(vaultId, id)
                                                        if (category == id) category = "all"
                                                        manageCategories = false
                                                        categoryToEdit = null
                                                        message = "Category deleted successfully."
                                                    } catch (error: Exception) {
                                                        message = error.localizedMessage ?: error.message
                                                            ?: "Reassign documents before deleting this category."
                                                    } finally {
                                                        categoryDeletingId = null
                                                    }
                                                }
                                            }
                                        ) {
                                            if (categoryDeletingId == id) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = "Delete $name",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (manageCategoriesScrollState.maxValue > 0) {
                            val scrollProgress = (
                                manageCategoriesScrollState.value.toFloat() /
                                    manageCategoriesScrollState.maxValue.toFloat()
                                ).coerceIn(0f, 1f)
                            BoxWithConstraints(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                val thumbTravel = (maxHeight - 34.dp).coerceAtLeast(0.dp)
                                Box(
                                    modifier = Modifier
                                        .offset(y = thumbTravel * scrollProgress)
                                        .width(4.dp)
                                        .height(34.dp)
                                        .background(documentAccent, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = categoryDeletingId == null,
                        onClick = { manageCategories = false }
                    ) {
                        Text("Done", color = documentAccent)
                    }
                }
            }
        )
    }

    categoryToEdit?.let { (id, oldName) ->
        AlertDialog(

            shape = RoundedCornerShape(24.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            tonalElevation = 8.dp,
            onDismissRequest = { if (!categoryRenameSaving) categoryToEdit = null },
            title = { Text("Rename Category", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = categoryEditName,
                        onValueChange = { categoryEditName = it.take(30); categoryRenameError = null },
                        modifier = Modifier.fillMaxWidth().offset(x = categoryRenameShake.value.dp),
                        label = { Text("New Category Name") },
                        singleLine = true,
                        isError = categoryRenameError != null,
                        enabled = !categoryRenameSaving,
                        trailingIcon = {
                            if (categoryEditName.isNotEmpty()) {
                                IconButton(onClick = { categoryEditName = ""; categoryRenameError = null }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear category name")
                                }
                            }
                        },
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    categoryRenameError.orEmpty(),
                                    color = if (categoryRenameError != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant                                )
                                Text("${categoryEditName.length}/30", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                    if (categoryRenameSaving) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = documentAccent)
                        Text("Saving category…", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !categoryRenameSaving,
                    onClick = {
                        val clean = categoryEditName.trim()
                        val validation = when {
                            clean.isEmpty() -> "Category Name is mandatory"
                            clean == oldName -> "Category Name is not modified"
                            customCategories.any { it.first != id && it.second.equals(clean, true) } ||
                                BuiltInDocumentCategoriesV2.all.any { it.name.equals(clean, true) } ->
                                "Category Name is already available"
                            else -> null
                        }
                        if (validation != null) {
                            categoryRenameError = validation
                            categoryRenameValidationAttempt++
                        } else {
                            scope.launch {
                                categoryRenameSaving = true
                                try {
                                    repository.renameCategory(vaultId, id, clean)
                                    categoryToEdit = null
                                    categoryRenameError = null
                                    message = "Category renamed successfully."
                                } catch (error: Exception) {
                                    if (error is FirebaseFunctionsException &&
                                        error.code == FirebaseFunctionsException.Code.ALREADY_EXISTS
                                    ) {
                                        categoryRenameError = "Category Name is already available"
                                        categoryRenameValidationAttempt++
                                    } else {
                                        message = error.localizedMessage ?: error.message ?: "Rename failed."
                                    }
                                } finally {
                                    categoryRenameSaving = false
                                }
                            }
                        }
                    }
                ) {
                    if (categoryRenameSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save")
                }
            },
            dismissButton = {
                TextButton(enabled = !categoryRenameSaving, onClick = { categoryToEdit = null }) { Text("Cancel") }
            }
        )
    }

    if (showFolderActions) {
        val folder = selectedFolder
        if (folder != null) {
            ModalBottomSheet(
                onDismissRequest = { showFolderActions = false },
                shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
                containerColor = if (isDarkUi) Color(0xFF241D30) else Color(0xFFF8F3FF),
                dragHandle = null
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                        .background(
                            if (isDarkUi) Color(0xFF241D30) else Color(0xFFF8F3FF)
                        )
                        .padding(horizontal = 22.dp)
                        .padding(top = 10.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        Modifier.align(Alignment.CenterHorizontally)
                            .width(34.dp).height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.accent.copy(alpha = 0.85f))
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(42.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (isDarkUi) Color(0xFF594078) else Color(0xFFE9DDFB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = if (isDarkUi) Color(0xFFD7B5FF) else Color(0xFF7136B8),
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                folder.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Folder Options",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(palette.surface2)
                                .border(1.dp, palette.line, RoundedCornerShape(18.dp))
                                .clickable {
                                    renameFolderName = folder.name
                                    showFolderActions = false
                                    showRenameFolder = true
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(palette.container),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Edit, null, tint = palette.accent, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Rename Folder", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Change this folder's name", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                                .background(palette.surface2)
                                .border(1.dp, palette.line, RoundedCornerShape(22.dp))
                                .clickable {
                                    moveParentFolderId = folder.parentFolderId
                                    moveTargetChosen = false
                                    showFolderActions = false
                                    showMoveFolder = true
                                }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(palette.container),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.DriveFileMove, null, tint = palette.accent, modifier = Modifier.size(25.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Move Folder", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Move with all subfolders and files", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                                .clickable {
                                    showFolderActions = false
                                    deleteFolderError = null
                                    showDeleteFolderConfirm = true
                                }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(25.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Delete Empty Folder", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                Text("Available only when no files or subfolders remain",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
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
        val parentScope = currentFolderId?.let { id -> folders.firstOrNull { it.id == id }?.scope }
        val folderSurface = if (isDarkUi) Color(0xFF241D30) else Color(0xFFF8F3FF)
        val folderWaveBack = if (isDarkUi) Color(0xFF63347F) else Color(0xFFDCC0F5)
        val folderWaveFront = if (isDarkUi) Color(0xFF8D52B4) else Color(0xFFE9D5FA)
        val folderAccent = if (isDarkUi) Color(0xFFD7B5FF) else Color(0xFF7136B8)

        Dialog(
            onDismissRequest = { if (!busyDocument) showCreateFolder = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val dialogShape = RoundedCornerShape(30.dp)
            Box(
                Modifier.fillMaxWidth(0.92f).clip(dialogShape).background(folderSurface)
                    .drawBehind {
                        val waveHeight = 48.dp.toPx()
                        val backWave = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height - waveHeight * 0.78f)
                            cubicTo(size.width * 0.24f, size.height - waveHeight * 1.35f,
                                size.width * 0.36f, size.height - waveHeight * 0.05f,
                                size.width * 0.58f, size.height - waveHeight * 0.45f)
                            cubicTo(size.width * 0.78f, size.height - waveHeight * 0.85f,
                                size.width * 0.86f, size.height - waveHeight * 1.18f,
                                size.width, size.height - waveHeight * 0.62f)
                            lineTo(size.width, size.height); lineTo(0f, size.height); close()
                        }
                        drawPath(backWave, folderWaveBack)
                        val frontWave = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height - waveHeight * 0.38f)
                            cubicTo(size.width * 0.22f, size.height - waveHeight * 0.85f,
                                size.width * 0.42f, size.height - waveHeight * 0.02f,
                                size.width * 0.62f, size.height - waveHeight * 0.28f)
                            cubicTo(size.width * 0.80f, size.height - waveHeight * 0.50f,
                                size.width * 0.88f, size.height - waveHeight * 0.68f,
                                size.width, size.height - waveHeight * 0.32f)
                            lineTo(size.width, size.height); lineTo(0f, size.height); close()
                        }
                        drawPath(frontWave, folderWaveFront)
                    }
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        .padding(top = 26.dp, bottom = 76.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(66.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkUi) Color(0xFF594078) else Color(0xFFE9DDFB)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.FolderOpen, null, tint = folderAccent, modifier = Modifier.size(38.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Create Folder", style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Add a new folder to organize your files",
                                style = MaterialTheme.typography.bodyMedium, color = folderAccent)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Folder Name", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it.take(30); createFolderError = null },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter folder name") },
                            supportingText = { Text("${newFolderName.length}/30") },
                            singleLine = true, shape = RoundedCornerShape(16.dp)
                        )
                    }

                    if (category == "all" && currentFolderId == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Category", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            Box {
                                OutlinedButton(onClick = { folderCategoryMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(16.dp)) {
                                    Icon(Icons.Outlined.GridView, null, tint = folderAccent)
                                    Spacer(Modifier.width(12.dp))
                                    Text(allCategories.firstOrNull { it.first == newFolderCategory }?.second ?: "Other",
                                        modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Choose category")
                                }
                                DropdownMenu(expanded = folderCategoryMenuExpanded,
                                    onDismissRequest = { folderCategoryMenuExpanded = false }) {
                                    allCategories.forEach { (id, label) ->
                                        DropdownMenuItem(text = { Text(label) }, onClick = {
                                            newFolderCategory = id; folderCategoryMenuExpanded = false
                                        })
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Visibility", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        if (parentScope != null) {
                            Surface(shape = RoundedCornerShape(14.dp), color = palette.surface2) {
                                Text(if (parentScope == "SHARED") "Shared (inherited from parent folder)"
                                    else "Personal (inherited from parent folder)",
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf("PERSONAL" to "Personal", "SHARED" to "Shared").forEach { (value, label) ->
                                    val selected = newFolderScope == value
                                    Surface(
                                        modifier = Modifier.weight(1f).height(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple()
                                            ) { newFolderScope = value },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) folderAccent.copy(alpha = 0.28f) else palette.surface2,
                                        border = androidx.compose.foundation.BorderStroke(1.dp,
                                            if (selected) folderAccent else palette.line)) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center) {
                                            Icon(if (value == "PERSONAL") Icons.Outlined.Person else Icons.Outlined.People,
                                                null, tint = folderAccent, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(label, color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        if (currentFolderId == null) "Folder will be created inside " +
                            (allCategories.firstOrNull { it.first == (if (category == "all") newFolderCategory else category) }?.second ?: "this category") + "."
                        else "Folder will be created inside " +
                            (folders.firstOrNull { it.id == currentFolderId }?.name ?: "the selected folder") + ".",
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(palette.surface2).padding(14.dp),
                        color = folderAccent, style = MaterialTheme.typography.bodySmall
                    )

                    createFolderError?.let { errorText ->
                        Snackbar(modifier = Modifier.fillMaxWidth(), containerColor = Color(0xFFC62828),
                            contentColor = Color.White) { Text(errorText) }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showCreateFolder = false },
                            enabled = !busyDocument,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkUi) Color(0xFFDCC0F5) else Color(0xFFE9D5FA),
                                contentColor = if (isDarkUi) Color(0xFF30203F) else Color(0xFF54217F)
                            )
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    busyDocument = true
                                    try {
                                        repository.createFolder(vaultId, memberUid, newFolderName,
                                            if (category == "all") newFolderCategory else category,
                                            currentFolderId, newFolderScope)
                                        showCreateFolder = false
                                        createFolderError = null
                                        message = "Folder created."
                                    } catch (error: Exception) {
                                        val failure = error.localizedMessage ?: error.message ?: "Unable to create folder."
                                        createFolderError = failure
                                        triggerVaultErrorVibration(context)
                                    } finally { busyDocument = false }
                                }
                            },
                            enabled = newFolderName.trim().isNotEmpty() && !busyDocument,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkUi) Color(0xFF8D43D7) else Color(0xFF8A35D1),
                                contentColor = Color.White)
                        ) {
                            if (busyDocument) CircularProgressIndicator(Modifier.size(20.dp),
                                color = Color.White, strokeWidth = 2.dp)
                            else Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
            containerColor = if (isDarkUi) Color(0xFF1D1725) else Color(0xFFF7F1FC),
            dragHandle = null
        ) {
            Column(
                Modifier.fillMaxWidth().then(
                    if (doc.isDeleted) Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)
                    else Modifier
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (doc.isDeleted) {
                    val categoryName = allCategories.firstOrNull { it.first == doc.categoryId }?.second
                        ?: customCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other"
                    val originalLocation = buildString {
                        append("Documents / ")
                        append(if (doc.scope == "SHARED") "Shared" else "Personal")
                        val folderNames = mutableListOf<String>()
                        var folderCursor = doc.folderId
                        val visitedFolderIds = mutableSetOf<String>()
                        while (folderCursor != null && visitedFolderIds.add(folderCursor)) {
                            val folder = folders.firstOrNull { it.id == folderCursor } ?: break
                            folderNames.add(0, folder.name)
                            folderCursor = folder.parentFolderId
                        }
                        folderNames.forEach { append(" / "); append(it) }
                    }
                    val deletedAtText = doc.deletedAt?.toDate()?.let {
                        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(it)
                    } ?: "—"
                    val purgeAtText = doc.purgeAt?.toDate()?.let {
                        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(it)
                    } ?: "—"
                    val remainingDays = doc.purgeAt?.toDate()?.time?.let {
                        kotlin.math.ceil((it - System.currentTimeMillis()).coerceAtLeast(0L) / 86400000.0).toInt()
                    }
                    val typeLabel = when {
                        doc.contentType.contains("pdf", true) -> "PDF Document"
                        doc.contentType.startsWith("image/") -> "${doc.contentType.substringAfter('/').uppercase()} Image"
                        doc.contentType.contains("word", true) -> "Word Document"
                        doc.contentType.contains("sheet", true) || doc.contentType.contains("excel", true) -> "Spreadsheet"
                        else -> doc.contentType.substringAfter('/').replace('-', ' ').replace('_', ' ')
                            .replaceFirstChar { it.uppercase() } + " File"
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Surface(
                                Modifier.width(42.dp).height(5.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            ) {}
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DocumentTypeIconNew(doc.contentType, Modifier.size(64.dp))
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    documentDisplayNameV2New(doc),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 4,
                                    overflow = TextOverflow.Clip,
                                    softWrap = true
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ScopePillV2New(doc, doc.ownerUid == memberUid, documentPaletteV2New(isDarkUi))
                                    val typeBadgeColors = when {
                                        doc.contentType.contains("pdf", true) ->
                                            (if (isDarkUi) Color(0xFF4A2020) else Color(0xFFFFE3E3)) to
                                                (if (isDarkUi) Color(0xFFFF9A9A) else Color(0xFFC42020))
                                        doc.contentType.startsWith("image/") ->
                                            (if (isDarkUi) Color(0xFF1E3350) else Color(0xFFDDEBFF)) to
                                                (if (isDarkUi) Color(0xFF9CC4FF) else Color(0xFF1D63C2))
                                        doc.contentType.contains("spreadsheet", true) ||
                                            doc.contentType.contains("excel", true) || doc.contentType.contains("sheet", true) ->
                                            (if (isDarkUi) Color(0xFF1B3A2A) else Color(0xFFDDF4E5)) to
                                                (if (isDarkUi) Color(0xFF9BE3BA) else Color(0xFF13733F))
                                        else ->
                                            (if (isDarkUi) Color(0xFF302447) else Color(0xFFEBE2FF)) to
                                                (if (isDarkUi) Color(0xFFCDB5FF) else Color(0xFF5B2FB5))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = typeBadgeColors.first
                                    ) {
                                        Text(
                                            typeLabel,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = typeBadgeColors.second,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { selectedDocument = null }) {
                                Icon(Icons.Outlined.Close, "Close")
                            }
                        }
                        Column(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDarkUi) Color(0xFF241C30) else palette.surface)
                                .border(
                                    1.dp,
                                    documentAccent.copy(alpha = if (isDarkUi) 0.42f else 0.22f),
                                    RoundedCornerShape(20.dp)
                                ),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            @Composable
                            fun detailRow(label: String, value: String, icon: ImageVector, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(icon, null, tint = if (isDarkUi) Color(0xFFD2A8FF) else documentAccent,
                                        modifier = Modifier.size(24.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(label, style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(value, style = MaterialTheme.typography.bodyLarge,
                                            color = valueColor, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (label != "Size") HorizontalDivider(
                                    Modifier.padding(start = 54.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            }
                            detailRow("Deleted On", deletedAtText, Icons.Outlined.CalendarToday)
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.History,
                                    null,
                                    tint = if (isDarkUi) Color(0xFFD2A8FF) else documentAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Auto Delete On",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            purgeAtText,
                                            modifier = Modifier.weight(1f, fill = false),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (remainingDays != null) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (remainingDays <= 0) "Due" else "($remainingDays days left)",
                                                color = if (remainingDays <= 0) MaterialTheme.colorScheme.error else Color(0xFF65D82F),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(
                                Modifier.padding(start = 54.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                            detailRow("Original Location", originalLocation, Icons.Outlined.FolderOpen)
                            detailRow("Category", categoryName, Icons.Outlined.FolderOpen)
                            detailRow("Size", formatDocumentBytesNew(doc.sizeBytes), Icons.Outlined.Description)
                        }
                        if (doc.scope != "SHARED" && doc.ownerUid == memberUid) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                modifier = Modifier.weight(1f).height(58.dp),
                                enabled = !busyDocument && doc.ownerUid == memberUid,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkUi) Color(0xFFB86AE8) else documentAccent,
                                    contentColor = Color(0xFF180F20)
                                ),
                                onClick = {
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
                            ) {
                                Icon(Icons.Outlined.Restore, null)
                                Spacer(Modifier.width(7.dp))
                                Text("Restore", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                modifier = Modifier.weight(1.35f).height(58.dp),
                                enabled = !busyDocument && doc.ownerUid == memberUid,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkUi) Color(0xFF5B203B) else Color(0xFFFFE3EA),
                                    contentColor = if (isDarkUi) Color(0xFFFF9BB2) else Color(0xFFB42345)
                                ),
                                onClick = { permanentDeleteTarget = doc }
                            ) {
                                Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Delete Permanently",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        }
                    }
                } else {
                    val fileName = doc.originalFileName.ifBlank { doc.title }
                    val fileIcon = when {
                        doc.contentType.contains("pdf", true) -> Icons.Outlined.PictureAsPdf
                        doc.contentType.startsWith("image/") -> Icons.Outlined.Image
                        else -> Icons.Outlined.InsertDriveFile
                    }
                    val favourite = doc.favouriteUids[memberUid] == true
                    val sheetBackground = if (isDarkUi) Color(0xFF1D1725) else Color(0xFFF7F1FC)
                    val actionCardBackground = if (isDarkUi) Color(0xFF2B2135) else Color(0xFFFFFFFF)
                    val accent = if (isDarkUi) Color(0xFFD4A1FF) else documentAccent
                    val muted = if (isDarkUi) Color(0xFFD0C4D9) else MaterialTheme.colorScheme.onSurfaceVariant
                    val danger = if (isDarkUi) Color(0xFFFF9BB2) else MaterialTheme.colorScheme.error

                    @Composable
                    fun quickAction(modifier: Modifier, icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
                        Column(
                            modifier = modifier.clip(RoundedCornerShape(18.dp))
                                .background(if (isDarkUi) Color(0xFF34263F) else Color(0xFFECE1F8))
                                .clickable(enabled = enabled, onClick = onClick)
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = if (enabled) accent else muted.copy(alpha = 0.45f),
                                modifier = Modifier.size(25.dp))
                            Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else muted,
                                style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                    }

                    @Composable
                    fun actionCard(icon: ImageVector, title: String, subtitle: String, tint: Color = accent,
                                   trailing: Boolean = false, onClick: () -> Unit) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(actionCardBackground)
                                .clickable(enabled = !busyDocument, onClick = onClick)
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(title, color = if (tint == danger) danger else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(subtitle, color = muted, style = MaterialTheme.typography.bodySmall)                            }
                            if (trailing) Icon(Icons.Outlined.ChevronRight, null, tint = muted)
                        }
                    }

                    Box(
                        Modifier.fillMaxWidth()
                            .background(sheetBackground)
                            .drawBehind {
                                val wave = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * 0.965f)
                                    cubicTo(size.width * 0.24f, size.height, size.width * 0.38f, size.height * 0.975f,
                                        size.width * 0.58f, size.height * 0.965f)
                                    cubicTo(size.width * 0.78f, size.height * 0.94f, size.width * 0.88f,
                                        size.height * 0.95f, size.width, size.height * 0.975f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(wave, if (isDarkUi) Color(0xFF563273) else Color(0xFFE5D1F7))
                            }
                    ) {
                        Column(
                            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 42.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                Modifier.align(Alignment.CenterHorizontally).width(42.dp).height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDarkUi) Color(0xFFD7B9EB) else Color(0xFFB69ACB))
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    Modifier.size(76.dp).clip(RoundedCornerShape(22.dp))
                                        .background(if (isDarkUi) Color(0xFF243C5C) else Color(0xFFDCEBFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(fileIcon, null, tint = if (isDarkUi) Color(0xFF9CC4FF) else Color(0xFF2468C5),
                                        modifier = Modifier.size(38.dp))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        fileName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${formatDocumentBytesNew(doc.sizeBytes)}  •  ${doc.contentType.substringAfter('/').uppercase()}",
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                quickAction(Modifier.weight(1f), Icons.Outlined.Visibility, "Preview",
                                    enabled = !busyDocument && !doc.isDeleted &&
                                        (doc.contentType.startsWith("image/") || doc.contentType.contains("pdf", true) ||
                                            doc.originalFileName.endsWith(".pdf", true))) { previewDocument(doc) }
                                quickAction(Modifier.weight(1f), Icons.Outlined.Download, "Download", !busyDocument && !doc.isDeleted) {
                                    downloadDocument(doc)
                                }
                                if (doc.ownerUid == memberUid && !doc.isDeleted) {
                                    quickAction(Modifier.weight(1f), Icons.Outlined.Edit, "Edit", !busyDocument) {
                                        editTitle = doc.title
                                        editDescription = doc.description
                                        editCategory = doc.categoryId
                                        editScope = doc.scope
                                        editingDocument = doc
                                        selectedDocument = null
                                    }
                                }
                                quickAction(Modifier.weight(1f), Icons.Outlined.Share, "Share", !busyDocument && !doc.isDeleted) {
                                    shareDocument(doc)
                                }
                                quickAction(Modifier.weight(1f), Icons.Outlined.Info, "Details", !busyDocument) { showDocumentDetails = true }
                            }

                            if (!doc.isDeleted) {
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                        .background(actionCardBackground)
                                        .clickable(enabled = !busyDocument) {
                                            toggleFavourite(doc)
                                            selectedDocument = null
                                        }
                                        .padding(horizontal = 16.dp, vertical = 17.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(Icons.Outlined.Favorite, null,
                                        tint = if (favourite) Color(0xFFFF83B5) else accent,
                                        modifier = Modifier.size(27.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(if (favourite) "Remove from Favourites" else "Add to Favourites",
                                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                        Text("Only your account's Favourite list is changed", color = muted,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (favourite) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFFFF83B5))
                                    } else {
                                        Icon(Icons.Outlined.ChevronRight, null, tint = muted)
                                    }
                                }
                                if (doc.ownerUid == memberUid) {
                                    actionCard(
                                        Icons.Outlined.People,
                                        "Change Access",
                                        if (doc.scope == "SHARED") "Shared with household members" else "Only you can access this document",
                                        trailing = true
                                    ) {
                                        editTitle = doc.title
                                        editDescription = doc.description
                                        editCategory = doc.categoryId
                                        editScope = doc.scope
                                        editingDocument = doc
                                        selectedDocument = null
                                    }
                                    actionCard(
                                        Icons.Outlined.FolderOpen,
                                        "Move to Folder",
                                        "Organise this file inside a folder in the same category",
                                        trailing = true
                                    ) {
                                        documentToMove = doc
                                        moveDocumentTargetId = doc.folderId
                                        moveDocumentTargetChosen = false
                                        selectedDocument = null
                                        showMoveDocument = true
                                    }
                                    actionCard(
                                        Icons.Outlined.Delete,
                                        "Move to Recycle Bin",
                                        "Keep it recoverable for 30 days",
                                        tint = danger
                                    ) {
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
                                }
                            } else if (doc.ownerUid == memberUid) {
                                actionCard(Icons.Outlined.Restore, "Restore Document", "Return this document to its original location") {
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
                                actionCard(Icons.Outlined.DeleteForever, "Delete Permanently", "This action cannot be undone", tint = danger) {
                                    permanentDeleteTarget = doc
                                }
                            }
                        }
                    }
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
        val darkDownloadUi = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val dialogBackground = if (darkDownloadUi) Color(0xFF211B2B) else Color(0xFFF8F3FF)
        val downloadAccent = if (darkDownloadUi) Color(0xFFD6A5FF) else Color(0xFF7438B8)
        val downloadTrack = if (darkDownloadUi) Color(0xFF49385C) else Color(0xFFE5D7F5)
        val waveColor = if (darkDownloadUi) Color(0xFF563273) else Color(0xFFE5D1F7)
        val fileTypeLabel = downloadingFileType
        val (fileIconBackground, fileIconTint, downloadFileIcon) = when {
            fileTypeLabel == "PDF" -> Triple(
                if (darkDownloadUi) Color(0xFF4A2020) else Color(0xFFFFE3E3),
                if (darkDownloadUi) Color(0xFFFF9A9A) else Color(0xFFC42020),
                Icons.Outlined.PictureAsPdf
            )
            fileTypeLabel in setOf("JPG", "JPEG", "PNG", "GIF", "WEBP", "IMAGE") -> Triple(
                if (darkDownloadUi) Color(0xFF1E3350) else Color(0xFFDDEBFF),
                if (darkDownloadUi) Color(0xFF9CC4FF) else Color(0xFF1D63C2),
                Icons.Outlined.Image
            )
            fileTypeLabel in setOf("XLS", "XLSX", "CSV") -> Triple(
                if (darkDownloadUi) Color(0xFF1B3A2A) else Color(0xFFDDF4E5),
                if (darkDownloadUi) Color(0xFF9BE3BA) else Color(0xFF13733F),
                Icons.Outlined.Description
            )
            fileTypeLabel in setOf("DOC", "DOCX", "RTF") -> Triple(
                if (darkDownloadUi) Color(0xFF1E3350) else Color(0xFFDDEBFF),
                if (darkDownloadUi) Color(0xFF9CC4FF) else Color(0xFF1D63C2),
                Icons.Outlined.Description
            )
            else -> Triple(
                if (darkDownloadUi) Color(0xFF302447) else Color(0xFFEBE2FF),
                if (darkDownloadUi) Color(0xFFCDB5FF) else Color(0xFF5B2FB5),
                Icons.Outlined.InsertDriveFile
            )
        }

        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 620.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(dialogBackground)
                    .drawBehind {
                        val wave = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height * 0.88f)
                            cubicTo(size.width * 0.22f, size.height * 0.99f, size.width * 0.42f, size.height * 0.94f, size.width * 0.58f, size.height * 0.92f)
                            cubicTo(size.width * 0.76f, size.height * 0.87f, size.width * 0.88f, size.height * 0.88f, size.width, size.height * 0.93f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(wave, waveColor)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = fileIconBackground
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    downloadFileIcon,
                                    contentDescription = "$fileTypeLabel document",
                                    tint = fileIconTint,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "Downloading Document",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                            Text(
                                downloadingFileName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "$fileTypeLabel  •  ${if (total > 0L) formatDocumentBytesNew(total) else "Calculating size…"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(50)),
                            color = downloadAccent,
                            trackColor = downloadTrack
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (total > 0L) "${(progressValue * 100).toInt()}% Downloaded" else "Downloading…",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = downloadAccent
                            )
                            Text(
                                "${formatDocumentBytesNew(downloadTransferredBytes)} of ${if (total > 0L) formatDocumentBytesNew(total) else "—"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = { if (!isSavingDownloadedFile) activeDocumentTransferJob?.cancel() },
                        enabled = !isSavingDownloadedFile && activeDocumentTransferJob?.isActive == true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 72.dp).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = waveColor,
                            contentColor = if (darkDownloadUi) Color.White else Color(0xFF5B2FB5),
                            disabledContainerColor = waveColor.copy(alpha = 0.65f),
                            disabledContentColor = (if (darkDownloadUi) Color.White else Color(0xFF5B2FB5)).copy(alpha = 0.75f)
                        )
                    ) {
                        if (isSavingDownloadedFile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = if (darkDownloadUi) Color.White else Color(0xFF5B2FB5)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Finishing download", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cancel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEmptyRecycleBinConfirm) {
        AlertDialog(
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            onDismissRequest = { if (!busyDocument) showEmptyRecycleBinConfirm = false },
            title = { Text("Empty Recycle Bin?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("All deleted documents owned by you will be permanently removed, including their stored files. This cannot be undone.")
                    if (busyDocument) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Emptying Recycle Bin…", style = MaterialTheme.typography.bodySmall)
                    }
                }
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
                            } catch (error: FirebaseFunctionsException) {
                                message = if (error.code == FirebaseFunctionsException.Code.NOT_FOUND) {
                                    "The Empty Recycle Bin Firebase function is not deployed to the active project. Run: firebase deploy --only functions --project personalvault2-development"
                                } else {
                                    error.localizedMessage ?: "Unable to empty Recycle Bin."
                                }
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

    if (showBulkPermanentDeleteConfirm) {
        AlertDialog(
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { if (!busyDocument) showBulkPermanentDeleteConfirm = false },
            title = { Text("Delete selected documents permanently?") },
            text = { Text("The selected personal documents owned by you and their stored files will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !busyDocument,
                    onClick = {
                        showBulkPermanentDeleteConfirm = false
                        runBulkPermanentDelete()
                    }
                ) { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    enabled = !busyDocument,
                    onClick = { showBulkPermanentDeleteConfirm = false }
                ) { Text("Cancel") }
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
                modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.88f),
                properties = DialogProperties(usePlatformDefaultWidth = false),
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                onDismissRequest = { showDocumentDetails = false },
                title = {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("FILE INFORMATION", style = MaterialTheme.typography.labelSmall,
                                color = palette.accent, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                            Text("File Details", style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { showDocumentDetails = false }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close file details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                text = {
                    Column(
                        Modifier.fillMaxWidth().fillMaxHeight(0.94f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                    .background(palette.surface2)
                                    .border(1.dp, palette.line, RoundedCornerShape(20.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DocumentTypeIconNew(doc.contentType, Modifier.size(58.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        documentDisplayNameV2New(doc),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        doc.originalFileName.ifBlank { documentDisplayNameV2New(doc) },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val categoryLabel = allCategories.firstOrNull { it.first == doc.categoryId }?.second
                                ?: customCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other"

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ScopePillV2New(doc, doc.ownerUid == memberUid, palette)
                                Surface(shape = RoundedCornerShape(50), color = palette.container) {
                                    Text(
                                        categoryLabel,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        color = palette.accent,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(
                                Modifier.fillMaxWidth().weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.surface2)
                                    .border(1.dp, palette.line, RoundedCornerShape(20.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "DETAILS",
                                    Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp
                                )
                                DocumentDetailRowV2New(
                                    "File type",
                                    doc.contentType.substringAfter('/').uppercase().ifBlank { "File" },
                                    Icons.Outlined.Description
                                )
                                DocumentDetailRowV2New("File size", formatDocumentBytesNew(doc.sizeBytes), Icons.Outlined.Storage)
                                DocumentDetailRowV2New(
                                    "Location",
                                    "My Documents / $categoryLabel",
                                    Icons.Outlined.FolderOpen
                                )
                                DocumentDetailRowV2New(
                                    "Date added",
                                    doc.uploadedAt?.toDate()?.let {
                                        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(it)
                                    } ?: "—",
                                    Icons.Outlined.CalendarToday
                                )
                                DocumentDetailRowV2New(
                                    "Last modified",
                                    doc.updatedAt?.toDate()?.let {
                                        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(it)
                                    } ?: "—",
                                    Icons.Outlined.History
                                )
                            }
                        }

                        Button(
                            onClick = { showDocumentDetails = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = palette.onAccent
                            )
                        ) {
                            Text("Done", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }

    editingDocument?.let { doc ->
        val isEditBusy = busyDocument || replacingDocument
        val editPalette = documentPaletteV2New(isDarkUi)
        var replacementUploadJob by remember(doc.id) { mutableStateOf<Job?>(null) }
        FullScreenDocumentForm(
            dark = isDarkUi,
            compactDialog = true,
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
            header = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkUi) Color(0xFF4B2A68) else Color(0xFFE9D8FF)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, null, tint = editPalette.accent, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Edit Document", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            color = if (isDarkUi) Color(0xFFF7F1FF) else Color(0xFF281638))
                        Text("Update file details and access settings", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            contentFillsSpace = false,
            content = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val selectedReplacement = pendingReplacement?.takeIf { replacementTarget?.id == doc.id }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, editPalette.line),
                        colors = CardDefaults.cardColors(containerColor = editPalette.surface2)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DocumentTypeIconNew(doc.contentType, Modifier.size(42.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                selectedReplacement?.fileName ?: doc.originalFileName,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = if (isDarkUi) Color(0xFFF5ECFF) else Color(0xFF281638),
                                softWrap = true
                            )
                            Text(
                                formatDocumentBytesNew(selectedReplacement?.sizeBytes ?: doc.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = editPalette.muted
                            )
                        }
                        IconButton(
                            enabled = !isEditBusy,
                            onClick = {
                                replacementTarget = doc
                                VaultExternalActivityState.documentPickerActive = true
                                replacementPicker.launch(arrayOf("*/*"))
                            }
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Change file",
                                tint = editPalette.accent)
                        }
                    }
                    if (selectedReplacement != null && replacingDocument) {
                        Text(
                            "${formatDocumentBytesNew((selectedReplacement.sizeBytes * replacementProgress).toLong())} / ${formatDocumentBytesNew(selectedReplacement.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        LinearProgressIndicator(
                            progress = { replacementProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    }
                    }
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it.take(50) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        label = { Text("File Name (max 50)") }, singleLine = true,
                        enabled = !isEditBusy,
                        trailingIcon = {
                            if (editTitle.isNotEmpty()) IconButton(enabled = !isEditBusy, onClick = { editTitle = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear file name")
                            }
                        },
                        supportingText = { Text("${editTitle.length}/50") }, shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(value = editDescription, onValueChange = { editDescription = it.take(50) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), label = { Text("Notes (max 50)") }, singleLine = true,
                        enabled = !isEditBusy,
                        supportingText = { Text("${editDescription.length}/50") }, shape = RoundedCornerShape(14.dp))
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    var editCategoryMenuExpanded by remember(doc.id) { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { editCategoryMenuExpanded = true },
                            enabled = !isEditBusy,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
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
                                    onClick = { editCategory = id; editCategoryMenuExpanded = false },
                                    enabled = !isEditBusy
                                )
                            }
                        }
                    }
                    Text("Access", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("PERSONAL" to "Personal", "SHARED" to "Shared").forEach { (value, label) ->
                            val selected = editScope == value
                            val selectorShape = RoundedCornerShape(16.dp)
                            Row(Modifier.weight(1f).height(56.dp).clip(selectorShape)
                                .background(if (selected) editPalette.container else editPalette.surface2)
                                .border(1.dp, if (selected) editPalette.accent else editPalette.line, selectorShape)
                                .clickable(enabled = !isEditBusy,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color(0xFFD9B5FF))) { editScope = value }
                                .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                RadioButton(
                                    modifier = Modifier.size(30.dp),
                                    selected = selected,
                                    enabled = !isEditBusy,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = editPalette.accent,
                                        unselectedColor = editPalette.muted
                                    ),
                                    onClick = { editScope = value }
                                )
                                Icon(if (value == "PERSONAL") Icons.Outlined.Person else Icons.Outlined.People,
                                    null, tint = editPalette.accent, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(label, color = if (isDarkUi) Color(0xFFF5ECFF) else Color(0xFF281638),
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1, softWrap = false)
                            }
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
            primaryAction = {
                Button(
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9137D7), contentColor = Color.White),
                    onClick = {
                    val selectedReplacement = pendingReplacement
                        ?.takeIf { replacementTarget?.id == doc.id }
                    if (selectedReplacement != null) {
                        DocumentUploadForegroundService.start(context, "Replacing document")
                    }
                    scope.launch {
                        val replacementJob = currentCoroutineContext()[Job]
                        replacementUploadJob = replacementJob
                        if (selectedReplacement != null) {
                            DocumentUploadForegroundService.setCancelAction { replacementUploadJob?.cancel() }
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
                            message = "Document replacement cancelled."
                            pendingReplacement = null
                            replacementTarget = null
                            editingDocument = null
                            throw error
                        } catch (error: Exception) {
                            editingDocument = null
                            replacementTarget = null
                            message = error.localizedMessage ?: error.message ?: "Save failed."
                        } finally {
                            replacementUploadJob = null
                            replacingDocument = false
                            busyDocument = false
                            if (selectedReplacement != null) {
                                DocumentUploadForegroundService.setCancelAction(null)
                                DocumentUploadForegroundService.stop(context)
                            }
                        }
                    }
                }, enabled = !busyDocument && !replacingDocument) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            secondaryAction = {
                Button(enabled = replacingDocument || !busyDocument,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCC0F5), contentColor = Color(0xFF30203F)),
                    onClick = {
                    if (replacingDocument) {
                        replacementUploadJob?.cancel()
                    } else if (!busyDocument) {
                        if (replacementTarget?.id == doc.id) {
                            pendingReplacement = null
                            replacementTarget = null
                            editTitle = doc.title
                        }
                        editingDocument = null
                    }
                }) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
            }
        )
    }
}

// ───────────────────────── Redesign: palette & helpers ─────────────────────────

@Composable
private fun FullScreenDocumentForm(
    dark: Boolean,
    onDismissRequest: () -> Unit,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    contentFillsSpace: Boolean = true,
    compactDialog: Boolean = false,
    fullScreenPage: Boolean = false,
    primaryAction: @Composable () -> Unit,
    secondaryAction: @Composable () -> Unit
) {
    if (fullScreenPage) {
        androidx.activity.compose.BackHandler(onBack = onDismissRequest)
        Surface(Modifier.fillMaxSize().imePadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(bottom = 12.dp)) {
                header()
                Spacer(Modifier.height(4.dp))
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) { content() }
                if (contentFillsSpace) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { secondaryAction() }
                        Box(Modifier.weight(1f)) { primaryAction() }
                    }
                }
            }
        }
    } else {
    Dialog(onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = compactDialog, decorFitsSystemWindows = false)) {
        Surface(
            modifier = (if (compactDialog) Modifier.fillMaxWidth().heightIn(max = 760.dp) else Modifier.fillMaxSize())
                .documentDialogWaveBackground(dark),
            color = Color.Transparent,
            shape = if (compactDialog) RoundedCornerShape(28.dp) else androidx.compose.ui.graphics.RectangleShape
        ) {
            Column(
                (if (compactDialog) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
                    .imePadding()
                    .then(
                        if (compactDialog) Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 42.dp)
                        else Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 30.dp)
                    )
            ) {
                header()
                Spacer(Modifier.height(18.dp))
                if (contentFillsSpace) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                } else {
                    Box(Modifier.fillMaxWidth().wrapContentHeight()) { content() }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    secondaryAction()
                    Spacer(Modifier.width(12.dp))
                    primaryAction()
                }
            }
        }
    }
    }
}

private fun Modifier.documentDialogWaveBackground(dark: Boolean): Modifier = this
    .clip(RoundedCornerShape(28.dp))
    .drawBehind {
        drawRect(if (dark) Color(0xFF241B30) else Color(0xFFF8F3FF))
    }

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
        accent = Color(0xFFCBA6FF),
        onAccent = Color(0xFF1B0B33),
        container = Color(0xFF2E2145),
        surface = Color(0xFF1D1925),
        surface2 = Color(0xFF272131),
        muted = Color(0xFFB0A8C2),
        line = Color(0xFF2C2536)
    ) else DocPaletteV2New(
        accent = Color(0xFF6F35D5),
        onAccent = Color.White,
        container = Color(0xFFF0E8FD),
        surface = Color.White,
        surface2 = Color(0xFFF1EEF7),
        muted = Color(0xFF625B75),
        line = Color(0xFFE8E2F3)
    )

private fun Modifier.dashedBorderV2New(
    color: Color,
    radius: androidx.compose.ui.unit.Dp
): Modifier = this.drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        )
    )
}

private fun formatDocumentBytesNew(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

// ───────────────────────── Redesign: components ─────────────────────────

@Composable
private fun DocumentDashboardStatCardV2New(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier,
    cardColor: Color,
    iconTint: Color = Color.White
) {
    Card(
        modifier = modifier.heightIn(min = 104.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun documentDisplayNameV2New(document: DocumentRecordV2): String {
    val displayName = document.title.ifBlank { document.originalFileName }.trim()
    val originalName = document.originalFileName.trim()
    val extension = originalName.substringAfterLast('.', "")
        .takeIf { it.isNotBlank() && originalName.lastIndexOf('.') > 0 }
        ?: return displayName

    return if (displayName.endsWith(".$extension", ignoreCase = true)) {
        displayName
    } else {
        "$displayName.$extension"
    }
}

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
    Card(modifier = modifier.heightIn(min = 68.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = tileBackground)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tileAccent, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (isShared != null) tileAccent else palette.muted, maxLines = 1)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (isShared != null) tileAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
    onLongClick: () -> Unit = {},
    onMore: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = documentPaletteV2New(dark)
    val mine = document.ownerUid == currentMemberUid
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).combinedClickable(
            interactionSource = null,
            indication = ripple(color = palette.accent),
            onClick = onClick,
            onLongClick = if (isRecycleBin) onLongClick else null
        ),
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    documentDisplayNameV2New(document),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScopePillV2New(document, mine, palette)
                    if (isRecycleBin) {
                        val remainingDays = document.purgeAt?.toDate()?.time?.let {
                            kotlin.math.ceil(
                                (it - System.currentTimeMillis()).coerceAtLeast(0L) / 86400000.0
                            ).toInt()
                        }
                        if (remainingDays != null) {
                            val retentionBackground = if (dark) Color(0xFF1D3A2C) else Color(0xFFE4F5E9)
                            val retentionForeground = if (dark) Color(0xFF8FE0A7) else Color(0xFF21834A)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = retentionBackground
                            ) {
                                Text(
                                    if (remainingDays <= 0) "Due" else "$remainingDays days left",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = retentionForeground,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (document.favouriteUids[currentMemberUid] == true && !isRecycleBin) {
                        Icon(Icons.Outlined.Favorite, contentDescription = "Favourite",
                            tint = Color(0xFFB45BCE), modifier = Modifier.size(15.dp))
                    }
                }
            }
            if (!isRecycleBin) IconButton(onClick = onMore) {
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
    onLongClick: () -> Unit = {},
    onMore: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = documentPaletteV2New(dark)
    val mine = document.ownerUid == currentMemberUid
    Card(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).combinedClickable(
            interactionSource = null,
            indication = ripple(color = palette.accent),
            onClick = onClick,
            onLongClick = onLongClick
        ),
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
                if (!isRecycleBin) IconButton(onClick = onMore) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More document options", tint = palette.muted)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                documentDisplayNameV2New(document),
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
            ScopePillV2New(document, mine, palette)
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
        modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(
            interactionSource = null,
            indication = ripple(color = palette.accent),
            onClick = onClick
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(36.dp), shape = RoundedCornerShape(11.dp), color = tileBg) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tileFg, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
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
    palette: DocPaletteV2New,
    fileCount: Int,
    subfolderCount: Int,
    modifier: Modifier,
    onOpen: () -> Unit,
    onMore: () -> Unit
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(
            interactionSource = null,
            indication = ripple(color = palette.accent),
            onClick = onOpen
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
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
                Text(
                    buildString {
                        append("$fileCount files")
                        if (subfolderCount > 0) append(" · $subfolderCount folders")
                    },
                    color = palette.muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Folder options", tint = palette.muted)
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
            .clickable(
                interactionSource = null,
                indication = ripple(color = palette.accent),
                onClick = onClick
            )
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

