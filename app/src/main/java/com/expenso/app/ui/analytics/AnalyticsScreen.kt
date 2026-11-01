package com.expenso.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.LoanSummary
import com.expenso.app.data.Money
import com.expenso.app.data.local.CreditCardEntity
import com.expenso.app.ui.components.CategoryAvatar
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.DonutChart
import com.expenso.app.ui.components.DonutSlice
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.FormField
import com.expenso.app.ui.components.LegendDot
import com.expenso.app.ui.components.MiniProgressBar
import com.expenso.app.ui.components.MonthStepper
import com.expenso.app.ui.components.MonthlyTrendChart
import com.expenso.app.ui.components.ProgressRing
import com.expenso.app.ui.components.ScreenHeader
import com.expenso.app.ui.components.SectionHeader
import com.expenso.app.ui.components.SegmentedControl
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.components.parseHexColor
import com.expenso.app.ui.theme.CardGradientAmberEnd
import com.expenso.app.ui.theme.CardGradientAmberStart
import com.expenso.app.ui.theme.CardGradientIndigoEnd
import com.expenso.app.ui.theme.CardGradientIndigoStart
import com.expenso.app.ui.theme.CardGradientTealEnd
import com.expenso.app.ui.theme.CardGradientTealStart
import com.expenso.app.ui.theme.LocalAppColors
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
fun AnalyticsScreen(
    vm: AnalyticsViewModel,
    onOpenLoan: (Long) -> Unit,
    onOpenEmi: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    val currency = state.preferences.currencyCode

    var cardEditor by remember { mutableStateOf<CreditCardEntity?>(null) }
    var showNewCard by remember { mutableStateOf(false) }
    var paymentCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var deleteCard by remember { mutableStateOf<CreditCardEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp)
    ) {
        item {
            ScreenHeader(
                title = "Analytics",
                subtitle = "Understand where your money goes each month.",
                eyebrow = "Insights"
            )
            Spacer(Modifier.height(16.dp))
            SegmentedControl(
                options = listOf(AnalyticsTab.SPEND, AnalyticsTab.CREDIT, AnalyticsTab.EMI),
                selected = state.tab,
                label = {
                    when (it) {
                        AnalyticsTab.SPEND -> "Spend"
                        AnalyticsTab.CREDIT -> "Credit"
                        AnalyticsTab.EMI -> "EMI"
                    }
                },
                onSelect = vm::setTab
            )
            Spacer(Modifier.height(16.dp))
        }

        when (state.tab) {
            AnalyticsTab.SPEND -> {
                item {
                    MonthStepper(
                        label = state.monthLabel,
                        onPrevious = vm::previousMonth,
                        onNext = vm::nextMonth,
                        nextEnabled = !state.isCurrentMonth
                    )
                    Spacer(Modifier.height(14.dp))
                }

                item {
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = app.muted
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = Money.format(state.expenseCents, currency),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(7.dp))
                                val change = state.changePercent
                                Text(
                                    text = when {
                                        change == null -> "No comparison for last month"
                                        change > 0 -> "↑ ${"%.1f".format(Locale.US, change)}% vs last month"
                                        change < 0 -> "↓ ${"%.1f".format(Locale.US, -change)}% vs last month"
                                        else -> "Same as last month"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        change == null -> app.muted
                                        change > 0 -> app.expense
                                        else -> app.income
                                    }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(app.indigoSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = app.line)
                        Spacer(Modifier.height(13.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiMetric("Daily average", Money.format(state.dailyAverageCents, currency), Modifier.weight(1f))
                            KpiMetric("Income", Money.format(state.incomeCents, currency), Modifier.weight(1f), app.income)
                            KpiMetric("Transactions", state.transactionCount.toString(), Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                item {
                    SectionHeader(title = "By category")
                    Spacer(Modifier.height(10.dp))
                    if (state.categories.isEmpty()) {
                        SurfaceCard {
                            EmptyState(
                                icon = Icons.Filled.PieChart,
                                title = "No spending this month",
                                message = "Add an expense and your category breakdown will appear here."
                            )
                        }
                    } else {
                        SurfaceCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(
                                    slices = state.categories.map {
                                        DonutSlice(it.category, it.amountCents, parseHexColor(it.colorHex))
                                    },
                                    modifier = Modifier.size(142.dp),
                                    ringWidth = 18.dp
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Spent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = app.muted
                                        )
                                        Text(
                                            text = Money.compact(state.expenseCents, currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    state.categories.take(5).forEach { category ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(category.colorHex))
                                            )
                                            Spacer(Modifier.width(7.dp))
                                            Text(
                                                text = category.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${category.amountCents * 100 / state.expenseCents.coerceAtLeast(1)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = app.muted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                if (state.categories.isNotEmpty()) {
                    item {
                        SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                            state.categories.take(6).forEachIndexed { index, category ->
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryAvatar(name = category.category, colorHex = category.colorHex, size = 40.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = category.category,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = Money.format(category.amountCents, currency),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        MiniProgressBar(
                                            progress = category.amountCents.toFloat() /
                                                state.expenseCents.coerceAtLeast(1).toFloat(),
                                            color = parseHexColor(category.colorHex),
                                            height = 6.dp
                                        )
                                    }
                                }
                                if (index != state.categories.take(6).lastIndex) {
                                    HorizontalDivider(color = app.line)
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }

                item {
                    SectionHeader(title = "Income vs expenses", actionText = "6 months")
                    Spacer(Modifier.height(10.dp))
                    SurfaceCard {
                        MonthlyTrendChart(
                            months = state.trend.map { DateX.shortMonth(it.month) },
                            income = state.trend.map { it.incomeCents },
                            expense = state.trend.map { it.expenseCents },
                            currencyCode = currency
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }

                item {
                    val within = (state.expenseCents <= state.incomeCents)
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (within) app.incomeSoft else app.expenseSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = if (within) app.income else app.expense,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (within) "You're living within your income" else "Spending is ahead of income",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (within) {
                                        "You kept ${Money.format(state.incomeCents - state.expenseCents, currency)} this month."
                                    } else {
                                        "You spent ${Money.format(state.expenseCents - state.incomeCents, currency)} more than you earned."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = app.muted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            AnalyticsTab.CREDIT -> {
                item {
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(
                                progress = state.creditUtilization,
                                color = when {
                                    state.creditUtilization > 0.7f -> app.overdue
                                    state.creditUtilization > 0.4f -> app.warning
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(100.dp),
                                strokeWidth = 10.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(state.creditUtilization * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "used",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = app.muted
                                    )
                                }
                            }
                            Spacer(Modifier.width(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Credit used",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = app.muted
                                )
                                Text(
                                    text = Money.format(state.creditBalanceCents, currency),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = "of ${Money.format(state.creditLimitCents, currency)} limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = app.muted
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${Money.format(state.creditSpendCents, currency)} charged this month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    SectionHeader(
                        title = "Your cards",
                        actionText = "Add card",
                        onAction = { showNewCard = true }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (state.creditCards.isEmpty()) {
                    item {
                        SurfaceCard {
                            EmptyState(
                                icon = Icons.Filled.CreditCard,
                                title = "No credit cards yet",
                                message = "Add a card to track balances, limits and monthly due dates.",
                                actionText = "Add a card",
                                onAction = { showNewCard = true }
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                } else {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(state.creditCards, key = { it.id }) { card ->
                                CreditCardTile(
                                    card = card,
                                    currency = currency,
                                    onPay = { paymentCard = card },
                                    onEdit = { cardEditor = card },
                                    onDelete = { deleteCard = card }
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    item {
                        SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                            state.creditCards.forEachIndexed { index, card ->
                                Row(
                                    modifier = Modifier.padding(vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(creditGradient(card.colorIndex).first())
                                    )
                                    Spacer(Modifier.width(11.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${card.name} ••${card.last4}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Due day ${card.dueDay} · limit ${Money.format(card.limitCents, currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = app.muted
                                        )
                                    }
                                    Text(
                                        text = Money.format(card.balanceCents, currency),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (index != state.creditCards.lastIndex) {
                                    HorizontalDivider(color = app.line)
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }

            AnalyticsTab.EMI -> {
                item {
                    SurfaceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly EMI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = app.muted
                                )
                                Text(
                                    text = Money.format(state.monthlyEmiCents, currency),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiMetric("Outstanding", Money.format(state.outstandingCents, currency), Modifier.weight(1f))
                            KpiMetric("Interest left", Money.format(state.totalInterestCents, currency), Modifier.weight(1f))
                            KpiMetric("Completed", state.completedLoans.toString(), Modifier.weight(1f))
                        }
                        if (state.nextEmiDate != null) {
                            Spacer(Modifier.height(13.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = app.warningSoft
                            ) {
                                Text(
                                    text = "Next EMI due ${DateX.formatDate(state.nextEmiDate!!)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = app.warning,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    SectionHeader(
                        title = "Active loans",
                        actionText = "Manage",
                        onAction = onOpenEmi
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (state.loans.isEmpty()) {
                    item {
                        SurfaceCard {
                            EmptyState(
                                icon = Icons.Filled.Payments,
                                title = "No active loans",
                                message = "Add a loan to see EMI progress and your repayment schedule.",
                                actionText = "Manage loans",
                                onAction = onOpenEmi
                            )
                        }
                        Spacer(Modifier.height(22.dp))
                    }
                } else {
                    items(state.loans, key = { it.loan.id }) { summary ->
                        LoanAnalyticsCard(
                            summary = summary,
                            currency = currency,
                            onClick = { onOpenLoan(summary.loan.id) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    item { Spacer(Modifier.height(14.dp)) }
                }
            }
        }
    }

    if (showNewCard || cardEditor != null) {
        val editing = cardEditor
        CardEditorDialog(
            initial = editing,
            currency = currency,
            onDismiss = {
                showNewCard = false
                cardEditor = null
            },
            onSave = { name, last4, limit, balance, dueDay, colorIndex ->
                vm.saveCard(
                    id = editing?.id ?: 0,
                    name = name,
                    last4 = last4,
                    limitCents = limit,
                    balanceCents = balance,
                    dueDay = dueDay,
                    colorIndex = colorIndex
                )
                showNewCard = false
                cardEditor = null
            }
        )
    }

    paymentCard?.let { card ->
        CardPaymentDialog(
            card = card,
            currency = currency,
            onDismiss = { paymentCard = null },
            onPay = { amount ->
                vm.payCard(card, amount)
                paymentCard = null
            }
        )
    }

    deleteCard?.let { card ->
        ConfirmDialog(
            title = "Remove card?",
            message = "“${card.name}” will be removed from your credit overview. Past transactions are kept.",
            onConfirm = {
                vm.deleteCard(card.id)
                deleteCard = null
            },
            onDismiss = { deleteCard = null }
        )
    }
}

@Composable
private fun KpiMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppColors.current.muted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun creditGradient(index: Int): List<Color> = when (((index % 3) + 3) % 3) {
    0 -> listOf(CardGradientIndigoStart, CardGradientIndigoEnd)
    1 -> listOf(CardGradientTealStart, CardGradientTealEnd)
    else -> listOf(CardGradientAmberStart, CardGradientAmberEnd)
}

@Composable
private fun CreditCardTile(
    card: CreditCardEntity,
    currency: String,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val app = LocalAppColors.current
    val colors = creditGradient(card.colorIndex)
    val utilization = if (card.limitCents <= 0) 0f else (card.balanceCents.toFloat() / card.limitCents).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(292.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(colors))
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.09f),
                    radius = size.width * 0.38f,
                    center = Offset(size.width * 0.98f, size.height * 0.05f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.07f),
                    radius = size.width * 0.24f,
                    center = Offset(size.width * 0.1f, size.height * 1.02f)
                )
            }
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•••• ${card.last4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit ${card.name}",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Remove ${card.name}",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Balance",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = Money.format(card.balanceCents, currency),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            MiniProgressBar(
                progress = utilization,
                color = Color.White,
                modifier = Modifier.width(180.dp),
                height = 6.dp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "${(utilization * 100).toInt()}% of ${Money.format(card.limitCents, currency)} · Due ${card.dueDay}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (card.balanceCents == 0L) "Paid off" else "Payment due",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Surface(
                    onClick = onPay,
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    enabled = card.balanceCents > 0
                ) {
                    Text(
                        text = if (card.balanceCents > 0) "Pay card" else "Settled",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanAnalyticsCard(summary: LoanSummary, currency: String, onClick: () -> Unit) {
    val app = LocalAppColors.current
    val next = summary.nextInstallment
    SurfaceCard(
        padding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(app.indigoSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.loan.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (summary.loan.lender.isBlank()) {
                        "${summary.paidCount} of ${summary.loan.tenureMonths} installments paid"
                    } else {
                        "${summary.loan.lender} · ${summary.paidCount}/${summary.loan.tenureMonths} paid"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(summary.loan.emiCents, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "/month",
                    style = MaterialTheme.typography.labelSmall,
                    color = app.muted
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        MiniProgressBar(progress = summary.progress, color = MaterialTheme.colorScheme.primary, height = 7.dp)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(summary.progress * 100).toInt()}% repaid",
                style = MaterialTheme.typography.labelSmall,
                color = app.muted
            )
            Text(
                text = next?.let { "Next ${DateX.formatShortDate(it.dueDate)} · ${Money.format(it.amountCents, currency)}" }
                    ?: "All installments paid",
                style = MaterialTheme.typography.labelSmall,
                color = if (next == null) app.income else app.muted
            )
        }
    }
}

@Composable
private fun CardEditorDialog(
    initial: CreditCardEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long, Int, Int) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var last4 by remember(initial?.id) { mutableStateOf(initial?.last4 ?: "") }
    var limit by remember(initial?.id) {
        mutableStateOf(initial?.let { plainAmount(it.limitCents) } ?: "")
    }
    var balance by remember(initial?.id) {
        mutableStateOf(initial?.let { plainAmount(it.balanceCents) } ?: "")
    }
    var dueDay by remember(initial?.id) { mutableStateOf((initial?.dueDay ?: 15).toString()) }
    var colorIndex by remember(initial?.id) { mutableStateOf(initial?.colorIndex ?: 0) }
    var error by remember(initial?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add credit card" else "Edit card") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                FormField(label = "Card name", value = name, onValueChange = { name = it })
                Spacer(Modifier.height(10.dp))
                FormField(
                    label = "Last 4 digits",
                    value = last4,
                    onValueChange = { value -> last4 = value.filter { it.isDigit() }.take(4) },
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    label = "Credit limit",
                    value = limit,
                    onValueChange = { limit = it },
                    keyboardType = KeyboardType.Decimal,
                    leading = { Text(Money.symbol(currency)) }
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    label = "Current balance",
                    value = balance,
                    onValueChange = { balance = it },
                    keyboardType = KeyboardType.Decimal,
                    leading = { Text(Money.symbol(currency)) }
                )
                Spacer(Modifier.height(10.dp))
                FormField(
                    label = "Due day (1–28)",
                    value = dueDay,
                    onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) },
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Card colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalAppColors.current.muted
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(0, 1, 2).forEach { index ->
                        val colors = creditGradient(index)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors))
                                .clickable { colorIndex = index }
                                .then(
                                    if (colorIndex == index) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limitCents = Money.parseToCents(limit.ifBlank { "0" })
                    val balanceCents = if (balance.isBlank()) 0L else Money.parseToCents(balance)
                    val day = dueDay.toIntOrNull()
                    error = when {
                        name.isBlank() -> "Give this card a name"
                        last4.length != 4 -> "Enter the last 4 digits"
                        limitCents == null -> "Enter a credit limit greater than zero"
                        balanceCents == null -> "Enter a valid balance"
                        day == null || day !in 1..28 -> "Due day must be 1–28"
                        else -> null
                    }
                    if (error == null) {
                        onSave(name.trim(), last4, limitCents!!, balanceCents!!, day!!, colorIndex)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CardPaymentDialog(
    card: CreditCardEntity,
    currency: String,
    onDismiss: () -> Unit,
    onPay: (Long) -> Unit
) {
    var amount by remember(card.id) { mutableStateOf(plainAmount(card.balanceCents)) }
    var error by remember(card.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay ${card.name}") },
        text = {
            Column {
                Text(
                    text = "Current balance ${Money.format(card.balanceCents, currency)}. The payment is recorded as an expense and reduces this card's balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    label = "Payment amount",
                    value = amount,
                    onValueChange = { amount = it },
                    keyboardType = KeyboardType.Decimal,
                    leading = { Text(Money.symbol(currency)) }
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = Money.parseToCents(amount)
                    error = when {
                        cents == null -> "Enter an amount greater than zero"
                        cents > card.balanceCents -> "Payment cannot exceed the balance"
                        else -> null
                    }
                    if (error == null) onPay(cents!!)
                }
            ) { Text("Record payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun plainAmount(cents: Long): String =
    BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString()
