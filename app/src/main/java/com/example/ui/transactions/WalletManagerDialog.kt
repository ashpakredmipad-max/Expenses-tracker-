package com.example.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun WalletManagerDialog(onDismiss: () -> Unit) {
    val wallets = remember { mutableStateListOf<Wallet>() }
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var deleteWallet by remember { mutableStateOf<Wallet?>(null) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val collection = remember(uid) { uid?.let { FirebaseFirestore.getInstance().collection("users").document(it).collection("wallets") } }

    LaunchedEffect(uid) {
        collection?.addSnapshotListener { snapshot, _ ->
            wallets.clear()
            snapshot?.documents?.mapNotNull { doc -> Wallet(doc.id, doc.getString("name") ?: "", doc.getDouble("balance") ?: 0.0) }?.let(wallets::addAll)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wallets") },
        text = {
            Column {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(wallets, key = { it.id }) { wallet ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(wallet.name, Modifier.weight(1f))
                            IconButton(onClick = { editingWallet = wallet; name = wallet.name; showEditor = true }) { Icon(Icons.Default.Edit, "Edit") }
                            IconButton(onClick = { deleteWallet = wallet }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { editingWallet = null; name = ""; showEditor = true }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Text(" Add Wallet")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingWallet == null) "Add Wallet" else "Edit Wallet") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Wallet name") }) },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        val ref = editingWallet?.id?.let { collection?.document(it) } ?: collection?.document()
                        ref?.set(mapOf("name" to trimmed, "balance" to (editingWallet?.balance ?: 0.0)))
                    }
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }

    deleteWallet?.let { wallet ->
        AlertDialog(
            onDismissRequest = { deleteWallet = null },
            title = { Text("Delete wallet?") },
            text = { Text("Delete ${wallet.name}?") },
            confirmButton = { TextButton(onClick = { collection?.document(wallet.id)?.delete(); deleteWallet = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteWallet = null }) { Text("Cancel") } }
        )
    }
}
