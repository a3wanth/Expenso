package com.expenso.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DueItem
import com.expenso.app.data.DueKind
import com.expenso.app.data.DueStatus
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.Money
import com.expenso.app.ui.components.Avatar
import com.expenso.app.ui.components.BudgetTrendChart
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.LegendDot
import com.expenso.app.ui.components.QuickAction
import com.expenso.app.ui.components.SectionHeader
import com.expenso.app.ui.components.SpendingBarChart
import com.expenso.app.ui.components.StatusPill
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.components.TransactionRow
import com.expenso.app.ui.components.categoryIconFor
import com.expenso.app.ui.theme.CardGradientIndigoEnd
import com.expenso.app.ui.theme.CardGradientIndigoStart
import com.expenso.app.ui.theme.LocalAppColors
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenEmi: () -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    var showNotifications by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currency = state.preferences.currencyCode
    val hidden = state.hiddenBalance

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    initials = state.preferences.initials,
                    avatarId = state.preferences.avatarId,
                    onClick = onOpenSettings
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${state.greeting},",
                        style = MaterialTheme.typography.bodySmall,
                        color = app.muted
                    )
                    Text(
                        text = state.preferences.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NotificationButton(
                    count = state.alertCount,
                    onClick = { showNotifications = true }
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            BalanceCard(
                state = state,
                currency = currency,
                hidden = hidden,
                onToggleVisibility = vm::toggleBalanceVisibility
            )
            Spacer(Modifier.height(18.dp))
        }

        if (state.overdue.isNotEmpty()) {
            item {
                OverdueBanner(
                    count = state.overdue.size,
                    amountCents = state.overdue.sumOf { it.amountCents },
                    currency = currency,
                    onClick = { showNotifications = true }
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAction(Icons.Filled.Add, "Expense", onAddExpense, Modifier.weight(1f))
                QuickAction(Icons.Filled.ArrowDownward, "Income", onAddIncome, Modifier.weight(1f))
                QuickAction(Icons.Filled.DonutLarge, "Budget", onOpenBudgets, Modifier.weight(1f))
                QuickAction(Icons.Filled.CalendarMonth, "Calendar", onOpenCalendar, Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            SectionHeader(
                title = "Monthly budget",
                actionText = "Manage",
                onAction = onOpenBudgets
            )
            Spacer(Modifier.height(11.dp))
            if (state.budgets.isEmpty()) {
                NoBudgetCard(onOpenBudgets)
            } else {
                BudgetCard(state, currency)
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            SurfaceCard(padding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spending",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Last 7 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = app.muted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (hidden) "••••" else Money.format(state.weeklyTotalCents, currency),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.recent.count { it.type == "EXPENSE" }} recent expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = app.muted
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                SpendingBarChart(
                    values = state.weekly.map { it.second },
                    labels = state.weekly.map {
                        it.first.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    },
                    dateLabels = state.weekly.map { it.first.dayOfMonth.toString() },
                    currencyCode = currency
                )
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            SectionHeader(
                title = "Upcoming payments",
                actionText = "View all",
                onAction = onOpenCalendar
            )
            Spacer(Modifier.height(11.dp))
            SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                if (state.upcoming.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wallet,
                            contentDescription = null,
                            tint = app.muted,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Nothing due in the next 30 days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = app.muted
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            onClick = onOpenEmi,
                            shape = RoundedCornerShape(12.dp),
                            color = app.indigoSoft
                        ) {
                            Text(
                                text = "Add a bill or EMI",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    state.upcoming.take(3).forEachIndexed { index, item ->
                        DueRow(item = item, currency = currency, hidden = hidden, onClick = onOpenCalendar)
                        if (index != state.upcoming.take(3).lastIndex) {
                            HorizontalDivider(color = app.line)
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            SectionHeader(
                title = "Recent activity",
                actionText = "See all",
                onAction = onOpenExpenses
            )
            Spacer(Modifier.height(11.dp))
            SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)) {
                if (state.recent.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = "No transactions yet",
                        message = "Add your first expense or income to see it here.",
                        actionText = "Add expense",
                        onAction = onAddExpense
                    )
                } else {
                    state.recent.forEachIndexed { index, transaction ->
                        TransactionRow(
                            transaction = transaction,
                            categoryColor = state.categoryColors[transaction.category] ?: "#625BFF",
                            currencyCode = currency,
                            hidden = hidden,
                            onClick = { onEditTransaction(transaction.id) }
                        )
                        if (index != state.recent.lastIndex) {
                            HorizontalDivider(color = app.line)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Text(
                text = "${state.monthLabel} · Expenso",
                style = MaterialTheme.typography.labelSmall,
                color = app.muted.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showNotifications) {
        ModalBottomSheet(
            onDismissRequest = { showNotifications = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NotificationsContent(
                overdue = state.overdue,
                upcoming = state.upcoming,
                currency = currency,
                onOpenCalendar = {
                    showNotifications = false
                    onOpenCalendar()
                },
                onOpenEmi = {
                    showNotifications = false
                    onOpenEmi()
                }
            )
        }
    }
}

@Composable
private fun NotificationButton(count: Int, onClick: () -> Unit) {
    val app = LocalAppColors.current
    Box {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, app.line),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.NotificationsNone,
                    contentDescription = if (count > 0) "Notifications, $count alerts" else "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(app.expense),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(
    state: HomeUiState,
    currency: String,
    hidden: Boolean,
    onToggleVisibility: () -> Unit
) {
    val app = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(CardGradientIndigoStart, CardGradientIndigoEnd)))
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.07f),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 1.02f, -size.height * 0.08f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = size.width * 0.30f,
                    center = Offset(size.width * 0.06f, size.height * 1.06f)
                )
            }
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.76f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = state.monthLabel.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onToggleVisibility, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (hidden) "Show balance" else "Hide balance",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                targetState = hidden,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "balanceVisibility"
            ) { isHidden ->
                Text(
                    text = if (isHidden) "••••••" else Money.format(state.balanceCents, currency),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.monthSavingsCents >= 0) {
                    "You're up ${Money.format(state.monthSavingsCents, currency)} this month"
                } else {
                    "You're down ${Money.formatAbs(state.monthSavingsCents, currency)} this month"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f)
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.16f))
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                BalanceMetric(
                    icon = Icons.Filled.ArrowDownward,
                    label = "Income",
                    amount = if (hidden) "••••" else Money.format(state.monthIncomeCents, currency),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(Color.White.copy(alpha = 0.16f))
                )
                Spacer(Modifier.width(18.dp))
                BalanceMetric(
                    icon = Icons.Filled.ArrowUpward,
                    label = "Expenses",
                    amount = if (hidden) "••••" else Money.format(state.monthExpenseCents, currency),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BalanceMetric(
    icon: ImageVector,
    label: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverdueBanner(count: Int, amountCents: Long, currency: String, onClick: () -> Unit) {
    val app = LocalAppColors.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = app.overdueSoft,
        border = BorderStroke(1.dp, app.overdue.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = app.overdue,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$count overdue ${if (count == 1) "payment" else "payments"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = app.overdue
                )
                Text(
                    text = "${Money.format(amountCents, currency)} past due",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.overdue.copy(alpha = 0.85f)
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = app.overdue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NoBudgetCard(onOpenBudgets: () -> Unit) {
    val app = LocalAppColors.current
    SurfaceCard(padding = PaddingValues(16.dp), onClick = onOpenBudgets) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(app.indigoSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.DonutLarge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Set your first budget",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Track spending against a monthly limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
            }
            Text(
                "Set up",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BudgetCard(state: HomeUiState, currency: String) {
    val app = LocalAppColors.current
    val over = state.budgetSpentCents > state.budgetTotalCents
    val ratio = state.budgetProgress
    val color = when {
        over -> app.overdue
        ratio >= 0.85f -> app.warning
        else -> MaterialTheme.colorScheme.primary
    }
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${Money.format(state.budgetSpentCents, currency)} spent",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "of ${Money.format(state.budgetTotalCents, currency)} across ${state.budgets.size} categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
            }
            Text(
                text = "${(ratio * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
        Spacer(Modifier.height(13.dp))
        BudgetTrendChart(
            cumulative = state.budgetSeries,
            monthDays = state.monthDays,
            budgetCents = state.budgetTotalCents,
            color = color,
            currencyCode = currency
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = color, label = "Spent so far")
            if (state.budgetTotalCents > 0) {
                LegendDot(color = app.muted.copy(alpha = 0.7f), label = "Budget target")
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            MiniMetric("Remaining", Money.formatAbs(state.budgetTotalCents - state.budgetSpentCents, currency), Modifier.weight(1f))
            MiniMetric("Top category", state.budgets.maxByOrNull { it.spentCents }?.budget?.category ?: "—", Modifier.weight(1f))
            MiniMetric(
                "Status",
                if (over) "Over budget" else "On track",
                Modifier.weight(1f),
                valueColor = if (over) app.expense else app.income
            )
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.muted)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DueRow(item: DueItem, currency: String, hidden: Boolean, onClick: () -> Unit) {
    val app = LocalAppColors.current
    val (icon, tint) = when (item.kind) {
        DueKind.LOAN -> Icons.Filled.ReceiptLong to MaterialTheme.colorScheme.primary
        DueKind.CARD -> Icons.Filled.CreditCard to Color(0xFF0EA5E9)
        else -> Icons.Filled.CalendarMonth to app.warning
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = if (app.isDark) 0.2f else 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${FinanceMath.dueLabel(item)} · ${com.expenso.app.data.DateX.formatShortDate(item.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.status == DueStatus.OVERDUE) app.overdue else app.muted,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (hidden) "••••" else Money.format(item.amountCents, currency),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            StatusPill(
                status = item.status,
                label = when (item.status) {
                    DueStatus.OVERDUE -> "Overdue"
                    DueStatus.PAID -> "Paid"
                    else -> FinanceMath.dueLabel(item)
                }
            )
        }
    }
}

@Composable
private fun NotificationsContent(
    overdue: List<DueItem>,
    upcoming: List<DueItem>,
    currency: String,
    onOpenCalendar: () -> Unit,
    onOpenEmi: () -> Unit
) {
    val app = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your bills and loan reminders in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = app.muted
        )
        Spacer(Modifier.height(18.dp))
        if (overdue.isEmpty() && upcoming.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.NotificationsNone,
                title = "You're all caught up",
                message = "No overdue or upcoming payments right now."
            )
        } else {
            overdue.forEach { item ->
                NotificationItem(item, currency, onClick = onOpenCalendar)
            }
            upcoming.take(6).forEach { item ->
                NotificationItem(item, currency, onClick = onOpenCalendar)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onOpenCalendar,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = app.indigoSoft
            ) {
                Text(
                    "Open calendar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 13.dp)
                )
            }
            Surface(
                onClick = onOpenEmi,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    "Manage bills",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 13.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationItem(item: DueItem, currency: String, onClick: () -> Unit) {
    val app = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (item.status == DueStatus.OVERDUE) app.overdueSoft else app.warningSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.kind == DueKind.LOAN) Icons.Filled.ReceiptLong else Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = if (item.status == DueStatus.OVERDUE) app.overdue else app.warning,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${item.subtitle} · ${FinanceMath.dueLabel(item)}",
                style = MaterialTheme.typography.bodySmall,
                color = app.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = Money.format(item.amountCents, currency),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
