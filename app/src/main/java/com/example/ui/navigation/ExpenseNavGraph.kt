package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.budget.BudgetScreen
import com.example.ui.calendar.CalendarScreen
import com.example.ui.categories.CategoriesScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.transactions.AddEditTransactionScreen
import com.example.ui.transactions.TransactionsHistoryScreen
import com.example.viewmodel.ExpenseViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object Calendar : Screen("calendar", "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Reports : Screen("reports", "Reports", Icons.Filled.PieChart, Icons.Outlined.PieChart)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Calendar,
    Screen.Reports,
    Screen.Settings
)

@Composable
fun ExpenseApp(
    viewModel: ExpenseViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isBottomBarVisible = bottomNavScreens.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { navController.navigate("add_transaction") },
                    onNavigateToEditTransaction = { id -> navController.navigate("edit_transaction/$id") },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToBudget = { navController.navigate("budget") }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsHistoryScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { navController.navigate("add_transaction") },
                    onNavigateToEditTransaction = { id -> navController.navigate("edit_transaction/$id") }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = viewModel)
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { navController.navigate("add_transaction") }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToCategories = { navController.navigate("categories") },
                    onNavigateToBudget = { navController.navigate("budget") }
                )
            }

            composable("add_transaction") {
                AddEditTransactionScreen(
                    viewModel = viewModel,
                    transactionId = 0L,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.navigate("categories") }
                )
            }

            composable(
                route = "edit_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                AddEditTransactionScreen(
                    viewModel = viewModel,
                    transactionId = txId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.navigate("categories") }
                )
            }

            composable("categories") {
                CategoriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("budget") {
                BudgetScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
