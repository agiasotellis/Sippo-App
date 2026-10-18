package com.sippo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sippo.app.Screen
import com.sippo.app.SippoViewModel
import com.sippo.app.UiState
import com.sippo.app.ui.components.NavBar
import com.sippo.app.ui.components.NavTab
import com.sippo.app.ui.screens.AddDrinkSheet
import com.sippo.app.ui.screens.AwardsScreen
import com.sippo.app.ui.screens.Celebration
import com.sippo.app.ui.screens.EditSheet
import com.sippo.app.ui.screens.HistoryScreen
import com.sippo.app.ui.screens.HomeScreen
import com.sippo.app.ui.screens.HoursSheet
import com.sippo.app.ui.screens.SoundSheet
import com.sippo.app.ui.screens.OnboardingScreen
import com.sippo.app.ui.screens.PaywallScreen
import com.sippo.app.ui.screens.Promo
import com.sippo.app.ui.screens.SettingsScreen
import com.sippo.app.ui.screens.Snack
import com.sippo.app.ui.screens.StatsScreen
import com.sippo.app.ui.screens.ThemeSheet
import com.sippo.app.ui.screens.WardrobeScreen
import com.sippo.app.ui.screens.WelcomeScreen
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.SippoTheme
import com.sippo.app.ui.theme.Sym
import com.sippo.app.ui.theme.Themes

private val Tabs = listOf(
    NavTab(Screen.HOME, Sym.WATER_DROP, "Today"),
    NavTab(Screen.STATS, Sym.DONUT_LARGE, "Stats"),
    NavTab(Screen.HIST, Sym.CALENDAR_MONTH, "History"),
    NavTab(Screen.AWARDS, Sym.EMOJI_EVENTS, "Awards"),
    NavTab(Screen.SETTINGS, Sym.NOTIFICATIONS, "Reminders"),
)

private val ScreenEase = CubicBezierEasing(.2f, .9f, .3f, 1f)

@Composable
fun SippoApp(vm: SippoViewModel, onExit: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val colors = Themes.of(ui.settings.theme)
    SippoTheme(colors) {
        BackHandler { if (!vm.back()) onExit() }
        val th = LocalSippo.current
        val showNav = ui.screen !in setOf(Screen.WELCOME, Screen.ONB, Screen.PAYWALL)
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val slide = with(LocalDensity.current) { 14.dp.roundToPx() }

        Box(Modifier.fillMaxSize().background(th.bg)) {
            // Keep layouts phone-shaped on tablets and foldables.
            Box(Modifier.align(Alignment.TopCenter).widthIn(max = 520.dp).fillMaxSize()) {
                AnimatedContent(
                    targetState = ui.screen to (if (ui.screen == Screen.ONB) ui.step else 0),
                    transitionSpec = {
                        (fadeIn(tween(400, easing = ScreenEase)) + slideInVertically(tween(400, easing = ScreenEase)) { slide }) togetherWith fadeOut(tween(120))
                    },
                    label = "screen",
                ) { (screen, _) ->
                    ScreenContent(screen, ui, vm, navSpace = if (showNav) 96.dp + navInset else 0.dp)
                }

                if (showNav) {
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp + navInset).background(Brush.verticalGradient(0f to Color.Transparent, .55f to th.bg)))
                    NavBar(
                        Tabs,
                        active = Tabs.indexOfFirst { it.key == (if (ui.screen == Screen.WARD) Screen.HOME else ui.screen) }.coerceAtLeast(0),
                        onTab = { vm.go(Tabs[it].key as Screen) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = navInset + 6.dp),
                    )
                }

                Snack(ui, vm, bottom = if (showNav) 94.dp + navInset else 24.dp + navInset)
                AddDrinkSheet(ui, vm)
                EditSheet(ui, vm)
                ThemeSheet(ui, vm)
                HoursSheet(ui, vm)
                SoundSheet(ui, vm)
                Promo(ui, vm)
                Celebration(ui, vm)
            }
        }
    }
}

@Composable
private fun ScreenContent(screen: Screen, ui: UiState, vm: SippoViewModel, navSpace: androidx.compose.ui.unit.Dp) {
    when (screen) {
        Screen.WELCOME -> WelcomeScreen(ui, vm)
        Screen.ONB -> Box(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
            OnboardingScreen(ui, vm)
        }
        Screen.PAYWALL -> Column(Modifier.fillMaxSize().background(Color(0xFF4A52D8)).verticalScroll(rememberScrollState())) {
            PaywallScreen(vm)
            androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
        }
        else -> Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
            when (screen) {
                Screen.HOME -> HomeScreen(ui, vm)
                Screen.STATS -> StatsScreen(ui)
                Screen.HIST -> HistoryScreen(ui, vm)
                Screen.AWARDS -> AwardsScreen(ui, vm)
                Screen.SETTINGS -> SettingsScreen(ui, vm)
                Screen.WARD -> WardrobeScreen(ui, vm)
                else -> Unit
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(navSpace))
        }
    }
}
