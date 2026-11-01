package com.expenso.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.local.CategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRow(
    val category: CategoryEntity,
    val transactionCount: Int,
    val hasBudget: Boolean
)

data class CategoriesUiState(
    val loaded: Boolean = false,
    val rows: List<CategoryRow> = emptyList(),
    val expenseCount: Int = 0,
    val incomeCount: Int = 0
)

class CategoriesViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<CategoriesUiState> = repository.snapshot
        .map { snapshot ->
            val usage = snapshot.transactions.groupingBy { it.category }.eachCount()
            val budgeted = snapshot.budgets.map { it.category }.toSet()
            CategoriesUiState(
                loaded = true,
                rows = snapshot.categories.map {
                    CategoryRow(it, usage[it.name] ?: 0, it.name in budgeted)
                },
                expenseCount = snapshot.categories.count { it.type == "EXPENSE" },
                incomeCount = snapshot.categories.count { it.type == "INCOME" }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun createCategory(name: String, type: String, iconKey: String, colorHex: String) {
        if (name.isBlank()) {
            _message.value = "Enter a category name"
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.saveCategory(
                    CategoryEntity(
                        name = name.trim(),
                        type = type,
                        iconKey = iconKey,
                        colorHex = colorHex
                    )
                )
            }.onSuccess {
                _message.value = "Category added"
            }.onFailure {
                _message.value = it.message ?: "Could not add category"
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _message.value = "Category removed"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
