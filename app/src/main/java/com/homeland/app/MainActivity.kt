package com.homeland.app

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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.distinctUntilChanged

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val db = FirebaseFirestore.getInstance()
            var appTheme by remember { mutableStateOf(ThemeConfig()) }
            
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

                val context = LocalContext.current
                val isTv = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || 
                           (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

                if (!isAppUnlocked) {
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
                            HomelandApp(auth = auth, profileName = currentProfileName, theme = appTheme, savedPin = savedPin, onLogout = { isAuthenticated = false }, onSwitchProfile = { })
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
                                HomelandApp(auth = auth, profileName = currentProfileName, theme = appTheme, savedPin = savedPin, onLogout = { isAuthenticated = false; showProfileSelector = false }, onSwitchProfile = { showProfileSelector = true })
                            }
                        } else {
                            AuthScreen(auth = auth, onLoginSuccess = { isAuthenticated = true; showProfileSelector = true })
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun AppLockScreen(theme: ThemeConfig, savedPin: String, onUnlock: () -> Unit, onForgotPin: () -> Unit) {
    var inputPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = theme.primary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("App Locked", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Enter your 4-digit PIN", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (i < inputPin.length) theme.primary else Color.DarkGray)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (errorMsg != null) {
                Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
            }
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
                                    .background(if (key == "DEL") Color.DarkGray else Color(0xFF15171E))
                                    .clickable {
                                        if (key == "DEL") {
                                            if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                                        } else {
                                            if (inputPin.length < 4) inputPin += key
                                        }
                                        errorMsg = null

                                        if (inputPin.length == 4) {
                                            if (inputPin == savedPin) {
                                                onUnlock()
                                            } else {
                                                errorMsg = "Incorrect PIN"
                                                inputPin = ""
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = Color.White)
                                else Text(key, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(70.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onForgotPin) {
                Text("Forgot PIN? (Logs you out)", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomelandApp(auth: FirebaseAuth, profileName: String, theme: ThemeConfig, savedPin: String?, onLogout: () -> Unit, onSwitchProfile: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var selectedTab by remember { mutableStateOf(0) }
    var currentlyPlaying by remember { mutableStateOf<Movie?>(null) }
    var viewingDetails by remember { mutableStateOf<Movie?>(null) }
    var userVipExpiry by remember { mutableLongStateOf(0L) }
    var showVipModal by remember { mutableStateOf(false) }

    var pendingAdultMovie by remember { mutableStateOf<Movie?>(null) }
    var showAdultPinPrompt by remember { mutableStateOf(false) }
    var adultPinInput by remember { mutableStateOf("") }

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                userVipExpiry = (doc?.getLong("vipExpiry")) ?: 0L
            }
        }
    }

    LaunchedEffect(Unit) { FirebaseMessaging.getInstance().subscribeToTopic("homeland_updates") }

    BackHandler(enabled = true) {
        if (currentlyPlaying != null) {
            currentlyPlaying = null 
        } else if (viewingDetails != null) {
            viewingDetails = null 
        } else if (showVipModal) {
            showVipModal = false 
        } else if (showAdultPinPrompt) {
            showAdultPinPrompt = false
            pendingAdultMovie = null
        } else if (selectedTab != 0) {
            selectedTab = 0 
        } else {
            (context as? androidx.activity.ComponentActivity)?.finish()
        }
    }

    if (showAdultPinPrompt) {
        AlertDialog(
            onDismissRequest = { showAdultPinPrompt = false; pendingAdultMovie = null },
            title = { Text("18+ Content Locked", color = Color.White) },
            text = { 
                Column {
                    Text("Enter your App PIN to watch this movie.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adultPinInput,
                        onValueChange = { if (it.length <= 4) adultPinInput = it },
                        placeholder = { Text("4-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adultPinInput == savedPin) {
                            currentlyPlaying = pendingAdultMovie
                            showAdultPinPrompt = false
                            pendingAdultMovie = null
                            adultPinInput = ""
                        } else {
                            Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                            adultPinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { showAdultPinPrompt = false; pendingAdultMovie = null; adultPinInput = "" }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF15171E)
        )
    }

    if (currentlyPlaying != null) {
        val movie = currentlyPlaying!!
        PlayerScreen(
            title = movie.title, videoUrl = movie.videoUrl, episodes = movie.episodes, startPosition = 0L, subtitleUrl = movie.subtitleUrl.ifBlank { null },
            onClose = { currentlyPlaying = null }
        )
        return 
    }

    if (viewingDetails != null) {
        MovieDetailsScreen(
            movie = viewingDetails!!, userVipExpiry = userVipExpiry,
            onPlay = { movieToPlay -> 
                if (movieToPlay.isVip && userVipExpiry < System.currentTimeMillis()) {
                    showVipModal = true
                } else if (movieToPlay.isAdult && savedPin != null) {
                    pendingAdultMovie = movieToPlay
                    showAdultPinPrompt = true
                } else {
                    currentlyPlaying = movieToPlay; viewingDetails = null 
                }
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
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP_NUMBER?text=Hi, I want to subscribe to Homeland VIP."))
                    context.startActivity(intent)
                }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) { Text("Subscribe via WhatsApp") }
            },
            dismissButton = { TextButton(onClick = { showVipModal = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    val isTv = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || 
               (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    if (isTv) {
        Row(modifier = Modifier.fillMaxSize().background(theme.background)) {
            NavigationRail(
                containerColor = Color(0xFF15171E),
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                NavigationRailItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
                NavigationRailItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Tv, "TV") }, label = { Text("TV Live") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
                NavigationRailItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Favorite, "My List") }, label = { Text("List") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
                NavigationRailItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") }, colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (theme.bgImageUrl.isNotEmpty()) {
                    AsyncImage(model = theme.bgImageUrl, contentDescription = "App Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(theme.background))
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                        1 -> TvLiveScreen(theme = theme, userVipExpiry = userVipExpiry, onPlayMedia = { currentlyPlaying = it })
                        2 -> MyListScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                        3 -> ProfileScreen(auth, profileName = profileName, userVipExpiry, theme, savedPin = savedPin, onLogout, onSwitchProfile)
                    }
                }
            }
        }
    } else {
        Scaffold(
            // 🔥 OPTION 2: GLASSMORPHISM BOTTOM BAR
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0C10).copy(alpha = 0.94f))
                        .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    NavigationBar(containerColor = Color.Transparent) {
                        NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.2f)))
                        NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Tv, "TV Live") }, label = { Text("TV Live") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.2f)))
                        NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Favorite, "My List") }, label = { Text("My List") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.2f)))
                        NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = theme.primary.copy(alpha = 0.2f)))
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (theme.bgImageUrl.isNotEmpty()) {
                    AsyncImage(model = theme.bgImageUrl, contentDescription = "App Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(theme.background))
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                        1 -> TvLiveScreen(theme = theme, userVipExpiry = userVipExpiry, onPlayMedia = { currentlyPlaying = it })
                        2 -> MyListScreen(theme = theme, profileName = profileName, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                        3 -> ProfileScreen(auth, profileName = profileName, userVipExpiry, theme, savedPin = savedPin, onLogout, onSwitchProfile)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSelectorScreen(theme: ThemeConfig, onProfileSelected: (String) -> Unit) {
    var profiles by remember { mutableStateOf(listOf("Me", "Kids", "Guest")) }
    
    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        if (theme.bgImageUrl.isNotEmpty()) {
            AsyncImage(
                model = theme.bgImageUrl,
                contentDescription = "App Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Who's Watching?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
            profiles.forEach { profile ->
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onProfileSelected(profile) }
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .fillMaxWidth(0.6f), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(theme.primary), contentAlignment = Alignment.Center) {
                        Text(profile.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(profile, color = Color.White, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { profiles = profiles + "New Profile" }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) { Text("Add Profile") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(auth: FirebaseAuth, profileName: String, userVipExpiry: Long, theme: ThemeConfig, savedPin: String?, onLogout: () -> Unit, onSwitchProfile: () -> Unit) {
    val context = LocalContext.current
    var showAdminPanel by remember { mutableStateOf(false) }
    var couponCode by remember { mutableStateOf("") }
    val isVip = userVipExpiry > System.currentTimeMillis()

    var showTvDialog by remember { mutableStateOf(false) }
    var tvCodeInput by remember { mutableStateOf("") }

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("homeland_prefs", Context.MODE_PRIVATE) }

    if (showAdminPanel) {
        AdminScreen(onBack = { showAdminPanel = false })
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF15171E)).clickable { Toast.makeText(context, "Tap to change avatar", Toast.LENGTH_SHORT).show() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, "Avatar", tint = theme.primary, modifier = Modifier.size(60.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("MY PROFILE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Watching as: $profileName", color = theme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Logged in as: ${auth.currentUser?.email ?: "Unknown"}", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${if (isVip) "VIP Active 👑" else "Free User"}", color = if (isVip) Color(0xFFFCDC04) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onSwitchProfile, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Icon(Icons.Default.SwitchAccount, null); Spacer(modifier = Modifier.width(8.dp)); Text("Switch Profile")
            }

            Button(
                onClick = { showPinDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = if (savedPin != null) Color(0xFF25D366) else Color.DarkGray),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Icon(if (savedPin != null) Icons.Default.Lock else Icons.Default.LockOpen, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (savedPin != null) "Remove App Lock" else "Setup App Lock (PIN)")
            }

            Button(onClick = { showTvDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Icon(Icons.Default.Tv, null); Spacer(modifier = Modifier.width(8.dp)); Text("Login on TV")
            }

            if (!isVip) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = couponCode, onValueChange = { couponCode = it }, placeholder = { Text("Coupon Code") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val db = FirebaseFirestore.getInstance()
                        db.collection("homeland_coupons").whereEqualTo("code", couponCode.trim()).whereEqualTo("isUsed", false).get().addOnSuccessListener { snap ->
                            if (!snap.isEmpty) {
                                val doc = snap.documents[0]
                                val days = doc.getLong("days") ?: 7L
                                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                                db.collection("users").document(uid).update("vipExpiry", System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L))
                                db.collection("homeland_coupons").document(doc.id).update("isUsed", true)
                                Toast.makeText(context, "Success! You are now VIP.", Toast.LENGTH_LONG).show()
                                couponCode = ""
                            } else {
                                Toast.makeText(context, "Invalid or used code.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary)) { Text("Redeem") }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP_NUMBER?text=Hi, I want to subscribe to Homeland VIP."))
                    context.startActivity(intent)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Icon(Icons.Default.Whatsapp, null); Spacer(modifier = Modifier.width(8.dp)); Text("Subscribe via WhatsApp")
                }
            }

            if (auth.currentUser?.email == ADMIN_EMAIL) {
                Button(onClick = { showAdminPanel = true }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text("Open Admin Panel") }
            }
            Button(onClick = { auth.signOut(); onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth()) { Text("Log Out") }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (savedPin != null) "Remove App Lock" else "Setup App Lock", color = Color.White) },
            text = {
                if (savedPin != null) {
                    Text("Are you sure you want to remove the PIN? Anyone can open the app.", color = Color.Gray)
                } else {
                    Column {
                        Text("Enter a 4-digit PIN. You will need this to open the app and watch 18+ content.", color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 4) newPinInput = it },
                            placeholder = { Text("Enter 4-digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (savedPin != null) {
                            prefs.edit().remove("app_pin").apply()
                            Toast.makeText(context, "App Lock Removed", Toast.LENGTH_SHORT).show()
                            showPinDialog = false
                            onSwitchProfile() 
                        } else {
                            if (newPinInput.length == 4) {
                                prefs.edit().putString("app_pin", newPinInput).apply()
                                Toast.makeText(context, "App Lock Enabled!", Toast.LENGTH_SHORT).show()
                                showPinDialog = false
                                newPinInput = ""
                                onSwitchProfile() 
                            } else {
                                Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (savedPin != null) Color.Red else Color(0xFF25D366))
                ) { Text(if (savedPin != null) "Remove" else "Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; newPinInput = "" }) { Text("Cancel", color = Color.Gray) }
            },
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
                    OutlinedTextField(
                        value = tvCodeInput, 
                        onValueChange = { tvCodeInput = it }, 
                        placeholder = { Text("e.g. 603061") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput, 
                        onValueChange = { passwordInput = it }, 
                        placeholder = { Text("Your Account Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
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
                                    db.collection("tv_login_codes").document(tvCodeInput).update(
                                        "status", "approved",
                                        "email", userEmail,
                                        "password", passwordInput 
                                    ).addOnSuccessListener {
                                        Toast.makeText(context, "TV Authorized successfully!", Toast.LENGTH_SHORT).show()
                                        showTvDialog = false
                                        tvCodeInput = ""
                                        passwordInput = ""
                                    }
                                } else {
                                    Toast.makeText(context, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please enter 6 digits and your password", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                    Text("Authorize")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTvDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF15171E)
        )
    }
}

// 🔥 OPTION 1: NETFLIX CINEMATIC MOVIE DETAILS SCREEN
@Composable
fun MovieDetailsScreen(movie: Movie, userVipExpiry: Long, onPlay: (Movie) -> Unit, onBack: () -> Unit) {
    val isLocked = movie.isVip && userVipExpiry < System.currentTimeMillis()
    val isSeries = movie.episodes.isNotEmpty()
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10))) {
        // Blurred Background Poster
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(40.dp),
            loading = { Box(modifier = Modifier.fillMaxSize().background(Color.Black)) },
            error = { Box(modifier = Modifier.fillMaxSize().background(Color.Black)) }
        )
        // Deep Dark Overlay
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.95f)))))

        // Foreground Content
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)) { 
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) 
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Details", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Center Poster
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(),
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(200.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                    error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) } }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(movie.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${movie.videoTag} • ${movie.year} • ${movie.category}", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                
                if (movie.isAdult) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🔞 18+ Restricted", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!isSeries) {
                    Button(
                        onClick = { onPlay(movie) }, 
                        modifier = Modifier.fillMaxWidth().height(56.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(28.dp)); 
                        Spacer(modifier = Modifier.width(8.dp)); 
                        Text(if (isLocked) "Unlock with VIP" else "Play Now", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isSeries) {
                Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(movie.episodes) { ep ->
                        val epLabel = "${ep.season} • ${ep.episode}"
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                .background(if (selectedEpisode == ep) Color.White else Color(0xFF15171E), RoundedCornerShape(12.dp))
                                .clickable { 
                                    if (isLocked) onPlay(movie)
                                    else { selectedEpisode = ep; onPlay(Movie(title = "$epLabel - ${movie.title}", videoUrl = ep.url, subtitleUrl = ep.subtitleUrl, isAdult = movie.isAdult)) }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(epLabel, color = if (selectedEpisode == ep) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.PlayArrow, "Play", tint = if (selectedEpisode == ep) Color.Black else Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(theme: ThemeConfig, profileName: String, userVipExpiry: Long, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var featuredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var historyMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var lastVisibleMovie by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    var isFetchingMore by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    fun loadMoreMovies() {
        if (isFetchingMore || lastVisibleMovie == null) return
        isFetchingMore = true
        var query = db.collection("homeland_movies").orderBy("timestamp", Query.Direction.DESCENDING).limit(20)
        if (lastVisibleMovie != null) {
            query = query.startAfter(lastVisibleMovie)
        }
        query.get().addOnSuccessListener { snap ->
            if (snap != null && !snap.isEmpty) {
                val newMovies = snap.documents.map { doc -> 
                    Movie(id = doc.id, title = doc.getString("title") ?: "", poster = doc.getString("poster") ?: "", videoUrl = doc.getString("videoUrl") ?: "", category = doc.getString("category") ?: "", videoTag = doc.getString("videoTag") ?: "", year = doc.getString("year") ?: "", isVip = doc.getBoolean("isVip") ?: false, isFeatured = doc.getBoolean("isFeatured") ?: false, isAdult = doc.getBoolean("isAdult") ?: false) 
                }
                movies = (movies + newMovies).distinctBy { it.id }
                lastVisibleMovie = snap.documents.lastOrNull()
            }
            isFetchingMore = false
        }.addOnFailureListener {
            isFetchingMore = false
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { index ->
                if (index != null && index >= movies.size - 4) {
                    loadMoreMovies()
                }
            }
    }

    LaunchedEffect(Unit) {
        db.collection("homeland_movies").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).get().addOnSuccessListener { snap ->
            if (snap != null) {
                movies = snap.documents.map { doc -> 
                    Movie(id = doc.id, title = doc.getString("title") ?: "", poster = doc.getString("poster") ?: "", videoUrl = doc.getString("videoUrl") ?: "", category = doc.getString("category") ?: "", videoTag = doc.getString("videoTag") ?: "", year = doc.getString("year") ?: "", isVip = doc.getBoolean("isVip") ?: false, isFeatured = doc.getBoolean("isFeatured") ?: false, isAdult = doc.getBoolean("isAdult") ?: false) 
                }
                lastVisibleMovie = snap.documents.lastOrNull()
                featuredMovies = movies.filter { it.isFeatured }.take(5)
                if (featuredMovies.isEmpty()) featuredMovies = movies.take(3)
                isLoading = false
            }
        }
    }

    LaunchedEffect(movies, profileName) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            db.collection("users").document(user.uid).collection("history")
                .whereEqualTo("profile", profileName)
                .get().addOnSuccessListener { historySnap ->
                val hIds = historySnap.documents.map { it.id }
                val valid = movies.filter { it.id in hIds }
                historyMovies = if (valid.isEmpty()) movies.take(3) else valid
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.primary) }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() 
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).background(theme.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("H", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "HOMELAND", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(text = " TV", color = theme.primary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            OutlinedTextField(
                value = searchQuery, 
                onValueChange = { searchQuery = it }, 
                placeholder = { Text("Search titles...", color = Color.Gray, fontSize = 14.sp) }, 
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) }, 
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp), 
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent, 
                    unfocusedBorderColor = Color.Transparent, 
                    focusedContainerColor = Color(0xFF15171E), 
                    unfocusedContainerColor = Color(0xFF15171E), 
                    focusedTextColor = Color.White, 
                    unfocusedTextColor = Color.White
                ), 
                shape = RoundedCornerShape(12.dp)
            )

            if (featuredMovies.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == "All") {
                val pagerState = rememberPagerState(pageCount = { featuredMovies.size })
                
                Box(modifier = Modifier.fillMaxWidth().height(260.dp).padding(bottom = 16.dp)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val movie = featuredMovies[page]
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))) {
                            
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                                error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E))) }
                            )
                            
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black))))
                            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
                            
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(theme.primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("FEATURED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(movie.category.uppercase(), color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(movie.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFCDC04), modifier = Modifier.size(14.dp))
                                    Text(" 4.0  •  ${movie.year}  •  ${movie.videoTag}", color = Color.LightGray, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row {
                                    Button(onClick = { onDetails(movie) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(end = 8.dp)) { 
                                        Icon(Icons.Default.PlayArrow, null, tint = Color.Black); Text("Play Now", color = Color.Black, fontWeight = FontWeight.Bold) 
                                    }
                                    OutlinedButton(onClick = { onDetails(movie) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { 
                                        Icon(Icons.Default.Info, null); Text("Details") 
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(featuredMovies.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) theme.primary else Color.DarkGray
                            Box(modifier = Modifier.size(if (pagerState.currentPage == iteration) 16.dp else 6.dp, 6.dp).clip(CircleShape).background(color))
                        }
                    }
                }
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(movieCategories) { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.primary, selectedLabelColor = Color.White, containerColor = Color(0xFF15171E), labelColor = Color.Gray)) }
            }

            LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(110.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (historyMovies.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == "All") {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text(if (historyMovies.size == 3) "Top Picks For You" else "Continue Watching", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(historyMovies) { movie -> MovieCard(movie, theme, profileName, userVipExpiry, onDetails) }
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Trending Now", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                val filtered = movies.filter { it.title.contains(searchQuery, true) && (selectedCategory == "All" || it.category == selectedCategory) }
                items(filtered) { movie -> MovieCard(movie, theme, profileName, userVipExpiry, onDetails) }
                
                if (isFetchingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF15171E))
            .onFocusChanged { isFocused = it.isFocused }
            .border(if (isFocused) 3.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
            .clickable { onDetails(movie) }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(movie.poster).crossfade(true).build(),
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
            error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) } }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${movie.videoTag} • ${movie.year}",
                color = if (movie.isVip) Color(0xFFFCDC04) else Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, "Locked", tint = Color(0xFFFCDC04), modifier = Modifier.size(32.dp))
            }
        }

        if (movie.isAdult) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("18+", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }

        IconButton(
            onClick = { 
                if (uid != null) {
                    val dbRef = db.collection("users").document(uid)
                    if (isFav) dbRef.update("favorites.$profileName", FieldValue.arrayRemove(movie.id))
                    else dbRef.update("favorites.$profileName", FieldValue.arrayUnion(movie.id))
                    isFav = !isFav
                }
            }, 
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                contentDescription = "Fav", 
                tint = if (isFav) theme.primary else Color.White
            )
        }
    }
}

@Composable
fun MyListScreen(theme: ThemeConfig, profileName: String, userVipExpiry: Long, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var favMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }

    LaunchedEffect(uid, profileName) {
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                val favMap = doc?.get("favorites") as? Map<*, *>
                val profileFavs = (favMap?.get(profileName) as? List<*>)?.map { it.toString() } ?: emptyList()
                
                db.collection("homeland_movies").get().addOnSuccessListener { snap ->
                    favMovies = snap.documents.mapNotNull { 
                        Movie(id = it.id, title = it.getString("title") ?: "", poster = it.getString("poster") ?: "", videoUrl = it.getString("videoUrl") ?: "", videoTag = it.getString("videoTag") ?: "", year = it.getString("year") ?: "", isVip = it.getBoolean("isVip") ?: false, isAdult = it.getBoolean("isAdult") ?: false) 
                    }.filter { it.id in profileFavs }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()) {
        Text("My List ($profileName)", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        if (favMovies.isEmpty()) Text("Your list is empty. Add favorites from Home.", color = Color.Gray)
        else LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(favMovies) { movie -> MovieCard(movie, theme, profileName, userVipExpiry, onDetails) }
        }
    }
}

// 🔥 OPTION 3: SMART TV LIVE SCREEN
@Composable
fun TvLiveScreen(theme: ThemeConfig, userVipExpiry: Long, onPlayMedia: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }

    DisposableEffect(Unit) {
        val listener = db.collection("homeland_tv").orderBy("name", Query.Direction.ASCENDING).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                channels = snapshot.documents.mapNotNull { doc ->
                    TvChannel(
                        id = doc.id, 
                        name = doc.getString("name") ?: "", 
                        logo = doc.getString("logo") ?: "", 
                        streamUrl = doc.getString("streamUrl") ?: "", 
                        category = doc.getString("category") ?: "", 
                        isVip = doc.getBoolean("isVip") ?: false, 
                        isFeatured = doc.getBoolean("isFeatured") ?: false // 🔥 NEW
                    )
                }
            }
        }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        
        val featuredChannels = channels.filter { it.isFeatured }.take(4)
        val displayFeatured = if (featuredChannels.isNotEmpty()) featuredChannels else channels.take(4)

        // Featured Channels Row
        if (displayFeatured.isNotEmpty()) {
            Text("Featured Channels", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(displayFeatured) { channel ->
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlayMedia(Movie(title = channel.name, videoUrl = channel.streamUrl, isVip = channel.isVip)) }
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(channel.logo).crossfade(true).build(),
                            contentDescription = channel.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) }
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(channel.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                                Text(" LIVE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tvCategories) { category -> 
                FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.primary, selectedLabelColor = Color.White, containerColor = Color(0xFF15171E), labelColor = Color.Gray)) 
            }
        }
        
        val filtered = channels.filter { selectedCategory == "All" || it.category == selectedCategory }
        LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filtered) { channel ->
                val isLocked = channel.isVip && userVipExpiry < System.currentTimeMillis()
                var isFocused by remember { mutableStateOf(false) }
                
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(if (isFocused) 3.dp else 0.dp, Color.White, RoundedCornerShape(16.dp)) 
                            .background(Color(0xFF15171E))
                            .clickable { onPlayMedia(Movie(title = channel.name, videoUrl = channel.streamUrl, isVip = channel.isVip)) }
                            .padding(12.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(channel.logo).crossfade(true).build(),
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                            error = { Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15171E)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Tv, null, tint = Color.Gray) } }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(channel.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "blink")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "alpha"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red.copy(alpha = alpha)))
                            Text(" LIVE", color = Color.Red.copy(alpha = alpha), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isLocked) {
                        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, "Locked", tint = Color(0xFFFCDC04), modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
