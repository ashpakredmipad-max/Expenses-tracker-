package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BudgetStatusCard
import com.example.ui.components.EmptyStateView
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
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            ) { Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp)) }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeMonthHeader(monthYearDisplay, viewModel::previousMonth, viewModel::nextMonth, viewModel::setCurrentMonth)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Monthly Overview", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Default.TrendingDown, null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(CurrencyUtils.formatPaise(monthSummary.totalExpenseInPaise, showDecimals = false), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Total expenses in $monthYearDisplay", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeMiniMetric(Modifier.weight(1f), "Income", CurrencyUtils.formatPaise(monthSummary.totalIncomeInPaise, showDecimals = false), Color(0xFF9BE7B0), Icons.Default.ArrowUpward)
                            HomeMiniMetric(Modifier.weight(1f), "Spent", CurrencyUtils.formatPaise(monthSummary.totalExpenseInPaise, showDecimals = false), Color(0xFFFFA3A3), Icons.Default.ArrowDownward)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeStatCard(Modifier.weight(1f), "Balance", CurrencyUtils.formatPaise(monthSummary.balanceInPaise, showDecimals = false), Icons.Default.CalendarToday)
                    HomeStatCard(Modifier.weight(1f), "Today", CurrencyUtils.formatPaise(monthSummary.todayExpenseInPaise, showDecimals = false), Icons.Default.TrendingDown)
                }
            }

            item {
                if (budgetEntity != null && budgetEntity!!.budgetInPaise > 0) {
                    BudgetStatusCard(budgetPaise = budgetEntity!!.budgetInPaise, spentPaise = monthSummary.totalExpenseInPaise, onSetBudgetClick = onNavigateToBudget)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToBudget() }.testTag("set_budget_banner"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Savings, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Set a Monthly Budget", fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("Track your spending limit", style = MaterialTheme.typography.bodySmall, color = Color.Black.copy(alpha = 0.58f))
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.Black)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Your latest activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (recentTransactions.isNotEmpty()) TextButton(onClick = onNavigateToTransactions, modifier = Modifier.testTag("see_all_transactions_button")) { Text("See All", fontWeight = FontWeight.SemiBold) }
                }
            }

            if (recentTransactions.isEmpty()) {
                item { EmptyStateView(message = "No transactions in $monthYearDisplay", subMessage = "Add your first expense or income for this month", onAddClick = onNavigateToAddTransaction) }
            } else {
                items(recentTransactions, key = { it.id }) { transaction -> TransactionCard(transaction = transaction, onClick = { onNavigateToEditTransaction(transaction.id) }) }
            }
        }
    }
}

@Composable
private fun HomeMonthHeader(monthYearDisplay: String, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, onCurrentMonth: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onPreviousMonth) { Text("‹", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCurrentMonth() }) {
            Text("HOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(monthYearDisplay, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onNextMonth) { Text("›", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun HomeMiniMetric(modifier: Modifier, title: String, value: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(title, color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeStatCard(modifier: Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(7.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
