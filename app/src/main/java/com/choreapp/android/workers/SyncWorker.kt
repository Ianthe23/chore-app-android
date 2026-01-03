package com.choreapp.android.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choreapp.android.R
import com.choreapp.android.repository.ChoreRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SyncWorker"
        const val CHANNEL_ID = "sync_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")

        return try {
            val repository = ChoreRepository(applicationContext)
            val syncedCount = repository.syncPendingOperations()

            if (syncedCount > 0) {
                showSyncNotification(syncedCount)
                Log.d(TAG, "Successfully synced $syncedCount operations")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    private fun showSyncNotification(syncedCount: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sync Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background synchronization notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Chores Synced")
            .setContentText("Successfully synced $syncedCount chore(s)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}