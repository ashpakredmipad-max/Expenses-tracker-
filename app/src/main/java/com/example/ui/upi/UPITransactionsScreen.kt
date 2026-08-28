package com.example.ui.upi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    val amount: String?,
    val recipient: String
)

private val tagIcons = listOf(
    Icons.Default.LocalOffer, Icons.Default.Fastfood, Icons.Default.ShoppingCart,
    Icons.Default.LocalGasStation, Icons.Default.Home, Icons.Default.MedicalServices,
    Icons.Default.Movie, Icons.Default.School, Icons.Default.Flight,
    Icons.Default.Wifi, Icons.Default.Power, Icons.Default.AccountBalanceWallet,
    Icons.Default.Restaurant, Icons.Default.DirectionsCar, Icons.Default.PhoneAndroid,
    Icons.Default.CardGiftcard
)

@Composable
fun UPITransactionsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var smsList by remember { mutableStateOf<List<UpiSms>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) }
    var selectedSms by remember { mutableStateOf<UpiSms?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) smsList = readUpiSms(context)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) smsList = readUpiSms(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                                Text(sms.recipient, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                sms.amount?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(sms.body, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { message = "Add Transaction feature is ready for the next step" }) {
                                    Text("Add Transaction")
                                }
                                Button(onClick = {
                                    selectedSms = sms
                                    showTagDialog = true
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

    if (showTagDialog && selectedSms != null) {
        TagUpiDialog(
            sms = selectedSms!!,
            onDismiss = {
                showTagDialog = false
                selectedSms = null
            },
            onSaved = { savedMessage ->
                message = savedMessage
                showTagDialog = false
                selectedSms = null
            }
        )
    }
}

private fun readUpiSms(context: Context): List<UpiSms> {
    val result = mutableListOf<UpiSms>()
    val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
    val selection = "LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ?"
    val args = arrayOf("%upi%", "%debited%", "%credited%", "%transaction%")
    context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, selection, args, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
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
                amount = amount,
                recipient = extractRecipient(body)
            )
        }
    }
    return result
}

private fun extractRecipient(body: String): String {
    val patterns = listOf(
        Regex("(?i)\\bto\\s+(.+?)(?:\\s+on\\s+|\\s+ref(?:erence)?\\s*|\\s+upi\\s*ref|$)"),
        Regex("(?i)\\bto\\s*[:\\-]?\\s*(.+?)(?:\\.|\\n|$)")
    )
    for (pattern in patterns) {
        val match = pattern.find(body)
        val value = match?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.', ',')
        if (!value.isNullOrBlank() && !value.equals("HDFC Bank", true)) return value
    }
    return "Unknown"
}

@Composable
private fun TagUpiDialog(
    sms: UpiSms,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var tagName by remember { mutableStateOf(sms.recipient) }
    var selectedIcon by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        shape = RoundedCornerShape(32.dp),
        title = { Text("Tag UPI", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                )
                Text("Choose Icon", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tagIcons.chunked(4).forEachIndexed { rowIndex, row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEachIndexed { colIndex, icon ->
                                val index = rowIndex * 4 + colIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selectedIcon == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = { selectedIcon = index }) {
                                        Icon(icon, contentDescription = null, tint = if (selectedIcon == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    if (tagName.trim().isEmpty()) {
                        error = "Tag name is required"
                        return@Button
                    }
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        error = "User is not logged in"
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("users").document(uid)
                                .collection("upi_tags").document(sms.id)
                                .set(mapOf(
                                    "tagName" to tagName.trim(),
                                    "iconIndex" to selectedIcon,
                                    "recipient" to sms.recipient,
                                    "sender" to sms.sender,
                                    "amount" to sms.amount,
                                    "body" to sms.body,
                                    "date" to sms.date,
                                    "updatedAt" to com.google.firebase.Timestamp.now()
                                )).await()
                            onSaved("UPI tag saved")
                        } catch (e: Exception) {
                            error = e.message ?: "Unable to save UPI tag"
                            saving = false
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp)
            ) { Text(if (saving) "Saving..." else "Save") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}
