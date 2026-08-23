package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.database.dao.AppSettingDao
import com.example.data.local.database.dao.BudgetDao
import com.example.data.local.database.dao.CategoryDao
import com.example.data.local.database.dao.TransactionDao
import com.example.data.local.database.entity.AppSettingEntity
import com.example.data.local.database.entity.BudgetEntity
import com.example.data.local.database.entity.CategoryEntity
import com.example.data.local.database.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            CategoryEntity(name = "Food", type = "EXPENSE", iconName = "Restaurant", colorHex = "#FF7043", isDefault = true),
            CategoryEntity(name = "Groceries", type = "EXPENSE", iconName = "ShoppingCart", colorHex = "#66BB6A", isDefault = true),
            CategoryEntity(name = "Transport", type = "EXPENSE", iconName = "DirectionsCar", colorHex = "#42A5F5", isDefault = true),
            CategoryEntity(name = "Fuel", type = "EXPENSE", iconName = "LocalGasStation", colorHex = "#FFA726", isDefault = true),
            CategoryEntity(name = "Shopping", type = "EXPENSE", iconName = "ShoppingBag", colorHex = "#AB47BC", isDefault = true),
            CategoryEntity(name = "Bills", type = "EXPENSE", iconName = "ReceiptLong", colorHex = "#26A69A", isDefault = true),
            CategoryEntity(name = "Electricity", type = "EXPENSE", iconName = "Bolt", colorHex = "#FBC02D", isDefault = true),
            CategoryEntity(name = "Mobile", type = "EXPENSE", iconName = "PhoneAndroid", colorHex = "#29B6F6", isDefault = true),
            CategoryEntity(name = "Internet", type = "EXPENSE", iconName = "Wifi", colorHex = "#5C6BC0", isDefault = true),
            CategoryEntity(name = "Rent", type = "EXPENSE", iconName = "Home", colorHex = "#8D6E63", isDefault = true),
            CategoryEntity(name = "Medical", type = "EXPENSE", iconName = "MedicalServices", colorHex = "#EF5350", isDefault = true),
            CategoryEntity(name = "Entertainment", type = "EXPENSE", iconName = "Movie", colorHex = "#EC407A", isDefault = true),
            CategoryEntity(name = "Education", type = "EXPENSE", iconName = "School", colorHex = "#7E57C2", isDefault = true),
            CategoryEntity(name = "Travel", type = "EXPENSE", iconName = "Flight", colorHex = "#26C6DA", isDefault = true),
            CategoryEntity(name = "Other", type = "EXPENSE", iconName = "Category", colorHex = "#78909C", isDefault = true)
        )

        val DEFAULT_INCOME_CATEGORIES = listOf(
            CategoryEntity(name = "Salary", type = "INCOME", iconName = "Payments", colorHex = "#4CAF50", isDefault = true),
            CategoryEntity(name = "Business", type = "INCOME", iconName = "Storefront", colorHex = "#2E7D32", isDefault = true),
            CategoryEntity(name = "Freelance", type = "INCOME", iconName = "LaptopMac", colorHex = "#00897B", isDefault = true),
            CategoryEntity(name = "Bonus", type = "INCOME", iconName = "CardGiftcard", colorHex = "#F57F17", isDefault = true),
            CategoryEntity(name = "Investment", type = "INCOME", iconName = "TrendingUp", colorHex = "#1E88E5", isDefault = true),
            CategoryEntity(name = "Other", type = "INCOME", iconName = "Savings", colorHex = "#78909C", isDefault = true)
        )
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.categoryDao().insertAll(DEFAULT_EXPENSE_CATEGORIES)
                    database.categoryDao().insertAll(DEFAULT_INCOME_CATEGORIES)
                }
            }
        }
    }
}
