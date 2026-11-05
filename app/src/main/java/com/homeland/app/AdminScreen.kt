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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

val vjList = listOf(
    "VJ Akaana", "VJ Arafa", "VJ Aron", "VJ Ashim", "VJ Ashim J", "VJ Banks", "VJ Baros", "VJ Bonny", "VJ Cabs", "VJ CB", "VJ Dan De", 
    "VJ Eddy", "VJ Emmy", "VJ Fredy", "VJ Geofry", "VJ Ghost King", "VJ Ham", "VJ Harderway", "VJ HD", "VJ Heavy Q", "VJ Henrico", 
    "VJ Isma", "VJ Isma K", "VJ Ivo", "VJ Jimmy", "VJ Jingo", "VJ Jovan", "VJ Jumpers", "VJ Junior", "VJ Kam", "VJ Kamran", 
    "VJ Kevo", "VJ Kevin", "VJ Khan Lee", "VJ Kimuli", "VJ Kin", "VJ Kriss", "VJ Kriss S", "VJ Kriss Sweet", "VJ KS", "VJ Lance", 
    "VJ Lash", "VJ Light", "VJ Little T", "VJ Mark", "VJ Martin K", "VJ MK", "VJ Monks", "VJ Mosco", "VJ Mox", "VJ Muba", 
    "VJ Mukibi", "VJ Mun", "VJ Musa", "VJ Neil", "VJ Nelly", "VJ Nelson", "VJ Omutaka Ice P", "VJ Pauleta", "VJ Pax Woo", "VJ Phoenix", 
    "VJ Raji", "VJ Roddy", "VJ Ronage", "VJ Ronnie", "VJ Ryan", "VJ Sammy", "VJ Shao Khan", "VJ Sharz", "VJ Shield", "VJ Silver", 
    "VJ SMK", "VJ Son", "VJ Soul", "VJ Tom", "VJ Tonny", "VJ Ulio", "VJ Uncle T", "VJ Waza", "VJ Yat", "VJ Zaidi"
)

val categories = listOf("Action", "Sci-Fi", "Comedy", "Animation", "Romantic", "Horror", "Adventure")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Movie, 1 = TV Live
    
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Edit Tracking
    var editingMovieId by remember { mutableStateOf<String?>(null) }
    var editingTvId by remember { mutableStateOf<String?>(null) }

    // --- MOVIE STATE ---
    var title by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2026") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedVj by remember { mutableStateOf(vjList[0]) }
    var selectedType by remember { mutableStateOf("Movie") }
    
    // --- TV STATE ---
    var tvName by remember { mutableStateOf("") }
    var tvLogo by remember { mutableStateOf("") }
    var tvUrl by remember { mutableStateOf("") }
    var tvCategory by remember { mutableStateOf("News") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ADMIN PANEL", color = Color(0xFFE50914), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF15171E),
            contentColor = Color(0xFFE50914)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Movies/Shows") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("TV Live") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (statusMessage != null) {
            Text(
                text = statusMessage!!,
                color = if (statusMessage!!.contains("Success")) Color.Green else Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (selectedTab == 0) {
            // --- MOVIE FORM ---
            Text(if (editingMovieId != null) "EDITING MOVIE" else "ADD NEW MOVIE", color = Color.White)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { selectedType = "Movie" }, colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "Movie") Color(0xFFE50914) else Color.DarkGray)) { Text("Movie") }
                Button(onClick = { selectedType = "TV Show" }, colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "TV Show") Color(0xFFE50914) else Color.DarkGray)) { Text("TV Show") }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Stream URL (.mp4/.m3u8)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; catExpanded = false }) }
                }
            }

            var vjExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = vjExpanded, onExpandedChange = { vjExpanded = !vjExpanded }) {
                OutlinedTextField(value = selectedVj, onValueChange = {}, readOnly = true, label = { Text("VJ Tag") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vjExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor().padding(bottom = 16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                ExposedDropdownMenu(expanded = vjExpanded, onDismissRequest = { vjExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                    vjList.forEach { vj -> DropdownMenuItem(text = { Text(vj) }, onClick = { selectedVj = vj; vjExpanded = false }) }
                }
            }

            Button(
                onClick = {
                    if (title.isBlank() || posterUrl.isBlank() || videoUrl.isBlank()) { statusMessage = "Fill required fields."; return@Button }
                    isLoading = true
                    val newMovie = hashMapOf("title" to title, "poster" to posterUrl, "videoUrl" to videoUrl, "category" to selectedCategory, "videoTag" to selectedVj, "year" to year, "type" to selectedType)
                    
                    if (editingMovieId != null) {
                        // UPDATE EXISTING
                        db.collection("homeland_movies").document(editingMovieId!!).set(newMovie).addOnSuccessListener {
                            isLoading = false; statusMessage = "Success! Movie updated."
                            editingMovieId = null; title = ""; posterUrl = ""; videoUrl = ""
                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                    } else {
                        // CREATE NEW
                        db.collection("homeland_movies").add(newMovie).addOnSuccessListener {
                            isLoading = false; statusMessage = "Success! Movie added."
                            title = ""; posterUrl = ""; videoUrl = ""
                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text(if (editingMovieId != null) "Update Content" else "Publish Content")
            }

            if (editingMovieId != null) {
                TextButton(onClick = { editingMovieId = null; title = ""; posterUrl = ""; videoUrl = "" }) { Text("Cancel Edit", color = Color.Gray) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Manage Movies/Shows", color = Color.White, fontWeight = FontWeight.Bold)
            var existingMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
            LaunchedEffect(selectedTab) {
                db.collection("homeland_movies").addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        existingMovies = snap.documents.map { doc ->
                            Movie(id = doc.id, title = doc.getString("title") ?: "", poster = doc.getString("poster") ?: "", videoUrl = doc.getString("videoUrl") ?: "", category = doc.getString("category") ?: "", videoTag = doc.getString("videoTag") ?: "", year = doc.getString("year") ?: "")
                        }
                    }
                }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(existingMovies) { movie ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(movie.title, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            editingMovieId = movie.id; title = movie.title; posterUrl = movie.poster; videoUrl = movie.videoUrl; selectedCategory = movie.category; selectedVj = movie.videoTag; year = movie.year
                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Yellow) }
                        IconButton(onClick = { db.collection("homeland_movies").document(movie.id).delete() }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                    }
                }
            }

        } else {
            // --- TV LIVE FORM ---
            Text(if (editingTvId != null) "EDITING TV" else "ADD TV LIVE", color = Color.White)
            OutlinedTextField(value = tvName, onValueChange = { tvName = it }, label = { Text("Channel Name") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = tvLogo, onValueChange = { tvLogo = it }, label = { Text("Logo URL") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = tvUrl, onValueChange = { tvUrl = it }, label = { Text("Stream URL (.m3u8/.mp4)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = tvCategory, onValueChange = { tvCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

            Button(
                onClick = {
                    if (tvName.isBlank() || tvUrl.isBlank()) { statusMessage = "Fill required fields."; return@Button }
                    isLoading = true
                    val newTv = hashMapOf("name" to tvName, "logo" to tvLogo, "streamUrl" to tvUrl, "category" to tvCategory)
                    
                    if (editingTvId != null) {
                        db.collection("homeland_tv").document(editingTvId!!).set(newTv).addOnSuccessListener {
                            isLoading = false; statusMessage = "Success! TV updated."; editingTvId = null; tvName = ""; tvLogo = ""; tvUrl = ""
                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                    } else {
                        db.collection("homeland_tv").add(newTv).addOnSuccessListener {
                            isLoading = false; statusMessage = "Success! TV added."; tvName = ""; tvLogo = ""; tvUrl = ""
                        }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text(if (editingTvId != null) "Update TV Channel" else "Publish TV Channel")
            }

            if (editingTvId != null) {
                TextButton(onClick = { editingTvId = null; tvName = ""; tvLogo = ""; tvUrl = "" }) { Text("Cancel Edit", color = Color.Gray) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Manage TV Channels", color = Color.White, fontWeight = FontWeight.Bold)
            var existingTv by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
            LaunchedEffect(selectedTab) {
                db.collection("homeland_tv").addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        existingTv = snap.documents.map { doc ->
                            TvChannel(id = doc.id, name = doc.getString("name") ?: "", logo = doc.getString("logo") ?: "", streamUrl = doc.getString("streamUrl") ?: "", category = doc.getString("category") ?: "")
                        }
                    }
                }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(existingTv) { tv ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tv.name, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editingTvId = tv.id; tvName = tv.name; tvLogo = tv.logo; tvUrl = tv.streamUrl; tvCategory = tv.category }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Yellow) }
                        IconButton(onClick = { db.collection("homeland_tv").document(tv.id).delete() }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("Go Back", color = Color.Gray) }
    }
}
