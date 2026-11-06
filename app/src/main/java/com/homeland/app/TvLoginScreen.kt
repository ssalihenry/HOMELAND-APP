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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

@Composable
fun TVLoginScreen(theme: ThemeConfig, onLoginSuccess: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var loginCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Generating code...") }
    var showManualLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!showManualLogin && loginCode.isEmpty()) {
            val code = String.format("%06d", Random.nextInt(999999))
            loginCode = code
            statusMessage = "Waiting for phone approval..."
            
            db.collection("tv_login_codes").document(code).set(
                hashMapOf(
                    "status" to "pending",
                    "timestamp" to System.currentTimeMillis()
                )
            )

            db.collection("tv_login_codes").document(code).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "approved") {
                        val email = snapshot.getString("email")
                        val password = snapshot.getString("password")
                        if (email != null && password != null) {
                            statusMessage = "Logging in..."
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                    db.collection("tv_login_codes").document(code).delete()
                                } else {
                                    statusMessage = "Error: ${task.exception?.message}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualLogin) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthScreen(auth = auth, onLoginSuccess = onLoginSuccess)
            
            IconButton(
                onClick = { showManualLogin = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(theme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("HOMELAND TV", color = theme.primary, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Go to the Homeland app on your phone", color = Color.White, fontSize = 20.sp)
        Text("Go to Profile -> Login on TV", color = Color.Gray, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter this code:", color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .background(Color(0xFF15171E), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            Text(text = loginCode, color = theme.primary, fontSize = 56.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(statusMessage, color = Color.Gray, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = { showManualLogin = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Log in manually with Email/Password", fontSize = 16.sp)
        }
    }
}
