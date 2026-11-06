package com.homeland.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

@Composable
fun TVLoginScreen(theme: ThemeConfig, onLoginSuccess: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    
    // Generate a random 6-digit code
    val tvCode by remember { mutableStateOf(String.format("%06d", Random.nextInt(999999))) }
    var statusMessage by remember { mutableStateOf("Waiting for phone approval...") }
    
    // State for manual login fallback
    var showManualLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Save the code to Firebase
        db.collection("tv_login_codes").document(tvCode).set(
            hashMapOf("status" to "waiting", "timestamp" to System.currentTimeMillis())
        )

        // Listen for the phone to approve it
        db.collection("tv_login_codes").document(tvCode).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                val status = doc.getString("status")
                if (status == "approved") {
                    val email = doc.getString("email")
                    val password = doc.getString("password")
                    
                    if (email != null && password != null) {
                        statusMessage = "Logging in..."
                        // 🔥 ACTUALLY LOG INTO FIREBASE
                        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                                db.collection("tv_login_codes").document(tvCode).delete()
                            } else {
                                statusMessage = "Error: ${task.exception?.message}"
                            }
                        }
                    }
                }
            }
        }
    }

    // Show the normal Email/Password screen if they choose manual login
    if (showManualLogin) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthScreen(auth = auth, onLoginSuccess = onLoginSuccess)
            IconButton(
                onClick = { showManualLogin = false },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
        return
    }

    // Show the QR Code Login Screen
    Column(
        modifier = Modifier.fillMaxSize().background(theme.background).padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to Homeland", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Go to Profile > Login on TV on your phone to enter this code.", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 18.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        AsyncImage(
            model = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$tvCode",
            contentDescription = "QR Code",
            modifier = Modifier.size(200.dp).background(Color.White).padding(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Your Code:", color = Color.Gray, fontSize = 20.sp)
        Text(tvCode, color = theme.primary, fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = 10.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator(color = theme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(statusMessage, color = Color.Gray, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(48.dp))
        
        // Manual Login Fallback
        Button(
            onClick = { showManualLogin = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Log in manually with Email/Password", fontSize = 16.sp)
        }
    }
}
