package com.expenso.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.AccountType
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.Money
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.local.TransactionEntity
import com.expenso.app.data.TxType
import com.expenso.app.data.UserPreferences
import com.expenso.app.data.local.CategoryEntity
import com.expenso.app.data.local.CreditCardEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class TransactionForm(
    val loading: Boolean = false,
    val id: Long = 0,
    val sourceKey: String? = null,
    val title: String = "",
    val amountText: String = "",
    val type: String = TxType.EXPENSE,
    val category: String = "General",
    val dateEpochDay: Long = DateX.epochDayNow,
    val notes: String = "",
    val account: String = AccountType.BANK,
    val creditCardId: Long? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
    val isEditing: Boolean get() = id > 0
}

data class TransactionEditorUiState(
    val form: TransactionForm = TransactionForm(),
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<CategoryEntity> = emptyList(),
    val cards: List<CreditCardEntity> = emptyList()
)

class TransactionEditorViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository,
    transactionId: Long,
    initialType: String
) : ViewModel() {

    private val form = MutableStateFlow(
        TransactionForm(
            loading = transactionId > 0,
            type = if (initialType == TxType.INCOME) TxType.INCOME else TxType.EXPENSE,
            category = if (initialType == TxType.INCOME) "Salary" else "General"
        )
    )

    val uiState: StateFlow<TransactionEditorUiState> = combine(
        form,
        repository.snapshot,
        settings.preferences
    ) { currentForm, snapshot, prefs ->
        TransactionEditorUiState(
            form = currentForm,
            preferences = prefs,
            categories = snapshot.categories,
            cards = snapshot.cards
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionEditorUiState())

    init {
        if (transactionId > 0) {
            viewModelScope.launch {
                val transaction = repository.getTransaction(transactionId)
                if (transaction == null) {
                    form.value = form.value.copy(loading = false, error = "Transaction not found")
                } else {
                    form.value = TransactionForm(
                        loading = false,
                        id = transaction.id,
                        sourceKey = transaction.sourceKey,
                        title = transaction.title,
                        amountText = plainAmount(transaction.amountCents),
                        type = transaction.type,
                        category = transaction.category,
                        dateEpochDay = transaction.dateEpochDay,
                        notes = transaction.notes,
                        account = transaction.account,
                        creditCardId = transaction.creditCardId
                    )
                }
            }
        } else {
            form.value = form.value.copy(loading = false)
        }
    }

    fun setTitle(value: String) = update { it.copy(title = value, error = null) }
    fun setAmount(value: String) = update { it.copy(amountText = value, error = null) }
    fun setNotes(value: String) = update { it.copy(notes = value) }
    fun setDate(date: LocalDate) = update { it.copy(dateEpochDay = date.toEpochDay()) }

    fun setType(type: String) {
        update {
            it.copy(
                type = type,
                category = when {
                    type == TxType.INCOME && it.category in listOf("General", "Food", "Bills", "Shopping") -> "Salary"
                    type == TxType.EXPENSE && it.category in listOf("Salary", "Freelance", "Investments") -> "General"
                    else -> it.category
                },
                error = null
            )
        }
    }

    fun setCategory(category: String) = update { it.copy(category = category) }

    fun setAccount(account: String) = update {
        it.copy(
            account = account,
            creditCardId = if (account == AccountType.CREDIT) it.creditCardId else null
        )
    }

    fun setCreditCard(id: Long?) = update { it.copy(creditCardId = id) }

    fun save() {
        val current = form.value
        if (current.saving) return
        val amount = Money.parseToCents(current.amountText)
        val error = when {
            current.title.isBlank() -> "Add a title for this transaction"
            amount == null -> "Enter an amount greater than zero"
            current.category.isBlank() -> "Choose a category"
            else -> null
        }
        if (error != null) {
            update { it.copy(error = error) }
            return
        }
        update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repository.saveTransaction(
                    TransactionEntity(
                        id = current.id,
                        title = current.title,
                        amountCents = amount!!,
                        type = current.type,
                        category = current.category,
                        dateEpochDay = current.dateEpochDay,
                        notes = current.notes,
                        account = current.account,
                        creditCardId = if (current.account == AccountType.CREDIT) current.creditCardId else null,
                        sourceKey = current.sourceKey
                    )
                )
            }.onSuccess {
                update { it.copy(saving = false, saved = true) }
            }.onFailure { failure ->
                update { it.copy(saving = false, error = failure.message ?: "Could not save transaction") }
            }
        }
    }

    private fun update(block: (TransactionForm) -> TransactionForm) {
        form.value = block(form.value)
    }

    private fun plainAmount(cents: Long): String =
        BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()
}
