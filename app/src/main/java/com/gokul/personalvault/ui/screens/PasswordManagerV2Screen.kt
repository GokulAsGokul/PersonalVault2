
package com.gokul.personalvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
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
import com.gokul.personalvault.data.firebase.PasswordRepositoryFirestoreV2
import com.gokul.personalvault.data.model.PasswordRecordV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PvNavy = Color(0xFF27358F)
private val PvBlue = Color(0xFF4267D5)
private val PvTeal = Color(0xFF0F9D9A)
private val PvBackground = Color(0xFFF5F6FC)
private val PvText = Color(0xFF182033)
private val PvMuted = Color(0xFF687187)
private val PvLavender = Color(0xFFEAEAFF)

@Composable
fun PasswordManagerV2Screen(
    vaultId: String,
    memberUid: String
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var records by remember {
        mutableStateOf<List<PasswordRecordV2>>(emptyList())
    }

    var selectedScope by rememberSaveable {
        mutableStateOf(PasswordRecordV2.SCOPE_PERSONAL)
    }

    var selectedCategory by rememberSaveable {
        mutableStateOf("All")
    }

    var search by rememberSaveable {
        mutableStateOf("")
    }

    var selectedRecord by remember {
        mutableStateOf<PasswordRecordV2?>(null)
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }
    var recordToEdit by remember { mutableStateOf<PasswordRecordV2?>(null) }
    var recordToDelete by remember { mutableStateOf<PasswordRecordV2?>(null) }

    var loading by remember {
        mutableStateOf(true)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    suspend fun reloadPasswords() {
        loading = true

        try {
            records = withContext(Dispatchers.IO) {
                PasswordRepositoryFirestoreV2.load(
                    vaultId = vaultId,
                    memberUid = memberUid
                )
            }
            message = ""
        } catch (error: Exception) {
            message = error.localizedMessage
                ?: "Unable to load passwords from Firestore."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(vaultId, memberUid) {
        reloadPasswords()
    }

    val visibleRecords = records
        .filter { it.scope == selectedScope }
        .filter {
            selectedCategory == "All" ||
                    it.category == selectedCategory
        }
        .filter {
            search.isBlank() ||
                    it.websiteApp.contains(search, ignoreCase = true) ||
                    it.username.contains(search, ignoreCase = true) ||
                    it.category.contains(search, ignoreCase = true)
        }
        .sortedBy { it.websiteApp.lowercase() }

    Scaffold(
        containerColor = PvBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PvNavy,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add password"
                )
            }
        }
    ) { padding ->

        Column(
            Modifier.fillMaxSize().padding(padding).background(PvBackground).padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Password Manager", color = PvText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, tint = PvTeal, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${records.size} credentials • Encrypted", color = PvMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("⋮", color = PvMuted, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, placeholder = { Text("Search passwords or usernames…") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = PvMuted) },
                trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Clear") } },
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFE9EAF5), RoundedCornerShape(15.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ScopeTabV2("Personal", selectedScope == PasswordRecordV2.SCOPE_PERSONAL, Modifier.weight(1f)) {
                    selectedScope = PasswordRecordV2.SCOPE_PERSONAL; selectedCategory = "All"
                }
                ScopeTabV2("Shared", selectedScope == PasswordRecordV2.SCOPE_SHARED, Modifier.weight(1f)) {
                    selectedScope = PasswordRecordV2.SCOPE_SHARED; selectedCategory = "All"
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All").plus(PasswordRecordV2.CATEGORIES).forEach { category ->
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, shape = RoundedCornerShape(50),
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${visibleRecords.size} passwords", color = PvText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("A–Z", color = PvNavy, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            if (message.isNotBlank()) {
                Text(message, color = if (message.contains("Unable")) MaterialTheme.colorScheme.error else PvTeal, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PvBlue) }
                visibleRecords.isEmpty() -> EmptyPasswordsV2(selectedScope)
                else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(11.dp), contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)) {
                    items(visibleRecords, key = { it.id }) { record -> PasswordRowV2(record = record, memberUid = memberUid) { selectedRecord = record } }
                }
            }
        }
    }

    if (showAddDialog) {
        PasswordAddDialog(
            saving = saving,
            onDismiss = { showAddDialog = false },
            onSave = { website, username, password, category, notes, recordScope ->

                scope.launch {
                    saving = true
                    message = ""

                    try {
                        withContext(Dispatchers.IO) {
                            PasswordRepositoryFirestoreV2.create(
                                vaultId = vaultId,
                                memberUid = memberUid,
                                scope = recordScope,
                                websiteApp = website,
                                username = username,
                                password = password,
                                category = category,
                                notes = notes
                            )
                        }

                        showAddDialog = false
                        selectedScope = recordScope
                        selectedCategory = "All"

                        reloadPasswords()
                        message = "Password saved securely to your vault."

                    } catch (error: Exception) {
                        message = error.localizedMessage
                            ?: "Unable to save password."
                    } finally {
                        saving = false
                    }
                }
            }
        )
    }

    selectedRecord?.let { record ->
        PasswordDetailsDialogV2(
            record = record,
            onDismiss = { selectedRecord = null },
            onCopyUsername = {
                clipboard.setText(AnnotatedString(record.username))
                message = "Username copied."
            },
            canManage = record.ownerUid == memberUid,
            onEdit = { recordToEdit = record; selectedRecord = null },
            onDelete = { recordToDelete = record; selectedRecord = null },
            onCopyPassword = {
                clipboard.setText(AnnotatedString(record.password))
                message = "Password copied."
            }
        )
    }

    recordToEdit?.let { record ->
        PasswordAddDialog(
            saving = saving,
            initialRecord = record,
            onDismiss = { if (!saving) recordToEdit = null },
            onSave = { website, username, password, category, notes, _ ->
                scope.launch {
                    saving = true
                    message = ""
                    try {
                        withContext(Dispatchers.IO) {
                            PasswordRepositoryFirestoreV2.update(
                                vaultId, memberUid, record, website,
                                username, password, category, notes
                            )
                        }
                        recordToEdit = null
                        reloadPasswords()
                        message = "Password updated securely."
                    } catch (error: Exception) {
                        message = error.localizedMessage ?: "Unable to update password."
                    } finally { saving = false }
                }
            }
        )
    }

    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!saving) recordToDelete = null },
            title = { Text("Delete password?") },
            text = { Text("Delete " + record.websiteApp + "? This action cannot be undone.") },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    scope.launch {
                        saving = true
                        try {
                            withContext(Dispatchers.IO) {
                                PasswordRepositoryFirestoreV2.delete(vaultId, memberUid, record)
                            }
                            recordToDelete = null
                            reloadPasswords()
                            message = "Password deleted."
                        } catch (error: Exception) {
                            message = error.localizedMessage ?: "Unable to delete password."
                        } finally { saving = false }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScopeTabV2(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(11.dp),
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(
            Modifier.padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (selected) PvNavy else PvMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PasswordRowV2(record: PasswordRecordV2, memberUid: String, onClick: () -> Unit) {
    val accent = if (record.isShared) Color(0xFF8B63E8) else PvTeal
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = Color.White, shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = if (record.isShared) Color(0xFFF0EAFE) else Color(0xFFE5F5F3), modifier = Modifier.size(46.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Key, null, tint = if (record.isShared) Color(0xFF7046C4) else PvTeal, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(record.websiteApp, color = PvText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(record.username.ifBlank { "Username not added" }, color = PvMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Surface(shape = RoundedCornerShape(50), color = if (record.isShared) Color(0xFFEFE8FF) else Color(0xFFDDF3ED)) {
                        Text(if (record.ownerUid == memberUid) "MINE" else "SPOUSE", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = if (record.ownerUid == memberUid) Color(0xFF08785C) else Color(0xFF6841B8), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = PvMuted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("••••••••••••", color = PvMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFF0F2F8)) {
                        Text(record.category, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = PvNavy, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.Visibility, null, tint = PvNavy, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("Tap to view details, copy or manage", color = PvMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EmptyPasswordsV2(scope: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Lock,
            null,
            tint = PvBlue,
            modifier = Modifier.size(42.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            if (scope == PasswordRecordV2.SCOPE_PERSONAL)
                "No personal passwords yet"
            else
                "No shared passwords yet",
            color = PvText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(5.dp))

        Text(
            "Tap + to securely add your first credential.",
            color = PvMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordAddDialog(
    saving: Boolean,
    initialRecord: PasswordRecordV2? = null,
    onDismiss: () -> Unit,
    onSave: (
        String, String, String, String, String, String
    ) -> Unit
) {
    var website by remember(initialRecord?.id) { mutableStateOf(initialRecord?.websiteApp.orEmpty()) }
    var username by remember(initialRecord?.id) { mutableStateOf(initialRecord?.username.orEmpty()) }
    var password by remember(initialRecord?.id) { mutableStateOf(initialRecord?.password.orEmpty()) }
    var category by remember(initialRecord?.id) { mutableStateOf(initialRecord?.category ?: "Other") }
    var notes by remember(initialRecord?.id) { mutableStateOf(initialRecord?.notes.orEmpty()) }
    var recordScope by remember(initialRecord?.id) {
        mutableStateOf(initialRecord?.scope ?: PasswordRecordV2.SCOPE_PERSONAL)
    }
    var showPassword by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        title = { Text(if (initialRecord == null) "Add password" else "Edit password") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initialRecord == null) {
                    Text("SAVE IN")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = recordScope ==
                                PasswordRecordV2.SCOPE_PERSONAL,
                        onClick = {
                            recordScope = PasswordRecordV2.SCOPE_PERSONAL
                        },
                        label = { Text("Personal") }
                    )

                    FilterChip(
                        selected = recordScope ==
                                PasswordRecordV2.SCOPE_SHARED,
                        onClick = {
                            recordScope = PasswordRecordV2.SCOPE_SHARED
                        },
                        label = { Text("Shared") }
                    )
                    }
                } else {
                    Text("SAVED IN ${initialRecord.scope}")
                }

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website / App *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password *") },
                    singleLine = true,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Icon(
                                if (showPassword)
                                    Icons.Outlined.VisibilityOff
                                else
                                    Icons.Outlined.Visibility,
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = {
                        categoryExpanded = it
                    }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                categoryExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = {
                            categoryExpanded = false
                        }
                    ) {
                        PasswordRecordV2.CATEGORIES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    if (website.isBlank() || password.isBlank()) {
                        error = "Website / App and Password are required."
                    } else {
                        onSave(
                            website,
                            username,
                            password,
                            category,
                            notes,
                            recordScope
                        )
                    }
                }
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (initialRecord == null) "Save password" else "Save changes")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasswordDetailsDialogV2(
    record: PasswordRecordV2,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit
) {
    var showPassword by remember(record.id) {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.websiteApp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Username / Email")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.username.ifBlank { "—" },
                        Modifier.weight(1f)
                    )
                    IconButton(onClick = onCopyUsername) {
                        Icon(Icons.Outlined.ContentCopy, null)
                    }
                }

                HorizontalDivider()

                Text("Password")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showPassword) record.password else "••••••••••••",
                        Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { showPassword = !showPassword }
                    ) {
                        Icon(
                            if (showPassword)
                                Icons.Outlined.VisibilityOff
                            else
                                Icons.Outlined.Visibility,
                            null
                        )
                    }

                    IconButton(onClick = onCopyPassword) {
                        Icon(Icons.Outlined.ContentCopy, null)
                    }
                }

                HorizontalDivider()

                Text("Category: ${record.category}")
                Text("Notes: ${record.notes.ifBlank { "—" }}")
                Text(
                    "Vault: " +
                            if (record.isShared) "Shared" else "Personal"
                )
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canManage) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}