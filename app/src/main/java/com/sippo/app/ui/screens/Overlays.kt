package com.sippo.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sippo.app.Screen
import com.sippo.app.SippoViewModel
import com.sippo.app.UiState
import com.sippo.app.data.Drink
import com.sippo.app.data.ReminderSound
import com.sippo.app.ui.components.BottomSheet
import com.sippo.app.ui.components.PopEase
import com.sippo.app.ui.components.SippoSlider
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
import kotlin.math.floor
import kotlin.math.sin

@Composable
private fun DrinkGrid(selected: Drink?, premium: Boolean, showFactor: Boolean, onPick: (Drink) -> Unit) {
    val th = LocalSippo.current
    val drinks = if (premium) Drink.entries else Drink.free
    drinks.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { d ->
                val on = d == selected
                Column(
                    Modifier.weight(1f)
                        .then(if (on) Modifier.ring(2.dp, d.color, 18.dp) else Modifier)
                        .background(if (on) (if (th.dark) th.soft else d.tint) else th.soft2, RoundedCornerShape(18.dp))
                        .tap { onPick(d) }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(d.icon, 26, d.color)
                    Text(d.label, style = ts(12, 700, th.ink), maxLines = 1)
                    if (showFactor) Text("${Math.round(d.factor * 100)}% hydration", style = ts(10, 400, th.sub2), maxLines = 1)
                }
            }
            repeat(4 - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun BoxScope.AddDrinkSheet(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    BottomSheet(ui.sheet, vm::closeSheet) {
        Text("Add a drink", style = ts(20, 800, th.ink))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { DrinkGrid(s.lastDrink, s.premium, true, vm::sheetDrink) }
        if (!s.premium) {
            Row(
                Modifier.fillMaxWidth().background(th.soft2, RoundedCornerShape(14.dp)).tap { vm.openPaywall(Screen.HOME) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Sym.LOCK, 16, th.acc)
                Text("Milk, soda, smoothie & sports drinks", style = ts(12, 700, th.acc), modifier = Modifier.weight(1f))
                Text("PLUS", style = ts(10, 800, Color.White), modifier = Modifier.background(th.inkBg, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Amount", style = ts(13, 700, th.sub))
            Text(ui.fmt(s.sheetAmount), style = ts(28, 800, th.ink, -.02f))
        }
        SippoSlider(s.sheetAmount, 50..1000, 10, vm::sheetAmount)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(150, 250, 350, 500).forEach { v ->
                val on = v == s.sheetAmount
                Box(
                    Modifier.weight(1f).background(if (on) th.acc else th.soft, RoundedCornerShape(14.dp)).tap { vm.sheetAmount(v) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(ui.fmt(v), style = ts(12, 700, if (on) Color.White else th.ink)) }
            }
        }
        PrimaryButton("Add ${ui.fmt(s.sheetAmount)}", vm::addFromSheet)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, height: Dp = 56.dp) {
    val th = LocalSippo.current
    Box(Modifier.fillMaxWidth().height(height).background(th.acc, RoundedCornerShape(20.dp)).press(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = ts(16, 700, Color.White))
    }
}

@Composable
fun BoxScope.EditSheet(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    // Keep the last edit on screen while the sheet animates out.
    val last = remember { arrayOfNulls<com.sippo.app.EditState>(1) }
    ui.edit?.let { last[0] = it }
    val e = ui.edit ?: last[0]
    BottomSheet(ui.edit != null, vm::closeEdit) {
        if (e == null) return@BottomSheet
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Edit drink", style = ts(20, 800, th.ink))
                Text("Logged at ${e.time}", style = ts(12, 600, th.sub2))
            }
            Row(
                Modifier.height(40.dp).background(Color(0xFFFFE8EC), RoundedCornerShape(14.dp)).press(onClick = vm::deleteEdit).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Sym.DELETE, 18, Color(0xFFD8385A))
                Text("Delete", style = ts(13, 700, Color(0xFFD8385A)))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { DrinkGrid(e.drink, ui.settings.premium || e.drink.premium, false, vm::editDrink) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SquareButton(Sym.REMOVE, 48, 16, 24) { vm.editAmount(e.ml - 50) }
            Text(ui.fmt(e.ml), style = ts(30, 800, th.ink, -.02f))
            SquareButton(Sym.ADD, 48, 16, 24) { vm.editAmount(e.ml + 50) }
        }
        SippoSlider(e.ml, 10..1000, 10, vm::editAmount)
        PrimaryButton("Save changes", vm::saveEdit)
    }
}

@Composable
fun BoxScope.ThemeSheet(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    BottomSheet(ui.themeSheet, vm::closeTheme) {
        Text("Theme", style = ts(20, 800, th.ink))
        Themes.all.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { t ->
                    Column(
                        Modifier.weight(1f)
                            .then(if (t.key == th.key) Modifier.ring(3.dp, th.acc, 20.dp) else Modifier)
                            .background(th.soft2, RoundedCornerShape(20.dp)).tap { vm.setTheme(t.key) }.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().height(64.dp).background(t.bg, RoundedCornerShape(14.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.fillMaxWidth(.5f).height(8.dp).background(t.ink, RoundedCornerShape(4.dp)))
                            Box(Modifier.fillMaxWidth().weight(1f).background(t.card, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterEnd) {
                                Box(Modifier.size(16.dp).background(t.acc, CircleShape))
                            }
                        }
                        Text(t.name, style = ts(13, 700, th.ink))
                    }
                }
            }
        }
        PrimaryButton("Done", vm::closeTheme, height = 54.dp)
    }
}

@Composable
fun BoxScope.Snack(ui: UiState, vm: SippoViewModel, bottom: Dp) {
    val th = LocalSippo.current
    val last = remember { arrayOf("", "false") }
    ui.snack?.let { last[0] = it; last[1] = ui.snackUndo.toString() }
    AnimatedVisibility(
        ui.snack != null,
        enter = fadeIn(tween(150)) + scaleIn(tween(300, easing = PopEase), initialScale = .6f),
        exit = fadeOut(tween(200)),
        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 14.dp, end = 14.dp, bottom = bottom),
    ) {
        Row(
            Modifier.fillMaxWidth().height(52.dp)
                .shadow(16.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x73000000), spotColor = Color(0x73000000))
                .background(th.inkBg, RoundedCornerShape(14.dp)).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Sym.WATER_DROP, 20, Pal.waterLight)
            Text(last[0], style = ts(14, 600, Color.White), modifier = Modifier.weight(1f), maxLines = 2)
            if (last[1] == "true") {
                Row(
                    Modifier.height(38.dp).clip(RoundedCornerShape(10.dp)).tap(vm::undo).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Sym.UNDO, 18, Color(0xFFA9AEFF))
                    Text("Undo", style = ts(14, 800, Color(0xFFA9AEFF)))
                }
            }
        }
    }
}

@Composable
fun BoxScope.Promo(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val price by vm.price.collectAsState()
    val show = ui.promo && ui.screen == Screen.HOME
    AnimatedVisibility(show, enter = fadeIn(tween(300)), exit = fadeOut(tween(200)), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x8C141632)).tap(vm::closePromo))
    }
    AnimatedVisibility(
        show,
        enter = fadeIn(tween(200)) + scaleIn(tween(500, easing = androidx.compose.animation.core.CubicBezierEasing(.3f, 1.5f, .5f, 1f)), initialScale = .6f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = .9f),
        modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth()
                .shadow(30.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x99141632), spotColor = Color(0x99141632))
                .clip(RoundedCornerShape(32.dp)).background(th.card).tap { }.padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Pal.navy, Pal.navyLight)))) {
                Text(
                    "${price ?: "€2.99"} once",
                    style = ts(11, 800, th.ink),
                    modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).background(Pal.gold, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 5.dp),
                )
                MascotTrio(84, 110, Modifier.align(Alignment.BottomCenter).fillMaxWidth(), overlap = 12, sink = 10)
            }
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Pingo & Axel want to join!", style = ts(22, 800, th.ink, -.02f).copy(textAlign = TextAlign.Center))
                Text(
                    "Unlock every friend, outfit, unlimited history, smart reminders and more with Sippo+. One payment, yours forever.",
                    style = ts(14, 400, th.sub).copy(textAlign = TextAlign.Center),
                )
            }
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(52.dp).background(th.inkBg, RoundedCornerShape(18.dp)).press { vm.openPaywall(Screen.HOME) },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Sym.WORKSPACE_PREMIUM, 20, Pal.gold)
                    Text("See Sippo+", style = ts(15, 800, Color.White))
                }
                Box(Modifier.fillMaxWidth().height(42.dp).tap(vm::closePromo), contentAlignment = Alignment.Center) {
                    Text("Maybe later", style = ts(14, 700, th.sub2))
                }
            }
        }
    }
}

private data class Confetto(val left: Float, val width: Float, val color: Color, val dur: Float, val delay: Float)

private fun pseudo(n: Int): Float {
    val x = sin(n * 99.13) * 43758.5
    return (x - floor(x)).toFloat()
}

private val Confetti = List(26) { i ->
    Confetto(
        pseudo(i + 3), 6f + Math.round(pseudo(i + 40) * 6), listOf(0xFFFFC93D, 0xFFFF6B8B, 0xFF7CC0FF, 0xFF7BE0A8, 0xFFFFFFFF, 0xFFB48CFF).map { Color(it) }[i % 6],
        2.2f + pseudo(i + 9) * 1.8f, -pseudo(i + 21) * 3f,
    )
}

@Composable
fun BoxScope.Celebration(ui: UiState, vm: SippoViewModel) {
    val s = ui.settings
    AnimatedVisibility(ui.celebrate, enter = fadeIn(tween(300)), exit = fadeOut(tween(250)), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(Color(0xD11F2350)).tap(vm::closeCelebrate), contentAlignment = Alignment.Center) {
            val frame = remember { mutableIntStateOf(0) }
            val clock = remember { floatArrayOf(0f) }
            LaunchedEffect(Unit) {
                val start = withFrameNanos { it }
                while (true) withFrameNanos { clock[0] = (it - start) / 1e9f; frame.intValue++ }
            }
            Canvas(Modifier.fillMaxSize()) {
                frame.intValue
                val t = clock[0]
                val travel = size.height + 60.dp.toPx()
                Confetti.forEach { c ->
                    val x = (t - c.delay) / c.dur
                    val p = x - floor(x)
                    val y = -40.dp.toPx() + travel * p
                    val left = c.left * size.width
                    val w = c.width.dp.toPx()
                    val h = 14.dp.toPx()
                    rotate(720f * p, Offset(left + w / 2, y + h / 2)) {
                        drawRoundRect(c.color, Offset(left, y), Size(w, h), CornerRadius(3.dp.toPx()))
                    }
                }
            }
            AnimatedVisibility(
                ui.celebrate,
                enter = scaleIn(tween(600, easing = androidx.compose.animation.core.CubicBezierEasing(.3f, 1.5f, .5f, 1f)), initialScale = .6f) + fadeIn(tween(300)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.navigationBarsPadding()) {
                    MascotView(s.mascot, Mood.HAPPY, 190.dp, acc = s.accessory)
                    Text("Goal reached!", style = ts(30, 800, Color.White, -.02f))
                    val streak = com.sippo.app.data.Progress.streak(com.sippo.app.data.Progress.dailyTotals(ui.entries), s.goalMl, ui.today, s.premium)
                    Text(
                        "${s.mascot.label} is doing a happy dance." + if (streak > 1) " $streak-day streak!" else "",
                        style = ts(14, 400, Color.White.copy(alpha = .85f)).copy(textAlign = TextAlign.Center),
                        modifier = Modifier.widthIn(max = 240.dp),
                    )
                    Text(
                        "Yay!",
                        style = ts(14, 700, LocalSippo.current.ink),
                        modifier = Modifier.padding(top = 8.dp).background(LocalSippo.current.card, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    val th = LocalSippo.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = ts(14, 700, th.ink))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SquareButton(Sym.REMOVE, 40, 14, 22) { onChange(hour - 1) }
            Text("%02d:00".format(hour), style = ts(20, 800, th.ink).copy(textAlign = TextAlign.Center), modifier = Modifier.widthIn(min = 72.dp))
            SquareButton(Sym.ADD, 40, 14, 22) { onChange(hour + 1) }
        }
    }
}

@Composable
fun BoxScope.HoursSheet(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    BottomSheet(ui.hoursSheet, vm::closeHours) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Active hours", style = ts(20, 800, th.ink))
            Text("${s.mascot.label} only reminds you between these times.", style = ts(13, 400, th.sub))
        }
        HourStepper("From", s.activeStart, vm::setActiveStart)
        HourStepper("Until", s.activeEnd, vm::setActiveEnd)
        PrimaryButton("Done", vm::closeHours, height = 54.dp)
    }
}

@Composable
fun BoxScope.SoundSheet(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    BottomSheet(ui.soundSheet, vm::closeSound) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Reminder sound", style = ts(20, 800, th.ink))
            Text("Tap a sound to hear it.", style = ts(13, 400, th.sub))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderSound.entries.forEach { snd ->
                val on = snd == s.sound
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (on) Modifier.ring(2.dp, th.acc, 16.dp) else Modifier)
                        .background(th.soft2, RoundedCornerShape(16.dp)).tap { vm.setSound(snd) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(snd.label, style = ts(14, 700, th.ink))
                    if (on) Icon(Sym.CHECK_CIRCLE, 20, th.acc)
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(50.dp).background(th.soft, RoundedCornerShape(18.dp)).press(onClick = vm::testReminder),
            contentAlignment = Alignment.Center,
        ) { Text("Send a test reminder", style = ts(14, 700, th.acc)) }
        PrimaryButton("Done", vm::closeSound, height = 54.dp)
    }
}
