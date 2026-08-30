package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import com.example.ui.settings.TagsManagerScreen
import com.example.ui.transactions.AddEditTransactionScreen
import com.example.ui.transactions.TransactionsHistoryScreen
import com.example.ui.transactions.WalletManagerScreen
import com.example.ui.upi.UPITransactionsScreen
import com.example.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object Calendar : Screen("calendar", "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Reports : Screen("reports", "Reports", Icons.Filled.PieChart, Icons.Outlined.PieChart)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavScreens = listOf(Screen.Dashboard, Screen.Transactions, Screen.Calendar, Screen.Reports, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseApp(viewModel: ExpenseViewModel, navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = bottomNavScreens.firstOrNull { it.route == currentRoute } ?: Screen.Dashboard
    val isBottomBarVisible = bottomNavScreens.any { it.route == currentRoute }
    val isUpiScreen = currentRoute == "upi_transactions"
    val isTagsManagerScreen = currentRoute == "manage_tags"
    val isCategoriesScreen = currentRoute == "categories"
    val isWalletManagerScreen = currentRoute == "manage_wallets"
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isBottomBarVisible || isUpiScreen || isTagsManagerScreen) {
                TopAppBar(
                    title = { Text(text = when { isUpiScreen -> "UPI Transactions"; isTagsManagerScreen -> "Manage Tags"; else -> currentScreen.title }, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 0.5.sp)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    navigationIcon = { if (isUpiScreen || isTagsManagerScreen) IconButton(onClick = { navController.popBackStack() }) { Text("‹", fontSize = 36.sp) } },
                    actions = {
                        if (isBottomBarVisible) {
                            IconButton(onClick = { navController.navigate("upi_transactions") }) { Text("@", fontSize = 22.sp) }
                            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { DropdownMenuItem(text = { Text("Logout") }, onClick = { menuExpanded = false; FirebaseAuth.getInstance().signOut() }) }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar { bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(icon = { Icon(imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon, contentDescription = screen.title) }, label = { Text(text = screen.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }, selected = isSelected, onClick = { if (currentRoute != screen.route) navController.navigate(screen.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }, modifier = Modifier.testTag("nav_item_${screen.route}"))
                } }
            }
        }
    ) { innerPadding ->
        val navHostModifier = if (isCategoriesScreen || isWalletManagerScreen) {
            Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier.fillMaxSize().padding(innerPadding)
        }
        NavHost(navController = navController, startDestination = Screen.Dashboard.route, modifier = navHostModifier) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel, { navController.navigate("add_transaction") }, { id -> navController.navigate("edit_transaction/$id") }, { navController.navigate(Screen.Transactions.route) }, { navController.navigate("budget") }) }
            composable(Screen.Transactions.route) { TransactionsHistoryScreen(viewModel, { navController.navigate("add_transaction") }, { id -> navController.navigate("edit_transaction/$id") }) }
            composable(Screen.Calendar.route) { CalendarScreen(viewModel) }
            composable(Screen.Reports.route) { ReportsScreen(viewModel, { navController.navigate("add_transaction") }) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel, { navController.navigate("categories") }, { navController.navigate("manage_wallets") }, { navController.navigate("manage_tags") }) }
            composable("upi_transactions") { UPITransactionsScreen(viewModel) }
            composable("manage_tags") { TagsManagerScreen { navController.popBackStack() } }
            composable("manage_wallets") { WalletManagerScreen { navController.popBackStack() } }
            composable("add_transaction") { AddEditTransactionScreen(viewModel, 0L, { navController.popBackStack() }, { navController.navigate("categories") }) }
            composable("edit_transaction/{transactionId}", arguments = listOf(navArgument("transactionId") { type = NavType.LongType })) { entry -> AddEditTransactionScreen(viewModel, entry.arguments?.getLong("transactionId") ?: 0L, { navController.popBackStack() }, { navController.navigate("categories") }) }
            composable("categories") { CategoriesScreen(viewModel, { navController.popBackStack() }) }
            composable("budget") { BudgetScreen(viewModel, { navController.popBackStack() }) }
        }
    }
}
