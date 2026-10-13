package com.example.amazondeals.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.amazondeals.data.model.Deal
import com.example.amazondeals.data.repository.DealRepository
import com.example.amazondeals.notification.NotificationHelper
import com.example.amazondeals.worker.DealCheckWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DealUiState(
    val deals: List<Deal> = emptyList(),
    val filteredDeals: List<Deal> = emptyList(),
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val selectedFilter: String = "Alle",
    val lastUpdated: Long = 0L,
    val backendUrl: String = "",
    val keywords: List<String> = emptyList(),
    val errorMessage: String? = null
)

class DealViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DealRepository(application)
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(DealUiState())
    val uiState: StateFlow<DealUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        refreshDeals()
        DealCheckWorker.schedule(application)
    }

    fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            backendUrl = repository.getBackendUrl(),
            keywords = repository.getKeywords()
        )
    }

    fun updateBackendUrl(url: String) {
        repository.setBackendUrl(url)
        _uiState.value = _uiState.value.copy(backendUrl = url)
        refreshDeals()
    }

    fun updateKeywords(keywords: List<String>) {
        repository.setKeywords(keywords)
        _uiState.value = _uiState.value.copy(keywords = keywords)
        applyFiltersAndSearch(_uiState.value.selectedFilter, _uiState.value.searchQuery)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFiltersAndSearch(_uiState.value.selectedFilter, query)
    }

    fun toggleFavorite(dealId: String) {
        repository.toggleFavorite(dealId)
        val newFavs = _uiState.value.deals.map { it.id }.filter { repository.isFavorite(it) }.toSet()
        _uiState.value = _uiState.value.copy(favorites = newFavs)
        applyFiltersAndSearch(_uiState.value.selectedFilter, _uiState.value.searchQuery)
    }

    fun isFavorite(dealId: String): Boolean {
        return repository.isFavorite(dealId)
    }

    fun refreshDeals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val deals = repository.fetchDeals()
                val favs = deals.map { it.id }.filter { repository.isFavorite(it) }.toSet()
                _uiState.value = _uiState.value.copy(
                    deals = deals,
                    favorites = favs,
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis()
                )
                applyFiltersAndSearch(_uiState.value.selectedFilter, _uiState.value.searchQuery)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Fehler beim Laden der Angebote: ${e.localizedMessage}"
                )
            }
        }
    }

    fun applyFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyFiltersAndSearch(filter, _uiState.value.searchQuery)
    }

    private fun applyFiltersAndSearch(filter: String, query: String) {
        val allDeals = _uiState.value.deals
        var result = when (filter) {
            "0.01€ / 1ct" -> allDeals.filter { it.isGlitch || it.price.contains("0.01") || it.couponText.contains("0.01") || it.couponText.contains("1ct") }
            "50% Rabatt" -> allDeals.filter { it.couponText.contains("50%") || it.description.contains("50%") }
            "Glitch Deals" -> allDeals.filter { it.isGlitch }
            "Favoriten" -> allDeals.filter { repository.isFavorite(it.id) }
            else -> allDeals
        }

        if (query.trim().isNotEmpty()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.couponText.lowercase().contains(q) ||
                it.price.lowercase().contains(q)
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredDeals = result
        )
    }

    fun sendTestNotification() {
        val testDeal = Deal(
            id = "test_${System.currentTimeMillis()}",
            title = "TEST: Gillette Mach3 8er (Sparen 2 für 0.01€)",
            description = "Test-Alarm für 0.01€ Amazon Gutschein-Glitches.",
            price = "0.01 €",
            couponText = "Sparen: Erhalte 2 für 0.01€",
            dealUrl = "https://www.amazon.de/dp/B08N5WRWNW",
            isGlitch = true
        )
        notificationHelper.sendDealNotification(testDeal)
    }
}
