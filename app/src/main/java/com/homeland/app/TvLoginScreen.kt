package com.homeland.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

@Composable
fun TvLoginScreen(onLoginSuccess: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    // Generate a random 4-digit code
    val tvCode by remember { mutableStateOf(Random.nextInt(1000, 9999).toString()) }

    LaunchedEffect(Unit) {
        // Save the code to Firebase so the phone can find it
        db.collection("tv_logins").document(tvCode).set(
            hashMapOf("status" to "waiting", "timestamp" to System.currentTimeMillis())
        )

        // Listen for the phone to approve it
        db.collection("tv_logins").document(tvCode).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                val status = doc.getString("status")
                val userEmail = doc.getString("email")
                if (status == "authorized" && userEmail != null) {
                    onLoginSuccess(userEmail)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10)).padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to Homeland", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Scan the QR code or go to Profile > Login on TV on your phone to enter the code.", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 18.sp)
        
        Spacer(modifier = Modifier.height(48.dp))

        // Display the QR Code using a free API
        AsyncImage(
            model = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$tvCode",
            contentDescription = "QR Code",
            modifier = Modifier.size(300.dp).background(Color.White).padding(16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("Your Code:", color = Color.Gray, fontSize = 20.sp)
        Text(tvCode, color = Color(0xFFE50914), fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = 10.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator(color = Color(0xFFE50914))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Waiting for phone approval...", color = Color.Gray, fontSize = 16.sp)
    }
}
