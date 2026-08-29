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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.entity.CategoryEntity
import com.example.ui.theme.ExpenseRed
import com.example.utils.CategoryIconHelper
import com.example.utils.CurrencyUtils
import com.example.ui.transactions.Wallet
import com.example.ui.transactions.WalletBalanceManager
import com.example.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class UpiSms(val id: String, val sender: String, val body: String, val date: Long, val amount: String?, val recipient: String, val isCredit: Boolean)
private data class UpiTag(val id: String, val tagName: String, val iconName: String, val colorHex: String, val recipient: String, val categoryId: Long?, val categoryName: String?)

@Composable
fun UPITransactionsScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    var smsList by remember { mutableStateOf<List<UpiSms>>(emptyList()) }
    var upiTags by remember { mutableStateOf<List<UpiTag>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) }
    var selectedSms by remember { mutableStateOf<UpiSms?>(null) }
    var selectedTagForTransaction by remember { mutableStateOf<UpiTag?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var savingTransaction by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val wallets = remember { mutableStateListOf<Wallet>() }

    fun loadTags() {
        scope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("users").document(uid).collection("upi_tags").get().await()
                upiTags = snapshot.documents.mapNotNull { doc ->
                    val recipient = doc.getString("recipient")?.trim()
                    val tagName = doc.getString("tagName")?.trim()
                    if (recipient.isNullOrBlank() || tagName.isNullOrBlank()) null else UpiTag(
                        doc.id, tagName, doc.getString("iconName") ?: "Category",
                        doc.getString("colorHex") ?: "#00897B", recipient,
                        doc.getLong("categoryId"), doc.getString("categoryName")?.trim()
                    )
                }
            } catch (_: Exception) { }
        }
    }

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val walletCollection = remember(uid) {
        uid?.let { FirebaseFirestore.getInstance().collection("users").document(it).collection("wallets") }
    }

    LaunchedEffect(uid) {
        walletCollection?.addSnapshotListener { snapshot, _ ->
            wallets.clear()
            snapshot?.documents?.mapNotNull { doc ->
                Wallet(doc.id, doc.getString("name") ?: "", doc.getDouble("balance") ?: 0.0)
            }?.let(wallets::addAll)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) smsList = readUpiSms(context)
    }
    LaunchedEffect(hasPermission) { if (hasPermission) smsList = readUpiSms(context); loadTags() }
    LaunchedEffect(showTagDialog) { if (!showTagDialog) loadTags() }

    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedMonth.time)
    val monthSms = remember(smsList, selectedMonth) { smsList.filter { isSameMonth(it.date, selectedMonth) } }

    val green = MaterialTheme.colorScheme.primary
    val pageBg = MaterialTheme.colorScheme.background
    val softGreen = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)

    Column(modifier = Modifier.fillMaxSize().background(pageBg)) {

        if (selectedTab == 1) {
            if (upiTags.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(52.dp), tint = green)
                    Spacer(Modifier.height(12.dp)); Text("No UPI tags yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tag a UPI message to see it here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
                items(upiTags, key = { it.id }) { tag ->
                    val tagColor = CategoryIconHelper.parseColor(tag.colorHex)
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(tagColor.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Icon(CategoryIconHelper.getIcon(tag.iconName), tag.tagName, tint = tagColor, modifier = Modifier.size(24.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tag.tagName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(2.dp)); Text(tag.recipient, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                tag.categoryName?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = green, modifier = Modifier.padding(top = 3.dp)) }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                                    try {
                                        FirebaseFirestore.getInstance().collection("users").document(userId).collection("upi_tags").document(tag.id).delete().await()
                                        upiTags = upiTags.filterNot { it.id == tag.id }; message = "Tag deleted"
                                    } catch (e: Exception) { message = e.message ?: "Unable to delete tag" }
                                }
                            }) { Icon(Icons.Default.DeleteOutline, "Delete tag", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        } else if (!hasPermission) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Sms, null, modifier = Modifier.size(32.dp), tint = green) }
                        Spacer(Modifier.height(16.dp)); Text("SMS permission required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp)); Text("Allow SMS access to show your UPI transaction messages.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(18.dp)); Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.LockOpen, null); Spacer(Modifier.width(8.dp)); Text("Allow SMS Access") }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    IconButton(onClick = { selectedMonth = (selectedMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) { Icon(Icons.Default.ChevronLeft, "Previous month") }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(monthTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("UPI messages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    IconButton(onClick = { selectedMonth = (selectedMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) { Icon(Icons.Default.ChevronRight, "Next month") }
                }
            }
            if (monthSms.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No UPI transaction SMS for $monthTitle") }
            else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)) {
                items(monthSms, key = { it.id }) { sms ->
                    val matchingTag = upiTags.firstOrNull { tagsMatch(it.recipient, sms.recipient) }
                    val alreadyAdded = isAlreadyAdded(sms, allTransactions)
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Sms, null, tint = green, modifier = Modifier.size(23.dp)) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(sms.recipient, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                    Text(SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(sms.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                                }
                                sms.amount?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (sms.isCredit) Color(0xFF2E7D32) else ExpenseRed) }
                            }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            Spacer(Modifier.height(9.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (sms.isCredit) Icons.Default.ArrowCircleDown else Icons.Default.ArrowCircleUp, null, tint = if (sms.isCredit) Color(0xFF2E7D32) else ExpenseRed, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text(if (sms.isCredit) "Credited ${sms.amount ?: "—"}" else "Sent ${sms.amount ?: "—"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (sms.isCredit) Color(0xFF2E7D32) else ExpenseRed) }
                                    Spacer(Modifier.height(7.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalance, null, tint = green, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("From HDFC Bank A/C *2668", style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                                }
                                Box(Modifier.width(1.dp).height(42.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PersonOutline, null, tint = green, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("To ${sms.recipient}", style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                                    Spacer(Modifier.height(7.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CalendarToday, null, tint = green, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("On ${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(sms.date)}", style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                            matchingTag?.let { tag ->
                                Spacer(Modifier.height(8.dp)); val tagColor = CategoryIconHelper.parseColor(tag.colorHex)
                                Row(Modifier.clip(RoundedCornerShape(10.dp)).background(tagColor.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(CategoryIconHelper.getIcon(tag.iconName), tag.tagName, tint = tagColor, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(tag.tagName, color = tagColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            if (alreadyAdded) {
                                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp), color = softGreen) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = green, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text("Added", color = green, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(enabled = matchingTag != null && !savingTransaction, onClick = {
                                        if (matchingTag == null) message = "Please tag this recipient first"
                                        else if (sms.amount == null) message = "Amount could not be read from this SMS"
                                        else if (wallets.isEmpty()) message = "Please add a wallet first"
                                        else { selectedSms = sms; selectedTagForTransaction = matchingTag; showWalletDialog = true }
                                    }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Add Transaction") }
                                    if (matchingTag == null) Button(onClick = { selectedSms = sms; showTagDialog = true }, modifier = Modifier.weight(0.9f).height(40.dp), shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Tag UPI") }
                                }
                            }
                        }
                    }
                }
            }
        }
        message?.let { Text(it, color = green, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold) }
    }

    if (showTagDialog && selectedSms != null) TagUpiDialog(selectedSms!!, expenseCategories, { showTagDialog = false; selectedSms = null }) { saved -> message = saved; showTagDialog = false; selectedSms = null }
    if (showWalletDialog && selectedSms != null && selectedTagForTransaction != null) {
        UpiWalletSelectionDialog(selectedSms!!, selectedTagForTransaction!!, wallets, expenseCategories, savingTransaction, { if (!savingTransaction) { showWalletDialog = false; selectedSms = null; selectedTagForTransaction = null } }) { wallet ->
            val sms = selectedSms ?: return@UpiWalletSelectionDialog
            val tag = selectedTagForTransaction ?: return@UpiWalletSelectionDialog
            if (isAlreadyAdded(sms, allTransactions)) { message = "This transaction is already added"; showWalletDialog = false; return@UpiWalletSelectionDialog }
            val category = expenseCategories.firstOrNull { tag.categoryId != null && it.id == tag.categoryId } ?: expenseCategories.firstOrNull { it.name.equals(tag.categoryName, true) }
            val paise = sms.amount?.let(::parseDisplayAmountToPaise)
            if (category == null) { message = "Category for this tag was not found"; return@UpiWalletSelectionDialog }
            if (paise == null || paise <= 0) { message = "Invalid transaction amount"; return@UpiWalletSelectionDialog }
            savingTransaction = true
            scope.launch {
                viewModel.saveTransaction(id = 0L, type = "EXPENSE", amountInPaise = paise, category = category, date = sms.date, note = "${sms.recipient} • ${tag.tagName}", upiSmsId = sms.id) {
                    WalletBalanceManager.applyTransaction(wallet.id, "EXPENSE", paise, {
                        savingTransaction = false; showWalletDialog = false; selectedSms = null; selectedTagForTransaction = null; message = "Transaction added to ${wallet.name}"
                    })
                }
            }
        }
    }
}

private fun isSameMonth(timestamp: Long, selected: Calendar): Boolean {
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return date.get(Calendar.YEAR) == selected.get(Calendar.YEAR) && date.get(Calendar.MONTH) == selected.get(Calendar.MONTH)
}

private fun isAlreadyAdded(sms: UpiSms, transactions: List<com.example.data.local.database.entity.TransactionEntity>): Boolean {
    val amount = sms.amount?.let(::parseDisplayAmountToPaise) ?: return false
    if (transactions.any { it.type.equals("EXPENSE", true) && it.upiSmsId == sms.id }) return true
    val smsDay = Calendar.getInstance().apply { timeInMillis = sms.date }
    return transactions.any {
        if (!it.type.equals("EXPENSE", true) || it.amountInPaise != amount || !it.note.contains(sms.recipient, true)) return@any false
        val txDay = Calendar.getInstance().apply { timeInMillis = it.date }
        txDay.get(Calendar.YEAR) == smsDay.get(Calendar.YEAR) && txDay.get(Calendar.DAY_OF_YEAR) == smsDay.get(Calendar.DAY_OF_YEAR)
    }
}

private fun tagsMatch(savedRecipient: String, smsRecipient: String): Boolean = savedRecipient.trim().replace(Regex("\\s+"), " ").equals(smsRecipient.trim().replace(Regex("\\s+"), " "), true)

private fun parseDisplayAmountToPaise(value: String): Long? = CurrencyUtils.parseAmountToPaise(value.replace("₹", "").replace(",", "").trim())

private fun readUpiSms(context: Context): List<UpiSms> {
    val result = mutableListOf<UpiSms>()
    val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
    val selection = "LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ? OR LOWER(${Telephony.Sms.BODY}) LIKE ?"
    val args = arrayOf("%upi%", "%debited%", "%credited%", "%transaction%")
    context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, selection, args, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID); val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY); val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
        while (cursor.moveToNext()) {
            val body = cursor.getString(bodyIndex) ?: continue
            val amount = Regex("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1)?.let { "₹$it" }
            result += UpiSms(cursor.getString(idIndex), cursor.getString(addressIndex) ?: "Unknown", body, cursor.getLong(dateIndex), amount, extractRecipient(body), isCredit = isCreditSms(body))
        }
    }
    return result
}

private fun isCreditSms(body: String): Boolean {
    val hasCredit = body.contains("credit", ignoreCase = true)
    val hasSent = body.contains("sent", ignoreCase = true)
    return hasCredit && !hasSent
}

private fun extractRecipient(body: String): String {
    val patterns = listOf(Regex("(?i)\\bto\\s+(.+?)(?:\\s+on\\s+|\\s+ref(?:erence)?\\s*|\\s+upi\\s*ref|$)"), Regex("(?i)\\bto\\s*[:\\-]?\\s*(.+?)(?:\\.|\\n|$)"))
    for (pattern in patterns) {
        val value = pattern.find(body)?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.', ',')
        if (!value.isNullOrBlank() && !value.equals("HDFC Bank", true)) return value
    }
    return "Unknown"
}

@Composable
private fun UpiWalletSelectionDialog(sms: UpiSms, tag: UpiTag, wallets: List<Wallet>, categories: List<CategoryEntity>, saving: Boolean, onDismiss: () -> Unit, onWalletSelected: (Wallet) -> Unit) {
    val categoryName = categories.firstOrNull { tag.categoryId != null && it.id == tag.categoryId }?.name ?: tag.categoryName ?: "Unknown"
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Select Wallet", fontWeight = FontWeight.Bold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(sms.recipient, fontWeight = FontWeight.SemiBold); Text("Amount: ${sms.amount ?: "—"}"); Text("Category: $categoryName", color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp))
            wallets.forEach { wallet ->
                Surface(Modifier.fillMaxWidth().clickable(enabled = !saving) { onWalletSelected(wallet) }, shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(wallet.name, fontWeight = FontWeight.SemiBold); Text("Balance: ₹${String.format("%.2f", wallet.balance)}", style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
            if (saving) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Adding transaction...") }
        }
    }, confirmButton = {}, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagUpiDialog(sms: UpiSms, categories: List<CategoryEntity>, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val scope = rememberCoroutineScope(); var tagName by remember { mutableStateOf("") }; var selectedIcon by remember { mutableStateOf("Restaurant") }; var selectedColor by remember { mutableStateOf("#FF7043") }; var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }; var categoryMenuExpanded by remember { mutableStateOf(false) }; var saving by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(sms.recipient, fontWeight = FontWeight.Bold) }, text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(tagName, { tagName = it; error = null }, label = { Text("Tag Name") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            Box { OutlinedButton(onClick = { categoryMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(selectedCategory?.name ?: "Select Expense Category", Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null) }; DropdownMenu(categoryMenuExpanded, { categoryMenuExpanded = false }) { categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { selectedCategory = category; categoryMenuExpanded = false }) } } }
            error?.let { Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall) }
            Text("Choose Icon", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { CategoryIconHelper.AVAILABLE_ICONS.take(16).forEach { (key, vector) -> val selected = selectedIcon == key; Box(Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable { selectedIcon = key }, contentAlignment = Alignment.Center) { Icon(vector, key, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) } } }
            Text("Choose Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { CategoryIconHelper.PRESET_COLORS.take(12).forEach { colorHex -> val selected = selectedColor.equals(colorHex, true); Box(Modifier.size(34.dp).clip(CircleShape).background(CategoryIconHelper.parseColor(colorHex)).clickable { selectedColor = colorHex }, contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) } } }
        }
    }, confirmButton = { Button(enabled = !saving, onClick = {
        if (tagName.isBlank()) { error = "Tag name cannot be empty"; return@Button }; if (selectedCategory == null) { error = "Please select an expense category"; return@Button }; val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { error = "User is not logged in"; return@Button }; saving = true
        scope.launch { try { FirebaseFirestore.getInstance().collection("users").document(uid).collection("upi_tags").document(sms.id).set(mapOf("tagName" to tagName.trim(), "iconName" to selectedIcon, "colorHex" to selectedColor, "recipient" to sms.recipient, "sender" to sms.sender, "amount" to sms.amount, "body" to sms.body, "date" to sms.date, "categoryId" to selectedCategory!!.id, "categoryName" to selectedCategory!!.name, "updatedAt" to com.google.firebase.Timestamp.now())).await(); onSaved("UPI tag saved") } catch (e: Exception) { error = e.message ?: "Unable to save UPI tag"; saving = false } }
    }) { Text(if (saving) "Saving..." else "Save") } }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}
