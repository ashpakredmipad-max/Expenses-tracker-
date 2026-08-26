package com.example.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun WalletManagerDialog(onDismiss: () -> Unit) {
    val wallets = remember { mutableStateListOf<Wallet>() }
    var name by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wallets") },
        text = {
            Column {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(wallets, key = { it.id }) { wallet ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(wallet.name, modifier = Modifier.padding(8.dp))
                            Row {
                                IconButton(onClick = {
                                    editingId = wallet.id
                                    name = wallet.name
                                    showEditor = true
                                }) { Icon(Icons.Default.Edit, "Edit") }
                                IconButton(onClick = { wallets.remove(wallet) }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    editingId = null
                    name = ""
                    showEditor = true
                }) { Text("+ Add Wallet") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingId == null) "Add Wallet" else "Edit Wallet") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Wallet name") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isNotEmpty()) {
                        val id = editingId
                        if (id == null) wallets.add(Wallet(UUID.randomUUID().toString(), cleanName))
                        else {
                            val index = wallets.indexOfFirst { it.id == id }
                            if (index >= 0) wallets[index] = wallets[index].copy(name = cleanName)
                        }
                        showEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }
}
