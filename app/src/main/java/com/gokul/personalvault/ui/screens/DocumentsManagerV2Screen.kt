package com.gokul.personalvault.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.tasks.await
import java.util.Locale

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
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var visibility by remember { mutableStateOf("ALL") }
    var fileType by remember { mutableStateOf("ALL") }
    var sort by remember { mutableStateOf(DocumentSortV2.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(false) }
    var recycleBin by remember { mutableStateOf(false) }
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
    var uploadCategory by remember { mutableStateOf("other") }
    var uploadScope by remember { mutableStateOf("PERSONAL") }
    var uploadCategoryMenu by remember { mutableStateOf(false) }
    var replacementTarget by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var pendingReplacement by remember { mutableStateOf<PendingDocumentReplacementV2?>(null) }
    var replacingDocument by remember { mutableStateOf(false) }
    var replacementProgress by remember { mutableFloatStateOf(0f) }

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

    DisposableEffect(vaultId, category) {
        if (category == "all") {
            folders = emptyList()
            currentFolderId = null
            onDispose { }
        } else {
            val sharedFolders = mutableMapOf<String, DocumentFolderV2>()
            val personalFolders = mutableMapOf<String, DocumentFolderV2>()
            fun publishFolders() {
                folders = (sharedFolders.values + personalFolders.values).distinctBy { it.id }
            }
            val sharedFolderListener = repository.sharedFoldersQuery(vaultId, category)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        sharedFolders.clear()
                        snapshot.documents.forEach { doc ->
                            doc.data?.let { sharedFolders[doc.id] = DocumentFolderV2.fromSnapshot(doc.id, it) }
                        }
                        publishFolders()
                    } else if (error != null) message = "Shared folders: ${error.localizedMessage ?: "Unable to load folders."}"
                }
            val personalFolderListener = repository.personalFoldersQuery(vaultId, category, memberUid)
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
                    title = fileName.substringBeforeLast('.', fileName).take(30)
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
                    val derivedTitle = fileName.substringBeforeLast('.').trim().take(30)
                    if (derivedTitle.isNotBlank()) editTitle = derivedTitle
                }
            }
        } else {
            pendingReplacement = null
            replacementTarget = null
            editTitle = editingDocument?.title ?: editTitle
        }
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

    fun downloadDocument(doc: DocumentRecordV2) {
        scope.launch {
            busyDocument = true
            try {
                val url = repository.downloadUrl(vaultId, doc)
                enqueueDocumentDownload(context, url, doc.originalFileName)
                message = "Download Started. Check Your Downloads Folder."
            } catch (error: Exception) {
                message = error.message ?: "Download Failed."
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
            if (!recycleBin) FloatingActionButton(
                onClick = { showAddDocuments = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add documents") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Outlined.FolderOpen, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(9.dp).size(27.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (recycleBin) "Recycle Bin" else if (category == "all") "Documents"
                                else allCategories.firstOrNull { it.first == category }?.second ?: "Documents",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (recycleBin) "Deleted files · 30-day retention"
                            else "${active.size} files · Personal Vault",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { search = if (search.isBlank()) " " else "" }) {
                        Icon(Icons.Outlined.Search, "Search")
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (recycleBin) "All Documents" else "Recycle Bin") },
                                onClick = {
                                    recycleBin = !recycleBin
                                    category = "all"
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
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf("ALL" to "All", "PERSONAL" to "Personal", "SHARED" to "Shared").forEach { (value, label) ->
                        FilterChip(
                            selected = visibility == value,
                            onClick = { visibility = value },
                            label = { Text(label) }
                        )
                    }
                    FilterChip(
                        selected = recycleBin,
                        onClick = {
                            recycleBin = !recycleBin
                            category = "all"
                            currentFolderId = null
                        },
                        label = { Text("Recycle Bin") }
                    )
                }
            }
            if (!recycleBin && category == "all" && currentFolderId == null) {
                item {
                    Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(allCategories.chunked(3)) { categoryRow ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        categoryRow.forEachIndexed { index, (id, name) ->
                            val count = active.count { it.categoryId == id }
                            val icon = when (id) {
                                "banking_finance" -> Icons.Outlined.AccountBalance
                                "property" -> Icons.Outlined.Home
                                "insurance" -> Icons.Outlined.HealthAndSafety
                                "employment" -> Icons.Outlined.Work
                                "vehicle" -> Icons.Outlined.DirectionsCar
                                "travel_visa" -> Icons.Outlined.Flight
                                "medical" -> Icons.Outlined.MedicalServices
                                "education" -> Icons.Outlined.School
                                "legal_documents" -> Icons.Outlined.Gavel
                                else -> Icons.Outlined.Description
                            }
                            Card(
                                Modifier.weight(1f).clickable {
                                    category = id
                                    currentFolderId = null
                                },
                                shape = RoundedCornerShape(15.dp),
                                border = if (category == id) androidx.compose.foundation.BorderStroke(
                                    1.5.dp, MaterialTheme.colorScheme.primary
                                ) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (category == id)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(8.dp).size(22.dp))
                                    }
                                    Text(name, style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold, maxLines = 2)
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
                        TextButton(onClick = { category = "all"; currentFolderId = null }) { Text("See all") }
                    }
                }
                items(active.filter { visibility == "ALL" || it.scope == visibility }
                    .sortedByDescending { it.uploadedAt?.toDate()?.time ?: 0L }.take(3), key = { "recent-${it.id}" }) { doc ->
                    DocumentCardV2(
                        doc, Modifier.fillMaxWidth(), false,
                        categoryName = allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other",
                        onClick = { selectedDocument = doc },
                        onDownload = { downloadDocument(doc) },
                        onEdit = { editDocument(doc) },
                        onDelete = { deleteDocument(doc) },
                        canDelete = doc.ownerUid == memberUid
                    )
                }
                item {
                    Text("All Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            } else if (!recycleBin) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { category = "all"; currentFolderId = null }) {
                            Icon(Icons.Outlined.FolderOpen, null)
                            Text("Documents")
                        }
                        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(allCategories.firstOrNull { it.first == category }?.second ?: "Category",
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                if (category != "all") {
                    val childFolders = folders.filter {
                        it.parentFolderId == currentFolderId && (visibility == "ALL" || it.scope == visibility)
                    }.sortedBy { it.name.lowercase(Locale.ROOT) }
                    if (currentFolderId != null) {
                        item {
                            TextButton(onClick = {
                                currentFolderId = folders.firstOrNull { it.id == currentFolderId }?.parentFolderId
                            }) { Text("← Back to parent folder") }
                        }
                    }
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
                            newFolderScope = if (visibility == "SHARED") "SHARED" else "PERSONAL"
                            showCreateFolder = true
                        }) {
                            Icon(Icons.Outlined.Add, null)
                            Text("Create Folder")
                        }
                    }
                }
            }
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
                        doc, Modifier.fillMaxWidth(), recycleBin,
                        categoryName = allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other",
                        onClick = { selectedDocument = doc },
                        onDownload = { downloadDocument(doc) },
                        onEdit = { editDocument(doc) },
                        onDelete = { deleteDocument(doc) },
                        canDelete = doc.ownerUid == memberUid
                    )
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
                    Text("Category", fontWeight = FontWeight.SemiBold)
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
                    Divider()
                    pendingUploads.forEachIndexed { index, pending ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(pending.fileName, fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            OutlinedTextField(
                                value = pending.title,
                                onValueChange = { value ->
                                    pendingUploads[index] = pending.copy(title = value.take(30))
                                },
                                label = { Text("Title (max 30)") },
                                supportingText = { Text("${pending.title.length}/30") },
                                singleLine = true,
                                enabled = !uploading,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pending.notes,
                                onValueChange = { value ->
                                    pendingUploads[index] = pending.copy(notes = value.take(35))
                                },
                                label = { Text("Notes (max 35)") },
                                supportingText = { Text("${pending.notes.length}/35") },
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
                    it.categoryId == folder.categoryId && it.scope == folder.scope
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
                    Text("Visibility", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(newFolderScope == "PERSONAL", { newFolderScope = "PERSONAL" }, label = { Text("Personal") })
                        FilterChip(newFolderScope == "SHARED", { newFolderScope = "SHARED" }, label = { Text("Shared") })
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

    selectedDocument?.let { doc ->
        AlertDialog(
            onDismissRequest = { if (!busyDocument) selectedDocument = null },
            title = { Text(doc.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category: ${allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: doc.categoryId}")
                    Text("Notes: ${doc.description.ifBlank { "—" }}")
                    Text("File: ${doc.originalFileName}")
                    Text("Type: ${doc.contentType}")
                    Text("Size: ${formatDocumentBytes(doc.sizeBytes)}")
                    Text("Uploaded: ${doc.uploadedAt?.toDate()?.toString() ?: "—"}")
                    Text("Last updated: ${doc.updatedAt?.toDate()?.toString() ?: "—"}")
                    Text("Owner: ${if (doc.ownerUid == memberUid) "You" else "Household member"}")
                    Text("Visibility: ${doc.scope}")
                    if (doc.isDeleted) Text("Purge date: ${doc.purgeAt?.toDate()?.toString() ?: "—"}")
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!doc.isDeleted) {
                        if (doc.ownerUid == memberUid) {
                            TextButton(enabled = !busyDocument, onClick = {
                                selectedDocument = null
                                replacementTarget = doc
                                VaultExternalActivityState.documentPickerActive = true
                                replacementPicker.launch(arrayOf("*/*"))
                            }) { Text("Replace") }
                        }
                        TextButton(enabled = !busyDocument, onClick = {
                            editTitle = doc.title; editDescription = doc.description
                            editCategory = doc.categoryId; editScope = doc.scope
                            editingDocument = doc; selectedDocument = null
                        }) { Icon(Icons.Outlined.Edit, null); Text("Edit") }
                        TextButton(enabled = !busyDocument, onClick = {
                            scope.launch {
                                busyDocument = true
                                try {
                                    val url = repository.downloadUrl(vaultId, doc)
                                    enqueueDocumentDownload(context, url, doc.originalFileName)
                                    message = "Download started. Check your Downloads folder."
                                    selectedDocument = null
                                } catch (error: Exception) { message = error.message ?: "Download failed." }
                                busyDocument = false
                            }
                        }) { Icon(Icons.Outlined.Download, null); Text("Download") }
                        TextButton(enabled = !busyDocument && doc.ownerUid == memberUid, onClick = {
                            scope.launch {
                                busyDocument = true
                                try { repository.moveToRecycleBin(vaultId, doc); selectedDocument = null; message = "Moved to Recycle Bin. Retained for 30 days." }
                                catch (error: Exception) { message = error.message ?: "Delete failed." }
                                busyDocument = false
                            }
                        }) { Icon(Icons.Outlined.Delete, null); Text("Delete") }
                    } else if (doc.ownerUid == memberUid) {
                        TextButton(enabled = !busyDocument, onClick = {
                            scope.launch {
                                busyDocument = true
                                try { repository.restore(vaultId, doc.id); selectedDocument = null; message = "Document restored." }
                                catch (error: Exception) { message = error.message ?: "Restore failed." }
                                busyDocument = false
                            }
                        }) { Icon(Icons.Outlined.Restore, null); Text("Restore") }
                        TextButton(enabled = !busyDocument, onClick = {
                            scope.launch {
                                busyDocument = true
                                try { repository.permanentlyDelete(vaultId, doc.id); selectedDocument = null; message = "Document permanently deleted." }
                                catch (error: Exception) { message = error.message ?: "Permanent deletion failed." }
                                busyDocument = false
                            }
                        }) { Icon(Icons.Outlined.Delete, null); Text("Delete forever") }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { selectedDocument = null }) { Text("Close") } }
        )
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
                    OutlinedTextField(editTitle, { editTitle = it.take(30) }, label = { Text("Title (max 30)") }, singleLine = true)
                    OutlinedTextField(editDescription, { editDescription = it.take(35) },
                        label = { Text("Notes (max 35)") }, singleLine = true)
                    Text("Category")
                    allCategories.forEach { (id, name) ->
                        if (editCategory == id) Text("✓ $name")
                        else TextButton(onClick = { editCategory = id }) { Text(name) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(editScope == "PERSONAL", { editScope = "PERSONAL" }, label = { Text("Personal") })
                        FilterChip(editScope == "SHARED", { editScope = "SHARED" }, label = { Text("Shared") })
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
    categoryName: String,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val fileIcon = when {
        document.contentType.contains("pdf", true) -> Icons.Outlined.PictureAsPdf
        document.contentType.startsWith("image/") -> Icons.Outlined.Image
        else -> Icons.Outlined.InsertDriveFile
    }
    Card(modifier, shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(fileIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(31.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(document.originalFileName.ifBlank { document.title },
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(formatDocumentBytes(document.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(if (document.scope == "SHARED") Icons.Outlined.Groups else Icons.Outlined.Person,
                        null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                    Text(
                        if (document.scope == "SHARED") "Shared Vault" else "Personal Vault",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1
                    )
                }
                if (isRecycleBin) Text("Deleted", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
            }
            if (!isRecycleBin) {
                VerticalDivider(Modifier.height(76.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onDownload, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Download, "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete, enabled = canDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
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
