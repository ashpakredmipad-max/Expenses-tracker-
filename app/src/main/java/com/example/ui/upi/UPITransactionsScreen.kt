package com.example.ui.upi

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.database.AppDatabase
import com.example.data.local.database.entity.CategoryEntity
import com.example.utils.CategoryIconHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

private data class UpiSmsTransaction(val sender: String, val amount: String, val date: String, val message: String)
private data class WalletOption(val id: String, val name: String)

@Composable
fun UPITransactionsScreen(onAddTransaction: (String, String) -> Unit = { _, _ -> }) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    registerSender?.let { sender -> RegisterUpiDialog(sender, database) { registerSender = null } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegisterUpiDialog(initialName: String, database: AppDatabase, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var upiName by remember(initialName) { mutableStateOf(initialName) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var wallets by remember { mutableStateOf<List<WalletOption>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedWallet by remember { mutableStateOf<WalletOption?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        categories = withContext(Dispatchers.IO) { database.categoryDao().getAllCategoriesDirect().filter { it.type == "EXPENSE" } }
        selectedCategory = categories.firstOrNull()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("wallets").addSnapshotListener { snapshot, _ ->
                wallets = snapshot?.documents?.map { WalletOption(it.id, it.getString("name") ?: "") }?.filter { it.name.isNotBlank() } ?: emptyList()
                if (selectedWallet == null || wallets.none { it.id == selectedWallet?.id }) selectedWallet = wallets.firstOrNull()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Register UPI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = upiName, onValueChange = { upiName = it }, singleLine = true, label = { Text("UPI Name") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                Column {
                    Text("Select Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(10.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory?.id == category.id
                            val catColor = CategoryIconHelper.parseColor(category.colorHex)
                            Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface, tonalElevation = if (isSelected) 3.dp else 1.dp, modifier = Modifier.border(if (isSelected) 2.dp else 1.dp, if (isSelected) catColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).clickable(enabled = !saving) { selectedCategory = category }) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(CategoryIconHelper.getIcon(category.iconName), contentDescription = category.name, tint = catColor, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(category.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
                Column {
                    Text("Select Wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(10.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        wallets.forEach { wallet ->
                            val isSelected = selectedWallet?.id == wallet.id
                            Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface, tonalElevation = if (isSelected) 3.dp else 1.dp, modifier = Modifier.border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).clickable(enabled = !saving) { selectedWallet = wallet }) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = wallet.name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(wallet.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                    if (wallets.isEmpty()) Text("No wallets yet. Add a wallet first.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val name = upiName.trim(); val category = selectedCategory; val wallet = selectedWallet
                when {
                    name.isEmpty() -> error = "UPI name is required"
                    category == null -> error = "Select a category"
                    wallet == null -> error = "Select a wallet"
                    else -> {
                        saving = true
                        scope.launch {
                            try {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("User is not logged in")
                                val collection = FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi")
                                val existing = collection.whereEqualTo("upiName", name).limit(1).get().await()
                                val data = hashMapOf<String, Any>("upiName" to name, "categoryId" to category.id, "categoryName" to category.name, "categoryIcon" to category.iconName, "categoryColorHex" to category.colorHex, "walletId" to wallet.id, "walletName" to wallet.name, "updatedAt" to com.google.firebase.Timestamp.now())
                                if (existing.documents.isNotEmpty()) existing.documents.first().reference.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                                else { data["createdAt"] = com.google.firebase.Timestamp.now(); collection.add(data).await() }
                                onDismiss()
                            } catch (e: Exception) { error = e.message ?: "Unable to save UPI"; saving = false }
                        }
                    }
                }
            }) { Text(if (saving) "Saving..." else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun extractAmount(message: String): String? {
    val matcher = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE).matcher(message)
    return if (matcher.find()) matcher.group(1)?.replace(",", "") else null
}
