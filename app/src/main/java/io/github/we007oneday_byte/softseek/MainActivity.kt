package io.github.we007oneday_byte.softseek

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import io.github.we007oneday_byte.softseek.core.model.AppInfo
import io.github.we007oneday_byte.softseek.ui.navigation.AppNavGraph
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appInfo: AppInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("SoftSeek", "Hilt injected: ${appInfo.packageName} ${appInfo.versionName}")
        setContent {
            SoftSeekTheme {
                AppNavGraph()
            }
        }
    }
}
