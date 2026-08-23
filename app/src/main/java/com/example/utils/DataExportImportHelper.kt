package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.local.database.entity.BudgetEntity
import com.example.data.local.database.entity.CategoryEntity
import com.example.data.local.database.entity.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

data class ImportResult(
    val successCount: Int,
    val skippedCount: Int,
    val transactions: List<TransactionEntity>
)

data class BackupData(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>
)

object DataExportImportHelper {

    fun exportTransactionsToCsv(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    // CSV Header
                    writer.write("Date,Type,Category,Amount,Note\n")

                    for (item in transactions) {
                        val dateStr = DateUtils.formatCsvDate(item.date)
                        val typeStr = item.type
                        val categoryStr = escapeCsv(item.categoryName)
                        val amountDecimal = CurrencyUtils.paiseToInputString(item.amountInPaise)
                        val noteStr = escapeCsv(item.note)

                        writer.write("$dateStr,$typeStr,$categoryStr,$amountDecimal,$noteStr\n")
                    }
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importTransactionsFromCsv(
        context: Context,
        uri: Uri,
        existingCategories: List<CategoryEntity>
    ): ImportResult {
        var successCount = 0
        var skippedCount = 0
        val importedList = mutableListOf<TransactionEntity>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    var isHeader = true
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty()) continue

                        if (isHeader) {
                            isHeader = false
                            // If the line looks like header, skip it
                            if (currentLine.startsWith("Date", ignoreCase = true) ||
                                currentLine.contains("Type", ignoreCase = true)) {
                                continue
                            }
                        }

                        val tokens = parseCsvLine(currentLine)
                        if (tokens.size < 4) {
                            skippedCount++
                            continue
                        }

                        val dateStr = tokens[0].trim()
                        val typeStr = tokens[1].trim().uppercase()
                        val categoryName = tokens[2].trim()
                        val amountStr = tokens[3].trim()
                        val noteStr = if (tokens.size > 4) tokens[4].trim() else ""

                        val timestamp = DateUtils.parseCsvDate(dateStr)
                        val paise = CurrencyUtils.parseAmountToPaise(amountStr)
                        val type = if (typeStr == "INCOME" || typeStr == "EXPENSE") typeStr else null

                        if (timestamp == null || paise == null || type == null || categoryName.isEmpty()) {
                            skippedCount++
                            continue
                        }

                        // Match category or use default fallback
                        val matchedCategory = existingCategories.firstOrNull {
                            it.name.equals(categoryName, ignoreCase = true) && it.type.equals(type, ignoreCase = true)
                        } ?: existingCategories.firstOrNull {
                            it.name.equals("Other", ignoreCase = true) && it.type.equals(type, ignoreCase = true)
                        }

                        val catId = matchedCategory?.id ?: 0L
                        val catIcon = matchedCategory?.iconName ?: "Category"
                        val catColor = matchedCategory?.colorHex ?: "#78909C"

                        importedList.add(
                            TransactionEntity(
                                id = 0,
                                type = type,
                                amountInPaise = paise,
                                categoryId = catId,
                                categoryName = categoryName,
                                categoryIcon = catIcon,
                                categoryColorHex = catColor,
                                date = timestamp,
                                note = noteStr,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        successCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ImportResult(successCount, skippedCount, importedList)
    }

    fun exportBackupJson(
        context: Context,
        uri: Uri,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>
    ): Boolean {
        return try {
            val root = JSONObject().apply {
                put("version", 1)
                put("appName", "Expense Tracker")
                put("exportTimestamp", System.currentTimeMillis())

                val catArray = JSONArray()
                for (cat in categories) {
                    val catObj = JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("type", cat.type)
                        put("iconName", cat.iconName)
                        put("colorHex", cat.colorHex)
                        put("isDefault", cat.isDefault)
                    }
                    catArray.put(catObj)
                }
                put("categories", catArray)

                val txArray = JSONArray()
                for (tx in transactions) {
                    val txObj = JSONObject().apply {
                        put("id", tx.id)
                        put("type", tx.type)
                        put("amountInPaise", tx.amountInPaise)
                        put("categoryId", tx.categoryId)
                        put("categoryName", tx.categoryName)
                        put("categoryIcon", tx.categoryIcon)
                        put("categoryColorHex", tx.categoryColorHex)
                        put("date", tx.date)
                        put("note", tx.note)
                        put("createdAt", tx.createdAt)
                    }
                    txArray.put(txObj)
                }
                put("transactions", txArray)

                val budgetArray = JSONArray()
                for (b in budgets) {
                    val bObj = JSONObject().apply {
                        put("yearMonth", b.yearMonth)
                        put("budgetInPaise", b.budgetInPaise)
                    }
                    budgetArray.put(bObj)
                }
                put("budgets", budgetArray)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importBackupJson(
        context: Context,
        uri: Uri
    ): BackupData? {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return null

            val root = JSONObject(content)
            if (!root.has("categories") || !root.has("transactions")) {
                return null
            }

            val categoryList = mutableListOf<CategoryEntity>()
            val catArray = root.getJSONArray("categories")
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                categoryList.add(
                    CategoryEntity(
                        id = obj.optLong("id", 0),
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        iconName = obj.optString("iconName", "Category"),
                        colorHex = obj.optString("colorHex", "#78909C"),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }

            val txList = mutableListOf<TransactionEntity>()
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                txList.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0),
                        type = obj.getString("type"),
                        amountInPaise = obj.getLong("amountInPaise"),
                        categoryId = obj.optLong("categoryId", 0),
                        categoryName = obj.getString("categoryName"),
                        categoryIcon = obj.optString("categoryIcon", "Category"),
                        categoryColorHex = obj.optString("categoryColorHex", "#78909C"),
                        date = obj.getLong("date"),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val budgetList = mutableListOf<BudgetEntity>()
            if (root.has("budgets")) {
                val budgetArray = root.getJSONArray("budgets")
                for (i in 0 until budgetArray.length()) {
                    val obj = budgetArray.getJSONObject(i)
                    budgetList.add(
                        BudgetEntity(
                            yearMonth = obj.getString("yearMonth"),
                            budgetInPaise = obj.getLong("budgetInPaise")
                        )
                    )
                }
            }

            BackupData(categoryList, txList, budgetList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        curVal.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }
}
