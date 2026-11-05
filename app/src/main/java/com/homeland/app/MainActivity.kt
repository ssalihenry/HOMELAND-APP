package com.homeland.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

const val ADMIN_EMAIL = "ssalihenry136@gmail.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF15171E) 
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE50914),
                        selectedTextColor = Color(0xFFE50914),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Tv, contentDescription = "TV Live") },
                    label = { Text("TV Live") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE50914),
                        selectedTextColor = Color(0xFFE50914),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE50914),
                        selectedTextColor = Color(0xFFE50914),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0B0C10)) 
        ) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> TvLiveScreen()
                2 -> ProfileScreen(auth, onLogout)
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val db = FirebaseFirestore.getInstance()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 🔥 REAL-TIME SYNC TO HOMELAND MOVIES ONLY
    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("homeland_movies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fetchedMovies = snapshot.documents.mapNotNull { doc ->
                        Movie(
                            id = doc.id,
                            title = doc.getString("title") ?: "Unknown",
                            poster = doc.getString("poster") ?: "",
                            videoUrl = doc.getString("videoUrl") ?: "",
                            category = doc.getString("category") ?: "",
                            videoTag = doc.getString("videoTag") ?: "",
                            year = doc.get("year")?.toString() ?: "",
                            type = doc.getString("type") ?: "Movie"
                        )
                    }
                    movies = fetchedMovies
                    isLoading = false
                }
            }
        onDispose { listenerRegistration.remove() }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE50914))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Homeland Originals",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (movies.isEmpty()) {
                Text("No content found. Use the Admin Panel to add movies!", color = Color.Gray)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(movies) { movie ->
                        MovieCard(movie)
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
    ) {
        AsyncImage(
            model = movie.poster,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f) 
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF15171E))
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = "${movie.videoTag} • ${movie.year}",
            color = Color.Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TvLiveScreen() {
    val db = FirebaseFirestore.getInstance()
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 🔥 REAL-TIME SYNC TO HOMELAND TV ONLY
    DisposableEffect(Unit) {
        val listener = db.collection("homeland_tv")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val fetchedChannels = snapshot.documents.mapNotNull { doc ->
                        TvChannel(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            logo = doc.getString("logo") ?: "",
                            streamUrl = doc.getString("streamUrl") ?: "",
                            category = doc.getString("category") ?: ""
                        )
                    }
                    channels = fetchedChannels
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE50914))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Homeland Live TV",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (channels.isEmpty()) {
                Text("No channels found. Add some in the Admin Panel!", color = Color.Gray)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), // 2 columns for TV logos
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(channels) { channel ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF15171E))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = channel.logo,
                                contentDescription = channel.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = channel.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = channel.category,
                                color = Color(0xFFE50914),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(auth: FirebaseAuth, onLogout: () -> Unit) {
    var showAdminPanel by remember { mutableStateOf(false) }

    if (showAdminPanel) {
        AdminScreen(onBack = { showAdminPanel = false })
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "MY PROFILE", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Logged in as: ${auth.currentUser?.email ?: "Unknown"}",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (auth.currentUser?.email == ADMIN_EMAIL) {
                Button(
                    onClick = { showAdminPanel = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text("Open Admin Panel")
                }
            }

            Button(
                onClick = {
                    auth.signOut()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Out")
            }
        }
    }
}
