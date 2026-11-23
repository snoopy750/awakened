package ch.awakened.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ch.awakened.app.ui.AppRoot
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.theme.AwakenedTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !vm.ready.value }
        enableEdgeToEdge()
        pendingRoute.value = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            AwakenedTheme {
                AppRoot(vm = vm, pendingRoute = pendingRoute.value, onRouteConsumed = { pendingRoute.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_ROUTE)?.let { pendingRoute.value = it }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}
