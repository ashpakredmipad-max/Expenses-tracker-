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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.ui.navigation.ExpenseApp
import com.example.ui.theme.ExpenseTrackerTheme
import com.example.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = ExpenseRepository(db)
        ExpenseViewModel.Factory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            ExpenseTrackerTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ExpenseApp(viewModel = viewModel)
                }
            }
        }
    }
}
