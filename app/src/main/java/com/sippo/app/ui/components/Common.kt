package com.sippo.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.Sym
import com.sippo.app.ui.theme.Icon
import com.sippo.app.ui.theme.ts
import kotlin.math.roundToInt

val SheetEase = CubicBezierEasing(.2f, .9f, .3f, 1f)
val PopEase = CubicBezierEasing(.3f, 1.4f, .5f, 1f)

/** Clickable without ripple that shrinks slightly while pressed (the design's `style-active`). */
fun Modifier.press(scale: Float = .97f, enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) scale else 1f, tween(120), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** CSS `box-shadow: 0 0 0 Npx` ring drawn just outside the bounds. */
fun Modifier.ring(width: Dp, color: Color, radius: Dp): Modifier = drawBehind {
    if (width.value <= 0f || color.alpha == 0f) return@drawBehind
    val w = width.toPx()
    drawRoundRect(
        color, Offset(-w / 2f, -w / 2f), Size(size.width + w, size.height + w),
        CornerRadius(radius.toPx() + w / 2f), style = Stroke(w),
    )
}

/** Selected (3dp accent) vs idle (1dp hairline) ring used by pickers. */
fun Modifier.ring(on: Boolean, acc: Color, line: Color, radius: Dp): Modifier =
    if (on) ring(3.dp, acc, radius) else ring(1.dp, line, radius)

/** Plain tap target with no visual feedback, for rows and list items. */
fun Modifier.tap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

@Composable
fun Card(modifier: Modifier = Modifier, radius: Dp = 24.dp, color: Color = LocalSippo.current.card, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(radius)).background(color), content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ts(13, 800, LocalSippo.current.sub), modifier = modifier.padding(horizontal = 4.dp))
}

/** Rounded segmented choice (activity, interval, sizes). */
@Composable
fun RowScope.Pill(label: String, active: Boolean, radius: Dp = 16.dp, vPad: Dp = 12.dp, size: Int = 13, onClick: () -> Unit) {
    val t = LocalSippo.current
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(radius)).background(if (active) t.acc else t.soft)
            .tap(onClick).padding(vertical = vPad),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = ts(size, 700, if (active) Color.White else t.ink)) }
}

/** The ml/oz segmented toggle. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    val t = LocalSippo.current
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(t.soft).padding(3.dp)) {
        options.forEach { (v, label) ->
            val on = v == selected
            Box(
                Modifier.clip(RoundedCornerShape(11.dp)).background(if (on) t.card else Color.Transparent)
                    .tap { onPick(v) }.padding(horizontal = 16.dp, vertical = 7.dp),
            ) { Text(label, style = ts(13, 700, if (on) t.ink else t.sub2)) }
        }
    }
}

@Composable
fun Switch(on: Boolean, onClick: (() -> Unit)?) {
    val t = LocalSippo.current
    val x by animateDpAsState(if (on) 25.dp else 3.dp, tween(200), label = "switch")
    Box(
        Modifier.size(52.dp, 30.dp).clip(RoundedCornerShape(30.dp)).background(if (on) t.acc else t.line)
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier),
    ) {
        Box(Modifier.offset(x = x, y = 3.dp).size(24.dp).shadow(2.dp, CircleShape).background(t.card, CircleShape))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SippoSlider(value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    val t = LocalSippo.current
    val steps = ((range.last - range.first) / step - 1).coerceAtLeast(0)
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(((it - range.first) / step).roundToInt() * step + range.first) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = if (steps > 200) 0 else steps,
        colors = SliderDefaults.colors(
            thumbColor = t.acc,
            activeTrackColor = t.acc,
            inactiveTrackColor = t.soft,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PlusTag(modifier: Modifier = Modifier, text: String = "PLUS", height: Dp = 22.dp) {
    val t = LocalSippo.current
    Row(
        modifier.height(height).clip(RoundedCornerShape(height / 2)).background(t.inkBg).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(Sym.LOCK, 13, Color.White)
        Text(text, style = ts(10, 800, Color.White))
    }
}

@Composable
fun LockBadge(size: Dp, iconSize: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(LocalSippo.current.inkBg, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Sym.LOCK, iconSize, Color.White)
    }
}

/** Dimmed backdrop plus a bottom sheet that slides up, matching the design's sheets. */
@Composable
fun BoxScope.BottomSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalSippo.current
    AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(200)), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x73141632)).tap(onDismiss))
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(350, easing = SheetEase)) { it },
        exit = slideOutVertically(tween(250)) { it },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(t.card)
                .tap { }
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).background(t.line, RoundedCornerShape(4.dp)))
            content()
        }
    }
}
