package com.expenso.app.ui.emi

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.DateX
import com.expenso.app.data.Installment
import com.expenso.app.data.Money
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.DetailTopBar
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.KeyValueRow
import com.expenso.app.ui.components.MiniProgressBar
import com.expenso.app.ui.components.PillAction
import com.expenso.app.ui.components.SectionHeader
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.theme.CardGradientIndigoEnd
import com.expenso.app.ui.theme.CardGradientIndigoStart
import com.expenso.app.ui.theme.LocalAppColors
import java.time.LocalDate

@Composable
fun LoanDetailScreen(
    vm: LoanDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    val currency = state.preferences.currencyCode
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    val summary = state.summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        DetailTopBar(
            title = summary?.loan?.name ?: "Loan",
            subtitle = summary?.loan?.lender?.takeIf { it.isNotBlank() } ?: "Loan details",
            onBack = onBack,
            actions = {
                if (summary != null) {
                    IconButton(onClick = { onEdit(summary.loan.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit loan",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        )

        if (summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.WarningAmber,
                    title = "Loan not found",
                    message = "It may have been deleted. Go back to see your other loans.",
                    actionText = "Go back",
                    onAction = onBack
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(CardGradientIndigoStart, CardGradientIndigoEnd)))
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = size.width * 0.4f,
                                    center = Offset(size.width * 1.02f, 0f)
                                )
                            }
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Outstanding principal",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = Money.format(summary.outstandingPrincipalCents, currency),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(9.dp))
                            Text(
                                text = "${summary.paidCount} of ${summary.loan.tenureMonths} installments paid",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.22f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(summary.progress.coerceIn(0f, 1f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Monthly EMI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                    Text(
                                        text = Money.format(summary.loan.emiCents, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Interest rate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                    Text(
                                        text = "${summary.loan.annualRatePercent}% p.a.",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Next due",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                    Text(
                                        text = summary.nextInstallment?.let { DateX.formatShortDate(it.dueDate) } ?: "—",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                item {
                    SurfaceCard {
                        KeyValueRow("Principal", Money.format(summary.loan.principalCents, currency))
                        Spacer(Modifier.height(10.dp))
                        KeyValueRow("Total interest", Money.format(summary.totalInterestCents, currency))
                        Spacer(Modifier.height(10.dp))
                        KeyValueRow("Total payable", Money.format(summary.totalPayableCents, currency))
                        Spacer(Modifier.height(10.dp))
                        KeyValueRow("Already repaid", Money.format(summary.paidCents, currency), valueColor = app.income)
                        Spacer(Modifier.height(10.dp))
                        KeyValueRow("Tenure", "${summary.loan.tenureMonths} months")
                        if (summary.loan.notes.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = app.line)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = summary.loan.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = app.muted
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    SectionHeader(title = "Repayment schedule")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Each installment splits into principal and interest. Marking one paid records a linked expense.",
                        style = MaterialTheme.typography.bodySmall,
                        color = app.muted
                    )
                    Spacer(Modifier.height(10.dp))
                }

                items(summary.installments, key = { it.number }) { installment ->
                    InstallmentRow(
                        installment = installment,
                        currency = currency,
                        onTogglePaid = { vm.setInstallmentPaid(installment.number, !installment.isPaid) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        onClick = { showDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = app.expenseSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = app.expense,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Delete this loan",
                                style = MaterialTheme.typography.labelLarge,
                                color = app.expense
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete loan?",
            message = "The loan and its repayment history will be permanently removed, including linked EMI expenses.",
            onConfirm = {
                showDelete = false
                vm.deleteLoan()
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun InstallmentRow(
    installment: Installment,
    currency: String,
    onTogglePaid: () -> Unit
) {
    val app = LocalAppColors.current
    val overdue = !installment.isPaid && installment.dueDate.isBefore(LocalDate.now())
    SurfaceCard(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            installment.isPaid -> app.incomeSoft
                            overdue -> app.overdueSoft
                            else -> app.surfaceRaised
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (installment.isPaid) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Paid",
                        tint = app.income,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = installment.number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdue) app.overdue else app.muted
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Installment ${installment.number}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${DateX.formatDate(installment.dueDate)} · principal ${Money.format(installment.principalCents, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) app.overdue else app.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(installment.amountCents, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                PillAction(
                    text = if (installment.isPaid) "Undo" else "Mark paid",
                    onClick = onTogglePaid,
                    color = if (installment.isPaid) app.muted else MaterialTheme.colorScheme.primary,
                    background = if (installment.isPaid) app.surfaceRaised else app.indigoSoft
                )
            }
        }
    }
}
