package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun getBudgetForMonth(yearMonth: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun getBudgetForMonthDirect(yearMonth: String): BudgetEntity?

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsList(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun deleteBudgetForMonth(yearMonth: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
