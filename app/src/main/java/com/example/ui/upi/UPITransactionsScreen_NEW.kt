package com.example.ui.upi

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            // Categories are read from the user's existing Firestore categories.
            val categorySnap = db.collection("users").document(uid).collection("categories").get().await()
            categories = categorySnap.documents.map { d ->
                mapOf("id" to d.id, "name" to d.getString("name"), "icon" to d.getString("icon"), "color" to d.getString("color"), "type" to d.getString("type"))
            }.filter { it["type"]?.toString()?.uppercase() == "EXPENSE" }
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
                OutlinedTextField(
                    value = upiName,
                    onValueChange = { upiName = it },
                    label = { Text("UPI Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category")
                var categoryMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategory?.get("name")?.toString() ?: "Select Category")
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category["name"]?.toString() ?: "") },
                                onClick = { selectedCategory = category; categoryMenu = false }
                            )
                        }
                    }
                }

                Text("Wallet")
                var walletMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { walletMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(selectedWallet?.get("name")?.toString() ?: "Select Wallet")
                    }
                    DropdownMenu(expanded = walletMenu, onDismissRequest = { walletMenu = false }) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet["name"]?.toString() ?: "") },
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                onClick = { selectedWallet = wallet; walletMenu = false }
                            )
                        }
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
                        val db = FirebaseFirestore.getInstance()
                        val ref = db.collection("users").document(uid).collection("registered_upi").document()
                        ref.set(mapOf(
                            "upiName" to upiName.trim(),
                            "categoryId" to category["id"],
                            "categoryName" to category["name"],
                            "categoryIcon" to category["icon"],
                            "categoryColor" to category["color"],
                            "walletId" to wallet["id"],
                            "walletName" to wallet["name"],
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )).await()
                        onDismiss()
                    } catch (e: Exception) {
                        error = e.message ?: "Unable to save UPI"
                        saving = false
                    }
                }
            }) { Text(if (saving) "Saving..." else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}
