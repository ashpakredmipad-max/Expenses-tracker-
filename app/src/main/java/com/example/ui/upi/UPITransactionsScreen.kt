package com.example.ui.upi

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

private data class UpiSmsTransaction(
    val sender: String,
    val amount: String,
    val date: String,
    val message: String
)

@Composable
fun UPITransactionsScreen() {
    val context = LocalContext.current
    var transactions by remember { mutableStateOf<List<UpiSmsTransaction>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }

    fun loadTransactions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val result = mutableListOf<UpiSmsTransaction>()
        val projection = arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE)
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.Inbox.DATE + " DESC"
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.Inbox.DATE)
            while (it.moveToNext() && result.size < 200) {
                val body = if (bodyIndex >= 0) it.getString(bodyIndex).orEmpty() else ""
                val lower = body.lowercase(Locale.ROOT)
                val isUpi = listOf("upi", "vpa", "@ok", "@ybl", "@paytm", "@ibl", "@axis", "@sbi", "@hdfc", "@icici", "bhim")
                    .any { keyword -> lower.contains(keyword) }
                val isPayment = listOf("debited", "credited", "paid", "payment", "transaction", "sent", "received")
                    .any { keyword -> lower.contains(keyword) }
                if (!isUpi || !isPayment) continue

                val amount = extractAmount(body) ?: continue
                val sender = if (addressIndex >= 0) it.getString(addressIndex).orEmpty() else ""
                val time = if (dateIndex >= 0) it.getLong(dateIndex) else 0L
                val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time))
                result.add(UpiSmsTransaction(sender, amount, date, body))
            }
        }
        transactions = result
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) loadTransactions()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            loadTransactions()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        when {
            permissionDenied -> {
                Text(
                    text = "SMS permission is required to show UPI transactions.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            transactions.isEmpty() -> {
                Text(
                    text = "No UPI transactions found in SMS.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions) { transaction ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "₹${transaction.amount}", style = MaterialTheme.typography.titleLarge)
                                Text(text = transaction.date, style = MaterialTheme.typography.bodySmall)
                                if (transaction.sender.isNotBlank()) {
                                    Text(text = transaction.sender, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = transaction.message,
                                    modifier = Modifier.padding(top = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun extractAmount(message: String): String? {
    val pattern = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(message)
    return if (matcher.find()) matcher.group(1)?.replace(",", "") else null
}
