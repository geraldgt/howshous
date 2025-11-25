package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.NotificationRepository
import io.github.howshous.data.models.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val notifRepo = NotificationRepository()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadNotificationsForUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val notifs = notifRepo.getNotificationsForUser(userId)
            _notifications.value = notifs
            _isLoading.value = false
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notifRepo.markNotificationAsRead(notificationId)
            loadNotificationsForUser(_notifications.value.firstOrNull()?.userId ?: "")
        }
    }
}
