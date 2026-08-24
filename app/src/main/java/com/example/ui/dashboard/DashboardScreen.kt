package com.example.ui.dashboard

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BudgetStatusCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.TransactionCard
import com.example.ui.theme.ExpenseRed
import com.example.utils.CurrencyUtils
import com.example.viewmodel.ExpenseViewModel

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthYearDisplay by viewModel.selectedMonthYearDisplay.collectAsStateWithLifecycle()
    val monthSummary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val budgetEntity by viewModel.currentMonthBudget.collectAsStateWithLifecycle()

    val recentTransactions = monthlyTransactions.take(5)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
                
            contentPadding = PaddingValues(
    start = 16.dp,
    end = 16.dp,
    bottom = 16.dp
),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            item {
                MonthSelectorBar(
                    monthYearDisplay = monthYearDisplay,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onCurrentMonth = { viewModel.setCurrentMonth() }
                )
            }

            // Top Financial Summary Card (Total Balance, Income, Expense)
            item {
                FinancialSummaryCard(
                    totalBalancePaise = monthSummary.balanceInPaise,
                    totalIncomePaise = monthSummary.totalIncomeInPaise,
                    totalExpensePaise = monthSummary.totalExpenseInPaise
                )
            }

            // Quick Stats Row: Month Expense & Today's Expense
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month Expense Stat
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "This Month",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyUtils.formatPaise(monthSummary.totalExpenseInPaise, showDecimals = false),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Today Expense Stat
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Today's Spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyUtils.formatPaise(monthSummary.todayExpenseInPaise, showDecimals = false),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Budget Widget
            item {
                if (budgetEntity != null && budgetEntity!!.budgetInPaise > 0) {
                    BudgetStatusCard(
                        budgetPaise = budgetEntity!!.budgetInPaise,
                        spentPaise = monthSummary.totalExpenseInPaise,
                        onSetBudgetClick = onNavigateToBudget
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBudget() }
                            .testTag("set_budget_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Set a Monthly Budget",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Track your spending limit for $monthYearDisplay",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (recentTransactions.isNotEmpty()) {
                        TextButton(
                            onClick = onNavigateToTransactions,
                            modifier = Modifier.testTag("see_all_transactions_button")
                        ) {
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Recent Transactions List or Empty State
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "No transactions in $monthYearDisplay",
                        subMessage = "Add your first expense or income for this month",
                        onAddClick = onNavigateToAddTransaction
                    )
                }
            } else {
                items(recentTransactions, key = { it.id }) { item ->
                    TransactionCard(
                        transaction = item,
                        onClick = { onNavigateToEditTransaction(item.id) }
                    )
                }
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
