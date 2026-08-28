package com.example.ui.upi

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.database.AppDatabase
import com.example.utils.CategoryIconHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

private data class UpiSmsTransaction(val sender: String, val amount: String, val date: String, val message: String, val payeeName: String?)
private data class TagOption(val id: String, val name: String, val icon: String, val color: String)
private data class RegisteredUpi(val id: String, val name: String, val tags: List<TagOption>)

@Composable
fun UPITransactionsScreen(onAddTransaction: (String, String) -> Unit = { _, _ -> }) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    var transactions by remember { mutableStateOf<List<UpiSmsTransaction>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }
    var registerTransaction by remember { mutableStateOf<UpiSmsTransaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var registeredUpis by remember { mutableStateOf<List<RegisteredUpi>>(emptyList()) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    fun loadTransactions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return
        val result = mutableListOf<UpiSmsTransaction>()
        val projection = arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE)
        context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, Telephony.Sms.Inbox.DATE + " DESC")?.use { cursor ->
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.Inbox.DATE)
            while (cursor.moveToNext() && result.size < 200) {
                val body = if (bodyIndex >= 0) cursor.getString(bodyIndex).orEmpty() else ""
                val lower = body.lowercase(Locale.ROOT)
                val isUpi = listOf("upi", "vpa", "@ok", "@ybl", "@paytm", "@ibl", "@axis", "@sbi", "@hdfc", "@icici", "bhim").any(lower::contains)
                val isPayment = listOf("debited", "credited", "paid", "payment", "transaction", "sent", "received").any(lower::contains)
                if (!isUpi || !isPayment) continue
                val amount = extractAmount(body) ?: continue
                val sender = if (addressIndex >= 0) cursor.getString(addressIndex).orEmpty() else ""
                val time = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L
                result.add(UpiSmsTransaction(sender, amount, SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time)), body, extractPayeeName(body)))
            }
        }
        transactions = result
    }

    LaunchedEffect(uid) {
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi")
                .addSnapshotListener { snapshot, _ ->
                    registeredUpis = snapshot?.documents?.map { d ->
                        val tags = (d.get("tags") as? List<*>)?.mapNotNull { raw ->
                            val map = raw as? Map<*, *> ?: return@mapNotNull null
                            TagOption(map["id"]?.toString().orEmpty(), map["name"]?.toString().orEmpty(), map["icon"]?.toString().orEmpty(), map["color"]?.toString() ?: "#006C50")
                        }?.filter { it.name.isNotBlank() } ?: emptyList()
                        RegisteredUpi(d.id, d.getString("upiName") ?: "", tags)
                    }?.sortedBy { it.name.lowercase(Locale.ROOT) } ?: emptyList()
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) loadTransactions()
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) loadTransactions()
        else permissionLauncher.launch(Manifest.permission.READ_SMS)
    }

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
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    if (uid != null) scope.launch {
                                        FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi").document(item.id).delete().await()
                                    }
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                            if (item.tags.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item.tags.forEach { tag ->
                                        val color = CategoryIconHelper.parseColor(tag.color)
                                        Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = .15f), modifier = Modifier.border(1.dp, color, RoundedCornerShape(12.dp))) {
                                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(CategoryIconHelper.getIcon(tag.icon), tag.name, tint = color, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp)); Text(tag.name, color = color, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
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
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var upiName by remember(initialName) { mutableStateOf(initialName) }
    var tags by remember { mutableStateOf<List<TagOption>>(emptyList()) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var showAddTag by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid != null) FirebaseFirestore.getInstance().collection("users").document(uid).collection("tags")
            .addSnapshotListener { snapshot, _ ->
                tags = snapshot?.documents?.map { d -> TagOption(d.id, d.getString("name") ?: "", d.getString("icon") ?: "Category", d.getString("color") ?: "#006C50") }
                    ?.filter { it.name.isNotBlank() }?.sortedBy { it.name.lowercase(Locale.ROOT) } ?: emptyList()
            }
    }

    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Register UPI") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(upiName, { upiName = it }, singleLine = true, label = { Text("UPI Name") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tags", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAddTag = true }, enabled = !saving) {
                    Icon(Icons.Default.Add, contentDescription = "Add Tag", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("New Tag")
                }
            }
            if (tags.isEmpty()) Text("No tags yet. Tap New Tag to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    val selected = selectedTags.contains(tag.id)
                    val color = CategoryIconHelper.parseColor(tag.color)
                    Surface(shape = RoundedCornerShape(12.dp), color = if (selected) color.copy(alpha = .22f) else MaterialTheme.colorScheme.surface, modifier = Modifier.border(if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).clickable(enabled = !saving) {
                        selectedTags = if (selected) selectedTags - tag.id else selectedTags + tag.id
                    }) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(CategoryIconHelper.getIcon(tag.icon), tag.name, tint = color, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(7.dp)); Text(tag.name, color = if (selected) color else MaterialTheme.colorScheme.onSurface, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !saving, onClick = {
            val name = upiName.trim()
            when {
                name.isEmpty() -> error = "UPI name is required"
                selectedTags.isEmpty() -> error = "Select at least one tag"
                uid == null -> error = "User is not logged in"
                else -> {
                    saving = true
                    scope.launch {
                        try {
                            val selected = tags.filter { selectedTags.contains(it.id) }
                            val tagData = selected.map { mapOf("id" to it.id, "name" to it.name, "icon" to it.icon, "color" to it.color) }
                            val collection = FirebaseFirestore.getInstance().collection("users").document(uid).collection("registered_upi")
                            val existing = collection.whereEqualTo("upiName", name).limit(1).get().await()
                            val data = hashMapOf<String, Any>("upiName" to name, "tags" to tagData, "updatedAt" to com.google.firebase.Timestamp.now())
                            if (existing.documents.isNotEmpty()) existing.documents.first().reference.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                            else { data["createdAt"] = com.google.firebase.Timestamp.now(); collection.add(data).await() }
                            onDismiss()
                        } catch (e: Exception) { error = e.message ?: "Unable to save UPI"; saving = false }
                    }
                }
            }
        }) { Text(if (saving) "Saving..." else "Save") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })

    if (showAddTag) AddTagDialog(uid = uid, onDismiss = { showAddTag = false })
}

@Composable
private fun AddTagDialog(uid: String?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(CategoryIconHelper.AVAILABLE_ICONS.first().first) }
    var selectedColor by remember { mutableStateOf(CategoryIconHelper.PRESET_COLORS.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Add New Tag") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Tag Name") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
            Text("Choose Icon", fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIconHelper.AVAILABLE_ICONS.forEach { (iconName, icon) ->
                    val selected = selectedIcon == iconName
                    Surface(shape = RoundedCornerShape(10.dp), color = if (selected) CategoryIconHelper.parseColor(selectedColor).copy(alpha = .2f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.border(if (selected) 2.dp else 1.dp, if (selected) CategoryIconHelper.parseColor(selectedColor) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).clickable { selectedIcon = iconName }) {
                        Icon(icon, contentDescription = iconName, tint = if (selected) CategoryIconHelper.parseColor(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(9.dp).size(22.dp))
                    }
                }
            }
            Text("Choose Color", fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIconHelper.PRESET_COLORS.forEach { hex ->
                    val selected = selectedColor == hex
                    val color = CategoryIconHelper.parseColor(hex)
                    Surface(shape = RoundedCornerShape(50), color = color, modifier = Modifier.size(30.dp).border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else color, RoundedCornerShape(50)).clickable { selectedColor = hex }) { }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !saving, onClick = {
            val cleanName = name.trim()
            when {
                cleanName.isEmpty() -> error = "Tag name is required"
                uid == null -> error = "User is not logged in"
                else -> {
                    saving = true
                    scope.launch {
                        try {
                            val collection = FirebaseFirestore.getInstance().collection("users").document(uid).collection("tags")
                            collection.add(mapOf("name" to cleanName, "icon" to selectedIcon, "color" to selectedColor, "createdAt" to com.google.firebase.Timestamp.now())).await()
                            onDismiss()
                        } catch (e: Exception) { error = e.message ?: "Unable to save tag"; saving = false }
                    }
                }
            }
        }) { Text(if (saving) "Saving..." else "Create") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

private fun extractAmount(message: String): String? {
    val matcher = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE).matcher(message)
    return if (matcher.find()) matcher.group(1)?.replace(",", "") else null
}

private fun extractPayeeName(message: String): String? {
    val patterns = listOf(
        Pattern.compile("\\bto\\s*[:\\-]?\\s*([A-Za-z][A-Za-z0-9 .&'_-]{1,60}?)(?=\\s+(?:via|using|on|for|ref|upi|vpa)\\b|[,.]|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpaid\\s+to\\s*[:\\-]?\\s*([A-Za-z][A-Za-z0-9 .&'_-]{1,60}?)(?=\\s+(?:via|using|on|for|ref|upi|vpa)\\b|[,.]|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsent\\s+to\\s*[:\\-]?\\s*([A-Za-z][A-Za-z0-9 .&'_-]{1,60}?)(?=\\s+(?:via|using|on|for|ref|upi|vpa)\\b|[,.]|$)", Pattern.CASE_INSENSITIVE)
    )
    for (pattern in patterns) {
        val matcher = pattern.matcher(message)
        if (matcher.find()) {
            val value = matcher.group(1)?.trim()?.replace(Regex("\\s+"), " ")
            if (!value.isNullOrBlank() && !value.equals("upi", true) && !value.equals("vpa", true)) return value
        }
    }
    return null
}
