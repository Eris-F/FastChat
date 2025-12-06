package com.fastchat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastchat.data.models.Chat
import com.fastchat.data.models.Message
import com.fastchat.data.models.MessageType
import com.fastchat.data.models.User
import com.fastchat.data.repository.ChatRepository
import com.fastchat.data.repository.StorageRepository
import com.fastchat.utils.EncryptionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var typingJob: Job? = null

    fun loadChatsForUser(userId: String) {
        viewModelScope.launch {
            chatRepository.getChatsForUser(userId).collect { chatList ->
                _chats.value = chatList
            }
        }
    }

    fun loadMessagesForChat(chatId: String, sharedKey: String) {
        viewModelScope.launch {
            chatRepository.getMessagesForChat(chatId).collect { messageList ->
                val key = EncryptionUtils.stringToKey(sharedKey)
                _messages.value = messageList.map { message ->
                    try {
                        val decryptedContent = if (!message.deletedForEveryone && message.encryptedContent.isNotEmpty()) {
                            EncryptionUtils.decrypt(message.encryptedContent, key)
                        } else {
                            ""
                        }
                        message.copy(encryptedContent = decryptedContent)
                    } catch (e: Exception) {
                        message.copy(encryptedContent = "[Failed to decrypt]")
                    }
                }
            }
        }
    }

    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): String? {
        val result = chatRepository.getOrCreateChat(currentUserId, otherUserId)
        return result.getOrNull()
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        sharedKey: String,
        type: MessageType = MessageType.TEXT
    ) {
        viewModelScope.launch {
            chatRepository.sendMessage(chatId, senderId, senderName, content, type, sharedKey)
        }
    }

    fun sendImageMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        imageBytes: ByteArray,
        sharedKey: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val key = EncryptionUtils.stringToKey(sharedKey)
            val uploadResult = storageRepository.uploadEncryptedImage(imageBytes, senderId, key)

            if (uploadResult.isSuccess) {
                val mediaUrl = uploadResult.getOrNull()!!
                chatRepository.sendMessage(
                    chatId, senderId, senderName, "Image", MessageType.IMAGE, sharedKey, mediaUrl
                )
            }
            _isLoading.value = false
        }
    }

    fun sendVoiceMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        audioBytes: ByteArray,
        sharedKey: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val key = EncryptionUtils.stringToKey(sharedKey)
            val uploadResult = storageRepository.uploadEncryptedVoice(audioBytes, senderId, key)

            if (uploadResult.isSuccess) {
                val mediaUrl = uploadResult.getOrNull()!!
                chatRepository.sendMessage(
                    chatId, senderId, senderName, "Voice message", MessageType.VOICE, sharedKey, mediaUrl
                )
            }
            _isLoading.value = false
        }
    }

    suspend fun downloadAndDecryptMedia(url: String, sharedKey: String): ByteArray? {
        val key = EncryptionUtils.stringToKey(sharedKey)
        val result = storageRepository.downloadAndDecryptMedia(url, key)
        return result.getOrNull()
    }

    fun markMessagesAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatId, userId)
        }
    }

    fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            chatRepository.setTypingStatus(chatId, userId, isTyping)
            if (isTyping) {
                delay(3000)
                chatRepository.setTypingStatus(chatId, userId, false)
            }
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId, forEveryone)
        }
    }

    fun searchUsers(query: String, currentUserId: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            val result = chatRepository.searchUsers(query, currentUserId)
            _searchResults.value = result.getOrNull() ?: emptyList()
        }
    }

    suspend fun getUser(userId: String): User? {
        val result = chatRepository.getUser(userId)
        return result.getOrNull()
    }

    fun setCurrentChat(chat: Chat?) {
        _currentChat.value = chat
    }
}
