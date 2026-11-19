package com.gokul.personalvault.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gokul.personalvault.data.firebase.PasswordRepositoryFirestoreV2
import com.gokul.personalvault.data.local.PasswordExpiryReminderScheduler
import com.gokul.personalvault.data.model.PasswordRecordV2
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────────────────── Limits & timings (unchanged behaviour) ─────────────────────────

private const val MAX_WEBSITE = 30
private const val MAX_USERNAME = 35
private const val MAX_PASSWORD = 25
private const val MAX_NOTES = 30
private const val MAX_CATEGORY = 24
private const val REVEAL_MS = 15_000L
private const val CLIPBOARD_CLEAR_MS = 30_000L
private const val EXPIRY_SOON_DAYS = 30
private const val DAY_MS = 86_400_000L

private val SORT_OPTIONS = listOf("A–Z", "Z–A", "Newest first", "Oldest first")

// ───────────────────────── Theme: Material 3 with the app's navy + teal identity ─────────────────────────

private val LightScheme = lightColorScheme(
    primary = Color(0xFF27358F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE1FF),
    onPrimaryContainer = Color(0xFF0A1470),
    secondary = Color(0xFF5B5F7E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E3FA),
    onSecondaryContainer = Color(0xFF181B33),
    tertiary = Color(0xFF006A68),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA4F1EE),
    onTertiaryContainer = Color(0xFF00201F),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F2FC),
    surfaceContainer = Color(0xFFEFEDF7),
    surfaceContainerHigh = Color(0xFFE9E7F2),
    surfaceContainerHighest = Color(0xFFE3E1EC)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFBBC3FF),
    onPrimary = Color(0xFF0A1470),
    primaryContainer = Color(0xFF2C3A94),
    onPrimaryContainer = Color(0xFFDFE1FF),
    secondary = Color(0xFFC4C6E2),
    onSecondary = Color(0xFF2D3049),
    secondaryContainer = Color(0xFF434660),
    onSecondaryContainer = Color(0xFFE1E3FA),
    tertiary = Color(0xFF86D5D2),
    onTertiary = Color(0xFF003736),
    tertiaryContainer = Color(0xFF00504E),
    onTertiaryContainer = Color(0xFFA4F1EE),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A30),
    surfaceContainerHighest = Color(0xFF34343B)
)

@Composable
private fun VaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}

// ───────────────────────── Entry point (signature unchanged) ─────────────────────────

@Composable
fun PasswordManagerV2Screen(
    vaultId: String,
    memberUid: String
) {
    VaultTheme { PasswordManagerContent(vaultId, memberUid) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PasswordManagerContent(vaultId: String, memberUid: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hostView = LocalView.current
    val snackbar = remember { SnackbarHostState() }
    val clipboardHandler = remember { Handler(Looper.getMainLooper()) }

    // Protect the Password Manager and its sheets/dialogs from screenshots and recents previews.
    DisposableEffect(hostView) {
        val window = hostView.context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    fun notify(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text, withDismissAction = true)
        }
    }

    // Marks the clip as sensitive (hides the preview on Android 13+) and clears it later.
    fun copyCredential(label: String, value: String, secret: Boolean) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        if (secret) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        manager.setPrimaryClip(clip)
        if (secret && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboardHandler.removeCallbacksAndMessages(null)
            clipboardHandler.postDelayed({ manager.clearPrimaryClip() }, CLIPBOARD_CLEAR_MS)
            notify("$label copied. It will clear from the clipboard in 30 seconds.")
        } else {
            notify("$label copied.")
        }
    }

    var records by remember { mutableStateOf<List<PasswordRecordV2>>(emptyList()) }
    var selectedScope by rememberSaveable { mutableStateOf(PasswordRecordV2.SCOPE_PERSONAL) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var search by rememberSaveable { mutableStateOf("") }
    var sortOrder by rememberSaveable { mutableStateOf(SORT_OPTIONS.first()) }
    var showMenu by remember { mutableStateOf(false) }

    var selectedRecord by remember { mutableStateOf<PasswordRecordV2?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<PasswordRecordV2?>(null) }
    var recordToDelete by remember { mutableStateOf<PasswordRecordV2?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    // Defence in depth: never list the other member's personal records even if the repository returns them.
    val accessible = remember(records, memberUid) {
        records.filter { it.isShared || it.ownerUid == memberUid }
    }

    suspend fun reloadPasswords() {
        loading = true
        try {
            records = withContext(Dispatchers.IO) {
                PasswordRepositoryFirestoreV2.load(vaultId = vaultId, memberUid = memberUid)
            }
            // Schedule locally on every member device so shared-password reminders
            // are available to both approved household members.
            records.filter { it.isShared || it.ownerUid == memberUid }.forEach { record ->
                PasswordExpiryReminderScheduler.schedule(
                    context, record.id, record.websiteApp, record.expiryAt
                )
            }
        } catch (error: Exception) {
            notify(error.localizedMessage ?: "Couldn't load passwords. Check your connection and try again.")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(vaultId, memberUid) { reloadPasswords() }

    val categories = remember(accessible) {
        (PasswordRecordV2.CATEGORIES + accessible.map { it.category }).distinct()
    }
    val personalCount = accessible.count { it.scope == PasswordRecordV2.SCOPE_PERSONAL }
    val sharedCount = accessible.count { it.scope == PasswordRecordV2.SCOPE_SHARED }

    val visibleRecords = remember(accessible, selectedScope, selectedCategory, search, sortOrder) {
        accessible
            .filter { it.scope == selectedScope }
            .filter { selectedCategory == "All" || it.category == selectedCategory }
            .filter {
                search.isBlank() ||
                    it.websiteApp.contains(search, ignoreCase = true) ||
                    it.username.contains(search, ignoreCase = true) ||
                    it.category.contains(search, ignoreCase = true)
            }
            .let { filtered ->
                when (sortOrder) {
                    "Z–A" -> filtered.sortedByDescending { it.websiteApp.lowercase() }
                    "Newest first" -> filtered.sortedByDescending { it.createdAt }
                    "Oldest first" -> filtered.sortedBy { it.createdAt }
                    else -> filtered.sortedBy { it.websiteApp.lowercase() }
                }
            }
    }
    val groupByLetter = sortOrder == "A–Z" || sortOrder == "Z–A"
    val filtersActive = search.isNotBlank() || selectedCategory != "All"

    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Passwords", fontWeight = FontWeight.SemiBold) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            Text(
                                "Sort by",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SORT_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    leadingIcon = {
                                        if (option == sortOrder) Icon(Icons.Outlined.Check, null)
                                        else Spacer(Modifier.size(24.dp))
                                    },
                                    onClick = { sortOrder = option; showMenu = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Send test notification") },
                                onClick = {
                                    showMenu = false
                                    PasswordExpiryReminderScheduler.sendTestNotification(context)
                                    notify("Test notification sent. Check your notification shade.")
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                expanded = fabExpanded,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Add password") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (loading && records.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (loading) {
                    item(key = "progress") {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
                item(key = "search") {
                    TextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search passwords") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { search = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        )
                    )
                }
                item(key = "scope") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        SegmentedButton(
                            selected = selectedScope == PasswordRecordV2.SCOPE_PERSONAL,
                            onClick = { selectedScope = PasswordRecordV2.SCOPE_PERSONAL; selectedCategory = "All" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("Personal ($personalCount)") }
                        SegmentedButton(
                            selected = selectedScope == PasswordRecordV2.SCOPE_SHARED,
                            onClick = { selectedScope = PasswordRecordV2.SCOPE_SHARED; selectedCategory = "All" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("Shared ($sharedCount)") }
                    }
                }
                item(key = "categories") {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All").plus(categories).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }

                if (visibleRecords.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            isPersonal = selectedScope == PasswordRecordV2.SCOPE_PERSONAL,
                            filtersActive = filtersActive,
                            onAdd = { showAddSheet = true },
                            onClearFilters = { search = ""; selectedCategory = "All" }
                        )
                    }
                } else if (groupByLetter) {
                    visibleRecords.groupBy { initialOf(it.websiteApp) }.forEach { (letter, group) ->
                        stickyHeader(key = "header_$letter") { LetterHeader(letter) }
                        items(group, key = { it.id }) { record ->
                            PasswordRow(
                                record = record,
                                isMine = record.ownerUid == memberUid,
                                onClick = { selectedRecord = record },
                                onCopy = { copyCredential("Password", record.password, secret = true) }
                            )
                        }
                    }
                } else {
                    items(visibleRecords, key = { it.id }) { record ->
                        PasswordRow(
                            record = record,
                            isMine = record.ownerUid == memberUid,
                            onClick = { selectedRecord = record },
                            onCopy = { copyCredential("Password", record.password, secret = true) }
                        )
                    }
                }
            }
        }
    }

    // ───────────── Add ─────────────
    if (showAddSheet) {
        PasswordEditorSheet(
            saving = saving,
            defaultScope = selectedScope,
            categoryOptions = categories,
            onDismiss = { showAddSheet = false },
            onSave = { website, username, password, category, notes, recordScope, expiryAt ->
                scope.launch {
                    saving = true
                    try {
                        val created = withContext(Dispatchers.IO) {
                            PasswordRepositoryFirestoreV2.create(
                                vaultId = vaultId,
                                memberUid = memberUid,
                                scope = recordScope,
                                websiteApp = website,
                                username = username,
                                password = password,
                                category = category,
                                notes = notes,
                                expiryAt = expiryAt
                            )
                        }
                        PasswordExpiryReminderScheduler.schedule(context, created.id, created.websiteApp, created.expiryAt)
                        showAddSheet = false
                        selectedScope = recordScope
                        selectedCategory = "All"
                        reloadPasswords()
                        notify("Password saved.")
                    } catch (error: Exception) {
                        notify(error.localizedMessage ?: "Couldn't save the password. Try again.")
                    } finally {
                        saving = false
                    }
                }
            }
        )
    }

    // ───────────── Details ─────────────
    selectedRecord?.let { record ->
        PasswordDetailsSheet(
            record = record,
            isMine = record.ownerUid == memberUid,
            onDismiss = { selectedRecord = null },
            onCopyUsername = { copyCredential("Username", record.username, secret = false) },
            onCopyPassword = { copyCredential("Password", record.password, secret = true) },
            onEdit = { recordToEdit = record; selectedRecord = null },
            onDelete = { recordToDelete = record; selectedRecord = null }
        )
    }

    // ───────────── Edit ─────────────
    recordToEdit?.let { record ->
        PasswordEditorSheet(
            saving = saving,
            initialRecord = record,
            defaultScope = record.scope,
            categoryOptions = categories,
            onDismiss = { if (!saving) recordToEdit = null },
            onSave = { website, username, password, category, notes, _, expiryAt ->
                scope.launch {
                    saving = true
                    try {
                        val updated = withContext(Dispatchers.IO) {
                            PasswordRepositoryFirestoreV2.update(
                                vaultId, memberUid, record, website,
                                username, password, category, notes, expiryAt
                            )
                        }
                        PasswordExpiryReminderScheduler.schedule(context, updated.id, updated.websiteApp, updated.expiryAt)
                        recordToEdit = null
                        reloadPasswords()
                        notify("Changes saved.")
                    } catch (error: Exception) {
                        notify(error.localizedMessage ?: "Couldn't save your changes. Try again.")
                    } finally {
                        saving = false
                    }
                }
            }
        )
    }

    // ───────────── Delete ─────────────
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!saving) recordToDelete = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Delete ${record.websiteApp}?") },
            text = {
                Text(
                    "This removes the password from your vault and cancels its reminders. " +
                        "Provider backups may keep a copy for a short time."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        scope.launch {
                            saving = true
                            try {
                                withContext(Dispatchers.IO) {
                                    PasswordRepositoryFirestoreV2.delete(vaultId, memberUid, record)
                                }
                                PasswordExpiryReminderScheduler.cancel(context, record.id)
                                recordToDelete = null
                                reloadPasswords()
                                notify("Password deleted.")
                            } catch (error: Exception) {
                                notify(error.localizedMessage ?: "Couldn't delete the password. Try again.")
                            } finally {
                                saving = false
                            }
                        }
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { recordToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ───────────────────────── List pieces ─────────────────────────

@Composable
private fun LetterHeader(letter: String) {
    Text(
        letter,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun PasswordRow(
    record: PasswordRecordV2,
    isMine: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit
) {
    val expiry = remember(record.expiryAt) { expiryInfo(record.expiryAt) }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Monogram(record.websiteApp, 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    record.websiteApp,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    record.username.ifBlank { "No username" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (expiry.state == ExpiryState.SOON || expiry.state == ExpiryState.EXPIRED) {
                    Text(
                        expiry.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (expiry.state == ExpiryState.EXPIRED)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
                if (!isMine) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Person, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Added by your partner",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            FilledTonalIconButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy password for ${record.websiteApp}")
            }
        }
    }
}

@Composable
private fun Monogram(name: String, size: Dp) {
    val scheme = MaterialTheme.colorScheme
    val (background, foreground) = when (Math.floorMod(name.lowercase().hashCode(), 3)) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initialOf(name),
            color = foreground,
            style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyState(
    isPersonal: Boolean,
    filtersActive: Boolean,
    onAdd: () -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (filtersActive) Icons.Outlined.Search else Icons.Outlined.Lock, null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            when {
                filtersActive -> "No matches"
                isPersonal -> "No personal passwords yet"
                else -> "No shared passwords yet"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            when {
                filtersActive -> "Try a different search or category."
                isPersonal -> "Passwords you add here are visible only to you."
                else -> "Passwords you add here are visible to both of you. Only the person who added one can edit or delete it."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (filtersActive) {
            OutlinedButton(onClick = onClearFilters) { Text("Clear search and filters") }
        } else {
            Button(onClick = onAdd) { Text("Add password") }
        }
    }
}

// ───────────────────────── Details sheet ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordDetailsSheet(
    record: PasswordRecordV2,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit
) {
    var showPassword by remember(record.id) { mutableStateOf(false) }
    LaunchedEffect(showPassword) {
        if (showPassword) {
            delay(REVEAL_MS)
            showPassword = false
        }
    }
    val expiry = remember(record.expiryAt) { expiryInfo(record.expiryAt) }
    val shared = record.scope == PasswordRecordV2.SCOPE_SHARED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Monogram(record.websiteApp, 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        record.websiteApp,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        buildString {
                            append(record.category)
                            append(" – ")
                            append(
                                when {
                                    !shared -> "Personal"
                                    isMine -> "Shared by you"
                                    else -> "Shared by your partner"
                                }
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DetailBlock("Username or email") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.username.ifBlank { "Not added" }, Modifier.weight(1f))
                    if (record.username.isNotBlank()) {
                        IconButton(onClick = onCopyUsername) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy username")
                        }
                    }
                }
            }

            DetailBlock("Password") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showPassword) record.password else "••••••••••••",
                        modifier = Modifier.weight(1f),
                        fontFamily = if (showPassword) FontFamily.Monospace else FontFamily.Default
                    )
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                    IconButton(onClick = onCopyPassword) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy password")
                    }
                }
                if (record.password.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    PasswordStrengthIndicator(record.password)
                }
            }

            if (expiry.state != ExpiryState.NONE) {
                DetailBlock("Expiry") {
                    Text(
                        expiry.label,
                        color = when (expiry.state) {
                            ExpiryState.EXPIRED -> MaterialTheme.colorScheme.error
                            ExpiryState.SOON -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            if (record.notes.isNotBlank()) {
                DetailBlock("Notes") { Text(record.notes) }
            }

            if (isMine) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Only the person who added this password can edit or delete it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailBlock(label: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

// ───────────────────────── Add / edit sheet ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordEditorSheet(
    saving: Boolean,
    initialRecord: PasswordRecordV2? = null,
    defaultScope: String,
    categoryOptions: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, Long?) -> Unit
) {
    val isEdit = initialRecord != null
    var website by remember(initialRecord?.id) { mutableStateOf(initialRecord?.websiteApp.orEmpty()) }
    var username by remember(initialRecord?.id) { mutableStateOf(initialRecord?.username.orEmpty()) }
    var password by remember(initialRecord?.id) { mutableStateOf(initialRecord?.password.orEmpty()) }
    var category by remember(initialRecord?.id) { mutableStateOf(initialRecord?.category ?: "Other") }
    var notes by remember(initialRecord?.id) { mutableStateOf(initialRecord?.notes.orEmpty()) }
    var recordScope by remember(initialRecord?.id) { mutableStateOf(initialRecord?.scope ?: defaultScope) }
    var expiryAt by remember(initialRecord?.id) { mutableStateOf(initialRecord?.expiryAt) }

    var showPassword by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var addingCustomCategory by remember { mutableStateOf(false) }
    var customCategory by remember { mutableStateOf("") }
    var customCategoryError by remember { mutableStateOf("") }
    var showExpiryPicker by remember { mutableStateOf(false) }
    var websiteError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    val savingNow by rememberUpdatedState(saving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !savingNow }
    )

    if (showExpiryPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expiryAt)
        DatePickerDialog(
            onDismissRequest = { showExpiryPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expiryAt = datePickerState.selectedDateMillis
                    showExpiryPicker = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showExpiryPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isEdit) "Edit password" else "Add password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (!isEdit) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = recordScope == PasswordRecordV2.SCOPE_PERSONAL,
                        onClick = { recordScope = PasswordRecordV2.SCOPE_PERSONAL },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Personal") }
                    SegmentedButton(
                        selected = recordScope == PasswordRecordV2.SCOPE_SHARED,
                        onClick = { recordScope = PasswordRecordV2.SCOPE_SHARED },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Shared") }
                }
                Text(
                    if (recordScope == PasswordRecordV2.SCOPE_SHARED)
                        "Both of you can see it. Only you can edit or delete it."
                    else "Only you can see it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = website,
                onValueChange = { website = it.take(MAX_WEBSITE); websiteError = "" },
                label = { Text("Website or app") },
                supportingText = { Text(websiteError.ifBlank { "${website.length}/$MAX_WEBSITE" }) },
                isError = websiteError.isNotBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(MAX_USERNAME) },
                label = { Text("Username or email (optional)") },
                supportingText = { Text("${username.length}/$MAX_USERNAME") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(MAX_PASSWORD); passwordError = "" },
                label = { Text("Password") },
                supportingText = { Text(passwordError.ifBlank { "${password.length}/$MAX_PASSWORD" }) },
                isError = passwordError.isNotBlank(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    password = generatePassword(minOf(16, MAX_PASSWORD))
                    passwordError = ""
                    showPassword = true
                }) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate strong password")
                }
            }
            if (password.isNotBlank()) PasswordStrengthIndicator(password)

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                                addingCustomCategory = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Add custom category") },
                        leadingIcon = { Icon(Icons.Outlined.Add, null) },
                        onClick = {
                            categoryExpanded = false
                            customCategory = ""
                            customCategoryError = ""
                            addingCustomCategory = true
                        }
                    )
                }
            }

            if (addingCustomCategory) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it.take(MAX_CATEGORY); customCategoryError = "" },
                        label = { Text("New category") },
                        singleLine = true,
                        isError = customCategoryError.isNotBlank(),
                        supportingText = { Text(customCategoryError.ifBlank { "${customCategory.length}/$MAX_CATEGORY" }) },
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = {
                            val clean = customCategory.trim()
                            when {
                                clean.isBlank() -> customCategoryError = "Enter a category name."
                                categoryOptions.any { it.equals(clean, ignoreCase = true) } ->
                                    customCategoryError = "That category already exists."
                                else -> {
                                    category = clean
                                    addingCustomCategory = false
                                    customCategory = ""
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp)
                    ) { Text("Add") }
                }
            }

            OutlinedButton(onClick = { showExpiryPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    expiryAt?.let {
                        "Expires " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Set expiry date (optional)"
                )
            }
            if (expiryAt != null) {
                TextButton(onClick = { expiryAt = null }) { Text("Remove expiry date") }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(MAX_NOTES) },
                label = { Text("Notes (optional)") },
                supportingText = { Text("${notes.length}/$MAX_NOTES") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    enabled = !saving,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        websiteError = if (website.isBlank()) "Enter a website or app name." else ""
                        passwordError = if (password.isBlank()) "Enter the password." else ""
                        if (websiteError.isNotBlank() || passwordError.isNotBlank()) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            onSave(website, username, password, category, notes, recordScope, expiryAt)
                        }
                    }
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isEdit) "Save changes" else "Save password")
                    }
                }
            }
        }
    }
}

// ───────────────────────── Password strength ─────────────────────────

private enum class PasswordStrengthLevel(val label: String, val color: Color, val segments: Int) {
    VERY_WEAK("Very weak", Color(0xFFD14343), 1),
    WEAK("Weak", Color(0xFFE58A32), 2),
    FAIR("Fair", Color(0xFFD2A52A), 2),
    STRONG("Strong", Color(0xFF159B70), 3),
    VERY_STRONG("Very strong", Color(0xFF087F59), 4)
}

private fun evaluatePasswordStrength(password: String): PasswordStrengthLevel {
    if (password.isEmpty()) return PasswordStrengthLevel.VERY_WEAK
    val lower = password.lowercase()
    val variety = listOf(
        password.any { it.isLowerCase() },
        password.any { it.isUpperCase() },
        password.any { it.isDigit() },
        password.any { !it.isLetterOrDigit() }
    ).count { it }
    val repeated = password.length >= 3 && password.all { it == password.first() }
    val common = listOf("123456", "abcdef", "qwerty", "password", "letmein", "admin").any { lower.contains(it) }
    val score = (if (password.length >= 8) 1 else 0) +
        (if (password.length >= 12) 1 else 0) +
        (if (password.length >= 16) 1 else 0) +
        variety +
        (if (repeated) -3 else 0) +
        (if (common) -2 else 0)
    return when {
        repeated || password.length < 6 || score <= 2 -> PasswordStrengthLevel.VERY_WEAK
        score <= 4 -> PasswordStrengthLevel.WEAK
        score <= 5 -> PasswordStrengthLevel.FAIR
        score <= 7 -> PasswordStrengthLevel.STRONG
        else -> PasswordStrengthLevel.VERY_STRONG
    }
}

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = remember(password) { evaluatePasswordStrength(password) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Strength",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                strength.label,
                style = MaterialTheme.typography.labelMedium,
                color = strength.color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < strength.segments) strength.color
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }
        }
    }
}

// ───────────────────────── Helpers ─────────────────────────

private enum class ExpiryState { NONE, LATER, SOON, EXPIRED }

private data class ExpiryInfo(val state: ExpiryState, val label: String)

private fun expiryInfo(expiryAt: Long?): ExpiryInfo {
    if (expiryAt == null) return ExpiryInfo(ExpiryState.NONE, "No expiry date")
    val days = Math.floorDiv(expiryAt - System.currentTimeMillis(), DAY_MS)
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expiryAt))
    return when {
        days < 0 -> ExpiryInfo(ExpiryState.EXPIRED, "Expired on $date")
        days == 0L -> ExpiryInfo(ExpiryState.SOON, "Expires today")
        days == 1L -> ExpiryInfo(ExpiryState.SOON, "Expires tomorrow")
        days <= EXPIRY_SOON_DAYS -> ExpiryInfo(ExpiryState.SOON, "Expires in $days days")
        else -> ExpiryInfo(ExpiryState.LATER, "Expires on $date")
    }
}

private fun initialOf(name: String): String =
    name.trim().firstOrNull()?.takeIf { it.isLetter() }?.uppercaseChar()?.toString() ?: "#"

private fun generatePassword(length: Int = 16): String {
    val lower = "abcdefghijkmnopqrstuvwxyz"
    val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val digits = "23456789"
    val symbols = "!@#$%^&*-_=+?"
    val all = lower + upper + digits + symbols
    val random = SecureRandom()
    fun String.pick() = this[random.nextInt(this.length)]
    val chars = mutableListOf(lower.pick(), upper.pick(), digits.pick(), symbols.pick())
    while (chars.size < length) chars.add(all.pick())
    Collections.shuffle(chars, random)
    return chars.joinToString("")
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}