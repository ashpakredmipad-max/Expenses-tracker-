package com.example.ui.upi

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class UpiSms(
    val id: String,
    val sender: String,
    val body: String,
    val date: Long,
    val amount: String?
)

@Composable
fun UPITransactionsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var smsList by remember { mutableStateOf<List<UpiSms>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var selectedSms by remember { mutableStateOf<UpiSms?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) {
            smsList = readUpiSms(context)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) smsList = readUpiSms(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("UPI Transactions", style = MaterialTheme.typography.headlineSmall)
                Text("UPI transaction SMS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { showRegisterDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Register UPI")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!hasPermission) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("SMS permission required", style = MaterialTheme.typography.titleMedium)
                    Text("Allow SMS access to show your UPI transaction messages.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                        Text("Allow SMS Access")
                    }
                }
            }
        } else if (smsList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No UPI transaction SMS found.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(smsList, key = { it.id }) { sms ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(sms.sender, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                sms.amount?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(sms.body, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    selectedSms = sms
                                    showRegisterDialog = true
                                }) {
                                    Text("Add Transaction")
                                }
                                Button(onClick = {
                                    scope.launch {
                                        try {
                                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                                .collection("upi_sms") .document(sms.id)
                                                .set(mapOf(
                                                    "sender" to sms.sender,
                                                    "body" to sms.body,
                                                    "date" to sms.date,
                                                    "amount" to sms.amount,
                                                    "taggedAsUpi" to true,
                                                    "updatedAt" to com.google.firebase.Timestamp.now()
                                                )).await()
                                            message = "UPI tag saved"
                                        } catch (e: Exception) {
                                            message = e.message ?: "Unable to tag UPI"
                                        }
                                    }
                                }) {
                                    Text("Tag UPI")
                                }
                            }
                        }
                    }
                }
            }
        }

        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
    }

    if (showRegisterDialog) {
        RegisterUpiDialogNew(
            initialUpiName = selectedSms?.sender ?: "",
            onDismiss = {
                showRegisterDialog = false
                selectedSms = null
            }
        )
    }
}

private fun readUpiSms(context: android.content.Context): List<UpiSms> {
    val result = mutableListOf<UpiSms>()
    val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
    val selection = "LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ?"
    val args = arrayOf("%upi%", "%debited%", "%credited%", "%transaction%")
    context.contentResolver.query(
        Telephony.Sms.CONTENT_URI, projection, selection, args, "${Telephony.Sms.DATE} DESC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
        val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
        while (cursor.moveToNext()) {
            val body = cursor.getString(bodyIndex) ?: continue
            val amount = Regex("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
                .find(body)?.groupValues?.getOrNull(1)?.let { "₹$it" }
            result += UpiSms(
                id = cursor.getString(idIndex),
                sender = cursor.getString(addressIndex) ?: "Unknown",
                body = body,
                date = cursor.getLong(dateIndex),
                amount = amount
            )
        }
    }
    return result
}

@Composable
fun RegisterUpiDialogNew(
    initialUpiName: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var upiName by remember { mutableStateOf(initialUpiName) }
    var categories by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var wallets by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var selectedWallet by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val categorySnap = db.collection("users").document(uid).collection("categories").get().await()
            categories = categorySnap.documents.map { d -> mapOf("id" to d.id, "name" to d.getString("name"), "icon" to d.getString("icon"), "color" to d.getString("color"), "type" to d.getString("type")) }
                .filter { it["type"]?.toString()?.uppercase() == "EXPENSE" }
            selectedCategory = categories.firstOrNull()
            val walletSnap = db.collection("users").document(uid).collection("wallets").get().await()
            wallets = walletSnap.documents.map { d -> mapOf("id" to d.id, "name" to d.getString("name")) }.filter { !it["name"].isNullOrBlank() }
            selectedWallet = wallets.firstOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Register UPI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = upiName, onValueChange = { upiName = it }, label = { Text("UPI Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Category")
                var categoryMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedCategory?.get("name")?.toString() ?: "Select Category") }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        categories.forEach { category -> DropdownMenuItem(text = { Text(category["name"]?.toString() ?: "") }, onClick = { selectedCategory = category; categoryMenu = false }) }
                    }
                }
                Text("Wallet")
                var walletMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { walletMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(selectedWallet?.get("name")?.toString() ?: "Select Wallet")
                    }
                    DropdownMenu(expanded = walletMenu, onDismissRequest = { walletMenu = false }) {
                        wallets.forEach { wallet -> DropdownMenuItem(text = { Text(wallet["name"]?.toString() ?: "") }, onClick = { selectedWallet = wallet; walletMenu = false }) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val category = selectedCategory
                val wallet = selectedWallet
                if (upiName.trim().isEmpty()) { error = "UPI name is required"; return@TextButton }
                if (category == null) { error = "Select a category"; return@TextButton }
                if (wallet == null) { error = "Select a wallet"; return@TextButton }
                if (uid == null) { error = "User is not logged in"; return@TextButton }
                saving = true
                scope.launch {
                    try {
                        FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi").document()
                            .set(mapOf("upiName" to upiName.trim(), "categoryId" to category["id"], "categoryName" to category["name"], "categoryIcon" to category["icon"], "categoryColor" to category["color"], "walletId" to wallet["id"], "walletName" to wallet["name"], "updatedAt" to com.google.firebase.Timestamp.now())).await()
                        onDismiss()
                    } catch (e: Exception) { error = e.message ?: "Unable to save UPI"; saving = false }
                }
            }) { Text(if (saving) "Saving..." else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}
