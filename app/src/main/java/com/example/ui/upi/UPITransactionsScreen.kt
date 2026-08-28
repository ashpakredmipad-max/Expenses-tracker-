package com.example.ui.upi

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private data class UpiSmsTransaction(val sender: String, val amount: String, val date: String, val message: String, val payeeName: String?)
private data class WalletOption(val id: String, val name: String)
private data class RegisteredUpi(val id: String, val name: String, val categoryName: String, val categoryIcon: String, val categoryColor: String, val walletName: String)

@Composable
fun UPITransactionsScreen(onAddTransaction: (String, String) -> Unit = { _, _ -> }) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    var transactions by remember { mutableStateOf<List<UpiSmsTransaction>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }
    var registerTransaction by remember { mutableStateOf<UpiSmsTransaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var registeredUpis by remember { mutableStateOf<List<RegisteredUpi>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    fun loadTransactions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return
        val result = mutableListOf<UpiSmsTransaction>()
        val projection = arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE)
        context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, Telephony.Sms.Inbox.DATE + " DESC")?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS); val bodyIndex = it.getColumnIndex(Telephony.Sms.Inbox.BODY); val dateIndex = it.getColumnIndex(Telephony.Sms.Inbox.DATE)
            while (it.moveToNext() && result.size < 200) {
                val body = if (bodyIndex >= 0) it.getString(bodyIndex).orEmpty() else ""
                val lower = body.lowercase(Locale.ROOT)
                val isUpi = listOf("upi", "vpa", "@ok", "@ybl", "@paytm", "@ibl", "@axis", "@sbi", "@hdfc", "@icici", "bhim").any(lower::contains)
                val isPayment = listOf("debited", "credited", "paid", "payment", "transaction", "sent", "received").any(lower::contains)
                if (!isUpi || !isPayment) continue
                val amount = extractAmount(body) ?: continue
                val sender = if (addressIndex >= 0) it.getString(addressIndex).orEmpty() else ""
                val time = if (dateIndex >= 0) it.getLong(dateIndex) else 0L
                result.add(UpiSmsTransaction(sender, amount, SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time)), body, extractPayeeName(body)))
            }
        }
        transactions = result
    }

    fun loadRegistered() {
        if (uid == null) return
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi").addSnapshotListener { snapshot, _ ->
            registeredUpis = snapshot?.documents?.map { d -> RegisteredUpi(d.id, d.getString("upiName") ?: "", d.getString("categoryName") ?: "", d.getString("categoryIcon") ?: "", d.getString("categoryColorHex") ?: "#6750A4", d.getString("walletName") ?: "") }?.sortedBy { it.name.lowercase(Locale.ROOT) } ?: emptyList()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permissionDenied = !granted; if (granted) loadTransactions() }
    LaunchedEffect(Unit) { loadRegistered(); if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) loadTransactions() else permissionLauncher.launch(Manifest.permission.READ_SMS) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Messages") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Registered List") })
        }
        if (selectedTab == 0) {
            when {
                permissionDenied -> Text("SMS permission is required to show UPI transactions.", Modifier.padding(16.dp))
                transactions.isEmpty() -> Text("No UPI transactions found in SMS.", Modifier.padding(16.dp))
                else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(transactions) { transaction ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text("₹${transaction.amount}", style = MaterialTheme.typography.titleLarge)
                                Text(transaction.date, style = MaterialTheme.typography.bodySmall)
                                Text(transaction.payeeName ?: transaction.sender, fontWeight = FontWeight.Bold)
                                Text(transaction.message, Modifier.padding(top = 6.dp))
                                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button({ onAddTransaction(transaction.amount, transaction.message) }, Modifier.weight(1f)) { Text("Add Transaction") }
                                    Button({ registerTransaction = transaction }, Modifier.weight(1f)) { Text("Register UPI") }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (registeredUpis.isEmpty()) Text("No registered UPI found.", Modifier.padding(16.dp))
            else LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(registeredUpis, key = { it.id }) { item ->
                    val iconColor = CategoryIconHelper.parseColor(item.categoryColor)
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(CategoryIconHelper.getIcon(item.categoryIcon), item.categoryName, Modifier.size(30.dp), iconColor)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${item.categoryName} • ${item.walletName}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { if (uid != null) scope.launch { FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi").document(item.id).delete().await() } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
    registerTransaction?.let { transaction -> RegisterUpiDialog(transaction.payeeName ?: "", database) { registerTransaction = null } }
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
        if (uid != null) FirebaseFirestore.getInstance().collection("users").document(uid).collection("wallets").addSnapshotListener { snapshot, _ -> wallets = snapshot?.documents?.map { WalletOption(it.id, it.getString("name") ?: "") }?.filter { it.name.isNotBlank() } ?: emptyList(); if (selectedWallet == null || wallets.none { it.id == selectedWallet?.id }) selectedWallet = wallets.firstOrNull() }
    }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Register UPI") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(upiName, { upiName = it }, singleLine = true, label = { Text("UPI Name") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
            Text("Select Category", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { categories.forEach { category ->
                val selected = selectedCategory?.id == category.id; val color = CategoryIconHelper.parseColor(category.colorHex)
                Surface(shape = RoundedCornerShape(12.dp), color = if (selected) color.copy(alpha = .2f) else MaterialTheme.colorScheme.surface, modifier = Modifier.border(if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).clickable { selectedCategory = category }) { Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(CategoryIconHelper.getIcon(category.iconName), category.name, Modifier.size(20.dp), color); Spacer(Modifier.width(8.dp)); Text(category.name) } }
            } }
            Text("Select Wallet", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { wallets.forEach { wallet ->
                val selected = selectedWallet?.id == wallet.id
                Surface(shape = RoundedCornerShape(12.dp), color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .15f) else MaterialTheme.colorScheme.surface, modifier = Modifier.border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).clickable { selectedWallet = wallet }) { Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalanceWallet, wallet.name, Modifier.size(20.dp), MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(wallet.name) } }
            } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !saving, onClick = {
        val name = upiName.trim(); val category = selectedCategory; val wallet = selectedWallet
        when { name.isEmpty() -> error = "UPI name is required"; category == null -> error = "Select a category"; wallet == null -> error = "Select a wallet"; else -> { saving = true; scope.launch { try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("User is not logged in")
            val collection = FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi")
            val existing = collection.whereEqualTo("upiName", name).limit(1).get().await()
            val data = hashMapOf<String, Any>("upiName" to name, "categoryId" to category.id, "categoryName" to category.name, "categoryIcon" to category.iconName, "categoryColorHex" to category.colorHex, "walletId" to wallet.id, "walletName" to wallet.name, "updatedAt" to com.google.firebase.Timestamp.now())
            if (existing.documents.isNotEmpty()) existing.documents.first().reference.set(data, com.google.firebase.firestore.SetOptions.merge()).await() else { data["createdAt"] = com.google.firebase.Timestamp.now(); collection.add(data).await() }
            onDismiss()
        } catch (e: Exception) { error = e.message ?: "Unable to save UPI"; saving = false } } } }
    }) { Text(if (saving) "Saving..." else "Save") } }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

private fun extractAmount(message: String): String? { val matcher = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE).matcher(message); return if (matcher.find()) matcher.group(1)?.replace(",", "") else null }
private fun extractPayeeName(message: String): String? {
    val patterns = listOf(
        Pattern.compile("\\bto\\s*[:\\-]?\\s*([A-Za-z][A-Za-z0-9 .&'_-]{1,60}?)(?=\\s+(?:via|using|on|for|ref|upi|vpa)\\b|[,.]|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpaid\\s+to\\s*[:\\-]?\\s*([A-Za-z][A-Za-z0-9 .&'_-]{1,60}?)(?=\\s+(?:via|using|on|for|ref|upi|vpa)\\b|[,.]|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsent\\s+to\\s*[:\\-]?\\s*([A-Za-z][A-Za-z0-9 .&'_-]{1,60}?)(?=\\s+(?:via|using|on|for|ref|upi|vpa)\\b|[,.]|$)", Pattern.CASE_INSENSITIVE)
    )
    for (pattern in patterns) { val matcher = pattern.matcher(message); if (matcher.find()) { val value = matcher.group(1)?.trim()?.replace(Regex("\\s+"), " "); if (!value.isNullOrBlank() && !value.equals("upi", true) && !value.equals("vpa", true)) return value } }
    return null
}
