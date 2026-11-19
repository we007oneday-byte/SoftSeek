package io.github.we007oneday_byte.softseek.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.we007oneday_byte.softseek.ui.screens.about.AboutScreen
import io.github.we007oneday_byte.softseek.ui.screens.call.CallScreen
import io.github.we007oneday_byte.softseek.ui.screens.chat.ChatScreen
import io.github.we007oneday_byte.softseek.ui.screens.guide.GuideScreen
import io.github.we007oneday_byte.softseek.ui.screens.settings.SettingsScreen

sealed class AppRoute(val route: String) {
    data object Chat : AppRoute("chat")
    data object Settings : AppRoute("settings")
    data object Guide : AppRoute("guide")
    data object Call : AppRoute("call")
    data object About : AppRoute("about")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Chat.route,
    ) {
        composable(AppRoute.Chat.route) {
            ChatScreen(
                onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
                onOpenCall = { navController.navigate(AppRoute.Call.route) },
            )
        }
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(AppRoute.About.route) },
                onOpenGuide = { navController.navigate(AppRoute.Guide.route) },
            )
        }
        composable(AppRoute.Guide.route) {
            GuideScreen(onDone = { navController.popBackStack() })
        }
        composable(AppRoute.Call.route) {
            CallScreen(onEndCall = { navController.popBackStack() })
        }
        composable(AppRoute.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
