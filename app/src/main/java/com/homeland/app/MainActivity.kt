package com.homeland.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.google.firebase.messaging.FirebaseMessaging

const val ADMIN_EMAIL = "ssalihenry136@gmail.com"
const val WHATSAPP_NUMBER = "256742199254" 
val movieCategories = listOf("All", "Action", "Sci-Fi", "Comedy", "Animation", "Romantic", "Horror", "Adventure")
val tvCategories = listOf("All", "News", "Sports", "Movies", "Entertainment", "Music", "Kids")

// 🔥 ONE UNIFIED CLASS FOR PLAYBACK (Includes isVip)
data class MediaPlaybackItem(val title: String, val url: String, val id: String, val isVip: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MaterialTheme {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                var isAuthenticated by remember { mutableStateOf(currentUser != null) }

                if (isAuthenticated) {
                    HomelandApp(auth = auth, onLogout = { isAuthenticated = false })
                } else {
                    AuthScreen(auth = auth, onLoginSuccess = { isAuthenticated = true })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomelandApp(auth: FirebaseAuth, onLogout: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var selectedTab by remember { mutableStateOf(0) }
    var currentlyPlaying by remember { mutableStateOf<MediaPlaybackItem?>(null) }
    var userVipExpiry by remember { mutableLongStateOf(0L) }
    var showVipModal by remember { mutableStateOf(false) }

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                userVipExpiry = (doc?.getLong("vipExpiry")) ?: 0L
            }
        }
    }

    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().subscribeToTopic("homeland_updates")
    }

    BackHandler(enabled = currentlyPlaying != null || selectedTab != 0) {
        if (currentlyPlaying != null) currentlyPlaying = null else selectedTab = 0
    }

    if (currentlyPlaying != null) {
        PlayerScreen(title = currentlyPlaying!!.title, videoUrl = currentlyPlaying!!.url, onClose = { currentlyPlaying = null })
        return 
    }

    if (showVipModal) {
        AlertDialog(
            onDismissRequest = { showVipModal = false },
            title = { Text("VIP Content", color = Color.White) },
            text = { Text("This content is for VIP members only. Subscribe to unlock all premium movies and TV channels.", color = Color.Gray) },
            confirmButton = {
                Button(onClick = { 
                    showVipModal = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP_NUMBER?text=Hi, I want to subscribe to Homeland VIP."))
                    context.startActivity(intent)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Subscribe via WhatsApp") }
            },
            dismissButton = { TextButton(onClick = { showVipModal = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF15171E)) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE50914), selectedTextColor = Color(0xFFE50914), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Tv, "TV Live") }, label = { Text("TV Live") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE50914), selectedTextColor = Color(0xFFE50914), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Favorite, "My List") }, label = { Text("My List") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE50914), selectedTextColor = Color(0xFFE50914), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE50914), selectedTextColor = Color(0xFFE50914), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFF0B0C10))) {
            when (selectedTab) {
                0 -> HomeScreen(userVipExpiry = userVipExpiry, onPlayMedia = { 
                    if (it.isVip && userVipExpiry < System.currentTimeMillis()) { showVipModal = true } 
                    else { currentlyPlaying = it } 
                })
                1 -> TvLiveScreen(userVipExpiry = userVipExpiry, onPlayMedia = { 
                    if (it.isVip && userVipExpiry < System.currentTimeMillis()) { showVipModal = true } 
                    else { currentlyPlaying = it } 
                })
                2 -> MyListScreen(userVipExpiry = userVipExpiry, onPlayMedia = { currentlyPlaying = it })
                3 -> ProfileScreen(auth, userVipExpiry, onLogout)
            }
        }
    }
}

@Composable
fun HomeScreen(userVipExpiry: Long, onPlayMedia: (MediaPlaybackItem) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var historyMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    DisposableEffect(Unit) {
        val listener = db.collection("homeland_movies").addSnapshotListener { snap, _ ->
            if (snap != null) {
                movies = snap.documents.map { doc -> 
                    Movie(id = doc.id, title = doc.getString("title") ?: "", poster = doc.getString("poster") ?: "", videoUrl = doc.getString("videoUrl") ?: "", category = doc.getString("category") ?: "", videoTag = doc.getString("videoTag") ?: "", year = doc.getString("year") ?: "", isVip = doc.getBoolean("isVip") ?: false) 
                }
                isLoading = false
            }
        }
        onDispose { listener.remove() }
    }

    LaunchedEffect(movies) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            db.collection("users").document(user.uid).collection("history").get().addOnSuccessListener { historySnap ->
                val hIds = historySnap.documents.map { it.id }
                historyMovies = movies.filter { it.id in hIds }
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFE50914)) }
    } else {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search...", color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) }, modifier = Modifier.fillMaxWidth().padding(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color(0xFF15171E), unfocusedContainerColor = Color(0xFF15171E), focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(12.dp))

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(movieCategories) { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE50914), selectedLabelColor = Color.White, containerColor = Color(0xFF15171E), labelColor = Color.Gray)) }
            }

            LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (historyMovies.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == "All") {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Continue Watching", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(historyMovies) { movie -> MovieCard(movie, userVipExpiry, onPlayMedia) }
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Trending Now", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                val filtered = movies.filter { it.title.contains(searchQuery, true) && (selectedCategory == "All" || it.category == selectedCategory) }
                items(filtered) { movie -> MovieCard(movie, userVipExpiry, onPlayMedia) }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, userVipExpiry: Long, onPlayMedia: (MediaPlaybackItem) -> Unit) {
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
            AsyncImage(model = movie.poster, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF15171E)).clickable { 
                if (uid != null && !isLocked) { db.collection("users").document(uid).collection("history").document(movie.id).set(mapOf("timestamp" to System.currentTimeMillis())) }
                onPlayMedia(MediaPlaybackItem(movie.title, movie.videoUrl, movie.id, movie.isVip)) 
            })
            
            if (isLocked) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, "Locked", tint = Color(0xFFFCDC04), modifier = Modifier.size(32.dp))
                }
            }

            IconButton(onClick = { 
                if (uid != null) {
                    if (isFav) db.collection("users").document(uid).update("favorites", FieldValue.arrayRemove(movie.id))
                    else db.collection("users").document(uid).update("favorites", FieldValue.arrayUnion(movie.id))
                    isFav = !isFav
                }
            }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Fav", tint = if (isFav) Color(0xFFE50914) else Color.White)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(movie.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${movie.videoTag} • ${movie.year}", color = if (movie.isVip) Color(0xFFFCDC04) else Color.Gray, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun MyListScreen(userVipExpiry: Long, onPlayMedia: (MediaPlaybackItem) -> Unit) {
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
            items(favMovies) { movie -> MovieCard(movie, userVipExpiry, onPlayMedia) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(auth: FirebaseAuth, userVipExpiry: Long, onLogout: () -> Unit) {
    val context = LocalContext.current
    var showAdminPanel by remember { mutableStateOf(false) }
    var couponCode by remember { mutableStateOf("") }
    val isVip = userVipExpiry > System.currentTimeMillis()

    if (showAdminPanel) {
        AdminScreen(onBack = { showAdminPanel = false })
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MY PROFILE", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Logged in as: ${auth.currentUser?.email ?: "Unknown"}", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Status: ${if (isVip) "VIP Active 👑" else "Free User"}", color = if (isVip) Color(0xFFFCDC04) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Spacer(modifier = Modifier.height(20.dp))

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
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Redeem") }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (!isVip) {
                Button(onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP_NUMBER?text=Hi, I want to subscribe to Homeland VIP."))
                    context.startActivity(intent)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Icon(Icons.Default.Whatsapp, null); Spacer(modifier = Modifier.width(8.dp)); Text("Subscribe via WhatsApp")
                }
            }

            if (auth.currentUser?.email == ADMIN_EMAIL) {
                Button(onClick = { showAdminPanel = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text("Open Admin Panel") }
            }
            Button(onClick = { auth.signOut(); onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth()) { Text("Log Out") }
        }
    }
}

@Composable
fun TvLiveScreen(userVipExpiry: Long, onPlayMedia: (MediaPlaybackItem) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }

    DisposableEffect(Unit) {
        val listener = db.collection("homeland_tv").addSnapshotListener { snapshot, _ ->
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
            items(tvCategories) { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE50914), selectedLabelColor = Color.White, containerColor = Color(0xFF15171E), labelColor = Color.Gray)) }
        }
        val filtered = channels.filter { selectedCategory == "All" || it.category == selectedCategory }
        LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filtered) { channel ->
                val isLocked = channel.isVip && userVipExpiry < System.currentTimeMillis()
                Box {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF15171E)).clickable { onPlayMedia(MediaPlaybackItem(channel.name, channel.streamUrl, channel.id, channel.isVip)) }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = channel.logo, contentDescription = channel.name, contentScale = ContentScale.Fit, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(channel.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(channel.category, color = Color(0xFFE50914), fontSize = 10.sp)
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
