package com.homeland.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HomelandFCM"
        private const val CHANNEL_ID = "homeland_default_channel"
        private const val CHANNEL_NAME = "Homeland Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // This fires whenever the device gets a fresh FCM token.
        // Helpful for debugging — visible in logcat / Logcat Reader app.
        Log.d(TAG, "New FCM token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived from: ${remoteMessage.from}")
        Log.d(TAG, "Notification payload: title=${remoteMessage.notification?.title} body=${remoteMessage.notification?.body}")
        Log.d(TAG, "Data payload: ${remoteMessage.data}")

        // Read from notification payload first, then fallback to data payload.
        // This way, both notification-type AND data-only sends will work.
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Homeland App"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "New update available!"

        Log.d(TAG, "Showing notification -> title=$title, body=$body")
        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        // 1) Create channel (Android 8+)
        createChannelIfNeeded()

        // 2) Runtime permission check (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted — notification dropped.")
                return
            }
        }

        // 3) Tap intent -> open MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_notification", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4) Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 5) Post — unique ID so notifications stack instead of overwriting
        val notificationId = System.currentTimeMillis().toInt()
        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
            Log.d(TAG, "Notification posted with id=$notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification: ${e.message}")
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Homeland push notifications"
                enableVibration(true)
                enableLights(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
