package com.example.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions
            .groupBy { DateUtils.getStartOfDay(it.date) }
            .toSortedMap(compareByDescending { it })
    }

    val totalVisibleIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amountInPaise }
    }
    val totalVisibleExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountInPaise }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by category, note, amount...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { viewModel.filterType.value = "ALL" },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = filterType == "EXPENSE",
                        onClick = { viewModel.filterType.value = "EXPENSE" },
                        label = { Text("Expenses") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = .2f),
                            selectedLabelColor = ExpenseRed
                        )
                    )
                    FilterChip(
                        selected = filterType == "INCOME",
                        onClick = { viewModel.filterType.value = "INCOME" },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen.copy(alpha = .2f),
                            selectedLabelColor = IncomeGreen
                        )
                    )
                    FilterChip(
                        selected = filterMonthOnly,
                        onClick = { viewModel.filterMonthOnly.value = !filterMonthOnly },
                        label = { Text(if (filterMonthOnly) monthYearDisplay else "Month Only") }
                    )
                }
            }

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
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (filterCategory == "ALL" || filterCategory == null) "All Categories"
                                    else "Category: $filterCategory",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
                        allCategories
                            .map { it.name }
                            .distinct()
                            .forEach { catName ->
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

            if (filteredTransactions.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${filteredTransactions.size} record${if (filteredTransactions.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (totalVisibleIncome > 0) {
                                    Text(
                                        "+${CurrencyUtils.formatPaise(totalVisibleIncome, showDecimals = false)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                                if (totalVisibleExpense > 0) {
                                    Text(
                                        "-${CurrencyUtils.formatPaise(totalVisibleExpense, showDecimals = false)}",
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

            if (groupedTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "No matching transactions",
                        subMessage = if (
                            searchQuery.isNotEmpty() ||
                            filterType != "ALL" ||
                            filterCategory != "ALL"
                        ) {
                            "Try clearing your search or filters to see more results"
                        } else {
                            "Tap the button below to add your first transaction"
                        },
                        onAddClick = onNavigateToAddTransaction
                    )
                }
            } else {
                groupedTransactions.forEach { (dayStartMillis, txList) ->
                    item(key = "header_$dayStartMillis") {
                        val dayTotalExpense = txList
                            .filter { it.type == "EXPENSE" }
                            .sumOf { it.amountInPaise }
                        val dayTotalIncome = txList
                            .filter { it.type == "INCOME" }
                            .sumOf { it.amountInPaise }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                DateUtils.getRelativeDateHeader(dayStartMillis),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (dayTotalExpense > 0) {
                                    "-${CurrencyUtils.formatPaise(dayTotalExpense, showDecimals = false)}"
                                } else {
                                    "+${CurrencyUtils.formatPaise(dayTotalIncome, showDecimals = false)}"
                                },
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
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
