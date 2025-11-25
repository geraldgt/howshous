package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.ChatRepository
import io.github.howshous.data.models.Chat
import io.github.howshous.data.models.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepo = ChatRepository()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentChatId = MutableStateFlow("")
    val currentChatId: StateFlow<String> = _currentChatId

    fun loadChatsForUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val chats = chatRepo.getChatsForUser(userId)
            _chats.value = chats
            _isLoading.value = false
        }
    }

    fun loadMessagesForChat(chatId: String) {
        viewModelScope.launch {
            _currentChatId.value = chatId
            _isLoading.value = true
            val messages = chatRepo.getMessagesForChat(chatId)
            _messages.value = messages
            _isLoading.value = false
        }
    }

    fun sendMessage(chatId: String, senderId: String, text: String) {
        viewModelScope.launch {
            chatRepo.sendMessage(chatId, senderId, text)
            loadMessagesForChat(chatId)
        }
    }
}
