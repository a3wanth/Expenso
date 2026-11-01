package com.expenso.app.data

import com.expenso.app.data.local.AppDatabase
import com.expenso.app.data.local.BillEntity
import com.expenso.app.data.local.BudgetEntity
import com.expenso.app.data.local.CategoryEntity
import com.expenso.app.data.local.CreditCardEntity
import com.expenso.app.data.local.LoanEntity
import com.expenso.app.data.local.LoanPaymentEntity
import com.expenso.app.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.Locale

class FinanceRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val loanDao = database.loanDao()
    private val loanPaymentDao = database.loanPaymentDao()
    private val billDao = database.billDao()
    private val cardDao = database.creditCardDao()
    private val categoryDao = database.categoryDao()

    private data class Core(
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>,
        val loans: List<LoanEntity>,
        val payments: List<LoanPaymentEntity>,
        val bills: List<BillEntity>
    )

    val snapshot: Flow<FinanceSnapshot> = combine(
        transactionDao.observeAll(),
        budgetDao.observeAll(),
        loanDao.observeAll(),
        loanPaymentDao.observeAll(),
        billDao.observeAll()
    ) { transactions, budgets, loans, payments, bills ->
        Core(transactions, budgets, loans, payments, bills)
    }.combine(cardDao.observeAll()) { core, cards -> core to cards }
        .combine(categoryDao.observeAll()) { (core, cards), categories ->
            FinanceSnapshot(
                transactions = core.transactions,
                budgets = core.budgets,
                loans = core.loans,
                loanPayments = core.payments,
                bills = core.bills,
                cards = cards,
                categories = categories
            )
        }

    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun seedCategoriesIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(defaultCategories())
        }
    }

    suspend fun resetCategoriesToDefaults() {
        categoryDao.clear()
        categoryDao.insertAll(defaultCategories())
    }

    private fun defaultCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Food", iconKey = "restaurant", colorHex = "#F59E0B", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Groceries", iconKey = "groceries", colorHex = "#10B981", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Dining", iconKey = "dining", colorHex = "#F97316", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Transport", iconKey = "transport", colorHex = "#3B82F6", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Shopping", iconKey = "shopping", colorHex = "#EC4899", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Entertainment", iconKey = "entertainment", colorHex = "#8B5CF6", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Bills", iconKey = "bills", colorHex = "#EF4444", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Health", iconKey = "health", colorHex = "#14B8A6", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Education", iconKey = "education", colorHex = "#6366F1", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Travel", iconKey = "travel", colorHex = "#06B6D4", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Loans", iconKey = "loans", colorHex = "#625BFF", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Credit Card", iconKey = "credit_card", colorHex = "#0EA5E9", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "General", iconKey = "category", colorHex = "#94A3B8", type = TxType.EXPENSE, isDefault = true),
        CategoryEntity(name = "Salary", iconKey = "salary", colorHex = "#16A36A", type = TxType.INCOME, isDefault = true),
        CategoryEntity(name = "Freelance", iconKey = "freelance", colorHex = "#22C55E", type = TxType.INCOME, isDefault = true),
        CategoryEntity(name = "Investments", iconKey = "investments", colorHex = "#0EA5E9", type = TxType.INCOME, isDefault = true),
        CategoryEntity(name = "Gifts", iconKey = "gifts", colorHex = "#F59E0B", type = TxType.INCOME, isDefault = true),
        CategoryEntity(name = "Other Income", iconKey = "income", colorHex = "#84CC16", type = TxType.INCOME, isDefault = true)
    )

    // ----------------------------- Transactions -----------------------------

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        val sanitized = transaction.copy(
            title = transaction.title.trim().ifBlank { "Untitled" },
            category = transaction.category.ifBlank { "General" },
            notes = transaction.notes.trim()
        )
        val existing = if (sanitized.id > 0) transactionDao.getById(sanitized.id) else null
        val withCreated = if (existing != null) sanitized.copy(createdAt = existing.createdAt) else sanitized
        return transactionDao.upsert(withCreated)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun getTransaction(id: Long): TransactionEntity? = transactionDao.getById(id)

    // -------------------------------- Budgets -------------------------------

    suspend fun saveBudget(category: String, amountCents: Long): Long {
        val existing = budgetDao.getByCategory(category)
        val entity = BudgetEntity(
            id = existing?.id ?: 0,
            category = category,
            amountCents = amountCents,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        return budgetDao.upsert(entity)
    }

    suspend fun deleteBudget(id: Long) = budgetDao.deleteById(id)

    // --------------------------------- Loans --------------------------------

    suspend fun saveLoan(loan: LoanEntity): Long {
        val emi = if (loan.emiCents > 0) loan.emiCents else FinanceMath.calculateEmiCents(
            loan.principalCents, loan.annualRatePercent, loan.tenureMonths
        )
        val existing = if (loan.id > 0) loanDao.getById(loan.id) else null
        val entity = loan.copy(
            name = loan.name.trim().ifBlank { "Loan" },
            lender = loan.lender.trim(),
            emiCents = emi,
            annualRatePercent = loan.annualRatePercent.coerceIn(0.0, 100.0),
            tenureMonths = loan.tenureMonths.coerceIn(1, 480),
            dueDay = loan.dueDay.coerceIn(1, 28),
            createdAt = existing?.createdAt ?: loan.createdAt
        )
        return loanDao.upsert(entity)
    }

    suspend fun deleteLoan(id: Long) {
        loanPaymentDao.deleteForLoan(id)
        transactionDao.deleteBySourcePrefix("loan:$id:")
        loanDao.deleteById(id)
    }

    suspend fun getLoan(id: Long): LoanEntity? = loanDao.getById(id)

    suspend fun markInstallmentPaid(loan: LoanEntity, number: Int, paid: Boolean) {
        val payments = loanPaymentDao.getForLoan(loan.id)
        val summary = FinanceMath.buildLoanSchedule(loan, payments)
        val installment = summary.installments.firstOrNull { it.number == number } ?: return
        val sourceKey = "loan:${loan.id}:$number"
        if (paid) {
            if (payments.none { it.installmentNumber == number }) {
                loanPaymentDao.upsert(
                    LoanPaymentEntity(
                        loanId = loan.id,
                        installmentNumber = number,
                        amountCents = installment.amountCents,
                        paidEpochDay = DateX.epochDayNow
                    )
                )
            }
            putTransaction(
                TransactionEntity(
                    title = "EMI · ${loan.name}",
                    amountCents = installment.amountCents,
                    type = TxType.EXPENSE,
                    category = "Loans",
                    dateEpochDay = DateX.epochDayNow,
                    notes = "Installment $number of ${loan.tenureMonths}",
                    account = AccountType.BANK,
                    sourceKey = sourceKey
                )
            )
        } else {
            loanPaymentDao.deletePayment(loan.id, number)
            transactionDao.deleteBySourceKey(sourceKey)
        }
        val updated = FinanceMath.buildLoanSchedule(loan, loanPaymentDao.getForLoan(loan.id))
        loanDao.upsert(loan.copy(isClosed = updated.isComplete))
    }

    // --------------------------------- Bills --------------------------------

    suspend fun saveBill(bill: BillEntity): Long {
        val existing = if (bill.id > 0) billDao.getById(bill.id) else null
        return billDao.upsert(
            bill.copy(
                name = bill.name.trim().ifBlank { "Bill" },
                dueDay = bill.dueDay.coerceIn(1, 28),
                category = bill.category.ifBlank { "Bills" },
                createdAt = existing?.createdAt ?: bill.createdAt
            )
        )
    }

    suspend fun deleteBill(id: Long) {
        transactionDao.deleteBySourcePrefix("bill:$id:")
        billDao.deleteById(id)
    }

    suspend fun getBill(id: Long): BillEntity? = billDao.getById(id)

    suspend fun markBillPaid(bill: BillEntity, monthKey: String, paid: Boolean) {
        if (paid) {
            billDao.upsert(bill.copy(lastPaidMonth = monthKey))
            putTransaction(
                TransactionEntity(
                    title = "Bill · ${bill.name}",
                    amountCents = bill.amountCents,
                    type = TxType.EXPENSE,
                    category = bill.category,
                    dateEpochDay = DateX.epochDayNow,
                    notes = if (bill.autoPay) "Auto-paid · $monthKey" else "Paid · $monthKey",
                    account = AccountType.BANK,
                    sourceKey = "bill:${bill.id}:$monthKey"
                )
            )
        } else {
            billDao.upsert(bill.copy(lastPaidMonth = null))
            transactionDao.deleteBySourceKey("bill:${bill.id}:$monthKey")
        }
    }

    // ----------------------------- Credit cards -----------------------------

    suspend fun saveCard(card: CreditCardEntity): Long {
        val existing = if (card.id > 0) cardDao.getById(card.id) else null
        return cardDao.upsert(
            card.copy(
                name = card.name.trim().ifBlank { "Credit Card" },
                last4 = card.last4.filter { it.isDigit() }.takeLast(4).padStart(4, '0'),
                limitCents = card.limitCents.coerceAtLeast(0),
                balanceCents = card.balanceCents.coerceAtLeast(0),
                dueDay = card.dueDay.coerceIn(1, 28),
                createdAt = existing?.createdAt ?: card.createdAt
            )
        )
    }

    suspend fun deleteCard(id: Long) = cardDao.deleteById(id)

    suspend fun getCard(id: Long): CreditCardEntity? = cardDao.getById(id)

    suspend fun recordCardPayment(card: CreditCardEntity, amountCents: Long, monthKey: String) {
        if (amountCents <= 0 || card.balanceCents <= 0) return
        val payment = amountCents.coerceAtMost(card.balanceCents)
        val remaining = card.balanceCents - payment
        cardDao.upsert(card.copy(balanceCents = remaining, lastPaidMonth = monthKey))
        val sourceKey = "cardpay:${card.id}:$monthKey"
        val existing = transactionDao.getBySourceKey(sourceKey)
        val previous = existing?.amountCents ?: 0L
        putTransaction(
            TransactionEntity(
                id = existing?.id ?: 0,
                title = "Card payment · ${card.name}",
                amountCents = previous + payment,
                type = TxType.EXPENSE,
                category = "Credit Card",
                dateEpochDay = DateX.epochDayNow,
                notes = "••${card.last4} payment",
                account = AccountType.BANK,
                sourceKey = sourceKey,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
    }

    // ------------------------------- Categories -----------------------------

    suspend fun saveCategory(category: CategoryEntity): Long {
        val existingByName = categoryDao.getByName(category.name.trim())
        if (existingByName != null && existingByName.id != category.id) {
            throw IllegalArgumentException("A category with this name already exists")
        }
        val existing = if (category.id > 0) categoryDao.getAll().firstOrNull { it.id == category.id } else null
        return categoryDao.upsert(
            category.copy(
                name = category.name.trim(),
                createdAt = existing?.createdAt ?: category.createdAt
            )
        )
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        val budget = budgetDao.getByCategory(category.name)
        if (budget != null) budgetDao.deleteById(budget.id)
        categoryDao.deleteById(category.id)
    }

    // ------------------------------- Utilities ------------------------------

    private suspend fun putTransaction(transaction: TransactionEntity) {
        val existing = transaction.sourceKey?.let { transactionDao.getBySourceKey(it) }
        val merged = if (existing != null) {
            transaction.copy(id = existing.id, createdAt = existing.createdAt)
        } else transaction
        transactionDao.upsert(merged)
    }

    suspend fun clearFinancialData() {
        transactionDao.clear()
        budgetDao.clear()
        loanPaymentDao.clear()
        loanDao.clear()
        billDao.clear()
        cardDao.clear()
    }

    suspend fun buildCsv(): String {
        val snapshotData = FinanceSnapshot(
            transactions = transactionDao.getAll(),
            budgets = budgetDao.getAll(),
            loans = loanDao.getAll(),
            loanPayments = loanPaymentDao.getAll(),
            bills = billDao.getAll(),
            cards = cardDao.getAll(),
            categories = categoryDao.getAll()
        )
        val sb = StringBuilder()
        sb.appendLine("Expenso export")
        sb.appendLine("Generated,${csv(DateX.today().toString())}")
        sb.appendLine()
        sb.appendLine("Transactions")
        sb.appendLine("Date,Title,Type,Category,Amount,Account,Notes")
        snapshotData.transactions.forEach { tx ->
            sb.appendLine(
                listOf(
                    DateX.date(tx.dateEpochDay).toString(),
                    tx.title,
                    tx.type,
                    tx.category,
                    moneyPlain(tx.amountCents),
                    tx.account,
                    tx.notes
                ).joinToString(",") { csv(it) }
            )
        }
        sb.appendLine()
        sb.appendLine("Budgets")
        sb.appendLine("Category,Monthly limit")
        snapshotData.budgets.forEach {
            sb.appendLine("${csv(it.category)},${moneyPlain(it.amountCents)}")
        }
        sb.appendLine()
        sb.appendLine("Loans")
        sb.appendLine("Name,Lender,Principal,Annual rate,Months,EMI,Start,Outstanding principal")
        snapshotData.loans.forEach { loan ->
            val summary = FinanceMath.buildLoanSchedule(loan, snapshotData.loanPayments)
            sb.appendLine(
                listOf(
                    loan.name,
                    loan.lender,
                    moneyPlain(loan.principalCents),
                    loan.annualRatePercent.toString(),
                    loan.tenureMonths.toString(),
                    moneyPlain(loan.emiCents),
                    DateX.date(loan.startEpochDay).toString(),
                    moneyPlain(summary.outstandingPrincipalCents)
                ).joinToString(",") { csv(it) }
            )
        }
        sb.appendLine()
        sb.appendLine("Repayment schedule")
        sb.appendLine("Loan,Installment,Due date,Amount,Principal,Interest,Status")
        snapshotData.loans.forEach { loan ->
            val summary = FinanceMath.buildLoanSchedule(loan, snapshotData.loanPayments)
            summary.installments.forEach { installment ->
                sb.appendLine(
                    listOf(
                        loan.name,
                        installment.number.toString(),
                        installment.dueDate.toString(),
                        moneyPlain(installment.amountCents),
                        moneyPlain(installment.principalCents),
                        moneyPlain(installment.interestCents),
                        if (installment.isPaid) "PAID" else "PENDING"
                    ).joinToString(",") { csv(it) }
                )
            }
        }
        sb.appendLine()
        sb.appendLine("Bills")
        sb.appendLine("Name,Category,Amount,Due day,Frequency,Last paid month")
        snapshotData.bills.forEach {
            sb.appendLine(
                listOf(
                    it.name,
                    it.category,
                    moneyPlain(it.amountCents),
                    it.dueDay.toString(),
                    it.frequency,
                    it.lastPaidMonth ?: ""
                ).joinToString(",") { csv(it) }
            )
        }
        sb.appendLine()
        sb.appendLine("Credit cards")
        sb.appendLine("Name,Last 4,Limit,Balance,Due day")
        snapshotData.cards.forEach {
            sb.appendLine(
                listOf(
                    it.name,
                    it.last4,
                    moneyPlain(it.limitCents),
                    moneyPlain(it.balanceCents),
                    it.dueDay.toString()
                ).joinToString(",") { csv(it) }
            )
        }
        return sb.toString()
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('\n') || escaped.contains('"')) {
            "\"$escaped\""
        } else escaped
    }

    private fun moneyPlain(cents: Long): String =
        BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toPlainString()

    // -------------------------------- Demo data -----------------------------

    suspend fun loadDemoData() {
        clearFinancialData()
        resetCategoriesToDefaults()
        val today = DateX.today()
        val monthStart = today.withDayOfMonth(1)

        val cardId = cardDao.upsert(
            CreditCardEntity(
                name = "Sapphire Reserve",
                last4 = "8241",
                limitCents = 800_000,
                balanceCents = 124_000,
                dueDay = 15,
                colorIndex = 0
            )
        )
        cardDao.upsert(
            CreditCardEntity(
                name = "Everyday Cash",
                last4 = "1908",
                limitCents = 300_000,
                balanceCents = 38_500,
                dueDay = 22,
                colorIndex = 1
            )
        )

        val transactions = buildList {
            add(TransactionEntity(title = "Monthly salary", amountCents = 520_000, type = TxType.INCOME, category = "Salary", dateEpochDay = monthStart.toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Freelance · Northwind", amountCents = 85_000, type = TxType.INCOME, category = "Freelance", dateEpochDay = today.minusDays(6).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Dividend payout", amountCents = 12_400, type = TxType.INCOME, category = "Investments", dateEpochDay = today.minusDays(11).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Rent", amountCents = 185_000, type = TxType.EXPENSE, category = "Bills", dateEpochDay = monthStart.toEpochDay(), account = AccountType.BANK, notes = "Monthly apartment rent"))
            add(TransactionEntity(title = "Whole Foods Market", amountCents = 12_842, type = TxType.EXPENSE, category = "Groceries", dateEpochDay = today.minusDays(1).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Blue Bottle Coffee", amountCents = 675, type = TxType.EXPENSE, category = "Dining", dateEpochDay = today.toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Uber ride", amountCents = 2_450, type = TxType.EXPENSE, category = "Transport", dateEpochDay = today.minusDays(2).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Nike running shoes", amountCents = 9_600, type = TxType.EXPENSE, category = "Shopping", dateEpochDay = today.minusDays(4).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Netflix", amountCents = 1_549, type = TxType.EXPENSE, category = "Entertainment", dateEpochDay = today.minusDays(7).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Apple Music", amountCents = 1_099, type = TxType.EXPENSE, category = "Entertainment", dateEpochDay = today.minusDays(9).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Equinox membership", amountCents = 4_500, type = TxType.EXPENSE, category = "Health", dateEpochDay = today.minusDays(12).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Shell fuel", amountCents = 5_820, type = TxType.EXPENSE, category = "Transport", dateEpochDay = today.minusDays(8).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Amazon order", amountCents = 7_499, type = TxType.EXPENSE, category = "Shopping", dateEpochDay = today.minusDays(14).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Sunday brunch", amountCents = 6_820, type = TxType.EXPENSE, category = "Dining", dateEpochDay = today.minusDays(3).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Pharmacy", amountCents = 3_240, type = TxType.EXPENSE, category = "Health", dateEpochDay = today.minusDays(16).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Electric bill", amountCents = 9_450, type = TxType.EXPENSE, category = "Bills", dateEpochDay = today.minusDays(10).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Trader Joe's", amountCents = 8_764, type = TxType.EXPENSE, category = "Groceries", dateEpochDay = today.minusDays(19).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Spotify", amountCents = 1_199, type = TxType.EXPENSE, category = "Entertainment", dateEpochDay = today.minusDays(21).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Previous salary", amountCents = 520_000, type = TxType.INCOME, category = "Salary", dateEpochDay = monthStart.minusMonths(1).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Last month rent", amountCents = 185_000, type = TxType.EXPENSE, category = "Bills", dateEpochDay = monthStart.minusMonths(1).toEpochDay(), account = AccountType.BANK))
            add(TransactionEntity(title = "Weekend trip", amountCents = 42_000, type = TxType.EXPENSE, category = "Travel", dateEpochDay = today.minusDays(38).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
            add(TransactionEntity(title = "Restaurant week", amountCents = 11_300, type = TxType.EXPENSE, category = "Dining", dateEpochDay = today.minusDays(41).toEpochDay(), account = AccountType.CREDIT, creditCardId = cardId))
        }
        transactionDao.insertAll(transactions)

        budgetDao.insertAll(
            listOf(
                BudgetEntity(category = "Groceries", amountCents = 60_000),
                BudgetEntity(category = "Dining", amountCents = 25_000),
                BudgetEntity(category = "Transport", amountCents = 18_000),
                BudgetEntity(category = "Shopping", amountCents = 30_000),
                BudgetEntity(category = "Entertainment", amountCents = 12_000),
                BudgetEntity(category = "Bills", amountCents = 200_000)
            )
        )

        billDao.insertAll(
            listOf(
                BillEntity(name = "Apartment rent", amountCents = 185_000, category = "Bills", dueDay = 1, startEpochDay = today.minusMonths(18).withDayOfMonth(1).toEpochDay(), lastPaidMonth = DateX.monthKey(today)),
                BillEntity(name = "Electricity", amountCents = 9_450, category = "Bills", dueDay = 18, startEpochDay = today.minusMonths(12).toEpochDay()),
                BillEntity(name = "Internet", amountCents = 7_000, category = "Bills", dueDay = 12, startEpochDay = today.minusMonths(10).toEpochDay()),
                BillEntity(name = "Netflix", amountCents = 1_549, category = "Entertainment", dueDay = 22, startEpochDay = today.minusMonths(8).toEpochDay()),
                BillEntity(name = "Equinox", amountCents = 4_500, category = "Health", dueDay = 5, startEpochDay = today.minusMonths(6).toEpochDay())
            )
        )

        val carLoanId = loanDao.upsert(
            LoanEntity(
                name = "Tesla Model 3",
                lender = "Chase Auto",
                principalCents = 3_800_000,
                annualRatePercent = 5.4,
                tenureMonths = 60,
                emiCents = 0,
                startEpochDay = today.minusMonths(10).toEpochDay(),
                dueDay = 8
            ).let {
                it.copy(emiCents = FinanceMath.calculateEmiCents(it.principalCents, it.annualRatePercent, it.tenureMonths))
            }
        )
        val macLoanId = loanDao.upsert(
            LoanEntity(
                name = "MacBook Pro",
                lender = "Apple Card",
                principalCents = 249_900,
                annualRatePercent = 0.0,
                tenureMonths = 12,
                emiCents = 0,
                startEpochDay = today.minusMonths(3).toEpochDay(),
                dueDay = 20
            ).let {
                it.copy(emiCents = FinanceMath.calculateEmiCents(it.principalCents, it.annualRatePercent, it.tenureMonths))
            }
        )
        // Seed realistic repayment history for the active loans.
        listOf(carLoanId to 9, macLoanId to 2).forEach { (loanId, paidCount) ->
            val loan = loanDao.getById(loanId) ?: return@forEach
            val summary = FinanceMath.buildLoanSchedule(loan, emptyList())
            summary.installments.take(paidCount).forEach { installment ->
                loanPaymentDao.upsert(
                    LoanPaymentEntity(
                        loanId = loan.id,
                        installmentNumber = installment.number,
                        amountCents = installment.amountCents,
                        paidEpochDay = installment.dueDate.toEpochDay()
                    )
                )
                transactionDao.insertAll(
                    listOf(
                        TransactionEntity(
                            title = "EMI · ${loan.name}",
                            amountCents = installment.amountCents,
                            type = TxType.EXPENSE,
                            category = "Loans",
                            dateEpochDay = installment.dueDate.toEpochDay(),
                            notes = "Installment ${installment.number} of ${loan.tenureMonths}",
                            account = AccountType.BANK,
                            sourceKey = "loan:${loan.id}:${installment.number}"
                        )
                    )
                )
            }
            loanDao.upsert(loan.copy(isClosed = paidCount >= loan.tenureMonths))
        }
    }

    suspend fun countTransactions(): Int = transactionDao.count()

    suspend fun reportSummary(): String {
        val tx = transactionDao.getAll()
        val income = FinanceMath.totalIncome(tx)
        val expense = FinanceMath.totalExpense(tx)
        return "Expenso · ${tx.size} transactions · Income ${Money.format(income, "USD")} · Expenses ${Money.format(expense, "USD")} · ${YearMonth.now()}"
    }
}
