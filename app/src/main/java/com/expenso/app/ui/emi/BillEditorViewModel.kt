package com.expenso.app.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.Money
import com.expenso.app.data.local.BillEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class BillFormState(
    val loading: Boolean = false,
    val id: Long = 0,
    val name: String = "",
    val amountText: String = "",
    val category: String = "Bills",
    val dueDay: String = "1",
    val frequency: String = "MONTHLY",
    val startDate: LocalDate = LocalDate.now(),
    val autoPay: Boolean = false,
    val notes: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
) {
    val isEditing: Boolean get() = id > 0
}

class BillEditorViewModel(
    private val repository: FinanceRepository,
    billId: Long
) : ViewModel() {

    private val _form = MutableStateFlow(BillFormState(loading = billId > 0))
    val form: StateFlow<BillFormState> = _form.asStateFlow()

    init {
        if (billId > 0) {
            viewModelScope.launch {
                val bill = repository.getBill(billId)
                if (bill == null) {
                    _form.value = _form.value.copy(loading = false, error = "Bill not found")
                } else {
                    _form.value = BillFormState(
                        loading = false,
                        id = bill.id,
                        name = bill.name,
                        amountText = plainAmount(bill.amountCents),
                        category = bill.category,
                        dueDay = bill.dueDay.toString(),
                        frequency = bill.frequency,
                        startDate = DateX.date(bill.startEpochDay),
                        autoPay = bill.autoPay,
                        notes = bill.notes
                    )
                }
            }
        } else {
            _form.value = _form.value.copy(loading = false)
        }
    }

    fun setName(value: String) = update { it.copy(name = value, error = null) }
    fun setAmount(value: String) = update { it.copy(amountText = value, error = null) }
    fun setCategory(value: String) = update { it.copy(category = value) }
    fun setDueDay(value: String) = update { it.copy(dueDay = value.filter { c -> c.isDigit() }.take(2)) }
    fun setFrequency(value: String) = update { it.copy(frequency = value) }
    fun setStartDate(value: LocalDate) = update { it.copy(startDate = value) }
    fun setAutoPay(value: Boolean) = update { it.copy(autoPay = value) }
    fun setNotes(value: String) = update { it.copy(notes = value) }

    fun save() {
        val current = _form.value
        val amount = Money.parseToCents(current.amountText)
        val dueDay = current.dueDay.toIntOrNull()
        val error = when {
            current.name.isBlank() -> "Give this bill a name"
            amount == null -> "Enter an amount greater than zero"
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
                repository.saveBill(
                    BillEntity(
                        id = current.id,
                        name = current.name,
                        amountCents = amount!!,
                        category = current.category,
                        dueDay = dueDay!!,
                        frequency = current.frequency,
                        startEpochDay = current.startDate.toEpochDay(),
                        autoPay = current.autoPay,
                        notes = current.notes
                    )
                )
            }.onSuccess {
                update { it.copy(saving = false, saved = true) }
            }.onFailure { failure ->
                update { it.copy(saving = false, error = failure.message ?: "Could not save bill") }
            }
        }
    }

    fun delete() {
        val id = _form.value.id
        if (id <= 0) return
        viewModelScope.launch {
            repository.deleteBill(id)
            update { it.copy(deleted = true) }
        }
    }

    private fun update(block: (BillFormState) -> BillFormState) {
        _form.value = block(_form.value)
    }

    private fun plainAmount(cents: Long): String =
        BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()
}
