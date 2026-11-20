package io.github.we007oneday_byte.softseek.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.we007oneday_byte.softseek.core.logging.LogModule
import io.github.we007oneday_byte.softseek.ui.logging.LogModuleScope
import io.github.we007oneday_byte.softseek.ui.screens.about.AboutScreen
import io.github.we007oneday_byte.softseek.ui.screens.call.CallScreen
import io.github.we007oneday_byte.softseek.ui.screens.chat.ChatScreen
import io.github.we007oneday_byte.softseek.ui.screens.guide.GuideScreen
import io.github.we007oneday_byte.softseek.ui.screens.logs.LogViewerScreen
import io.github.we007oneday_byte.softseek.ui.screens.settings.SettingsHomeScreen
import io.github.we007oneday_byte.softseek.ui.screens.settings.SettingsScreen
import io.github.we007oneday_byte.softseek.ui.screens.settings.SettingsSection

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
        enterTransition = {
            fadeIn(tween(220, delayMillis = 30)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260),
                )
        },
        exitTransition = {
            fadeOut(tween(160)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(220),
                )
        },
        popEnterTransition = {
            fadeIn(tween(220, delayMillis = 30)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260),
                )
        },
        popExitTransition = {
            fadeOut(tween(160)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220),
                )
        },
    ) {
        composable(AppRoute.Chat.route) {
            LogModuleScope(LogModule.CHAT) {
                ChatScreen(
                    onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
                    onOpenCall = { navController.navigate(AppRoute.Call.route) },
                )
            }
        }
        composable(AppRoute.Settings.route) {
            LogModuleScope(LogModule.SETTINGS) {
                SettingsHomeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSection = { section -> navController.navigate(section.route) },
                )
            }
        }
        SettingsSection.entries.forEach { section ->
            composable(section.route) {
                LogModuleScope(
                    if (section == SettingsSection.LOGS) LogModule.LOG else LogModule.SETTINGS,
                ) {
                    if (section == SettingsSection.LOGS) {
                        LogViewerScreen(onBack = { navController.popBackStack() })
                    } else {
                        SettingsScreen(
                            section = section,
                            onBack = { navController.popBackStack() },
                            onOpenAbout = { navController.navigate(AppRoute.About.route) },
                        )
                    }
                }
            }
        }
        composable(AppRoute.Guide.route) {
            LogModuleScope(LogModule.APP) {
                GuideScreen(onDone = { navController.popBackStack() })
            }
        }
        composable(AppRoute.Call.route) {
            LogModuleScope(LogModule.CALL) {
                CallScreen(onEndCall = { navController.popBackStack() })
            }
        }
        composable(AppRoute.About.route) {
            LogModuleScope(LogModule.SETTINGS) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
