package com.homeland.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// 🔥 ADDED IMPORTS FOR YOCINEMA SYNC & BACKGROUND SCOPE & JSON IMPORT
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import com.google.android.gms.tasks.Tasks

// 🔥 GLOBAL BACKGROUND SYNC SCOPE & STATE
val AppSyncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
var globalIsSyncing by mutableStateOf(false)
var globalSyncProgressMsg by mutableStateOf("")

val vjList = listOf("VJ Akaana", "VJ Arafa", "VJ Aron", "VJ Ashim", "VJ Ashim J", "VJ Banks", "VJ Baros", "VJ Bonny", "VJ Cabs", "VJ CB", "VJ Dan De", "VJ Eddy", "VJ Emmy", "VJ Fredy", "VJ Geofry", "VJ Ghost King", "VJ Ham", "VJ Harderway", "VJ HD", "VJ Heavy Q", "VJ Henrico", "VJ Isma", "VJ Isma K", "VJ Ivo", "VJ Jimmy", "VJ Jingo", "VJ Jovan", "VJ Jumpers", "VJ Junior", "VJ Kam", "VJ Kamran", "VJ Kevo", "VJ Kevin", "VJ Khan Lee", "VJ Kimuli", "VJ Kin", "VJ Kriss", "VJ Kriss S", "VJ Kriss Sweet", "VJ KS", "VJ Lance", "VJ Lash", "VJ Light", "VJ Little T", "VJ Mark", "VJ Martin K", "VJ MK", "VJ Monks", "VJ Mosco", "VJ Mox", "VJ Muba", "VJ Mukibi", "VJ Mun", "VJ Musa", "VJ Neil", "VJ Nelly", "VJ Nelson", "VJ Omutaka Ice P", "VJ Pauleta", "VJ Pax Woo", "VJ Phoenix", "VJ Raji", "VJ Roddy", "VJ Ronage", "VJ Ronnie", "VJ Ryan", "VJ Sammy", "VJ Shao Khan", "VJ Sharz", "VJ Shield", "VJ Silver", "VJ SMK", "VJ Son", "VJ Soul", "VJ Tom", "VJ Tonny", "VJ Ulio", "VJ Uncle T", "VJ Waza", "VJ Yat", "VJ Zaidi")
val categories = listOf("Action", "Sci-Fi", "Comedy", "Animation", "Romantic", "Horror", "Adventure")

data class AdminUser(
    val uid: String = "",
    val email: String = "",
    val homelandVipExpiry: Long = 0L,
    val isBanned: Boolean = false
)

data class VipPlan(
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val days: Int = 0
)

data class PaymentRequest(
    val id: String = "",
    val email: String = "",
    val tid: String = "",
    val planName: String = "",
    val price: String = "",
    val days: Int = 0,
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("homeland_prefs", Context.MODE_PRIVATE)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var selectedTab by remember { mutableIntStateOf(0) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // 🔥 YOCINEMA SYNC STATE
    var yocApiKey by remember { mutableStateOf(prefs.getString("yoc_api_key", "") ?: "") }
    var lastSyncTime by remember { mutableLongStateOf(prefs.getLong("yoc_last_sync", 0L)) }

    // 🔥 MOVIES JSON IMPORT STATE
    var isImportingJson by remember { mutableStateOf(false) }
    var jsonImportProgress by remember { mutableStateOf("") }
    var lastJsonUri by remember { mutableStateOf<String?>(prefs.getString("last_json_uri", null)) }
    var pendingCount by remember { mutableIntStateOf(prefs.getInt("pending_count", 0)) }

    // 🔥 TV JSON IMPORT STATE
    var isImportingTvJson by remember { mutableStateOf(false) }
    var tvJsonProgress by remember { mutableStateOf("") }
    var lastTvJsonUri by remember { mutableStateOf<String?>(prefs.getString("last_tv_json_uri", null)) }
    var pendingTvCount by remember { mutableIntStateOf(prefs.getInt("pending_tv_count", 0)) }

    var editingMovieId by remember { mutableStateOf<String?>(null) }
    var editingTvId by remember { mutableStateOf<String?>(null) }

    // Movie State
    var title by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var subtitleUrl by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2026") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedVj by remember { mutableStateOf(vjList[0]) }
    var selectedType by remember { mutableStateOf("Movie") }
    var isVipMovie by remember { mutableStateOf(false) }
    var isFeaturedMovie by remember { mutableStateOf(false) }
    var isAdultMovie by remember { mutableStateOf(false) }
    var episodesList by remember { mutableStateOf<List<Episode>>(emptyList()) }

    var catExpanded by remember { mutableStateOf(false) }
    var vjExpanded by remember { mutableStateOf(false) }

    // TV State
    var tvName by remember { mutableStateOf("") }
    var tvLogo by remember { mutableStateOf("") }
    var tvUrl by remember { mutableStateOf("") }
    var tvCategory by remember { mutableStateOf("News") }
    var isVipTv by remember { mutableStateOf(false) }
    var isFeaturedTv by remember { mutableStateOf(false) }

    // VIP & Theme & Push State
    var couponDays by remember { mutableStateOf("7") }
    var allUsers by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var themeFontUrl by remember { mutableStateOf("") }
    var themeBgImageUrl by remember { mutableStateOf("") }

    var customVipDays by remember { mutableStateOf("30") }
    var newAdminPin by remember { mutableStateOf("") }

    var pushTitle by remember { mutableStateOf("") }
    var pushMessage by remember { mutableStateOf("") }
    var pushDeepLink by remember { mutableStateOf("") }
    var selectedPushMovieTitle by remember { mutableStateOf("None (Open Home Screen)") }
    var pushMovieExpanded by remember { mutableStateOf(false) }

    var isMaintenanceActive by remember { mutableStateOf(false) }
    var maintenanceMsg by remember { mutableStateOf("") }

    var whatsappLink by remember { mutableStateOf("") }
    var telegramLink by remember { mutableStateOf("") }
    var tiktokLink by remember { mutableStateOf("") }
    var twitterLink by remember { mutableStateOf("") }
    var instagramLink by remember { mutableStateOf("") }

    var vipPlans by remember { mutableStateOf<List<VipPlan>>(emptyList()) }
    var newPlanName by remember { mutableStateOf("") }
    var newPlanPrice by remember { mutableStateOf("") }
    var newPlanDays by remember { mutableStateOf("") }

    var paymentRequests by remember { mutableStateOf<List<PaymentRequest>>(emptyList()) }

    var existingMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var existingTv by remember { mutableStateOf<List<TvChannel>>(emptyList()) }

    // ═══════════════════════════════════════════════════════════════════
    // 🔥 MOVIES — Batch Importer (100 per tap)
    // ═══════════════════════════════════════════════════════════════════
    val runBatchImport: (android.net.Uri) -> Unit = { uri ->
        isImportingJson = true
        jsonImportProgress = "Reading file..."
        statusMessage = null

        AppSyncScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (jsonString.isNullOrBlank()) throw Exception("File is empty.")

                val trimmed = jsonString.trim().removePrefix("\uFEFF").trim()
                val jsonArray: JSONArray = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val root = JSONObject(trimmed)
                    var found: JSONArray? = null
                    for (key in listOf("movies", "data", "items", "results", "list", "content")) {
                        val arr = root.optJSONArray(key)
                        if (arr != null) { found = arr; break }
                    }
                    if (found == null) {
                        val dataObj = root.optJSONObject("data")
                        if (dataObj != null) {
                            for (key in listOf("movies", "items", "results")) {
                                val arr = dataObj.optJSONArray(key)
                                if (arr != null) { found = arr; break }
                            }
                        }
                    }
                    found ?: JSONArray().apply { put(root) }
                }

                val totalInFile = jsonArray.length()
                Log.d("JSON_IMPORT", "File has $totalInFile items")

                mainHandler.post { jsonImportProgress = "Checking database ($totalInFile in file)..." }
                val existingSlugs: Set<String> = try {
                    val snap = Tasks.await(db.collection("homeland_movies").get(), 20, TimeUnit.SECONDS)
                    snap.documents.mapNotNull { it.getString("slug") }.filter { it.isNotBlank() }.toSet()
                } catch (e: Exception) {
                    Log.w("JSON_IMPORT", "Could not fetch existing slugs", e)
                    emptySet()
                }

                val pending: List<JSONObject> = (0 until totalInFile).mapNotNull { i ->
                    val o = jsonArray.getJSONObject(i)
                    val titleVal = o.optString("title", "").trim()
                    if (titleVal.isBlank()) return@mapNotNull null
                    val slug = o.optString("slug", "").trim()
                    if (slug.isNotBlank() && existingSlugs.contains(slug)) return@mapNotNull null
                    o
                }

                Log.d("JSON_IMPORT", "${pending.size} pending (${existingSlugs.size} already in DB)")
                mainHandler.post { pendingCount = pending.size }

                if (pending.isEmpty()) {
                    mainHandler.post {
                        isImportingJson = false
                        jsonImportProgress = ""
                        statusMessage = "✅ All $totalInFile movies already imported. Nothing new to add."
                        pendingCount = 0
                        lastJsonUri = null
                        prefs.edit().remove("last_json_uri").putInt("pending_count", 0).apply()
                    }
                    return@launch
                }

                val batchToAdd = pending.take(100)
                mainHandler.post { jsonImportProgress = "Building batch of ${batchToAdd.size} movies (${pending.size} pending)..." }

                fun mapGenre(raw: String): String {
                    if (raw.isBlank()) return "Action"
                    val g = raw.lowercase()
                    return when {
                        g.contains("sci") || g.contains("fiction") -> "Sci-Fi"
                        g.contains("roman") -> "Romantic"
                        g.contains("comedy") -> "Comedy"
                        g.contains("horror") -> "Horror"
                        g.contains("animat") -> "Animation"
                        g.contains("adventure") -> "Adventure"
                        else -> "Action"
                    }
                }
                fun mapVj(raw: String): String {
                    if (raw.isBlank()) return "VJ HD"
                    val v = raw.trim()
                    return when (v.lowercase()) {
                        "vj ice p", "ice p" -> "VJ Omutaka Ice P"
                        "vj mk kisuule", "mk kisuule" -> "VJ MK"
                        else -> vjList.firstOrNull { it.equals(v, true) } ?: v
                    }
                }
                fun parseUploadDate(s: String): Long {
                    if (s.isBlank()) return System.currentTimeMillis()
                    for (f in listOf(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        "yyyy-MM-dd'T'HH:mm:ssXXX",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'"
                    )) {
                        try { return SimpleDateFormat(f, Locale.US).parse(s)?.time ?: 0L } catch (_: Exception) {}
                    }
                    return System.currentTimeMillis()
                }

                val docs = mutableListOf<Map<String, Any>>()
                for (o in batchToAdd) {
                    try {
                        val titleVal = o.optString("title", "").trim()

                        var poster = o.optString("image", "").trim()
                        if (poster.isBlank()) poster = o.optString("thumbnail", "").trim()
                        if (poster.isBlank()) poster = o.optString("poster", "").trim()
                        if (poster.isBlank()) poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=400&q=80"

                        var videoUrlVal = o.optString("videoUrl", "").trim()
                        if (videoUrlVal.isBlank()) videoUrlVal = o.optString("video_url", "").trim()
                        if (videoUrlVal.isBlank()) videoUrlVal = o.optString("streamUrl", "").trim()

                        val rawType = o.optString("type", "movie").lowercase()
                        val normalizedType = if (rawType == "series" || rawType == "tv" || rawType == "tv show") "TV Show" else "Movie"

                        val genreArr = o.optJSONArray("genres") ?: o.optJSONArray("genre")
                        val firstGenre = if (genreArr != null && genreArr.length() > 0) genreArr.optString(0) else o.optString("genre", "")
                        val category = mapGenre(firstGenre)

                        val rawVj = o.optString("vj", "").ifBlank { o.optString("videoTag", "") }
                        val vj = mapVj(rawVj)

                        val yearVal = o.optString("year", "2024")
                        val yearStr = if (yearVal.length >= 4) yearVal.take(4) else "2024"

                        val movieMap = hashMapOf<String, Any>(
                            "title" to titleVal,
                            "poster" to poster,
                            "videoUrl" to videoUrlVal,
                            "category" to category,
                            "videoTag" to vj,
                            "year" to yearStr,
                            "type" to normalizedType,
                            "isVip" to o.optBoolean("isVip", false),
                            "isFeatured" to o.optBoolean("isFeatured", false),
                            "isAdult" to false,
                            "subtitleUrl" to "",
                            "timestamp" to parseUploadDate(o.optString("uploadDate", ""))
                        )
                        val slug = o.optString("slug", "").trim()
                        if (slug.isNotBlank()) movieMap["slug"] = slug
                        val desc = o.optString("description", "").trim()
                        if (desc.isNotBlank()) movieMap["description"] = desc
                        if (o.has("id") && !o.isNull("id")) movieMap["externalId"] = o.optInt("id", 0)

                        docs.add(movieMap)
                    } catch (e: Exception) {
                        Log.e("JSON_IMPORT", "Bad item", e)
                    }
                }

                mainHandler.post { jsonImportProgress = "Writing ${docs.size} movies..." }
                var written = 0
                var firstErr = ""
                try {
                    val batch = db.batch()
                    for (doc in docs) {
                        batch.set(db.collection("homeland_movies").document(), doc)
                    }
                    Tasks.await(batch.commit(), 30, TimeUnit.SECONDS)
                    written = docs.size
                } catch (e: Exception) {
                    Log.e("JSON_IMPORT", "Batch commit failed", e)
                    firstErr = e.message?.take(100) ?: "Batch error"
                }

                val remaining = pending.size - written
                mainHandler.post {
                    isImportingJson = false
                    jsonImportProgress = "Done. Added: $written | Remaining: $remaining"
                    pendingCount = remaining
                    prefs.edit().putInt("pending_count", remaining).apply()

                    statusMessage = when {
                        written > 0 && remaining > 0 ->
                            "✅ Added $written movies. $remaining still pending — tap Continue."
                        written > 0 && remaining == 0 -> {
                            lastJsonUri = null
                            prefs.edit().remove("last_json_uri").putInt("pending_count", 0).apply()
                            "🎉 All done! Last $written movies added. Library complete."
                        }
                        else -> "Nothing added. $firstErr"
                    }
                }

            } catch (e: Exception) {
                Log.e("JSON_IMPORT", "Import failed", e)
                mainHandler.post {
                    isImportingJson = false
                    jsonImportProgress = ""
                    statusMessage = "JSON Error: ${e.message}"
                }
            }
        }
    }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            prefs.edit().putString("last_json_uri", uri.toString()).putInt("pending_count", 0).apply()
            lastJsonUri = uri.toString()
            pendingCount = 0
            runBatchImport(uri)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🔥 TV CHANNELS — Batch Upsert (100 per tap)
    // ═══════════════════════════════════════════════════════════════════
    val runBatchTvImport: (android.net.Uri, Boolean) -> Unit = { uri, resetPosition ->
        isImportingTvJson = true
        tvJsonProgress = if (resetPosition) "Reading file (fresh sync)..." else "Reading file (continuing)..."
        statusMessage = null

        AppSyncScope.launch {
            try {
                if (resetPosition) {
                    prefs.edit().putInt("tv_json_position", 0).apply()
                }
                val currentPosition = prefs.getInt("tv_json_position", 0)

                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (jsonString.isNullOrBlank()) throw Exception("File is empty.")

                val trimmed = jsonString.trim().removePrefix("\uFEFF").trim()
                val jsonArray: JSONArray = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val root = JSONObject(trimmed)
                    var found: JSONArray? = null
                    for (key in listOf("channels", "tv", "streams", "movies", "data", "items", "results", "list", "content")) {
                        val arr = root.optJSONArray(key)
                        if (arr != null) { found = arr; break }
                    }
                    if (found == null) {
                        val dataObj = root.optJSONObject("data")
                        if (dataObj != null) {
                            for (key in listOf("channels", "tv", "streams", "items", "results")) {
                                val arr = dataObj.optJSONArray(key)
                                if (arr != null) { found = arr; break }
                            }
                        }
                    }
                    found ?: JSONArray().apply { put(root) }
                }

                val total = jsonArray.length()
                Log.d("TV_IMPORT", "File has $total items, starting at position $currentPosition")

                if (currentPosition >= total) {
                    mainHandler.post {
                        isImportingTvJson = false
                        tvJsonProgress = "All $total channels already synced."
                        statusMessage = "✅ All $total channels synced. Pick the file again to re-sync."
                        pendingTvCount = 0
                        lastTvJsonUri = null
                        prefs.edit().remove("last_tv_json_uri")
                            .putInt("pending_tv_count", 0)
                            .putInt("tv_json_position", 0)
                            .apply()
                    }
                    return@launch
                }

                val end = minOf(currentPosition + 100, total)
                val sliceSize = end - currentPosition
                mainHandler.post { tvJsonProgress = "Preparing $sliceSize channels ($currentPosition/$total)..." }

                val existingSnap = Tasks.await(db.collection("homeland_tv").get(), 20, TimeUnit.SECONDS)
                val existingByName = mutableMapOf<String, String>()
                val existingBySlug = mutableMapOf<String, String>()
                for (doc in existingSnap.documents) {
                    val n = doc.getString("name")?.trim()?.lowercase() ?: ""
                    if (n.isNotBlank()) existingByName[n] = doc.id
                    val s = doc.getString("slug")?.trim()
                    if (!s.isNullOrBlank()) existingBySlug[s] = doc.id
                }
                Log.d("TV_IMPORT", "Existing: ${existingSnap.size()} channels in DB")

                val wb = db.batch()
                var added = 0
                var updated = 0
                var skipped = 0
                val touchedInBatch = mutableSetOf<String>()

                for (i in currentPosition until end) {
                    try {
                        val o = jsonArray.getJSONObject(i)

                        val nameVal = o.optString("name", "").trim()
                            .ifBlank { o.optString("channelName", "").trim() }
                            .ifBlank { o.optString("channel_name", "").trim() }
                            .ifBlank { o.optString("title", "").trim() }
                        if (nameVal.isBlank()) { skipped++; continue }

                        val streamVal = o.optString("streamUrl", "").trim()
                            .ifBlank { o.optString("stream_url", "").trim() }
                            .ifBlank { o.optString("url", "").trim() }
                            .ifBlank { o.optString("link", "").trim() }
                            .ifBlank { o.optString("src", "").trim() }
                        if (streamVal.isBlank()) { skipped++; continue }

                        val logoVal = o.optString("img", "").trim()
    .ifBlank { o.optString("logo", "").trim() }
    .ifBlank { o.optString("logoUrl", "").trim() }
    .ifBlank { o.optString("logo_url", "").trim() }
    .ifBlank { o.optString("tvgLogo", "").trim() }
    .ifBlank { o.optString("tvg_logo", "").trim() }
    .ifBlank { o.optString("icon", "").trim() }

                        val catVal = o.optString("category", "").trim()
                            .ifBlank { o.optString("group", "").trim() }
                            .ifBlank { o.optString("groupTitle", "").trim() }
                            .ifBlank { o.optString("group_title", "").trim() }
                            .ifBlank { "General" }

                        val slugVal = o.optString("slug", "").trim()
                            .ifBlank { o.optString("tvgId", "").trim() }
                            .ifBlank { o.optString("tvg_id", "").trim() }
                            .ifBlank { o.optString("channelId", "").trim() }
                            .ifBlank { o.optString("channel_id", "").trim() }

                        val doc = hashMapOf<String, Any>(
                            "name" to nameVal,
                            "streamUrl" to streamVal,
                            "logo" to logoVal,
                            "category" to catVal,
                            "timestamp" to System.currentTimeMillis()
                        )
                        if (slugVal.isNotBlank()) doc["slug"] = slugVal
                        if (o.has("isVip")) doc["isVip"] = o.optBoolean("isVip", false)
                        if (o.has("isFeatured")) doc["isFeatured"] = o.optBoolean("isFeatured", false)
                        if (o.has("isAdult")) doc["isAdult"] = o.optBoolean("isAdult", false)

                        var existingId: String? = null
                        if (slugVal.isNotBlank()) existingId = existingBySlug[slugVal]
                        if (existingId == null) existingId = existingByName[nameVal.lowercase()]

                        if (existingId != null) {
                            if (!touchedInBatch.contains(existingId)) {
                                touchedInBatch.add(existingId)
                                wb.update(db.collection("homeland_tv").document(existingId), doc)
                                updated++
                            } else {
                                skipped++
                            }
                        } else {
                            wb.set(db.collection("homeland_tv").document(), doc)
                            added++
                        }
                    } catch (e: Exception) {
                        Log.e("TV_IMPORT", "Item $i failed", e)
                        skipped++
                    }
                }

                mainHandler.post { tvJsonProgress = "Writing $added new + $updated updates..." }
                Tasks.await(wb.commit(), 30, TimeUnit.SECONDS)

                val newPosition = end
                val remaining = total - newPosition

                mainHandler.post {
                    isImportingTvJson = false
                    tvJsonProgress = "Synced $newPosition/$total | +$added new, ~$updated updated"
                    pendingTvCount = remaining
                    prefs.edit()
                        .putInt("tv_json_position", newPosition)
                        .putInt("pending_tv_count", remaining)
                        .apply()

                    statusMessage = when {
                        remaining > 0 -> "✅ Added $added, updated $updated. $remaining remaining — tap Continue."
                        added > 0 || updated > 0 -> {
                            lastTvJsonUri = null
                            prefs.edit().remove("last_tv_json_uri")
                                .putInt("pending_tv_count", 0)
                                .putInt("tv_json_position", 0)
                                .apply()
                            "🎉 Sync complete! +$added new, ~$updated updated."
                        }
                        else -> "No changes. All $total channels already up to date."
                    }
                }

            } catch (e: Exception) {
                Log.e("TV_IMPORT", "Batch failed", e)
                mainHandler.post {
                    isImportingTvJson = false
                    tvJsonProgress = ""
                    statusMessage = "TV JSON Error: ${e.message}"
                }
            }
        }
    }

    val tvJsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            prefs.edit()
                .putString("last_tv_json_uri", uri.toString())
                .putInt("tv_json_position", 0)
                .putInt("pending_tv_count", 0)
                .apply()
            lastTvJsonUri = uri.toString()
            pendingTvCount = 0
            runBatchTvImport(uri, true)
        }
    }

    // 🔥 PERFORMANCE FIX: DisposableEffect cleans up listeners on tab switch (no leaks)
    DisposableEffect(selectedTab) {
        val regs = mutableListOf<ListenerRegistration>()

        when (selectedTab) {
            0, 4 -> {
                regs.add(
                    db.collection("homeland_movies").addSnapshotListener { snap, _ ->
                        if (snap != null) {
                            existingMovies = snap.documents.map { doc ->
                                Movie(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    poster = doc.getString("poster") ?: "",
                                    videoUrl = doc.getString("videoUrl") ?: "",
                                    category = doc.getString("category") ?: "",
                                    videoTag = doc.getString("videoTag") ?: "",
                                    year = doc.getString("year") ?: "",
                                    isVip = doc.getBoolean("isVip") ?: false,
                                    isAdult = doc.getBoolean("isAdult") ?: false
                                )
                            }
                        }
                    }
                )
            }
            1 -> {
                regs.add(
                    db.collection("homeland_tv").addSnapshotListener { snap, _ ->
                        if (snap != null) {
                            existingTv = snap.documents.map { doc ->
                                TvChannel(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "",
                                    logo = doc.getString("logo") ?: "",
                                    streamUrl = doc.getString("streamUrl") ?: "",
                                    category = doc.getString("category") ?: "",
                                    isVip = doc.getBoolean("isVip") ?: false,
                                    isFeatured = doc.getBoolean("isFeatured") ?: false
                                )
                            }
                        }
                    }
                )
            }
            2 -> {
                regs.add(
                    db.collection("users").addSnapshotListener { snap, _ ->
                        if (snap != null) {
                            allUsers = snap.documents.map { doc ->
                                AdminUser(
                                    uid = doc.id,
                                    email = doc.getString("email") ?: "Unknown",
                                    homelandVipExpiry = doc.getLong("homelandVipExpiry") ?: 0L,
                                    isBanned = doc.getBoolean("isBanned") ?: false
                                )
                            }
                        }
                    }
                )
                regs.add(
                    db.collection("homeland_vip_plans").addSnapshotListener { snap, _ ->
                        if (snap != null) {
                            vipPlans = snap.documents.map { doc ->
                                VipPlan(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "",
                                    price = doc.getString("price") ?: "",
                                    days = doc.getLong("days")?.toInt() ?: 0
                                )
                            }
                        }
                    }
                )
            }
            3 -> {
                regs.add(
                    db.collection("config").document("maintenance").addSnapshotListener { doc, _ ->
                        if (doc != null && doc.exists()) {
                            isMaintenanceActive = doc.getBoolean("active") ?: false
                            maintenanceMsg = doc.getString("message") ?: ""
                        }
                    }
                )
            }
            5 -> {
                regs.add(
                    db.collection("config").document("social_links").addSnapshotListener { doc, _ ->
                        if (doc != null && doc.exists()) {
                            whatsappLink = doc.getString("whatsapp") ?: ""
                            telegramLink = doc.getString("telegram") ?: ""
                            tiktokLink = doc.getString("tiktok") ?: ""
                            twitterLink = doc.getString("twitter") ?: ""
                            instagramLink = doc.getString("instagram") ?: ""
                        }
                    }
                )
            }
            6 -> {
                regs.add(
                    db.collection("payment_requests")
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { snap, _ ->
                            if (snap != null) {
                                paymentRequests = snap.documents.map { doc ->
                                    PaymentRequest(
                                        id = doc.id,
                                        email = doc.getString("email") ?: "Unknown",
                                        tid = doc.getString("tid") ?: "",
                                        planName = doc.getString("planName") ?: "Unknown Plan",
                                        price = doc.getString("price") ?: "0",
                                        days = doc.getLong("days")?.toInt() ?: 0,
                                        timestamp = doc.getLong("timestamp") ?: 0L
                                    )
                                }.sortedByDescending { it.timestamp }
                            }
                        }
                )
            }
        }

        onDispose {
            regs.forEach { it.remove() }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0C10),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                    Text("ADMIN PANEL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFE50914),
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Movies") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("TV Live") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("VIP") })
                    Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text("Payments") })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Theme") })
                    Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Push") })
                    Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Socials") })
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (statusMessage != null) {
                item {
                    Text(
                        text = statusMessage!!,
                        color = if (statusMessage!!.contains("Success") || statusMessage!!.contains("Approved") || statusMessage!!.contains("Complete") || statusMessage!!.contains("✅") || statusMessage!!.contains("🎉") || statusMessage!!.contains("Added")) Color.Green else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedTab == 0) {

                // 🔥 MOVIES JSON IMPORT CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Upload", tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bulk Import from JSON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text(
                                "Adds 100 movies per tap. Re-tapping is safe — no duplicates, always continues where you left off.",
                                color = Color.Gray, fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
                            )

                            Button(
                                onClick = { jsonPickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isImportingJson
                            ) {
                                if (isImportingJson) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Select JSON File", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (lastJsonUri != null && pendingCount > 0 && !isImportingJson) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { runBatchImport(android.net.Uri.parse(lastJsonUri!!)) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCDC04)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("▶ Continue Import ($pendingCount remaining)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (isImportingJson || jsonImportProgress.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(jsonImportProgress, color = Color(0xFFFCDC04), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Sync", tint = Color(0xFFE50914))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("YOCINEMA Auto-Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Import latest movies automatically using the API.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))

                            OutlinedTextField(
                                value = yocApiKey,
                                onValueChange = { yocApiKey = it },
                                label = { Text("YOCINEMA API Key (YOCD_...)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f))
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val key = yocApiKey.trim()
                                        if (!key.startsWith("YOCD_")) {
                                            statusMessage = "Error: Key must start with YOCD_"
                                            return@Button
                                        }
                                        prefs.edit().putString("yoc_api_key", key).apply()
                                        statusMessage = "Success! API Key saved on device."
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Save Key", color = Color.White) }

                                Button(
                                    onClick = {
                                        if (yocApiKey.isBlank() || !yocApiKey.startsWith("YOCD_")) {
                                            statusMessage = "Error: Please enter a valid API Key first."
                                            return@Button
                                        }
                                        globalIsSyncing = true
                                        globalSyncProgressMsg = "Starting sync..."

                                        AppSyncScope.launch {
                                            try {
                                                val result = executeYocSync(context, yocApiKey, db) { msg ->
                                                    mainHandler.post { globalSyncProgressMsg = msg }
                                                }
                                                mainHandler.post {
                                                    globalSyncProgressMsg = if (result.first > 0)
                                                        "Success! Added ${result.first} movies. Failed: ${result.second}."
                                                    else
                                                        "No new movies found. Already up to date!"
                                                    globalIsSyncing = false
                                                    lastSyncTime = System.currentTimeMillis()
                                                    prefs.edit().putLong("yoc_last_sync", lastSyncTime).apply()
                                                }
                                            } catch (e: Exception) {
                                                Log.e("YOC_SYNC", "Sync failed", e)
                                                mainHandler.post {
                                                    globalSyncProgressMsg = "Sync Error: ${e.message ?: "Network failure"}"
                                                    globalIsSyncing = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !globalIsSyncing
                                ) {
                                    if (globalIsSyncing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    else Text("Sync Now", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (globalIsSyncing) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(globalSyncProgressMsg, color = Color(0xFFFCDC04), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else if (globalSyncProgressMsg.isNotEmpty() && globalSyncProgressMsg.contains("Error", true)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(globalSyncProgressMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else if (lastSyncTime > 0) {
                                val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Last synced: ${sdf.format(Date(lastSyncTime))}", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(if (editingMovieId != null) "EDITING MOVIE" else "ADD NEW MOVIE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { selectedType = "Movie" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "Movie") Color(0xFFE50914) else Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Movie") }
                                Button(
                                    onClick = { selectedType = "TV Show" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "TV Show") Color(0xFFE50914) else Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("TV Show") }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))

                            if (posterUrl.isNotBlank()) {
                                PosterImage(
                                    posterData = posterUrl,
                                    contentDescription = "Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            OutlinedTextField(
                                value = posterUrl,
                                onValueChange = { posterUrl = it },
                                label = { Text("Poster URL or Base64") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFE50914),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Subtitle URL (.vtt) - Optional") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))

                            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                                OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }, modifier = Modifier.background(Color(0xFF15171E))) {
                                    categories.forEach { cat -> DropdownMenuItem(text = { Text(cat, color = Color.White) }, onClick = { selectedCategory = cat; catExpanded = false }) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            ExposedDropdownMenuBox(expanded = vjExpanded, onExpandedChange = { vjExpanded = !vjExpanded }) {
                                OutlinedTextField(value = selectedVj, onValueChange = {}, readOnly = true, label = { Text("VJ Tag") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vjExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                                ExposedDropdownMenu(expanded = vjExpanded, onDismissRequest = { vjExpanded = false }, modifier = Modifier.heightIn(max = 200.dp).background(Color(0xFF15171E))) {
                                    vjList.forEach { vj -> DropdownMenuItem(text = { Text(vj, color = Color.White) }, onClick = { selectedVj = vj; vjExpanded = false }) }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (selectedType == "TV Show") {
                                Text("Add Episodes", color = Color.White, fontWeight = FontWeight.Bold)
                                var epSeason by remember { mutableStateOf("1") }
                                var epNumber by remember { mutableStateOf("1") }
                                var epUrl by remember { mutableStateOf("") }
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = epSeason, onValueChange = { epSeason = it }, label = { Text("S") }, modifier = Modifier.width(60.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedTextField(value = epNumber, onValueChange = { epNumber = it }, label = { Text("Ep") }, modifier = Modifier.width(60.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedTextField(value = epUrl, onValueChange = { epUrl = it }, label = { Text("Video URL") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        if (epUrl.isNotBlank()) {
                                            episodesList = episodesList + Episode("S$epSeason", "Ep $epNumber", epUrl)
                                            epUrl = ""
                                        }
                                    }, shape = RoundedCornerShape(12.dp)) { Text("Add") }
                                }
                                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                    items(episodesList) { ep ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("${ep.season} | ${ep.episode}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(onClick = { episodesList = episodesList - ep }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                                            }
                                        }
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    }
                                }
                            } else {
                                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Movie Stream URL (.mp4/.m3u8)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("VIP Only", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isVipMovie, onCheckedChange = { isVipMovie = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE50914)))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Featured Banner", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isFeaturedMovie, onCheckedChange = { isFeaturedMovie = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE50914)))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("18+ Restricted (Requires PIN)", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isAdultMovie, onCheckedChange = { isAdultMovie = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red))
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (title.isBlank() || posterUrl.isBlank() || (videoUrl.isBlank() && episodesList.isEmpty())) { statusMessage = "Fill required fields."; return@Button }
                                    isLoading = true
                                    val newMovie = hashMapOf(
                                        "title" to title, "poster" to posterUrl, "videoUrl" to videoUrl, "category" to selectedCategory,
                                        "videoTag" to selectedVj, "year" to year, "type" to selectedType, "isVip" to isVipMovie,
                                        "isFeatured" to isFeaturedMovie, "isAdult" to isAdultMovie, "subtitleUrl" to subtitleUrl, "timestamp" to System.currentTimeMillis(),
                                        "episodes" to episodesList
                                    )

                                    if (editingMovieId != null) {
                                        db.collection("homeland_movies").document(editingMovieId!!).set(newMovie).addOnSuccessListener {
                                            isLoading = false; statusMessage = "Success! Updated."; editingMovieId = null; title = ""; posterUrl = ""; videoUrl = ""; episodesList = emptyList(); isAdultMovie = false
                                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                                    } else {
                                        db.collection("homeland_movies").add(newMovie).addOnSuccessListener {
                                            isLoading = false; statusMessage = "Success! Added."; title = ""; posterUrl = ""; videoUrl = ""; episodesList = emptyList(); isAdultMovie = false
                                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (editingMovieId != null) "Update Content" else "Publish Content", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                            if (editingMovieId != null) {
                                TextButton(onClick = { editingMovieId = null; title = ""; posterUrl = ""; videoUrl = ""; episodesList = emptyList(); isAdultMovie = false }) { Text("Cancel Edit", color = Color.Gray) }
                            }
                        }
                    }
                }

                // 🔥 LAZY Manage Content — one item per movie (only visible rows compose)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Manage Content (${existingMovies.size} movies)",
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Scroll to browse. Only visible rows are rendered.",
                                color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                items(existingMovies) { movie ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PosterImage(
                                posterData = movie.poster,
                                contentDescription = movie.title,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(movie.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                Text(
                                    if (movie.poster.startsWith("data:")) "Poster: Base64 ✓"
                                    else if (movie.poster.startsWith("http")) "Poster: URL ✓"
                                    else "⚠ No poster",
                                    color = if (movie.poster.isNotBlank()) Color(0xFF10B981) else Color.Red,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(onClick = {
                                editingMovieId = movie.id; title = movie.title; posterUrl = movie.poster
                                videoUrl = movie.videoUrl; selectedCategory = movie.category; selectedVj = movie.videoTag
                                year = movie.year; isVipMovie = movie.isVip; episodesList = movie.episodes; isAdultMovie = movie.isAdult
                            }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Yellow) }
                            IconButton(onClick = { db.collection("homeland_movies").document(movie.id).delete() }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {

                // 🔥 TV BULK SYNC CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Upload", tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bulk Sync TV Channels from JSON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text(
                                "Adds 100 channels per tap. If a channel already exists (by name or slug), it UPDATES its stream URL instead of duplicating.",
                                color = Color.Gray, fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
                            )

                            Button(
                                onClick = { tvJsonPickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isImportingTvJson
                            ) {
                                if (isImportingTvJson) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Select TV JSON File", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (lastTvJsonUri != null && pendingTvCount > 0 && !isImportingTvJson) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { runBatchTvImport(android.net.Uri.parse(lastTvJsonUri!!), false) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCDC04)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("▶ Continue TV Sync ($pendingTvCount remaining)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (isImportingTvJson || tvJsonProgress.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(tvJsonProgress, color = Color(0xFFFCDC04), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(if (editingTvId != null) "EDITING TV" else "ADD TV LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(value = tvName, onValueChange = { tvName = it }, label = { Text("Channel Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = tvLogo, onValueChange = { tvLogo = it }, label = { Text("Logo URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = tvUrl, onValueChange = { tvUrl = it }, label = { Text("Stream URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = tvCategory, onValueChange = { tvCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Require VIP", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isVipTv, onCheckedChange = { isVipTv = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE50914)))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Featured Channel", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isFeaturedTv, onCheckedChange = { isFeaturedTv = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE50914)))
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (tvName.isBlank() || tvUrl.isBlank()) { statusMessage = "Fill required fields."; return@Button }
                                    isLoading = true
                                    val newTv = hashMapOf("name" to tvName, "logo" to tvLogo, "streamUrl" to tvUrl, "category" to tvCategory, "isVip" to isVipTv, "isFeatured" to isFeaturedTv)

                                    if (editingTvId != null) {
                                        db.collection("homeland_tv").document(editingTvId!!).set(newTv).addOnSuccessListener {
                                            isLoading = false; statusMessage = "Success! TV updated."; editingTvId = null; tvName = ""; tvLogo = ""; tvUrl = ""; isVipTv = false; isFeaturedTv = false
                                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                                    } else {
                                        db.collection("homeland_tv").add(newTv).addOnSuccessListener {
                                            isLoading = false; statusMessage = "Success! TV added."; tvName = ""; tvLogo = ""; tvUrl = ""; isVipTv = false; isFeaturedTv = false
                                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (editingTvId != null) "Update TV Channel" else "Publish TV Channel", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                            if (editingTvId != null) {
                                TextButton(onClick = { editingTvId = null; tvName = ""; tvLogo = ""; tvUrl = ""; isFeaturedTv = false }) { Text("Cancel Edit", color = Color.Gray) }
                            }
                        }
                    }
                }

                // 🔥 LAZY Manage TV Channels — one item per channel
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Manage TV Channels (${existingTv.size} channels)",
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                items(existingTv) { tv ->
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔥 NEW: Display the TV Logo
            PosterImage(
                posterData = tv.logo,
                contentDescription = tv.name,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(tv.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
                Text(
                    if (tv.logo.startsWith("http")) "Logo: URL ✓" else "⚠ No logo",
                    color = if (tv.logo.isNotBlank()) Color(0xFF10B981) else Color.Red,
                    fontSize = 10.sp
                )
            }
            
            IconButton(onClick = {
                editingTvId = tv.id; tvName = tv.name; tvLogo = tv.logo; tvUrl = tv.streamUrl
                tvCategory = tv.category; isVipTv = tv.isVip; isFeaturedTv = tv.isFeatured
            }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Yellow) }
            
            IconButton(onClick = { db.collection("homeland_tv").document(tv.id).delete() }) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
            }
        }
    }
                }
            } else if (selectedTab == 2) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Manage VIP Plans", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("These plans appear to users in the app.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(value = newPlanName, onValueChange = { newPlanName = it }, label = { Text("Plan Name (e.g. 1 Month)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))

                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = newPlanPrice, onValueChange = { newPlanPrice = it }, label = { Text("Price (e.g. 10,000 UGX)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                                OutlinedTextField(value = newPlanDays, onValueChange = { newPlanDays = it }, label = { Text("Days (e.g. 30)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            }

                            Button(onClick = {
                                if (newPlanName.isNotBlank() && newPlanPrice.isNotBlank() && newPlanDays.isNotBlank()) {
                                    val daysInt = newPlanDays.toIntOrNull() ?: 0
                                    db.collection("homeland_vip_plans").add(hashMapOf(
                                        "name" to newPlanName,
                                        "price" to newPlanPrice,
                                        "days" to daysInt
                                    )).addOnSuccessListener {
                                        statusMessage = "Plan added successfully!"
                                        newPlanName = ""; newPlanPrice = ""; newPlanDays = ""
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth().height(50.dp).shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(12.dp)) {
                                Text("Add Plan", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (vipPlans.isEmpty()) {
                                Text("No plans created yet.", color = Color.Gray, fontSize = 14.sp)
                            } else {
                                vipPlans.forEach { plan ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(plan.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${plan.price} • ${plan.days} Days", color = Color(0xFFFCDC04), fontSize = 12.sp)
                                        }
                                        IconButton(onClick = { db.collection("homeland_vip_plans").document(plan.id).delete() }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
                item {
                    val now = System.currentTimeMillis()
                    val threeDaysFromNow = now + (3 * 24 * 60 * 60 * 1000L)

                    val expiringSoonUsers = allUsers.filter { it.homelandVipExpiry > now && it.homelandVipExpiry <= threeDaysFromNow }.sortedBy { it.homelandVipExpiry }

                    if (expiringSoonUsers.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B1B).copy(alpha = 0.8f)),
                            border = BorderStroke(1.dp, Color.Red),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("VIP Expiring Soon", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text("These users expire in the next 3 days. Message them to renew!", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))

                                expiringSoonUsers.forEach { user ->
                                    val timeLeft = user.homelandVipExpiry - now
                                    val daysLeft = timeLeft / (1000 * 60 * 60 * 24)
                                    val hoursLeft = (timeLeft % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
                                    val timeText = if (daysLeft > 0) "${daysLeft}d ${hoursLeft}h left" else "${hoursLeft}h left"

                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.email, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Text(timeText, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Generate VIP Coupon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = couponDays, onValueChange = { couponDays = it }, label = { Text("Days") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        val days = couponDays.toIntOrNull() ?: 7
                                        val code = "HOMELAND-${days}D-${Random.nextInt(1000, 9999)}"
                                        db.collection("homeland_coupons").add(hashMapOf("code" to code, "days" to days, "isUsed" to false))
                                            .addOnSuccessListener { statusMessage = "Success! Code: $code" }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                    modifier = Modifier.height(56.dp).shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Generate", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Manage Users", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                            OutlinedTextField(
                                value = customVipDays,
                                onValueChange = { customVipDays = it },
                                label = { Text("Custom Days (e.g. 30, 90)") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f))
                            )

                            allUsers.forEach { user ->
                                val isVip = user.homelandVipExpiry > System.currentTimeMillis()

                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.email, color = if (user.isBanned) Color.Red else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = when {
                                                    user.isBanned -> "BANNED"
                                                    isVip -> "VIP Active"
                                                    else -> "Free User"
                                                },
                                                color = when {
                                                    user.isBanned -> Color.Red
                                                    isVip -> Color(0xFFFCDC04)
                                                    else -> Color.Gray
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(onClick = {
                                            db.collection("users").document(user.uid).update("isBanned", !user.isBanned)
                                                .addOnSuccessListener {
                                                    statusMessage = if (!user.isBanned) "User Banned" else "User Unbanned"
                                                }
                                        }) {
                                            Icon(
                                                imageVector = if (user.isBanned) Icons.Default.CheckCircle else Icons.Default.Block,
                                                contentDescription = "Ban Status",
                                                tint = if (user.isBanned) Color.Green else Color.Red
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                        Button(onClick = {
                                            val base = if (isVip) user.homelandVipExpiry else System.currentTimeMillis()
                                            db.collection("users").document(user.uid).update("homelandVipExpiry", base + (7 * 24 * 60 * 60 * 1000L))
                                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)), modifier = Modifier.padding(end = 8.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                                            Text("+7d", fontSize = 11.sp, color = Color.White)
                                        }

                                        Button(onClick = {
                                            val daysToAdd = customVipDays.toIntOrNull() ?: 0
                                            if (daysToAdd > 0) {
                                                val base = if (isVip) user.homelandVipExpiry else System.currentTimeMillis()
                                                db.collection("users").document(user.uid).update("homelandVipExpiry", base + (daysToAdd.toLong() * 24 * 60 * 60 * 1000L))
                                                    .addOnSuccessListener { statusMessage = "Added $daysToAdd days for ${user.email}" }
                                            }
                                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                                            Text("+${customVipDays}d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            } else if (selectedTab == 6) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Pending Payments", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Approve to grant VIP based on the selected plan.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (paymentRequests.isEmpty()) {
                                Text("No pending payments.", color = Color.Gray, fontSize = 14.sp)
                            } else {
                                paymentRequests.forEach { req ->
                                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(req.timestamp))

                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(req.email, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Text(dateStr, color = Color.Gray, fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("TID: ", color = Color.Gray, fontSize = 12.sp)
                                            Text(req.tid, color = Color(0xFFFCDC04), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Plan: ", color = Color.Gray, fontSize = 12.sp)
                                            Text("${req.planName} (${req.price}) - ${req.days} Days", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                onClick = {
                                                    db.collection("payment_requests").document(req.id).update("status", "rejected")
                                                        .addOnSuccessListener { statusMessage = "Payment rejected." }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp)
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = "Reject", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject", fontSize = 12.sp, color = Color.Red)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    val msToAdd = req.days.toLong() * 24 * 60 * 60 * 1000L

                                                    db.collection("users").whereEqualTo("email", req.email).get().addOnSuccessListener { userSnap ->
                                                        if (!userSnap.isEmpty) {
                                                            val userDoc = userSnap.documents[0]
                                                            val currentExpiry = userDoc.getLong("homelandVipExpiry") ?: 0L
                                                            val newExpiry = if (currentExpiry > System.currentTimeMillis()) currentExpiry + msToAdd else System.currentTimeMillis() + msToAdd

                                                            userDoc.reference.update("homelandVipExpiry", newExpiry)

                                                            db.collection("payment_requests").document(req.id).update("status", "approved")
                                                                .addOnSuccessListener { statusMessage = "Approved! ${req.days} days granted to ${req.email}" }
                                                        } else {
                                                            statusMessage = "Error: User email not found in database."
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Approve", tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 3) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("App Theme Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(value = themeFontUrl, onValueChange = { themeFontUrl = it }, label = { Text("Custom Font URL (.ttf or .otf)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = themeBgImageUrl, onValueChange = { themeBgImageUrl = it }, label = { Text("Custom Background Image URL (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = {
                                val themeMap = hashMapOf("fontUrl" to themeFontUrl, "bgImageUrl" to themeBgImageUrl)
                                db.collection("config").document("theme").set(themeMap, com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener { statusMessage = "Theme updated globally!" }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(12.dp)) { Text("Apply Theme to All Devices", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("App Maintenance Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Enable Maintenance Mode", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isMaintenanceActive, onCheckedChange = { isMaintenanceActive = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = maintenanceMsg, onValueChange = { maintenanceMsg = it }, label = { Text("Maintenance Message") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = {
                                db.collection("config").document("maintenance").set(hashMapOf("active" to isMaintenanceActive, "message" to maintenanceMsg))
                                    .addOnSuccessListener { statusMessage = "Maintenance settings updated!" }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(12.dp)) { Text("Save Maintenance Settings", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Admin Security (2FA)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Change your 6-digit PIN used to open this panel.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newAdminPin,
                                onValueChange = { if (it.length <= 6) newAdminPin = it },
                                label = { Text("New 6-digit PIN") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f))
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(onClick = {
                                if (newAdminPin.length == 6) {
                                    db.collection("config").document("admin").set(hashMapOf("adminPin" to newAdminPin), com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener {
                                            statusMessage = "Success! Admin PIN updated."
                                            newAdminPin = ""
                                        }
                                        .addOnFailureListener {
                                            statusMessage = "Error: ${it.message}"
                                        }
                                } else {
                                    statusMessage = "PIN must be exactly 6 digits."
                                }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(12.dp)) {
                                Text("Save New Admin PIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (selectedTab == 4) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Send Push Notification", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(value = pushTitle, onValueChange = { pushTitle = it }, label = { Text("Notification Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = pushMessage, onValueChange = { pushMessage = it }, label = { Text("Notification Message") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))

                            Spacer(modifier = Modifier.height(16.dp))

                            ExposedDropdownMenuBox(
                                expanded = pushMovieExpanded,
                                onExpandedChange = { pushMovieExpanded = !pushMovieExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedPushMovieTitle,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Attach Movie to Notification (Optional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pushMovieExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f))
                                )
                                ExposedDropdownMenu(
                                    expanded = pushMovieExpanded,
                                    onDismissRequest = { pushMovieExpanded = false },
                                    modifier = Modifier.heightIn(max = 200.dp).background(Color(0xFF15171E))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (Open Home Screen)", color = Color.White) },
                                        onClick = {
                                            selectedPushMovieTitle = "None (Open Home Screen)"
                                            pushDeepLink = ""
                                            pushMovieExpanded = false
                                        }
                                    )
                                    existingMovies.forEach { movie ->
                                        DropdownMenuItem(
                                            text = { Text(movie.title, color = Color.White) },
                                            onClick = {
                                                selectedPushMovieTitle = movie.title
                                                pushDeepLink = "homeland://movie/${movie.id}"
                                                pushMovieExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(onClick = {
                                if (pushTitle.isBlank() || pushMessage.isBlank()) { statusMessage = "Fill in both fields."; return@Button }
                                isLoading = true
                                val notifData = hashMapOf(
                                    "title" to pushTitle,
                                    "message" to pushMessage,
                                    "deep_link" to pushDeepLink,
                                    "status" to "pending",
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("push_notifications").add(notifData).addOnSuccessListener {
                                    isLoading = false
                                    statusMessage = "Success! Notification queued."
                                    pushTitle = ""
                                    pushMessage = ""
                                    pushDeepLink = ""
                                    selectedPushMovieTitle = "None (Open Home Screen)"
                                }.addOnFailureListener {
                                    isLoading = false
                                    statusMessage = "Error: ${it.message}"
                                }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(12.dp)) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Send to All Users", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } else if (selectedTab == 5) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Social Media Links", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("These links appear on Login & Profile screens.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(value = whatsappLink, onValueChange = { whatsappLink = it }, label = { Text("WhatsApp Link") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            OutlinedTextField(value = telegramLink, onValueChange = { telegramLink = it }, label = { Text("Telegram Link") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            OutlinedTextField(value = tiktokLink, onValueChange = { tiktokLink = it }, label = { Text("TikTok Link") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            OutlinedTextField(value = twitterLink, onValueChange = { twitterLink = it }, label = { Text("Twitter (X) Link") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
                            OutlinedTextField(value = instagramLink, onValueChange = { instagramLink = it }, label = { Text("Instagram Link") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = {
                                val linksMap = hashMapOf(
                                    "whatsapp" to whatsappLink,
                                    "telegram" to telegramLink,
                                    "tiktok" to tiktokLink,
                                    "twitter" to twitterLink,
                                    "instagram" to instagramLink
                                )
                                db.collection("config").document("social_links").set(linksMap)
                                    .addOnSuccessListener { statusMessage = "Success! Social links updated." }
                                    .addOnFailureListener { statusMessage = "Error: ${it.message}" }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(12.dp)) {
                                Text("Save Social Links", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔥 YOCINEMA SYNC NETWORK LOGIC

suspend fun executeYocSync(context: Context, apiKey: String, db: FirebaseFirestore, onProgress: (String) -> Unit): Pair<Int, Int> = withContext(Dispatchers.IO) {

    onProgress("Checking database...")
    val haveIds: Set<String> = try {
        val snapCheck = Tasks.await(db.collection("homeland_movies").get(), 20, TimeUnit.SECONDS)
        snapCheck.documents.mapNotNull { it.getString("yocId") }.toSet()
    } catch (e: Exception) {
        Log.w("YOC_SYNC", "Could not load existing IDs, proceeding anyway", e)
        emptySet()
    }
    onProgress("Database checked. ${haveIds.size} already synced.")

    val all = mutableListOf<JSONObject>()
    var page = 1
    var fetchedCount = 0

    while (fetchedCount < 30 && page <= 15) {
        onProgress("Fetching page $page...")
        try {
            val url = URL("https://muno-yoc-proxy.henryssali219.workers.dev/movies?page=$page&limit=30&sort=latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 20000
            conn.readTimeout = 30000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            conn.setRequestProperty("Origin", "https://munowatch-2c399.web.app")
            conn.setRequestProperty("Referer", "https://munowatch-2c399.web.app/")

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                conn.disconnect()
                throw Exception("Server returned $responseCode: ${err.take(100)}")
            }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            val successField = json.opt("success")
            val isSuccess = when (successField) {
                is Boolean -> successField
                is String -> successField.equals("true", ignoreCase = true)
                else -> false
            }
            if (!isSuccess) throw Exception("API returned failure: ${json.optString("error", "Unknown")}")

            val dataObj = json.optJSONObject("data")
            val moviesArray = dataObj?.optJSONArray("movies") ?: break
            if (moviesArray.length() == 0) break

            for (i in 0 until moviesArray.length()) {
                val m = moviesArray.getJSONObject(i)
                val id = m.optString("_id", "")
                if (id.isNotEmpty() && !haveIds.contains(id)) {
                    all.add(m)
                    fetchedCount++
                    if (fetchedCount >= 30) break
                }
            }
            page++
            if (all.size > 0) onProgress("Found ${all.size} new movies...")
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("Connection timed out. The proxy server did not respond in time.")
        } catch (e: java.net.UnknownHostException) {
            throw Exception("No internet connection. Check your network.")
        } catch (e: Exception) {
            Log.e("YOC_SYNC", "Fetch page $page failed", e)
            if (all.isEmpty()) throw e else break
        }
    }

    if (all.isEmpty()) {
        return@withContext Pair(0, 0)
    }

    var added = 0
    var failed = 0

    for (i in all.indices) {
        val m = all[i]
        val yocId = m.getString("_id")
        val movieTitle = m.optString("title", "Unknown")

        onProgress("Saving ${i + 1}/${all.size}: $movieTitle")

        try {
            val detUrl = URL("https://muno-yoc-proxy.henryssali219.workers.dev/movies/$yocId")
            val detConn = detUrl.openConnection() as HttpURLConnection
            detConn.connectTimeout = 15000
            detConn.readTimeout = 30000
            detConn.setRequestProperty("Accept", "application/json")
            detConn.setRequestProperty("Authorization", "Bearer $apiKey")
            detConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
            val detResponse = detConn.inputStream.bufferedReader().readText()
            detConn.disconnect()
            val detail = JSONObject(detResponse).getJSONObject("data")

            var posterBase64 = ""
            try {
                val coverUrl = URL("https://muno-yoc-proxy.henryssali219.workers.dev/movies/$yocId/cover")
                val coverConn = coverUrl.openConnection() as HttpURLConnection
                coverConn.instanceFollowRedirects = true
                coverConn.connectTimeout = 15000
                coverConn.readTimeout = 30000
                coverConn.requestMethod = "GET"
                coverConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                coverConn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                coverConn.setRequestProperty("Origin", "https://munowatch-2c399.web.app")
                coverConn.setRequestProperty("Referer", "https://munowatch-2c399.web.app/")

                val coverResponseCode = coverConn.responseCode
                val contentType = coverConn.contentType ?: ""
                Log.d("YOC_SYNC", "Cover response: $coverResponseCode, type: $contentType for $yocId")

                if (coverResponseCode == 200) {
                    val inputStream: InputStream = coverConn.inputStream
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(chunk).also { bytesRead = it } != -1) {
                        buffer.write(chunk, 0, bytesRead)
                    }
                    inputStream.close()
                    coverConn.disconnect()

                    val imageBytes = buffer.toByteArray()
                    Log.d("YOC_SYNC", "Downloaded ${imageBytes.size} bytes for $yocId")

                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    if (bitmap != null) {
                        val maxWidth = 400
                        val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
                        val newHeight = (maxWidth * ratio).toInt()
                        val resized = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)

                        val outputStream = ByteArrayOutputStream()
                        resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val finalBytes = outputStream.toByteArray()
                        posterBase64 = "data:image/jpeg;base64," + Base64.encodeToString(finalBytes, Base64.NO_WRAP)
                        Log.d("YOC_SYNC", "✅ Poster encoded successfully for $yocId (${posterBase64.length} chars)")
                    } else {
                        Log.e("YOC_SYNC", "❌ BitmapFactory returned null for $yocId. Content-Type was: $contentType")
                    }
                } else {
                    val err = coverConn.errorStream?.bufferedReader()?.readText() ?: ""
                    Log.e("YOC_SYNC", "❌ Cover HTTP $coverResponseCode for $yocId: ${err.take(200)}")
                    coverConn.disconnect()
                }
            } catch (e: Exception) {
                Log.e("YOC_SYNC", "❌ Cover exception for $yocId: ${e.message}", e)
            }

            if (posterBase64.isEmpty()) {
                Log.w("YOC_SYNC", "Using fallback poster for $yocId")
                posterBase64 = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=400&q=80"
            }

            val isSeries = detail.optString("type") == "series"

            val yocVjMap = mapOf(
                "Banks" to "VJ Banks", "Emmy" to "VJ Emmy", "Ice P" to "VJ Omutaka Ice P", "Junior" to "VJ Junior",
                "Kevin" to "VJ Kevin", "Kevo" to "VJ Kevo", "Muba" to "VJ Muba", "Ulio" to "VJ Ulio"
            )
            val rawVj = detail.optString("vjName", m.optString("vjName", "")).trim()
            var mappedVj = "VJ HD"
            for ((k, v) in yocVjMap) { if (k.equals(rawVj, true)) { mappedVj = v; break } }

            val yocGenreMap = mapOf(
                "Action" to "Action", "Adventure" to "Adventure", "Animation" to "Animation", "Comedy" to "Comedy",
                "Crime" to "Action", "Drama" to "Action", "Fantasy" to "Adventure", "History" to "Action",
                "Horror" to "Horror", "Romance" to "Romantic", "Thriller" to "Action", "War" to "Action", "Sci-Fi" to "Sci-Fi"
            )
            val rawGenre = detail.optString("genre", m.optString("genre", "")).split(",")[0].trim()
            var mappedCat = "Action"
            for ((k, v) in yocGenreMap) { if (k.equals(rawGenre, true)) { mappedCat = v; break } }

            val rawDate = detail.optString("releaseDate", m.optString("releaseDate", ""))
            val mappedYear = if (rawDate.length >= 4) rawDate.take(4) else "2026"

            val doc = hashMapOf<String, Any>(
                "yocId" to yocId,
                "title" to detail.optString("title", movieTitle),
                "description" to detail.optString("description", ""),
                "category" to mappedCat,
                "videoTag" to mappedVj,
                "year" to mappedYear,
                "poster" to posterBase64,
                "type" to if (isSeries) "TV Show" else "Movie",
                "rating" to detail.optDouble("imdbRating", 8.0).toString(),
                "videoUrl" to detail.optString("streamUrl", ""),
                "downloadUrl" to detail.optString("downloadUrl", ""),
                "subtitleUrl" to "",
                "timestamp" to System.currentTimeMillis(),
                "isVip" to false,
                "isFeatured" to false,
                "isAdult" to false
            )

            if (isSeries && detail.has("episodes")) {
                val epsArr = detail.getJSONArray("episodes")
                val epsList = mutableListOf<HashMap<String, String>>()
                for (j in 0 until epsArr.length()) {
                    val ep = epsArr.getJSONObject(j)
                    epsList.add(hashMapOf(
                        "id" to ep.optString("_id", ep.optString("id", "")),
                        "season" to "S${ep.optInt("season")}",
                        "episode" to "Ep ${ep.optInt("episode")}",
                        "url" to ep.optString("streamUrl", ""),
                        "downloadUrl" to ep.optString("downloadUrl", "")
                    ))
                }
                doc["episodes"] = epsList
            }

            try {
                val tokUrl = URL("https://muno-yoc-proxy.henryssali219.workers.dev/movies/$yocId/stream-token")
                val tokConn = tokUrl.openConnection() as HttpURLConnection
                tokConn.connectTimeout = 15000
                tokConn.readTimeout = 20000
                tokConn.requestMethod = "POST"
                tokConn.setRequestProperty("Content-Type", "application/json")
                tokConn.doOutput = true
                tokConn.outputStream.use { it.write("{}".toByteArray()) }

                val tokResponse = tokConn.inputStream.bufferedReader().readText()
                tokConn.disconnect()
                val tokJson = JSONObject(tokResponse)
                if (tokJson.optBoolean("success", false) && tokJson.has("data")) {
                    val data = tokJson.getJSONObject("data")
                    if (data.has("streamToken")) {
                        doc["streamToken"] = data.getString("streamToken")
                        doc["streamTokenExpires"] = System.currentTimeMillis() + (data.optLong("expiresIn", 7200) * 1000)
                    }
                } else if (tokJson.has("streamToken")) {
                    doc["streamToken"] = tokJson.getString("streamToken")
                    doc["streamTokenExpires"] = System.currentTimeMillis() + (7200 * 1000)
                }
            } catch (e: Exception) {
                Log.w("YOC_SYNC", "Token mint failed for $yocId", e)
            }

            Tasks.await(db.collection("homeland_movies").add(doc), 15, TimeUnit.SECONDS)
            added++
            delay(150)

        } catch (e: Exception) {
            Log.e("YOC_SYNC", "Failed to save $movieTitle", e)
            failed++
        }
    }

    return@withContext Pair(added, failed)
}
