package com.expenso.app.ui.expenses

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DateX
import com.expenso.app.data.Money
import com.expenso.app.data.local.TransactionEntity
import com.expenso.app.ui.components.AppDatePickerDialog
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.RoundIconButton
import com.expenso.app.ui.components.ScreenHeader
import com.expenso.app.ui.components.SegmentedControl
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.components.TransactionRow
import com.expenso.app.ui.theme.LocalAppColors
import java.time.LocalDate

@Composable
fun ExpensesScreen(
    vm: ExpensesViewModel,
    onAddExpense: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onOpenCalendar: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    val currency = state.preferences.currencyCode
    val filters = state.filters

    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var customFrom by remember { mutableStateOf(filters.customFrom) }

    val grouped = state.transactions
        .groupBy { DateX.date(it.dateEpochDay) }
        .entries
        .sortedByDescending { it.key }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 108.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Expenses",
                    subtitle = "Every transaction, stored privately on this device.",
                    eyebrow = "Activity"
                ) {
                    RoundIconButton(
                        icon = Icons.Filled.CalendarMonth,
                        contentDescription = "Open due calendar",
                        onClick = onOpenCalendar
                    )
                    RoundIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Add expense",
                        onClick = onAddExpense,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            item {
                SurfaceCard(padding = PaddingValues(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Money out",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.muted
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = Money.format(state.expenseCents, currency),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(42.dp)
                                .background(app.line)
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Money in",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.muted
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = Money.format(state.incomeCents, currency),
                                style = MaterialTheme.typography.titleLarge,
                                color = app.income
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${state.transactions.size} of ${state.totalCount} transactions shown",
                        style = MaterialTheme.typography.bodySmall,
                        color = app.muted
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search title, category or notes") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                SegmentedControl(
                    options = listOf(
                        TransactionTab.ALL,
                        TransactionTab.EXPENSES,
                        TransactionTab.INCOME,
                        TransactionTab.CREDIT
                    ),
                    selected = filters.tab,
                    label = {
                        when (it) {
                            TransactionTab.ALL -> "All"
                            TransactionTab.EXPENSES -> "Expenses"
                            TransactionTab.INCOME -> "Income"
                            TransactionTab.CREDIT -> "Credit"
                        }
                    },
                    onSelect = vm::setTab
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.category == null,
                            onClick = { vm.setCategory(null) },
                            label = { Text("All categories") }
                        )
                    }
                    items(state.categories, key = { it.name }) { category ->
                        FilterChip(
                            selected = filters.category == category.name,
                            onClick = {
                                vm.setCategory(if (filters.category == category.name) null else category.name)
                            },
                            label = { Text(category.name) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dateOptions = listOf(
                        DateFilter.ALL to "Any time",
                        DateFilter.THIS_MONTH to "This month",
                        DateFilter.LAST_30 to "Last 30 days",
                        DateFilter.THIS_YEAR to "This year"
                    )
                    items(dateOptions) { (option, label) ->
                        FilterChip(
                            selected = filters.dateFilter == option,
                            onClick = { vm.setDateFilter(option) },
                            label = { Text(label) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.dateFilter == DateFilter.CUSTOM,
                            onClick = {
                                customFrom = filters.customFrom
                                showFromPicker = true
                            },
                            label = {
                                Text(
                                    if (filters.dateFilter == DateFilter.CUSTOM) {
                                        "${DateX.formatShortDate(filters.customFrom)} – ${DateX.formatShortDate(filters.customTo)}"
                                    } else "Custom range"
                                )
                            }
                        )
                    }
                }
                if (state.hasActiveFilters) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = vm::clearFilters) {
                            Text("Clear ${state.activeFilterCount} filters")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (grouped.isEmpty()) {
                item {
                    SurfaceCard {
                        EmptyState(
                            icon = Icons.Filled.ReceiptLong,
                            title = if (state.hasActiveFilters) "No matching transactions" else "No transactions yet",
                            message = if (state.hasActiveFilters) {
                                "Try a different search, category or date range."
                            } else {
                                "Add your first expense or income to start tracking your money."
                            },
                            actionText = if (state.hasActiveFilters) "Clear filters" else "Add expense",
                            onAction = if (state.hasActiveFilters) vm::clearFilters else onAddExpense
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                grouped.forEach { (date, dayTransactions) ->
                    item(key = "header-${date.toEpochDay()}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DateX.formatRelative(date),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = Money.format(
                                    dayTransactions.filter { it.type == com.expenso.app.data.TxType.EXPENSE }
                                        .sumOf { it.amountCents },
                                    currency
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = app.muted
                            )
                        }
                    }
                    items(dayTransactions, key = { it.id }) { transaction ->
                        SurfaceCard(
                            padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            TransactionRow(
                                transaction = transaction,
                                categoryColor = state.categoryColors[transaction.category] ?: "#625BFF",
                                currencyCode = currency,
                                hidden = state.preferences.hideBalance,
                                onClick = { onEditTransaction(transaction.id) },
                                trailing = {
                                    IconButton(onClick = { pendingDelete = transaction }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete ${transaction.title}",
                                            tint = app.muted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Showing real data stored in this app. Swipe-free editing keeps every change local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = app.muted.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Surface(
            onClick = onAddExpense,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    pendingDelete?.let { transaction ->
        ConfirmDialog(
            title = "Delete transaction?",
            message = "“${transaction.title}” will be permanently removed. Linked bills and EMIs are not affected.",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteTransaction(transaction.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (showFromPicker) {
        AppDatePickerDialog(
            initialDate = customFrom,
            onDismiss = { showFromPicker = false },
            onDateSelected = { date ->
                customFrom = date
                showToPicker = true
            }
        )
    }

    if (showToPicker) {
        AppDatePickerDialog(
            initialDate = LocalDate.now(),
            onDismiss = { showToPicker = false },
            onDateSelected = { date -> vm.setCustomRange(customFrom, date) }
        )
    }
}
