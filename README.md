# FastChat - Encrypted Kotlin Chat App

A sleek, barebones encrypted chat application built with Kotlin and Jetpack Compose.

## Features

✅ End-to-End Encryption (AES-256-GCM)
✅ Firebase Authentication (Email + Google Sign-In)
✅ Real-time Messaging
✅ Image Sharing (Encrypted)
✅ Voice Messages
✅ Message Status Indicators (Sent/Delivered/Read)
✅ Typing Indicators
✅ Last Seen & Online Status
✅ Push Notifications (FCM)
✅ Dark Mode
✅ Message Deletion
✅ Material Design 3

## Setup

1. **Firebase Setup:**
   - Create a new Firebase project at https://console.firebase.google.com/
   - Add an Android app with package name `com.fastchat`
   - Download `google-services.json` and replace the template file in `app/google-services.json`
   - Enable Authentication (Email/Password + Google)
   - Create Firestore Database
   - Set up Firebase Storage
   - Enable Cloud Messaging (FCM)

2. **Firestore Security Rules:**
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read: if request.auth != null;
         allow write: if request.auth != null && request.auth.uid == userId;
       }
       match /chats/{chatId} {
         allow read, write: if request.auth != null &&
           request.auth.uid in resource.data.participants;
       }
       match /messages/{messageId} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

3. **Storage Security Rules:**
   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /chat_media/{userId}/{allPaths=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

4. **Build and Run:**
   ```bash
   ./gradlew assembleDebug
   ```

## Architecture

- **MVVM Pattern** with Jetpack Compose
- **Repository Pattern** for data access
- **Coroutines & Flow** for async operations
- **Firebase** for backend services
- **AES-256-GCM** for end-to-end encryption

## Project Structure

```
app/src/main/java/com/fastchat/
├── data/
│   ├── models/          # Data classes
│   └── repository/      # Firebase repositories
├── ui/
│   ├── screens/         # Compose screens
│   ├── components/      # Reusable UI components
│   ├── theme/           # Material 3 theme
│   └── navigation/      # Navigation graph
├── utils/               # Encryption & utilities
├── viewmodels/          # ViewModels
└── MainActivity.kt
```

## Security Note

All messages, images, and voice recordings are encrypted using AES-256-GCM before being stored in Firebase. Encryption keys are derived from user credentials and never stored in plaintext.
