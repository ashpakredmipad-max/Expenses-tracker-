@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val collection = remember(uid) {
        uid?.let { FirebaseFirestore.getInstance().collection("users").document(it).collection("wallets") }
    }

    LaunchedEffect(uid) {
        collection?.addSnapshotListener { snapshot, _ ->
            wallets.clear()
            snapshot?.documents?.mapNotNull { doc ->
                Wallet(id = doc.id, name = doc.getString("name") ?: "", balance = doc.getDouble("balance") ?: 0.0)
            }?.let(wallets::addAll)
        }
    }

    WalletManagerEditorDialog(
        wallets = wallets,
        editingWallet = editingWallet,
        showEditor = showEditor,
        name = name,
        deleteWallet = deleteWallet,
        collection = collection,
        onDismiss = onDismiss,
        onEdit = { wallet -> editingWallet = wallet; name = wallet.name; showEditor = true },
        onAdd = { editingWallet = null; name = ""; showEditor = true },
        onShowEditorChange = { showEditor = it },
        onNameChange = { name = it },
        onDeleteChange = { deleteWallet = it }
    )
}

@Composable
private fun WalletManagerEditorDialog(
    wallets: List<Wallet>,
    editingWallet: Wallet?,
    showEditor: Boolean,
    name: String,
    deleteWallet: Wallet?,
    collection: com.google.firebase.firestore.CollectionReference?,
    onDismiss: () -> Unit,
    onEdit: (Wallet) -> Unit,
    onAdd: () -> Unit,
    onShowEditorChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onDeleteChange: (Wallet?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wallets") },
        text = {
            Column {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(wallets, key = { it.id }) { wallet ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(wallet.name, modifier = Modifier.weight(1f).clickable { onEdit(wallet) })
                            IconButton(onClick = { onEdit(wallet) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                            IconButton(onClick = { onDeleteChange(wallet) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Add Wallet")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { onShowEditorChange(false) },
            title = { Text(if (editingWallet == null) "Add Wallet" else "Edit Wallet") },
            text = { OutlinedTextField(value = name, onValueChange = onNameChange, singleLine = true, label = { Text("Wallet name") }) },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        val ref = editingWallet?.id?.let { collection?.document(it) } ?: collection?.document()
                        ref?.set(mapOf("name" to trimmed, "balance" to (editingWallet?.balance ?: 0.0)))
                    }
                    onShowEditorChange(false)
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { onShowEditorChange(false) }) { Text("Cancel") } }
        )
    }

    deleteWallet?.let { wallet ->
        AlertDialog(
            onDismissRequest = { onDeleteChange(null) },
            title = { Text("Delete wallet?") },
            text = { Text("Delete ${wallet.name}?") },
            confirmButton = {
                TextButton(onClick = { collection?.document(wallet.id)?.delete(); onDeleteChange(null) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { onDeleteChange(null) }) { Text("Cancel") } }
        )
    }
}

@Composable
fun WalletManagerScreen(onNavigateBack: () -> Unit) {
    val wallets = remember { mutableStateListOf<Wallet>() }
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var deleteWallet by remember { mutableStateOf<Wallet?>(null) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val collection = remember(uid) {
        uid?.let { FirebaseFirestore.getInstance().collection("users").document(it).collection("wallets") }
    }

    LaunchedEffect(uid) {
        collection?.addSnapshotListener { snapshot, _ ->
            wallets.clear()
            snapshot?.documents?.mapNotNull { doc ->
                Wallet(id = doc.id, name = doc.getString("name") ?: "", balance = doc.getDouble("balance") ?: 0.0)
            }?.let(wallets::addAll)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Wallets", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingWallet = null; name = ""; showEditor = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Wallet")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(wallets, key = { it.id }) { wallet ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(wallet.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Balance: ₹${String.format("%.2f", wallet.balance)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { editingWallet = wallet; name = wallet.name; showEditor = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { deleteWallet = wallet }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingWallet == null) "Add Wallet" else "Edit Wallet") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Wallet name") }) },
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
