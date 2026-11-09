package com.homeland.app

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPassword by remember { mutableStateOf(false) }

    // Social Links State
    var socialLinks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("config").document("social_links")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    socialLinks = mapOf(
                        "whatsapp" to (snapshot.getString("whatsapp") ?: ""),
                        "telegram" to (snapshot.getString("telegram") ?: ""),
                        "tiktok" to (snapshot.getString("tiktok") ?: ""),
                        "twitter" to (snapshot.getString("twitter") ?: ""),
                        "instagram" to (snapshot.getString("instagram") ?: "")
                    )
                }
            }
    }

    // GOOGLE SIGN-IN SETUP
    val webClientId = "26733775290-filospi99uuqn4kqn7r6sgfv83ljckln.apps.googleusercontent.com"
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!
                isLoading = true
                errorMessage = null

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = authTask.result?.user
                        if (user != null) {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                                if (!doc.exists()) {
                                    val userData = hashMapOf(
                                        "uid" to user.uid,
                                        "email" to user.email,
                                        "name" to (user.displayName ?: user.email?.split("@")?.get(0) ?: "User"),
                                        "vipExpiry" to 0L,
                                        "homelandVipExpiry" to 0L,
                                        "favorites" to emptyMap<String, List<String>>()
                                    )
                                    db.collection("users").document(user.uid).set(userData).addOnCompleteListener {
                                        isLoading = false
                                        onLoginSuccess()
                                    }
                                } else {
                                    isLoading = false
                                    onLoginSuccess()
                                }
                            }.addOnFailureListener {
                                isLoading = false
                                onLoginSuccess() 
                            }
                        } else {
                            isLoading = false
                            onLoginSuccess()
                        }
                    } else {
                        isLoading = false
                        errorMessage = authTask.exception?.message ?: "Google Sign-In failed."
                        Log.e("GoogleAuth", "Firebase Auth failed", authTask.exception)
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Google Sign-In failed: ${e.message}"
                Log.e("GoogleAuth", "ApiException code: ${e.statusCode}", e)
            }
        } else {
            isLoading = false
        }
    }

    // 🔥 BEAUTIFUL CINEMATIC BACKGROUND
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A0004), Color(0xFF0B0C10), Color(0xFF0B0C10)),
                    radius = 1200f
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black, spotColor = Color.Black),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E).copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HOMELAND",
                    color = Color(0xFFE50914),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create an Account",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!, 
                        color = Color.Red, 
                        fontSize = 13.sp, 
                        textAlign = TextAlign.Center, 
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // 🔥 PILL-SHAPED ELEGANT INPUTS
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), 
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White, 
                        focusedBorderColor = Color(0xFFE50914), 
                        unfocusedBorderColor = Color.DarkGray,
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                    )
                )

                OutlinedTextField(
                    value = password, 
                    onValueChange = { password = it }, 
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(), 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), 
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White, 
                        focusedBorderColor = Color(0xFFE50914), 
                        unfocusedBorderColor = Color.DarkGray,
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe, 
                            onCheckedChange = { rememberMe = it }, 
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE50914), uncheckedColor = Color.Gray)
                        )
                        Text("Remember Me", color = Color.Gray, fontSize = 12.sp)
                    }
                    if (isLoginMode) {
                        TextButton(onClick = { showForgotPassword = true }) {
                            Text("Forgot Password?", color = Color(0xFFE50914), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // 🔥 GLOWING PRIMARY BUTTON
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) { errorMessage = "Please fill in all fields."; return@Button }
                        isLoading = true; errorMessage = null
                        if (isLoginMode) {
                            auth.signInWithEmailAndPassword(email.trim(), password.trim()).addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) onLoginSuccess() else errorMessage = task.exception?.message ?: "Login failed"
                            }
                        } else {
                            auth.createUserWithEmailAndPassword(email.trim(), password.trim()).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = task.result?.user
                                    if (user != null) {
                                        val db = FirebaseFirestore.getInstance()
                                        val userData = hashMapOf(
                                            "uid" to user.uid,
                                            "email" to user.email,
                                            "name" to (user.email?.split("@")?.get(0) ?: "User"),
                                            "vipExpiry" to 0L,
                                            "favorites" to emptyList<String>()
                                        )
                                        db.collection("users").document(user.uid).set(userData).addOnCompleteListener {
                                            isLoading = false
                                            onLoginSuccess()
                                        }
                                    } else {
                                        isLoading = false
                                        onLoginSuccess()
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = task.exception?.message ?: "Registration failed"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(12.dp, RoundedCornerShape(50), ambientColor = Color(0xFFE50914), spotColor = Color(0xFFE50914)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(50)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) 
                    else Text(if (isLoginMode) "Log In" else "Register", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // OR DIVIDER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.DarkGray)
                    Text(" OR ", color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp), fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.DarkGray)
                }

                // GOOGLE BUTTON
                Button(
                    onClick = {
                        if (activity != null) {
                            isLoading = true
                            errorMessage = null
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        } else {
                            errorMessage = "Activity context not found."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { isLoginMode = !isLoginMode; errorMessage = null }) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? Register" else "Already have an account? Log In", 
                        color = Color.Gray, 
                        fontSize = 14.sp
                    )
                }

                // SOCIAL MEDIA LINKS
                if (socialLinks.values.any { it.isNotBlank() }) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = Color.DarkGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Follow Us", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    val uriHandler = LocalUriHandler.current
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!socialLinks["whatsapp"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["whatsapp"]!!) }) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                            }
                        }
                        if (!socialLinks["telegram"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["telegram"]!!) }) {
                                Icon(Icons.Default.Send, contentDescription = "Telegram", tint = Color(0xFF0088cc))
                            }
                        }
                        if (!socialLinks["tiktok"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["tiktok"]!!) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "TikTok", tint = Color.White)
                            }
                        }
                        if (!socialLinks["twitter"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["twitter"]!!) }) {
                                Icon(Icons.Default.Share, contentDescription = "Twitter", tint = Color(0xFF1DA1F2))
                            }
                        }
                        if (!socialLinks["instagram"].isNullOrBlank()) {
                            IconButton(onClick = { uriHandler.openUri(socialLinks["instagram"]!!) }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Instagram", tint = Color(0xFFE1306C))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Reset Password", color = Color.White) },
            text = { Text("Send a password reset link to your email?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    if (email.isNotBlank()) {
                        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { 
                            showForgotPassword = false
                            errorMessage = "Reset link sent to $email"
                        }
                    } else { errorMessage = "Enter your email address first."; showForgotPassword = false }
                }) { Text("Send Link", color = Color(0xFFE50914)) }
            },
            dismissButton = { TextButton(onClick = { showForgotPassword = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color(0xFF15171E)
        )
    }
}
