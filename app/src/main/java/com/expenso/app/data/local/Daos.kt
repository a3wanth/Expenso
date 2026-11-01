package com.expenso.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAt DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE sourceKey = :key LIMIT 1")
    suspend fun getBySourceKey(key: String): TransactionEntity?

    @Upsert
    suspend fun upsert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE sourceKey = :key")
    suspend fun deleteBySourceKey(key: String)

    @Query("DELETE FROM transactions WHERE sourceKey LIKE :prefix || '%'")
    suspend fun deleteBySourcePrefix(prefix: String)

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY category COLLATE NOCASE")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY category COLLATE NOCASE")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getByCategory(category: String): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY isClosed ASC, createdAt DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY isClosed ASC, createdAt DESC")
    suspend fun getAll(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LoanEntity?

    @Upsert
    suspend fun upsert(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<LoanEntity>)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM loans")
    suspend fun clear()
}

@Dao
interface LoanPaymentDao {
    @Query("SELECT * FROM loan_payments ORDER BY paidEpochDay DESC")
    fun observeAll(): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments ORDER BY paidEpochDay DESC")
    suspend fun getAll(): List<LoanPaymentEntity>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY installmentNumber")
    suspend fun getForLoan(loanId: Long): List<LoanPaymentEntity>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId AND installmentNumber = :number LIMIT 1")
    suspend fun getPayment(loanId: Long, number: Int): LoanPaymentEntity?

    @Upsert
    suspend fun upsert(payment: LoanPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<LoanPaymentEntity>)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId AND installmentNumber = :number")
    suspend fun deletePayment(loanId: Long, number: Int)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deleteForLoan(loanId: Long)

    @Query("DELETE FROM loan_payments")
    suspend fun clear()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE isActive = 1 ORDER BY dueDay")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY dueDay")
    suspend fun getAll(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BillEntity?

    @Upsert
    suspend fun upsert(bill: BillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<BillEntity>)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bills")
    suspend fun clear()
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY createdAt")
    fun observeAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY createdAt")
    suspend fun getAll(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CreditCardEntity?

    @Upsert
    suspend fun upsert(card: CreditCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CreditCardEntity>)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM credit_cards")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name COLLATE NOCASE")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?

    @Upsert
    suspend fun upsert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("DELETE FROM categories")
    suspend fun clear()
}
