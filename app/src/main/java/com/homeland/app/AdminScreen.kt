package com.homeland.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    
    // Common State
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Movie State
    var title by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2026") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedVj by remember { mutableStateOf(vjList[0]) }
    var selectedType by remember { mutableStateOf("Movie") }
    
    // TV State
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
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Add Movie / Show") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Add TV Live") })
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { selectedType = "Movie" }, 
                    colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "Movie") Color(0xFFE50914) else Color.DarkGray)
                ) { Text("Movie") }
                Button(
                    onClick = { selectedType = "TV Show" }, 
                    colors = ButtonDefaults.buttonColors(containerColor = if(selectedType == "TV Show") Color(0xFFE50914) else Color.DarkGray)
                ) { Text("TV Show") }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Stream URL (.mp4/.m3u8)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

            // Category Dropdown (Corrected syntax)
            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                OutlinedTextField(
                    value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; catExpanded = false }) }
                }
            }

            // VJ Dropdown (Corrected syntax)
            var vjExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = vjExpanded, onExpandedChange = { vjExpanded = !vjExpanded }) {
                OutlinedTextField(
                    value = selectedVj, onValueChange = {}, readOnly = true, label = { Text("VJ Tag") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vjExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                ExposedDropdownMenu(expanded = vjExpanded, onDismissRequest = { vjExpanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                    vjList.forEach { vj -> DropdownMenuItem(text = { Text(vj) }, onClick = { selectedVj = vj; vjExpanded = false }) }
                }
            }

            Button(
                onClick = {
                    if (title.isBlank() || posterUrl.isBlank() || videoUrl.isBlank()) { statusMessage = "Fill required fields."; return@Button }
                    isLoading = true
                    val newMovie = hashMapOf("title" to title, "poster" to posterUrl, "videoUrl" to videoUrl, "category" to selectedCategory, "videoTag" to selectedVj, "year" to year, "type" to selectedType)
                    db.collection("homeland_movies").add(newMovie).addOnSuccessListener {
                        isLoading = false; statusMessage = "Success! Movie added."; title = ""; posterUrl = ""; videoUrl = ""
                    }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Publish Content")
            }

        } else {
            // --- TV LIVE FORM ---
            OutlinedTextField(value = tvName, onValueChange = { tvName = it }, label = { Text("Channel Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = tvLogo, onValueChange = { tvLogo = it }, label = { Text("Logo URL") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = tvUrl, onValueChange = { tvUrl = it }, label = { Text("Stream URL (.m3u8/.mp4)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            OutlinedTextField(value = tvCategory, onValueChange = { tvCategory = it }, label = { Text("Category (e.g. News, Sports)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

            Button(
                onClick = {
                    if (tvName.isBlank() || tvUrl.isBlank()) { statusMessage = "Fill required fields."; return@Button }
                    isLoading = true
                    val newTv = hashMapOf("name" to tvName, "logo" to tvLogo, "streamUrl" to tvUrl, "category" to tvCategory)
                    db.collection("homeland_tv").add(newTv).addOnSuccessListener {
                        isLoading = false; statusMessage = "Success! TV Channel added."; tvName = ""; tvLogo = ""; tvUrl = ""
                    }.addOnFailureListener { isLoading = false; statusMessage = "Error: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Publish TV Channel")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("Go Back", color = Color.Gray) }
    }
}
