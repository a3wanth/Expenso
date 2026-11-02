package com.expenso.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expenso.app.ExpensoApplication
import com.expenso.app.data.TxType
import com.expenso.app.ui.RootViewModel
import com.expenso.app.ui.analytics.AnalyticsScreen
import com.expenso.app.ui.analytics.AnalyticsViewModel
import com.expenso.app.ui.applock.AppLockScreen
import com.expenso.app.ui.components.FloatingBottomNav
import com.expenso.app.ui.components.bottomDestinations
import com.expenso.app.ui.emi.BillEditorScreen
import com.expenso.app.ui.emi.BillEditorViewModel
import com.expenso.app.ui.emi.CalendarScreen
import com.expenso.app.ui.emi.CalendarViewModel
import com.expenso.app.ui.emi.EmiScreen
import com.expenso.app.ui.emi.EmiViewModel
import com.expenso.app.ui.emi.LoanDetailScreen
import com.expenso.app.ui.emi.LoanDetailViewModel
import com.expenso.app.ui.emi.LoanEditorScreen
import com.expenso.app.ui.emi.LoanEditorViewModel
import com.expenso.app.ui.expenses.ExpensesScreen
import com.expenso.app.ui.expenses.ExpensesViewModel
import com.expenso.app.ui.expenses.TransactionEditorScreen
import com.expenso.app.ui.expenses.TransactionEditorViewModel
import com.expenso.app.ui.home.HomeScreen
import com.expenso.app.ui.home.HomeViewModel
import com.expenso.app.ui.settings.BudgetsScreen
import com.expenso.app.ui.settings.BudgetsViewModel
import com.expenso.app.ui.settings.CategoriesScreen
import com.expenso.app.ui.settings.CategoriesViewModel
import com.expenso.app.ui.settings.SettingsScreen
import com.expenso.app.ui.settings.SettingsViewModel
import com.expenso.app.ui.theme.ExpensoTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_EXPENSES = "expenses"
private const val ROUTE_ANALYTICS = "analytics"
private const val ROUTE_EMI = "emi"
private const val ROUTE_SETTINGS = "settings"

private const val TAB_DURATION_MS = 240
private const val TAB_FADE_MS = 170
private const val DETAIL_DURATION_MS = 280
private const val DETAIL_FADE_MS = 200

private val tabRoutes = listOf(ROUTE_HOME, ROUTE_EXPENSES, ROUTE_ANALYTICS, ROUTE_EMI, ROUTE_SETTINGS)

private fun routeTabIndex(route: String?): Int = tabRoutes.indexOf(route)

private val NavBackStackEntry.isTabDestination: Boolean
    get() = routeTabIndex(destination.route) >= 0

/** Direction of a tab change: +1 moves right through the tab order, -1 moves left. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = routeTabIndex(initialState.destination.route)
    val to = routeTabIndex(targetState.destination.route)
    return if (to >= from) 1 else -1
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.navigationEnter(): EnterTransition {
    if (initialState.isTabDestination && targetState.isTabDestination) {
        val direction = tabDirection()
        return fadeIn(tween(TAB_FADE_MS)) +
            slideInHorizontally(tween(TAB_DURATION_MS, easing = FastOutSlowInEasing)) {
                direction * it / 14
            }
    }
    return fadeIn(tween(DETAIL_FADE_MS)) +
        slideInHorizontally(tween(DETAIL_DURATION_MS, easing = FastOutSlowInEasing)) { it / 4 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.navigationExit(): ExitTransition {
    if (initialState.isTabDestination && targetState.isTabDestination) {
        val direction = tabDirection()
        return fadeOut(tween(TAB_FADE_MS)) +
            slideOutHorizontally(tween(TAB_DURATION_MS, easing = FastOutSlowInEasing)) {
                -direction * it / 14
            }
    }
    return fadeOut(tween(DETAIL_FADE_MS)) +
        slideOutHorizontally(tween(DETAIL_DURATION_MS, easing = FastOutSlowInEasing)) { -it / 10 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.navigationPopEnter(): EnterTransition {
    if (initialState.isTabDestination && targetState.isTabDestination) {
        val direction = tabDirection()
        return fadeIn(tween(TAB_FADE_MS)) +
            slideInHorizontally(tween(TAB_DURATION_MS, easing = FastOutSlowInEasing)) {
                direction * it / 14
            }
    }
    return fadeIn(tween(DETAIL_FADE_MS)) +
        slideInHorizontally(tween(DETAIL_DURATION_MS, easing = FastOutSlowInEasing)) { -it / 8 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.navigationPopExit(): ExitTransition {
    if (initialState.isTabDestination && targetState.isTabDestination) {
        val direction = tabDirection()
        return fadeOut(tween(TAB_FADE_MS)) +
            slideOutHorizontally(tween(TAB_DURATION_MS, easing = FastOutSlowInEasing)) {
                -direction * it / 14
            }
    }
    return fadeOut(tween(DETAIL_FADE_MS)) +
        slideOutHorizontally(tween(DETAIL_DURATION_MS, easing = FastOutSlowInEasing)) { it / 5 }
}

@Composable
fun ExpensoApp() {
    val context = LocalContext.current
    val container = (context.applicationContext as ExpensoApplication).container

    val rootVm: RootViewModel = viewModel(
        factory = vmFactory { RootViewModel(container.settings) }
    )
    val preferences by rootVm.preferences.collectAsStateWithLifecycle()
    val unlocked by rootVm.unlocked.collectAsStateWithLifecycle()
    val pinError by rootVm.pinError.collectAsStateWithLifecycle()

    val prefs = preferences

    ExpensoTheme(themeMode = prefs?.themeMode ?: "SYSTEM") {
        when {
            prefs == null -> LoadingScreen()
            prefs.lockActive && !unlocked -> AppLockScreen(
                name = prefs.name,
                initials = prefs.initials,
                avatarId = prefs.avatarId,
                error = pinError,
                onUnlock = rootVm::unlock,
                onClearError = rootVm::clearPinError
            )
            else -> MainNavigation(rootViewModel = rootVm)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MainNavigation(rootViewModel: RootViewModel) {
    val context = LocalContext.current
    val container = (context.applicationContext as ExpensoApplication).container
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomDestinations.any { it.route == currentRoute }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { navigationEnter() },
            exitTransition = { navigationExit() },
            popEnterTransition = { navigationPopEnter() },
            popExitTransition = { navigationPopExit() }
        ) {
            composable(ROUTE_HOME) {
                val vm: HomeViewModel = viewModel(
                    factory = vmFactory { HomeViewModel(container.repository, container.settings) }
                )
                HomeScreen(
                    vm = vm,
                    onAddExpense = { navController.navigate("transaction?type=${TxType.EXPENSE}") },
                    onAddIncome = { navController.navigate("transaction?type=${TxType.INCOME}") },
                    onOpenBudgets = { navController.navigate("budgets") },
                    onOpenCalendar = { navController.navigate("calendar") },
                    onOpenSettings = { navController.navigateToTab(ROUTE_SETTINGS) },
                    onOpenExpenses = { navController.navigateToTab(ROUTE_EXPENSES) },
                    onOpenEmi = { navController.navigateToTab(ROUTE_EMI) },
                    onEditTransaction = { id -> navController.navigate("transaction?txId=$id") }
                )
            }

            composable(ROUTE_EXPENSES) {
                val vm: ExpensesViewModel = viewModel(
                    factory = vmFactory { ExpensesViewModel(container.repository, container.settings) }
                )
                ExpensesScreen(
                    vm = vm,
                    onAddExpense = { navController.navigate("transaction?type=${TxType.EXPENSE}") },
                    onEditTransaction = { id -> navController.navigate("transaction?txId=$id") },
                    onOpenCalendar = { navController.navigate("calendar") }
                )
            }

            composable(ROUTE_ANALYTICS) {
                val vm: AnalyticsViewModel = viewModel(
                    factory = vmFactory { AnalyticsViewModel(container.repository, container.settings) }
                )
                AnalyticsScreen(
                    vm = vm,
                    onOpenLoan = { id -> navController.navigate("loan/$id") },
                    onOpenEmi = { navController.navigateToTab(ROUTE_EMI) }
                )
            }

            composable(ROUTE_EMI) {
                val vm: EmiViewModel = viewModel(
                    factory = vmFactory { EmiViewModel(container.repository, container.settings) }
                )
                EmiScreen(
                    vm = vm,
                    onOpenCalendar = { navController.navigate("calendar") },
                    onAddLoan = { navController.navigate("loanEditor") },
                    onEditLoan = { id -> navController.navigate("loanEditor?loanId=$id") },
                    onOpenLoan = { id -> navController.navigate("loan/$id") },
                    onAddBill = { navController.navigate("billEditor") },
                    onEditBill = { id -> navController.navigate("billEditor?billId=$id") }
                )
            }

            composable(ROUTE_SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = vmFactory { SettingsViewModel(container.repository, container.settings, container.reminders) }
                )
                SettingsScreen(
                    vm = vm,
                    onOpenBudgets = { navController.navigate("budgets") },
                    onOpenCategories = { navController.navigate("categories") },
                    onLockNow = rootViewModel::lockNow
                )
            }

            composable(
                route = "transaction?txId={txId}&type={type}",
                arguments = listOf(
                    navArgument("txId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = TxType.EXPENSE
                    }
                )
            ) { entry ->
                val txId = entry.arguments?.getLong("txId") ?: 0L
                val type = entry.arguments?.getString("type") ?: TxType.EXPENSE
                val vm: TransactionEditorViewModel = viewModel(
                    factory = vmFactory {
                        TransactionEditorViewModel(container.repository, container.settings, txId, type)
                    }
                )
                TransactionEditorScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("calendar") {
                val vm: CalendarViewModel = viewModel(
                    factory = vmFactory { CalendarViewModel(container.repository, container.settings) }
                )
                CalendarScreen(vm = vm, onBack = { navController.popBackStack() })
            }

            composable("budgets") {
                val vm: BudgetsViewModel = viewModel(
                    factory = vmFactory { BudgetsViewModel(container.repository, container.settings) }
                )
                BudgetsScreen(vm = vm, onBack = { navController.popBackStack() })
            }

            composable("categories") {
                val vm: CategoriesViewModel = viewModel(
                    factory = vmFactory { CategoriesViewModel(container.repository) }
                )
                CategoriesScreen(vm = vm, onBack = { navController.popBackStack() })
            }

            composable(
                route = "loan/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { entry ->
                val loanId = entry.arguments?.getLong("loanId") ?: 0L
                val vm: LoanDetailViewModel = viewModel(
                    factory = vmFactory {
                        LoanDetailViewModel(container.repository, container.settings, loanId)
                    }
                )
                LoanDetailScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("loanEditor?loanId=$id") }
                )
            }

            composable(
                route = "loanEditor?loanId={loanId}",
                arguments = listOf(
                    navArgument("loanId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                val loanId = entry.arguments?.getLong("loanId") ?: 0L
                val vm: LoanEditorViewModel = viewModel(
                    factory = vmFactory { LoanEditorViewModel(container.repository, loanId) }
                )
                LoanEditorScreen(vm = vm, onBack = { navController.popBackStack() })
            }

            composable(
                route = "billEditor?billId={billId}",
                arguments = listOf(
                    navArgument("billId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                val billId = entry.arguments?.getLong("billId") ?: 0L
                val vm: BillEditorViewModel = viewModel(
                    factory = vmFactory { BillEditorViewModel(container.repository, billId) }
                )
                BillEditorScreen(vm = vm, onBack = { navController.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 2 }
            ) + fadeIn(tween(160)),
            exit = slideOutVertically(
                animationSpec = tween(170, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 2 }
            ) + fadeOut(tween(120))
        ) {
            FloatingBottomNav(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    if (currentRoute != destination.route) {
                        navController.navigateToTab(destination.route)
                    }
                }
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private class SimpleViewModelFactory<T : ViewModel>(
    private val create: () -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}

private fun <T : ViewModel> vmFactory(create: () -> T): ViewModelProvider.Factory =
    SimpleViewModelFactory(create)
