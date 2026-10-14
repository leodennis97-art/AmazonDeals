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
    val errorMessage: String? = null,
    val isLicenseActive: Boolean = false,
    val isVerifyingLicense: Boolean = false,
    val savedEmail: String = "",
    val savedLicenseKey: String = "",
    val licenseErrorMessage: String? = null,
    val totalSavingsEuros: Double = 0.0
)

class DealViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DealRepository(application)
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(DealUiState())
    val uiState: StateFlow<DealUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkLicenseState()
        refreshDeals()
        DealCheckWorker.schedule(application)
    }

    fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            backendUrl = repository.getBackendUrl(),
            keywords = repository.getKeywords(),
            savedEmail = repository.getSavedEmail(),
            savedLicenseKey = repository.getSavedLicenseKey(),
            isLicenseActive = repository.isLicenseActive()
        )
    }

    fun checkLicenseState() {
        _uiState.value = _uiState.value.copy(
            isLicenseActive = repository.isLicenseActive()
        )
    }

    fun verifyLicense(email: String, key: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isVerifyingLicense = true,
                licenseErrorMessage = null
            )
            val (success, message) = repository.verifyLicenseWithCloud(email, key)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isVerifyingLicense = false,
                    isLicenseActive = true,
                    savedEmail = email,
                    savedLicenseKey = key,
                    licenseErrorMessage = null
                )
                refreshDeals()
            } else {
                _uiState.value = _uiState.value.copy(
                    isVerifyingLicense = false,
                    isLicenseActive = false,
                    licenseErrorMessage = message
                )
            }
        }
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
        val allDeals = _uiState.value.deals.sortedWith(
            compareByDescending<Deal> { it.getTradingProfitMarginEuros() }
                .thenByDescending { it.discountPercent }
                .thenByDescending { it.isGlitch }
                .thenByDescending { it.timestamp }
        )
        var result = when (filter) {
            "Mega-Filter (>=95%)" -> allDeals.filter { it.isMegaGlitch() }
            "💥 Kaufe 2/3 zahle 1ct" -> allDeals.filter { it.discountPercent >= 199 }
            "🏷️ 50% Rabatt" -> allDeals.filter { it.discountPercent == 50 || it.couponText.contains("50%") }
            "95%-100% Preisfehler" -> allDeals.filter { it.discountPercent >= 95 || it.isGlitch }
            "75%-199% Rabatt" -> allDeals.filter { it.discountPercent >= 75 || it.isGlitch }
            "99% / Gratis" -> allDeals.filter { it.discountPercent >= 90 || it.isGlitch || it.price.contains("0.00") || it.price.contains("0.01") }
            "0.01€ / 1ct" -> allDeals.filter { it.isGlitch || it.price.contains("0.01") || it.couponText.contains("0.01") || it.couponText.contains("1ct") }
            "Glitch Deals" -> allDeals.filter { it.isGlitch }
            "Drogerie Bot" -> allDeals.filter { it.title.contains("Drogerie") || it.description.lowercase().contains("gillette") || it.description.lowercase().contains("persil") || it.description.lowercase().contains("finish") || it.title.lowercase().contains("rasier") }
            "Elektronik Bot" -> allDeals.filter { it.title.contains("Elektronik") || it.description.lowercase().contains("anker") || it.description.lowercase().contains("usb") || it.description.lowercase().contains("led") || it.title.lowercase().contains("charger") }
            "Haushalt Bot" -> allDeals.filter { it.title.contains("Haushalt") || it.description.lowercase().contains("kaffee") || it.description.lowercase().contains("küche") || it.description.lowercase().contains("tabs") || it.title.lowercase().contains("finish") }
            "Gaming Bot" -> allDeals.filter { it.title.contains("Gaming") || it.description.lowercase().contains("ps5") || it.description.lowercase().contains("xbox") || it.description.lowercase().contains("switch") || it.title.lowercase().contains("controller") }
            "Fashion Bot" -> allDeals.filter { it.title.contains("Fashion") || it.description.lowercase().contains("nike") || it.description.lowercase().contains("adidas") || it.description.lowercase().contains("schuhe") || it.title.lowercase().contains("t-shirt") }
            "Tierwelt Bot" -> allDeals.filter { it.title.contains("Tierwelt") || it.description.lowercase().contains("katze") || it.description.lowercase().contains("hund") || it.description.lowercase().contains("felix") || it.title.lowercase().contains("futter") }
            "Baumarkt Bot" -> allDeals.filter { it.title.contains("Baumarkt") || it.description.lowercase().contains("bosch") || it.description.lowercase().contains("makita") || it.description.lowercase().contains("werkzeug") || it.title.lowercase().contains("akku") }
            "Favoriten" -> allDeals.filter { repository.isFavorite(it.id) }
            else -> allDeals
        }

        if (result.isEmpty() && filter != "Favoriten" && query.isEmpty()) {
            result = allDeals
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

        // Dynamically re-sort the filtered category deals so top 3 items of THIS category are always at the top!
        result = result.sortedWith(
            compareByDescending<Deal> { it.discountPercent }
                .thenByDescending { it.isGlitch }
                .thenByDescending { it.timestamp }
        )

        val totalSavings = result.sumOf { deal ->
            val orig = deal.originalPrice.replace("€", "").trim().toDoubleOrNull() ?: 29.99
            val cur = deal.price.replace("€", "").replace("(Gratis)", "").replace("(Zu verschenken)", "").trim().toDoubleOrNull() ?: 0.01
            (orig - cur).coerceAtLeast(0.0)
        }

        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredDeals = result,
            totalSavingsEuros = totalSavings
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
