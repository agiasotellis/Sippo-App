package com.sippo.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.sippo.app.SippoViewModel
import com.sippo.app.UiState
import com.sippo.app.data.Entry
import com.sippo.app.ui.components.Card
import com.sippo.app.ui.components.press
import com.sippo.app.ui.components.tap
import com.sippo.app.ui.theme.Icon
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.Pal
import com.sippo.app.ui.theme.Sym
import com.sippo.app.ui.theme.ts
import com.sippo.app.ui.water.Bowl
import com.sippo.app.ui.water.BowlSim
import com.sippo.app.ui.water.rememberTilt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HomeScreen(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    val sim = remember { BowlSim() }
    val tilt = rememberTilt()
    val haptic = LocalHapticFeedback.current
    val pourMl = remember { mutableIntStateOf(0) }
    val total = ui.todayTotal
    val drink = s.lastDrink

    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(LocalDate.ofEpochDay(ui.today).format(DateTimeFormatter.ofPattern("EEEE, MMM d")), style = ts(12, 700, th.sub))
                Text("Today", style = ts(24, 800, th.ink, -.02f))
            }
            Row(
                Modifier.height(40.dp).background(th.card, RoundedCornerShape(14.dp)).press { vm.go(com.sippo.app.Screen.WARD) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Sym.CHECKROOM, 18, th.acc)
                Text("Wardrobe", style = ts(13, 700, th.ink))
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.Center) {
            val size = (maxWidth.value * .82f).coerceIn(220f, 320f).dp
            Bowl(
                size = size, total = total, goal = s.goalMl, pouring = ui.pouring,
                pourColor = drink.color, pourFactor = drink.factor, splashCount = ui.splash,
                mascot = s.mascot, mood = ui.mood, acc = s.accessory, tilt = tilt, sim = sim,
                onPourTick = { pourMl.intValue = it },
            )
        }

        Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("HOLD TO POUR · TILT OR SWIPE THE BOWL TO SLOSH", style = ts(11, 700, Pal.mute, .04f), modifier = Modifier.padding(bottom = 6.dp))
            Text(ui.fmt(total), style = ts(40, 800, th.ink, -.03f, 1f))
            Text("of ${ui.fmt(s.goalMl)} · ${(total / s.goalMl * 100).roundToInt()}%", style = ts(14, 600, th.sub))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            val bg by animateColorAsState(if (ui.pouring) drink.color else th.card, tween(200), label = "pourBg")
            val sc by animateFloatAsState(if (ui.pouring) .96f else 1f, tween(150), label = "pourScale")
            val fg = if (ui.pouring) Color.White else th.ink
            Row(
                Modifier.height(52.dp).widthIn(min = 150.dp)
                    .graphicsLayer { scaleX = sc; scaleY = sc }
                    .background(bg, RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            sim.pouring = true; sim.pour = 0f; sim.pt = 0f
                            pourMl.intValue = 0
                            vm.pourStart()
                            tryAwaitRelease()
                            val ml = maxOf(50, (sim.pour / 10f).roundToInt() * 10)
                            sim.pouring = false; sim.pour = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.pourEnd(ml)
                        })
                    }
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Sym.WATER_DROP, 20, fg)
                Text(if (ui.pouring) "+${ui.fmt(pourMl.intValue)}" else "Hold to pour", style = ts(14, 700, fg))
            }
            Row(
                Modifier.height(52.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = th.acc, spotColor = th.acc)
                    .background(th.acc, RoundedCornerShape(18.dp))
                    .press(.95f, onClick = vm::openSheet)
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Sym.ADD, 22, Color.White)
                Text("Add drink", style = ts(14, 700, Color.White))
            }
        }

        Card(Modifier.fillMaxWidth(), radius = 22.dp) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Today's drinks", style = ts(13, 800, th.ink))
                    if (ui.todayEntries.isNotEmpty()) Text("Tap to edit", style = ts(11, 600, Pal.mute))
                }
                val rows = ui.todayEntries.reversed().take(6)
                if (rows.isEmpty()) {
                    Text("Nothing yet. Hold to pour your first sip!", style = ts(13, 600, th.sub2), modifier = Modifier.padding(vertical = 14.dp))
                }
                rows.forEachIndexed { i, e -> DrinkRow(e, ui, last = i == rows.lastIndex) { vm.openEdit(e) } }
            }
        }
    }
}

@Composable
fun DrinkRow(e: Entry, ui: UiState, last: Boolean, onClick: (() -> Unit)? = null) {
    val th = LocalSippo.current
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.tap(onClick) else Modifier).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            DrinkTile(e.drink, 36, 12, 20)
            Column(Modifier.weight(1f)) {
                Text(e.drink.label, style = ts(14, 700, th.ink))
                Text(e.timeLabel, style = ts(12, 400, th.sub2))
            }
            Text(ui.fmt(e.ml), style = ts(14, 700, th.ink))
            if (onClick != null) Icon(Sym.EDIT, 18, Pal.editIcon)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(th.soft))
    }
}

@Composable
fun DrinkTile(d: com.sippo.app.data.Drink, size: Int, radius: Int, icon: Int) {
    val th = LocalSippo.current
    Box(Modifier.size(size.dp).background(if (th.dark) th.soft else d.tint, RoundedCornerShape(radius.dp)), contentAlignment = Alignment.Center) {
        Icon(d.icon, icon, d.color)
    }
}
