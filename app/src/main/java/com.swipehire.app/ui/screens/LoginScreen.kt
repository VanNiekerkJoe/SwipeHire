package com.swipehire.app.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.auroraMesh
import com.swipehire.app.ui.theme.glow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onUseBiometric: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var registerMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    fun startGoogleSignIn() {
        scope.launch {
            busy = true
            errorMessage = null
            infoMessage = null
            try {
                val clientIdResource = context.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )
                check(clientIdResource != 0) {
                    "Google SSO is not configured. Enable Google Authentication, add the app SHA-1, and replace app/google-services.json."
                }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(clientIdResource))
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val credential = CredentialManager.create(context)
                    .getCredential(context = context, request = request)
                    .credential

                check(
                    credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) { "Google returned an unsupported credential." }

                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                check(FirebaseAuth.getInstance().currentUser != null) { "Firebase did not create a signed-in session." }
                onAuthenticated()
            } catch (_: GetCredentialCancellationException) {
                // The user closed the account chooser.
            } catch (error: Exception) {
                errorMessage = if (error.message?.contains("No credentials available", ignoreCase = true) == true) {
                    "No Google account is available. Add one in the emulator's Settings, use a Google Play emulator image, or sign in with email below."
                } else {
                    friendlyAuthError(error)
                }
            } finally {
                busy = false
            }
        }
    }

    fun submitEmailAuthentication() {
        val cleanEmail = email.trim()
        errorMessage = when {
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "Enter a valid email address."
            password.length < 6 -> "Password must contain at least 6 characters."
            registerMode && password != confirmPassword -> "The passwords do not match."
            else -> null
        }
        if (errorMessage != null) return

        scope.launch {
            busy = true
            infoMessage = null
            try {
                val auth = FirebaseAuth.getInstance()
                if (registerMode) {
                    auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                } else {
                    auth.signInWithEmailAndPassword(cleanEmail, password).await()
                }
                check(auth.currentUser != null) { "Firebase did not create a signed-in session." }
                onAuthenticated()
            } catch (error: Exception) {
                errorMessage = friendlyAuthError(error)
            } finally {
                busy = false
            }
        }
    }

    fun sendPasswordReset() {
        val cleanEmail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMessage = "Enter your email address first."
            return
        }
        scope.launch {
            busy = true
            errorMessage = null
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).await()
                infoMessage = "Password reset email sent."
            } catch (error: Exception) {
                errorMessage = friendlyAuthError(error)
            } finally {
                busy = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .auroraMesh(dark = true)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(70.dp).glow(Violet40, radiusMultiplier = 2.6f, alpha = 0.6f)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Style, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("SwipeHire", color = Color.White, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text(
                "Swipe your way to the right opportunity — or the right graduate.",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.09f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = ::startGoogleSignIn,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Violet40)
                    ) {
                        Text("Continue with Google", fontWeight = FontWeight.Bold)
                    }

                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.25f))
                        Text("  or email  ", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.25f))
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = Color.White
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (registerMode) ImeAction.Next else ImeAction.Done),
                        colors = fieldColors
                    )
                    if (registerMode) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            colors = fieldColors
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = ::submitEmailAuthentication,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text(if (registerMode) "Create account" else "Sign in")
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            registerMode = !registerMode
                            errorMessage = null
                            infoMessage = null
                        }) {
                            Text(if (registerMode) "Already registered?" else "Create an account", color = Color.White)
                        }
                        if (!registerMode) {
                            TextButton(onClick = ::sendPasswordReset, enabled = !busy) {
                                Text("Forgot password?", color = Color.White)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onUseBiometric,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Use biometric sign-in")
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            infoMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "By continuing you agree to SwipeHire's Terms & Privacy Policy.",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun friendlyAuthError(error: Exception): String = when (error) {
    is FirebaseAuthWeakPasswordException -> "Choose a stronger password with at least 6 characters."
    is FirebaseAuthUserCollisionException -> "An account already exists for this email. Sign in instead."
    is FirebaseAuthInvalidCredentialsException -> "The email or password is incorrect."
    is FirebaseNetworkException -> "Network connection failed. Check the emulator's internet connection."
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password login is not enabled in Firebase Console."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a moment and try again."
        else -> error.message ?: "Firebase authentication failed."
    }
    else -> error.message ?: "Authentication failed. Please try again."
}
