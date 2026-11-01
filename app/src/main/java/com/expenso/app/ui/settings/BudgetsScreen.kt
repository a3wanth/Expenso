package com.expenso.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.BudgetProgress
import com.expenso.app.data.Money
import com.expenso.app.ui.components.CategoryAvatar
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.DetailTopBar
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.MiniProgressBar
import com.expenso.app.ui.components.PillAction
import com.expenso.app.ui.components.PrimaryButton
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.theme.LocalAppColors

@Composable
fun BudgetsScreen(
    vm: BudgetsViewModel,
    onBack: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    val context = LocalContext.current
    val currency = state.preferences.currencyCode

    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BudgetProgress?>(null) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        DetailTopBar(
            title = "Budgets",
            subtitle = "Monthly limits by category",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add budget",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp)
        ) {
            item {
                SurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "This month",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.muted
                            )
                            Text(
                                text = Money.format(state.totalSpentCents, currency),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of ${Money.format(state.totalLimitCents, currency)} budgeted",
                                style = MaterialTheme.typography.bodySmall,
                                color = app.muted
                            )
                        }
                        Text(
                            text = "${(state.totalProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = when {
                                state.overBudgetCount > 0 -> app.expense
                                state.totalProgress >= 0.85f -> app.warning
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Spacer(Modifier.height(13.dp))
                    MiniProgressBar(
                        progress = state.totalProgress,
                        color = if (state.overBudgetCount > 0) app.expense else MaterialTheme.colorScheme.primary,
                        height = 9.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${state.budgets.size} categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = app.muted,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (state.overBudgetCount > 0) {
                                "${state.overBudgetCount} over budget"
                            } else {
                                "${Money.formatAbs(state.totalLimitCents - state.totalSpentCents, currency)} left"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.overBudgetCount > 0) app.expense else app.income
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            if (state.budgets.isEmpty()) {
                item {
                    SurfaceCard {
                        EmptyState(
                            icon = Icons.Filled.DonutLarge,
                            title = "No budgets yet",
                            message = "Set a monthly limit for a category to track spending against it.",
                            actionText = "Create a budget",
                            onAction = { showAdd = true }
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            } else {
                items(state.budgets, key = { it.budget.id }) { progress ->
                    BudgetRow(
                        progress = progress,
                        currency = currency,
                        onDelete = { pendingDelete = progress }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                if (state.availableCategories.isNotEmpty()) {
                    PrimaryButton(text = "Add a category budget", onClick = { showAdd = true })
                } else {
                    Text(
                        text = "Every expense category already has a budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = app.muted,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showAdd) {
        AddBudgetDialog(
            monthLabel = state.monthLabel,
            categories = state.availableCategories,
            currency = currency,
            onDismiss = { showAdd = false },
            onSave = { category, amount ->
                vm.saveBudget(category, amount)
                showAdd = false
            }
        )
    }

    pendingDelete?.let { progress ->
        ConfirmDialog(
            title = "Remove budget?",
            message = "The ${progress.budget.category} budget will be deleted. Transactions are not affected.",
            onConfirm = {
                vm.deleteBudget(progress.budget.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun BudgetRow(
    progress: BudgetProgress,
    currency: String,
    onDelete: () -> Unit
) {
    val app = LocalAppColors.current
    val ratio = if (progress.budget.amountCents <= 0) 0f else progress.spentCents.toFloat() / progress.budget.amountCents
    val over = progress.spentCents > progress.budget.amountCents
    val color = when {
        over -> app.expense
        ratio >= 0.85f -> app.warning
        else -> MaterialTheme.colorScheme.primary
    }
    SurfaceCard(padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(name = progress.budget.category, colorHex = "#625BFF", size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.budget.category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${Money.format(progress.spentCents, currency)} of ${Money.format(progress.budget.amountCents, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = color
                )
                Text(
                    text = if (over) {
                        "Over by ${Money.formatAbs(progress.spentCents - progress.budget.amountCents, currency)}"
                    } else {
                        "${Money.format(progress.remainingCents, currency)} left"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (over) app.expense else app.muted
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${progress.budget.category} budget",
                    tint = app.muted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        MiniProgressBar(progress = progress.progress, color = color, height = 8.dp)
    }
}

@Composable
private fun AddBudgetDialog(
    monthLabel: String,
    categories: List<String>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selected by remember(categories) { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New budget") },
        text = {
            Column {
                Text(
                    text = "Set a monthly spending limit for $monthLabel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it }) { category ->
                        FilterChip(
                            selected = selected == category,
                            onClick = { selected = category },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = { Text("Monthly limit") },
                    leadingIcon = { Text(Money.symbol(currency), modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when {
                        selected.isBlank() -> "Choose a category"
                        Money.parseToCents(amount) == null -> "Enter a limit greater than zero"
                        else -> null
                    }
                    if (error == null) onSave(selected, amount)
                }
            ) { Text("Save budget") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
