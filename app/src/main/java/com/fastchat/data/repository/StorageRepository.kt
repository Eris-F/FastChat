package com.fastchat.data.repository

import android.net.Uri
import com.fastchat.utils.EncryptionUtils
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.crypto.SecretKey

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadEncryptedImage(
        imageBytes: ByteArray,
        userId: String,
        key: SecretKey
    ): Result<String> {
        return try {
            val encryptedBytes = EncryptionUtils.encryptBytes(imageBytes, key)
            val fileName = "chat_media/$userId/${UUID.randomUUID()}.enc"
            val ref = storage.reference.child(fileName)

            val inputStream = ByteArrayInputStream(encryptedBytes)
            ref.putStream(inputStream).await()

            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadEncryptedVoice(
        audioBytes: ByteArray,
        userId: String,
        key: SecretKey
    ): Result<String> {
        return try {
            val encryptedBytes = EncryptionUtils.encryptBytes(audioBytes, key)
            val fileName = "chat_media/$userId/${UUID.randomUUID()}.enc"
            val ref = storage.reference.child(fileName)

            val inputStream = ByteArrayInputStream(encryptedBytes)
            ref.putStream(inputStream).await()

            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndDecryptMedia(url: String, key: SecretKey): Result<ByteArray> {
        return try {
            val ref = storage.getReferenceFromUrl(url)
            val maxDownloadSize = 10L * 1024 * 1024 // 10MB
            val encryptedBytes = ref.getBytes(maxDownloadSize).await()

            val decryptedBytes = EncryptionUtils.decryptBytes(encryptedBytes, key)
            Result.success(decryptedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePicture(imageUri: Uri, userId: String): Result<String> {
        return try {
            val fileName = "profile_pictures/$userId.jpg"
            val ref = storage.reference.child(fileName)

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
