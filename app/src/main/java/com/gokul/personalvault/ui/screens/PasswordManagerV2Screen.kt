package com.gokul.personalvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.gokul.personalvault.data.local.PasswordRepositoryV2
import com.gokul.personalvault.data.model.PasswordRecordV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PvNavy = Color(0xFF0B2D5C)
private val PvBlue = Color(0xFF2463C5)
private val PvBackground = Color(0xFFF7F8FA)
private val PvText = Color(0xFF172033)
private val PvMuted = Color(0xFF667085)

private enum class PasswordPage { LIST, ADD, DETAILS, EDIT }

@Composable
fun PasswordManagerV2Screen(vaultId: String, memberUid: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var records by remember { mutableStateOf<List<PasswordRecordV2>>(emptyList()) }
    var selectedScope by rememberSaveable { mutableStateOf(PasswordRecordV2.SCOPE_PERSONAL) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var search by rememberSaveable { mutableStateOf("") }
    var page by remember { mutableStateOf(PasswordPage.LIST) }
    var selectedRecord by remember { mutableStateOf<PasswordRecordV2?>(null) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                PasswordRepositoryV2.load(context, memberUid, vaultId)
            }
            result.fold(
                onSuccess = { records = it; message = "" },
                onFailure = { message = it.localizedMessage ?: "Unable to load passwords." }
            )
            loading = false
        }
    }

    LaunchedEffect(vaultId, memberUid) { reload() }

    val visibleRecords = records
        .filter { it.scope == selectedScope }
        .filter { selectedCategory == "All" || it.category == selectedCategory }
        .filter {
            search.isBlank() ||
                    it.websiteApp.contains(search, true) ||
                    it.username.contains(search, true) ||
                    it.category.contains(search, true)
        }
        .sortedBy { it.websiteApp.lowercase(Locale.getDefault()) }

    when (page) {
        PasswordPage.ADD -> PasswordEditorV2(
            title = "Add password",
            initial = null,
            onBack = { page = PasswordPage.LIST },
            onSave = { website, username, password, category, notes, scopeValue ->
                val record = PasswordRepositoryV2.newRecord(
                    memberUid, scopeValue, website, username, password, category, notes
                )
                val updated = records + record
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        PasswordRepositoryV2.save(context, memberUid, vaultId, updated)
                    }
                    result.fold(
                        onSuccess = { records = updated; page = PasswordPage.LIST; selectedScope = scopeValue; message = "Password saved securely on this device." },
                        onFailure = { message = it.localizedMessage ?: "Unable to save password." }
                    )
                }
            }
        )
        PasswordPage.EDIT -> selectedRecord?.let { record ->
            PasswordEditorV2(
                title = "Edit password",
                initial = record,
                onBack = { page = PasswordPage.DETAILS },
                onSave = { website, username, password, category, notes, scopeValue ->
                    // Editing and moving are owner-only. Records created by this account are owned here.
                    if (record.ownerUid != memberUid) {
                        message = "Only the owner can edit this password."
                    } else {
                        val changed = PasswordRepositoryV2.newRecord(
                            memberUid, scopeValue, website, username, password, category, notes, record
                        )
                        val updated = records.map { if (it.id == record.id) changed else it }
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                PasswordRepositoryV2.save(context, memberUid, vaultId, updated)
                            }
                            result.fold(
                                onSuccess = { records = updated; selectedRecord = changed; selectedScope = scopeValue; page = PasswordPage.DETAILS; message = "Password updated." },
                                onFailure = { message = it.localizedMessage ?: "Unable to update password." }
                            )
                        }
                    }
                }
            )
        }
        PasswordPage.DETAILS -> selectedRecord?.let { record ->
            PasswordDetailsV2(
                record = record,
                onBack = { page = PasswordPage.LIST },
                onEdit = { page = PasswordPage.EDIT },
                onCopyUsername = { clipboard.setText(AnnotatedString(record.username)); message = "Username copied." },
                onCopyPassword = { clipboard.setText(AnnotatedString(record.password)); message = "Password copied." },
                onDelete = { showDeleteConfirm = true }
            )
        }
        PasswordPage.LIST -> {
            Scaffold(
                containerColor = PvBackground,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { page = PasswordPage.ADD },
                        containerColor = PvNavy,
                        contentColor = Color.White
                    ) { Icon(Icons.Outlined.Add, contentDescription = "Add password") }
                }
            ) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).background(PvBackground)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(18.dp))
                    Text("PASSWORD MANAGER", color = PvMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Your passwords", color = PvText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${records.count { it.scope == selectedScope }} saved credentials", color = PvMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFE9EDF3), RoundedCornerShape(14.dp)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ScopeTabV2("Personal", selectedScope == PasswordRecordV2.SCOPE_PERSONAL, Modifier.weight(1f)) { selectedScope = PasswordRecordV2.SCOPE_PERSONAL; selectedCategory = "All" }
                        ScopeTabV2("Shared", selectedScope == PasswordRecordV2.SCOPE_SHARED, Modifier.weight(1f)) { selectedScope = PasswordRecordV2.SCOPE_SHARED; selectedCategory = "All" }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Search website or username") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = PvMuted) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PvBlue, unfocusedBorderColor = Color(0xFFDCE2EA)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All") .plus(PasswordRecordV2.CATEGORIES).take(5).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (message.isNotBlank()) {
                        Text(message, color = PvBlue, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                    }
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PvBlue) }
                        visibleRecords.isEmpty() -> EmptyPasswordsV2(selectedScope)
                        else -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                            contentPadding = PaddingValues(bottom = 92.dp)
                        ) {
                            items(visibleRecords, key = { it.id }) { record ->
                                PasswordRowV2(record) {
                                    selectedRecord = record
                                    page = PasswordPage.DETAILS
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete password?") },
            text = { Text("This permanently removes this credential from this device's encrypted V2 password store.") },
            confirmButton = {
                TextButton(onClick = {
                    val record = selectedRecord
                    if (record != null && record.ownerUid == memberUid) {
                        val updated = records.filterNot { it.id == record.id }
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                PasswordRepositoryV2.save(context, memberUid, vaultId, updated)
                            }
                            result.fold(
                                onSuccess = { records = updated; page = PasswordPage.LIST; selectedRecord = null; message = "Password deleted." },
                                onFailure = { message = it.localizedMessage ?: "Unable to delete password." }
                            )
                        }
                    } else message = "Only the owner can delete this password."
                    showDeleteConfirm = false
                }) { Text("Delete", color = Color(0xFFD92D20)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ScopeTabV2(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(11.dp),
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) PvNavy else PvMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PasswordRowV2(record: PasswordRecordV2, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color.White, shape = RoundedCornerShape(15.dp),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(Color(0xFFEAF0FA), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Key, null, tint = PvNavy) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.websiteApp, color = PvText, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(record.username, color = PvMuted, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(record.category, color = PvMuted, style = MaterialTheme.typography.labelSmall)
                Text(if (record.isShared) "SHARED" else "PERSONAL", color = PvBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyPasswordsV2(scope: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Lock, null, tint = PvBlue, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(14.dp))
        Text(if (scope == PasswordRecordV2.SCOPE_PERSONAL) "No personal passwords yet" else "No shared passwords yet",
            color = PvText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text("Tap + to securely add your first credential.", color = PvMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PasswordDetailsV2(
    record: PasswordRecordV2,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onDelete: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(PvBackground).padding(20.dp)) {
        TextButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Passwords")
        }
        Spacer(Modifier.height(12.dp))
        Text("PASSWORD DETAILS", color = PvMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(record.websiteApp, color = PvText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailFieldV2("Username / Email", record.username, onCopyUsername)
                HorizontalDivider(color = Color(0xFFE9EDF3))
                Column {
                    Text("Password", color = PvMuted, style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (showPassword) record.password else "••••••••••••",
                            Modifier.weight(1f), color = PvText, fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                        }
                        IconButton(onClick = onCopyPassword) { Icon(Icons.Outlined.ContentCopy, null) }
                    }
                }
                HorizontalDivider(color = Color(0xFFE9EDF3))
                DetailFieldV2("Category", record.category)
                DetailFieldV2("Notes", record.notes.ifBlank { "—" })
                DetailFieldV2("Created", formatDateV2(record.createdAt))
                DetailFieldV2("Last edited", formatDateV2(record.editedAt))
                DetailFieldV2("Vault", if (record.isShared) "Shared" else "Personal")
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit")
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete")
            }
        }
    }
}

@Composable
private fun DetailFieldV2(label: String, value: String, onCopy: (() -> Unit)? = null) {
    Column {
        Text(label, color = PvMuted, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, Modifier.weight(1f), color = PvText, style = MaterialTheme.typography.bodyLarge)
            if (onCopy != null) IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordEditorV2(
    title: String,
    initial: PasswordRecordV2?,
    onBack: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var website by remember(initial?.id) { mutableStateOf(initial?.websiteApp.orEmpty()) }
    var username by remember(initial?.id) { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember(initial?.id) { mutableStateOf(initial?.password.orEmpty()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "Other") }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var scopeValue by remember(initial?.id) { mutableStateOf(initial?.scope ?: PasswordRecordV2.SCOPE_PERSONAL) }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(PvBackground).padding(20.dp)) {
        TextButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Cancel")
        }
        Text(title, color = PvText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Your credential is encrypted before it is saved.", color = PvMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Text("SAVE IN", color = PvMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = scopeValue == PasswordRecordV2.SCOPE_PERSONAL,
                    onClick = { scopeValue = PasswordRecordV2.SCOPE_PERSONAL }, label = { Text("Personal") })
                FilterChip(selected = scopeValue == PasswordRecordV2.SCOPE_SHARED,
                    onClick = { scopeValue = PasswordRecordV2.SCOPE_SHARED }, label = { Text("Shared") })
            }
            OutlinedTextField(website, { website = it }, Modifier.fillMaxWidth(),
                label = { Text("Website / App *") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(),
                label = { Text("Username / Email") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
                label = { Text("Password *") }, singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                    }
                })
            Spacer(Modifier.height(8.dp))
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = category, onValueChange = {}, readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    PasswordRecordV2.CATEGORIES.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(),
                label = { Text("Notes") }, minLines = 2, maxLines = 3)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (website.isBlank() || password.isBlank()) error = "Website / App and Password are required."
                else onSave(website, username, password, category, notes, scopeValue)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PvNavy),
            shape = RoundedCornerShape(14.dp)
        ) { Text(if (initial == null) "Save password" else "Update password") }
    }
}

private fun formatDateV2(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
