package com.example.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TransferScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val wallets = remember { mutableStateListOf<Wallet>() }
    var fromWallet by remember { mutableStateOf<Wallet?>(null) }
    var toWallet by remember { mutableStateOf<Wallet?>(null) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val collection = remember(uid) {
        uid?.let { FirebaseFirestore.getInstance().collection("users").document(it).collection("wallets") }
    }

    LaunchedEffect(uid) {
        collection?.addSnapshotListener { snapshot, _ ->
            wallets.clear()
            snapshot?.documents?.mapNotNull { doc ->
                Wallet(doc.id, doc.getString("name") ?: "", doc.getDouble("balance") ?: 0.0)
            }?.let(wallets::addAll)
        }
    }

    LaunchedEffect(wallets.size) {
        if (fromWallet == null) fromWallet = wallets.firstOrNull()
        if (toWallet == null) toWallet = wallets.firstOrNull { it.id != fromWallet?.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Text(
                "Move money between wallets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        androidx.compose.material3.Text("From Wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        WalletSelector(wallets, fromWallet) {
            fromWallet = it
            if (toWallet?.id == it.id) toWallet = wallets.firstOrNull { w -> w.id != it.id }
        }

        androidx.compose.material3.Text("To Wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        WalletSelector(wallets.filter { it.id != fromWallet?.id }, toWallet) { toWallet = it }

        androidx.compose.material3.Text("Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = amount,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    amount = it
                    error = null
                }
            },
            placeholder = { androidx.compose.material3.Text("0.00") },
            prefix = { androidx.compose.material3.Text("₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            androidx.compose.material3.Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = !saving,
            onClick = {
                val value = amount.toDoubleOrNull()
                when {
                    fromWallet == null -> error = "Select the wallet to transfer from"
                    toWallet == null -> error = "Select the wallet to transfer to"
                    value == null || value <= 0.0 -> error = "Enter a valid amount"
                    value > fromWallet!!.balance -> error = "Insufficient balance in ${fromWallet!!.name}"
                    else -> {
                        saving = true
                        error = null
                        WalletBalanceManager.transfer(
                            fromWalletId = fromWallet!!.id,
                            toWalletId = toWallet!!.id,
                            amount = value,
                            onSuccess = { saving = false; onNavigateBack() },
                            onError = { saving = false; error = "Transfer failed. Please try again." }
                        )
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Text("Transfer Amount", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WalletSelector(wallets: List<Wallet>, selected: Wallet?, onSelect: (Wallet) -> Unit) {
    if (wallets.isEmpty()) {
        androidx.compose.material3.Text("No other wallet available")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            wallets.forEach { wallet ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = if (selected?.id == wallet.id) 3.dp else 1.dp,
                    color = if (selected?.id == wallet.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(wallet) }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            androidx.compose.material3.Text(wallet.name, fontWeight = FontWeight.Bold)
                            androidx.compose.material3.Text(
                                "Balance: ₹${String.format("%.2f", wallet.balance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
