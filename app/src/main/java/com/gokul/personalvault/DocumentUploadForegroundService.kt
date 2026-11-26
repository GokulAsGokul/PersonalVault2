package com.gokul.personalvault

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gokul.personalvault.data.firebase.DocumentRepositoryV2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class DocumentUploadFailure(
    val uri: String,
    val fileName: String,
    val sizeBytes: Long,
    val title: String,
    val notes: String,
    val categoryId: String,
    val scope: String,
    val folderId: String?,
    val errorMessage: String,
    val retryBlocked: Boolean
)

sealed class DocumentUploadUiState {
    object Idle : DocumentUploadUiState()
    data class Running(
        val fileName: String,
        val fileIndex: Int,
        val totalFiles: Int,
        val fileProgress: Float,
        val overallProgress: Float
    ) : DocumentUploadUiState()
    data class Completed(
        val uploadedCount: Int,
        val failures: List<DocumentUploadFailure>
    ) : DocumentUploadUiState()
    data class Failed(val message: String) : DocumentUploadUiState()
    object Cancelled : DocumentUploadUiState()
}

/**
 * Owns document upload work independently of the Compose screen. The foreground
 * service keeps the process alive while the device is locked or another app is open.
 * Removing the app task explicitly cancels the active Firebase Storage upload.
 */
class DocumentUploadForegroundService : Service() {
    private var notificationTitle = "Uploading documents"
    private var serviceScope: CoroutineScope? = null
    private var uploadJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        activeService = this
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationTitle = intent?.getStringExtra(EXTRA_TITLE)
            ?.takeIf { it.isNotBlank() } ?: "Uploading documents"
        startAsForeground(buildNotification(0, true))

        val uploadIntent = intent
        val uploadsJson = uploadIntent?.getStringExtra(EXTRA_UPLOADS_JSON)
        if (!uploadsJson.isNullOrBlank()) {
            uploadJob?.cancel()
            uploadJob = serviceScope?.launch {
                try {
                    runBatchUpload(
                        uploadIntent?.getStringExtra(EXTRA_VAULT_ID).orEmpty(),
                        uploadIntent?.getStringExtra(EXTRA_OWNER_UID).orEmpty(),
                        uploadsJson
                    )
                } catch (_: CancellationException) {
                    mutableUploadState.value = DocumentUploadUiState.Cancelled
                } catch (error: Exception) {
                    val errorText = error.localizedMessage ?: error.message ?: "Please retry."
                    mutableUploadState.value = DocumentUploadUiState.Failed(errorText)
                    updateNotification(0, false, "Upload failed: $errorText")
                } finally {
                    setCancelAction(null)
                    stopForegroundCompat()
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun runBatchUpload(vaultId: String, ownerUid: String, uploadsJson: String) {
        require(vaultId.isNotBlank() && ownerUid.isNotBlank()) {
            "The vault session is unavailable. Reopen the vault and retry."
        }
        val payload = JSONArray(uploadsJson)
        require(payload.length() > 0) { "No files were selected for upload." }

        val repository = DocumentRepositoryV2()
        var uploadedCount = 0
        val failures = mutableListOf<DocumentUploadFailure>()
        val total = payload.length()

        for (index in 0 until total) {
            val item = payload.getJSONObject(index)
            val uri = item.getString("uri")
            val fileName = item.getString("fileName")
            val sizeBytes = item.getLong("sizeBytes")
            val title = item.getString("title")
            val notes = item.optString("notes", "")
            val categoryId = item.getString("categoryId")
            val visibility = item.getString("scope")
            val folderId = item.optString("folderId").takeIf { it.isNotBlank() && it != "null" }
            var fileProgress = 0f

            try {
                repository.upload(
                    vaultId = vaultId,
                    ownerUid = ownerUid,
                    uri = android.net.Uri.parse(uri),
                    resolver = contentResolver,
                    title = title,
                    categoryId = categoryId,
                    description = notes,
                    scope = visibility,
                    folderId = folderId,
                    onUploadTaskCreated = { task ->
                        setCancelAction { task?.cancel() }
                    }
                ) { value ->
                    fileProgress = value.coerceIn(0f, 1f)
                    val overall = ((index + fileProgress) / total.toFloat()).coerceIn(0f, 1f)
                    mutableUploadState.value = DocumentUploadUiState.Running(
                        fileName = fileName,
                        fileIndex = index + 1,
                        totalFiles = total,
                        fileProgress = fileProgress,
                        overallProgress = overall
                    )
                    updateNotification((overall * 100).toInt(), false)
                }
                uploadedCount++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val errorText = error.localizedMessage ?: error.message ?: "Upload failed"
                failures += DocumentUploadFailure(
                    uri = uri,
                    fileName = fileName,
                    sizeBytes = sizeBytes,
                    title = title,
                    notes = notes,
                    categoryId = categoryId,
                    scope = visibility,
                    folderId = folderId,
                    errorMessage = errorText,
                    retryBlocked = error.message?.contains("Do not retry yet", ignoreCase = true) == true
                )
            }
        }

        mutableUploadState.value = DocumentUploadUiState.Completed(uploadedCount, failures)
        val completionText = if (failures.isEmpty()) {
            "$uploadedCount document(s) uploaded successfully."
        } else {
            "$uploadedCount uploaded, ${failures.size} failed."
        }
        updateNotification(100, false, completionText, ongoing = false)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Explicitly swiping Personal Vault away from Recents cancels the active upload.
        uploadJob?.cancel()
        cancelAction?.invoke()
        cancelAction = null
        mutableUploadState.value = DocumentUploadUiState.Cancelled
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (activeService === this) activeService = null
        uploadJob?.cancel()
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    private fun startAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Document uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while documents are being uploaded"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        progress: Int,
        indeterminate: Boolean,
        text: String? = null,
        ongoing: Boolean = true
    ): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(notificationTitle)
            .setContentText(text ?: if (indeterminate) "Preparing upload…" else "$progress% uploaded")
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), indeterminate)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

    private fun updateNotification(
        progress: Int,
        indeterminate: Boolean,
        text: String? = null,
        ongoing: Boolean = true
    ) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(progress, indeterminate, text, ongoing))
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }

    companion object {
        private const val CHANNEL_ID = "document_uploads"
        private const val NOTIFICATION_ID = 2407
        private const val EXTRA_TITLE = "upload_title"
        private const val EXTRA_UPLOADS_JSON = "uploads_json"
        private const val EXTRA_VAULT_ID = "vault_id"
        private const val EXTRA_OWNER_UID = "owner_uid"

        @Volatile
        private var activeService: DocumentUploadForegroundService? = null

        @Volatile
        private var cancelAction: (() -> Unit)? = null

        private val mutableUploadState = MutableStateFlow<DocumentUploadUiState>(DocumentUploadUiState.Idle)
        val uploadState: StateFlow<DocumentUploadUiState> = mutableUploadState.asStateFlow()

        fun start(context: Context, title: String) {
            val intent = Intent(context, DocumentUploadForegroundService::class.java)
                .putExtra(EXTRA_TITLE, title)
            ContextCompat.startForegroundService(context, intent)
        }

        fun startUploads(context: Context, vaultId: String, ownerUid: String, uploads: JSONArray) {
            mutableUploadState.value = DocumentUploadUiState.Idle
            val intent = Intent(context, DocumentUploadForegroundService::class.java)
                .putExtra(EXTRA_TITLE, "Uploading documents")
                .putExtra(EXTRA_VAULT_ID, vaultId)
                .putExtra(EXTRA_OWNER_UID, ownerUid)
                .putExtra(EXTRA_UPLOADS_JSON, uploads.toString())
            ContextCompat.startForegroundService(context, intent)
        }

        fun acknowledgeResult() {
            if (mutableUploadState.value is DocumentUploadUiState.Completed ||
                mutableUploadState.value is DocumentUploadUiState.Cancelled
            ) {
                mutableUploadState.value = DocumentUploadUiState.Idle
            }
        }

        fun setCancelAction(action: (() -> Unit)?) {
            cancelAction = action
        }

        fun updateProgress(progress: Int) {
            activeService?.updateNotification(progress, false)
        }

        fun cancelAndStop(context: Context) {
            activeService?.uploadJob?.cancel()
            cancelAction?.invoke()
            cancelAction = null
            mutableUploadState.value = DocumentUploadUiState.Cancelled
            context.stopService(Intent(context, DocumentUploadForegroundService::class.java))
        }

        fun stop(context: Context) {
            cancelAction = null
            context.stopService(Intent(context, DocumentUploadForegroundService::class.java))
        }
    }
}
