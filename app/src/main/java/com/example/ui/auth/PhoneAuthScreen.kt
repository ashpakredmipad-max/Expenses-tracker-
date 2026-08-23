package com.example.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun PhoneAuthScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = remember { FirebaseAuth.getInstance() }
    var phone by remember { mutableStateOf("+91") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun sendOtp() {
        error = null
        loading = true
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone.trim())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { loading = false; onSignedIn() }
                        .addOnFailureListener { loading = false; error = it.message ?: "Login failed" }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    loading = false
                    error = e.message ?: "Could not send OTP"
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    loading = false
                    verificationId = id
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        val id = verificationId ?: return
        error = null
        loading = true
        val credential = PhoneAuthProvider.getCredential(id, code.trim())
        auth.signInWithCredential(credential)
            .addOnSuccessListener { loading = false; onSignedIn() }
            .addOnFailureListener { loading = false; error = it.message ?: "Invalid OTP" }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign in", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Use your phone number to continue")
        Spacer(Modifier.height(24.dp))

        if (verificationId == null) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { sendOtp() },
                enabled = !loading && phone.length >= 10,
                modifier = Modifier.fillMaxWidth()
            ) { if (loading) CircularProgressIndicator() else Text("Send OTP") }
        } else {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("OTP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { verifyOtp() },
                enabled = !loading && code.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) { if (loading) CircularProgressIndicator() else Text("Verify OTP") }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
