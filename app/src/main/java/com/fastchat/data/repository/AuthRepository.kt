package com.fastchat.data.repository

import com.fastchat.data.models.User
import com.fastchat.utils.EncryptionUtils
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                firestore.collection("users")
                    .document(auth.currentUser!!.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val user = document.toObject(User::class.java)
                            trySend(user)
                        } else {
                            trySend(null)
                        }
                    }
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User ID not found")

            val userDoc = firestore.collection("users").document(uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

            updateOnlineStatus(uid, true)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, username: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User ID not found")

            val encryptionKey = EncryptionUtils.generateKey()
            val keyString = EncryptionUtils.keyToString(encryptionKey)

            val user = User(
                uid = uid,
                username = username,
                email = email,
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                encryptionKey = keyString
            )

            firestore.collection("users").document(uid).set(user.toMap()).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: throw Exception("User ID not found")

            val userDoc = firestore.collection("users").document(uid).get().await()

            val user = if (userDoc.exists()) {
                userDoc.toObject(User::class.java)!!
            } else {
                val encryptionKey = EncryptionUtils.generateKey()
                val keyString = EncryptionUtils.keyToString(encryptionKey)

                User(
                    uid = uid,
                    username = account.displayName ?: "User",
                    email = account.email ?: "",
                    photoUrl = account.photoUrl?.toString(),
                    isOnline = true,
                    encryptionKey = keyString
                )
            }

            firestore.collection("users").document(uid).set(user.toMap()).await()
            updateOnlineStatus(uid, true)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            updateOnlineStatus(uid, false)
        }
        auth.signOut()
    }

    suspend fun updateOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        try {
            firestore.collection("users").document(uid)
                .update("fcmToken", token).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
