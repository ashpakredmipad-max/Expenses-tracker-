package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.AppDatabase
import com.example.data.local.database.entity.BudgetEntity
import com.example.data.local.database.entity.CategoryEntity
import com.example.data.local.database.entity.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.ExpenseRepository
import com.example.ui.model.CategorySpendItem
import com.example.ui.model.MonthSummary
import com.example.utils.DataExportImportHelper
import com.example.utils.DateUtils
import com.example.utils.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class TransactionFilterParams(
    val query: String,
    val type: String?,
    val category: String?,
    val monthOnly: Boolean,
    val calendar: Calendar
)

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    // Calendar for selected month in dashboard & reports
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    // Key formatted as "yyyy-MM" for budget and filtering
    val selectedYearMonthKey: StateFlow<String> = _selectedCalendar.map {
        DateUtils.formatYearMonthKey(it.timeInMillis)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DateUtils.formatYearMonthKey(System.currentTimeMillis()))

    // Display string, e.g., "August 2026"
    val selectedMonthYearDisplay: StateFlow<String> = _selectedCalendar.map {
        DateUtils.formatMonthYear(it.timeInMillis)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DateUtils.formatMonthYear(System.currentTimeMillis()))

    // All categories
    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> = allCategories.map { list ->
        list.filter { it.type == "EXPENSE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = allCategories.map { list ->
        list.filter { it.type == "INCOME" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All transactions (all time)
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions for selected month
    val monthlyTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedCalendar
    ) { txList, cal ->
        val startOfMonth = DateUtils.getStartOfMonth(cal)
        val endOfMonth = DateUtils.getEndOfMonth(cal)
        txList.filter { it.date in startOfMonth..endOfMonth }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall summary calculation
    val monthSummary: StateFlow<MonthSummary> = combine(
        allTransactions,
        monthlyTransactions
    ) { allTx, monthTx ->
        val overallIncome = allTx.filter { it.type == "INCOME" }.sumOf { it.amountInPaise }
        val overallExpense = allTx.filter { it.type == "EXPENSE" }.sumOf { it.amountInPaise }
        val overallBal = overallIncome - overallExpense

        val mIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amountInPaise }
        val mExpense = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amountInPaise }
        val mBalance = mIncome - mExpense

        val startOfToday = DateUtils.getStartOfDay(System.currentTimeMillis())
        val endOfToday = DateUtils.getEndOfDay(System.currentTimeMillis())
        val tExpense = allTx.filter { it.type == "EXPENSE" && it.date in startOfToday..endOfToday }
            .sumOf { it.amountInPaise }

        MonthSummary(
            totalIncomeInPaise = mIncome,
            totalExpenseInPaise = mExpense,
            balanceInPaise = mBalance,
            todayExpenseInPaise = tExpense,
            overallBalanceInPaise = overallBal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthSummary())

    // Budget for selected month
    val currentMonthBudget: StateFlow<BudgetEntity?> = selectedYearMonthKey.flatMapLatest { key ->
        repository.getBudgetForMonth(key)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Category breakdown for selected month expenses
    val expenseCategoryBreakdown: StateFlow<List<CategorySpendItem>> = monthlyTransactions.map { list ->
        val expenses = list.filter { it.type == "EXPENSE" }
        val totalExpense = expenses.sumOf { it.amountInPaise }
        if (totalExpense <= 0) {
            emptyList()
        } else {
            expenses.groupBy { it.categoryName }
                .map { (catName, txGroup) ->
                    val sum = txGroup.sumOf { it.amountInPaise }
                    val percent = (sum.toDouble() / totalExpense.toDouble() * 100.0).toFloat()
                    val firstItem = txGroup.first()
                    CategorySpendItem(
                        categoryName = catName,
                        categoryIcon = firstItem.categoryIcon,
                        categoryColorHex = firstItem.categoryColorHex,
                        totalAmountInPaise = sum,
                        percentage = percent,
                        transactionCount = txGroup.size
                    )
                }
                .sortedByDescending { it.totalAmountInPaise }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category breakdown for selected month income
    val incomeCategoryBreakdown: StateFlow<List<CategorySpendItem>> = monthlyTransactions.map { list ->
        val incomes = list.filter { it.type == "INCOME" }
        val totalIncome = incomes.sumOf { it.amountInPaise }
        if (totalIncome <= 0) {
            emptyList()
        } else {
            incomes.groupBy { it.categoryName }
                .map { (catName, txGroup) ->
                    val sum = txGroup.sumOf { it.amountInPaise }
                    val percent = (sum.toDouble() / totalIncome.toDouble() * 100.0).toFloat()
                    val firstItem = txGroup.first()
                    CategorySpendItem(
                        categoryName = catName,
                        categoryIcon = firstItem.categoryIcon,
                        categoryColorHex = firstItem.categoryColorHex,
                        totalAmountInPaise = sum,
                        percentage = percent,
                        transactionCount = txGroup.size
                    )
                }
                .sortedByDescending { it.totalAmountInPaise }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transaction search and filter state
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<String?>("ALL") // "ALL", "EXPENSE", "INCOME"
    val filterCategory = MutableStateFlow<String?>("ALL")
    val filterMonthOnly = MutableStateFlow(false) // Filter by selected month or show all time

    private val filterParamsFlow: Flow<TransactionFilterParams> = combine(
        searchQuery,
        filterType,
        filterCategory,
        filterMonthOnly,
        _selectedCalendar
    ) { query, type, category, monthOnly, cal ->
        TransactionFilterParams(query, type, category, monthOnly, cal)
    }

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterParamsFlow
    ) { txList, params ->
        val startOfMonth = DateUtils.getStartOfMonth(params.calendar)
        val endOfMonth = DateUtils.getEndOfMonth(params.calendar)

        txList.filter { tx ->
            val matchesMonth = !params.monthOnly || (tx.date in startOfMonth..endOfMonth)
            val matchesType = params.type == null || params.type == "ALL" || tx.type.equals(params.type, ignoreCase = true)
            val matchesCategory = params.category == null || params.category == "ALL" || tx.categoryName.equals(params.category, ignoreCase = true)
            val matchesQuery = params.query.isBlank() ||
                    tx.categoryName.contains(params.query, ignoreCase = true) ||
                    tx.note.contains(params.query, ignoreCase = true) ||
                    (tx.amountInPaise / 100).toString().contains(params.query)

            matchesMonth && matchesType && matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Theme mode: "SYSTEM", "LIGHT", "DARK"
    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDefaults()
            repository.getSetting("theme_mode").collect { mode ->
                if (mode != null) {
                    _themeMode.value = mode
                }
            }
        }
    }

    // Month Navigation
    fun nextMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _selectedCalendar.value = cal
    }

    fun previousMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _selectedCalendar.value = cal
    }

    fun setCurrentMonth() {
        _selectedCalendar.value = Calendar.getInstance()
    }

    fun selectMonthAndYear(year: Int, month: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        _selectedCalendar.value = cal
    }

    // Transaction Actions
    fun saveTransaction(
        id: Long = 0,
        type: String,
        amountInPaise: Long,
        category: CategoryEntity,
        date: Long,
        note: String = "",
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = id,
                type = type,
                amountInPaise = amountInPaise,
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.iconName,
                categoryColorHex = category.colorHex,
                date = date,
                note = note.trim(),
                createdAt = if (id == 0L) System.currentTimeMillis() else date
            )

            if (id == 0L) {
                repository.insertTransaction(entity)
            } else {
                repository.updateTransaction(entity)
            }
            onComplete()
        }
    }

    fun deleteTransaction(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            onComplete()
        }
    }

    // Category Actions
    fun addCategory(
        name: String,
        type: String,
        iconName: String,
        colorHex: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                onComplete(false)
                return@launch
            }
            val entity = CategoryEntity(
                name = name.trim(),
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false
            )
            repository.insertCategory(entity)
            onComplete(true)
        }
    }

    fun updateCategory(category: CategoryEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateCategory(category)
            onComplete()
        }
    }

    fun checkCategoryUsage(categoryId: Long, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.getTransactionCountForCategory(categoryId)
            onResult(count)
        }
    }

    fun deleteCategory(
        category: CategoryEntity,
        reassignFallbackCategory: CategoryEntity? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            if (reassignFallbackCategory != null) {
                repository.reassignCategoryTransactions(category.id, reassignFallbackCategory)
            }
            repository.deleteCategory(category)
            onComplete()
        }
    }

    // Budget Actions
    fun setMonthlyBudget(budgetInPaise: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.setBudget(selectedYearMonthKey.value, budgetInPaise)
            onComplete()
        }
    }

    fun removeMonthlyBudget(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteBudgetForMonth(selectedYearMonthKey.value)
            onComplete()
        }
    }

    // Theme Actions
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            _themeMode.value = mode
            repository.setSetting("theme_mode", mode)
        }
    }

    // Data Export / Import / Backup
    suspend fun exportCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val list = repository.getAllTransactionsList()
        DataExportImportHelper.exportTransactionsToCsv(context, uri, list)
    }

    suspend fun importCsv(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val categories = allCategories.value
        val result = DataExportImportHelper.importTransactionsFromCsv(context, uri, categories)
        if (result.transactions.isNotEmpty()) {
            repository.insertAllTransactions(result.transactions)
        }
        result
    }

    suspend fun exportBackupJson(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val cats = allCategories.value
        val txs = repository.getAllTransactionsList()
        val budgets = repository.getAllBudgetsList()
        DataExportImportHelper.exportBackupJson(context, uri, cats, txs, budgets)
    }

    suspend fun importBackupJson(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val data = DataExportImportHelper.importBackupJson(context, uri) ?: return@withContext false
        if (data.categories.isNotEmpty()) {
            repository.insertAllCategories(data.categories)
        }
        if (data.transactions.isNotEmpty()) {
            repository.insertAllTransactions(data.transactions)
        }
        if (data.budgets.isNotEmpty()) {
            for (b in data.budgets) {
                repository.setBudget(b.yearMonth, b.budgetInPaise)
            }
        }
        true
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllData()
            onComplete()
        }
    }

    class Factory(
        private val application: Application,
        private val repository: ExpenseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                return ExpenseViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
