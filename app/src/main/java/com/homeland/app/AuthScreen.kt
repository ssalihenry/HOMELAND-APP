package com.homeland.app

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    // 🔥 NEW: GOOGLE SIGN-IN SETUP
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
                            // 🔥 Check if user exists in Firestore, if not create profile
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
                                onLoginSuccess() // Let them in even if firestore fails
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
            // User cancelled the Google popup
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0C10), Color(0xFF1A0002))))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15171E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HOMELAND",
                    color = Color(0xFFE50914),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create an Account",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp))
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray)
                )

                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE50914)))
                        Text("Remember Me", color = Color.Gray, fontSize = 12.sp)
                    }
                    if (isLoginMode) {
                        TextButton(onClick = { showForgotPassword = true }) {
                            Text("Forgot Password?", color = Color(0xFFE50914), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text(if (isLoginMode) "Log In" else "Register", fontWeight = FontWeight.Bold)
                }

                // 🔥 NEW: OR DIVIDER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.DarkGray)
                    Text(" OR ", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.DarkGray)
                }

                // 🔥 NEW: GOOGLE SIGN-IN BUTTON
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { isLoginMode = !isLoginMode; errorMessage = null }) {
                    Text(if (isLoginMode) "Don't have an account? Register" else "Already have an account? Log In", color = Color.Gray, fontSize = 14.sp)
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
