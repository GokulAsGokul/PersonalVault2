
package com.gokul.personalvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.outlined.Info
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import com.gokul.personalvault.PasswordRepository
import com.gokul.personalvault.HouseholdCrypto

private val PasswordNavy = Color(0xFF14213D)
private val PasswordBlue = Color(0xFF315CDB)
private val PasswordBackground = Color(0xFFF4F6FB)
private val PasswordMuted = Color(0xFF64748B)

private data class PasswordEntry(
    val id: String,
    val createdBy: String,
    val service: String,
    val username: String,
    val password: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Composable
fun PasswordsScreen(
    householdId: String,
    currentUid: String,
    ownerUid: String,
    memberUids: List<String>
) {

    val entries = remember {
        mutableStateListOf<PasswordEntry>()
    }
    val db = remember { FirebaseFirestore.getInstance() }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var screenMessage by remember { mutableStateOf<String?>(null) }

    fun refreshEntries() {
        isLoading = true
        PasswordRepository.load(db, householdId) { result ->
            isLoading = false
            result.onSuccess { records ->
                entries.clear()
                entries.addAll(records.map { record ->
                    PasswordEntry(
                        id = record.id,
                        createdBy = record.createdBy,
                        service = record.service,
                        username = record.username,
                        password = record.password,
                        category = record.category,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt
                    )
                })
                screenMessage = null
            }.onFailure { error ->
                screenMessage = error.message ?: "Unable to load passwords."
            }
        }
    }

    LaunchedEffect(householdId, currentUid) {
        refreshEntries()
    }

    var search by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember {
        mutableStateOf("All")
    }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingIndex by remember { mutableStateOf<Int?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var selectedEntry by remember {
        mutableStateOf<PasswordEntry?>(null)
    }
    var showDetailsPassword by remember {
        mutableStateOf(false)
    }
    var service by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showEditPassword by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var originalService by remember { mutableStateOf("") }
    var originalUsername by remember { mutableStateOf("") }
    var originalPassword by remember { mutableStateOf("") }
    var originalCategory by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }

    val filteredEntries = entries.filter { entry ->

        val query = search.trim()

        val matchesSearch =
            query.isEmpty() ||
                    entry.service.contains(query, ignoreCase = true) ||
                    entry.username.contains(query, ignoreCase = true) ||
                    entry.category.contains(query, ignoreCase = true)

        val matchesCategory =
            selectedCategoryFilter == "All" ||
                    entry.category == selectedCategoryFilter

        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PasswordBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            "PASSWORD VAULT",
            color = PasswordMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "Your passwords",
            color = PasswordNavy,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "Keep your accounts organised in one secure place.",
            color = PasswordMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = PasswordNavy
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Key,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        "Saved passwords",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${entries.size} encrypted accounts in your household",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        screenMessage?.let { message ->
            Text(message, color = Color(0xFFD94848), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        if (!HouseholdCrypto.isUnlocked(householdId)) {
            Text(
                "Household vault is locked. Return to Household connected and unlock it first.",
                color = Color(0xFFD94848),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search accounts") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(12.dp))

        CategoryFilterDropdown(
            selectedCategory = selectedCategoryFilter,
            onCategorySelected = {
                selectedCategoryFilter = it
            }
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Your accounts",
                color = PasswordNavy,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    if (!HouseholdCrypto.isUnlocked(householdId)) {
                        screenMessage = "Unlock the household vault before adding passwords."
                        return@Button
                    }
                    service = ""
                    username = ""
                    password = ""
                    category = "Personal"

                    originalService = ""
                    originalUsername = ""
                    originalPassword = ""
                    originalCategory = ""

                    showEditPassword = false
                    editingIndex = null
                    showDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PasswordBlue
                )
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredEntries.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Key,
                        contentDescription = null,
                        tint = PasswordBlue,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        if (isLoading) "Loading encrypted passwords..."
                        else if (search.isBlank()) "No passwords added yet"
                        else "No matching accounts",
                        fontWeight = FontWeight.Bold,
                        color = PasswordNavy
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        if (isLoading) "Please wait."
                        else if (search.isBlank()) "Tap Add to create your first account."
                        else "Try another search.",
                        color = PasswordMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

        } else {

            filteredEntries.forEach { entry ->

                val originalIndex = entries.indexOf(entry)

                PasswordEntryCard(
                    entry = entry,
                    ownerLabel = if (entry.createdBy == currentUid) "Mine" else "Spouse",
                    canEdit = entry.createdBy == currentUid,

                    onViewDetails = {
                        selectedEntry = entry
                        showDetailsPassword = false
                    },

                    onEdit = {
                        if (entry.createdBy != currentUid) return@PasswordEntryCard
                        service = entry.service
                        username = entry.username
                        password = entry.password
                        category = entry.category

                        originalService = entry.service
                        originalUsername = entry.username
                        originalPassword = entry.password
                        originalCategory = entry.category

                        showEditPassword = false
                        editingIndex = originalIndex
                        showDialog = true
                    },
                    onDelete = {
                        if (entry.createdBy == currentUid && originalIndex >= 0) {
                            deletingIndex = originalIndex
                            showDeleteDialog = true
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Security reminder",
            color = PasswordNavy,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "Password fields are encrypted with your household key before being saved to Firestore.",
            color = PasswordMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showDialog) {

        AlertDialog(
            onDismissRequest = {
                val hasChanges =
                    service != originalService ||
                            username != originalUsername ||
                            password != originalPassword ||
                            category != originalCategory

                if (editingIndex != null && hasChanges) {
                    showDiscardDialog = true
                } else {
                    showDialog = false
                }
            },
            title = {
                Text(
                    if (editingIndex == null)
                        "Add password"
                    else
                        "Edit password"
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {
                    OutlinedTextField(
                        value = service,
                        onValueChange = { service = it },
                        label = { Text("Service / Website") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Email") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (showEditPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showEditPassword = !showEditPassword
                                }
                            ) {
                                Icon(
                                    imageVector = if (showEditPassword) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    if (password.isNotEmpty()) {
                        val strength = getPasswordStrength(password)

                        val strengthColor = when (strength) {
                            "Very Weak" -> Color(0xFFD32F2F)
                            "Weak" -> Color(0xFFFF7043)
                            "Fair" -> Color(0xFFFFC107)
                            "Good" -> Color(0xFF8BC34A)
                            "Strong" -> Color(0xFF2E7D32)
                            else -> PasswordMuted
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Password strength:",
                                style = MaterialTheme.typography.bodySmall,
                                color = PasswordMuted
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                strength,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = strengthColor
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        val strengthLevel = when (strength) {
                            "Very Weak" -> 1
                            "Weak" -> 2
                            "Fair" -> 3
                            "Good" -> 4
                            "Strong" -> 5
                            else -> 0
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(5) { index ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .background(
                                            color = if (index < strengthLevel) {
                                                strengthColor
                                            } else {
                                                Color(0xFFE2E8F0)
                                            },
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }
                    }


                    Spacer(Modifier.height(8.dp))

                    CategoryDropdown(
                        selectedCategory = category,
                        onCategorySelected = { category = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isSaving) return@TextButton
                        if (!HouseholdCrypto.isUnlocked(householdId)) {
                            screenMessage = "Unlock the household vault before saving passwords."
                            return@TextButton
                        }
                        if (service.isBlank() || username.isBlank() || password.isBlank()) {
                            screenMessage = "Website/App, Username/Email and Password are required."
                            return@TextButton
                        }
                        val normalizedService = service.trim()
                        val duplicate = entries.any { existing ->
                            existing.service.trim().equals(normalizedService, ignoreCase = true) &&
                                existing.id != editingIndex?.let { entries.getOrNull(it)?.id }
                        }
                        if (duplicate) {
                            screenMessage = "A password for this Website/App already exists."
                            return@TextButton
                        }

                        isSaving = true
                        screenMessage = null
                        val editEntry = editingIndex?.let { entries.getOrNull(it) }
                        if (editEntry == null) {
                            PasswordRepository.add(
                                db = db,
                                householdId = householdId,
                                currentUid = currentUid,
                                service = normalizedService,
                                username = username.trim(),
                                password = password,
                                category = category.trim().ifBlank { "Personal" }
                            ) { result ->
                                isSaving = false
                                result.onSuccess {
                                    showDialog = false
                                    editingIndex = null
                                    refreshEntries()
                                }.onFailure { error ->
                                    screenMessage = error.message ?: "Could not save password."
                                }
                            }
                        } else {
                            if (editEntry.createdBy != currentUid) {
                                isSaving = false
                                screenMessage = "You can edit only your own passwords."
                                return@TextButton
                            }
                            PasswordRepository.update(
                                db = db,
                                householdId = householdId,
                                recordId = editEntry.id,
                                currentUid = currentUid,
                                service = normalizedService,
                                username = username.trim(),
                                password = password,
                                category = category.trim().ifBlank { "Personal" }
                            ) { result ->
                                isSaving = false
                                result.onSuccess {
                                    showDialog = false
                                    editingIndex = null
                                    refreshEntries()
                                }.onFailure { error ->
                                    screenMessage = error.message ?: "Could not update password."
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val hasChanges =
                            service != originalService ||
                                    username != originalUsername ||
                                    password != originalPassword ||
                                    category != originalCategory

                        if (editingIndex != null && hasChanges) {
                            showDiscardDialog = true
                        } else {
                            showDialog = false
                        }
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    selectedEntry?.let { entry ->

        val clipboardManager = LocalClipboardManager.current

        AlertDialog(
            onDismissRequest = {
                selectedEntry = null
            },
            title = {
                Column {
                    Text(entry.service)
                    Text(
                        if (entry.createdBy == currentUid) "Mine" else "Spouse",
                        color = PasswordMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getCategoryIcon(entry.category),
                            contentDescription = null,
                            tint = PasswordBlue
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            entry.category,
                            color = PasswordMuted
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "USERNAME / EMAIL",
                        style = MaterialTheme.typography.labelSmall,
                        color = PasswordMuted
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            entry.username,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            clipboardManager.setText(
                                AnnotatedString(entry.username)
                            )
                        }) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = "Copy username"
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "PASSWORD",
                        style = MaterialTheme.typography.labelSmall,
                        color = PasswordMuted
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (showDetailsPassword) {
                                entry.password
                            } else {
                                "••••••••"
                            },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            clipboardManager.setText(
                                AnnotatedString(entry.password)
                            )
                        }) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = "Copy password"
                            )
                        }

                        IconButton(onClick = {
                            showDetailsPassword = !showDetailsPassword
                        }) {
                            Icon(
                                if (showDetailsPassword)
                                    Icons.Outlined.VisibilityOff
                                else
                                    Icons.Outlined.Visibility,
                                contentDescription = "Show or hide password"
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Divider()

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Created: ${formatPasswordDate(entry.createdAt)}",
                        color = PasswordMuted,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Last edited: ${formatPasswordDate(entry.updatedAt)}",
                        color = PasswordMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedEntry = null
                }) {
                    Text("Close")
                }
            },
            dismissButton = {
                if (entry.createdBy == currentUid) {
                    TextButton(onClick = {
                        val index = entries.indexOf(entry)
                        if (index >= 0) {
                            service = entry.service
                            username = entry.username
                            password = entry.password
                            category = entry.category
                            originalService = entry.service
                            originalUsername = entry.username
                            originalPassword = entry.password
                            originalCategory = entry.category
                            editingIndex = index
                            selectedEntry = null
                            showDialog = true
                        }
                    }) {
                        Text("Edit")
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletingIndex = null
            },
            title = {
                Text("Delete password?")
            },
            text = {
                Text(
                    "Are you sure you want to delete this password? " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isSaving) return@TextButton
                        val target = deletingIndex?.let { entries.getOrNull(it) }
                        if (target == null || target.createdBy != currentUid) {
                            screenMessage = "You can delete only your own passwords."
                            showDeleteDialog = false
                            deletingIndex = null
                            return@TextButton
                        }
                        isSaving = true
                        PasswordRepository.delete(
                            db = db,
                            householdId = householdId,
                            recordId = target.id,
                            currentUid = currentUid
                        ) { result ->
                            isSaving = false
                            result.onSuccess {
                                showDeleteDialog = false
                                deletingIndex = null
                                refreshEntries()
                            }.onFailure { error ->
                                screenMessage = error.message ?: "Could not delete password."
                            }
                        }
                    }
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFD94848)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deletingIndex = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
            },
            title = {
                Text("Discard changes?")
            },
            text = {
                Text(
                    "You have unsaved changes. " +
                            "Are you sure you want to discard them?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        showDialog = false
                        editingIndex = null
                    }
                ) {
                    Text(
                        "Discard",
                        color = Color(0xFFD94848)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                    }
                ) {
                    Text("Keep editing")
                }
            }
        )
    }

}

private fun formatPasswordDate(timestamp: Long): String {
    val formatter = SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    )
    return formatter.format(Date(timestamp))
}

@Composable
private fun PasswordEntryCard(
    entry: PasswordEntry,
    ownerLabel: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewDetails: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Key,
                    contentDescription = null,
                    tint = PasswordBlue,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        entry.service,
                        color = PasswordNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(entry.category),
                            contentDescription = null,
                            tint = PasswordBlue,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(Modifier.width(5.dp))

                        Text(
                            entry.category,
                            color = PasswordMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            ownerLabel,
                            color = if (canEdit) PasswordBlue else PasswordMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onViewDetails) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "View details",
                        tint = PasswordBlue
                    )
                }

                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFD94848)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "USERNAME",
                color = PasswordMuted,
                style = MaterialTheme.typography.labelSmall
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    entry.username,
                    color = PasswordNavy,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString(entry.username)
                        )
                    }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Copy username",
                        tint = PasswordBlue
                    )
                }
            }

            Spacer(Modifier.height(8.dp))


            Text(
                "PASSWORD",
                color = PasswordMuted,
                style = MaterialTheme.typography.labelSmall
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Password text
                Text(
                    if (visible) entry.password else "••••••••",
                    color = PasswordNavy,
                    modifier = Modifier.weight(1f)
                )

                // Copy password
                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString(entry.password)
                        )
                    }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Copy password",
                        tint = PasswordBlue
                    )
                }

                // Show / Hide password
                IconButton(
                    onClick = { visible = !visible }
                ) {
                    Icon(
                        if (visible)
                            Icons.Outlined.VisibilityOff
                        else
                            Icons.Outlined.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = PasswordBlue
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Personal",
        "Email",
        "Banking & Finance",
        "Shopping",
        "Social Media",
        "Work & Business",
        "Entertainment",
        "Travel",
        "Government",
        "Education",
        "Utilities",
        "Other"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            categories.forEach { item ->

                DropdownMenuItem(
                    text = {
                        Text(item)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(item),
                            contentDescription = null,
                            tint = PasswordBlue
                        )
                    },
                    onClick = {
                        onCategorySelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Personal" -> Icons.Outlined.Person
        "Email" -> Icons.Outlined.Email
        "Banking & Finance" -> Icons.Outlined.AccountBalance
        "Shopping" -> Icons.Outlined.ShoppingCart
        "Social Media" -> Icons.Outlined.Share
        "Work & Business" -> Icons.Outlined.Work
        "Entertainment" -> Icons.Outlined.Movie
        "Travel" -> Icons.Outlined.Flight
        "Government" -> Icons.Outlined.VerifiedUser
        "Education" -> Icons.Outlined.School
        "Utilities" -> Icons.Outlined.Bolt
        else -> Icons.Outlined.Folder
    }
}

private fun getPasswordStrength(password: String): String {
    if (password.isEmpty()) return ""

    var score = 0

    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 1 -> "Very Weak"
        score == 2 -> "Weak"
        score == 3 -> "Fair"
        score == 4 -> "Good"
        else -> "Strong"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "All",
        "Personal",
        "Email",
        "Banking & Finance",
        "Shopping",
        "Social Media",
        "Work & Business",
        "Entertainment",
        "Travel",
        "Government",
        "Education",
        "Utilities",
        "Other"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filter by category") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            categories.forEach { category ->

                DropdownMenuItem(
                    text = {
                        Text(category)
                    },
                    leadingIcon = {
                        if (category == "All") {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = null
                            )
                        } else {
                            Icon(
                                getCategoryIcon(category),
                                contentDescription = null
                            )
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
