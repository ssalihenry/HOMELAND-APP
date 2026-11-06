package com.homeland.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun CheckForUpdates(activity: Activity) {
    var updateAvailable by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadId by remember { mutableStateOf(-1L) }

    // Check Firestore for the latest version
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("config").document("app_version").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val latestVersion: Long = doc.getLong("latest_version_code") ?: 1L
val currentVersion: Long = BuildConfig.VERSION_CODE.toLong()

if (latestVersion > currentVersion) {
                    updateUrl = doc.getString("update_url") ?: ""
                    updateAvailable = true
                }
            }
        }
    }

    // Handle the download progress
    if (isDownloading) {
        LaunchedEffect(downloadId) {
            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (bytesTotal > 0) {
                        downloadProgress = (bytesDownloaded * 100L / bytesTotal).toInt()
                    }
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                        isDownloading = false
                        installApk(activity, downloadId)
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                        isDownloading = false
                        Toast.makeText(activity, "Download failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                cursor.close()
                delay(500)
            }
        }
    }

    // The un-closable popup
    if (updateAvailable) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss by tapping outside */ },
            title = { Text("Update Required", color = Color.White, fontSize = 20.sp) },
            text = { 
                Column {
                    Text("A new version of Homeland is available. You must update to continue.", color = Color.Gray)
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.fillMaxWidth(), color = Color(0xFFE50914))
                        Text("Downloading... $downloadProgress%", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            if (updateUrl.isNotBlank()) {
                                isDownloading = true
                                val request = DownloadManager.Request(Uri.parse(updateUrl))
                                request.setTitle("Homeland Update")
                                request.setDescription("Downloading new version...")
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "homeland_update.apk")
                                
                                val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadId = downloadManager.enqueue(request)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) {
                        Text("Download & Install")
                    }
                }
            },
            containerColor = Color(0xFF15171E)
        )
    }
}

private fun installApk(activity: Activity, downloadId: Long) {
    val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val uri = downloadManager.getUriForDownloadedFile(downloadId)
    
    if (uri != null) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    } else {
        // Fallback for FileProvider
        val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "homeland_update.apk")
        if (file.exists()) {
            val apkUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }
}
