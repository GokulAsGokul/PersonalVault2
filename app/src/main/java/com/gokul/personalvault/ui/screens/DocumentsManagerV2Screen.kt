package com.gokul.personalvault.ui.screens

import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokul.personalvault.data.firebase.DocumentRepositoryV2
import com.gokul.personalvault.data.model.DocumentRecordV2
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsManagerV2Screen(vaultId: String, memberUid: String) {
    val context = LocalContext.current
    val repository = remember { DocumentRepositoryV2() }
    val scope = rememberCoroutineScope()
    var documents by remember { mutableStateOf<List<DocumentRecordV2>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var shared by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var message by remember { mutableStateOf("") }
    var categoryDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    var customCategories by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    DisposableEffect(vaultId) {
        val docsListener: ListenerRegistration = repository.documentCollection(vaultId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    documents = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { DocumentRecordV2.fromSnapshot(doc.id, it) }
                    }
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
        onDispose {
            docsListener.remove()
            categoriesListener.remove()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) scope.launch {
            uploading = true
            progress = 0f
            var done = 0
            val failures = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val filename = repository.readDisplayName(context.contentResolver, uri)
                message = "Uploading " + (index + 1) + " of " + uris.size + ": " + filename
                try {
                    val title = filename.substringBeforeLast('.', filename)
                    repository.upload(
                        vaultId, memberUid, uri, context.contentResolver,
                        title, if (category == "all") "other" else category,
                        "", if (shared) "SHARED" else "PERSONAL"
                    ) { p -> progress = (done + p) / uris.size.toFloat() }
                    done++
                } catch (e: Exception) {
                    failures.add(filename + ": " + (e.message ?: "Upload failed"))
                }
            }
            uploading = false
            progress = 1f
            message = if (failures.isEmpty()) done.toString() + " file(s) uploaded."
                else done.toString() + " uploaded; " + failures.size + " failed: " + failures.joinToString()
        }
    }

    val visible = documents.filter { !it.isDeleted }
        .filter { !shared || it.scope == "SHARED" }
        .filter { category == "all" || it.categoryId == category }
        .filter {
            val q = search.trim().lowercase(Locale.ROOT)
            q.isEmpty() || it.title.lowercase(Locale.ROOT).contains(q) ||
                it.description.lowercase(Locale.ROOT).contains(q)
        }.sortedBy { it.title.lowercase(Locale.ROOT) }

    val bytes = documents.filter { !it.isDeleted }.sumOf { it.sizeBytes }
    val pdfs = visible.count { it.contentType.contains("pdf", true) }
    val images = visible.count { it.contentType.startsWith("image/") }
    val others = visible.size - pdfs - images

    Scaffold(
        containerColor = Color(0xFFF4F6FB),
        floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch(arrayOf("*/*")) },
                containerColor = Color(0xFF315CDB), contentColor = Color.White) {
                Icon(Icons.Outlined.Add, contentDescription = "Add document")
            }
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
                        Text("Documents", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFF14213D))
                        Text("Your secure files", color = Color(0xFF64748B))
                    }
                    Button(onClick = { picker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Outlined.Add, null)
                        Text("Upload")
                    }
                }
            }
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
                        Text("STORAGE USED", color = Color(0xFFB9C9E2),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(formatDocumentBytes(bytes), color = Color.White,
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(documents.count { !it.isDeleted }.toString() + " active documents",
                            color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    DocumentSummaryTile("Files", visible.size.toString(), Modifier.weight(1f))
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
                        Icon(Icons.Outlined.Add, null)
                        Text("Add")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    DocumentCategoryChip("All", "all", category) { category = it }
                    DocumentCategoryChip("Identity", "personal_identity", category) { category = it }
                    DocumentCategoryChip("Banking", "banking_finance", category) { category = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    DocumentCategoryChip("Insurance", "insurance", category) { category = it }
                    DocumentCategoryChip("Employment", "employment", category) { category = it }
                    DocumentCategoryChip("Other", "other", category) { category = it }
                }
                customCategories.chunked(3).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        group.forEach { item ->
                            DocumentCategoryChip(item.second, item.first, category) { category = it }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("All Documents", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(visible.size.toString() + " files", color = Color(0xFF64748B))
                    }
                    FilterChip(shared, { shared = !shared }, label = { Text("Shared") })
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
                            Text("No documents found", fontWeight = FontWeight.SemiBold)
                            Text("Upload a file to get started.", color = Color(0xFF64748B))
                        }
                    }
                }
            } else {
                items(visible, key = { it.id }) { doc -> DocumentCardV2(doc) }
            }
            if (uploading) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(message, style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else if (message.isNotBlank()) {
                item { Text(message, color = Color(0xFF315CDB)) }
            }
        }
    }

    if (categoryDialog) {
        AlertDialog(
            onDismissRequest = { categoryDialog = false },
            title = { Text("New category") },
            text = {
                OutlinedTextField(newCategory, { newCategory = it.take(30) },
                    label = { Text("Category name (max 30)") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = newCategory.trim()
                    val duplicate = customCategories.any { it.second.equals(clean, true) } ||
                        com.gokul.personalvault.data.model.BuiltInDocumentCategoriesV2.all.any { it.name.equals(clean, true) }
                    if (clean.isNotEmpty() && !duplicate) {
                        val id = clean.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_") +
                            "_" + System.currentTimeMillis().toString().takeLast(6)
                        repository.customCategoryCollection(vaultId).document(id).set(
                            mapOf("name" to clean, "nameLower" to clean.lowercase(Locale.ROOT),
                                "createdByUid" to memberUid,
                                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                        )
                        newCategory = ""
                        categoryDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { categoryDialog = false }) { Text("Cancel") } }
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
private fun DocumentCardV2(document: DocumentRecordV2) {
    val icon = when {
        document.contentType.contains("pdf", true) -> Icons.Outlined.PictureAsPdf
        document.contentType.startsWith("image/") -> Icons.Outlined.Image
        else -> Icons.Outlined.InsertDriveFile
    }
    Card(Modifier.fillMaxWidth().clickable { }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF315CDB), modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(document.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF14213D), maxLines = 1)
                Text(document.categoryId.replace('_', ' ') + " • " +
                    document.contentType.substringAfter('/'),
                    color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Surface(color = if (document.scope == "SHARED") Color(0xFFE7EEFF) else Color(0xFFF0F2F5),
                shape = RoundedCornerShape(20.dp)) {
                Text(if (document.scope == "SHARED") "Shared" else "Personal",
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF315CDB))
            }
        }
    }
}

private fun formatDocumentBytes(bytes: Long): String = when {
    bytes < 1024 -> bytes.toString() + " B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
