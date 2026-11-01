package com.expenso.app.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.LoanSummary
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoanDetailUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val summary: LoanSummary? = null,
    val deleted: Boolean = false
)

class LoanDetailViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository,
    private val loanId: Long
) : ViewModel() {

    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<LoanDetailUiState> = combine(
        repository.snapshot,
        settings.preferences,
        deleted
    ) { snapshot, prefs, isDeleted ->
        val loan = snapshot.loans.firstOrNull { it.id == loanId }
        LoanDetailUiState(
            loaded = true,
            preferences = prefs,
            summary = loan?.let { FinanceMath.buildLoanSchedule(it, snapshot.loanPayments) },
            deleted = isDeleted
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoanDetailUiState())

    fun setInstallmentPaid(number: Int, paid: Boolean) {
        viewModelScope.launch {
            val loan = repository.getLoan(loanId) ?: return@launch
            repository.markInstallmentPaid(loan, number, paid)
        }
    }

    fun deleteLoan() {
        viewModelScope.launch {
            repository.deleteLoan(loanId)
            deleted.value = true
        }
    }
}
