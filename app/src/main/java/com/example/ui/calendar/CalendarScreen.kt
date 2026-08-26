package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { viewModel.previousMonth() }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("MONTHLY OVERVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.nextMonth() }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
        }

        Spacer(Modifier.height(10.dp))
        MonthlyOverviewCard(monthTotal, monthExpenses.size, previousTotal, changePercent, expensesByDay, maxDailyExpense)
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        val cells = buildList<Int?> { repeat(firstWeekday) { add(null) }; for (day in 1..daysInMonth) add(day) }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cells) { day ->
                if (day == null) Spacer(Modifier.height(70.dp))
                else CalendarDay(day, expensesByDay[day] ?: 0L, maxDailyExpense, calendar)
            }
        }
    }
}

@Composable
private fun MonthlyOverviewCard(total: Long, count: Int, previousTotal: Long, changePercent: Int, expensesByDay: Map<Int, Long>, maxDailyExpense: Long) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF272324)).padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text("MONTHLY EXPENSES", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("•  $count expenses", style = MaterialTheme.typography.labelMedium, color = Color(0xFF454042), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("₹${formatAmount(total)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            if (previousTotal > 0L) {
                val sign = if (changePercent >= 0) "+" else ""
                Text("vs previous month ₹${formatAmount(previousTotal)}  $sign$changePercent%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.48f))
            }
            Spacer(Modifier.height(16.dp))
            ExpenseBars(expensesByDay, maxDailyExpense)
        }
    }
}

@Composable
private fun ExpenseBars(expensesByDay: Map<Int, Long>, maxDailyExpense: Long) {
    val points = (1..31).map { expensesByDay[it] ?: 0L }
    Row(modifier = Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        points.forEach { value ->
            val fraction = if (maxDailyExpense > 0) (value.toFloat() / maxDailyExpense.toFloat()).coerceIn(0.08f, 1f) else 0.05f
            Box(modifier = Modifier.weight(1f).fillMaxHeight(fraction).clip(RoundedCornerShape(5.dp)).background(if (value > 0) Color(0xFFFF9A9F) else Color.White.copy(alpha = 0.05f)))
        }
    }
    Spacer(Modifier.height(5.dp))
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
    val borderModifier = if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(18.dp)) else Modifier

    Box(modifier = Modifier.fillMaxWidth().height(70.dp).then(borderModifier).clip(RoundedCornerShape(18.dp)).background(background).padding(7.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(day.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                if (isToday) Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
            }
            if (expenseInPaise > 0) {
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF202020)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text("₹${formatAmount(expenseInPaise)}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun isSameMonth(timestamp: Long, selected: Calendar): Boolean { val c = Calendar.getInstance().apply { timeInMillis = timestamp }; return c.get(Calendar.YEAR) == selected.get(Calendar.YEAR) && c.get(Calendar.MONTH) == selected.get(Calendar.MONTH) }
private fun dayOfMonth(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
private fun formatAmount(paise: Long): String { val rupees = paise / 100; val decimals = (paise % 100).toString().padStart(2, '0'); return "$rupees.$decimals" }
