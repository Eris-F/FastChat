package com.fastchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.rememberNavController
import com.fastchat.ui.navigation.AppNavigation
import com.fastchat.ui.theme.FastChatTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val ComponentActivity.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                // Token will be updated in AuthViewModel
            }
        }

        setContent {
            val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
            val darkModeFlow = dataStore.data.map { preferences ->
                preferences[DARK_MODE_KEY] ?: isSystemInDarkTheme()
            }
            val isDarkMode by darkModeFlow.collectAsState(initial = isSystemInDarkTheme())

            FastChatTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
