package com.example.ui.upi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.ExpenseRed
import com.example.utils.CategoryIconHelper
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

private data class UpiTag(
    val id: String,
    val tagName: String,
    val iconName: String,
    val colorHex: String,
    val recipient: String
)

@Composable
fun UPITransactionsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var smsList by remember { mutableStateOf<List<UpiSms>>(emptyList()) }
    var upiTags by remember { mutableStateOf<List<UpiTag>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) }
    var selectedSms by remember { mutableStateOf<UpiSms?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    fun loadTags() {
        scope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).collection("upi_tags").get().await()
                upiTags = snapshot.documents.mapNotNull { doc ->
                    val recipient = doc.getString("recipient")?.trim()
                    val tagName = doc.getString("tagName")?.trim()
                    if (recipient.isNullOrBlank() || tagName.isNullOrBlank()) null
                    else UpiTag(
                        id = doc.id,
                        tagName = tagName,
                        iconName = doc.getString("iconName") ?: "Category",
                        colorHex = doc.getString("colorHex") ?: "#00897B",
                        recipient = recipient
                    )
                }
            } catch (_: Exception) { }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) smsList = readUpiSms(context)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) smsList = readUpiSms(context)
        loadTags()
    }

    LaunchedEffect(showTagDialog) {
        if (!showTagDialog) loadTags()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Messages") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tags") })
        }
        Spacer(Modifier.height(12.dp))

        if (selectedTab == 1) {
            if (upiTags.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No UPI tags added")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(upiTags, key = { it.id }) { tag ->
                        val tagColor = CategoryIconHelper.parseColor(tag.colorHex)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(tagColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        CategoryIconHelper.getIcon(tag.iconName),
                                        contentDescription = tag.tagName,
                                        tint = tagColor,
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tag.tagName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(2.dp))
                                    Text(tag.recipient, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                                        try {
                                            FirebaseFirestore.getInstance()
                                                .collection("users").document(uid)
                                                .collection("upi_tags").document(tag.id).delete().await()
                                            upiTags = upiTags.filterNot { it.id == tag.id }
                                            message = "Tag deleted"
                                        } catch (e: Exception) {
                                            message = e.message ?: "Unable to delete tag"
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete tag", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        } else if (!hasPermission) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("SMS permission required", style = MaterialTheme.typography.titleMedium)
                    Text("Allow SMS access to show your UPI transaction messages.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) { Text("Allow SMS Access") }
                }
            }
        } else if (smsList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No UPI transaction SMS found.") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(smsList, key = { it.id }) { sms ->
                    val matchingTag = upiTags.firstOrNull { tagsMatch(it.recipient, sms.recipient) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(sms.recipient, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                sms.amount?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                            }

                            matchingTag?.let { tag ->
                                Spacer(Modifier.height(8.dp))
                                val tagColor = CategoryIconHelper.parseColor(tag.colorHex)
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(tagColor.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(CategoryIconHelper.getIcon(tag.iconName), contentDescription = tag.tagName, tint = tagColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tag.tagName, color = tagColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(sms.body, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { message = "Add Transaction feature is ready for the next step" }) { Text("Add Transaction") }
                                if (matchingTag == null) {
                                    Button(onClick = { selectedSms = sms; showTagDialog = true }) { Text("Tag UPI") }
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
            onDismiss = { showTagDialog = false; selectedSms = null },
            onSaved = { savedMessage -> message = savedMessage; showTagDialog = false; selectedSms = null }
        )
    }
}

private fun tagsMatch(savedRecipient: String, smsRecipient: String): Boolean {
    fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase()
    return normalize(savedRecipient) == normalize(smsRecipient)
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
            val amount = Regex("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1)?.let { "₹$it" }
            result += UpiSms(cursor.getString(idIndex), cursor.getString(addressIndex) ?: "Unknown", body, cursor.getLong(dateIndex), amount, extractRecipient(body))
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
        val value = pattern.find(body)?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.', ',')
        if (!value.isNullOrBlank() && !value.equals("HDFC Bank", true)) return value
    }
    return "Unknown"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagUpiDialog(sms: UpiSms, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var tagName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Restaurant") }
    var selectedColor by remember { mutableStateOf("#FF7043") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(sms.recipient, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = tagName, onValueChange = { tagName = it; error = null }, label = { Text("Tag Name") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall) }
                Text("Choose Icon", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryIconHelper.AVAILABLE_ICONS.take(16).forEach { (iconKey, vector) ->
                        val selected = selectedIcon == iconKey
                        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable { selectedIcon = iconKey }, contentAlignment = Alignment.Center) {
                            Icon(vector, contentDescription = iconKey, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Text("Choose Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryIconHelper.PRESET_COLORS.take(12).forEach { colorHex ->
                        val selected = selectedColor.equals(colorHex, ignoreCase = true)
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(CategoryIconHelper.parseColor(colorHex)).clickable { selectedColor = colorHex }, contentAlignment = Alignment.Center) {
                            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                if (tagName.isBlank()) { error = "Tag name cannot be empty"; return@Button }
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) { error = "User is not logged in"; return@Button }
                saving = true
                scope.launch {
                    try {
                        FirebaseFirestore.getInstance().collection("users").document(uid).collection("upi_tags").document(sms.id)
                            .set(mapOf("tagName" to tagName.trim(), "iconName" to selectedIcon, "colorHex" to selectedColor, "recipient" to sms.recipient, "sender" to sms.sender, "amount" to sms.amount, "body" to sms.body, "date" to sms.date, "updatedAt" to com.google.firebase.Timestamp.now())).await()
                        onSaved("UPI tag saved")
                    } catch (e: Exception) {
                        error = e.message ?: "Unable to save UPI tag"
                        saving = false
                    }
                }
            }) { Text(if (saving) "Saving..." else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}
