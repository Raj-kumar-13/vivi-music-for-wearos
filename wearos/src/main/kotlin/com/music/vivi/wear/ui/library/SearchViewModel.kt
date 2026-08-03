package com.music.vivi.wear.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.wear.data.WearYouTubeRepository
import com.music.vivi.wear.data.models.WearSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeRepository: WearYouTubeRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<WearSong>>(emptyList())
    val searchResults: StateFlow<List<WearSong>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _error.value = null
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _searchResults.value = emptyList()

            try {
                Timber.d("Searching for: $query")
                val result = youTubeRepository.search(query)

                if (result.isSuccess) {
                    _searchResults.value = result.getOrNull() ?: emptyList()
                    Timber.d("Found ${_searchResults.value.size} results")
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = exception?.message ?: "Search failed"
                    Timber.e(exception, "Search failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Search failed")
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearResults() {
        _searchResults.value = emptyList()
        _error.value = null
    }
}