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

/**
 * Keeps an in-process Firebase Storage upload alive while the activity is backgrounded.
 * Removing the app task explicitly cancels the screen-owned upload coroutine.
 */
class DocumentUploadForegroundService : Service() {
    private var notificationTitle = "Uploading documents"

    override fun onCreate() {
        super.onCreate()
        activeService = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationTitle = intent?.getStringExtra(EXTRA_TITLE)
            ?.takeIf { it.isNotBlank() } ?: "Uploading documents"
        val notification = buildNotification(0, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // A deliberate removal of the app from Recents is treated as closing the app.
        cancelAction?.invoke()
        cancelAction = null
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
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

    private fun buildNotification(progress: Int, indeterminate: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(notificationTitle)
            .setContentText(if (indeterminate) "Preparing upload…" else "$progress% uploaded")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), indeterminate)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

    private fun updateProgress(progress: Int, indeterminate: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(progress, indeterminate))
    }

    companion object {
        private const val CHANNEL_ID = "document_uploads"
        private const val NOTIFICATION_ID = 2407
        private const val EXTRA_TITLE = "upload_title"

        @Volatile
        private var activeService: DocumentUploadForegroundService? = null

        @Volatile
        private var cancelAction: (() -> Unit)? = null

        fun start(context: Context, title: String) {
            val intent = Intent(context, DocumentUploadForegroundService::class.java)
                .putExtra(EXTRA_TITLE, title)
            ContextCompat.startForegroundService(context, intent)
        }

        fun setCancelAction(action: (() -> Unit)?) {
            cancelAction = action
        }

        fun updateProgress(progress: Int) {
            activeService?.updateProgress(progress, false)
        }

        fun cancelAndStop(context: Context) {
            cancelAction?.invoke()
            cancelAction = null
            context.stopService(Intent(context, DocumentUploadForegroundService::class.java))
        }

        fun stop(context: Context) {
            cancelAction = null
            context.stopService(Intent(context, DocumentUploadForegroundService::class.java))
        }
    }
}
