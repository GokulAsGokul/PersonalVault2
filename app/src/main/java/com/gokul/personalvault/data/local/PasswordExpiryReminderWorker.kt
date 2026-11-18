package com.gokul.personalvault.data.local

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class PasswordExpiryReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val website = inputData.getString(KEY_WEBSITE)?.takeIf { it.isNotBlank() }
            ?: return Result.failure()
        val days = inputData.getInt(KEY_DAYS, 0)
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        return try {
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("Password expiry reminder")
                .setContentText("$website is due to expire in $days days.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext)
                .notify((website.hashCode() * 31 + days), notification)
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Password expiry reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_WEBSITE = "website"
        const val KEY_DAYS = "days"
        const val CHANNEL_ID = "password_expiry_reminders"
    }
}

object PasswordExpiryReminderScheduler {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private fun tag(recordId: String) = "password-expiry-$recordId"

    fun schedule(context: Context, recordId: String, website: String, expiryAt: Long?) {
        cancel(context, recordId)
        if (expiryAt == null) return

        listOf(7, 3, 1).forEach { days ->
            val reminderAt = expiryAt - (days * DAY_MILLIS)
            val delay = reminderAt - System.currentTimeMillis()
            if (delay > 0L) {
                val requestData: Data = workDataOf(
                    PasswordExpiryReminderWorker.KEY_WEBSITE to website,
                    PasswordExpiryReminderWorker.KEY_DAYS to days
                )
                val request = OneTimeWorkRequestBuilder<PasswordExpiryReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(requestData)
                    .addTag(tag(recordId))
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "password-expiry-$recordId-$days",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }

    fun cancel(context: Context, recordId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag(recordId))
    }
}
