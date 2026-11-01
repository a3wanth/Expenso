package com.expenso.app.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.Money
import com.expenso.app.data.local.LoanEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class LoanFormState(
    val loading: Boolean = false,
    val id: Long = 0,
    val name: String = "",
    val lender: String = "",
    val principalText: String = "",
    val rateText: String = "0.0",
    val tenureText: String = "12",
    val emiText: String = "",
    val dueDay: String = "5",
    val startDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
) {
    val isEditing: Boolean get() = id > 0
}

class LoanEditorViewModel(
    private val repository: FinanceRepository,
    loanId: Long
) : ViewModel() {

    private val _form = MutableStateFlow(LoanFormState(loading = loanId > 0))
    val form: StateFlow<LoanFormState> = _form.asStateFlow()

    init {
        if (loanId > 0) {
            viewModelScope.launch {
                val loan = repository.getLoan(loanId)
                if (loan == null) {
                    _form.value = _form.value.copy(loading = false, error = "Loan not found")
                } else {
                    _form.value = LoanFormState(
                        loading = false,
                        id = loan.id,
                        name = loan.name,
                        lender = loan.lender,
                        principalText = plainAmount(loan.principalCents),
                        rateText = trimNumber(loan.annualRatePercent),
                        tenureText = loan.tenureMonths.toString(),
                        emiText = plainAmount(loan.emiCents),
                        dueDay = loan.dueDay.toString(),
                        startDate = DateX.date(loan.startEpochDay),
                        notes = loan.notes
                    )
                }
            }
        } else {
            _form.value = _form.value.copy(loading = false)
        }
    }

    fun setName(value: String) = update { it.copy(name = value, error = null) }
    fun setLender(value: String) = update { it.copy(lender = value) }
    fun setPrincipal(value: String) = update { it.copy(principalText = value, error = null) }
    fun setRate(value: String) = update { it.copy(rateText = value, error = null) }
    fun setTenure(value: String) = update { it.copy(tenureText = value, error = null) }
    fun setEmi(value: String) = update { it.copy(emiText = value, error = null) }
    fun setDueDay(value: String) = update { it.copy(dueDay = value.filter { char -> char.isDigit() }.take(2)) }
    fun setStartDate(date: LocalDate) = update { it.copy(startDate = date) }
    fun setNotes(value: String) = update { it.copy(notes = value) }

    fun save() {
        val current = _form.value
        val principal = Money.parseToCents(current.principalText)
        val rate = current.rateText.toDoubleOrNull()
        val tenure = current.tenureText.toIntOrNull()
        val emi = if (current.emiText.isBlank()) null else Money.parseToCents(current.emiText)
        val dueDay = current.dueDay.toIntOrNull()
        val error = when {
            current.name.isBlank() -> "Give this loan a name"
            principal == null -> "Enter a principal greater than zero"
            rate == null || rate !in 0.0..100.0 -> "Enter an annual rate between 0 and 100"
            tenure == null || tenure !in 1..480 -> "Tenure must be between 1 and 480 months"
            current.emiText.isNotBlank() && emi == null -> "Enter a valid EMI amount or leave it blank"
            dueDay == null || dueDay !in 1..28 -> "Due day must be between 1 and 28"
            else -> null
        }
        if (error != null) {
            update { it.copy(error = error) }
            return
        }
        update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repository.saveLoan(
                    LoanEntity(
                        id = current.id,
                        name = current.name,
                        lender = current.lender,
                        principalCents = principal!!,
                        annualRatePercent = rate!!,
                        tenureMonths = tenure!!,
                        emiCents = emi ?: 0,
                        startEpochDay = current.startDate.toEpochDay(),
                        dueDay = dueDay!!,
                        notes = current.notes
                    )
                )
            }.onSuccess {
                update { it.copy(saving = false, saved = true) }
            }.onFailure { failure ->
                update { it.copy(saving = false, error = failure.message ?: "Could not save loan") }
            }
        }
    }

    fun delete() {
        val id = _form.value.id
        if (id <= 0) return
        viewModelScope.launch {
            repository.deleteLoan(id)
            update { it.copy(deleted = true) }
        }
    }

    private fun update(block: (LoanFormState) -> LoanFormState) {
        _form.value = block(_form.value)
    }

    private fun plainAmount(cents: Long): String =
        BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()

    private fun trimNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
