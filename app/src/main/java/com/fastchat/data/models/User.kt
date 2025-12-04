package com.fastchat.data.models

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val encryptionKey: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "email" to email,
            "photoUrl" to photoUrl,
            "isOnline" to isOnline,
            "lastSeen" to lastSeen,
            "fcmToken" to fcmToken,
            "encryptionKey" to encryptionKey
        )
    }
}
