package com.homeland.app

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.map.Mapper
import coil.request.ImageRequest
import coil.request.Options
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

const val ADMIN_EMAIL = "ssalihenry136@gmail.com"
const val WHATSAPP_NUMBER = "256742199254" 
val movieCategories = listOf("All", "Action", "Sci-Fi", "Comedy", "Animation", "Romantic", "Horror", "Adventure")
val tvCategories = listOf("All", "News", "Sports", "Movies", "Entertainment", "Music", "Kids")

data class ThemeConfig(
    val primary: Color = Color(0xFFE50914), 
    val background: Color = Color(0xFF0B0C10), 
    val fontSize: Int = 14, 
    val fontUrl: String = "", 
    val bgImageUrl: String = ""
)

data class ClientVipPlan(
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val days: Int = 0
)

class Base64ImageMapper : Mapper<String, Bitmap> {
    override fun map(data: String, options: Options): Bitmap? {
        if (!data.startsWith("data:image/") || !data.contains(";base64,")) return null
        return try {
            val base64 = data.substringAfter("base64,")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

fun translate(key: String, lang: String): String {
    val translations = mapOf(
        "English" to mapOf("home" to "Home", "tv" to "TV Live", "list" to "Series", "profile" to "Profile", "kids" to "Kids Mode", "lang" to "Language"),
        "Luganda" to mapOf("home" to "Awaka", "tv" to "TV Live", "list" to "Series", "profile" to "Profile", "kids" to "Kids Mode", "lang" to "Olulimi"),
        "Lunyankole" to mapOf("home" to "Omuka", "tv" to "TV Live", "list" to "Series", "profile" to "Profile", "kids" to "Kids Mode", "lang" to "Orurimi"),
        "Arabic" to mapOf("home" to "الرئيسية", "tv" to "بث مباشر", "list" to "مسلسلات", "profile" to "الملف الشخصي", "kids" to "وضع الأطفال", "lang" to "اللغة"),
        "Chinese" to mapOf("home" to "首页", "tv" to "电视直播", "list" to "系列", "profile" to "个人资料", "kids" to "儿童模式", "lang" to "语言"),
        "Japanese" to mapOf("home" to "ホーム", "tv" to "テレビ", "list" to "シリーズ", "profile" to "プロフィール", "kids" to "キッズモード", "lang" to "言語")
    )
    return translations[lang]?.get(key) ?: key
}

fun vibrate(context: Context, isStrong: Boolean = false) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(if (isStrong) 35L else 12L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (isStrong) 35L else 12L)
        }
    } catch (e: Exception) {}
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        Color(0xFF1F232D),
        Color(0xFF2A2F3A),
        Color(0xFF1F232D)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    Box(modifier = modifier.background(brush))
}

fun downloadFile(context: Context, url: String, fileName: String, saveToPublic: Boolean) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle("Downloading $fileName")
        request.setDescription("Homeland App")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setAllowedOverMetered(true)
        
        if (saveToPublic) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        } else {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download Started!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    userEmail: String,
    onDismiss: () -> Unit
) {
    var tidInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    
    var plans by remember { mutableStateOf<List<ClientVipPlan>>(emptyList()) }
    var selectedPlan by remember { mutableStateOf<ClientVipPlan?>(null) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("homeland_vip_plans").get().addOnSuccessListener { snap ->
            if (snap != null) {
                val fetchedPlans = snap.documents.map {
                    ClientVipPlan(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        price = it.getString("price") ?: "",
                        days = it.getLong("days")?.toInt() ?: 0
                    )
                }
                plans = fetchedPlans
                if (fetchedPlans.isNotEmpty()) {
                    selectedPlan = fetchedPlans[0]
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSuccess) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.Green, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Payment Submitted!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your VIP will be activated shortly after we verify your Transaction ID.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(50)) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Upgrade to VIP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select a plan to unlock all premium content.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (plans.isEmpty()) {
                        Text("No plans available at the moment.", color = Color.Red, fontSize = 14.sp)
                    } else {
                        plans.forEach { plan ->
                            val isSelected = selectedPlan?.id == plan.id
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { selectedPlan = plan },
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE50914).copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(plan.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("${plan.days} Days", color = Color.LightGray, fontSize = 12.sp)
                                    }
                                    Text(plan.price, color = if (isSelected) Color(0xFFFCDC04) else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Send Money To:", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("0772 123 456", color = Color(0xFFFCDC04), fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Text("Name: Homeland Media", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Enter Transaction ID (TID)", color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                    
                    OutlinedTextField(
                        value = tidInput,
                        onValueChange = { tidInput = it },
                        placeholder = { Text("e.g. 1234567890", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (tidInput.isNotBlank() && selectedPlan != null) {
                                isLoading = true
                                val requestData = hashMapOf(
                                    "email" to userEmail,
                                    "tid" to tidInput.trim(),
                                    "planName" to selectedPlan!!.name,
                                    "price" to selectedPlan!!.price,
                                    "days" to selectedPlan!!.days,
                                    "status" to "pending",
                                    "timestamp" to System.currentTimeMillis()
                                )
                                FirebaseFirestore.getInstance().collection("payment_requests").add(requestData)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        isSuccess = true
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                    }
                            }
                        },
                        enabled = !isLoading && tidInput.isNotBlank() && selectedPlan != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(50), spotColor = Color(0xFFE50914), ambientColor = Color(0xFFE50914)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Submit Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity(), ImageLoaderFactory {
    private var deepLinkMovieId by mutableStateOf<String?>(null)

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(Base64ImageMapper()) }
            .crossfade(true)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) {
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val isTvDevice = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || 
                         (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
        if (isTvDevice) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // FULLSCREEN IMMERSIVE MODE (Hides Status bar and Phone Navigation buttons)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val db = FirebaseFirestore.getInstance()
            var appTheme by remember { mutableStateOf(ThemeConfig()) }
            var isMaintenanceMode by remember { mutableStateOf(false) }
            var maintenanceMessage by remember { mutableStateOf("We'll be back soon!") }
            
            LaunchedEffect(Unit) {
                db.collection("config").document("theme").addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        val p = doc.getString("primary") ?: "#E50914"
                        val b = doc.getString("background") ?: "#0B0C10"
                        val f = doc.getLong("fontSize")?.toInt() ?: 14
                        val url = doc.getString("fontUrl") ?: ""
                        val bgImg = doc.getString("bgImageUrl") ?: ""
                        appTheme = ThemeConfig(Color(android.graphics.Color.parseColor(p)), Color(android.graphics.Color.parseColor(b)), f, url, bgImg)
                    }
                }
                db.collection("config").document("maintenance").addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        isMaintenanceMode = doc.getBoolean("active") ?: false
                        maintenanceMessage = doc.getString("message") ?: "We'll be back soon!"
                    }
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(primary = appTheme.primary, background = appTheme.background)
            ) {
                CheckForUpdates(this@MainActivity)
                
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                var isAuthenticated by remember { mutableStateOf(currentUser != null) }
                var showProfileSelector by remember { mutableStateOf(false) }
                
                var currentProfileName by remember { mutableStateOf("Me") }

                val prefs = remember { this@MainActivity.getSharedPreferences("homeland_prefs", Context.MODE_PRIVATE) }
                var savedPin by remember { mutableStateOf(prefs.getString("app_pin", null)) }
                var isAppUnlocked by remember { mutableStateOf(savedPin == null) }

                var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_seen", false)) }

                var appLanguage by remember { mutableStateOf(prefs.getString("app_language", "English") ?: "English") }
                var kidsMode by remember { mutableStateOf(prefs.getBoolean("kids_mode", false)) }

                var isBanned by remember { mutableStateOf(false) }
                
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        db.collection("users").document(currentUser.uid).addSnapshotListener { doc, _ ->
                            if (doc != null && doc.exists()) {
                                isBanned = doc.getBoolean("isBanned") ?: false
                            }
                        }
                    } else {
                        isBanned = false
                    }
                }

                var isDeviceAllowed by remember { mutableStateOf<Boolean?>(null) }
                var deviceLimitError by remember { mutableStateOf("") }

                LaunchedEffect(currentUser) {
                    if (currentUser == null) {
                        isDeviceAllowed = true
                        return@LaunchedEffect
                    }
                    if (currentUser.email == ADMIN_EMAIL) {
                        isDeviceAllowed = true
                        return@LaunchedEffect
                    }

                    val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
                    val now = System.currentTimeMillis()
                    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)

                    db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val vipExpiry = doc.getLong("homelandVipExpiry") ?: 0L
                            val maxAllowed = if (vipExpiry > now) 5 else 2
                            
                            @Suppress("UNCHECKED_CAST")
                            val activeDevices = (doc.get("activeDevices") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()

                            activeDevices.entries.removeAll { it.value < thirtyDaysAgo }

                            if (activeDevices.containsKey(deviceId)) {
                                db.collection("users").document(currentUser.uid).update("activeDevices.$deviceId", now)
                                isDeviceAllowed = true
                            } else {
                                if (activeDevices.size < maxAllowed) {
                                    db.collection("users").document(currentUser.uid).update("activeDevices.$deviceId", now)
                                    isDeviceAllowed = true
                                } else {
                                    isDeviceAllowed = false
                                    deviceLimitError = "Your account is already active on $maxAllowed devices. The limit for ${if (vipExpiry > now) "VIP" else "Free"} users is $maxAllowed. Please log out from another device."
                                }
                            }
                        } else {
                            isDeviceAllowed = true 
                        }
                    }.addOnFailureListener {
                        isDeviceAllowed = true 
                    }
                }

                val context = LocalContext.current
                val isTv = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || 
                           (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

                if (showOnboarding) {
                    OnboardingScreen(
                        theme = appTheme,
                        onComplete = {
                            prefs.edit().putBoolean("onboarding_seen", true).apply()
                            showOnboarding = false
                        }
                    )
                } else if (isDeviceAllowed == null) {
                    Box(modifier = Modifier.fillMaxSize().background(appTheme.background), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = appTheme.primary)
                    }
                } else if (isDeviceAllowed == false) {
                    DeviceLimitScreen(
                        theme = appTheme, 
                        message = deviceLimitError,
                        onLogout = {
                            auth.signOut()
                            isAuthenticated = false
                            isDeviceAllowed = null
                        }
                    )
                } else if (isBanned && currentUser?.email != ADMIN_EMAIL) {
                    BannedScreen(
                        theme = appTheme,
                        onLogout = {
                            auth.signOut()
                            isAuthenticated = false
                            isBanned = false
                        }
                    )
                } else if (isMaintenanceMode && currentUser?.email != ADMIN_EMAIL) {
                    MaintenanceScreen(theme = appTheme, message = maintenanceMessage)
                } else if (!isAppUnlocked) {
                    AppLockScreen(
                        theme = appTheme,
                        savedPin = savedPin!!,
                        onUnlock = { isAppUnlocked = true },
                        onForgotPin = {
                            prefs.edit().remove("app_pin").apply()
                            savedPin = null
                            isAppUnlocked = true
                            auth.signOut()
                            isAuthenticated = false
                            Toast.makeText(context, "PIN Reset. Please log in again.", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    if (isTv) {
                        if (isAuthenticated) {
                            HomelandApp(
                                auth = auth, 
                                profileName = currentProfileName, 
                                theme = appTheme, 
                                savedPin = savedPin, 
                                deepLinkMovieId = deepLinkMovieId, 
                                onDeepLinkHandled = { deepLinkMovieId = null }, 
                                appLanguage = appLanguage,
                                onLanguageChanged = { appLanguage = it; prefs.edit().putString("app_language", it).apply() },
                                kidsMode = kidsMode,
                                onKidsModeChanged = { kidsMode = it; prefs.edit().putBoolean("kids_mode", it).apply() },
                                isTv = true,
                                onLogout = { isAuthenticated = false }, 
                                onSwitchProfile = { }
                            )
                        } else {
                            TVLoginScreen(theme = appTheme, onLoginSuccess = { isAuthenticated = true })
                        }
                    } else {
                        if (isAuthenticated) {
                            if (showProfileSelector) {
                                ProfileSelectorScreen(theme = appTheme, onProfileSelected = { selectedProfile -> 
                                    currentProfileName = selectedProfile
                                    showProfileSelector = false 
                                })
                            } else {
                                HomelandApp(
                                    auth = auth, 
                                    profileName = currentProfileName, 
                                    theme = appTheme, 
                                    savedPin = savedPin, 
                                    deepLinkMovieId = deepLinkMovieId, 
                                    onDeepLinkHandled = { deepLinkMovieId = null }, 
                                    appLanguage = appLanguage,
                                    onLanguageChanged = { appLanguage = it; prefs.edit().putString("app_language", it).apply() },
                                    kidsMode = kidsMode,
                                    onKidsModeChanged = { kidsMode = it; prefs.edit().putBoolean("kids_mode", it).apply() },
                                    isTv = false,
                                    onLogout = { isAuthenticated = false; showProfileSelector = false }, 
                                    onSwitchProfile = { showProfileSelector = true }
                                )
                            }
                        } else {
                            AuthScreen(auth = auth, onLoginSuccess = { isAuthenticated = true; showProfileSelector = true })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            if (data.pathSegments.size >= 2 && data.pathSegments[0] == "movie") {
                deepLinkMovieId = data.pathSegments[1]
            } else if (data.lastPathSegment != null) {
                deepLinkMovieId = data.lastPathSegment
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(theme: ThemeConfig, onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val icon = when(page) { 0 -> Icons.Default.Movie; 1 -> Icons.Default.Tv; else -> Icons.Default.Star }
                val title = when(page) { 0 -> "Unlimited Movies"; 1 -> "Live TV Channels"; else -> "VIP Exclusive Content" }
                val description = when(page) { 0 -> "Stream the latest VJ movies, blockbusters, and exclusive homeland content in HD."; 1 -> "Watch live news, sports, music, and entertainment channels from around the world."; else -> "Subscribe to VIP for unlimited access on up to 5 devices, downloads, and zero ads." }
                
                Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(theme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = theme.primary, modifier = Modifier.size(90.dp))
                }
                Spacer(modifier = Modifier.height(40.dp))
                Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(description, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
            }
        }
        
        TextButton(onClick = onComplete, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Text("Skip", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(modifier = Modifier.size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp).clip(CircleShape).background(if (pagerState.currentPage == index) theme.primary else Color.DarkGray))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { if (pagerState.currentPage < 2) coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } else onComplete() },
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(50), spotColor = theme.primary, ambientColor = theme.primary),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(50)
            ) { Text(if (pagerState.currentPage < 2) "Next" else "Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun MaintenanceScreen(theme: ThemeConfig, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color(0xFF3A0004), Color(0xFF0B0C10), Color(0xFF0B0C10)), radius = 1200f)), 
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Build, contentDescription = "Maintenance", tint = theme.primary, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Under Maintenance", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = theme.primary)
            }
        }
    }
}

@Composable
fun DeviceLimitScreen(theme: ThemeConfig, message: String, onLogout: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color(0xFF1A0002), Color(0xFF0B0C10)), radius = 1200f)), 
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Devices, contentDescription = "Devices", tint = theme.primary, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Device Limit Reached", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onLogout, 
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary), 
                    shape = RoundedCornerShape(50), 
                    modifier = Modifier.height(56.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(50), spotColor = theme.primary, ambientColor = theme.primary)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun BannedScreen(theme: ThemeConfig, onLogout: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color(0xFF2D0000), Color(0xFF0B0C10)), radius = 1200f)), 
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Block, contentDescription = "Banned", tint = Color.Red, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Account Suspended", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your account has been banned by the administrator. If you believe this is a mistake, please contact support.", color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onLogout, 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red), 
                    shape = RoundedCornerShape(50), 
                    modifier = Modifier.height(56.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(50), spotColor = Color.Red, ambientColor = Color.Red)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(theme: ThemeConfig, savedPin: String, onUnlock: () -> Unit, onForgotPin: () -> Unit) {
    var inputPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color(0xFF1A0002), Color(0xFF0B0C10)), radius = 1200f)), 
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = theme.primary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("App Locked", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Enter your 4-digit PIN", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (i in 0 until 4) {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(if (i < inputPin.length) theme.primary else Color.DarkGray))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (errorMsg != null) { Text(errorMsg!!, color = Color.Red, fontSize = 12.sp) }
                Spacer(modifier = Modifier.height(24.dp))

                val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "DEL"))
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        row.forEach { key ->
                            if (key.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                        .clickable {
                                            if (key == "DEL") { if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1) } 
                                            else { if (inputPin.length < 4) inputPin += key }
                                            errorMsg = null
                                            if (inputPin.length == 4) {
                                                if (inputPin == savedPin) onUnlock() 
                                                else { errorMsg = "Incorrect PIN"; inputPin = "" }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = Color.White)
                                    else Text(key, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            } else { Spacer(modifier = Modifier.size(70.dp)) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onForgotPin) { Text("Forgot PIN? (Logs you out)", color = Color.Gray, fontSize = 12.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomelandApp(
    auth: FirebaseAuth, 
    profileName: String, 
    theme: ThemeConfig, 
    savedPin: String?, 
    deepLinkMovieId: String? = null, 
    onDeepLinkHandled: () -> Unit = {}, 
    appLanguage: String,
    onLanguageChanged: (String) -> Unit,
    kidsMode: Boolean,
    onKidsModeChanged: (Boolean) -> Unit,
    isTv: Boolean,
    onLogout: () -> Unit, 
    onSwitchProfile: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val prefs = remember { context.getSharedPreferences("homeland_prefs", Context.MODE_PRIVATE) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Movies, 2: Series, 3: TV Live
    var showProfile by remember { mutableStateOf(false) }
    
    var currentlyPlaying by remember { mutableStateOf<Movie?>(null) }
    var viewingDetails by remember { mutableStateOf<Movie?>(null) }
    var userVipExpiry by remember { mutableLongStateOf(0L) }
    var showVipModal by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    var globalSearchQuery by remember { mutableStateOf("") }

    var tvSessionCode by remember { mutableStateOf<String?>(null) }

    var pendingAdultMovie by remember { mutableStateOf<Movie?>(null) }
    var showAdultPinPrompt by remember { mutableStateOf(false) }
    var adultPinInput by remember { mutableStateOf("") }

    var wifiOnlyMode by remember { mutableStateOf(prefs.getBoolean("wifi_only", false)) }
    var showWifiWarning by remember { mutableStateOf(false) }
    var pendingPlayMovie by remember { mutableStateOf<Movie?>(null) }

    fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun tryPlay(movie: Movie) {
        if (wifiOnlyMode && !isOnWifi()) {
            pendingPlayMovie = movie
            showWifiWarning = true
        } else {
            currentlyPlaying = movie
        }
    }

    LaunchedEffect(isTv, auth.currentUser) {
        val user = auth.currentUser
        if (isTv && user != null) {
            val existing = prefs.getString("tv_session_code", null)
            val code = existing ?: (100000 + Random.nextInt(900000)).toString()
            prefs.edit().putString("tv_session_code", code).apply()
            tvSessionCode = code
            db.collection("tv_sessions").document(code).set(
                mapOf(
                    "userId" to user.uid,
                    "email" to (user.email ?: ""),
                    "createdAt" to System.currentTimeMillis(),
                    "lastSeen" to System.currentTimeMillis(),
                    "status" to "idle",
                    "currentTitle" to "",
                    "currentTab" to 0
                )
            )
        }
    }

    LaunchedEffect(tvSessionCode) {
        if (tvSessionCode != null) {
            val listener = db.collection("tv_sessions").document(tvSessionCode!!)
                .collection("commands")
                .whereEqualTo("processed", false)
                .addSnapshotListener { snap, _ ->
                    snap?.documents?.forEach { cmdDoc ->
                        val type = cmdDoc.getString("type") ?: return@forEach
                        val value = cmdDoc.getString("value") ?: ""
                        val act = context as? Activity
                        when (type) {
                            "tab" -> selectedTab = value.toIntOrNull() ?: 0
                            "back" -> {
                                if (currentlyPlaying != null) currentlyPlaying = null
                                else if (viewingDetails != null) viewingDetails = null
                                else if (showVipModal) showVipModal = false
                                else if (showAdultPinPrompt) { showAdultPinPrompt = false; pendingAdultMovie = null }
                                else if (showProfile) showProfile = false
                                else if (selectedTab != 0) selectedTab = 0
                            }
                            "search" -> {
                                globalSearchQuery = value
                                selectedTab = 0
                            }
                            "clear_search" -> globalSearchQuery = ""
                            "volume_up" -> {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                            }
                            "volume_down" -> {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                            }
                            "close_player" -> currentlyPlaying = null
                            
                            "dpad_up" -> {
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
                            }
                            "dpad_down" -> {
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
                            }
                            "dpad_left" -> {
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                            }
                            "dpad_right" -> {
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
                            }
                            "dpad_center" -> {
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
                                act?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
                            }
                        }
                        cmdDoc.reference.update("processed", true)
                    }
                }
        }
    }

    LaunchedEffect(currentlyPlaying, selectedTab, tvSessionCode, showProfile) {
        if (tvSessionCode != null) {
            db.collection("tv_sessions").document(tvSessionCode!!).update(
                "status", if (currentlyPlaying != null) "playing" else "idle",
                "currentTitle", currentlyPlaying?.title ?: "",
                "currentTab", selectedTab,
                "lastSeen", System.currentTimeMillis()
            )
        }
    }

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                userVipExpiry = (doc?.getLong("homelandVipExpiry")) ?: 0L
            }
        }
    }

    LaunchedEffect(Unit) { FirebaseMessaging.getInstance().subscribeToTopic("homeland_updates") }

    LaunchedEffect(deepLinkMovieId) {
        if (deepLinkMovieId != null) {
            db.collection("homeland_movies").document(deepLinkMovieId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val episodes = (doc.get("episodes") as? List<Map<String, Any>>)?.map {
                        Episode(
                            season = it["season"] as? String ?: "",
                            episode = it["episode"] as? String ?: "",
                            url = it["url"] as? String ?: "",
                            subtitleUrl = it["subtitleUrl"] as? String ?: ""
                        )
                    } ?: emptyList()
                    
                    val movie = Movie(
                        id = doc.id, 
                        title = doc.getString("title") ?: "", 
                        poster = doc.getString("poster") ?: "", 
                        videoUrl = doc.getString("videoUrl") ?: "", 
                        category = doc.getString("category") ?: "", 
                        videoTag = doc.getString("videoTag") ?: "", 
                        year = doc.getString("year") ?: "", 
                        isVip = doc.getBoolean("isVip") ?: false, 
                        isFeatured = doc.getBoolean("isFeatured") ?: false, 
                        isAdult = doc.getBoolean("isAdult") ?: false, 
                        subtitleUrl = doc.getString("subtitleUrl") ?: "",
                        episodes = episodes,
                        yocId = doc.getString("yocId") ?: ""
                    )
                    viewingDetails = movie 
                }
                onDeepLinkHandled() 
            }
        }
    }

    BackHandler(enabled = true) {
        if (currentlyPlaying != null) currentlyPlaying = null 
        else if (viewingDetails != null) viewingDetails = null 
        else if (showVipModal) showVipModal = false 
        else if (showPaymentDialog) showPaymentDialog = false 
        else if (showAdultPinPrompt) { showAdultPinPrompt = false; pendingAdultMovie = null }
        else if (showProfile) showProfile = false
        else if (selectedTab != 0) selectedTab = 0 
        else showExitDialog = true 
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Homeland?", color = Color.White) },
            text = { Text("We always welcome you back!", color = Color.Gray) },
            confirmButton = { Button(onClick = { (context as? Activity)?.finish() }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary), shape = RoundedCornerShape(50)) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("No", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (showPaymentDialog) {
        PaymentDialog(userEmail = auth.currentUser?.email ?: "Unknown", onDismiss = { showPaymentDialog = false })
    }

    if (showAdultPinPrompt) {
        AlertDialog(
            onDismissRequest = { showAdultPinPrompt = false; pendingAdultMovie = null },
            title = { Text("18+ Content Locked", color = Color.White) },
            text = { 
                Column {
                    Text("Enter your App PIN to watch this movie.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = adultPinInput, onValueChange = { if (it.length <= 4) adultPinInput = it }, placeholder = { Text("4-digit PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (adultPinInput == savedPin) {
                        val movieToPlay = pendingAdultMovie
                        showAdultPinPrompt = false; pendingAdultMovie = null; adultPinInput = ""
                        if (movieToPlay != null) tryPlay(movieToPlay)
                    } else { Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show(); adultPinInput = "" }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(50)) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = { showAdultPinPrompt = false; pendingAdultMovie = null; adultPinInput = "" }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (currentlyPlaying != null) {
        val movie = currentlyPlaying!!
        PlayerScreen(title = movie.title, videoUrl = movie.videoUrl, episodes = movie.episodes, startPosition = 0L, subtitleUrl = movie.subtitleUrl.ifBlank { null }, tvSessionCode = tvSessionCode, yocId = movie.yocId, onClose = { currentlyPlaying = null })
        return 
    }

    if (viewingDetails != null) {
        MovieDetailsScreen(
            movie = viewingDetails!!, userVipExpiry = userVipExpiry,
            onShowVipModal = { showVipModal = true },
            onPlay = { movieToPlay -> 
                if (movieToPlay.isVip && userVipExpiry < System.currentTimeMillis()) showVipModal = true
                else if (movieToPlay.isAdult && savedPin != null) { pendingAdultMovie = movieToPlay; showAdultPinPrompt = true } 
                else { tryPlay(movieToPlay) }
            },
            onBack = { viewingDetails = null }
        )
        return
    }

    if (showVipModal) {
        AlertDialog(
            onDismissRequest = { showVipModal = false },
            title = { Text("VIP Content", color = Color.White) },
            text = { Text("This content is for VIP members only.", color = Color.Gray) },
            confirmButton = {
                Button(onClick = { 
                    showVipModal = false
                    showPaymentDialog = true 
                }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary), shape = RoundedCornerShape(50)) { Text("Subscribe Now") }
            },
            dismissButton = { TextButton(onClick = { showVipModal = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (showWifiWarning && pendingPlayMovie != null) {
        AlertDialog(
            onDismissRequest = { showWifiWarning = false; pendingPlayMovie = null },
            title = { Text("Wi-Fi Only Mode", color = Color.White) },
            text = { Text("You are currently on Mobile Data but Wi-Fi Only streaming is enabled. Playing this video will use your mobile data. Continue anyway?", color = Color.Gray) },
            confirmButton = {
                Button(onClick = { 
                    val movie = pendingPlayMovie
                    showWifiWarning = false; pendingPlayMovie = null
                    if (movie != null) currentlyPlaying = movie
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(50)) { Text("Continue Anyway") }
            },
            dismissButton = { TextButton(onClick = { showWifiWarning = false; pendingPlayMovie = null }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (showProfile) {
        ProfileScreen(
            auth = auth, profileName = profileName, userVipExpiry = userVipExpiry, theme = theme, savedPin = savedPin, 
            wifiOnlyMode = wifiOnlyMode, onWifiOnlyChanged = { wifiOnlyMode = it; prefs.edit().putBoolean("wifi_only", it).apply() }, 
            appLanguage = appLanguage, onLanguageChanged = onLanguageChanged, 
            kidsMode = kidsMode, onKidsModeChanged = onKidsModeChanged, 
            isTv = isTv, tvSessionCode = tvSessionCode, 
            onLogout = onLogout, onSwitchProfile = onSwitchProfile, onPlay = { tryPlay(it) },
            onBack = { showProfile = false }
        )
        return
    }

    // TV Layout
    if (isTv) {
        Row(modifier = Modifier.fillMaxSize().background(theme.background)) {
            NavigationRail(containerColor = Color(0xFF15171E), modifier = Modifier.fillMaxHeight()) {
                Spacer(modifier = Modifier.height(32.dp))
                NavigationRailItem(selected = selectedTab == 0, onClick = { vibrate(context); selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
                NavigationRailItem(selected = selectedTab == 1, onClick = { vibrate(context); selectedTab = 1 }, icon = { Icon(Icons.Default.Movie, "Movies") }, label = { Text("Movies") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
                NavigationRailItem(selected = selectedTab == 2, onClick = { vibrate(context); selectedTab = 2 }, icon = { Icon(Icons.Default.VideoLibrary, "Series") }, label = { Text("Series") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
                NavigationRailItem(selected = selectedTab == 3, onClick = { vibrate(context); selectedTab = 3 }, icon = { Icon(Icons.Default.Tv, "TV Live") }, label = { Text("TV Live") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
                NavigationRailItem(selected = showProfile, onClick = { showProfile = true }, icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
            }

            // FIXED TV LAYOUT: Weight(1f) ensures it fills the remaining space next to the Rail
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (theme.bgImageUrl.isNotEmpty()) AsyncImage(model = theme.bgImageUrl, contentDescription = "App Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(modifier = Modifier.fillMaxSize().background(theme.background))
                
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onDetails = { viewingDetails = it }, isMoviesTab = false)
                        1 -> MoviesScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onDetails = { viewingDetails = it })
                        2 -> SeriesScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onDetails = { viewingDetails = it })
                        3 -> TvLiveScreen(theme = theme, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onPlayMedia = { tryPlay(it) })
                    }
                }
            }
        }
    } else {
        // Mobile Layout - Top Header + Search + Floating Bottom Nav
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF15171E).copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                ) {
                    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        NavigationBarItem(selected = selectedTab == 0, onClick = { vibrate(context); selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.15f)))
                        NavigationBarItem(selected = selectedTab == 1, onClick = { vibrate(context); selectedTab = 1 }, icon = { Icon(Icons.Default.Movie, "Movies") }, label = { Text("Movies") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.15f)))
                        NavigationBarItem(selected = selectedTab == 2, onClick = { vibrate(context); selectedTab = 2 }, icon = { Icon(Icons.Default.VideoLibrary, "Series") }, label = { Text("Series") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.15f)))
                        NavigationBarItem(selected = selectedTab == 3, onClick = { vibrate(context); selectedTab = 3 }, icon = { Icon(Icons.Default.Tv, "TV Live") }, label = { Text("TV Live") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.15f)))
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Background covers the entire screen
                if (theme.bgImageUrl.isNotEmpty()) {
                    AsyncImage(model = theme.bgImageUrl, contentDescription = "App Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(theme.background))
                }

                // 2. UI Content
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    
                    // FIXED HEADER: Removed displayCutoutPadding() so it goes all the way to the top edge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(theme.primary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { 
                                Text("H", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) 
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HOMELAND", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text(" TV", color = theme.primary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                        
                        IconButton(
                            onClick = { showProfile = true }, 
                            modifier = Modifier.size(40.dp).background(Color(0xFF15171E), CircleShape).border(1.dp, theme.primary, CircleShape)
                        ) { 
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = theme.primary) 
                        }
                    }

                    // Global Search Bar
                    OutlinedTextField(
                        value = globalSearchQuery, 
                        onValueChange = { globalSearchQuery = it }, 
                        placeholder = { Text("Search titles...", color = Color.Gray, fontSize = 14.sp) }, 
                        leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) }, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)), 
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent, 
                            unfocusedBorderColor = Color.Transparent, 
                            focusedContainerColor = Color(0xFF15171E).copy(alpha = 0.6f), 
                            unfocusedContainerColor = Color(0xFF15171E).copy(alpha = 0.6f), 
                            focusedTextColor = Color.White, 
                            unfocusedTextColor = Color.White
                        ), 
                        shape = RoundedCornerShape(50)
                    )

                    // Tab Content Area
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> HomeScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onDetails = { viewingDetails = it }, isMoviesTab = false)
                            1 -> MoviesScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onDetails = { viewingDetails = it })
                            2 -> SeriesScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onDetails = { viewingDetails = it })
                            3 -> TvLiveScreen(theme = theme, userVipExpiry = userVipExpiry, kidsMode = kidsMode, searchQuery = globalSearchQuery, onPlayMedia = { tryPlay(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSelectorScreen(theme: ThemeConfig, onProfileSelected: (String) -> Unit) {
    val profiles = listOf("Me", "Kids", "Guest")
    val avatarColors = listOf(Color(0xFFE50914), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFFCDC04), Color(0xFF8B5CF6))
    val avatarIcons = listOf(Icons.Default.Person, Icons.Default.ChildCare, Icons.Default.PersonOutline, Icons.Default.Star, Icons.Default.Face)
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        if (theme.bgImageUrl.isNotEmpty()) {
            AsyncImage(model = theme.bgImageUrl, contentDescription = "App Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
        }
        
        Column(
            modifier = Modifier.fillMaxSize().displayCutoutPadding(),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Who's Watching?", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 40.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                profiles.forEachIndexed { index, profile ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { vibrate(context); onProfileSelected(profile) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(avatarColors[index % avatarColors.size].copy(alpha = 0.1f))
                                .border(2.dp, avatarColors[index % avatarColors.size], RoundedCornerShape(24.dp))
                                .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = avatarColors[index % avatarColors.size]),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(avatarIcons[index % avatarIcons.size], contentDescription = profile, tint = avatarColors[index % avatarColors.size], modifier = Modifier.size(50.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(profile, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { vibrate(context); onProfileSelected("New Profile") }, 
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(50.dp).shadow(8.dp, RoundedCornerShape(50), spotColor = theme.primary, ambientColor = theme.primary)
            ) { Text("Add Profile", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun DownloadsScreen(theme: ThemeConfig, onBack: () -> Unit, onPlay: (Movie) -> Unit) {
    val context = LocalContext.current
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        downloadedFiles = dir?.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color(0xFF1A0002), Color(0xFF0B0C10)), radius = 1200f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).displayCutoutPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(12.dp))
                Text("My Downloads", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            
            if (downloadedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Download, contentDescription = "Empty", tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No in-app downloads found.", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(downloadedFiles) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { vibrate(context); onPlay(Movie(title = file.nameWithoutExtension, videoUrl = file.absolutePath)) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(theme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = "Movie", tint = theme.primary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(file.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val sizeInMb = file.length() / (1024 * 1024)
                                    Text("$sizeInMb MB", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF15171E).copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { vibrate(context); onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, onClick: () -> Unit, iconTint: Color = Color.White, titleColor: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Go", tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsRowWithSwitch(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, iconTint: Color = Color.White, titleColor: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE50914)))
    }
}

@Composable
fun NetflixHistoryCard(movie: Movie, progressPercent: Float, onClick: () -> Unit) {
    Box(modifier = Modifier.width(200.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).background(Color(0xFF15171E).copy(alpha = 0.8f)).clickable { onClick() }) {
        SubcomposeAsyncImage(model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(), contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) }, error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E))) })
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
        Box(modifier = Modifier.align(Alignment.Center).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp)) }
        Text(text = movie.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 10.dp, end = 8.dp))
        Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.DarkGray.copy(alpha = 0.5f))) { Box(modifier = Modifier.fillMaxWidth(progressPercent.coerceIn(0f, 1f)).fillMaxHeight().background(Color.Red)) }
    }
}

@Composable
fun VipCountdownCard(userVipExpiry: Long, theme: ThemeConfig) {
    var dateText by remember { mutableStateOf("Calculating...") }
    var timeText by remember { mutableStateOf("--:--:--") }
    
    LaunchedEffect(userVipExpiry) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = userVipExpiry - now
            if (diff <= 0) { dateText = "VIP Expired"; timeText = "00:00:00"; break } 
            else {
                val totalSeconds = diff / 1000
                val totalDays = totalSeconds / 86400
                val years = totalDays / 365; val remDaysAfterYears = totalDays % 365; val months = remDaysAfterYears / 30; val remDaysAfterMonths = remDaysAfterYears % 30; val weeks = remDaysAfterMonths / 7; val days = remDaysAfterMonths % 7
                val hours = (totalSeconds % 86400) / 3600; val minutes = (totalSeconds % 3600) / 60; val seconds = totalSeconds % 60
                dateText = String.format("%dY - %dM - %dW - %dD", years, months, weeks, days)
                timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
            delay(1000)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp), 
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)), 
        shape = RoundedCornerShape(16.dp), 
        border = BorderStroke(1.dp, Color(0xFFFCDC04).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dateText, color = Color(0xFFFCDC04), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(timeText, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    auth: FirebaseAuth, profileName: String, userVipExpiry: Long, theme: ThemeConfig, savedPin: String?, 
    wifiOnlyMode: Boolean = false, onWifiOnlyChanged: (Boolean) -> Unit = {},
    appLanguage: String, onLanguageChanged: (String) -> Unit,
    kidsMode: Boolean, onKidsModeChanged: (Boolean) -> Unit,
    isTv: Boolean,
    tvSessionCode: String?,
    onLogout: () -> Unit, onSwitchProfile: () -> Unit, onPlay: (Movie) -> Unit,
    onBack: () -> Unit = {} 
) {
    val context = LocalContext.current
    var showAdminPanel by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTvRemoteDialog by remember { mutableStateOf(false) }
    var couponCode by remember { mutableStateOf("") }
    val isVip = userVipExpiry > System.currentTimeMillis()

    var showPaymentDialog by remember { mutableStateOf(false) }

    var socialLinks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("config").document("social_links")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    socialLinks = mapOf(
                        "whatsapp" to (snapshot.getString("whatsapp") ?: ""),
                        "telegram" to (snapshot.getString("telegram") ?: ""),
                        "tiktok" to (snapshot.getString("tiktok") ?: ""),
                        "twitter" to (snapshot.getString("twitter") ?: ""),
                        "instagram" to (snapshot.getString("instagram") ?: "")
                    )
                }
            }
    }

    var showTvDialog by remember { mutableStateOf(false) }
    var tvCodeInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var showCouponDialog by remember { mutableStateOf(false) }
    
    var showAdmin2faDialog by remember { mutableStateOf(false) }
    var admin2faInput by remember { mutableStateOf("") }
    var admin2faError by remember { mutableStateOf<String?>(null) }
    var dbAdminPin by remember { mutableStateOf("123456") }

    val prefs = remember { context.getSharedPreferences("homeland_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("config").document("admin").addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) dbAdminPin = doc.getString("adminPin") ?: "123456"
        }
    }

    var profileHistory by remember { mutableStateOf<List<Pair<Movie, Float>>>(emptyList()) }
    LaunchedEffect(profileName) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).collection("history").whereEqualTo("profile", profileName).get().addOnSuccessListener { snap ->
            val historyMap = snap.documents.associate { 
                val prog = it.getLong("progress") ?: 0L; val dur = it.getLong("duration") ?: 1L
                it.id to (prog.toFloat() / dur.toFloat())
            }
            db.collection("homeland_movies").limit(20).get().addOnSuccessListener { moviesSnap ->
                profileHistory = moviesSnap.documents.mapNotNull {
                    val episodes = (it.get("episodes") as? List<Map<String, Any>>)?.map { ep ->
                        Episode(
                            season = ep["season"] as? String ?: "",
                            episode = ep["episode"] as? String ?: "",
                            url = ep["url"] as? String ?: "",
                            subtitleUrl = ep["subtitleUrl"] as? String ?: ""
                        )
                    } ?: emptyList()

                    val m = Movie(
                        id = it.id, 
                        title = it.getString("title") ?: "", 
                        poster = it.getString("poster") ?: "", 
                        videoUrl = it.getString("videoUrl") ?: "", 
                        isVip = it.getBoolean("isVip") ?: false,
                        episodes = episodes,
                        yocId = it.getString("yocId") ?: ""
                    )
                    val progress = historyMap[m.id]
                    if (progress != null) Pair(m, progress) else null
                }
            }
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(userEmail = auth.currentUser?.email ?: "Unknown", onDismiss = { showPaymentDialog = false })
    }

    if (showAdminPanel) {
        AdminScreen(onBack = { showAdminPanel = false })
    } else if (showDownloads) {
        DownloadsScreen(theme = theme, onBack = { showDownloads = false }, onPlay = onPlay)
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (theme.bgImageUrl.isNotEmpty()) {
                AsyncImage(model = theme.bgImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
            } else { Box(modifier = Modifier.fillMaxSize().background(theme.background)) }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).displayCutoutPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF15171E))
                        .border(3.dp, theme.primary, CircleShape)
                        .shadow(16.dp, CircleShape, spotColor = theme.primary, ambientColor = theme.primary), 
                    contentAlignment = Alignment.Center
                ) { 
                    Icon(Icons.Default.Person, "Avatar", tint = theme.primary, modifier = Modifier.size(60.dp)) 
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(auth.currentUser?.email?.split("@")?.getOrNull(0) ?: "User", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Watching as: $profileName", color = theme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                if (isVip) { VipCountdownCard(userVipExpiry = userVipExpiry, theme = theme) } 
                else { Text("Status: Free User", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)) }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickActionButton(icon = Icons.Default.Star, label = "VIP Plans", modifier = Modifier.weight(1f), onClick = {
                        if (!isVip) { showPaymentDialog = true }
                        else { Toast.makeText(context, "You are already VIP!", Toast.LENGTH_SHORT).show() }
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    QuickActionButton(icon = Icons.Default.Download, label = "Downloads", modifier = Modifier.weight(1f), onClick = { showDownloads = true })
                    Spacer(modifier = Modifier.width(8.dp))
                    QuickActionButton(icon = Icons.Default.Tv, label = "Login TV", modifier = Modifier.weight(1f), onClick = { showTvDialog = true })
                    Spacer(modifier = Modifier.width(8.dp))
                    QuickActionButton(icon = Icons.Default.SwitchAccount, label = "Profiles", modifier = Modifier.weight(1f), onClick = onSwitchProfile)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.7f)), 
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column {
                        SettingsRow(icon = Icons.Default.Lock, title = if (savedPin != null) "App Lock (Enabled)" else "Setup App Lock", onClick = { showPinDialog = true })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        
                        if (!isTv) {
                            SettingsRow(icon = Icons.Default.SettingsRemote, title = "TV Remote Control", onClick = { showTvRemoteDialog = true }, iconTint = theme.primary, titleColor = theme.primary)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                        
                        if (isTv && tvSessionCode != null) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cast, contentDescription = "TV Code", tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Your TV Control Code", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Open the app on your phone to control this TV", color = Color.Gray, fontSize = 11.sp)
                                }
                                Text(
                                    tvSessionCode.replaceFirst("(\\d{3})(\\d{3})".toRegex(), "$1 $2"),
                                    color = Color(0xFF10B981),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                        
                        SettingsRow(icon = Icons.Default.Language, title = "${translate("lang", appLanguage)}: $appLanguage", onClick = { showLanguageDialog = true })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        
                        SettingsRowWithSwitch(icon = Icons.Default.ChildCare, title = translate("kids", appLanguage), checked = kidsMode, onCheckedChange = onKidsModeChanged)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        
                        SettingsRowWithSwitch(icon = Icons.Default.Wifi, title = "Wi-Fi Only Streaming", checked = wifiOnlyMode, onCheckedChange = onWifiOnlyChanged)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        
                        if (!isVip) {
                            SettingsRow(icon = Icons.Default.ConfirmationNumber, title = "Redeem Coupon", onClick = { showCouponDialog = true })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                        
                        if (auth.currentUser?.email == ADMIN_EMAIL) {
                            SettingsRow(icon = Icons.Default.AdminPanelSettings, title = "Admin Panel (2FA)", onClick = { showAdmin2faDialog = true }, iconTint = theme.primary, titleColor = theme.primary)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                        
                        SettingsRow(icon = Icons.Default.Logout, title = "Log Out", onClick = { auth.signOut(); onLogout() }, iconTint = Color.Red, titleColor = Color.Red)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (profileHistory.isNotEmpty()) {
                    Text("Continue Watching", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(profileHistory) { pair -> NetflixHistoryCard(movie = pair.first, progressPercent = pair.second, onClick = { onPlay(pair.first) }) }
                    }
                }

                if (socialLinks.values.any { it.isNotBlank() }) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = Color.DarkGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Follow Us", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    val uriHandler = LocalUriHandler.current
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!socialLinks["whatsapp"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["whatsapp"]!!) }) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                            }
                        }
                        if (!socialLinks["telegram"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["telegram"]!!) }) {
                                Icon(Icons.Default.Send, contentDescription = "Telegram", tint = Color(0xFF0088cc))
                            }
                        }
                        if (!socialLinks["tiktok"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["tiktok"]!!) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "TikTok", tint = Color.White)
                            }
                        }
                        if (!socialLinks["twitter"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["twitter"]!!) }) {
                                Icon(Icons.Default.Share, contentDescription = "Twitter", tint = Color(0xFF1DA1F2))
                            }
                        }
                        if (!socialLinks["instagram"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["instagram"]!!) }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Instagram", tint = Color(0xFFE1306C))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showTvRemoteDialog) {
        TVRemoteConnectDialog(
            theme = theme,
            onDismiss = { showTvRemoteDialog = false }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language", color = Color.White) },
            text = {
                Column {
                    listOf("English", "Luganda", "Lunyankole", "Arabic", "Chinese", "Japanese").forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { vibrate(context); onLanguageChanged(lang); showLanguageDialog = false }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLanguage == lang,
                                onClick = { onLanguageChanged(lang); showLanguageDialog = false },
                                colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                            )
                            Text(lang, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Close", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (showAdmin2faDialog) {
        AlertDialog(
            onDismissRequest = { showAdmin2faDialog = false; admin2faInput = ""; admin2faError = null },
            title = { Text("Admin 2FA Security", color = Color.White) },
            text = { 
                Column {
                    Text("Enter your 6-digit Admin PIN to access the control panel.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = admin2faInput, onValueChange = { if (it.length <= 6) admin2faInput = it }, placeholder = { Text("6-digit PIN (Default: 123456)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    if (admin2faError != null) { Spacer(modifier = Modifier.height(4.dp)); Text(admin2faError!!, color = Color.Red, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (admin2faInput == dbAdminPin) { showAdminPanel = true; showAdmin2faDialog = false; admin2faInput = ""; admin2faError = null } 
                    else { admin2faError = "Incorrect Admin PIN"; admin2faInput = "" }
                }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary), shape = RoundedCornerShape(50)) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = { showAdmin2faDialog = false; admin2faInput = ""; admin2faError = null }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (showCouponDialog) {
        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text("Redeem Coupon", color = Color.White) },
            text = { OutlinedTextField(value = couponCode, onValueChange = { couponCode = it }, placeholder = { Text("Enter Code") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) },
            confirmButton = {
                Button(onClick = {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("homeland_coupons").whereEqualTo("code", couponCode.trim()).whereEqualTo("isUsed", false).get().addOnSuccessListener { snap ->
                        if (!snap.isEmpty) {
                            val doc = snap.documents[0]; val days = doc.getLong("days") ?: 7L
                            val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                            db.collection("users").document(uid).update("homelandVipExpiry", System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L))
                            db.collection("homeland_coupons").document(doc.id).update("isUsed", true)
                            Toast.makeText(context, "Success! You are now VIP.", Toast.LENGTH_LONG).show()
                            couponCode = ""; showCouponDialog = false; onSwitchProfile() 
                        } else { Toast.makeText(context, "Invalid or used code.", Toast.LENGTH_SHORT).show() }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary), shape = RoundedCornerShape(50)) { Text("Redeem") }
            },
            dismissButton = { TextButton(onClick = { showCouponDialog = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (savedPin != null) "Remove App Lock" else "Setup App Lock", color = Color.White) },
            text = {
                if (savedPin != null) { Text("Are you sure you want to remove the PIN? Anyone can open the app.", color = Color.Gray) } 
                else {
                    Column {
                        Text("Enter a 4-digit PIN. You will need this to open the app and watch 18+ content.", color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = newPinInput, onValueChange = { if (it.length <= 4) newPinInput = it }, placeholder = { Text("Enter 4-digit PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (savedPin != null) { prefs.edit().remove("app_pin").apply(); Toast.makeText(context, "App Lock Removed", Toast.LENGTH_SHORT).show(); showPinDialog = false; onSwitchProfile() } 
                    else {
                        if (newPinInput.length == 4) { prefs.edit().putString("app_pin", newPinInput).apply(); Toast.makeText(context, "App Lock Enabled!", Toast.LENGTH_SHORT).show(); showPinDialog = false; newPinInput = ""; onSwitchProfile() } 
                        else { Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show() }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = if (savedPin != null) Color.Red else Color(0xFF25D366)), shape = RoundedCornerShape(50)) { Text(if (savedPin != null) "Remove" else "Save PIN") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false; newPinInput = "" }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    if (showTvDialog) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTvDialog = false },
            title = { Text("Login on TV", color = Color.White) },
            text = { 
                Column {
                    Text("Enter the 6-digit code from your TV and your password to authorize.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = tvCodeInput, onValueChange = { tvCodeInput = it }, placeholder = { Text("e.g. 603061") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, placeholder = { Text("Your Account Password") }, visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tvCodeInput.length == 6 && passwordInput.isNotEmpty()) {
                        val userEmail = auth.currentUser?.email
                        if (userEmail != null) {
                            auth.signInWithEmailAndPassword(userEmail, passwordInput).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("tv_login_codes").document(tvCodeInput).update("status", "approved", "email", userEmail, "password", passwordInput).addOnSuccessListener {
                                        Toast.makeText(context, "TV Authorized successfully!", Toast.LENGTH_SHORT).show()
                                        showTvDialog = false; tvCodeInput = ""; passwordInput = ""
                                    }
                                } else { Toast.makeText(context, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    } else { Toast.makeText(context, "Please enter 6 digits and your password", Toast.LENGTH_SHORT).show() }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(50)) { Text("Authorize") }
            },
            dismissButton = { TextButton(onClick = { showTvDialog = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVRemoteConnectDialog(theme: ThemeConfig, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var connectedCode by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var tvStatus by remember { mutableStateOf("idle") }
    var tvTitle by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(connectedCode) {
        if (connectedCode != null) {
            val listener = db.collection("tv_sessions").document(connectedCode!!)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        tvStatus = doc.getString("status") ?: "idle"
                        tvTitle = doc.getString("currentTitle") ?: ""
                    } else {
                        errorMsg = "TV disconnected."
                        connectedCode = null
                    }
                }
        }
    }

    fun sendCommand(type: String, value: String = "") {
        if (connectedCode == null) return
        vibrate(context, isStrong = true)
        db.collection("tv_sessions").document(connectedCode!!).collection("commands").add(
            mapOf(
                "type" to type,
                "value" to value,
                "processed" to false,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF0B0C10),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
        ) {
            if (connectedCode == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SettingsRemote, contentDescription = "Remote", tint = theme.primary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connect to TV", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Open Homeland on your TV, go to Profile, and enter the 6-digit code shown there.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) codeInput = it },
                        placeholder = { Text("Enter 6-digit code", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = theme.primary,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg!!, color = Color.Red, fontSize = 13.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = {
                            if (codeInput.length == 6) {
                                isConnecting = true
                                errorMsg = null
                                db.collection("tv_sessions").document(codeInput).get()
                                    .addOnSuccessListener { doc ->
                                        isConnecting = false
                                        if (doc.exists()) {
                                            connectedCode = codeInput
                                            vibrate(context, isStrong = true)
                                        } else {
                                            errorMsg = "Invalid code. Check the TV screen."
                                        }
                                    }
                                    .addOnFailureListener {
                                        isConnecting = false
                                        errorMsg = "Network error. Try again."
                                    }
                            } else {
                                errorMsg = "Please enter 6 digits"
                            }
                        },
                        enabled = !isConnecting,
                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(50), spotColor = theme.primary, ambientColor = theme.primary),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (isConnecting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        else Text("Connect", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (tvStatus == "playing") Color(0xFF10B981) else Color.Yellow))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (tvStatus == "playing") "Playing on TV" else "Connected to TV",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (tvTitle.isNotEmpty()) {
                                Text(tvTitle, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { connectedCode = null; codeInput = ""; tvStatus = "idle"; tvTitle = "" }) {
                            Icon(Icons.Default.Close, "Disconnect", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Navigate", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteTabButton(icon = Icons.Default.Home, label = "Home", onClick = { sendCommand("tab", "0") }, theme = theme, modifier = Modifier.weight(1f))
                        RemoteTabButton(icon = Icons.Default.Movie, label = "Movies", onClick = { sendCommand("tab", "1") }, theme = theme, modifier = Modifier.weight(1f))
                        RemoteTabButton(icon = Icons.Default.VideoLibrary, label = "Series", onClick = { sendCommand("tab", "2") }, theme = theme, modifier = Modifier.weight(1f))
                        RemoteTabButton(icon = Icons.Default.Tv, label = "TV Live", onClick = { sendCommand("tab", "3") }, theme = theme, modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("D-Pad", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("dpad_up") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "Up", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("dpad_left") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, "Left", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Box(
                                modifier = Modifier.size(70.dp).clip(CircleShape).background(theme.primary).clickable { sendCommand("dpad_center") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OK", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Box(
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("dpad_right") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, "Right", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("dpad_down") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Down", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("Media & System", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("back") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("toggle_play") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("volume_up") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VolumeUp, "Volume Up", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { sendCommand("volume_down") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VolumeDown, "Volume Down", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("Search on TV", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("Type movie name...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = theme.primary,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { sendCommand("search", searchInput); searchInput = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(56.dp).shadow(8.dp, RoundedCornerShape(50), spotColor = theme.primary)
                        ) {
                            Icon(Icons.Default.Search, "Search", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun RemoteTabButton(icon: ImageVector, label: String, onClick: () -> Unit, theme: ThemeConfig, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = theme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(movie: Movie, userVipExpiry: Long, onShowVipModal: () -> Unit, onPlay: (Movie) -> Unit, onBack: () -> Unit) {
    val isLocked = movie.isVip && userVipExpiry < System.currentTimeMillis()
    val isVipUser = userVipExpiry > System.currentTimeMillis()
    val isSeries = movie.episodes.isNotEmpty()
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    val context = LocalContext.current

    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrlToUse by remember { mutableStateOf("") }
    var downloadTitleToUse by remember { mutableStateOf("") }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Download Options", color = Color.White) },
            text = { Text("Where would you like to save this video?", color = Color.Gray) },
            confirmButton = {
                Button(onClick = { downloadFile(context, downloadUrlToUse, "$downloadTitleToUse.mp4", saveToPublic = true); showDownloadDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), shape = RoundedCornerShape(50)) { Text("Direct to Phone") }
            },
            dismissButton = {
                Button(onClick = { downloadFile(context, downloadUrlToUse, "$downloadTitleToUse.mp4", saveToPublic = false); showDownloadDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(50)) { Text("In-App Download") }
            },
            containerColor = Color(0xFF15171E)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10))) {
        SubcomposeAsyncImage(model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(40.dp), loading = { Box(modifier = Modifier.fillMaxSize().background(Color.Black)) }, error = { Box(modifier = Modifier.fillMaxSize().background(Color.Black)) })
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.95f)))))

        LazyColumn(
            modifier = Modifier.fillMaxSize().displayCutoutPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Details", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(), 
                    contentDescription = movie.title, 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier
                        .width(220.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black), 
                    loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) }, 
                    error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) } }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(movie.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${movie.videoTag} • ${movie.year} • ${movie.category}", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    if (movie.isAdult) { 
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).border(1.dp, Color.Red, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text("🔞 18+ Restricted", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold) 
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                if (!isSeries) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { vibrate(context, true); onPlay(movie) }, 
                            modifier = Modifier.weight(1f).height(56.dp).shadow(12.dp, RoundedCornerShape(50), spotColor = Color.White, ambientColor = Color.White), 
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White), 
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(if (isLocked) "Unlock VIP" else "Play Now", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { 
                                if (isVipUser) {
                                    downloadUrlToUse = movie.videoUrl
                                    downloadTitleToUse = movie.title
                                    showDownloadDialog = true 
                                } else {
                                    onShowVipModal()
                                }
                            }, 
                            modifier = Modifier.height(56.dp).width(64.dp).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)), 
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)), 
                            shape = RoundedCornerShape(50), 
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.padding(top = 8.dp))
                                Box(modifier = Modifier.padding(4.dp).background(Color(0xFFE50914), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                                    Text("PRO", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (isSeries) {
                item {
                    Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp))
                }
                items(movie.episodes) { ep ->
                    val epLabel = "${ep.season} • ${ep.episode}"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                            .clickable { 
                                vibrate(context)
                                if (isLocked) onPlay(movie) 
                                else { 
                                    selectedEpisode = ep
                                    onPlay(Movie(title = "$epLabel - ${movie.title}", videoUrl = ep.url, subtitleUrl = ep.subtitleUrl, isAdult = movie.isAdult, yocId = movie.yocId)) 
                                } 
                            },
                        colors = CardDefaults.cardColors(containerColor = if (selectedEpisode == ep) Color.White else Color(0xFF15171E).copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, if (selectedEpisode == ep) Color.White else Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(epLabel, color = if (selectedEpisode == ep) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, "Play", tint = if (selectedEpisode == ep) Color.Black else Color.White)
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(onClick = { 
                                    if (isVipUser) {
                                        downloadUrlToUse = ep.url
                                        downloadTitleToUse = "${movie.title} $epLabel"
                                        showDownloadDialog = true
                                    } else {
                                        onShowVipModal()
                                    }
                                }) {
                                    Icon(Icons.Default.Download, "Download", tint = if (selectedEpisode == ep) Color.Black else Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    theme: ThemeConfig, 
    profileName: String, 
    userVipExpiry: Long, 
    kidsMode: Boolean, 
    searchQuery: String,
    onDetails: (Movie) -> Unit,
    isMoviesTab: Boolean = false 
) {
    val db = FirebaseFirestore.getInstance()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var featuredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("All") }

    var lastVisibleMovie by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    var isFetchingMore by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    fun loadMoreMovies() {
        if (isFetchingMore || lastVisibleMovie == null) return
        isFetchingMore = true
        var query = db.collection("homeland_movies").orderBy("timestamp", Query.Direction.DESCENDING).limit(100)
        if (lastVisibleMovie != null) query = query.startAfter(lastVisibleMovie)
        query.get().addOnSuccessListener { snap ->
            if (snap != null && !snap.isEmpty) {
                val newMovies = snap.documents.map { doc -> 
                    val episodes = (doc.get("episodes") as? List<Map<String, Any>>)?.map { ep ->
                        Episode(
                            season = ep["season"] as? String ?: "",
                            episode = ep["episode"] as? String ?: "",
                            url = ep["url"] as? String ?: "",
                            subtitleUrl = ep["subtitleUrl"] as? String ?: ""
                        )
                    } ?: emptyList()

                    Movie(
                        id = doc.id, 
                        title = doc.getString("title") ?: "", 
                        poster = doc.getString("poster") ?: "", 
                        videoUrl = doc.getString("videoUrl") ?: "", 
                        category = doc.getString("category") ?: "", 
                        videoTag = doc.getString("videoTag") ?: "", 
                        year = doc.getString("year") ?: "", 
                        isVip = doc.getBoolean("isVip") ?: false, 
                        isFeatured = doc.getBoolean("isFeatured") ?: false, 
                        isAdult = doc.getBoolean("isAdult") ?: false,
                        episodes = episodes,
                        yocId = doc.getString("yocId") ?: ""
                    ) 
                }
                movies = (movies + newMovies).distinctBy { it.id }
                lastVisibleMovie = snap.documents.lastOrNull()
            }
            isFetchingMore = false
        }.addOnFailureListener { isFetchingMore = false }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.distinctUntilChanged().collect { index -> if (index != null && index >= movies.size - 4) loadMoreMovies() }
    }

    LaunchedEffect(Unit) {
        db.collection("homeland_movies").orderBy("timestamp", Query.Direction.DESCENDING).limit(100).get().addOnSuccessListener { snap ->
            if (snap != null) {
                movies = snap.documents.map { doc -> 
                    val episodes = (doc.get("episodes") as? List<Map<String, Any>>)?.map { ep ->
                        Episode(
                            season = ep["season"] as? String ?: "",
                            episode = ep["episode"] as? String ?: "",
                            url = ep["url"] as? String ?: "",
                            subtitleUrl = ep["subtitleUrl"] as? String ?: ""
                        )
                    } ?: emptyList()

                    Movie(
                        id = doc.id, 
                        title = doc.getString("title") ?: "", 
                        poster = doc.getString("poster") ?: "", 
                        videoUrl = doc.getString("videoUrl") ?: "", 
                        category = doc.getString("category") ?: "", 
                        videoTag = doc.getString("videoTag") ?: "", 
                        year = doc.getString("year") ?: "", 
                        isVip = doc.getBoolean("isVip") ?: false, 
                        isFeatured = doc.getBoolean("isFeatured") ?: false, 
                        isAdult = doc.getBoolean("isAdult") ?: false,
                        episodes = episodes,
                        yocId = doc.getString("yocId") ?: ""
                    ) 
                }
                lastVisibleMovie = snap.documents.lastOrNull()
                featuredMovies = movies.filter { !kidsMode || !it.isAdult }.filter { it.isFeatured }.take(5)
                if (featuredMovies.isEmpty()) featuredMovies = movies.filter { !kidsMode || !it.isAdult }.take(3)
                isLoading = false
            }
        }
    }

    if (isLoading && movies.isEmpty()) { 
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.primary) } 
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (featuredMovies.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == "All") {
                val pagerState = rememberPagerState(pageCount = { featuredMovies.size })
                Box(modifier = Modifier.fillMaxWidth().height(260.dp).padding(bottom = 16.dp)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val movie = featuredMovies[page]
                        val trailerExoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(movie.videoUrl)); prepare(); playWhenReady = false; volume = 0f; repeatMode = Player.REPEAT_MODE_ONE } }
                        LaunchedEffect(movie.id) { delay(3000); trailerExoPlayer.playWhenReady = true }
                        DisposableEffect(movie.id) { onDispose { trailerExoPlayer.release() } }

                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp))) {
                            SubcomposeAsyncImage(model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) }, error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E))) })
                            AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = trailerExoPlayer; useController = false; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } }, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black))))
                            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
                            
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE50914).copy(alpha = 0.8f))
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) { 
                                        Text("FEATURED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) 
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(movie.category.uppercase(), color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(movie.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = Color(0xFFFCDC04), modifier = Modifier.size(14.dp)); Text(" 4.0  •  ${movie.year}  •  ${movie.videoTag}", color = Color.LightGray, fontSize = 12.sp) }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row {
                                    Button(onClick = { vibrate(context, true); onDetails(movie) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(50), modifier = Modifier.padding(end = 8.dp)) { Icon(Icons.Default.PlayArrow, null, tint = Color.Black); Text("Play Now", color = Color.Black, fontWeight = FontWeight.Bold) }
                                    OutlinedButton(onClick = { onDetails(movie) }, shape = RoundedCornerShape(50), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))) { Icon(Icons.Default.Info, null); Text("Details") }
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(featuredMovies.size) { iteration -> Box(modifier = Modifier.size(if (pagerState.currentPage == iteration) 16.dp else 6.dp, 6.dp).clip(CircleShape).background(if (pagerState.currentPage == iteration) theme.primary else Color.DarkGray)) }
                    }
                }
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(movieCategories) { category -> 
                    FilterChip(
                        selected = selectedCategory == category, 
                        onClick = { vibrate(context); selectedCategory = category }, 
                        label = { Text(category) }, 
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primary, 
                            selectedLabelColor = Color.White, 
                            containerColor = Color(0xFF15171E).copy(alpha = 0.6f), 
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = Color.Transparent
                        )
                    ) 
                }
            }

            LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(110.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val filtered = movies.filter { 
                    (!kidsMode || !it.isAdult) && 
                    it.title.contains(searchQuery, true) && 
                    (selectedCategory == "All" || it.category == selectedCategory) &&
                    (if (isMoviesTab) it.type != "TV Show" else true)
                }
                items(filtered) { movie -> MovieCard(movie, theme, profileName, userVipExpiry, onDetails) }
                if (isFetchingMore) { item(span = { GridItemSpan(maxLineSpan) }) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(24.dp)) } } }
            }
        }
    }
}

@Composable
fun MoviesScreen(theme: ThemeConfig, profileName: String, userVipExpiry: Long, kidsMode: Boolean, searchQuery: String, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("homeland_movies").addSnapshotListener { snap, _ ->
            if (snap != null) {
                movies = snap.documents.mapNotNull { doc -> 
                    val type = doc.getString("type") ?: ""
                    if (type == "TV Show") return@mapNotNull null // Only get movies
                    
                    val episodes = (doc.get("episodes") as? List<Map<String, Any>>)?.map { ep ->
                        Episode(
                            season = ep["season"] as? String ?: "",
                            episode = ep["episode"] as? String ?: "",
                            url = ep["url"] as? String ?: "",
                            subtitleUrl = ep["subtitleUrl"] as? String ?: ""
                        )
                    } ?: emptyList()

                    Movie(
                        id = doc.id, 
                        title = doc.getString("title") ?: "", 
                        poster = doc.getString("poster") ?: "", 
                        videoUrl = doc.getString("videoUrl") ?: "", 
                        videoTag = doc.getString("videoTag") ?: "", 
                        year = doc.getString("year") ?: "", 
                        isVip = doc.getBoolean("isVip") ?: false, 
                        isAdult = doc.getBoolean("isAdult") ?: false,
                        episodes = episodes,
                        yocId = doc.getString("yocId") ?: ""
                    ) 
                }.filter { (!kidsMode || !it.isAdult) && it.title.contains(searchQuery, true) }
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(theme.primary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Text("All Movies", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        if (isLoading && movies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.primary) }
        } else if (movies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No movies found.", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(movies) { movie -> MovieCard(movie, theme, profileName, userVipExpiry, onDetails) }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, theme: ThemeConfig, profileName: String, userVipExpiry: Long, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var isFav by remember { mutableStateOf(false) }
    val isLocked = movie.isVip && userVipExpiry < System.currentTimeMillis()
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uid, profileName) {
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                val favMap = doc.get("favorites") as? Map<*, *>
                val profileFavs = (favMap?.get(profileName) as? List<*>)?.map { it.toString() } ?: emptyList()
                isFav = profileFavs.contains(movie.id)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF15171E).copy(alpha = 0.8f))
            .onFocusChanged { isFocused = it.isFocused }
            .border(1.dp, if (isFocused) Color.White else Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { vibrate(context); onDetails(movie) }
    ) {
        SubcomposeAsyncImage(model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(), contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) }, error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) } })
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f), Color.Black))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(text = movie.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = "${movie.videoTag} • ${movie.year}", color = if (movie.isVip) Color(0xFFFCDC04) else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isLocked) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, "Locked", tint = Color(0xFFFCDC04), modifier = Modifier.size(32.dp)) } }
        if (movie.isAdult) { Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).background(Color.Red, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text("18+", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) } }
        IconButton(onClick = { if (uid != null) { vibrate(context, true); val dbRef = db.collection("users").document(uid); if (isFav) dbRef.update("favorites.$profileName", FieldValue.arrayRemove(movie.id)) else dbRef.update("favorites.$profileName", FieldValue.arrayUnion(movie.id)); isFav = !isFav } }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            Icon(imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Fav", tint = if (isFav) theme.primary else Color.White)
        }
    }
}

@Composable
fun SeriesScreen(theme: ThemeConfig, profileName: String, userVipExpiry: Long, kidsMode: Boolean, searchQuery: String, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var series by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("homeland_movies").whereEqualTo("type", "TV Show").addSnapshotListener { snap, _ ->
            if (snap != null) {
                series = snap.documents.map { doc -> 
                    val episodes = (doc.get("episodes") as? List<Map<String, Any>>)?.map { ep ->
                        Episode(
                            season = ep["season"] as? String ?: "",
                            episode = ep["episode"] as? String ?: "",
                            url = ep["url"] as? String ?: "",
                            subtitleUrl = ep["subtitleUrl"] as? String ?: ""
                        )
                    } ?: emptyList()

                    Movie(
                        id = doc.id, 
                        title = doc.getString("title") ?: "", 
                        poster = doc.getString("poster") ?: "", 
                        videoUrl = doc.getString("videoUrl") ?: "", 
                        videoTag = doc.getString("videoTag") ?: "", 
                        year = doc.getString("year") ?: "", 
                        isVip = doc.getBoolean("isVip") ?: false, 
                        isAdult = doc.getBoolean("isAdult") ?: false,
                        episodes = episodes,
                        yocId = doc.getString("yocId") ?: ""
                    ) 
                }.filter { (!kidsMode || !it.isAdult) && it.title.contains(searchQuery, true) }
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(theme.primary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.VideoLibrary, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Text("TV Shows & Series", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        if (isLoading && series.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.primary) }
        } else if (series.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideoLibrary, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No series found.", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(series) { movie -> MovieCard(movie, theme, profileName, userVipExpiry, onDetails) }
            }
        }
    }
}

@Composable
fun TvLiveScreen(theme: ThemeConfig, userVipExpiry: Long, kidsMode: Boolean, searchQuery: String, onPlayMedia: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val listener = db.collection("homeland_tv").orderBy("name", Query.Direction.ASCENDING).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                channels = snapshot.documents.mapNotNull { doc ->
                    TvChannel(id = doc.id, name = doc.getString("name") ?: "", logo = doc.getString("logo") ?: "", streamUrl = doc.getString("streamUrl") ?: "", category = doc.getString("category") ?: "", isVip = doc.getBoolean("isVip") ?: false, isFeatured = doc.getBoolean("isFeatured") ?: false)
                }
            }
        }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val filteredChannels = if (kidsMode) channels.filter { it.category == "Kids" || it.category == "Animation" } else channels
        
        // Apply search filter
        val searchedChannels = filteredChannels.filter { it.name.contains(searchQuery, true) }
        
        val featuredChannels = searchedChannels.filter { it.isFeatured }.take(4)
        val displayFeatured = if (featuredChannels.isNotEmpty()) featuredChannels else searchedChannels.take(4)

        if (displayFeatured.isNotEmpty() && searchQuery.isEmpty()) {
            Text("Featured Channels", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(displayFeatured) { channel ->
                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(125.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .clickable { vibrate(context); onPlayMedia(Movie(title = channel.name, videoUrl = channel.streamUrl, isVip = channel.isVip)) }
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(channel.logo).crossfade(true).build(),
                            contentDescription = channel.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) }
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black))))
                        
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(channel.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { 
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold) 
                            }
                        }
                    }
                }
            }
        }

        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tvCategories) { category -> 
                FilterChip(
                    selected = selectedCategory == category, 
                    onClick = { vibrate(context); selectedCategory = category }, 
                    label = { Text(category) }, 
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = theme.primary, 
                        selectedLabelColor = Color.White, 
                        containerColor = Color(0xFF15171E).copy(alpha = 0.6f), 
                        labelColor = Color.Gray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == category,
                        borderColor = Color.White.copy(alpha = 0.1f),
                        selectedBorderColor = Color.Transparent
                    )
                ) 
            }
        }
        
        val filtered = searchedChannels.filter { selectedCategory == "All" || it.category == selectedCategory }
        LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filtered) { channel ->
                val isLocked = channel.isVip && userVipExpiry < System.currentTimeMillis()
                var isFocused by remember { mutableStateOf(false) }
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(if (isFocused) 3.dp else 1.dp, if (isFocused) Color.White else Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                            .background(Color(0xFF15171E).copy(alpha = 0.7f))
                            .clickable { vibrate(context); onPlayMedia(Movie(title = channel.name, videoUrl = channel.streamUrl, isVip = channel.isVip)) }
                            .padding(12.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        verticalArrangement = Arrangement.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(channel.logo).crossfade(true).build(), 
                            contentDescription = channel.name, 
                            contentScale = ContentScale.Fit, 
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) }, 
                            error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Tv, null, tint = Color.Gray) } }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(channel.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "blink")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 1f, 
                                targetValue = 0.2f, 
                                animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), 
                                label = "alpha"
                            )
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red.copy(alpha = alpha)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", color = Color.Red.copy(alpha = alpha), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isLocked) { Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, "Locked", tint = Color(0xFFFCDC04), modifier = Modifier.size(32.dp)) } }
                }
            }
        }
    }
}
