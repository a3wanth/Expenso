package com.expenso.app.data

import com.expenso.app.data.local.BillEntity
import com.expenso.app.data.local.BudgetEntity
import com.expenso.app.data.local.CategoryEntity
import com.expenso.app.data.local.CreditCardEntity
import com.expenso.app.data.local.LoanEntity
import com.expenso.app.data.local.LoanPaymentEntity
import com.expenso.app.data.local.TransactionEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

object TxType {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
}

object AccountType {
    const val BANK = "BANK"
    const val CASH = "CASH"
    const val CREDIT = "CREDIT"
}

object DueStatus {
    const val PAID = "PAID"
    const val UPCOMING = "UPCOMING"
    const val OVERDUE = "OVERDUE"
}

object DueKind {
    const val BILL = "BILL"
    const val LOAN = "LOAN"
    const val CARD = "CARD"
}

interface FinanceSource {
    val transactions: List<TransactionEntity>
    val budgets: List<BudgetEntity>
    val loans: List<LoanEntity>
    val loanPayments: List<LoanPaymentEntity>
    val bills: List<BillEntity>
    val cards: List<CreditCardEntity>
    val categories: List<CategoryEntity>
}

data class FinanceSnapshot(
    override val transactions: List<TransactionEntity> = emptyList(),
    override val budgets: List<BudgetEntity> = emptyList(),
    override val loans: List<LoanEntity> = emptyList(),
    override val loanPayments: List<LoanPaymentEntity> = emptyList(),
    override val bills: List<BillEntity> = emptyList(),
    override val cards: List<CreditCardEntity> = emptyList(),
    override val categories: List<CategoryEntity> = emptyList()
) : FinanceSource

data class Installment(
    val number: Int,
    val dueDate: LocalDate,
    val amountCents: Long,
    val principalCents: Long,
    val interestCents: Long,
    val paidDate: LocalDate?
) {
    val isPaid: Boolean get() = paidDate != null
}

data class LoanSummary(
    val loan: LoanEntity,
    val installments: List<Installment>,
    val paidCount: Int,
    val paidCents: Long,
    val outstandingPrincipalCents: Long,
    val totalInterestCents: Long,
    val totalPayableCents: Long,
    val nextInstallment: Installment?
) {
    val progress: Float
        get() = if (loan.tenureMonths == 0) 0f
        else (paidCount.toFloat() / loan.tenureMonths).coerceIn(0f, 1f)

    val isComplete: Boolean get() = paidCount >= loan.tenureMonths
}

data class BudgetProgress(
    val budget: BudgetEntity,
    val spentCents: Long
) {
    val progress: Float
        get() = if (budget.amountCents <= 0) 0f
        else (spentCents.toFloat() / budget.amountCents).coerceIn(0f, 1f)

    val remainingCents: Long get() = budget.amountCents - spentCents
}

data class DueItem(
    val key: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val amountCents: Long,
    val dueDate: LocalDate,
    val status: String,
    val referenceId: Long,
    val installmentNumber: Int? = null,
    val monthKey: String? = null
)

data class CategoryTotal(
    val category: String,
    val amountCents: Long,
    val colorHex: String
)

data class MonthTotal(
    val month: YearMonth,
    val incomeCents: Long,
    val expenseCents: Long
)

object DateX {
    val epochDayNow: Long get() = LocalDate.now().toEpochDay()

    fun today(): LocalDate = LocalDate.now()

    fun date(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun monthKey(date: LocalDate): String =
        "%04d-%02d".format(Locale.US, date.year, date.monthValue)

    fun monthKey(month: YearMonth): String =
        "%04d-%02d".format(Locale.US, month.year, month.monthValue)

    fun parseMonthKey(key: String): YearMonth? = runCatching { YearMonth.parse(key) }.getOrNull()

    fun monthLabel(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year

    fun shortMonth(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))

    fun formatShortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

    fun formatRelative(date: LocalDate): String {
        val today = today()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        }
    }

    fun daysBetween(from: LocalDate, to: LocalDate): Long =
        to.toEpochDay() - from.toEpochDay()

    fun dueDateForMonth(month: YearMonth, dueDay: Int): LocalDate =
        month.atDay(dueDay.coerceIn(1, month.lengthOfMonth()))

    fun greeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}

object Money {
    fun format(cents: Long, currencyCode: String = "USD", showSign: Boolean = false): String {
        val code = runCatching { currencyCode }.getOrDefault("USD")
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        runCatching { formatter.currency = Currency.getInstance(code) }
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        val value = BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val formatted = formatter.format(value)
        return if (showSign && cents > 0) "+$formatted" else formatted
    }

    fun formatAbs(cents: Long, currencyCode: String = "USD", showSign: Boolean = false): String =
        format(abs(cents), currencyCode, showSign)

    fun compact(cents: Long, currencyCode: String = "USD"): String {
        val value = abs(cents) / 100.0
        val symbol = symbol(currencyCode)
        val sign = if (cents < 0) "-" else ""
        return when {
            value >= 1_000_000 -> "$sign$symbol${"%.1f".format(Locale.US, value / 1_000_000)}M"
            value >= 1_000 -> "$sign$symbol${"%.1f".format(Locale.US, value / 1_000)}k"
            value >= 100 -> "$sign$symbol${value.roundToLong()}"
            else -> "$sign$symbol${"%.0f".format(Locale.US, value)}"
        }
    }

    fun symbol(currencyCode: String = "USD"): String {
        val code = runCatching { currencyCode }.getOrDefault("USD")
        return runCatching { Currency.getInstance(code).getSymbol(Locale.getDefault()) }
            .getOrDefault("$")
    }

    fun parseToCents(input: String): Long? {
        val cleaned = input.trim()
            .replace(",", "")
            .replace(" ", "")
            .removePrefix("$")
            .removePrefix("₹")
            .removePrefix("€")
            .removePrefix("£")
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO) return null
        if (value > BigDecimal(1_000_000_000)) return null
        return value.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}

object FinanceMath {

    fun totalIncome(transactions: List<TransactionEntity>, from: LocalDate? = null, to: LocalDate? = null): Long =
        transactions.filter { it.type == TxType.INCOME }
            .filter { inRange(it, from, to) }
            .sumOf { it.amountCents }

    fun totalExpense(transactions: List<TransactionEntity>, from: LocalDate? = null, to: LocalDate? = null): Long =
        transactions.filter { it.type == TxType.EXPENSE }
            .filter { inRange(it, from, to) }
            .sumOf { it.amountCents }

    private fun inRange(tx: TransactionEntity, from: LocalDate?, to: LocalDate?): Boolean {
        val date = DateX.date(tx.dateEpochDay)
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to))
    }

    fun balance(transactions: List<TransactionEntity>): Long =
        transactions.sumOf { if (it.type == TxType.INCOME) it.amountCents else -it.amountCents }

    fun monthTotals(
        transactions: List<TransactionEntity>,
        month: YearMonth
    ): Pair<Long, Long> {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        return totalIncome(transactions, start, end) to totalExpense(transactions, start, end)
    }

    fun expenseByCategory(
        transactions: List<TransactionEntity>,
        from: LocalDate? = null,
        to: LocalDate? = null
    ): List<CategoryTotal> {
        val colors = mapOf(
            "Food" to "#F59E0B",
            "Groceries" to "#10B981",
            "Dining" to "#F97316",
            "Transport" to "#3B82F6",
            "Shopping" to "#EC4899",
            "Entertainment" to "#8B5CF6",
            "Bills" to "#EF4444",
            "Health" to "#14B8A6",
            "Education" to "#6366F1",
            "Travel" to "#06B6D4",
            "Loans" to "#625BFF",
            "Credit Card" to "#0EA5E9",
            "General" to "#94A3B8"
        )
        return transactions.filter { it.type == TxType.EXPENSE }
            .filter { inRange(it, from, to) }
            .groupBy { it.category }
            .map { (category, txs) ->
                CategoryTotal(category, txs.sumOf { it.amountCents }, colors[category] ?: "#625BFF")
            }
            .sortedByDescending { it.amountCents }
    }

    fun weeklyExpenses(transactions: List<TransactionEntity>, end: LocalDate = DateX.today()): List<Pair<LocalDate, Long>> {
        val start = end.minusDays(6)
        return (0..6).map { offset ->
            val day = start.plusDays(offset.toLong())
            day to transactions.filter {
                it.type == TxType.EXPENSE && DateX.date(it.dateEpochDay) == day
            }.sumOf { it.amountCents }
        }
    }

    fun monthlyTrend(transactions: List<TransactionEntity>, monthsBack: Int = 6): List<MonthTotal> {
        val current = YearMonth.now()
        return (monthsBack - 1 downTo 0).map { offset ->
            val month = current.minusMonths(offset.toLong())
            val (income, expense) = monthTotals(transactions, month)
            MonthTotal(month, income, expense)
        }
    }

    fun budgetProgress(
        snapshot: FinanceSource,
        month: YearMonth = YearMonth.now()
    ): List<BudgetProgress> {
        val categories = expenseByCategory(snapshot.transactions, month.atDay(1), month.atEndOfMonth())
            .associate { it.category to it.amountCents }
        return snapshot.budgets.map { budget ->
            BudgetProgress(budget, categories[budget.category] ?: 0L)
        }.sortedByDescending { it.progress }
    }

    /**
     * Cumulative spend for the given budget categories from the 1st of the month up to today,
     * used to draw the budget trend line on the home screen.
     */
    fun cumulativeBudgetSpendByDay(
        transactions: List<TransactionEntity>,
        month: YearMonth = YearMonth.now(),
        categories: Set<String>
    ): List<Long> {
        val lastDay = if (month == YearMonth.now()) {
            DateX.today().dayOfMonth.coerceIn(1, month.lengthOfMonth())
        } else {
            month.lengthOfMonth()
        }
        val daily = LongArray(lastDay + 1)
        transactions.forEach { tx ->
            if (tx.type != TxType.EXPENSE || tx.category !in categories) return@forEach
            val date = DateX.date(tx.dateEpochDay)
            if (YearMonth.from(date) != month) return@forEach
            val day = date.dayOfMonth
            if (day in 1..lastDay) daily[day] += tx.amountCents
        }
        var running = 0L
        return (1..lastDay).map { day ->
            running += daily[day]
            running
        }
    }

    fun calculateEmiCents(principalCents: Long, annualRatePercent: Double, tenureMonths: Int): Long {
        if (tenureMonths <= 0) return 0
        val principal = principalCents / 100.0
        val monthlyRate = annualRatePercent / 100.0 / 12.0
        val emi = if (monthlyRate <= 0.0) {
            principal / tenureMonths
        } else {
            val factor = (1 + monthlyRate).pow(tenureMonths.toDouble())
            principal * monthlyRate * factor / (factor - 1)
        }
        return (emi * 100).roundToLong()
    }

    fun buildLoanSchedule(loan: LoanEntity, payments: List<LoanPaymentEntity>): LoanSummary {
        val paymentMap = payments.filter { it.loanId == loan.id }.associateBy { it.installmentNumber }
        val monthlyRate = loan.annualRatePercent / 100.0 / 12.0
        val emi = if (loan.emiCents > 0) loan.emiCents else calculateEmiCents(
            loan.principalCents, loan.annualRatePercent, loan.tenureMonths
        )
        var balance = loan.principalCents.toDouble()
        val start = DateX.date(loan.startEpochDay)
        val installments = mutableListOf<Installment>()
        var totalInterest = 0L
        for (number in 1..loan.tenureMonths) {
            val interest = (balance * monthlyRate).roundToLong()
            var principalPart = (emi - interest).coerceAtLeast(0)
            if (number == loan.tenureMonths || principalPart >= balance) {
                principalPart = balance.roundToLong()
            }
            val amount = principalPart + interest
            balance = (balance - principalPart).coerceAtLeast(0.0)
            totalInterest += interest
            val baseMonth = YearMonth.from(start).plusMonths(number.toLong())
            val due = DateX.dueDateForMonth(baseMonth, loan.dueDay)
            val paid = paymentMap[number]
            installments += Installment(
                number = number,
                dueDate = due,
                amountCents = amount,
                principalCents = principalPart,
                interestCents = interest,
                paidDate = paid?.let { DateX.date(it.paidEpochDay) }
            )
        }
        val paidInstallments = installments.filter { it.isPaid }
        val paidPrincipal = paidInstallments.sumOf { it.principalCents }
        return LoanSummary(
            loan = loan,
            installments = installments,
            paidCount = paidInstallments.size,
            paidCents = paidInstallments.sumOf { it.amountCents },
            outstandingPrincipalCents = (loan.principalCents - paidPrincipal).coerceAtLeast(0),
            totalInterestCents = totalInterest,
            totalPayableCents = loan.principalCents + totalInterest,
            nextInstallment = installments.firstOrNull { !it.isPaid }
        )
    }

    fun loanSummaries(snapshot: FinanceSource): List<LoanSummary> =
        snapshot.loans.map { buildLoanSchedule(it, snapshot.loanPayments) }

    fun dueItemsForMonth(
        snapshot: FinanceSource,
        month: YearMonth,
        today: LocalDate = DateX.today()
    ): List<DueItem> {
        val items = mutableListOf<DueItem>()
        val monthKey = DateX.monthKey(month)

        snapshot.bills.filter { it.isActive }.forEach { bill ->
            val startMonth = YearMonth.from(DateX.date(bill.startEpochDay))
            val occurs = bill.frequency == "MONTHLY" && !month.isBefore(startMonth) ||
                bill.frequency == "ONE_TIME" && month == startMonth
            if (occurs) {
                val due = DateX.dueDateForMonth(month, bill.dueDay)
                val status = when {
                    bill.lastPaidMonth == monthKey -> DueStatus.PAID
                    due.isBefore(today) -> DueStatus.OVERDUE
                    else -> DueStatus.UPCOMING
                }
                items += DueItem(
                    key = "bill:${bill.id}:$monthKey",
                    kind = DueKind.BILL,
                    title = bill.name,
                    subtitle = bill.category,
                    amountCents = bill.amountCents,
                    dueDate = due,
                    status = status,
                    referenceId = bill.id,
                    monthKey = monthKey
                )
            }
        }

        snapshot.loans.forEach { loan ->
            val summary = buildLoanSchedule(loan, snapshot.loanPayments)
            summary.installments.filter { YearMonth.from(it.dueDate) == month }.forEach { installment ->
                val status = when {
                    installment.isPaid -> DueStatus.PAID
                    installment.dueDate.isBefore(today) -> DueStatus.OVERDUE
                    else -> DueStatus.UPCOMING
                }
                items += DueItem(
                    key = "loan:${loan.id}:${installment.number}",
                    kind = DueKind.LOAN,
                    title = "EMI · ${loan.name}",
                    subtitle = "Installment ${installment.number} of ${loan.tenureMonths}",
                    amountCents = installment.amountCents,
                    dueDate = installment.dueDate,
                    status = status,
                    referenceId = loan.id,
                    installmentNumber = installment.number,
                    monthKey = monthKey
                )
            }
        }

        snapshot.cards.filter { it.balanceCents > 0 || it.lastPaidMonth == monthKey }.forEach { card ->
            val due = DateX.dueDateForMonth(month, card.dueDay)
            val status = when {
                card.lastPaidMonth == monthKey -> DueStatus.PAID
                due.isBefore(today) -> DueStatus.OVERDUE
                else -> DueStatus.UPCOMING
            }
            items += DueItem(
                key = "card:${card.id}:$monthKey",
                kind = DueKind.CARD,
                title = "Card payment · ••${card.last4}",
                subtitle = card.name,
                amountCents = card.balanceCents,
                dueDate = due,
                status = status,
                referenceId = card.id,
                monthKey = monthKey
            )
        }

        return items.sortedWith(compareBy<DueItem> { it.dueDate }.thenBy { it.title })
    }

    fun upcoming(snapshot: FinanceSource, days: Long = 30): List<DueItem> {
        val today = DateX.today()
        val end = today.plusDays(days)
        val months = listOf(YearMonth.from(today), YearMonth.from(end)).distinct()
        return months.flatMap { dueItemsForMonth(snapshot, it, today) }
            .filter { !it.dueDate.isBefore(today) && !it.dueDate.isAfter(end) && it.status != DueStatus.PAID }
            .distinctBy { it.key }
            .sortedBy { it.dueDate }
    }

    fun overdueItems(snapshot: FinanceSource): List<DueItem> {
        val month = YearMonth.now()
        return dueItemsForMonth(snapshot, month)
            .filter { it.status == DueStatus.OVERDUE }
            .sortedBy { it.dueDate }
    }

    fun creditSummary(cards: List<CreditCardEntity>): Triple<Long, Long, Float> {
        val limit = cards.sumOf { it.limitCents }
        val balance = cards.sumOf { it.balanceCents }
        val utilization = if (limit <= 0) 0f else (balance.toFloat() / limit).coerceIn(0f, 1f)
        return Triple(limit, balance, utilization)
    }

    fun creditSpend(transactions: List<TransactionEntity>, from: LocalDate? = null): Long =
        transactions.filter { it.type == TxType.EXPENSE && it.account == AccountType.CREDIT }
            .filter { inRange(it, from, null) }
            .sumOf { it.amountCents }

    fun daysOverdue(item: DueItem): Long =
        if (item.status == DueStatus.OVERDUE) DateX.daysBetween(item.dueDate, DateX.today()) else 0

    fun dueLabel(item: DueItem): String = when (item.status) {
        DueStatus.PAID -> "Paid"
        DueStatus.OVERDUE -> "${daysOverdue(item)}d overdue"
        else -> {
            val days = DateX.daysBetween(DateX.today(), item.dueDate)
            when (days) {
                0L -> "Due today"
                1L -> "Due tomorrow"
                else -> "In $days days"
            }
        }
    }

    fun monthName(month: Month, locale: Locale = Locale.getDefault()): String =
        month.getDisplayName(TextStyle.FULL, locale)
}
