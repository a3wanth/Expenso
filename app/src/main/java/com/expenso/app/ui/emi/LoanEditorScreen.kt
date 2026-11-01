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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.Money
import com.expenso.app.ui.components.AppDatePickerDialog
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.DetailTopBar
import com.expenso.app.ui.components.FormField
import com.expenso.app.ui.components.PrimaryButton
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.theme.LocalAppColors

@Composable
fun LoanEditorScreen(
    vm: LoanEditorViewModel,
    onBack: () -> Unit
) {
    val form by vm.form.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(form.saved, form.deleted) {
        if (form.saved || form.deleted) onBack()
    }

    val principalCents = Money.parseToCents(form.principalText)
    val rate = form.rateText.toDoubleOrNull()
    val tenure = form.tenureText.toIntOrNull()
    val estimatedEmi = if (principalCents != null && rate != null && tenure != null && tenure > 0) {
        val explicit = if (form.emiText.isBlank()) 0L else Money.parseToCents(form.emiText)
        explicit ?: FinanceMath.calculateEmiCents(principalCents, rate, tenure)
    } else 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        DetailTopBar(
            title = if (form.isEditing) "Edit loan" else "New loan",
            subtitle = "EMI schedules are calculated automatically",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp)
        ) {
            item {
                FormField(
                    label = "Loan name",
                    value = form.name,
                    onValueChange = vm::setName,
                    placeholder = "e.g. Car loan"
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    label = "Lender (optional)",
                    value = form.lender,
                    onValueChange = vm::setLender,
                    placeholder = "e.g. Chase Auto"
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    label = "Principal amount",
                    value = form.principalText,
                    onValueChange = vm::setPrincipal,
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
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        label = "Annual rate %",
                        value = form.rateText,
                        onValueChange = vm::setRate,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    FormField(
                        label = "Tenure (months)",
                        value = form.tenureText,
                        onValueChange = vm::setTenure,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        label = "EMI override (optional)",
                        value = form.emiText,
                        onValueChange = vm::setEmi,
                        placeholder = "Auto",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    FormField(
                        label = "Due day (1–28)",
                        value = form.dueDay,
                        onValueChange = vm::setDueDay,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(14.dp))
                SurfaceCard(
                    padding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                    onClick = { showDatePicker = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "First EMI month",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.muted
                            )
                            Text(
                                text = DateX.formatDate(form.startDate),
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
                Spacer(Modifier.height(14.dp))
                SurfaceCard(padding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Estimated monthly EMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.muted
                            )
                            Text(
                                text = if (estimatedEmi > 0) Money.format(estimatedEmi) else "—",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (estimatedEmi > 0) "Calculated" else "Enter loan details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                FormField(
                    label = "Notes (optional)",
                    value = form.notes,
                    onValueChange = vm::setNotes,
                    singleLine = false,
                    placeholder = "Anything you want to remember"
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
                    text = if (form.saving) "Saving…" else if (form.isEditing) "Save loan" else "Create loan",
                    onClick = vm::save,
                    enabled = !form.saving
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "The repayment schedule uses reducing balance and your selected annual rate. You can adjust paid installments later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
                if (form.isEditing) {
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        text = "Delete loan",
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
            title = "Delete loan?",
            message = "This loan and its installments will be permanently removed.",
            onConfirm = {
                showDelete = false
                vm.delete()
            },
            onDismiss = { showDelete = false }
        )
    }
}
