package com.fastchat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fastchat.ui.screens.*
import com.fastchat.viewmodels.AuthViewModel
import com.fastchat.viewmodels.ChatViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val startDestination = if (authState is com.fastchat.viewmodels.AuthState.Authenticated) {
        Screen.ChatList.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToChatList = { navController.navigate(Screen.ChatList.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }},
                authViewModel = authViewModel
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToChatList = { navController.navigate(Screen.ChatList.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }},
                authViewModel = authViewModel
            )
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToUserSearch = { navController.navigate(Screen.UserSearch.route) },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ChatList.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel,
                chatViewModel = chatViewModel
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatScreen(
                chatId = chatId,
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel,
                chatViewModel = chatViewModel
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }

        composable(Screen.UserSearch.route) {
            UserSearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId)) {
                        popUpTo(Screen.ChatList.route)
                    }
                },
                authViewModel = authViewModel,
                chatViewModel = chatViewModel
            )
        }
    }
}
