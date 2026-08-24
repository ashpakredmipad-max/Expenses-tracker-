package com.example.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.entity.TransactionEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsHistoryScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
    val filterMonthOnly by viewModel.filterMonthOnly.collectAsStateWithLifecycle()
    val monthYearDisplay by viewModel.selectedMonthYearDisplay.collectAsStateWithLifecycle()

    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Group transactions by date
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { DateUtils.getStartOfDay(it.date) }
            .toSortedMap(compareByDescending { it })
    }

    // Totals of currently visible transactions
    val totalVisibleIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amountInPaise }
    }
    val totalVisibleExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountInPaise }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Box
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by category, note, amount...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_transactions_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Filter: ALL
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { viewModel.filterType.value = "ALL" },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    // Type Filter: EXPENSE
                    FilterChip(
                        selected = filterType == "EXPENSE",
                        onClick = { viewModel.filterType.value = "EXPENSE" },
                        label = { Text("Expenses") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                            selectedLabelColor = ExpenseRed
                        )
                    )

                    // Type Filter: INCOME
                    FilterChip(
                        selected = filterType == "INCOME",
                        onClick = { viewModel.filterType.value = "INCOME" },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen.copy(alpha = 0.2f),
                            selectedLabelColor = IncomeGreen
                        )
                    )

                    // Filter by Month Toggle
                    FilterChip(
                        selected = filterMonthOnly,
                        onClick = { viewModel.filterMonthOnly.value = !filterMonthOnly },
                        label = { Text(if (filterMonthOnly) monthYearDisplay else "Month Only") }
                    )
                }
            }

            // Category Filter Dropdown
            item {
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDropdown = true }
                            .testTag("filter_category_dropdown"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (filterCategory == "ALL" || filterCategory == null) "All Categories" else "Category: $filterCategory",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (filterCategory != "ALL" && filterCategory != null) {
                                TextButton(
                                    onClick = { viewModel.filterCategory.value = "ALL" },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories", fontWeight = FontWeight.Bold) },
                            onClick = {
                                viewModel.filterCategory.value = "ALL"
                                showCategoryDropdown = false
                            }
                        )
                        allCategories.map { it.name }.distinct().forEach { catName ->
                            DropdownMenuItem(
                                text = { Text(catName) },
                                onClick = {
                                    viewModel.filterCategory.value = catName
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Summary of filtered results
            if (filteredTransactions.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredTransactions.size} record${if (filteredTransactions.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (totalVisibleIncome > 0) {
                                    Text(
                                        text = "+${CurrencyUtils.formatPaise(totalVisibleIncome, showDecimals = false)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                                if (totalVisibleExpense > 0) {
                                    Text(
                                        text = "-${CurrencyUtils.formatPaise(totalVisibleExpense, showDecimals = false)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty state or Grouped Transactions
            if (groupedTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "No matching transactions",
                        subMessage = if (searchQuery.isNotEmpty() || filterType != "ALL" || filterCategory != "ALL")
                            "Try clearing your search or filters to see more results"
                        else
                            "Tap the button below to add your first transaction",
                        onAddClick = onNavigateToAddTransaction
                    )
                }
            } else {
                groupedTransactions.forEach { (dayStartMillis, txList) ->
                    item(key = "header_$dayStartMillis") {
                        val dayTotalExpense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amountInPaise }
                        val dayTotalIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amountInPaise }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateUtils.getRelativeDateHeader(dayStartMillis),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = if (dayTotalExpense > 0) "-${CurrencyUtils.formatPaise(dayTotalExpense, showDecimals = false)}"
                                else "+${CurrencyUtils.formatPaise(dayTotalIncome, showDecimals = false)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    items(txList, key = { it.id }) { tx ->
                        TransactionCard(
                            transaction = tx,
                            onClick = { onNavigateToEditTransaction(tx.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = ExpenseRed
                )
            },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete this transaction for ${CurrencyUtils.formatPaise(transactionToDelete!!.amountInPaise)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = transactionToDelete!!.id
                        transactionToDelete = null
                        viewModel.deleteTransaction(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
