package com.example.ui.upi

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.database.AppDatabase
import com.example.data.local.database.entity.CategoryEntity
import com.example.data.local.database.entity.RegisteredUpiEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

private data class UpiSmsTransaction(val sender: String, val amount: String, val date: String, val message: String)
private data class WalletOption(val id: String, val name: String)

@Composable
fun UPITransactionsScreen(onAddTransaction: (String, String) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    var transactions by remember { mutableStateOf<List<UpiSmsTransaction>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }
    var registerSender by remember { mutableStateOf<String?>(null) }

    fun loadTransactions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return
        val result = mutableListOf<UpiSmsTransaction>()
        val projection = arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE)
        context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, Telephony.Sms.Inbox.DATE + " DESC")?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.Inbox.DATE)
            while (it.moveToNext() && result.size < 200) {
                val body = if (bodyIndex >= 0) it.getString(bodyIndex).orEmpty() else ""
                val lower = body.lowercase(Locale.ROOT)
                val isUpi = listOf("upi", "vpa", "@ok", "@ybl", "@paytm", "@ibl", "@axis", "@sbi", "@hdfc", "@icici", "bhim").any(lower::contains)
                val isPayment = listOf("debited", "credited", "paid", "payment", "transaction", "sent", "received").any(lower::contains)
                if (!isUpi || !isPayment) continue
                val amount = extractAmount(body) ?: continue
                val sender = if (addressIndex >= 0) it.getString(addressIndex).orEmpty() else ""
                val time = if (dateIndex >= 0) it.getLong(dateIndex) else 0L
                result.add(UpiSmsTransaction(sender, amount, SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time)), body))
            }
        }
        transactions = result
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) loadTransactions()
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) loadTransactions()
        else permissionLauncher.launch(Manifest.permission.READ_SMS)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        when {
            permissionDenied -> Text("SMS permission is required to show UPI transactions.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            transactions.isEmpty() -> Text("No UPI transactions found in SMS.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(transactions) { transaction ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("₹${transaction.amount}", style = MaterialTheme.typography.titleLarge)
                            Text(transaction.date, style = MaterialTheme.typography.bodySmall)
                            if (transaction.sender.isNotBlank()) Text(transaction.sender, style = MaterialTheme.typography.bodyMedium)
                            Text(transaction.message, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onAddTransaction(transaction.amount, transaction.message) }, modifier = Modifier.weight(1f)) { Text("Add Transaction") }
                                Button(onClick = { registerSender = transaction.sender }, modifier = Modifier.weight(1f)) { Text("Register UPI") }
                            }
                        }
                    }
                }
            }
        }
    }

    registerSender?.let { sender ->
        RegisterUpiDialog(sender, database, { registerSender = null })
    }
}

@Composable
private fun RegisterUpiDialog(initialName: String, database: AppDatabase, onDismiss: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var upiName by remember(initialName) { mutableStateOf(initialName) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var wallets by remember { mutableStateOf<List<WalletOption>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedWallet by remember { mutableStateOf<WalletOption?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    var walletMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        categories = withContext(Dispatchers.IO) { database.categoryDao().getCategoriesByTypeDirect("EXPENSE") }
        selectedCategory = categories.firstOrNull()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("wallets").get()
                .addOnSuccessListener { snapshot ->
                    wallets = snapshot.documents.map { WalletOption(it.id, it.getString("name") ?: "") }.filter { it.name.isNotBlank() }
                    selectedWallet = wallets.firstOrNull()
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register UPI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = upiName, onValueChange = { upiName = it }, singleLine = true, label = { Text("UPI Name") }, modifier = Modifier.fillMaxWidth())
                Column {
                    Button(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedCategory?.name ?: "Select Category") }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { selectedCategory = category; categoryMenu = false }) }
                    }
                }
                Column {
                    Button(onClick = { walletMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedWallet?.name ?: "Select Wallet") }
                    DropdownMenu(expanded = walletMenu, onDismissRequest = { walletMenu = false }) {
                        wallets.forEach { wallet -> DropdownMenuItem(text = { Text(wallet.name) }, onClick = { selectedWallet = wallet; walletMenu = false }) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = upiName.trim()
                val category = selectedCategory
                val wallet = selectedWallet
                when {
                    name.isEmpty() -> error = "UPI name is required"
                    category == null -> error = "Select a category"
                    wallet == null -> error = "Select a wallet"
                    else -> scope.launch(Dispatchers.IO) {
                        val dao = database.registeredUpiDao()
                        dao.findByName(name)?.let { dao.deleteByName(name) }
                        dao.insert(RegisteredUpiEntity(upiName = name, categoryId = category.id, categoryName = category.name, walletName = wallet.name))
                        withContext(Dispatchers.Main) { onDismiss() }
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun extractAmount(message: String): String? {
    val matcher = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE).matcher(message)
    return if (matcher.find()) matcher.group(1)?.replace(",", "") else null
}
