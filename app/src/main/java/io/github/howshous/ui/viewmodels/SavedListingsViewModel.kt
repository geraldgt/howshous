package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.SavedListingsRepository
import io.github.howshous.data.models.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SavedListingsViewModel : ViewModel() {
    private val savedRepo = SavedListingsRepository()
    private val listingRepo = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadSavedListings(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _listings.value = savedRepo.getSavedListings(userId, listingRepo)
            _isLoading.value = false
        }
    }
}
