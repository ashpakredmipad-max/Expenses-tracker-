package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val calendar by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    val firstDay = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = firstDay.get(Calendar.DAY_OF_WEEK) - 1
    val monthExpenses = transactions.filter { it.type.equals("EXPENSE", true) && isSameMonth(it.date, calendar) }
    val previousMonth = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val previousExpenses = transactions.filter { it.type.equals("EXPENSE", true) && isSameMonth(it.date, previousMonth) }
    val expensesByDay = monthExpenses.groupBy { dayOfMonth(it.date) }.mapValues { (_, list) -> list.sumOf { it.amountInPaise } }
    val monthTotal = monthExpenses.sumOf { it.amountInPaise }
    val previousTotal = previousExpenses.sumOf { it.amountInPaise }
    val maxDailyExpense = expensesByDay.values.maxOrNull() ?: 0L
    val changePercent = if (previousTotal > 0) ((monthTotal - previousTotal).toDouble() / previousTotal.toDouble() * 100).toInt() else 0

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            MonthArrowButton(onClick = { viewModel.previousMonth() }, contentDescription = "Previous month")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("MONTHLY OVERVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            MonthArrowButton(onClick = { viewModel.nextMonth() }, contentDescription = "Next month")
        }
        MonthlyOverviewCard(monthTotal, monthExpenses.size, previousTotal, changePercent, expensesByDay, maxDailyExpense)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(4.dp))
        val cells = buildList<Int?> { repeat(firstWeekday) { add(null) }; for (day in 1..daysInMonth) add(day) }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(cells) { day -> if (day == null) Spacer(Modifier.height(61.dp)) else CalendarDay(day, expensesByDay[day] ?: 0L, maxDailyExpense, calendar) }
        }
    }
}

@Composable
private fun MonthArrowButton(onClick: () -> Unit, contentDescription: String) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp).graphicsLayer(shape = CircleShape, clip = true).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)) {
        Icon(if (contentDescription.startsWith("Previous")) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, contentDescription = contentDescription, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun MonthlyOverviewCard(total: Long, count: Int, previousTotal: Long, changePercent: Int, expensesByDay: Map<Int, Long>, maxDailyExpense: Long) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF272324)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(34.dp).graphicsLayer(shape = CircleShape, clip = true).background(Color.White.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.size(8.dp))
                    Text("MONTHLY EXPENSES", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium)
                }
                Box(modifier = Modifier.graphicsLayer(shape = RoundedCornerShape(20.dp), clip = true).background(Color.White).padding(horizontal = 10.dp, vertical = 5.dp)) { Text("•  $count expenses", style = MaterialTheme.typography.labelMedium, color = Color(0xFF454042), fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
            Text("₹${formatAmount(total)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            if (previousTotal > 0L) {
                val sign = if (changePercent >= 0) "+" else ""
                Text("vs previous month ₹${formatAmount(previousTotal)}  $sign$changePercent%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.48f))
            }
            Spacer(Modifier.height(12.dp))
            ExpenseBars(expensesByDay, maxDailyExpense)
        }
    }
}

@Composable
private fun ExpenseBars(expensesByDay: Map<Int, Long>, maxDailyExpense: Long) {
    val points = (1..31).map { expensesByDay[it] ?: 0L }
    Row(modifier = Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        points.forEach { value ->
            val fraction = if (maxDailyExpense > 0) (value.toFloat() / maxDailyExpense.toFloat()).coerceIn(0.08f, 1f) else 0.05f
            Box(modifier = Modifier.weight(1f).fillMaxHeight(fraction).graphicsLayer(shape = RoundedCornerShape(5.dp), clip = true).background(if (value > 0) Color(0xFFFF9A9F) else Color.White.copy(alpha = 0.05f)))
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Aug 1", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f))
        Text("Aug 15", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f))
        Text("Aug 31", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f))
    }
}

@Composable
private fun CalendarDay(day: Int, expenseInPaise: Long, maxDailyExpense: Long, selectedMonth: Calendar) {
    val intensity = if (maxDailyExpense > 0L) (expenseInPaise.toFloat() / maxDailyExpense.toFloat()).coerceIn(0f, 1f) else 0f
    val background = if (expenseInPaise > 0L) Color(0xFFFF6B73).copy(alpha = 0.08f + intensity * 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    val today = Calendar.getInstance()
    val isToday = today.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR) && today.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) == day
    val borderModifier = if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp)) else Modifier
    Box(modifier = Modifier.fillMaxWidth().height(61.dp).then(borderModifier).graphicsLayer(shape = RoundedCornerShape(16.dp), clip = true).background(background).padding(horizontal = 3.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(day.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                if (isToday) Box(Modifier.size(4.dp).graphicsLayer(shape = CircleShape, clip = true).background(MaterialTheme.colorScheme.onSurface))
            }
            if (expenseInPaise > 0) {
                Box(modifier = Modifier.graphicsLayer(shape = RoundedCornerShape(7.dp), clip = true).background(Color(0xFF202020)).padding(horizontal = 3.dp, vertical = 1.dp)) { Text("₹${formatAmountWhole(expenseInPaise)}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1) }
            } else Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

private fun isSameMonth(timestamp: Long, selected: Calendar): Boolean { val c = Calendar.getInstance().apply { timeInMillis = timestamp }; return c.get(Calendar.YEAR) == selected.get(Calendar.YEAR) && c.get(Calendar.MONTH) == selected.get(Calendar.MONTH) }
private fun dayOfMonth(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
private fun formatAmount(paise: Long): String { val rupees = paise / 100; val decimals = (paise % 100).toString().padStart(2, '0'); return "$rupees.$decimals" }
private fun formatAmountWhole(paise: Long): String = (paise / 100).toString()
