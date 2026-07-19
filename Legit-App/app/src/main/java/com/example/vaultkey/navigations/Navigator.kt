package com.example.vaultkey.navigations

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.vaultkey.data.AppController
import com.example.vaultkey.screens.CameraScreen
import com.example.vaultkey.screens.ChatScreen
import com.example.vaultkey.screens.HomeScreen
import com.example.vaultkey.screens.Documents
import com.example.vaultkey.screens.LoginScreen
import com.example.vaultkey.screens.ProfileScreen
import com.example.vaultkey.screens.SignupScreen
import com.example.vaultkey.screens.VerificationScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass,
    appController: AppController,
    startDestination: String = if (appController.session == null) "login" else "Home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(700)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                tween (700)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(700)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(700)
            )
        }
    ) {
        composable("Home") {
            HomeScreen(
                navController = navController,
                appController = appController,
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("issued") { Documents(navController, appController) }
        composable("login") { LoginScreen(navController, appController) }
        composable("profile") {
            ProfileScreen(
                appController = appController,
                onBack = { navController.popBackStack() },
                onLogout = {
                    appController.logout {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("chatScreen") { ChatScreen(navController, appController) }
        composable("verification") { VerificationScreen(navController, appController) }
        composable("signup") { SignupScreen(navController, appController) }
        composable("qr") { 
            CameraScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { 
                    navController.navigate("Home") {
                        popUpTo("Home") { inclusive = true }
                    }
                },
                appController = appController
            ) 
        }
    }
}

