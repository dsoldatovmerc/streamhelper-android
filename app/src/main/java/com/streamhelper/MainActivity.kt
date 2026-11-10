package com.streamhelper

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.streamhelper.core.snackbar.AppSnackbarHost
import com.streamhelper.core.snackbar.SnackbarBus
import com.streamhelper.core.startup.StartupGate
import com.streamhelper.core.ui.theme.StreamHelperTheme
import com.streamhelper.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var snackbarBus: SnackbarBus

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        val startupGate = EntryPointAccessors
            .fromApplication(applicationContext, StartupGate.Accessor::class.java)
            .startupGate()
        // Keep the splash up until Entry has decided where to go.
        splash.setKeepOnScreenCondition { !startupGate.isReady.value }
        // After process death the in-memory session is gone; drop restored navigation state so
        // the app restarts through Entry instead of showing Main without a user.
        super.onCreate(savedInstanceState.takeIf { startupGate.isReady.value })
        // Light-only theme: always dark system-bar icons, regardless of the device's dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            StreamHelperTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                    AppSnackbarHost(bus = snackbarBus, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}
