package com.sippo.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sippo.app.ui.SippoApp
import com.sippo.app.ui.theme.Themes
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PAYWALL = "open_paywall"
    }

    private val vm: SippoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !vm.ui.value.loaded }
        super.onCreate(savedInstanceState)
        applyBars(dark = false)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.map { Themes.of(it.settings.theme).dark to it.screen }.distinctUntilChanged().collect { (dark, screen) ->
                    // Light icons over the dark paywall and Midnight theme; dark icons elsewhere.
                    applyBars(dark || screen == Screen.PAYWALL)
                }
            }
        }
        handleIntent(intent)
        setContent { SippoApp(vm, onExit = ::finish) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_PAYWALL, false) == true) {
            lifecycleScope.launch {
                val u = vm.ui.first { it.loaded }
                if (u.settings.onboarded && !u.settings.premium) vm.openPaywall(Screen.HOME)
            }
            intent.removeExtra(EXTRA_PAYWALL)
        }
    }

    private fun applyBars(dark: Boolean) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
