package com.expenso.app.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.AccountType
import com.expenso.app.data.DateX
import com.expenso.app.data.Money
import com.expenso.app.data.TxType
import com.expenso.app.ui.components.AppDatePickerDialog
import com.expenso.app.ui.components.DetailTopBar
import com.expenso.app.ui.components.FormField
import com.expenso.app.ui.components.PrimaryButton
import com.expenso.app.ui.components.SegmentedControl
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.theme.LocalAppColors

@Composable
fun TransactionEditorScreen(
    vm: TransactionEditorViewModel,
    onBack: () -> Unit
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val form = ui.form
    val app = LocalAppColors.current
    val currency = ui.preferences.currencyCode

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(form.saved) {
        if (form.saved) onBack()
    }

    val typeCategories = ui.categories
        .filter { it.type == form.type }
        .map { it.name }
        .ifEmpty { ui.categories.map { it.name } }
    val categoryNames = (typeCategories + form.category).distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        DetailTopBar(
            title = if (form.isEditing) "Edit transaction" else if (form.type == TxType.INCOME) "Add income" else "Add expense",
            subtitle = if (form.isEditing) "Changes update your balances instantly" else "Saved only on this device",
            onBack = onBack
        )

        if (form.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp)
            ) {
                item {
                    SegmentedControl(
                        options = listOf(TxType.EXPENSE, TxType.INCOME),
                        selected = form.type,
                        label = { if (it == TxType.INCOME) "Income" else "Expense" },
                        onSelect = vm::setType
                    )
                    Spacer(Modifier.height(18.dp))
                }

                item {
                    FormField(
                        label = "Amount",
                        value = form.amountText,
                        onValueChange = vm::setAmount,
                        placeholder = "0.00",
                        keyboardType = KeyboardType.Decimal,
                        leading = {
                            Text(
                                text = Money.symbol(currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supporting = "Enter an amount greater than zero"
                    )
                    Spacer(Modifier.height(14.dp))
                    FormField(
                        label = "Title",
                        value = form.title,
                        onValueChange = vm::setTitle,
                        placeholder = if (form.type == TxType.INCOME) "e.g. Monthly salary" else "e.g. Grocery run"
                    )
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryNames, key = { it }) { name ->
                            FilterChip(
                                selected = form.category == name,
                                onClick = { vm.setCategory(name) },
                                label = { Text(name) },
                                leadingIcon = if (form.category == name) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    SurfaceCard(
                        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        onClick = { showDatePicker = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = app.muted
                                )
                                Text(
                                    text = DateX.formatDate(form.date),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    SegmentedControl(
                        options = listOf(AccountType.BANK, AccountType.CASH, AccountType.CREDIT),
                        selected = form.account,
                        label = {
                            when (it) {
                                AccountType.CREDIT -> "Credit card"
                                AccountType.CASH -> "Cash"
                                else -> "Bank"
                            }
                        },
                        onSelect = vm::setAccount
                    )
                    if (form.account == AccountType.CREDIT) {
                        Spacer(Modifier.height(12.dp))
                        if (ui.cards.isEmpty()) {
                            Text(
                                text = "No credit cards yet. Add one from Analytics → Credit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = app.muted
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ui.cards, key = { it.id }) { card ->
                                    FilterChip(
                                        selected = form.creditCardId == card.id,
                                        onClick = { vm.setCreditCard(card.id) },
                                        label = { Text("${card.name} ••${card.last4}") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Credit purchases are tracked separately from your bank balance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = app.muted
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    FormField(
                        label = "Notes (optional)",
                        value = form.notes,
                        onValueChange = vm::setNotes,
                        placeholder = "Add a note for your future self",
                        singleLine = false
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = app.line)
                    Spacer(Modifier.height(14.dp))
                }

                if (form.error != null) {
                    item {
                        SurfaceCard(padding = PaddingValues(14.dp)) {
                            Text(
                                text = form.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = app.expense
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }

                item {
                    PrimaryButton(
                        text = if (form.saving) "Saving…" else if (form.isEditing) "Save changes" else "Add transaction",
                        onClick = vm::save,
                        enabled = !form.saving
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (form.isEditing) {
                            "Editing this transaction recalculates balances, budgets and analytics."
                        } else {
                            "This transaction will be included in your balance, budgets and analytics."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = app.muted
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = form.date,
            onDismiss = { showDatePicker = false },
            onDateSelected = vm::setDate
        )
    }
}
