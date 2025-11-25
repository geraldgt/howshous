package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.UserRepository
import io.github.howshous.data.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {
    private val userRepo = UserRepository()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val profile = userRepo.getUserProfile(uid)
            _userProfile.value = profile
            _isLoading.value = false
        }
    }

    fun updateProfile(uid: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            userRepo.updateUserProfile(uid, updates)
            loadUserProfile(uid)
        }
    }
}
