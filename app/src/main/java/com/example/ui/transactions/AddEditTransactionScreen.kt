package com.example.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.entity.CategoryEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.utils.CategoryIconHelper
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: ExpenseViewModel,
    transactionId: Long = 0L,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = transactionId > 0L
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf("EXPENSE") }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var noteInput by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showWalletManager by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val wallets = remember { mutableStateListOf<Wallet>() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val walletCollection = remember(uid) {
        uid?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(it)
                .collection("wallets")
        }
    }

    LaunchedEffect(uid) {
        walletCollection?.addSnapshotListener { snapshot, _ ->
            wallets.clear()
            snapshot?.documents?.mapNotNull { doc ->
                Wallet(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    balance = doc.getDouble("balance") ?: 0.0
                )
            }?.let(wallets::addAll)
        }
    }

    LaunchedEffect(wallets) {
        if (selectedWallet == null || wallets.none { it.id == selectedWallet?.id }) {
            selectedWallet = wallets.firstOrNull()
        }
    }

    LaunchedEffect(transactionId, allTransactions) {
        if (isEditMode) {
            val existing = allTransactions.firstOrNull { it.id == transactionId }
            if (existing != null) {
                selectedType = existing.type
                amountInput = CurrencyUtils.paiseToInputString(existing.amountInPaise)
                selectedDateMillis = existing.date
                noteInput = existing.note

                val catList = if (existing.type == "EXPENSE") expenseCategories else incomeCategories
                selectedCategory = catList.firstOrNull { it.id == existing.categoryId }
                    ?: CategoryEntity(
                        id = existing.categoryId,
                        name = existing.categoryName,
                        type = existing.type,
                        iconName = existing.categoryIcon,
                        colorHex = existing.categoryColorHex
                    )
            }
        }
    }

    val currentCategories = if (selectedType == "EXPENSE") expenseCategories else incomeCategories
    LaunchedEffect(currentCategories, selectedType) {
        if (selectedCategory == null || selectedCategory?.type != selectedType) {
            selectedCategory = currentCategories.firstOrNull()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Transaction" else "Add Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Transaction", tint = ExpenseRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == "EXPENSE",
                        onClick = {
                            selectedType = "EXPENSE"
                            selectedCategory = expenseCategories.firstOrNull()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = ExpenseRed.copy(alpha = 0.15f), activeContentColor = ExpenseRed),
                        modifier = Modifier.testTag("type_expense_button")
                    ) { Text("Expense", fontWeight = FontWeight.Bold) }

                    SegmentedButton(
                        selected = selectedType == "INCOME",
                        onClick = {
                            selectedType = "INCOME"
                            selectedCategory = incomeCategories.firstOrNull()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = IncomeGreen.copy(alpha = 0.15f), activeContentColor = IncomeGreen),
                        modifier = Modifier.testTag("type_income_button")
                    ) { Text("Income", fontWeight = FontWeight.Bold) }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Enter Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        amountInput = input
                                        errorMessage = null
                                    }
                                },
                                placeholder = { Text("0.00", fontSize = 28.sp) },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                                modifier = Modifier.width(220.dp).testTag("amount_input")
                            )
                        }
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(errorMessage!!, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                        }
                    }
                }
            }

            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Select Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onNavigateToCategories, modifier = Modifier.testTag("manage_categories_button")) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New / Manage")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (category in currentCategories) {
                            val isSelected = selectedCategory?.id == category.id
                            val catColor = CategoryIconHelper.parseColor(category.colorHex)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                tonalElevation = if (isSelected) 3.dp else 1.dp,
                                modifier = Modifier
                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) catColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { selectedCategory = category }
                                    .testTag("category_chip_${category.name}")
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(CategoryIconHelper.getIcon(category.iconName), contentDescription = category.name, tint = catColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Select Wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showWalletManager = true }, modifier = Modifier.testTag("manage_wallets_button")) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New / Manage")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (wallets.isEmpty()) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("No wallets yet. Tap New / Manage to add one.")
                            }
                        }
                    } else {
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (wallet in wallets) {
                                val isSelected = selectedWallet?.id == wallet.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    tonalElevation = if (isSelected) 3.dp else 1.dp,
                                    modifier = Modifier
                                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .clickable { selectedWallet = wallet }
                                        .testTag("wallet_chip_${wallet.name}")
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = wallet.name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(wallet.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }.testTag("date_picker_trigger"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(DateUtils.formatFullDate(selectedDateMillis), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }
                            Text("Change", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Note (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("e.g. Grocery shopping at supermarket, dinner with friends") },
                        modifier = Modifier.fillMaxWidth().testTag("note_input"),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 3
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val paise = CurrencyUtils.parseAmountToPaise(amountInput)
                        if (paise == null || paise <= 0) {
                            errorMessage = "Please enter a valid amount greater than ₹0"
                            return@Button
                        }
                        val category = selectedCategory
                        if (category == null) {
                            errorMessage = "Please select a category"
                            return@Button
                        }
                        if (wallets.isEmpty()) {
                            errorMessage = "Please add a wallet first"
                            return@Button
                        }
                        if (selectedWallet == null) {
                            errorMessage = "Please select a wallet"
                            return@Button
                        }

                        viewModel.saveTransaction(
                            id = transactionId,
                            type = selectedType,
                            amountInPaise = paise,
                            category = category,
                            date = selectedDateMillis,
                            note = noteInput,
                            onComplete = {
                                if (!isEditMode) {
                                    WalletBalanceManager.applyTransaction(
                                        walletId = selectedWallet!!.id,
                                        type = selectedType,
                                        amountInPaise = paise,
                                        onComplete = onNavigateBack
                                    )
                                } else {
                                    onNavigateBack()
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("save_transaction_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditMode) "Update Transaction" else "Save Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showWalletManager) {
        WalletManagerDialog(onDismiss = { showWalletManager = false })
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to permanently delete this transaction?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTransaction(transactionId, onComplete = onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } }
        )
    }
}
