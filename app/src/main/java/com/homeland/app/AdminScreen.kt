package com.homeland.app

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

val vjList = listOf("VJ Akaana", "VJ Arafa", "VJ Aron", "VJ Ashim", "VJ Ashim J", "VJ Banks", "VJ Baros", "VJ Bonny", "VJ Cabs", "VJ CB", "VJ Dan De", "VJ Eddy", "VJ Emmy", "VJ Fredy", "VJ Geofry", "VJ Ghost King", "VJ Ham", "VJ Harderway", "VJ HD", "VJ Heavy Q", "VJ Henrico", "VJ Isma", "VJ Isma K", "VJ Ivo", "VJ Jimmy", "VJ Jingo", "VJ Jovan", "VJ Jumpers", "VJ Junior", "VJ Kam", "VJ Kamran", "VJ Kevo", "VJ Kevin", "VJ Khan Lee", "VJ Kimuli", "VJ Kin", "VJ Kriss", "VJ Kriss S", "VJ Kriss Sweet", "VJ KS", "VJ Lance", "VJ Lash", "VJ Light", "VJ Little T", "VJ Mark", "VJ Martin K", "VJ MK", "VJ Monks", "VJ Mosco", "VJ Mox", "VJ Muba", "VJ Mukibi", "VJ Mun", "VJ Musa", "VJ Neil", "VJ Nelly", "VJ Nelson", "VJ Omutaka Ice P", "VJ Pauleta", "VJ Pax Woo", "VJ Phoenix", "VJ Raji", "VJ Roddy", "VJ Ronage", "VJ Ronnie", "VJ Ryan", "VJ Sammy", "VJ Shao Khan", "VJ Sharz", "VJ Shield", "VJ Silver", "VJ SMK", "VJ Son", "VJ Soul", "VJ Tom", "VJ Tonny", "VJ Ulio", "VJ Uncle T", "VJ Waza", "VJ Yat", "VJ Zaidi")
val categories = listOf("Action", "Sci-Fi", "Comedy", "Animation", "Romantic", "Horror", "Adventure")

data class AdminUser(val uid: String = "", val email: String = "", val vipExpiry: Long = 0L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var selectedTab by remember { mutableIntStateOf(0) } 
    
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
    var pushTitle by remember { mutableStateOf("") }
    var pushMessage by remember { mutableStateOf("") }

    // Maintenance State
    var isMaintenanceActive by remember { mutableStateOf(false) }
    var maintenanceMsg by remember { mutableStateOf("") }

    var existingMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var existingTv by remember { mutableStateOf<List<TvChannel>>(emptyList()) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            db.collection("homeland_movies").addSnapshotListener { snap, _ ->
                if (snap != null) {
                    existingMovies = snap.documents.map { doc ->
                        Movie(id = doc.id, title = doc.getString("title") ?: "", poster = doc.getString("poster") ?: "", videoUrl = doc.getString("videoUrl") ?: "", category = doc.getString("category") ?: "", videoTag = doc.getString("videoTag") ?: "", year = doc.getString("year") ?: "", isVip = doc.getBoolean("isVip") ?: false, isAdult = doc.getBoolean("isAdult") ?: false)
                    }
                }
            }
        } else if (selectedTab == 1) {
            db.collection("homeland_tv").addSnapshotListener { snap, _ ->
                if (snap != null) {
                    existingTv = snap.documents.map { doc ->
                        TvChannel(id = doc.id, name = doc.getString("name") ?: "", logo = doc.getString("logo") ?: "", streamUrl = doc.getString("streamUrl") ?: "", category = doc.getString("category") ?: "", isVip = doc.getBoolean("isVip") ?: false, isFeatured = doc.getBoolean("isFeatured") ?: false)
                    }
                }
            }
        } else if (selectedTab == 2) {
            db.collection("users").addSnapshotListener { snap, _ ->
                if (snap != null) {
                    allUsers = snap.documents.map { doc ->
                        AdminUser(uid = doc.id, email = doc.getString("email") ?: "Unknown", vipExpiry = doc.getLong("vipExpiry") ?: 0L)
                    }
                }
            }
        } else if (selectedTab == 3) {
            db.collection("config").document("maintenance").addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    isMaintenanceActive = doc.getBoolean("active") ?: false
                    maintenanceMsg = doc.getString("message") ?: ""
                }
            }
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
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Theme") })
                    Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Push") })
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
                    Text(text = statusMessage!!, color = if (statusMessage!!.contains("Success")) Color.Green else Color.Red, fontWeight = FontWeight.Bold)
                }
            }

            if (selectedTab == 0) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (editingMovieId != null) "EDITING MOVIE" else "ADD NEW MOVIE", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { selectedType = "Movie" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "Movie") Color(0xFFE50914) else Color.DarkGray)) { Text("Movie") }
                                Button(onClick = { selectedType = "TV Show" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "TV Show") Color(0xFFE50914) else Color.DarkGray)) { Text("TV Show") }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Subtitle URL (.vtt) - Optional") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                                OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                    categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; catExpanded = false }) }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            ExposedDropdownMenuBox(expanded = vjExpanded, onExpandedChange = { vjExpanded = !vjExpanded }) {
                                OutlinedTextField(value = selectedVj, onValueChange = {}, readOnly = true, label = { Text("VJ Tag") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vjExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                ExposedDropdownMenu(expanded = vjExpanded, onDismissRequest = { vjExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                                    vjList.forEach { vj -> DropdownMenuItem(text = { Text(vj) }, onClick = { selectedVj = vj; vjExpanded = false }) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (selectedType == "TV Show") {
                                Text("Add Episodes", color = Color.White, fontWeight = FontWeight.Bold)
                                var epSeason by remember { mutableStateOf("1") }
                                var epNumber by remember { mutableStateOf("1") }
                                var epUrl by remember { mutableStateOf("") }
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = epSeason, onValueChange = { epSeason = it }, label = { Text("S") }, modifier = Modifier.width(60.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedTextField(value = epNumber, onValueChange = { epNumber = it }, label = { Text("Ep") }, modifier = Modifier.width(60.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedTextField(value = epUrl, onValueChange = { epUrl = it }, label = { Text("Video URL") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(onClick = { 
                                        if (epUrl.isNotBlank()) {
                                            episodesList = episodesList + Episode("S$epSeason", "Ep $epNumber", epUrl)
                                            epUrl = ""
                                        }
                                    }) { Text("Add") }
                                }
                                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                                    items(episodesList) { ep ->
                                        Text("${ep.season} ${ep.episode} - ${ep.url}", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            } else {
                                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Movie Stream URL (.mp4/.m3u8)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Toggles
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

                            Spacer(modifier = Modifier.height(16.dp))
                            
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
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (editingMovieId != null) "Update Content" else "Publish Content") }

                            if (editingMovieId != null) {
                                TextButton(onClick = { editingMovieId = null; title = ""; posterUrl = ""; videoUrl = ""; episodesList = emptyList(); isAdultMovie = false }) { Text("Cancel Edit", color = Color.Gray) }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Manage Content", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            existingMovies.forEach { movie ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(movie.title, color = Color.White, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        editingMovieId = movie.id; title = movie.title; posterUrl = movie.poster; videoUrl = movie.videoUrl; selectedCategory = movie.category; selectedVj = movie.videoTag; year = movie.year; isVipMovie = movie.isVip; episodesList = movie.episodes; isAdultMovie = movie.isAdult
                                    }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Yellow) }
                                    IconButton(onClick = { db.collection("homeland_movies").document(movie.id).delete() }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (editingTvId != null) "EDITING TV" else "ADD TV LIVE", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = tvName, onValueChange = { tvName = it }, label = { Text("Channel Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = tvLogo, onValueChange = { tvLogo = it }, label = { Text("Logo URL") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = tvUrl, onValueChange = { tvUrl = it }, label = { Text("Stream URL") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = tvCategory, onValueChange = { tvCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Require VIP", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isVipTv, onCheckedChange = { isVipTv = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE50914)))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Featured Channel", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isFeaturedTv, onCheckedChange = { isFeaturedTv = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE50914)))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
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
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (editingTvId != null) "Update TV Channel" else "Publish TV Channel") }

                            if (editingTvId != null) {
                                TextButton(onClick = { editingTvId = null; tvName = ""; tvLogo = ""; tvUrl = ""; isFeaturedTv = false }) { Text("Cancel Edit", color = Color.Gray) }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Manage TV Channels", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            existingTv.forEach { tv ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(tv.name, color = Color.White, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { 
                                        editingTvId = tv.id; tvName = tv.name; tvLogo = tv.logo; tvUrl = tv.streamUrl; tvCategory = tv.category; isVipTv = tv.isVip; isFeaturedTv = tv.isFeatured
                                    }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Yellow) }
                                    IconButton(onClick = { db.collection("homeland_tv").document(tv.id).delete() }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                item {
                    val now = System.currentTimeMillis()
                    val threeDaysFromNow = now + (3 * 24 * 60 * 60 * 1000L)
                    
                    val expiringSoonUsers = allUsers.filter { it.vipExpiry > now && it.vipExpiry <= threeDaysFromNow }.sortedBy { it.vipExpiry }

                    if (expiringSoonUsers.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B1B)), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("VIP Expiring Soon", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text("These users expire in the next 3 days. Message them to renew!", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                
                                expiringSoonUsers.forEach { user ->
                                    val timeLeft = user.vipExpiry - now
                                    val daysLeft = timeLeft / (1000 * 60 * 60 * 24)
                                    val hoursLeft = (timeLeft % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
                                    val timeText = if (daysLeft > 0) "${daysLeft}d ${hoursLeft}h left" else "${hoursLeft}h left"
                                    
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.email, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        Text(timeText, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Generate VIP Coupon", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = couponDays, onValueChange = { couponDays = it }, label = { Text("Days") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    val days = couponDays.toIntOrNull() ?: 7
                                    val code = "HOMELAND-${days}D-${Random.nextInt(1000, 9999)}"
                                    db.collection("homeland_coupons").add(hashMapOf("code" to code, "days" to days, "isUsed" to false))
                                        .addOnSuccessListener { statusMessage = "Success! Code: $code" }
                                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), modifier = Modifier.height(50.dp)) { Text("Generate") }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Manage Users", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            allUsers.forEach { user ->
                                val isVip = user.vipExpiry > System.currentTimeMillis()
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.email, color = Color.White, fontSize = 12.sp)
                                        Text(if (isVip) "VIP Active" else "Free", color = if (isVip) Color(0xFFE50914) else Color.Gray, fontSize = 10.sp)
                                    }
                                    Button(onClick = {
                                        val base = if (isVip) user.vipExpiry else System.currentTimeMillis()
                                        db.collection("users").document(user.uid).update("vipExpiry", base + (7 * 24 * 60 * 60 * 1000L))
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), modifier = Modifier.padding(end = 4.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+7d", fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 3) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("App Theme Settings", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = themeFontUrl, onValueChange = { themeFontUrl = it }, label = { Text("Custom Font URL (.ttf or .otf)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = themeBgImageUrl, onValueChange = { themeBgImageUrl = it }, label = { Text("Custom Background Image URL (Optional)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                val themeMap = hashMapOf("fontUrl" to themeFontUrl, "bgImageUrl" to themeBgImageUrl)
                                db.collection("config").document("theme").set(themeMap, com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener { statusMessage = "Theme updated globally!" }
                            }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Apply Theme to All Devices") }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("App Maintenance Mode", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Enable Maintenance Mode", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isMaintenanceActive, onCheckedChange = { isMaintenanceActive = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = maintenanceMsg, onValueChange = { maintenanceMsg = it }, label = { Text("Maintenance Message") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                db.collection("config").document("maintenance").set(hashMapOf("active" to isMaintenanceActive, "message" to maintenanceMsg))
                                    .addOnSuccessListener { statusMessage = "Maintenance settings updated!" }
                            }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Save Maintenance Settings") }
                        }
                    }
                }
            } else {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Send Push Notification", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = pushTitle, onValueChange = { pushTitle = it }, label = { Text("Notification Title") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = pushMessage, onValueChange = { pushMessage = it }, label = { Text("Notification Message") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                if (pushTitle.isBlank() || pushMessage.isBlank()) { statusMessage = "Fill in both fields."; return@Button }
                                isLoading = true
                                val notifData = hashMapOf(
                                    "title" to pushTitle,
                                    "message" to pushMessage,
                                    "status" to "pending",
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("push_notifications").add(notifData).addOnSuccessListener {
                                    isLoading = false
                                    statusMessage = "Success! Notification queued."
                                    pushTitle = ""
                                    pushMessage = ""
                                }.addOnFailureListener {
                                    isLoading = false
                                    statusMessage = "Error: ${it.message}"
                                }
                            }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Send to All Users")
                            }
                        }
                    }
                }
            }
        }
    }
}
