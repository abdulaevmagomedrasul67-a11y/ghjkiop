package com.example.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkType
import com.example.domain.repository.WorkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val works: List<WorkDocument> = emptyList(),
    val selectedFilter: WorkType? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel для управления историей созданных студенческих работ.
 */
class HistoryViewModel(
    private val workRepository: WorkRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<WorkType?>(null)
    val selectedFilter: StateFlow<WorkType?> = _selectedFilter.asStateFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        workRepository.getAllWorks(),
        _selectedFilter
    ) { allWorks, filter ->
        val filtered = if (filter == null) {
            allWorks
        } else {
            allWorks.filter { it.meta.type == filter }
        }
        HistoryUiState(works = filtered, selectedFilter = filter, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun onFilterSelected(type: WorkType?) {
        _selectedFilter.value = type
    }

    fun deleteWork(id: Long) {
        viewModelScope.launch {
            workRepository.deleteWork(id)
        }
    }

    class Factory(
        private val workRepository: WorkRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(workRepository) as T
        }
    }
}
