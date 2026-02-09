package io.github.howshous.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import io.github.howshous.data.firestore.AIChatRepository
import io.github.howshous.data.firestore.ListingMetricsRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.util.GroqApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Landlord Insights: tries the backend gateway first (when project is on Blaze).
 * If the gateway is not available (e.g. project not on Blaze), falls back to client-side AI
 * so the feature still works without paying for Cloud Functions.
 */
class LandlordAnalyticsAIViewModel : ViewModel() {

    private val aiChatRepository = AIChatRepository()
    private val listingRepository = ListingRepository()
    private val metricsRepository = ListingMetricsRepository()

    private val _messages = MutableStateFlow<List<TenantAIMessage>>(emptyList())
    val messages: StateFlow<List<TenantAIMessage>> = _messages

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    private var currentUserId: String = ""

    private val landlordChatKey = "landlord_analytics"

    fun initializeChat(userId: String) {
        if (currentUserId == userId && _messages.value.isNotEmpty()) return
        currentUserId = userId
        viewModelScope.launch {
            aiChatRepository.initializeLandlordAnalyticsWelcome(userId)
            loadChatHistory(userId)
        }
    }

    private suspend fun loadChatHistory(userId: String) {
        val saved = aiChatRepository.loadMessages(userId, landlordChatKey)
        _messages.value = saved.mapIndexed { index, aiMsg ->
            TenantAIMessage(
                id = aiMsg.timestamp?.toDate()?.time ?: (System.currentTimeMillis() + index),
                author = if (aiMsg.isTenant) MessageAuthor.TENANT else MessageAuthor.AI,
                text = aiMsg.text
            )
        }
    }

    /**
     * Call AI via backend gateway only. App never calls AI directly or sends API keys.
     * @param refresh true to force regeneration (ignore cache).
     */
    private companion object {
        const val GATEWAY_TIMEOUT_MS = 12_000L   // fail fast when Functions not deployed
        const val CACHE_FALLBACK_TIMEOUT_MS = 3_000L
    }

    private suspend fun callLandlordAnalyticsGateway(message: String, refresh: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(GATEWAY_TIMEOUT_MS) {
            try {
                val result = FirebaseFunctions.getInstance()
                    .getHttpsCallable("landlordAnalyticsAiGateway")
                    .call(hashMapOf("message" to message, "refresh" to refresh))
                    .await()
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any?> ?: return@withTimeoutOrNull Result.failure(IllegalStateException("No response"))
                val reply = data["reply"] as? String ?: return@withTimeoutOrNull Result.failure(IllegalStateException("No reply"))
                Result.success(reply)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } ?: Result.failure(IllegalStateException("Gateway timeout"))
    }

    /**
     * Build analytics context in the app (used when gateway is unavailable, e.g. not on Blaze).
     */
    private suspend fun buildAnalyticsContextJson(): String = withContext(Dispatchers.IO) {
        if (currentUserId.isBlank()) return@withContext "{}"
        val listings = listingRepository.getListingsForLandlord(currentUserId)
        if (listings.isEmpty()) return@withContext """{"listings":[],"summary":{"totalViews30d":0,"totalSaves30d":0,"totalMessages30d":0}}"""
        val metricsMap = metricsRepository.getMetricsForListings(listings.map { it.id })
        val totalViews30d = metricsMap.values.sumOf { it.views30d }
        val totalSaves30d = metricsMap.values.sumOf { it.saves30d }
        val totalMessages30d = metricsMap.values.sumOf { it.messages30d }
        val avgSave = if (totalViews30d > 0) (totalSaves30d.toFloat() / totalViews30d * 100) else 0f
        val avgMsg = if (totalViews30d > 0) (totalMessages30d.toFloat() / totalViews30d * 100) else 0f
        val saveToMsg = if (totalSaves30d > 0) (totalMessages30d.toFloat() / totalSaves30d * 100) else 0f
        val listingsArray = JSONArray()
        listings.forEach { listing ->
            val m = metricsMap[listing.id] ?: return@forEach
            val v = m.views30d
            val saveRate = if (v > 0) (m.saves30d.toFloat() / v * 100) else 0f
            val msgRate = if (v > 0) (m.messages30d.toFloat() / v * 100) else 0f
            val s2m = if (m.saves30d > 0) (m.messages30d.toFloat() / m.saves30d * 100) else 0f
            listingsArray.put(JSONObject().apply {
                put("listingId", listing.id)
                put("title", listing.title)
                put("price", listing.price)
                put("views7d", m.views7d); put("views30d", v)
                put("saves7d", m.saves7d); put("saves30d", m.saves30d)
                put("messages7d", m.messages7d); put("messages30d", m.messages30d)
                put("saveRatePct", "%.1f".format(saveRate))
                put("messageRatePct", "%.1f".format(msgRate))
                put("savesToMessagesPct", "%.1f".format(s2m))
            })
        }
        JSONObject().apply {
            put("summary", JSONObject().apply {
                put("totalViews30d", totalViews30d)
                put("totalSaves30d", totalSaves30d)
                put("totalMessages30d", totalMessages30d)
                put("avgSaveRatePct", "%.1f".format(avgSave))
                put("avgMessageRatePct", "%.1f".format(avgMsg))
                put("saveToMessageRatePct", "%.1f".format(saveToMsg))
            })
            put("listings", listingsArray)
        }.toString()
    }

    /**
     * Fetch last cached insight (for fallback when gateway fails). Never blocks the rest of the app.
     */
    suspend fun getCachedInsightFallback(): String? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(CACHE_FALLBACK_TIMEOUT_MS) {
            try {
                val result = FirebaseFunctions.getInstance()
                    .getHttpsCallable("getCachedLandlordInsight")
                    .call()
                    .await()
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any?> ?: return@withTimeoutOrNull null
                data["reply"] as? String
            } catch (_: Exception) {
                null
            }
        }
    }

    fun sendMessage(prompt: String, forceRefresh: Boolean = false) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isThinking.value || currentUserId.isEmpty()) return

        val userMessage = TenantAIMessage(
            id = System.currentTimeMillis(),
            author = MessageAuthor.TENANT,
            text = trimmed
        )
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            aiChatRepository.saveMessage(currentUserId, trimmed, isTenant = true, chatKey = landlordChatKey)
        }

        viewModelScope.launch {
            _isThinking.value = true
            val result = callLandlordAnalyticsGateway(trimmed, forceRefresh)
            val aiMessageText = result.fold(
                onSuccess = { it },
                onFailure = { e ->
                    val cached = getCachedInsightFallback()
                    if (cached != null) {
                        "**Insights temporarily unavailable for new questions.**\n\nShowing last saved insight:\n\n$cached"
                    } else {
                        val msg = e.message?.lowercase() ?: ""
                        when {
                            msg.contains("limit reached") || msg.contains("resource-exhausted") -> "Daily insight limit reached. Try again tomorrow."
                            msg.contains("unauthenticated") || msg.contains("permission") -> "Please sign in again and try again."
                            else -> {
                                val ctx = buildAnalyticsContextJson()
                                val clientReply = GroqApiClient.fetchLandlordAnalyticsReply(trimmed, ctx).getOrNull()
                                clientReply ?: when {
                                    msg.contains("temporarily unavailable") || msg.contains("failed-precondition") || msg.contains("internal") || msg.contains("underlying tasks failed") -> "Insights temporarily unavailable. Check your raw analytics on the Performance tab—your data is still there."
                                    else -> "Insights temporarily unavailable. Try again later or use the Performance tab for your raw analytics."
                                }
                            }
                        }
                    }
                }
            )
            _messages.value = _messages.value + TenantAIMessage(
                id = System.currentTimeMillis(),
                author = MessageAuthor.AI,
                text = aiMessageText
            )
            _isThinking.value = false
            aiChatRepository.saveMessage(
                currentUserId,
                aiMessageText,
                isTenant = false,
                chatKey = landlordChatKey
            )
        }
    }

    fun clearChatHistory() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            aiChatRepository.deleteChatHistory(currentUserId, landlordChatKey)
            _messages.value = emptyList()
            aiChatRepository.initializeLandlordAnalyticsWelcome(currentUserId)
            loadChatHistory(currentUserId)
        }
    }
}
