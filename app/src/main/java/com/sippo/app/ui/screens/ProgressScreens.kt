package com.sippo.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sippo.app.Screen
import com.sippo.app.SippoViewModel
import com.sippo.app.UiState
import com.sippo.app.data.Drink
import com.sippo.app.data.Entry
import com.sippo.app.data.Progress
import com.sippo.app.ui.components.Card
import com.sippo.app.ui.components.PlusTag
import com.sippo.app.ui.components.press
import com.sippo.app.ui.components.ring
import com.sippo.app.ui.components.tap
import com.sippo.app.ui.mascot.MascotView
import com.sippo.app.ui.mascot.Mood
import com.sippo.app.ui.theme.Icon
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.Pal
import com.sippo.app.ui.theme.Sym
import com.sippo.app.ui.theme.ts
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val RingEase = CubicBezierEasing(.3f, 1f, .4f, 1f)

@Composable
private fun Title(text: String) = Text(text, style = ts(24, 800, LocalSippo.current.ink, -.02f))

@Composable
fun StatsScreen(ui: UiState) {
    val th = LocalSippo.current
    val s = ui.settings
    val totals = remember(ui.entries) { Progress.dailyTotals(ui.entries) }
    val total = ui.todayTotal
    val p = (total / s.goalMl).coerceIn(0f, 1f)
    val streak = Progress.streak(totals, s.goalMl, ui.today, freeze = s.premium)
    val avg = Progress.dailyAverage(totals, ui.today)

    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("Stats")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val intro = remember { Animatable(0f) }
                LaunchedEffect(Unit) { intro.animateTo(1f, tween(1200, easing = RingEase)) }
                val shown by animateFloatAsState(p, tween(1000, easing = RingEase), label = "ring")
                Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 18.dp.toPx()
                        val r = 86.dp.toPx()
                        val tl = Offset(size.width / 2 - r, size.height / 2 - r)
                        drawCircle(th.soft, r, style = Stroke(sw))
                        val sweep = 360f * shown * intro.value
                        if (sweep > 0f) drawArc(th.acc, -90f, sweep, false, tl, Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(total / s.goalMl * 100).roundToInt()}%", style = ts(40, 800, th.ink, -.03f))
                        Text("${ui.fmt(total)} today", style = ts(12, 600, th.sub))
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    StatCell("$streak ${if (streak == 1) "day" else "days"}", "Streak")
                    StatCell(ui.fmt(avg), "Daily avg")
                    StatCell("${ui.todayEntries.size}", "Drinks today")
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This week", style = ts(14, 800, th.ink))
                val days = (6 downTo 0).map { ui.today - it }
                val vals = days.map { totals[it] ?: 0f }
                val mx = maxOf(s.goalMl.toFloat(), vals.max())
                Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    days.forEachIndexed { i, d ->
                        val h by animateFloatAsState(maxOf(.04f, vals[i] / mx), tween(800), label = "bar")
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight(h).background(
                                        when { i == 6 -> th.acc; vals[i] >= s.goalMl -> Color(0xFF9AA0F5); else -> Color(0xFFD8DAF8) },
                                        RoundedCornerShape(10.dp),
                                    ),
                                )
                            }
                            Text(LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = ts(11, 700, th.sub2))
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("By drink", style = ts(14, 800, th.ink))
                val by = ui.todayEntries.groupBy { it.drink }.mapValues { (_, l) -> l.sumOf { it.ml } }
                val shownDrinks = Drink.entries.filter { !it.premium || (by[it] ?: 0) > 0 || s.premium }
                val mx = maxOf(1, by.values.maxOrNull() ?: 0)
                shownDrinks.forEach { d ->
                    val w by animateFloatAsState((by[d] ?: 0).toFloat() / mx, tween(800), label = "byDrink")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(d.label, style = ts(13, 600, th.ink), modifier = Modifier.width(66.dp), maxLines = 1)
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(10.dp)).background(th.soft)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(w).background(d.color, RoundedCornerShape(10.dp)))
                        }
                        Text(ui.fmt(by[d] ?: 0), style = ts(12, 700, th.sub).copy(textAlign = TextAlign.End), modifier = Modifier.width(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatCell(value: String, label: String) {
    val th = LocalSippo.current
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = ts(18, 800, th.ink), maxLines = 1)
        Text(label, style = ts(11, 600, th.sub2))
    }
}

@Composable
fun HistoryScreen(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    val ctx = LocalContext.current
    val totals = remember(ui.entries) { Progress.dailyTotals(ui.entries) }
    val month = ui.histMonth
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - DayOfWeek.MONDAY.value)
    val sel = ui.selDay ?: ui.today

    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Title("History")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Sym.CHEVRON_LEFT, 22, th.acc, Modifier.tap { vm.shiftMonth(-1) }.padding(4.dp))
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = ts(14, 700, th.acc))
                val canNext = month.isBefore(java.time.YearMonth.now())
                Icon(Sym.CHEVRON_RIGHT, 22, th.acc, Modifier.alpha(if (canNext) 1f else .3f).tap { vm.shiftMonth(1) }.padding(4.dp))
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach {
                        Text(it.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = ts(11, 700, th.sub2).copy(textAlign = TextAlign.Center), modifier = Modifier.weight(1f))
                    }
                }
                val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it).toEpochDay() }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 0 until 7) {
                            val d = week.getOrNull(i)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (d != null) CalendarDay(d, ui, totals, sel) { vm.selectDay(d) }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
            Legend(th.acc, "Goal met")
            Legend(Pal.waterLight, "Partial")
        }
        if (!s.premium) {
            Row(
                Modifier.fillMaxWidth().background(th.inkBg, RoundedCornerShape(20.dp)).press(.98f) { vm.openPaywall(Screen.HIST) }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Sym.HISTORY, 24, Pal.waterLight)
                Column(Modifier.weight(1f)) {
                    Text("Free plan shows the last 7 days", style = ts(14, 800, Color.White))
                    Text("Unlock unlimited history, trends & export", style = ts(12, 400, Color.White.copy(alpha = .75f)))
                }
                Icon(Sym.CHEVRON_RIGHT, 20, Color.White)
            }
        }

        val slide = with(LocalDensity.current) { 14.dp.roundToPx() }
        AnimatedContent(sel, transitionSpec = { (fadeIn(tween(350)) + slideInVertically(tween(350, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) { slide }) togetherWith fadeOut(tween(1)) }, label = "day") { day ->
            DayCard(day, ui, totals)
        }

        Row(
            Modifier.fillMaxWidth().background(th.inkBg, RoundedCornerShape(24.dp)).padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            MascotView(s.mascot, Mood.HAPPY, 70.dp, acc = s.accessory)
            Column(Modifier.weight(1f)) {
                val n = Progress.goalDaysInMonth(totals, s.goalMl, month.atDay(1))
                Text("$n goal ${if (n == 1) "day" else "days"} ${if (month == java.time.YearMonth.now()) "this month" else "in ${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}"}", style = ts(16, 800, Color.White))
                Text("Best streak: ${Progress.bestStreak(totals, s.goalMl)} days", style = ts(12, 400, Color.White.copy(alpha = .75f)))
            }
        }
        if (s.premium) {
            Row(
                Modifier.fillMaxWidth().background(th.card, RoundedCornerShape(20.dp)).press(.98f) {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/csv")
                        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Sippo history")
                        .putExtra(android.content.Intent.EXTRA_TEXT, vm.csv())
                    ctx.startActivity(android.content.Intent.createChooser(send, "Export history"))
                }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Sym.IOS_SHARE, 22, th.acc)
                Text("Export history as CSV", style = ts(14, 700, th.ink), modifier = Modifier.weight(1f))
                Icon(Sym.CHEVRON_RIGHT, 20, th.sub2)
            }
        }
    }
}

@Composable
private fun CalendarDay(day: Long, ui: UiState, totals: Map<Long, Float>, sel: Long, onClick: () -> Unit) {
    val th = LocalSippo.current
    val goal = ui.settings.goalMl
    val f = ((totals[day] ?: 0f) / goal).coerceIn(0f, 1f)
    val met = f >= 1f
    val future = day > ui.today
    val locked = !ui.settings.premium && day < ui.today - 6
    val ringMod = when {
        day == sel -> Modifier.ring(3.dp, th.acc, 17.dp)
        day == ui.today -> Modifier.ring(2.dp, th.ink, 17.dp)
        else -> Modifier
    }
    Box(
        Modifier.size(34.dp).alpha(if (future) .45f else 1f).then(ringMod).clip(CircleShape).background(th.soft)
            .then(if (future) Modifier else Modifier.tap(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(f).background(if (met) th.acc else Pal.waterLight))
        Text("${LocalDate.ofEpochDay(day).dayOfMonth}", style = ts(12, 700, if (met) Color.White else th.ink))
        if (locked) {
            Box(Modifier.fillMaxSize().background(Color(0xDBEEEDF7)), contentAlignment = Alignment.Center) { Icon(Sym.LOCK, 15, th.sub2) }
        }
    }
}

@Composable
private fun Legend(c: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(c, CircleShape))
        Text(label, style = ts(12, 600, LocalSippo.current.sub))
    }
}

@Composable
private fun DayCard(day: Long, ui: UiState, totals: Map<Long, Float>) {
    val th = LocalSippo.current
    val goal = ui.settings.goalMl
    val date = LocalDate.ofEpochDay(day)
    val rows: List<Entry> = ui.entries.filter { it.day == day }.sortedBy { it.minute }
    val tot = totals[day] ?: 0f
    val pc = tot / goal
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(), style = ts(12, 700, th.sub2))
                    Text(if (day == ui.today) "Today" else date.format(DateTimeFormatter.ofPattern("MMMM d")), style = ts(20, 800, th.ink, -.02f))
                }
                val met = pc >= 1f
                Text(
                    if (met) "Goal met" else "${(pc * 100).roundToInt()}%",
                    style = ts(12, 800, if (met) th.acc else Color(0xFF3F7FF5)),
                    modifier = Modifier.background(if (met) Color(0xFFE8E9FF) else Color(0xFFEEF6FF), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ui.fmt(tot), style = ts(28, 800, th.ink, -.02f))
                Text("of ${ui.fmt(goal)}", style = ts(13, 600, th.sub), modifier = Modifier.padding(bottom = 4.dp))
            }
            val w by animateFloatAsState(pc.coerceIn(0f, 1f), tween(600), label = "dayPct")
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(th.soft)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(w).background(th.acc, RoundedCornerShape(8.dp)))
            }
            Column {
                if (rows.isEmpty()) Text("No drinks logged.", style = ts(13, 600, th.sub2), modifier = Modifier.padding(top = 4.dp))
                rows.forEach { e ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(th.soft))
                    Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        DrinkTile(e.drink, 32, 11, 18)
                        Text(e.drink.label, style = ts(14, 700, th.ink), modifier = Modifier.weight(1f))
                        Text(e.timeLabel, style = ts(12, 600, th.sub2))
                        Text(ui.fmt(e.ml), style = ts(13, 700, th.ink).copy(textAlign = TextAlign.End), modifier = Modifier.width(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AwardsScreen(ui: UiState, vm: SippoViewModel) {
    val th = LocalSippo.current
    val s = ui.settings
    val totals = remember(ui.entries) { Progress.dailyTotals(ui.entries) }
    val weekDays = Progress.goalDaysThisWeek(totals, s.goalMl, ui.today)
    val awards = remember(ui.entries, s.goalMl) { Progress.awards(ui.entries, totals, s.goalMl) }
    val tints = listOf(0xFFE3EFFF, 0xFFFFF0DC, 0xFFEFE7FF, 0xFFE4F4E4, 0xFFFFE3EA, 0xFFE3E6FB).map { Color(it) }
    val inks = listOf(Color(0xFF4A8DFF), Color(0xFFF28A1E), Color(0xFF8A6CF5), Color(0xFF4FA85A), Color(0xFFFF6B8B), th.acc)

    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("Awards")
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(th.acc, Color(0xFF8A6CF5))), RoundedCornerShape(24.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("WEEKLY CHALLENGE", style = ts(12, 700, Color.White.copy(alpha = .8f)))
            Text("Hit your goal 5 days this week", style = ts(18, 800, Color.White))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .25f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((weekDays / 5f).coerceIn(0f, 1f)).background(th.card, RoundedCornerShape(10.dp)))
            }
            Text(
                if (s.crownEarned) "Crown unlocked! Find it in the Wardrobe" else "${minOf(weekDays, 5)} of 5 days · unlocks the Crown",
                style = ts(12, 600, Color.White),
            )
        }
        Row(
            Modifier.fillMaxWidth().background(th.card, RoundedCornerShape(22.dp))
                .then(if (s.premium) Modifier else Modifier.press(.98f) { vm.openPaywall(Screen.AWARDS) })
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).background(Color(0xFFE3EFFF), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Sym.AC_UNIT, 24, Pal.water) }
            Column(Modifier.weight(1f)) {
                Text("Streak freeze", style = ts(14, 800, th.ink))
                Text(if (s.premium) "Active · forgives one missed day each week" else "Keep your streak after a missed day", style = ts(11, 600, th.sub))
            }
            if (!s.premium) PlusTag(height = 24.dp) else Icon(Sym.CHECK_CIRCLE, 20, th.acc)
        }
        awards.chunked(2).forEachIndexed { r, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { c, a ->
                    val i = r * 2 + c
                    val done = a.progress >= 1f
                    Column(
                        Modifier.weight(1f).alpha(if (done) 1f else .85f).background(th.card, RoundedCornerShape(22.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(48.dp).background(if (done) tints[i] else Color(0xFFF1F1F6), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Icon(a.icon, 26, if (done) inks[i] else Color(0xFFA5A8C4))
                        }
                        Text(a.name, style = ts(14, 800, th.ink))
                        Text(a.desc, style = ts(11, 600, th.sub))
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(th.soft)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(a.progress).background(if (done) inks[i] else Color(0xFFA5A8C4)))
                        }
                    }
                }
            }
        }
    }
}
