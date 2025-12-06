package com.fastchat.data.models

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String?> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap(),
    val isTyping: Map<String, Boolean> = emptyMap(),
    val sharedKey: String = "" // Shared encryption key for this chat
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "participants" to participants,
            "participantNames" to participantNames,
            "participantPhotos" to participantPhotos,
            "lastMessage" to lastMessage,
            "lastMessageTimestamp" to lastMessageTimestamp,
            "lastMessageSenderId" to lastMessageSenderId,
            "unreadCount" to unreadCount,
            "isTyping" to isTyping,
            "sharedKey" to sharedKey
        )
    }

    fun getOtherParticipantId(currentUserId: String): String? {
        return participants.firstOrNull { it != currentUserId }
    }

    fun getOtherParticipantName(currentUserId: String): String {
        val otherUserId = getOtherParticipantId(currentUserId)
        return participantNames[otherUserId] ?: "Unknown"
    }

    fun getOtherParticipantPhoto(currentUserId: String): String? {
        val otherUserId = getOtherParticipantId(currentUserId)
        return participantPhotos[otherUserId]
    }

    fun getUnreadCount(userId: String): Int {
        return unreadCount[userId] ?: 0
    }

    fun isOtherUserTyping(currentUserId: String): Boolean {
        val otherUserId = getOtherParticipantId(currentUserId)
        return isTyping[otherUserId] ?: false
    }
}
