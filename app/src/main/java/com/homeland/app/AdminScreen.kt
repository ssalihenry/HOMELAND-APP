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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    
    var title by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var videoTag by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2026") }
    
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ADMIN PANEL",
            color = Color(0xFFE50914),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (statusMessage != null) {
            Text(
                text = statusMessage!!,
                color = if (statusMessage!!.contains("Success")) Color.Green else Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Movie Title") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = posterUrl,
            onValueChange = { posterUrl = it },
            label = { Text("Poster Image URL") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = videoUrl,
            onValueChange = { videoUrl = it },
            label = { Text("Video Stream URL (.mp4 / .m3u8)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = videoTag,
            onValueChange = { videoTag = it },
            label = { Text("VJ Tag (e.g., VJ JINGO)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category (e.g., Action)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Button(
            onClick = {
                if (title.isBlank() || posterUrl.isBlank() || videoUrl.isBlank()) {
                    statusMessage = "Title, Poster, and Video URL are required."
                    return@Button
                }
                
                isSaving = true
                statusMessage = null
                
                // Save to the NEW collection "homeland_movies"
                val newMovie = hashMapOf(
                    "title" to title,
                    "poster" to posterUrl,
                    "videoUrl" to videoUrl,
                    "category" to category,
                    "videoTag" to videoTag,
                    "year" to year
                )

                db.collection("homeland_movies").add(newMovie)
                    .addOnSuccessListener {
                        isSaving = false
                        statusMessage = "Success! Movie added."
                        title = ""
                        posterUrl = ""
                        videoUrl = ""
                    }
                    .addOnFailureListener { e ->
                        isSaving = false
                        statusMessage = "Error: ${e.message}"
                    }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Upload Movie", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text("Go Back", color = Color.Gray)
        }
    }
}
