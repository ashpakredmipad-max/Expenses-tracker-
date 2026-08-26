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
import com.example.data.repository.ExpenseRepository
import com.example.data.model.TransactionType
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
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()
    val selectedYearMonthKey: StateFlow<String> = _selectedCalendar.map { DateUtils.formatYearMonthKey(it.timeInMillis) }.stateIn(viewModelScope, SharingStarted.Eagerly, DateUtils.formatYearMonthKey(System.currentTimeMillis()))
    val selectedMonthYearDisplay: StateFlow<String> = _selectedCalendar.map { DateUtils.formatMonthYear(it.timeInMillis) }.stateIn(viewModelScope, SharingStarted.Eagerly, DateUtils.formatMonthYear(System.currentTimeMillis()))

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenseCategories: StateFlow<List<CategoryEntity>> = allCategories.map { list -> list.filter { it.type == "EXPENSE" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val incomeCategories: StateFlow<List<CategoryEntity>> = allCategories.map { list -> list.filter { it.type == "INCOME" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTransactions: StateFlow<List<TransactionEntity>> = combine(allTransactions, _selectedCalendar) { txList, cal ->
        val startOfMonth = DateUtils.getStartOfMonth(cal)
        val endOfMonth = DateUtils.getEndOfMonth(cal)
        txList.filter { it.date in startOfMonth..endOfMonth }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthSummary: StateFlow<MonthSummary> = combine(allTransactions, monthlyTransactions) { allTx, monthTx ->
        val overallIncome = allTx.filter { it.type == "INCOME" }.sumOf { it.amountInPaise }
        val overallExpense = allTx.filter { it.type == "EXPENSE" }.sumOf { it.amountInPaise }
        val overallBal = overallIncome - overallExpense
        val mIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amountInPaise }
        val mExpense = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amountInPaise }
        val mBalance = mIncome - mExpense
        val startOfToday = DateUtils.getStartOfDay(System.currentTimeMillis())
        val endOfToday = DateUtils.getEndOfDay(System.currentTimeMillis())
        val tExpense = allTx.filter { it.type == "EXPENSE" && it.date in startOfToday..endOfToday }.sumOf { it.amountInPaise }
        MonthSummary(mIncome, mExpense, mBalance, tExpense, overallBal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthSummary())

    val currentMonthBudget: StateFlow<BudgetEntity?> = selectedYearMonthKey.flatMapLatest { key -> repository.getBudgetForMonth(key) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val expenseCategoryBreakdown: StateFlow<List<CategorySpendItem>> = monthlyTransactions.map { list ->
        val expenses = list.filter { it.type == "EXPENSE" }
        val totalExpense = expenses.sumOf { it.amountInPaise }
        if (totalExpense <= 0) emptyList() else expenses.groupBy { it.categoryName }.map { (catName, txGroup) ->
            val sum = txGroup.sumOf { it.amountInPaise }
            val firstItem = txGroup.first()
            CategorySpendItem(catName, firstItem.categoryIcon, firstItem.categoryColorHex, sum, (sum.toDouble() / totalExpense * 100.0).toFloat(), txGroup.size)
        }.sortedByDescending { it.totalAmountInPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val incomeCategoryBreakdown: StateFlow<List<CategorySpendItem>> = monthlyTransactions.map { list ->
        val incomes = list.filter { it.type == "INCOME" }
        val totalIncome = incomes.sumOf { it.amountInPaise }
        if (totalIncome <= 0) emptyList() else incomes.groupBy { it.categoryName }.map { (catName, txGroup) ->
            val sum = txGroup.sumOf { it.amountInPaise }
            val firstItem = txGroup.first()
            CategorySpendItem(catName, firstItem.categoryIcon, firstItem.categoryColorHex, sum, (sum.toDouble() / totalIncome * 100.0).toFloat(), txGroup.size)
        }.sortedByDescending { it.totalAmountInPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<String?>("ALL")
    val filterCategory = MutableStateFlow<String?>("ALL")
    val filterMonthOnly = MutableStateFlow(false)
    private val filterParamsFlow: Flow<TransactionFilterParams> = combine(searchQuery, filterType, filterCategory, filterMonthOnly, _selectedCalendar) { query, type, category, monthOnly, cal -> TransactionFilterParams(query, type, category, monthOnly, cal) }
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(allTransactions, filterParamsFlow) { txList, params ->
        val startOfMonth = DateUtils.getStartOfMonth(params.calendar)
        val endOfMonth = DateUtils.getEndOfMonth(params.calendar)
        txList.filter { tx ->
            val matchesMonth = !params.monthOnly || tx.date in startOfMonth..endOfMonth
            val matchesType = params.type == null || params.type == "ALL" || tx.type.equals(params.type, ignoreCase = true)
            val matchesCategory = params.category == null || params.category == "ALL" || tx.categoryName.equals(params.category, ignoreCase = true)
            val matchesQuery = params.query.isBlank() || tx.categoryName.contains(params.query, true) || tx.note.contains(params.query, true) || (tx.amountInPaise / 100).toString().contains(params.query)
            matchesMonth && matchesType && matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDefaults()
            repository.getSetting("theme_mode").collect { mode -> if (mode != null) _themeMode.value = mode }
        }
    }

    fun nextMonth() { val cal = _selectedCalendar.value.clone() as Calendar; cal.add(Calendar.MONTH, 1); _selectedCalendar.value = cal }
    fun previousMonth() { val cal = _selectedCalendar.value.clone() as Calendar; cal.add(Calendar.MONTH, -1); _selectedCalendar.value = cal }
    fun setCurrentMonth() { _selectedCalendar.value = Calendar.getInstance() }
    fun selectMonthAndYear(year: Int, month: Int) { val cal = Calendar.getInstance(); cal.set(Calendar.YEAR, year); cal.set(Calendar.MONTH, month); cal.set(Calendar.DAY_OF_MONTH, 1); _selectedCalendar.value = cal }

    fun saveTransaction(id: Long = 0, type: String, amountInPaise: Long, category: CategoryEntity, date: Long, note: String = "", onComplete: () -> Unit) {
        viewModelScope.launch {
            val entity = TransactionEntity(id, type, amountInPaise, category.id, category.name, category.iconName, category.colorHex, date, note.trim(), if (id == 0L) System.currentTimeMillis() else date)
            if (id == 0L) repository.insertTransaction(entity) else repository.updateTransaction(entity)
            onComplete()
        }
    }
    fun deleteTransaction(id: Long, onComplete: () -> Unit = {}) { viewModelScope.launch { repository.deleteTransactionById(id); onComplete() } }

    fun addCategory(name: String, type: String, iconName: String, colorHex: String, onComplete: (Boolean) -> Unit) { viewModelScope.launch { if (name.isBlank()) { onComplete(false); return@launch }; repository.insertCategory(CategoryEntity(name = name.trim(), type = type, iconName = iconName, colorHex = colorHex, isDefault = false)); onComplete(true) } }
    fun updateCategory(category: CategoryEntity, onComplete: () -> Unit) { viewModelScope.launch { repository.updateCategory(category); onComplete() } }
    fun checkCategoryUsage(categoryId: Long, onResult: (Int) -> Unit) { viewModelScope.launch { onResult(repository.getTransactionCountForCategory(categoryId)) } }
    fun deleteCategory(category: CategoryEntity, reassignFallbackCategory: CategoryEntity? = null, onComplete: () -> Unit) { viewModelScope.launch { if (reassignFallbackCategory != null) repository.reassignCategoryTransactions(category.id, reassignFallbackCategory); repository.deleteCategory(category); onComplete() } }

    fun setMonthlyBudget(budgetInPaise: Long, onComplete: () -> Unit = {}) { viewModelScope.launch { repository.setBudget(selectedYearMonthKey.value, budgetInPaise); onComplete() } }
    fun removeMonthlyBudget(onComplete: () -> Unit = {}) { viewModelScope.launch { repository.deleteBudgetForMonth(selectedYearMonthKey.value); onComplete() } }
    fun setThemeMode(mode: String) { viewModelScope.launch { _themeMode.value = mode; repository.setSetting("theme_mode", mode) } }

    suspend fun exportCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) { DataExportImportHelper.exportTransactionsToCsv(context, uri, repository.getAllTransactionsList()) }
    suspend fun importCsv(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) { val result = DataExportImportHelper.importTransactionsFromCsv(context, uri, allCategories.value); if (result.transactions.isNotEmpty()) repository.insertAllTransactions(result.transactions); result }
    suspend fun exportBackupJson(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) { DataExportImportHelper.exportBackupJson(context, uri, allCategories.value, repository.getAllTransactionsList(), repository.getAllBudgetsList()) }
    suspend fun importBackupJson(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) { val data = DataExportImportHelper.importBackupJson(context, uri) ?: return@withContext false; if (data.categories.isNotEmpty()) repository.insertAllCategories(data.categories); if (data.transactions.isNotEmpty()) repository.insertAllTransactions(data.transactions); if (data.budgets.isNotEmpty()) for (b in data.budgets) repository.setBudget(b.yearMonth, b.budgetInPaise); true }

    /** Reset transactions so every transaction-derived wallet balance becomes zero. */
    fun resetData(onComplete: () -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.resetAllTransactions() }
                .onSuccess { onComplete() }
                .onFailure { onError(it) }
        }
    }

    fun deleteAllData(onComplete: () -> Unit) { viewModelScope.launch { repository.deleteAllData(); onComplete() } }

    class Factory(private val application: Application, private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) return ExpenseViewModel(application, repository) as T
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
