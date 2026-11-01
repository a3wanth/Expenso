package com.expenso.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Money is always stored in minor units (cents) to avoid floating point drift.
 * Dates are stored as epoch days so day-based grouping is timezone-stable.
 */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["sourceKey"], unique = true), Index("dateEpochDay"), Index("category")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val type: String, // INCOME | EXPENSE
    val category: String,
    val dateEpochDay: Long,
    val notes: String = "",
    val account: String = "BANK", // BANK | CASH | CREDIT
    val creditCardId: Long? = null,
    val sourceKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val amountCents: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lender: String = "",
    val principalCents: Long,
    val annualRatePercent: Double,
    val tenureMonths: Int,
    val emiCents: Long,
    val startEpochDay: Long,
    val dueDay: Int = 5,
    val notes: String = "",
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "loan_payments",
    indices = [Index(value = ["loanId", "installmentNumber"], unique = true), Index("loanId")]
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val installmentNumber: Int,
    val amountCents: Long,
    val paidEpochDay: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val category: String,
    val dueDay: Int,
    val frequency: String = "MONTHLY", // MONTHLY | ONE_TIME
    val startEpochDay: Long,
    val lastPaidMonth: String? = null,
    val autoPay: Boolean = false,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val last4: String,
    val limitCents: Long,
    val balanceCents: Long = 0,
    val dueDay: Int = 15,
    val lastPaidMonth: String? = null,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String = "category",
    val colorHex: String = "#625BFF",
    val type: String = "EXPENSE", // EXPENSE | INCOME
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
