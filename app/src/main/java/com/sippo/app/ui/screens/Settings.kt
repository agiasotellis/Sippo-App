package com.sippo.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sippo.app.BuildConfig
import com.sippo.app.Screen
import com.sippo.app.SippoViewModel
import com.sippo.app.UiState
import com.sippo.app.data.Accessory
import com.sippo.app.data.Mascot
import com.sippo.app.data.Units
import com.sippo.app.reminders.Reminders
import com.sippo.app.ui.components.Card
import com.sippo.app.ui.components.LockBadge
import com.sippo.app.ui.components.Pill
import com.sippo.app.ui.components.PlusTag
import com.sippo.app.ui.components.SectionLabel
import com.sippo.app.ui.components.Segmented
import com.sippo.app.ui.components.Switch
import com.sippo.app.ui.components.press
import com.sippo.app.ui.components.ring
import com.sippo.app.ui.components.tap
import com.sippo.app.ui.mascot.MascotView
import com.sippo.app.ui.mascot.Mood
import com.sippo.app.ui.theme.Icon
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.Pal
import com.sippo.app.ui.theme.Sym
import com.sippo.app.ui.theme.Themes
import com.sippo.app.ui.theme.ts

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(LocalSippo.current.soft))

@Composable
fun SettingsScreen(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    val ctx = LocalContext.current
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val price by vm.price.collectAsState()

    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Reminders", style = ts(24, 800, th.ink, -.02f))
        if (!s.premium) {
            Row(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Pal.navy, Pal.navyLight)), RoundedCornerShape(24.dp))
                    .press(.98f) { vm.openPaywall(Screen.SETTINGS) }.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).background(Color.White.copy(alpha = .14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Sym.WORKSPACE_PREMIUM, 24, Pal.gold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Get Sippo+", style = ts(15, 800, Color.White))
                    Text("One-time ${price ?: "€2.99"} · no subscription", style = ts(12, 400, Color.White.copy(alpha = .8f)))
                }
                Icon(Sym.CHEVRON_RIGHT, 20, Color.White)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remind me to drink", style = ts(15, 700, th.ink))
                        Text("${s.mascot.label} will ping you", style = ts(12, 400, th.sub2))
                    }
                    Switch(s.remind) {
                        if (!s.remind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(ctx)) {
                            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        vm.toggleRemind()
                    }
                }
                Divider()
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Every", style = ts(13, 700, th.sub))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 60, 90, 120).forEach { v ->
                            Pill(if (v < 60) "${v}m" else "${(v / 60f).let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }}h", v == s.intervalMin, radius = 14.dp, vPad = 10.dp) { vm.setInterval(v) }
                        }
                    }
                }
                Divider()
                Row(Modifier.fillMaxWidth().tap(vm::smartTap).padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Smart reminders", style = ts(14, 700, th.ink))
                            Icon(Sym.AUTO_AWESOME, 16, Color(0xFFFFB020))
                        }
                        Text("Skips nudges when you're on pace", style = ts(12, 400, th.sub2))
                    }
                    if (s.premium) Switch(s.smart, null) else PlusTag(height = 24.dp)
                }
                Divider()
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Active hours", style = ts(14, 700, th.ink))
                    Text("%02d:00 – %02d:00".format(Reminders.ACTIVE_START_HOUR, Reminders.ACTIVE_END_HOUR), style = ts(14, 700, th.acc))
                }
                Divider()
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sound", style = ts(14, 700, th.ink))
                    Text("Bubble pop", style = ts(14, 700, th.acc))
                }
            }
        }
        SectionLabel("GOAL & UNITS", Modifier.padding(top = 4.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily goal", style = ts(14, 700, th.ink))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SquareButton(Sym.REMOVE, 34, 12, 20) { vm.goalStep(-100) }
                        Text(ui.fmt(s.goalMl), style = ts(15, 800, th.ink).copy(textAlign = TextAlign.Center), modifier = Modifier.widthIn(min = 74.dp))
                        SquareButton(Sym.ADD, 34, 12, 20) { vm.goalStep(100) }
                    }
                }
                Divider()
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Units", style = ts(14, 700, th.ink))
                    Segmented(listOf(Units.ML to "ml", Units.OZ to "oz"), s.units, vm::setUnits)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(th.card, RoundedCornerShape(24.dp)).tap(vm::themeTap).padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Sym.PALETTE, 22, th.acc)
                Column {
                    Text("Themes", style = ts(14, 700, th.ink))
                    Text(th.name, style = ts(12, 400, th.sub2))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(th.acc, Color(0xFF14161F), Color(0xFFFF8A65)).forEach { Box(Modifier.size(18.dp).background(it, CircleShape)) }
                if (!s.premium) Icon(Sym.LOCK, 18, th.sub2)
            }
        }
        if (s.premium && BuildConfig.DEBUG) {
            Text(
                "Sippo+ active · reset purchase (debug)",
                style = ts(12, 700, th.sub2).copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().height(40.dp).tap(vm::resetPremiumDebug).padding(top = 12.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(50.dp).background(th.card, RoundedCornerShape(18.dp)).press(onClick = vm::replayOnboarding), contentAlignment = Alignment.Center) {
            Text("Replay onboarding", style = ts(14, 700, th.acc))
        }
    }
}

@Composable
fun SquareButton(glyph: String, size: Int, radius: Int, icon: Int, onClick: () -> Unit) {
    val th = LocalSippo.current
    Box(Modifier.size(size.dp).background(th.soft, RoundedCornerShape(radius.dp)).press(.92f, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(glyph, icon, th.ink)
    }
}

@Composable
fun WardrobeScreen(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    val price by vm.price.collectAsState()
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(th.card, RoundedCornerShape(14.dp)).press { vm.go(Screen.HOME) }, contentAlignment = Alignment.Center) {
                Icon(Sym.ARROW_BACK, 22, th.ink)
            }
            Text("Wardrobe", style = ts(24, 800, th.ink, -.02f))
        }
        Box(Modifier.fillMaxWidth().height(250.dp).background(s.mascot.tint, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            MascotView(s.mascot, if (ui.mood == Mood.DRINK) Mood.DRINK else ui.wardMood, 200.dp, acc = s.accessory)
        }
        SectionLabel("CHARACTER")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mascot.entries.forEach { m ->
                Box(
                    Modifier.weight(1f).ring(m == s.mascot, th.acc, th.line, 20.dp).background(th.card, RoundedCornerShape(20.dp))
                        .press { vm.pickMascot(m) }.padding(top = 8.dp, bottom = 10.dp),
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        MascotView(m, Mood.IDLE, 60.dp)
                        Text(m.label, style = ts(13, 700, th.ink))
                    }
                    if (m.premium && !s.premium) LockBadge(22.dp, 13, Modifier.align(Alignment.TopEnd).offset((-6).dp, (-2).dp))
                }
            }
        }
        SectionLabel("ACCESSORIES")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Accessory.entries.forEach { a ->
                val locked = a.premium && !s.premium && !(a == Accessory.CROWN && s.crownEarned)
                Box(
                    Modifier.weight(1f).aspectRatio(1f).ring(a == s.accessory, th.acc, th.line, 18.dp).background(th.card, RoundedCornerShape(18.dp))
                        .press { vm.pickAccessory(a) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(a.icon, 24, th.acc)
                        Text(a.label, style = ts(10, 700, th.ink))
                    }
                    if (locked) LockBadge(18.dp, 11, Modifier.align(Alignment.TopEnd).offset((-4).dp, 4.dp))
                }
            }
        }
        if (!s.premium) {
            Row(
                Modifier.fillMaxWidth().background(th.inkBg, RoundedCornerShape(20.dp)).press(.98f) { vm.openPaywall(Screen.WARD) }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Sym.WORKSPACE_PREMIUM, 24, Pal.gold)
                Column(Modifier.weight(1f)) {
                    Text("Unlock every friend & outfit", style = ts(14, 800, Color.White))
                    Text("Pingo, Axel, bow, shades & crown", style = ts(12, 400, Color.White.copy(alpha = .75f)))
                }
                Text(price ?: "€2.99", style = ts(13, 800, Color.White))
            }
        }
    }
}

@Composable
fun PaywallScreen(vm: SippoViewModel) {
    val th = LocalSippo.current
    val activity = LocalContext.current as? android.app.Activity
    val storePrice by vm.price.collectAsState()
    val price = storePrice ?: "€2.99"
    val features = listOf(
        Triple(Sym.PETS, Color(0xFFFFB3C7), "All friends & full wardrobe" to "Pingo, Axel, bow, shades & crown"),
        Triple(Sym.CALENDAR_MONTH, Color(0xFF9FD2FF), "Unlimited history" to "Every month, trends & CSV export"),
        Triple(Sym.NOTIFICATIONS_ACTIVE, Pal.gold, "Smart reminders" to "Stays quiet when you're on pace"),
        Triple(Sym.LOCAL_CAFE, Color(0xFFF3C6A0), "More drinks" to "Milk, soda, smoothie & sports drinks"),
        Triple(Sym.WIDGETS, Color(0xFFA9AEFF), "Home-screen widget" to "Pour in one tap from your home screen"),
        Triple(Sym.PALETTE, Color(0xFFFF8A65), "Themes" to "Midnight dark mode, ocean, sunset"),
        Triple(Sym.LOCAL_FIRE_DEPARTMENT, Color(0xFFFFA53D), "Challenges & streak freeze" to "Keep your streak after a missed day"),
    )
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(0f to Pal.navy, .55f to Color(0xFF2F3488), 1f to Color(0xFF4A52D8)))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(14.dp)).press(onClick = vm::closePaywall), contentAlignment = Alignment.Center) {
                Icon(Sym.CLOSE, 22, Color.White)
            }
            Text("Restore purchase", style = ts(13, 700, Color.White.copy(alpha = .75f)), modifier = Modifier.tap(vm::restore).padding(8.dp))
        }
        MascotTrio(92, 118, Modifier.fillMaxWidth().height(120.dp).padding(top = 4.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text("Sippo", style = ts(34, 800, Color.White, -.03f))
                Text("+", style = ts(34, 800, Pal.gold, -.03f))
            }
            Text("Pay once. Yours forever.", style = ts(14, 400, Color.White.copy(alpha = .8f)))
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .08f))
                .ring(1.dp, Color.White.copy(alpha = .1f), 24.dp).padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            features.forEachIndexed { i, (icon, c, text) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(icon, 19, c) }
                    Column(Modifier.weight(1f)) {
                        Text(text.first, style = ts(14, 700, Color.White))
                        Text(text.second, style = ts(11, 400, Color.White.copy(alpha = .7f)))
                    }
                    Icon(Sym.CHECK_CIRCLE, 18, Pal.green)
                }
                if (i < features.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .08f)))
            }
        }
        Row(
            Modifier.fillMaxWidth().ring(3.dp, Pal.gold, 20.dp).background(th.card, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("LIFETIME", style = ts(12, 800, th.acc))
                Text("One-time payment", style = ts(13, 600, th.sub))
            }
            Text(price, style = ts(28, 800, th.ink, -.02f))
        }
        Box(
            Modifier.fillMaxWidth().height(58.dp)
                .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = Pal.gold, spotColor = Pal.gold)
                .background(Pal.gold, RoundedCornerShape(20.dp))
                .press { activity?.let(vm::buy) },
            contentAlignment = Alignment.Center,
        ) { Text("Unlock Sippo+", style = ts(16, 800, th.ink)) }
        Text(
            "No subscription   ·   No ads, ever   ·   Data stays on device",
            style = ts(11, 600, Color.White.copy(alpha = .7f)).copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Pingo (shades), Ottie (crown) and Axel (bow) overlapping, as on the paywall and promo. */
@Composable
fun MascotTrio(side: Int, mid: Int, modifier: Modifier = Modifier, overlap: Int = 14, sink: Int = 0) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        Box(Modifier.offset(x = overlap.dp, y = (sink * .6f).dp)) { MascotView(Mascot.PINGO, Mood.HAPPY, side.dp, acc = Accessory.SHADES) }
        Box(Modifier.zIndex(1f).offset(y = sink.dp)) { MascotView(Mascot.OTTIE, Mood.HAPPY, mid.dp, acc = Accessory.CROWN) }
        Box(Modifier.offset(x = (-overlap).dp, y = (sink * .6f).dp)) { MascotView(Mascot.AXEL, Mood.HAPPY, side.dp, acc = Accessory.BOW) }
    }
}
