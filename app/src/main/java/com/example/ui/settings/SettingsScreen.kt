package com.example.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var importResultDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showBackupRestoreSuccessDialog by remember { mutableStateOf<String?>(null) }

    // SAF Launchers for CSV Export / Import
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = viewModel.exportCsv(context, uri)
                if (success) {
                    Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to export CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.importCsv(context, uri)
                importResultDialog = Pair(result.successCount, result.skippedCount)
            }
        }
    }

    // SAF Launchers for JSON Backup / Restore
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = viewModel.exportBackupJson(context, uri)
                if (success) {
                    showBackupRestoreSuccessDialog = "Full backup exported successfully!"
                } else {
                    Toast.makeText(context, "Failed to create backup", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = viewModel.importBackupJson(context, uri)
                if (success) {
                    showBackupRestoreSuccessDialog = "Backup restored successfully!"
                } else {
                    Toast.makeText(context, "Failed to restore backup (invalid format)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
        
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
    start = 16.dp,
    end = 16.dp,
    bottom = 16.dp
),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings Group
            item {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // Currency setting
                        SettingsItem(
                            icon = Icons.Default.CurrencyRupee,
                            title = "Currency",
                            subtitle = "Indian Rupee (₹ INR)",
                            onClick = {
                                Toast.makeText(context, "Default currency is Indian Rupee (₹)", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // Manage categories
                        SettingsItem(
                            icon = Icons.Default.Category,
                            title = "Manage Categories",
                            subtitle = "Add, edit, or customize categories",
                            onClick = onNavigateToCategories,
                            testTag = "settings_manage_categories"
                        )

                        // Monthly budget
                        SettingsItem(
                            icon = Icons.Default.Savings,
                            title = "Monthly Budget",
                            subtitle = "Set monthly spending limits & alerts",
                            onClick = onNavigateToBudget,
                            testTag = "settings_monthly_budget"
                        )

                        // Theme Mode
                        val themeSubtitle = when (themeMode) {
                            "LIGHT" -> "Light Theme"
                            "DARK" -> "Dark Theme"
                            else -> "System Default"
                        }
                        SettingsItem(
                            icon = Icons.Default.DarkMode,
                            title = "Appearance",
                            subtitle = themeSubtitle,
                            onClick = { showThemeDialog = true },
                            testTag = "settings_theme_toggle"
                        )
                    }
                }
            }

            // Data Management Group
            item {
                Text(
                    text = "Data & Backup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // Export CSV
                        SettingsItem(
                            icon = Icons.Default.FileUpload,
                            title = "Export Data (CSV)",
                            subtitle = "Export transactions spreadsheet",
                            onClick = {
                                val filename = "expense_tracker_${System.currentTimeMillis()}.csv"
                                exportCsvLauncher.launch(filename)
                            },
                            testTag = "settings_export_csv"
                        )

                        // Import CSV
                        SettingsItem(
                            icon = Icons.Default.FileDownload,
                            title = "Import Data (CSV)",
                            subtitle = "Import transactions from CSV file",
                            onClick = {
                                importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                            },
                            testTag = "settings_import_csv"
                        )

                        // JSON Backup
                        SettingsItem(
                            icon = Icons.Default.Upload,
                            title = "Backup to JSON",
                            subtitle = "Export complete app database to JSON",
                            onClick = {
                                val filename = "expense_backup_${System.currentTimeMillis()}.json"
                                exportBackupLauncher.launch(filename)
                            },
                            testTag = "settings_backup_json"
                        )

                        // JSON Restore
                        SettingsItem(
                            icon = Icons.Default.Download,
                            title = "Restore from JSON",
                            subtitle = "Restore database from JSON backup",
                            onClick = {
                                importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            testTag = "settings_restore_json"
                        )

                        // Delete all data
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete All Data",
                            subtitle = "Reset transactions, budgets, and categories",
                            tint = ExpenseRed,
                            onClick = { showDeleteAllDialog = true },
                            testTag = "settings_delete_all"
                        )
                    }
                }
            }

            // About & Privacy
            item {
                Text(
                    text = "About & Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "100% Offline & Private",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "All financial records and categories are stored locally in your device's secure Room database. No data is ever sent to external cloud servers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Expense Tracker v1.0 • Built with Material 3 & Jetpack Compose",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode("SYSTEM")
                                showThemeDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == "SYSTEM",
                            onClick = {
                                viewModel.setThemeMode("SYSTEM")
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("System Default")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode("LIGHT")
                                showThemeDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == "LIGHT",
                            onClick = {
                                viewModel.setThemeMode("LIGHT")
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Light Theme")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode("DARK")
                                showThemeDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == "DARK",
                            onClick = {
                                viewModel.setThemeMode("DARK")
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dark Theme")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import CSV Result Dialog
    if (importResultDialog != null) {
        val (success, skipped) = importResultDialog!!
        AlertDialog(
            onDismissRequest = { importResultDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("CSV Import Summary") },
            text = {
                Text(
                    "Successfully imported: $success transaction${if (success != 1) "s" else ""}\n" +
                            "Skipped invalid rows: $skipped"
                )
            },
            confirmButton = {
                Button(onClick = { importResultDialog = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Backup/Restore success dialog
    if (showBackupRestoreSuccessDialog != null) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreSuccessDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = IncomeGreen
                )
            },
            title = { Text("Success") },
            text = { Text(showBackupRestoreSuccessDialog!!) },
            confirmButton = {
                Button(onClick = { showBackupRestoreSuccessDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Strong Delete All Data Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = ExpenseRed
                )
            },
            title = { Text("Delete All Data?") },
            text = {
                Text(
                    "This action is permanent and irreversible!\n\n" +
                            "All transactions, monthly budgets, and custom categories will be erased from the database."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllData {
                            Toast.makeText(context, "All data has been reset", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tint == ExpenseRed) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
