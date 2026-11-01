package com.expenso.app.ui.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DateX
import com.expenso.app.data.DueItem
import com.expenso.app.data.DueKind
import com.expenso.app.data.DueStatus
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.Money
import com.expenso.app.ui.components.DetailTopBar
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.MonthStepper
import com.expenso.app.ui.components.PillAction
import com.expenso.app.ui.components.SectionHeader
import com.expenso.app.ui.components.StatusPill
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.theme.LocalAppColors
import java.time.LocalDate

@Composable
fun CalendarScreen(
    vm: CalendarViewModel,
    onBack: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    val currency = state.preferences.currencyCode
    val month = state.month

    val leading = (month.atDay(1).dayOfWeek.value + 6) % 7
    val cells: List<LocalDate?> = buildList {
        repeat(leading) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    val itemsByDay = state.items.groupBy { it.dueDate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        DetailTopBar(
            title = "Due calendar",
            subtitle = "Every bill and EMI in one timeline",
            onBack = onBack,
            actions = {
                PillAction(
                    text = "Today",
                    onClick = vm::goToToday,
                    color = MaterialTheme.colorScheme.primary,
                    background = app.indigoSoft
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp)
        ) {
            item {
                MonthStepper(
                    label = state.monthLabel,
                    onPrevious = vm::previousMonth,
                    onNext = vm::nextMonth
                )
                Spacer(Modifier.height(14.dp))
            }

            item {
                SurfaceCard(padding = PaddingValues(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = app.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    cells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                DayCell(
                                    date = date,
                                    selected = date != null && date == state.selectedDate,
                                    today = date != null && date == LocalDate.now(),
                                    items = date?.let { itemsByDay[it].orEmpty() }.orEmpty(),
                                    onClick = { date?.let(vm::selectDate) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LegendDot(app.income, "Paid")
                        Spacer(Modifier.width(14.dp))
                        LegendDot(app.warning, "Upcoming")
                        Spacer(Modifier.width(14.dp))
                        LegendDot(app.overdue, "Overdue")
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            item {
                SurfaceCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CalendarMetric(
                            label = "Paid",
                            value = state.paidCount.toString(),
                            color = app.income,
                            modifier = Modifier.weight(1f)
                        )
                        CalendarMetric(
                            label = "Upcoming",
                            value = Money.format(state.upcomingAmountCents, currency),
                            color = app.warning,
                            modifier = Modifier.weight(1f)
                        )
                        CalendarMetric(
                            label = "Overdue",
                            value = if (state.overdueCount == 0) "None" else Money.format(state.overdueAmountCents, currency),
                            color = app.overdue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                SectionHeader(
                    title = DateX.formatRelative(state.selectedDate),
                    actionText = if (state.selectedItems.isEmpty()) null else "${state.selectedItems.size} items"
                )
                Spacer(Modifier.height(10.dp))
            }

            if (state.selectedItems.isEmpty()) {
                item {
                    SurfaceCard {
                        EmptyState(
                            icon = Icons.Filled.CalendarMonth,
                            title = "Nothing due this day",
                            message = "Pick another date to see scheduled bills and EMIs."
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            } else {
                items(state.selectedItems, key = { it.key }) { item ->
                    DueItemCard(
                        item = item,
                        currency = currency,
                        onTogglePaid = { paid -> vm.setPaid(item, paid) }
                    )
                    Spacer(Modifier.height(9.dp))
                }
                item { Spacer(Modifier.height(10.dp)) }
            }

            item {
                SurfaceCard(padding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Today,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(11.dp))
                        Text(
                            text = "Payments marked paid here also update your expense history and loan progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = app.muted
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    selected: Boolean,
    today: Boolean,
    items: List<DueItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalAppColors.current
    val hasOverdue = items.any { it.status == DueStatus.OVERDUE }
    val hasUpcoming = items.any { it.status == DueStatus.UPCOMING }
    val hasPaid = items.any { it.status == DueStatus.PAID }
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        date == null -> Color.Transparent
        today -> app.indigoSoft
        else -> Color.Transparent
    }
    val textColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        date == null -> Color.Transparent
        today -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .aspectRatio(0.95f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .then(
                if (today && !selected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date?.dayOfMonth?.toString() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (selected || today) FontWeight.SemiBold else FontWeight.Normal
            )
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasOverdue) Dot(if (selected) Color.White else app.overdue)
                    if (hasUpcoming) Dot(if (selected) Color.White else app.warning)
                    if (hasPaid && !hasOverdue && !hasUpcoming) Dot(if (selected) Color.White else app.income)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppColors.current.muted
        )
    }
}

@Composable
private fun CalendarMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
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
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DueItemCard(
    item: DueItem,
    currency: String,
    onTogglePaid: (Boolean) -> Unit
) {
    val app = LocalAppColors.current
    val paid = item.status == DueStatus.PAID
    val (icon, tint) = when (item.kind) {
        DueKind.LOAN -> Icons.Filled.Payments to MaterialTheme.colorScheme.primary
        DueKind.CARD -> Icons.Filled.CreditCard to Color(0xFF0EA5E9)
        else -> Icons.Filled.ReceiptLong to app.warning
    }
    SurfaceCard(padding = PaddingValues(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (app.isDark) 0.2f else 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
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
                    text = "${item.subtitle} · ${FinanceMath.dueLabel(item)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.status == DueStatus.OVERDUE) app.overdue else app.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(item.amountCents, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                StatusPill(status = item.status)
            }
        }
        Spacer(Modifier.height(11.dp))
        HorizontalDivider(color = app.line)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (item.kind) {
                    DueKind.LOAN -> "Loan installment"
                    DueKind.CARD -> "Card balance"
                    else -> "Recurring bill"
                },
                style = MaterialTheme.typography.labelSmall,
                color = app.muted
            )
            if (item.kind == DueKind.CARD) {
                if (!paid) {
                    PillAction(
                        text = "Pay balance",
                        onClick = { onTogglePaid(true) },
                        color = app.income,
                        background = app.incomeSoft
                    )
                } else {
                    Text(
                        text = "Paid this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = app.income
                    )
                }
            } else {
                PillAction(
                    text = if (paid) "Undo" else "Mark paid",
                    onClick = { onTogglePaid(!paid) },
                    color = if (paid) app.muted else app.income,
                    background = if (paid) app.surfaceRaised else app.incomeSoft
                )
            }
        }
    }
}
