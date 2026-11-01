package com.expenso.app.data

import java.util.UUID

enum class TransactionType {
    INCOME, EXPENSE
}

enum class EmiStatus {
    PENDING, PAID, OVERDUE
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String = "General",
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class Emi(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val monthlyAmount: Double = 0.0,
    val dueDayOfMonth: Int = 1,
    val dueDateMillis: Long = System.currentTimeMillis(),
    val status: EmiStatus = EmiStatus.PENDING,
    val lender: String = "",
    val notes: String = ""
)
