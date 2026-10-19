package com.craysafe.support

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.DiagnosticItem
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class SupportViewModel : ViewModel() {

    private val _items = MutableLiveData<List<DiagnosticItem>>(emptyList())
    val items: LiveData<List<DiagnosticItem>> = _items

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _actionError = MutableLiveData<String?>()
    val actionError: LiveData<String?> = _actionError

    // Full dataset from the API
    private var allItems: List<DiagnosticItem> = emptyList()

    // Current filter + search state
    private var currentCategory: String? = null    // null = "All"
    private var currentSearch: String = ""

    // ─────────────────────────────────────────────
    // Load from API
    // ─────────────────────────────────────────────
    fun loadQnA(sessionManager: SessionManager) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Please login again"
                    _isLoading.value = false
                    return@launch
                }

                val response = ApiClient.apiService.getDiagnosticQnA("Bearer $token")
                _isLoading.value = false

                if (response.success) {
                    allItems = response.data ?: emptyList()
                    applyFilters()
                } else {
                    _error.value = response.message ?: "Failed to load Q&A"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    // ─────────────────────────────────────────────
    // Filter by category
    // ─────────────────────────────────────────────
    fun filterByCategory(category: String?) {
        currentCategory = category
        applyFilters()
    }

    // ─────────────────────────────────────────────
    // Filter by search query
    // ─────────────────────────────────────────────
    fun filterBySearch(query: String) {
        currentSearch = query.trim()
        applyFilters()
    }

    // ─────────────────────────────────────────────
    // Combine category + search + sorting
    // ─────────────────────────────────────────────
    private fun applyFilters() {
        var filtered = allItems

        // 1. Category filter
        if (!currentCategory.isNullOrBlank()) {
            filtered = filtered.filter {
                it.category.equals(currentCategory, ignoreCase = true)
            }
        }

        // 2. Search filter
        if (currentSearch.isNotBlank()) {
            val q = currentSearch.lowercase()
            filtered = filtered.filter { item ->
                item.issue_title.lowercase().contains(q) ||
                        item.solution.lowercase().contains(q) ||
                        item.category.lowercase().contains(q)
            }
        }

        // 3. Apply the "Reddit-style" sorting algorithm
        _items.value = sortByScore(filtered)
    }

    fun submitFeedback(qaId: Int, isHelpful: Boolean, sessionManager: SessionManager) {
        // 1. Find the item in the MASTER list (allItems) to prevent votes from disappearing when filtering
        val itemIndex = allItems.indexOfFirst { it.qa_id == qaId }
        if (itemIndex == -1) return

        val item = allItems[itemIndex]
        val voteAsInt = if (isHelpful) 1 else 0

        // 2. Calculate new vote state (Toggle / Switch)
        val newVote = if (item.user_vote == voteAsInt) null else voteAsInt

        // 3. Calculate count changes
        val helpfulDelta = when {
            item.user_vote == 1 && newVote != 1 -> -1
            item.user_vote != 1 && newVote == 1 -> 1
            else -> 0
        }
        val unhelpfulDelta = when {
            item.user_vote == 0 && newVote != 0 -> -1
            item.user_vote != 0 && newVote == 0 -> 1
            else -> 0
        }

        // 4. Create the updated item
        val updatedItem = item.copy(
            user_vote = newVote,
            helpful_count = (item.helpful_count + helpfulDelta).coerceAtLeast(0),
            unhelpful_count = (item.unhelpful_count + unhelpfulDelta).coerceAtLeast(0)
        )

        // 5. Update the master list with the new item
        allItems = allItems.toMutableList().apply { this[itemIndex] = updatedItem }

        // 6. Re-apply filters and sorting. This triggers the UI update
        // and ListAdapter's DiffUtil will animate the card to its new position!
        applyFilters()

        // 7. Send to backend
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.submitQaFeedback("Bearer $token", qaId, isHelpful)
                if (!response.success) {
                    _actionError.value = response.message ?: "Failed to save feedback"
                }
            } catch (e: Exception) {
                _actionError.value = "Network error: ${e.message}"
            }
        }
    }

    // ─────────────────────────────────────────────
    // Sorting Algorithm: (Likes - Dislikes)
    // ─────────────────────────────────────────────
    private fun sortByScore(items: List<DiagnosticItem>): List<DiagnosticItem> {
        return items.sortedWith(
            compareByDescending<DiagnosticItem> { it.helpful_count - it.unhelpful_count }
                .thenByDescending { it.helpful_count } // Tie-breaker: more likes wins
                .thenBy { it.qa_id } // Final tie-breaker: oldest first
        )
    }
}