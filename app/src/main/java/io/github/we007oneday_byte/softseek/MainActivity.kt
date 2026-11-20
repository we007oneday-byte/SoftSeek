package io.github.we007oneday_byte.softseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.we007oneday_byte.softseek.core.logging.AppLog
import io.github.we007oneday_byte.softseek.core.logging.LogModule
import io.github.we007oneday_byte.softseek.core.model.AppInfo
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import io.github.we007oneday_byte.softseek.ui.navigation.AppNavGraph
import io.github.we007oneday_byte.softseek.ui.screens.splash.SplashIntro
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekBackdrop
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appInfo: AppInfo

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppLog.info(
            LogModule.APP,
            "Hilt 注入完成 package=${appInfo.packageName} version=${appInfo.versionName}",
        )
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            var showIntro by rememberSaveable { mutableStateOf(true) }
            SoftSeekTheme(settings = settings) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SoftSeekBackdrop()
                    if (showIntro) {
                        SplashIntro(onFinished = { showIntro = false })
                    } else {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}
