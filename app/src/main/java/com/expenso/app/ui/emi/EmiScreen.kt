package com.expenso.app.ui.emi

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DateX
import com.expenso.app.data.DueStatus
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.LoanSummary
import com.expenso.app.data.Money
import com.expenso.app.data.local.BillEntity
import com.expenso.app.ui.components.CategoryAvatar
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.MiniProgressBar
import com.expenso.app.ui.components.PillAction
import com.expenso.app.ui.components.PrimaryButton
import com.expenso.app.ui.components.RoundIconButton
import com.expenso.app.ui.components.ScreenHeader
import com.expenso.app.ui.components.SectionHeader
import com.expenso.app.ui.components.SegmentedControl
import com.expenso.app.ui.components.StatusPill
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.theme.LocalAppColors

@Composable
fun EmiScreen(
    vm: EmiViewModel,
    onOpenCalendar: () -> Unit,
    onAddLoan: () -> Unit,
    onEditLoan: (Long) -> Unit,
    onOpenLoan: (Long) -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (Long) -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    val currency = state.preferences.currencyCode

    var menuLoanId by remember { mutableStateOf<Long?>(null) }
    var menuBillId by remember { mutableStateOf<Long?>(null) }
    var pendingLoanDelete by remember { mutableStateOf<LoanSummary?>(null) }
    var pendingBillDelete by remember { mutableStateOf<BillEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp)
    ) {
        item {
            ScreenHeader(
                title = "EMI & Bills",
                subtitle = "Loans, installments and recurring payments.",
                eyebrow = "Commitments"
            ) {
                RoundIconButton(
                    icon = Icons.Filled.CalendarMonth,
                    contentDescription = "Open due calendar",
                    onClick = onOpenCalendar
                )
            }
            Spacer(Modifier.height(16.dp))
            SegmentedControl(
                options = listOf(EmiTab.LOANS, EmiTab.BILLS),
                selected = state.tab,
                label = { if (it == EmiTab.LOANS) "Loans & EMI" else "Bills" },
                onSelect = vm::setTab
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            SurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly commitment",
                            style = MaterialTheme.typography.labelMedium,
                            color = app.muted
                        )
                        Text(
                            text = Money.format(state.monthlyEmiCents, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Metric("Outstanding", Money.format(state.outstandingCents, currency), Modifier.weight(1f))
                    Metric("Repaid", Money.format(state.totalRepaidCents, currency), Modifier.weight(1f))
                    Metric(
                        "Next due",
                        state.nextDueDate?.let { DateX.formatShortDate(it) } ?: "—",
                        Modifier.weight(1f)
                    )
                }
                if (state.overdueBills > 0) {
                    Spacer(Modifier.height(13.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = app.overdueSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = app.overdue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "${state.overdueBills} overdue ${if (state.overdueBills == 1) "bill" else "bills"} this month",
                                style = MaterialTheme.typography.labelMedium,
                                color = app.overdue
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        when (state.tab) {
            EmiTab.LOANS -> {
                item {
                    SectionHeader(
                        title = "Active loans",
                        actionText = "Add loan",
                        onAction = onAddLoan
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (state.activeLoans.isEmpty()) {
                    item {
                        SurfaceCard {
                            EmptyState(
                                icon = Icons.Filled.AccountBalance,
                                title = "No active loans",
                                message = "Add a loan to track EMIs, interest and your repayment schedule.",
                                actionText = "Add a loan",
                                onAction = onAddLoan
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                } else {
                    items(state.activeLoans, key = { it.loan.id }) { summary ->
                        LoanCard(
                            summary = summary,
                            currency = currency,
                            menuExpanded = menuLoanId == summary.loan.id,
                            onOpen = { onOpenLoan(summary.loan.id) },
                            onMenu = { menuLoanId = summary.loan.id },
                            onDismissMenu = { menuLoanId = null },
                            onEdit = {
                                menuLoanId = null
                                onEditLoan(summary.loan.id)
                            },
                            onDelete = {
                                menuLoanId = null
                                pendingLoanDelete = summary
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                if (state.completedLoans.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Completed loans")
                        Spacer(Modifier.height(8.dp))
                        SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 5.dp)) {
                            state.completedLoans.forEachIndexed { index, summary ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenLoan(summary.loan.id) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = app.income,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = summary.loan.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${summary.loan.tenureMonths} installments · ${Money.format(summary.paidCents, currency)} repaid",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = app.muted
                                        )
                                    }
                                    Text(
                                        text = "Completed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = app.income
                                    )
                                }
                                if (index != state.completedLoans.lastIndex) {
                                    HorizontalDivider(color = app.line)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }

                item {
                    PrimaryButton(
                        text = "Add a loan or EMI",
                        onClick = onAddLoan
                    )
                    Spacer(Modifier.height(22.dp))
                }
            }

            EmiTab.BILLS -> {
                item {
                    SectionHeader(
                        title = "${state.monthLabel} bills",
                        actionText = "Add bill",
                        onAction = onAddBill
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (state.bills.isEmpty()) {
                    item {
                        SurfaceCard {
                            EmptyState(
                                icon = Icons.Filled.ReceiptLong,
                                title = "No bills this month",
                                message = "Add recurring bills like rent, internet or subscriptions to see them here.",
                                actionText = "Add a bill",
                                onAction = onAddBill
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                } else {
                    items(state.bills, key = { it.bill.id }) { row ->
                        BillCard(
                            row = row,
                            currency = currency,
                            menuExpanded = menuBillId == row.bill.id,
                            onMenu = { menuBillId = row.bill.id },
                            onDismissMenu = { menuBillId = null },
                            onEdit = {
                                menuBillId = null
                                onEditBill(row.bill.id)
                            },
                            onDelete = {
                                menuBillId = null
                                pendingBillDelete = row.bill
                            },
                            onTogglePaid = { vm.setBillPaid(row.bill, row.status != DueStatus.PAID) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = "Add a recurring bill",
                        onClick = onAddBill
                    )
                    Spacer(Modifier.height(22.dp))
                }
            }
        }

        item {
            SurfaceCard(padding = PaddingValues(16.dp), onClick = onOpenCalendar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(app.warningSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = app.warning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Due-date calendar",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "See every payment on a single timeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = app.muted
                        )
                    }
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingLoanDelete?.let { summary ->
        ConfirmDialog(
            title = "Delete loan?",
            message = "“${summary.loan.name}” and its repayment history will be removed. Linked EMI expenses are deleted too.",
            onConfirm = {
                vm.deleteLoan(summary.loan.id)
                pendingLoanDelete = null
            },
            onDismiss = { pendingLoanDelete = null }
        )
    }

    pendingBillDelete?.let { bill ->
        ConfirmDialog(
            title = "Delete bill?",
            message = "“${bill.name}” will be removed. Expenses already recorded for it are kept.",
            onConfirm = {
                vm.deleteBill(bill.id)
                pendingBillDelete = null
            },
            onDismiss = { pendingBillDelete = null }
        )
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
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
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoanCard(
    summary: LoanSummary,
    currency: String,
    menuExpanded: Boolean,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val app = LocalAppColors.current
    SurfaceCard(onClick = onOpen, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(app.indigoSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.loan.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (summary.loan.lender.isBlank()) {
                        "${summary.loan.tenureMonths} month plan"
                    } else summary.loan.lender,
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(summary.loan.emiCents, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "EMI / month",
                    style = MaterialTheme.typography.labelSmall,
                    color = app.muted
                )
            }
            Box {
                IconButton(onClick = onMenu) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Loan options",
                        tint = app.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                    DropdownMenuItem(
                        text = { Text("Edit loan") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = onEdit
                    )
                    DropdownMenuItem(
                        text = { Text("Delete loan") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = app.expense
                            )
                        },
                        onClick = onDelete
                    )
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        MiniProgressBar(progress = summary.progress, color = MaterialTheme.colorScheme.primary, height = 8.dp)
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${summary.paidCount} of ${summary.loan.tenureMonths} paid · ${(summary.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = app.muted
            )
            val next = summary.nextInstallment
            Surface(
                shape = RoundedCornerShape(50),
                color = app.indigoSoft
            ) {
                Text(
                    text = next?.let {
                        "${FinanceMath.dueLabel(
                            com.expenso.app.data.DueItem(
                                key = "",
                                kind = com.expenso.app.data.DueKind.LOAN,
                                title = "",
                                subtitle = "",
                                amountCents = it.amountCents,
                                dueDate = it.dueDate,
                                status = if (it.dueDate.isBefore(java.time.LocalDate.now())) DueStatus.OVERDUE else DueStatus.UPCOMING,
                                referenceId = summary.loan.id
                            )
                        )} · ${Money.format(it.amountCents, currency)}"
                    } ?: "Fully repaid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun BillCard(
    row: BillRow,
    currency: String,
    menuExpanded: Boolean,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePaid: () -> Unit
) {
    val app = LocalAppColors.current
    val paid = row.status == DueStatus.PAID
    SurfaceCard(padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(name = row.bill.category, colorHex = "#EF4444", size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.bill.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${row.bill.category} · due ${DateX.formatShortDate(row.dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(row.bill.amountCents, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    status = row.status,
                    label = if (paid) "Paid" else FinanceMath.dueLabel(
                        com.expenso.app.data.DueItem(
                            key = "",
                            kind = com.expenso.app.data.DueKind.BILL,
                            title = "",
                            subtitle = "",
                            amountCents = row.bill.amountCents,
                            dueDate = row.dueDate,
                            status = row.status,
                            referenceId = row.bill.id
                        )
                    )
                )
            }
            Box {
                IconButton(onClick = onMenu) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Bill options",
                        tint = app.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                    DropdownMenuItem(
                        text = { Text("Edit bill") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = onEdit
                    )
                    DropdownMenuItem(
                        text = { Text("Delete bill") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = app.expense
                            )
                        },
                        onClick = onDelete
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (row.bill.autoPay) "Auto-pay enabled" else "Manual payment",
                style = MaterialTheme.typography.labelSmall,
                color = app.muted
            )
            PillAction(
                text = if (paid) "Undo payment" else "Mark paid",
                onClick = onTogglePaid,
                color = if (paid) app.muted else app.income,
                background = if (paid) app.surfaceRaised else app.incomeSoft
            )
        }
    }
}
