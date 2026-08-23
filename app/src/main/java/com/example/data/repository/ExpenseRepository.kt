package com.example.data.repository

import com.example.data.local.database.AppDatabase
import com.example.data.local.database.dao.AppSettingDao
import com.example.data.local.database.dao.BudgetDao
import com.example.data.local.database.dao.CategoryDao
import com.example.data.local.database.dao.TransactionDao
import com.example.data.local.database.entity.AppSettingEntity
import com.example.data.local.database.entity.BudgetEntity
import com.example.data.local.database.entity.CategoryEntity
import com.example.data.local.database.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExpenseRepository(private val database: AppDatabase) {
    private val transactionDao: TransactionDao = database.transactionDao()
    private val categoryDao: CategoryDao = database.categoryDao()
    private val budgetDao: BudgetDao = database.budgetDao()
    private val appSettingDao: AppSettingDao = database.appSettingDao()

    // Transaction flows
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(start, end)

    fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id)

    suspend fun getTransactionByIdDirect(id: Long): TransactionEntity? =
        transactionDao.getTransactionByIdDirect(id)

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    suspend fun getAllTransactionsList(): List<TransactionEntity> =
        transactionDao.getAllTransactionsList()

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun insertAllTransactions(transactions: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        transactionDao.insertAll(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionById(id)
    }

    // Category flows
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)

    suspend fun getCategoryCount(): Int =
        categoryDao.getCategoryCount()

    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun insertAllCategories(categories: List<CategoryEntity>) = withContext(Dispatchers.IO) {
        categoryDao.insertAll(categories)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getTransactionCountForCategory(categoryId: Long): Int = withContext(Dispatchers.IO) {
        transactionDao.getTransactionCountByCategoryIdDirect(categoryId)
    }

    fun getTransactionCountForCategoryFlow(categoryId: Long): Flow<Int> =
        transactionDao.getTransactionCountByCategoryId(categoryId)

    suspend fun reassignCategoryTransactions(fromCategoryId: Long, toCategory: CategoryEntity) = withContext(Dispatchers.IO) {
        val affected = transactionDao.getTransactionsByCategoryId(fromCategoryId)
        for (tx in affected) {
            transactionDao.updateTransaction(
                tx.copy(
                    categoryId = toCategory.id,
                    categoryName = toCategory.name,
                    categoryIcon = toCategory.iconName,
                    categoryColorHex = toCategory.colorHex
                )
            )
        }
    }

    // Budget flows
    fun getBudgetForMonth(yearMonth: String): Flow<BudgetEntity?> =
        budgetDao.getBudgetForMonth(yearMonth)

    suspend fun getBudgetForMonthDirect(yearMonth: String): BudgetEntity? =
        budgetDao.getBudgetForMonthDirect(yearMonth)

    fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getAllBudgets()

    suspend fun getAllBudgetsList(): List<BudgetEntity> =
        budgetDao.getAllBudgetsList()

    suspend fun setBudget(yearMonth: String, budgetInPaise: Long) = withContext(Dispatchers.IO) {
        budgetDao.insertOrUpdateBudget(BudgetEntity(yearMonth = yearMonth, budgetInPaise = budgetInPaise))
    }

    suspend fun deleteBudgetForMonth(yearMonth: String) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudgetForMonth(yearMonth)
    }

    // Settings
    fun getSetting(key: String): Flow<String?> = appSettingDao.getSettingFlow(key)

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        appSettingDao.setSetting(AppSettingEntity(key = key, value = value))
    }

    // Full Reset
    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
        budgetDao.deleteAllBudgets()
        categoryDao.deleteAllCategories()
        // Re-seed default categories
        categoryDao.insertAll(AppDatabase.DEFAULT_EXPENSE_CATEGORIES)
        categoryDao.insertAll(AppDatabase.DEFAULT_INCOME_CATEGORIES)
    }

    // Seed defaults if empty
    suspend fun checkAndSeedDefaults() = withContext(Dispatchers.IO) {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertAll(AppDatabase.DEFAULT_EXPENSE_CATEGORIES)
            categoryDao.insertAll(AppDatabase.DEFAULT_INCOME_CATEGORIES)
        }
    }
}
