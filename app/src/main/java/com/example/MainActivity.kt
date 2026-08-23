package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.ui.auth.PhoneAuthScreen
import com.example.ui.navigation.ExpenseApp
import com.example.ui.theme.ExpenseTrackerTheme
import com.example.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = ExpenseRepository(db)
        ExpenseViewModel.Factory(application, repository)
    }

    private var isSignedIn by mutableStateOf(FirebaseAuth.getInstance().currentUser != null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            isSignedIn = auth.currentUser != null
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            ExpenseTrackerTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isSignedIn) {
                        ExpenseApp(viewModel = viewModel)
                    } else {
                        PhoneAuthScreen(onSignedIn = { isSignedIn = true })
                    }
                }
            }
        }
    }
}
