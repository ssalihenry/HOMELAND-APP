package com.homeland.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging

const val ADMIN_EMAIL = "ssalihenry136@gmail.com"
const val WHATSAPP_NUMBER = "256742199254" 
val movieCategories = listOf("All", "Action", "Sci-Fi", "Comedy", "Animation", "Romantic", "Horror", "Adventure")
val tvCategories = listOf("All", "News", "Sports", "Movies", "Entertainment", "Music", "Kids")

data class ThemeConfig(val primary: Color = Color(0xFFE50914), val background: Color = Color(0xFF0B0C10), val fontSize: Int = 14, val fontUrl: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        appTheme = ThemeConfig(Color(android.graphics.Color.parseColor(p)), Color(android.graphics.Color.parseColor(b)), f, url)
                    }
                }
            }

            // 🔥 DYNAMIC FONT LOADING
            val dynamicFontFamily = if (appTheme.fontUrl.isNotBlank()) {
                FontFamily(Font(appTheme.fontUrl))
            } else {
                FontFamily.Default
            }

            MaterialTheme(
                typography = Typography(defaultFontFamily = dynamicFontFamily),
                colorScheme = darkColorScheme(primary = appTheme.primary, background = appTheme.background)
            ) {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                var isAuthenticated by remember { mutableStateOf(currentUser != null) }
                var showProfileSelector by remember { mutableStateOf(false) }

                if (isAuthenticated) {
                    // 🔥 MULTI-PROFILE LOGIC
                    if (showProfileSelector) {
                        ProfileSelectorScreen(theme = appTheme, onProfileSelected = { showProfileSelector = false })
                    } else {
                        HomelandApp(auth = auth, theme = appTheme, onLogout = { isAuthenticated = false; showProfileSelector = false }, onSwitchProfile = { showProfileSelector = true })
                    }
                } else {
                    AuthScreen(auth = auth, onLoginSuccess = { isAuthenticated = true; showProfileSelector = true })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomelandApp(auth: FirebaseAuth, theme: ThemeConfig, onLogout: () -> Unit, onSwitchProfile: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var selectedTab by remember { mutableStateOf(0) }
    var currentlyPlaying by remember { mutableStateOf<Movie?>(null) }
    var viewingDetails by remember { mutableStateOf<Movie?>(null) }
    var userVipExpiry by remember { mutableLongStateOf(0L) }
    var showVipModal by remember { mutableStateOf(false) }

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                userVipExpiry = (doc?.getLong("vipExpiry")) ?: 0L
            }
        }
    }

    LaunchedEffect(Unit) { FirebaseMessaging.getInstance().subscribeToTopic("homeland_updates") }

    BackHandler(enabled = currentlyPlaying != null || viewingDetails != null || selectedTab != 0) {
        if (currentlyPlaying != null) currentlyPlaying = null
        else if (viewingDetails != null) viewingDetails = null
        else selectedTab = 0
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
            onPlay = { 
                if (it.isVip && userVipExpiry < System.currentTimeMillis()) showVipModal = true
                else { currentlyPlaying = it; viewingDetails = null }
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

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF15171E)) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Tv, "TV Live") }, label = { Text("TV Live") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Favorite, "My List") }, label = { Text("My List") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.primary, selectedTextColor = theme.primary, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(theme.background)) {
            when (selectedTab) {
                0 -> HomeScreen(theme = theme, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                1 -> TvLiveScreen(theme = theme, userVipExpiry = userVipExpiry, onPlayMedia = { currentlyPlaying = it })
                2 -> MyListScreen(theme = theme, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                3 -> ProfileScreen(auth, userVipExpiry, theme, onLogout, onSwitchProfile)
            }
        }
    }
}

// 🔥 MULTI-PROFILE SELECTOR
@Composable
fun ProfileSelectorScreen(theme: ThemeConfig, onProfileSelected: () -> Unit) {
    var profiles by remember { mutableStateOf(listOf("Me", "Kids", "Guest")) }
    var showAddProfile by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(theme.background).systemBarsPadding(),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Who's Watching?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        
        profiles.forEach { profile ->
            Row(modifier = Modifier.padding(8.dp).clickable { onProfileSelected() }.background(Color.DarkGray, RoundedCornerShape(8.dp)).padding(16.dp).fillMaxWidth(0.6f), verticalAlignment = Alignment.CenterVertically) {
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

// 🔥 PROFESSIONAL PROFILE SCREEN
@Composable
fun ProfileScreen(auth: FirebaseAuth, userVipExpiry: Long, theme: ThemeConfig, onLogout: () -> Unit, onSwitchProfile: () -> Unit) {
    val context = LocalContext.current
    var showAdminPanel by remember { mutableStateOf(false) }
    var couponCode by remember { mutableStateOf("") }
    val isVip = userVipExpiry > System.currentTimeMillis()

    if (showAdminPanel) {
        AdminScreen(onBack = { showAdminPanel = false })
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Profile Avatar
            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF15171E)).clickable { Toast.makeText(context, "Tap to change avatar", Toast.LENGTH_SHORT).show() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, "Avatar", tint = theme.primary, modifier = Modifier.size(60.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("MY PROFILE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Logged in as: ${auth.currentUser?.email ?: "Unknown"}", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${if (isVip) "VIP Active 👑" else "Free User"}", color = if (isVip) Color(0xFFFCDC04) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = onSwitchProfile, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Icon(Icons.Default.SwitchAccount, null); Spacer(modifier = Modifier.width(8.dp)); Text("Switch Profile")
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
}

// 🔥 PROFESSIONAL SERIES DETAILS SCREEN
@Composable
fun MovieDetailsScreen(movie: Movie, userVipExpiry: Long, onPlay: (Movie) -> Unit, onBack: () -> Unit) {
    val isLocked = movie.isVip && userVipExpiry < System.currentTimeMillis()
    val isSeries = movie.episodes.isNotEmpty()
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10)).padding(16.dp).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text(movie.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(model = movie.poster, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.width(120.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)))
            Column {
                Text(movie.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${movie.videoTag} • ${movie.year} • ${movie.category}", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isSeries) {
                    Button(onClick = { onPlay(movie) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(modifier = Modifier.width(8.dp)); Text(if (isLocked) "Unlock with VIP" else "Play Now", fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🔥 SERIES EPISODES LIST
        if (isSeries) {
            Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(movie.episodes) { ep ->
                    val epLabel = "${ep.season} • ${ep.episode}"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(if (selectedEpisode == ep) Color(0xFFE50914) else Color(0xFF15171E), RoundedCornerShape(8.dp)).clickable { 
                            if (isLocked) onPlay(movie)
                            else { selectedEpisode = ep; onPlay(Movie(title = "$epLabel - ${movie.title}", videoUrl = ep.url, subtitleUrl = ep.subtitleUrl)) }
                        }.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(epLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
                    }
                }
            }
        }
    }
}

// (Keep your existing HomeScreen, MyListScreen, TvLiveScreen, MovieCard functions exactly as they are. They are not affected by this update.)
