package com.example.ui.budget

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BudgetStatusCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.ExpenseRed
import com.example.utils.CurrencyUtils
import com.example.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthYearDisplay by viewModel.selectedMonthYearDisplay.collectAsStateWithLifecycle()
    val monthSummary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val currentBudget by viewModel.currentMonthBudget.collectAsStateWithLifecycle()

    var budgetInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentBudget) {
        if (currentBudget != null && currentBudget!!.budgetInPaise > 0) {
            budgetInput = CurrencyUtils.paiseToInputString(currentBudget!!.budgetInPaise)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Monthly Budget", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("budget_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (currentBudget != null && currentBudget!!.budgetInPaise > 0) {
                        IconButton(
                            onClick = {
                                viewModel.removeMonthlyBudget {
                                    budgetInput = ""
                                }
                            },
                            modifier = Modifier.testTag("remove_budget_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Budget",
                                tint = ExpenseRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
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

            // Current budget preview if already set
            if (currentBudget != null && currentBudget!!.budgetInPaise > 0) {
                item {
                    BudgetStatusCard(
                        budgetPaise = currentBudget!!.budgetInPaise,
                        spentPaise = monthSummary.totalExpenseInPaise,
                        onSetBudgetClick = {}
                    )
                }
            }

            // Set/Update Budget Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Set Budget for $monthYearDisplay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    budgetInput = it
                                    inputError = null
                                }
                            },
                            label = { Text("Budget Amount (₹)") },
                            placeholder = { Text("e.g. 50000") },
                            leadingIcon = {
                                Text("₹", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_amount_input")
                        )

                        if (inputError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = inputError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(20000L, 50000L, 100000L).forEach { presetRupees ->
                                OutlinedButton(
                                    onClick = {
                                        budgetInput = presetRupees.toString()
                                        inputError = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("₹${presetRupees / 1000}k", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val paise = CurrencyUtils.parseAmountToPaise(budgetInput)
                                if (paise == null || paise <= 0) {
                                    inputError = "Please enter a valid budget amount"
                                    return@Button
                                }
                                viewModel.setMonthlyBudget(paise) {
                                    onNavigateBack()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_budget_button")
                        ) {
                            Text(
                                text = if (currentBudget != null && currentBudget!!.budgetInPaise > 0) "Update Budget" else "Set Budget",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
