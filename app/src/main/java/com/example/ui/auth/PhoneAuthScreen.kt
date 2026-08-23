package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

private fun mobileAsEmail(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return "user_${digits}@expenses-tracker.invalid"
}

@Composable
fun PhoneAuthScreen(onSignedIn: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var phone by remember { mutableStateOf("+91") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        error = null
        val cleanPhone = phone.trim()
        if (cleanPhone.filter { it.isDigit() }.length < 10) {
            error = "Enter a valid mobile number"
            return
        }
        if (password.length < 6) {
            error = "Password must be at least 6 characters"
            return
        }
        if (isSignUp && password != confirmPassword) {
            error = "Passwords do not match"
            return
        }

        loading = true
        val email = mobileAsEmail(cleanPhone).lowercase(Locale.US)
        val task = if (isSignUp) {
            auth.createUserWithEmailAndPassword(email, password)
        } else {
            auth.signInWithEmailAndPassword(email, password)
        }
        task.addOnSuccessListener {
            loading = false
            onSignedIn()
        }.addOnFailureListener {
            loading = false
            error = it.message ?: if (isSignUp) "Sign up failed" else "Login failed"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isSignUp) "Create account" else "Sign in",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text("Use your mobile number and password")
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Mobile number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (isSignUp) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { submit() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator()
            else Text(if (isSignUp) "Sign Up" else "Login")
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                isSignUp = !isSignUp
                error = null
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) "Already have an account? Login" else "New user? Sign Up")
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
