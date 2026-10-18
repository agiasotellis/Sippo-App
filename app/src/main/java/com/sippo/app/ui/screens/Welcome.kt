package com.sippo.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.sippo.app.reminders.Reminders
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sippo.app.SippoViewModel
import com.sippo.app.UiState
import com.sippo.app.data.Mascot
import com.sippo.app.data.Units
import com.sippo.app.ui.components.Card
import com.sippo.app.ui.components.PlusTag
import com.sippo.app.ui.components.Pill
import com.sippo.app.ui.components.Segmented
import com.sippo.app.ui.components.SippoSlider
import com.sippo.app.ui.components.press
import com.sippo.app.ui.components.ring
import com.sippo.app.ui.mascot.MascotView
import com.sippo.app.ui.mascot.Mood
import com.sippo.app.ui.theme.Icon
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.Sym
import com.sippo.app.ui.theme.ts
import com.sippo.app.ui.water.Pool

@Composable
fun WelcomeScreen(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to th.bg, .45f to Color(0xFFDFE3FF), 1f to Color(0xFFDFE3FF))),
    ) {
        val h = maxHeight
        // Clouds in the sky.
        Box(Modifier.offset(34.dp, h * (190f / 748f)).size(76.dp, 24.dp).alpha(.85f).background(th.card, RoundedCornerShape(24.dp)))
        Box(Modifier.offset(54.dp, h * (176f / 748f)).size(40.dp, 32.dp).alpha(.85f).background(th.card, CircleShape))
        Box(Modifier.align(Alignment.TopEnd).offset((-40).dp, h * (236f / 748f)).size(58.dp, 20.dp).alpha(.7f).background(th.card, RoundedCornerShape(20.dp)))

        Pool(key = ui.welcomeRun, ignoreBottom = 140f) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.statusBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Sym.WATER_DROP, 22, th.acc)
                        Text("sippo", style = ts(20, 800, th.ink, -.03f))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Hydration is better with friends", style = ts(32, 800, th.ink, -.03f, 1.05f))
                        Text("Ottie, Pingo and Axel will cheer every sip.", style = ts(14, 400, th.sub))
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(56.dp)
                            .shadow(18.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x99141E5A), spotColor = Color(0x99141E5A))
                            .background(th.card, RoundedCornerShape(20.dp))
                            .press(onClick = vm::getStarted),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Get started", style = ts(16, 800, th.ink))
                        Icon(Sym.ARROW_FORWARD, 20, th.ink, Modifier.padding(start = 8.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().height(28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Sym.LOCK, 16, Color.White)
                        Text("No account needed · your data stays on this phone", style = ts(12, 600, Color.White), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    val ctx = LocalContext.current
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val next = {
        if (ui.step == 2 && s.remind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(ctx)) {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.onbNext()
    }
    Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(Modifier.weight(1f).height(5.dp).background(if (i <= ui.step) th.acc else Color(0xFFDCDBEA), RoundedCornerShape(5.dp)))
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (ui.step) {
                0 -> {
                    Heading("Pick your hydration buddy", "They float, splash and cheer as you drink.")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Mascot.entries.forEach { m ->
                            val locked = m.premium && !s.premium
                            Box(
                                Modifier.fillMaxWidth()
                                    .background(th.card, RoundedCornerShape(24.dp))
                                    .ring(m == s.mascot, th.acc, th.line, 24.dp)
                                    .press(.98f) { vm.pickMascot(m) }
                                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 16.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(86.dp).background(m.tint, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                                        MascotView(m, Mood.IDLE, 80.dp)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(m.label, style = ts(18, 800, th.ink))
                                        Text(m.species, style = ts(12, 700, th.acc))
                                        Text(m.blurb, style = ts(12, 400, th.sub))
                                    }
                                }
                                if (locked) PlusTag(Modifier.align(Alignment.TopEnd).offset(6.dp, (-2).dp))
                            }
                        }
                    }
                }
                1 -> {
                    Heading("Let's find your daily goal", "Based on body weight and activity.")
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Text("Weight", style = ts(13, 700, th.sub))
                                Text(if (s.units == Units.OZ) "${Math.round(s.weightKg * 2.2046f)} lb" else "${s.weightKg} kg", style = ts(22, 800, th.ink))
                            }
                            SippoSlider(s.weightKg, 40..140, 1, vm::setWeight)
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Activity", style = ts(13, 700, th.sub))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Light", "Active", "Athlete").forEachIndexed { i, label ->
                                    Pill(label, i == s.activity) { vm.setActivity(i) }
                                }
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Units", style = ts(13, 700, th.sub))
                            Segmented(listOf(Units.ML to "ml", Units.OZ to "oz"), s.units, vm::setUnits)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().background(th.acc, RoundedCornerShape(24.dp)).padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Your goal", style = ts(14, 600, Color.White))
                        Text(ui.fmt(s.goalMl), style = ts(30, 800, Color.White, -.02f))
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(230.dp).background(s.mascot.tint, CircleShape), contentAlignment = Alignment.Center) {
                            MascotView(s.mascot, Mood.HAPPY, 190.dp)
                        }
                        Text("Hi, I'm ${s.mascot.label}!", style = ts(28, 800, th.ink, -.02f))
                        Text(
                            "Let's drink ${ui.fmt(s.goalMl)} a day. I'll nudge you every ${intervalLabel(s.intervalMin)}.",
                            style = ts(14, 400, th.sub).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            modifier = Modifier.padding(horizontal = 40.dp),
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ui.step > 0) {
                Box(Modifier.size(56.dp).background(th.card, RoundedCornerShape(20.dp)).press(onClick = vm::onbBack), contentAlignment = Alignment.Center) {
                    Icon(Sym.ARROW_BACK, 24, th.ink)
                }
            }
            Box(
                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(20.dp)).background(th.inkBg).press(onClick = next),
                contentAlignment = Alignment.Center,
            ) { Text(listOf("Continue", "Set goal", "Start drinking")[ui.step], style = ts(16, 700, Color.White)) }
        }
    }
}

@Composable
private fun Heading(title: String, sub: String) {
    val th = LocalSippo.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = ts(28, 800, th.ink, -.02f, 1.1f))
        Text(sub, style = ts(14, 400, th.sub))
    }
}

fun intervalLabel(min: Int): String = when {
    min < 60 -> "$min min"
    min == 60 -> "1 hour"
    min % 60 == 0 -> "${min / 60} hours"
    else -> "${min / 60f} hours"
}
