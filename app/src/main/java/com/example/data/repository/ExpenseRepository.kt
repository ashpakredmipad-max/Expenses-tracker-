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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseRepository(private val database: AppDatabase) {
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val appSettingDao = database.appSettingDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            syncScope.launch {
                if (uid == null) transactionDao.deleteAllTransactions()
                else runCatching { syncTransactionsForUser(uid) }
            }
        }
    }

    private fun transactionsCollection(uid: String) = firestore.collection("users").document(uid).collection("transactions")
    private fun currentUid(uid: String?) = uid ?: auth.currentUser?.uid

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> = transactionDao.getTransactionsBetween(start, end)
    fun getTransactionById(id: Long): Flow<TransactionEntity?> = transactionDao.getTransactionById(id)
    suspend fun getTransactionByIdDirect(id: Long): TransactionEntity? = transactionDao.getTransactionByIdDirect(id)
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(limit)
    suspend fun getAllTransactionsList(): List<TransactionEntity> = transactionDao.getAllTransactionsList()

    suspend fun insertTransaction(transaction: TransactionEntity, uid: String? = null): Long = withContext(Dispatchers.IO) {
        val id = transactionDao.insertTransaction(transaction)
        currentUid(uid)?.let { saveTransactionToCloud(it, transaction.copy(id = id)) }
        id
    }

    suspend fun insertAllTransactions(transactions: List<TransactionEntity>, uid: String? = null) = withContext(Dispatchers.IO) {
        transactionDao.insertAll(transactions)
        currentUid(uid)?.let { transactions.forEach { tx -> saveTransactionToCloud(it, tx) } }
    }

    suspend fun updateTransaction(transaction: TransactionEntity, uid: String? = null) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
        currentUid(uid)?.let { saveTransactionToCloud(it, transaction) }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity, uid: String? = null) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
        currentUid(uid)?.let { deleteTransactionFromCloud(it, transaction.id) }
    }

    suspend fun deleteTransactionById(id: Long, uid: String? = null) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionById(id)
        currentUid(uid)?.let { deleteTransactionFromCloud(it, id) }
    }

    private fun transactionMap(tx: TransactionEntity) = hashMapOf<String, Any>(
        "id" to tx.id, "type" to tx.type, "amountInPaise" to tx.amountInPaise,
        "categoryId" to tx.categoryId, "categoryName" to tx.categoryName,
        "categoryIcon" to tx.categoryIcon, "categoryColorHex" to tx.categoryColorHex,
        "date" to tx.date, "note" to tx.note, "createdAt" to tx.createdAt
    )

    private fun saveTransactionToCloud(uid: String, tx: TransactionEntity) {
        transactionsCollection(uid).document(tx.id.toString()).set(transactionMap(tx))
    }

    private fun deleteTransactionFromCloud(uid: String, id: Long) {
        transactionsCollection(uid).document(id.toString()).delete()
    }

    suspend fun syncTransactionsForUser(uid: String) = withContext(Dispatchers.IO) {
        val snapshot = Tasks.await(transactionsCollection(uid).get())
        val cloudTransactions = snapshot.documents.mapNotNull { doc ->
            try {
                TransactionEntity(
                    id = doc.getLong("id") ?: doc.id.toLong(),
                    type = doc.getString("type") ?: return@mapNotNull null,
                    amountInPaise = doc.getLong("amountInPaise") ?: 0L,
                    categoryId = doc.getLong("categoryId") ?: 0L,
                    categoryName = doc.getString("categoryName") ?: "",
                    categoryIcon = doc.getString("categoryIcon") ?: "",
                    categoryColorHex = doc.getString("categoryColorHex") ?: "",
                    date = doc.getLong("date") ?: 0L,
                    note = doc.getString("note") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } catch (_: Exception) { null }
        }
        transactionDao.deleteAllTransactions()
        if (cloudTransactions.isNotEmpty()) transactionDao.insertAll(cloudTransactions)
    }

    /** Permanently removes every transaction locally and from the signed-in user's cloud data. */
    suspend fun resetAllTransactions() = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val snapshot = Tasks.await(transactionsCollection(uid).get())
            for (document in snapshot.documents) {
                Tasks.await(document.reference.delete())
            }
        }
        transactionDao.deleteAllTransactions()
    }

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)
    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)
    suspend fun getCategoryCount(): Int = categoryDao.getCategoryCount()
    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) { categoryDao.insertCategory(category) }
    suspend fun insertAllCategories(categories: List<CategoryEntity>) = withContext(Dispatchers.IO) { categoryDao.insertAll(categories) }
    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.updateCategory(category) }
    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.deleteCategory(category) }
    suspend fun getTransactionCountForCategory(categoryId: Long): Int = withContext(Dispatchers.IO) { transactionDao.getTransactionCountByCategoryIdDirect(categoryId) }
    fun getTransactionCountForCategoryFlow(categoryId: Long): Flow<Int> = transactionDao.getTransactionCountByCategoryId(categoryId)

    suspend fun reassignCategoryTransactions(fromCategoryId: Long, toCategory: CategoryEntity) = withContext(Dispatchers.IO) {
        val affected = transactionDao.getTransactionsByCategoryId(fromCategoryId)
        for (tx in affected) transactionDao.updateTransaction(tx.copy(categoryId = toCategory.id, categoryName = toCategory.name, categoryIcon = toCategory.iconName, categoryColorHex = toCategory.colorHex))
    }

    fun getBudgetForMonth(yearMonth: String): Flow<BudgetEntity?> = budgetDao.getBudgetForMonth(yearMonth)
    suspend fun getBudgetForMonthDirect(yearMonth: String): BudgetEntity? = budgetDao.getBudgetForMonthDirect(yearMonth)
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    suspend fun getAllBudgetsList(): List<BudgetEntity> = budgetDao.getAllBudgetsList()
    suspend fun setBudget(yearMonth: String, budgetInPaise: Long) = withContext(Dispatchers.IO) { budgetDao.insertOrUpdateBudget(BudgetEntity(yearMonth = yearMonth, budgetInPaise = budgetInPaise)) }
    suspend fun deleteBudgetForMonth(yearMonth: String) = withContext(Dispatchers.IO) { budgetDao.deleteBudgetForMonth(yearMonth) }

    fun getSetting(key: String): Flow<String?> = appSettingDao.getSettingFlow(key)
    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) { appSettingDao.setSetting(AppSettingEntity(key = key, value = value)) }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions(); budgetDao.deleteAllBudgets(); categoryDao.deleteAllCategories()
        categoryDao.insertAll(AppDatabase.DEFAULT_EXPENSE_CATEGORIES); categoryDao.insertAll(AppDatabase.DEFAULT_INCOME_CATEGORIES)
    }

    suspend fun checkAndSeedDefaults() = withContext(Dispatchers.IO) {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertAll(AppDatabase.DEFAULT_EXPENSE_CATEGORIES); categoryDao.insertAll(AppDatabase.DEFAULT_INCOME_CATEGORIES)
        }
    }
}
