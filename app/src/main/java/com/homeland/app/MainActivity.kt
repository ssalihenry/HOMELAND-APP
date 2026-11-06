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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import android.content.pm.PackageManager
import android.content.res.Configuration

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
                
                // 🔥 TV DETECTION LOGIC
                val context = LocalContext.current
                val isTv = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || 
                           (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

                if (isTv) {
                    if (isAuthenticated) {
                        HomelandApp(auth = auth, theme = appTheme, onLogout = { isAuthenticated = false }, onSwitchProfile = { })
                    } else {
                        TVLoginScreen(theme = appTheme, onLoginSuccess = { 
                            isAuthenticated = true 
                        })
                    }
                } else {
                    if (isAuthenticated) {
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 🔥 BACKGROUND LAYER
            if (theme.bgImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = theme.bgImageUrl,
                    contentDescription = "App Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(theme.background))
            }

            // 🔥 CONTENT LAYER
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeScreen(theme = theme, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                    1 -> TvLiveScreen(theme = theme, userVipExpiry = userVipExpiry, onPlayMedia = { currentlyPlaying = it })
                    2 -> MyListScreen(theme = theme, userVipExpiry = userVipExpiry, onDetails = { viewingDetails = it })
                    3 -> ProfileScreen(auth, userVipExpiry, theme, onLogout, onSwitchProfile)
                }
            }
        }
    }
}

@Composable
fun ProfileSelectorScreen(theme: ThemeConfig, onProfileSelected: () -> Unit) {
    var profiles by remember { mutableStateOf(listOf("Me", "Kids", "Guest")) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(auth: FirebaseAuth, userVipExpiry: Long, theme: ThemeConfig, onLogout: () -> Unit, onSwitchProfile: () -> Unit) {
    val context = LocalContext.current
    var showAdminPanel by remember { mutableStateOf(false) }
    var couponCode by remember { mutableStateOf("") }
    val isVip = userVipExpiry > System.currentTimeMillis()

    var showTvDialog by remember { mutableStateOf(false) }
    var tvCodeInput by remember { mutableStateOf("") }

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
            Text("Logged in as: ${auth.currentUser?.email ?: "Unknown"}", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${if (isVip) "VIP Active 👑" else "Free User"}", color = if (isVip) Color(0xFFFCDC04) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onSwitchProfile, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Icon(Icons.Default.SwitchAccount, null); Spacer(modifier = Modifier.width(8.dp)); Text("Switch Profile")
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
                            // 1. Verify the password with Firebase first
                            auth.signInWithEmailAndPassword(userEmail, passwordInput).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // 2. Password is correct! Send it to the TV.
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("tv_login_codes").document(tvCodeInput).update(
                                        "status", "approved",
                                        "email", userEmail,
                                        "password", passwordInput // Send valid details to TV
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
} // <--- THIS IS THE BRACE THAT WAS MISSING TO CLOSE ProfileScreen

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(theme: ThemeConfig, userVipExpiry: Long, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var featuredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var historyMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var lastVisibleMovie by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    var isFetchingMore by remember { mutableStateOf(false) }

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
                    Movie(id = doc.id, title = doc.getString("title") ?: "", poster = doc.getString("poster") ?: "", videoUrl = doc.getString("videoUrl") ?: "", category = doc.getString("category") ?: "", videoTag = doc.getString("videoTag") ?: "", year = doc.getString("year") ?: "", isVip = doc.getBoolean("isVip") ?: false, isFeatured = doc.getBoolean("isFeatured") ?: false) 
                }
                movies = (movies + newMovies).distinctBy { it.id }
                lastVisibleMovie = snap.documents.lastOrNull()
            }
            isFetchingMore = false
        }.addOnFailureListener {
            isFetchingMore = false
        }
    }

    LaunchedEffect(Unit) {
        db.collection("homeland_movies").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).get().addOnSuccessListener { snap ->
            if (snap != null) {
                movies = snap.documents.map { doc -> 
                    Movie(id = doc.id, title = doc.getString("title") ?: "", poster = doc.getString("poster") ?: "", videoUrl = doc.getString("videoUrl") ?: "", category = doc.getString("category") ?: "", videoTag = doc.getString("videoTag") ?: "", year = doc.getString("year") ?: "", isVip = doc.getBoolean("isVip") ?: false, isFeatured = doc.getBoolean("isFeatured") ?: false) 
                }
                lastVisibleMovie = snap.documents.lastOrNull()
                featuredMovies = movies.filter { it.isFeatured }.take(5)
                if (featuredMovies.isEmpty()) featuredMovies = movies.take(3)
                isLoading = false
            }
        }
    }

    LaunchedEffect(movies) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            db.collection("users").document(user.uid).collection("history").get().addOnSuccessListener { historySnap ->
                val hIds = historySnap.documents.map { it.id }
                val valid = movies.filter { it.id in hIds }
                historyMovies = if (valid.isEmpty()) movies.take(3) else valid
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.primary) }
    } else {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            if (featuredMovies.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == "All") {
                val pagerState = rememberPagerState(pageCount = { featuredMovies.size })
                HorizontalPager(state = pagerState, modifier = Modifier.height(250.dp).fillMaxWidth().padding(bottom = 16.dp)) { page ->
                    val movie = featuredMovies[page]
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(model = movie.poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, theme.background))))
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text(movie.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = { onDetails(movie) }, colors = ButtonDefaults.buttonColors(containerColor = theme.primary), modifier = Modifier.padding(top = 8.dp)) { Icon(Icons.Default.PlayArrow, null); Text("Play Now") }
                        }
                    }
                }
            }

            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search...", color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.primary, unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color(0xFF15171E), unfocusedContainerColor = Color(0xFF15171E), focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(12.dp))

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(movieCategories) { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.primary, selectedLabelColor = Color.White, containerColor = Color(0xFF15171E), labelColor = Color.Gray)) }
            }

            LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (historyMovies.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == "All") {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text(if (historyMovies.size == 3) "Top Picks For You" else "Continue Watching", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(historyMovies) { movie -> MovieCard(movie, theme, userVipExpiry, onDetails) }
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Trending Now", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                val filtered = movies.filter { it.title.contains(searchQuery, true) && (selectedCategory == "All" || it.category == selectedCategory) }
                items(filtered) { movie -> MovieCard(movie, theme, userVipExpiry, onDetails) }
                
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (isFetchingMore) CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(24.dp))
                        else Button(onClick = { loadMoreMovies() }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Load More") }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, theme: ThemeConfig, userVipExpiry: Long, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var isFav by remember { mutableStateOf(false) }
    val isLocked = movie.isVip && userVipExpiry < System.currentTimeMillis()

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                isFav = (doc.get("favorites") as? List<String> ?: emptyList()).contains(movie.id)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().clickable { onDetails(movie) }) {
        Box {
            AsyncImage(model = movie.poster, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF15171E)))
            if (isLocked) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, "Locked", tint = Color(0xFFFCDC04), modifier = Modifier.size(32.dp)) }
            }
            IconButton(onClick = { 
                if (uid != null) {
                    if (isFav) db.collection("users").document(uid).update("favorites", FieldValue.arrayRemove(movie.id))
                    else db.collection("users").document(uid).update("favorites", FieldValue.arrayUnion(movie.id))
                    isFav = !isFav
                }
            }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Fav", tint = if (isFav) theme.primary else Color.White)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(movie.title, color = Color.White, fontSize = (theme.fontSize).sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${movie.videoTag} • ${movie.year}", color = if (movie.isVip) Color(0xFFFCDC04) else Color.Gray, fontSize = (theme.fontSize - 4).sp, maxLines = 1)
    }
}

@Composable
fun MyListScreen(theme: ThemeConfig, userVipExpiry: Long, onDetails: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var favMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                val favs = doc?.get("favorites") as? List<String> ?: emptyList()
                db.collection("homeland_movies").get().addOnSuccessListener { snap ->
                    favMovies = snap.documents.mapNotNull { 
                        Movie(id = it.id, title = it.getString("title") ?: "", poster = it.getString("poster") ?: "", videoUrl = it.getString("videoUrl") ?: "", videoTag = it.getString("videoTag") ?: "", year = it.getString("year") ?: "", isVip = it.getBoolean("isVip") ?: false) 
                    }.filter { it.id in favs }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()) {
        Text("My List", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        if (favMovies.isEmpty()) Text("Your list is empty. Add favorites from Home.", color = Color.Gray)
        else LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(favMovies) { movie -> MovieCard(movie, theme, userVipExpiry, onDetails) }
        }
    }
}

@Composable
fun TvLiveScreen(theme: ThemeConfig, userVipExpiry: Long, onPlayMedia: (Movie) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }

    DisposableEffect(Unit) {
        val listener = db.collection("homeland_tv").orderBy("name", Query.Direction.ASCENDING).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                channels = snapshot.documents.mapNotNull { doc ->
                    TvChannel(id = doc.id, name = doc.getString("name") ?: "", logo = doc.getString("logo") ?: "", streamUrl = doc.getString("streamUrl") ?: "", category = doc.getString("category") ?: "", isVip = doc.getBoolean("isVip") ?: false)
                }
            }
        }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tvCategories) { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.primary, selectedLabelColor = Color.White, containerColor = Color(0xFF15171E), labelColor = Color.Gray)) }
        }
        val filtered = channels.filter { selectedCategory == "All" || it.category == selectedCategory }
        LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filtered) { channel ->
                val isLocked = channel.isVip && userVipExpiry < System.currentTimeMillis()
                Box {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF15171E)).clickable { 
                        onPlayMedia(Movie(title = channel.name, videoUrl = channel.streamUrl, isVip = channel.isVip)) 
                    }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = channel.logo, contentDescription = channel.name, contentScale = ContentScale.Fit, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(channel.name, color = Color.White, fontSize = theme.fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(channel.category, color = theme.primary, fontSize = (theme.fontSize - 4).sp)
                    }
                    if (isLocked) {
                        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, "Locked", tint = Color(0xFFFCDC04), modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
