package com.gokul.personalvault.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import com.gokul.personalvault.data.firebase.DocumentRepositoryV2
import com.gokul.personalvault.data.model.BuiltInDocumentCategoriesV2
import com.gokul.personalvault.data.model.DocumentRecordV2
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

private enum class DocumentSortV2 { NAME_ASC, NAME_DESC, NEWEST, OLDEST, LARGEST, SMALLEST }

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
    var message by remember { mutableStateOf("") }
    var categoryDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    var customCategories by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedDocument by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var editingDocument by remember { mutableStateOf<DocumentRecordV2?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("other") }
    var editScope by remember { mutableStateOf("PERSONAL") }
    var busyDocument by remember { mutableStateOf(false) }

    DisposableEffect(vaultId) {
        val docsListener: ListenerRegistration = repository.documentCollection(vaultId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    documents = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { DocumentRecordV2.fromSnapshot(doc.id, it) }
                    }
                } else if (error != null) message = error.localizedMessage ?: "Unable to load documents."
            }
        val categoriesListener = repository.customCategoryCollection(vaultId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    customCategories = snapshot.documents.mapNotNull { doc ->
                        doc.getString("name")?.let { doc.id to it }
                    }
                }
            }
        onDispose { docsListener.remove(); categoriesListener.remove() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            uploading = true
            progress = 0f
            var completed = 0
            val failures = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val filename = repository.readDisplayName(context.contentResolver, uri)
                message = "Uploading ${index + 1} of ${uris.size}: $filename"
                try {
                    val title = filename.substringBeforeLast('.', filename)
                    repository.upload(
                        vaultId, memberUid, uri, context.contentResolver, title,
                        if (category == "all") "other" else category, "",
                        if (visibility == "SHARED") "SHARED" else "PERSONAL"
                    ) { value -> progress = (completed + value) / uris.size.toFloat() }
                    completed++
                } catch (error: Exception) {
                    failures.add("$filename: ${error.message ?: "Upload failed"}")
                }
            }
            uploading = false
            progress = 1f
            message = if (failures.isEmpty()) "$completed file(s) uploaded."
                else "$completed uploaded; ${failures.size} failed. " + failures.joinToString(" | ")
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

    Scaffold(
        containerColor = Color(0xFFF4F6FB),
        floatingActionButton = {
            if (!recycleBin) FloatingActionButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                containerColor = Color(0xFF315CDB), contentColor = Color.White
            ) { Icon(Icons.Outlined.Add, contentDescription = "Add document") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (recycleBin) "Recycle Bin" else "Documents",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFF14213D))
                        Text(if (recycleBin) "Deleted files are kept for 30 days" else "Your secure files",
                            color = Color(0xFF64748B))
                    }
                    TextButton(onClick = { recycleBin = !recycleBin; category = "all"; visibility = "ALL" }) {
                        Text(if (recycleBin) "All Documents" else "Recycle Bin")
                    }
                }
            }
            if (!recycleBin) {
                item {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(),
                        placeholder = { Text("Search title or description") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        singleLine = true, shape = RoundedCornerShape(16.dp))
                }
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14213D))) {
                        Column(Modifier.padding(18.dp)) {
                            Text("STORAGE USED · ESTIMATED FROM DOCUMENT RECORDS",
                                color = Color(0xFFB9C9E2), style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold)
                            Text(formatDocumentBytes(bytes), color = Color.White,
                                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("${active.size} active documents", color = Color(0xFFCBD5E1),
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Categories", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { categoryDialog = true }) {
                            Icon(Icons.Outlined.Add, null); Text("Add")
                        }
                    }
                    allCategories.chunked(3).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            group.forEach { (id, name) ->
                                DocumentCategoryChip(name, id, category) { category = it }
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("ALL", "PERSONAL", "SHARED").forEach { value ->
                            FilterChip(visibility == value, { visibility = value },
                                label = { Text(value.lowercase().replaceFirstChar { it.uppercase() }) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("ALL", "PDF", "IMAGE", "OFFICE", "OTHER").forEach { value ->
                            FilterChip(fileType == value, { fileType = value }, label = { Text(value) })
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (recycleBin) "Deleted Documents" else "All Documents",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${visible.size} files", color = Color(0xFF64748B))
                    }
                    Box {
                        TextButton(onClick = { sortMenu = true }) { Text("Sort") }
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
                                DocumentCardV2(doc, Modifier.weight(1f), recycleBin) { selectedDocument = doc }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(visible, key = { it.id }) { doc ->
                    DocumentCardV2(doc, Modifier.fillMaxWidth(), recycleBin) { selectedDocument = doc }
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

    selectedDocument?.let { doc ->
        AlertDialog(
            onDismissRequest = { if (!busyDocument) selectedDocument = null },
            title = { Text(doc.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category: ${allCategories.firstOrNull { it.first == doc.categoryId }?.second ?: doc.categoryId}")
                    Text("Description: ${doc.description.ifBlank { "—" }}")
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
            onDismissRequest = { editingDocument = null },
            title = { Text("Edit document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editTitle, { editTitle = it.take(160) }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(editDescription, { editDescription = it.take(50) },
                        label = { Text("Description (max 50)") }, singleLine = true)
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
                            repository.updateMetadata(vaultId, doc, editTitle, editDescription, editCategory, editScope)
                            editingDocument = null; message = "Document details updated."
                        } catch (error: Exception) { message = error.message ?: "Update failed." }
                        busyDocument = false
                    }
                }, enabled = !busyDocument) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingDocument = null }) { Text("Cancel") } }
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
    onClick: () -> Unit
) {
    val icon = when {
        document.contentType.contains("pdf", true) -> Icons.Outlined.PictureAsPdf
        document.contentType.startsWith("image/") -> Icons.Outlined.Image
        else -> Icons.Outlined.InsertDriveFile
    }
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF315CDB), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (document.scope == "SHARED") "Shared" else "Personal",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF315CDB))
            }
            Spacer(Modifier.height(8.dp))
            Text(document.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF14213D), maxLines = 2)
            Text(document.categoryId.replace('_', ' '), color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodySmall, maxLines = 1)
            if (isRecycleBin) Text("Deleted", color = Color(0xFFB45309), style = MaterialTheme.typography.labelSmall)
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
