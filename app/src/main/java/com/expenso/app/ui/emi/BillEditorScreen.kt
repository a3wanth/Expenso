package com.expenso.app.ui.emi

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DateX
import com.expenso.app.data.Money
import com.expenso.app.ui.components.AppDatePickerDialog
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.DetailTopBar
import com.expenso.app.ui.components.FormField
import com.expenso.app.ui.components.PrimaryButton
import com.expenso.app.ui.components.SegmentedControl
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.theme.LocalAppColors

private val billCategories = listOf(
    "Bills", "Rent", "Utilities", "Entertainment", "Health",
    "Education", "Subscriptions", "Transport", "Other"
)

@Composable
fun BillEditorScreen(
    vm: BillEditorViewModel,
    onBack: () -> Unit
) {
    val form by vm.form.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(form.saved, form.deleted) {
        if (form.saved || form.deleted) onBack()
    }

    val categories = (billCategories + form.category).distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        DetailTopBar(
            title = if (form.isEditing) "Edit bill" else "New bill",
            subtitle = "Recurring payments and reminders",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp)
        ) {
            item {
                FormField(
                    label = "Bill name",
                    value = form.name,
                    onValueChange = vm::setName,
                    placeholder = "e.g. Internet"
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    label = "Amount",
                    value = form.amountText,
                    onValueChange = vm::setAmount,
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    leading = {
                        Text(
                            text = Money.symbol(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it }) { category ->
                        FilterChip(
                            selected = form.category == category,
                            onClick = { vm.setCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                SegmentedControl(
                    options = listOf("MONTHLY", "ONE_TIME"),
                    selected = form.frequency,
                    label = { if (it == "ONE_TIME") "One-time" else "Monthly" },
                    onSelect = vm::setFrequency
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        label = "Due day (1–28)",
                        value = form.dueDay,
                        onValueChange = vm::setDueDay,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start date",
                            style = MaterialTheme.typography.labelSmall,
                            color = app.muted
                        )
                        Spacer(Modifier.height(4.dp))
                        SurfaceCard(
                            padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                            onClick = { showDatePicker = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = DateX.formatShortDate(form.startDate),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-pay",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mark this bill as paid automatically each month.",
                                style = MaterialTheme.typography.bodySmall,
                                color = app.muted
                            )
                        }
                        Switch(
                            checked = form.autoPay,
                            onCheckedChange = vm::setAutoPay
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                FormField(
                    label = "Notes (optional)",
                    value = form.notes,
                    onValueChange = vm::setNotes,
                    singleLine = false,
                    placeholder = "Account number, billing details…"
                )
                Spacer(Modifier.height(16.dp))
            }

            if (form.error != null) {
                item {
                    SurfaceCard(padding = PaddingValues(14.dp)) {
                        Text(
                            text = form.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = app.expense
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            item {
                PrimaryButton(
                    text = if (form.saving) "Saving…" else if (form.isEditing) "Save bill" else "Create bill",
                    onClick = vm::save,
                    enabled = !form.saving
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Marking a bill paid creates a linked expense in your transactions. Undo removes that expense.",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
                if (form.isEditing) {
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        text = "Delete bill",
                        onClick = { showDelete = true }
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = form.startDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = vm::setStartDate
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete bill?",
            message = "This bill will be permanently removed. Existing expenses stay in your history.",
            onConfirm = {
                showDelete = false
                vm.delete()
            },
            onDismiss = { showDelete = false }
        )
    }
}
