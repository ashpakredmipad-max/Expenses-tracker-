package com.example.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val expensesByDay = transactions.filter { it.type.equals("EXPENSE", true) && isSameMonth(it.date, calendar) }.groupBy { dayOfMonth(it.date) }.mapValues { (_, list) -> list.sumOf { it.amountInPaise } }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { viewModel.previousMonth() }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
            Text(monthTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.nextMonth() }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        val cells = buildList<Int?> { repeat(firstWeekday) { add(null) }; for (day in 1..daysInMonth) add(day) }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cells) { day -> if (day == null) Spacer(Modifier.height(72.dp)) else CalendarDay(day, expensesByDay[day] ?: 0L) }
        }
    }
}

@Composable
private fun CalendarDay(day: Int, expenseInPaise: Long) {
    Card(modifier = Modifier.fillMaxWidth().height(72.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text(day.toString(), fontWeight = FontWeight.Bold)
            Text(if (expenseInPaise > 0) "₹${formatAmount(expenseInPaise)}" else "—", style = MaterialTheme.typography.labelSmall, color = if (expenseInPaise > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun isSameMonth(timestamp: Long, selected: Calendar): Boolean { val c = Calendar.getInstance().apply { timeInMillis = timestamp }; return c.get(Calendar.YEAR) == selected.get(Calendar.YEAR) && c.get(Calendar.MONTH) == selected.get(Calendar.MONTH) }
private fun dayOfMonth(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
private fun formatAmount(paise: Long): String { val rupees = paise / 100; val decimals = (paise % 100).toString().padStart(2, '0'); return "$rupees.$decimals" }
