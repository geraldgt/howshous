package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.howshous.data.firestore.ChatRepository
import io.github.howshous.data.firestore.ContractRepository
import io.github.howshous.data.models.Chat
import io.github.howshous.data.models.Contract
import io.github.howshous.data.models.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepo = ChatRepository()
    private val contractRepo = ContractRepository()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat

    private val _contracts = MutableStateFlow<List<Contract>>(emptyList())
    val contracts: StateFlow<List<Contract>> = _contracts

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

    fun loadChat(chatId: String) {
        viewModelScope.launch {
            val chat = chatRepo.getChat(chatId)
            _currentChat.value = chat
        }
    }

    fun loadMessagesForChat(chatId: String, userId: String) {
        viewModelScope.launch {
            _currentChatId.value = chatId
            _isLoading.value = true
            val messages = chatRepo.getMessagesForChat(chatId)
            _messages.value = messages
            loadChat(chatId)
            loadContractsForChat(chatId, userId)
            _isLoading.value = false
        }
    }

    fun sendMessage(chatId: String, senderId: String, text: String) {
        viewModelScope.launch {
            chatRepo.sendMessage(chatId, senderId, text)
            loadMessagesForChat(chatId, senderId)
        }
    }

    fun loadContractsForChat(chatId: String, userId: String) {
        viewModelScope.launch {
            val contracts = contractRepo.getContractsForChat(chatId, userId)
            _contracts.value = contracts
        }
    }

    suspend fun sendContract(
        chatId: String,
        listingId: String,
        landlordId: String,
        tenantId: String,
        monthlyRent: Int,
        deposit: Int
    ): String {
        if (landlordId.isBlank() || chatId.isBlank() || listingId.isBlank()) return ""
        var resolvedTenantId = tenantId
        if (resolvedTenantId.isBlank()) {
            val chat = chatRepo.getChat(chatId)
            resolvedTenantId = chat?.tenantId ?: ""
        }
        if (resolvedTenantId.isBlank()) return ""
        val fallbackTerms = generatePlaceholderContract(monthlyRent, deposit)
        val contractId = contractRepo.createContractFromListingTemplate(
            listingId = listingId,
            chatId = chatId,
            landlordId = landlordId,
            tenantId = resolvedTenantId,
            fallbackTitle = "Rental Agreement",
            fallbackTerms = fallbackTerms,
            fallbackMonthlyRent = monthlyRent,
            fallbackDeposit = deposit
        )
        if (contractId.isNotEmpty()) {
            // Send contract as a message
            chatRepo.sendMessage(chatId, landlordId, "Contract sent. Please review and sign.")
            loadMessagesForChat(chatId, landlordId)
        }
        return contractId
    }

    fun signContract(contractId: String) {
        viewModelScope.launch {
            val success = contractRepo.signContract(contractId)
            if (success) {
                val contract = contractRepo.getContract(contractId)
                contract?.let {
                    loadContractsForChat(it.chatId, it.tenantId)
                    // Send confirmation message
                    chatRepo.sendMessage(it.chatId, it.tenantId, "Contract signed!")
                    loadMessagesForChat(it.chatId, it.tenantId)
                }
            }
        }
    }

    private fun generatePlaceholderContract(monthlyRent: Int, deposit: Int): String {
        return """
            RENTAL AGREEMENT FOR BOARDING HOUSE

            TERMS AND CONDITIONS:

            1. RENTAL AMOUNT: PHP $monthlyRent per month
            2. SECURITY DEPOSIT: PHP $deposit (refundable upon termination)
            3. PAYMENT TERMS: Monthly rent due on the 1st of each month
            4. DURATION: 12 months (renewable)
            5. UTILITIES: Included in rent (subject to fair usage policy)
            6. HOUSE RULES:
               - No smoking inside the premises
               - Quiet hours: 10 PM - 6 AM
               - Keep common areas clean
               - No pets without prior approval
            7. TERMINATION: 30 days written notice required
            8. DAMAGES: Tenant responsible for any damages beyond normal wear

            By signing this contract, both parties agree to the terms stated above.
        """.trimIndent()
    }
}
