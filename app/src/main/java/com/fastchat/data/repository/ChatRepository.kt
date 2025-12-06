package com.fastchat.data.repository

import com.fastchat.data.models.Chat
import com.fastchat.data.models.Message
import com.fastchat.data.models.MessageStatus
import com.fastchat.data.models.MessageType
import com.fastchat.data.models.User
import com.fastchat.utils.EncryptionUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getChatsForUser(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    fun getMessagesForChat(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): Result<String> {
        return try {
            val existingChats = firestore.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            val existingChat = existingChats.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(otherUserId) == true
            }

            if (existingChat != null) {
                return Result.success(existingChat.id)
            }

            val currentUser = firestore.collection("users").document(currentUserId).get().await()
                .toObject(User::class.java) ?: throw Exception("Current user not found")

            val otherUser = firestore.collection("users").document(otherUserId).get().await()
                .toObject(User::class.java) ?: throw Exception("Other user not found")

            val chatId = UUID.randomUUID().toString()

            // Generate shared encryption key for this chat
            val sharedKey = EncryptionUtils.generateKey()
            val sharedKeyString = EncryptionUtils.keyToString(sharedKey)

            val chat = Chat(
                id = chatId,
                participants = listOf(currentUserId, otherUserId),
                participantNames = mapOf(
                    currentUserId to currentUser.username,
                    otherUserId to otherUser.username
                ),
                participantPhotos = mapOf(
                    currentUserId to currentUser.photoUrl,
                    otherUserId to otherUser.photoUrl
                ),
                unreadCount = mapOf(currentUserId to 0, otherUserId to 0),
                isTyping = mapOf(currentUserId to false, otherUserId to false),
                sharedKey = sharedKeyString
            )

            firestore.collection("chats").document(chatId).set(chat.toMap()).await()
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        type: MessageType,
        sharedKey: String,
        mediaUrl: String? = null
    ): Result<Unit> {
        return try {
            val key = EncryptionUtils.stringToKey(sharedKey)
            val encryptedContent = EncryptionUtils.encrypt(content, key)

            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                encryptedContent = encryptedContent,
                timestamp = System.currentTimeMillis(),
                type = type,
                status = MessageStatus.SENT,
                mediaUrl = mediaUrl
            )

            firestore.collection("messages").document(messageId).set(message.toMap()).await()

            val chatRef = firestore.collection("chats").document(chatId)
            val chatDoc = chatRef.get().await()
            val chat = chatDoc.toObject(Chat::class.java) ?: throw Exception("Chat not found")

            val updates = mapOf(
                "lastMessage" to (if (type == MessageType.TEXT) content else "${type.name} message"),
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to senderId,
                "unreadCount" to chat.unreadCount.mapValues { (userId, count) ->
                    if (userId == senderId) 0 else count + 1
                }
            )

            chatRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String) {
        try {
            val chatRef = firestore.collection("chats").document(chatId)
            val chatDoc = chatRef.get().await()
            val chat = chatDoc.toObject(Chat::class.java) ?: return

            val updatedUnreadCount = chat.unreadCount.toMutableMap()
            updatedUnreadCount[userId] = 0

            chatRef.update("unreadCount", updatedUnreadCount).await()

            firestore.collection("messages")
                .whereEqualTo("chatId", chatId)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()
                .documents.forEach { doc ->
                    doc.reference.update("status", MessageStatus.READ.name)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        try {
            val chatRef = firestore.collection("chats").document(chatId)
            val chatDoc = chatRef.get().await()
            val chat = chatDoc.toObject(Chat::class.java) ?: return

            val updatedTyping = chat.isTyping.toMutableMap()
            updatedTyping[userId] = isTyping

            chatRef.update("isTyping", updatedTyping).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMessage(messageId: String, forEveryone: Boolean) {
        try {
            val updates = if (forEveryone) {
                mapOf(
                    "deletedForEveryone" to true,
                    "encryptedContent" to "",
                    "mediaUrl" to null
                )
            } else {
                mapOf("deleted" to true)
            }

            firestore.collection("messages").document(messageId)
                .update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchUsers(query: String, currentUserId: String): Result<List<User>> {
        return try {
            val users = firestore.collection("users")
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
                .filter { it.uid != currentUserId }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val user = firestore.collection("users")
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java) ?: throw Exception("User not found")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
