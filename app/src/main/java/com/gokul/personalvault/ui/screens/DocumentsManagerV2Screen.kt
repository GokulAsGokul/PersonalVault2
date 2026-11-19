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
                } else if (error != null) message = error.localizedMessage ?: "Unable to load shared documents."
            }
        val personalListener: ListenerRegistration = repository.personalDocumentsQuery(vaultId, memberUid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    personalDocs.clear()
                    snapshot.documents.forEach { doc ->
                        doc.data?.let { personalDocs[doc.id] = DocumentRecordV2.fromSnapshot(doc.id, it) }
                    }
                    publishDocuments()
                } else if (error != null) message = error.localizedMessage ?: "Unable to load personal documents."
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
        containerColor = Color(0xFFF4F6FB),
        floatingActionButton = {
            if (!recycleBin) FloatingActionButton(
                onClick = { VaultExternalActivityState.documentPickerActive = true; picker.launch(arrayOf("*/*")) },
                containerColor = Color(0xFF315CDB), contentColor = Color.White
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add document") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp, 0.dp, 18.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Outlined.FolderOpen, null, tint = Color(0xFF315CDB), modifier = Modifier.size(30.dp))
                        Column {
                            Text(if (recycleBin) "Recycle Bin" else "Documents",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = Color(0xFF14213D))
                            Text(if (recycleBin) "Deleted Files Are Kept For 30 Days" else "Your Secure Files",
                                color = Color(0xFF64748B))
                        }
                    }
                    TextButton(onClick = { recycleBin = !recycleBin; category = "all"; visibility = "ALL" }) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (recycleBin) "All Documents" else "Recycle Bin")
                    }
                }
            }
            if (!recycleBin) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14213D))) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Outlined.Storage, null, tint = Color(0xFFB9C9E2), modifier = Modifier.size(16.dp)); Text("Storage Used · Estimated From Document Records",
                                color = Color(0xFFB9C9E2), style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold) }
                            Text(formatDocumentBytes(bytes), color = Color.White,
                                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("${active.size} Active Documents", color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        DocumentSummaryTile("Files", active.size.toString(), Modifier.weight(1f))
                        DocumentSummaryTile("Images", images.toString(), Modifier.weight(1f))
                        DocumentSummaryTile("PDFs", pdfs.toString(), Modifier.weight(1f))
                        DocumentSummaryTile("Other", others.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(),
                        placeholder = { Text("Search Title or Notes") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        singleLine = true, shape = RoundedCornerShape(16.dp))
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Categories", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { categoryDialog = true }) {
                            Icon(Icons.Outlined.Add, null); Text("Add")
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        DocumentCategoryChip("All", "all", category) { category = it }
                        allCategories.forEach { (id, name) ->
                            DocumentCategoryChip(name, id, category) { category = it }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("ALL", "PERSONAL", "SHARED").forEach { value ->
                                FilterChip(
                                    visibility == value,
                                    { visibility = value },
                                    label = { Text(value.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("ALL" to "All", "PDF" to "Pdf", "IMAGE" to "Image", "OFFICE" to "Office", "OTHER" to "Other").forEach { (value, label) ->
                                FilterChip(fileType == value, { fileType = value }, label = { Text(label) })
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (recycleBin) "Deleted Documents" else "Documents",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${visible.size} Files", color = Color(0xFF64748B))
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
            if (visible.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Description, null, tint = Color(0xFF315CDB))
                            Text(if (recycleBin) "Recycle Bin is empty" else "No documents found",
                                fontWeight = FontWeight.SemiBold)
                            Text(if (recycleBin) "Deleted files will appear here." else "Upload a file to get started.",
                                color = Color(0xFF64748B))
                        }
                    }
                }
            } else if (grid) {
                visible.chunked(2).forEach { pair ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { doc ->
                                DocumentCardV2(
                                    doc, Modifier.weight(1f), recycleBin,
                                    categoryName = allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: "Other",
                                    onClick = { selectedDocument = doc },
                                    onDownload = { downloadDocument(doc) },
                                    onEdit = { editDocument(doc) },
                                    onDelete = { deleteDocument(doc) },
                                    canDelete = doc.ownerUid == memberUid,
                                    addedByLabel = if (doc.ownerUid == memberUid) "You" else "Household Member"
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
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
                        canDelete = doc.ownerUid == memberUid,
                                    addedByLabel = if (doc.ownerUid == memberUid) "You" else "Household Member"
                    )
                }
            }
            if (uploading) item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(message, style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                    }
                }
            } else if (message.isNotBlank()) item { Text(message, color = Color(0xFF315CDB)) }
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
                        color = Color(0xFF64748B))
                    Text(
                        "Limits: 50 MB per file · 200 MB per batch",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Total selected: ${formatDocumentBytes(pendingUploads.sumOf { it.sizeBytes })}",
                        color = Color(0xFF64748B),
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
                                            pending.title.trim(), uploadCategory, pending.notes.trim(), uploadScope
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
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(9.dp)) {
            Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF14213D))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
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
    canDelete: Boolean,
    addedByLabel: String
) {
    val fileIcon = when {
        document.contentType.contains("pdf", true) -> Icons.Outlined.PictureAsPdf
        document.contentType.startsWith("image/") -> Icons.Outlined.Image
        else -> Icons.Outlined.InsertDriveFile
    }
    Card(modifier, shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(fileIcon, null, tint = Color(0xFF315CDB), modifier = Modifier.size(31.dp))
                Surface(color = Color(0xFFE7EEFF), shape = RoundedCornerShape(50)) {
                    Text(categoryName, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color(0xFF214DB8), style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(document.originalFileName.ifBlank { document.title },
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF14213D), maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Storage, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                    Text(formatDocumentBytes(document.sizeBytes), color = Color(0xFF64748B),
                        style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(if (document.scope == "SHARED") Icons.Outlined.Groups else Icons.Outlined.Person,
                        null, tint = Color(0xFF526987), modifier = Modifier.size(17.dp))
                    Text(
                        if (document.scope == "SHARED") "Shared Vault · Added By $addedByLabel" else "Personal Vault",
                        color = Color(0xFF526987), style = MaterialTheme.typography.bodySmall, maxLines = 1
                    )
                }
                if (isRecycleBin) Text("Deleted", color = Color(0xFFB45309), style = MaterialTheme.typography.labelSmall)
            }
            if (!isRecycleBin) {
                VerticalDivider(Modifier.height(76.dp), color = Color(0xFFD7DCE7))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onDownload, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Download, "Download", tint = Color(0xFF1D4FA8), modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = Color(0xFF1D4FA8), modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete, enabled = canDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFFB91C1C), modifier = Modifier.size(19.dp))
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
