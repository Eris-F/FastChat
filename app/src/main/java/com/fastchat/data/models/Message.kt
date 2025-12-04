package com.fastchat.data.models

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val encryptedContent: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val mediaUrl: String? = null,
    val deleted: Boolean = false,
    val deletedForEveryone: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "chatId" to chatId,
            "senderId" to senderId,
            "senderName" to senderName,
            "encryptedContent" to encryptedContent,
            "timestamp" to timestamp,
            "type" to type.name,
            "status" to status.name,
            "mediaUrl" to mediaUrl,
            "deleted" to deleted,
            "deletedForEveryone" to deletedForEveryone
        )
    }
}

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}
