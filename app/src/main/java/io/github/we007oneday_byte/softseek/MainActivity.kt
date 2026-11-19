package io.github.we007oneday_byte.softseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.we007oneday_byte.softseek.ui.navigation.AppNavGraph
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoftSeekTheme {
                AppNavGraph()
            }
        }
    }
}
