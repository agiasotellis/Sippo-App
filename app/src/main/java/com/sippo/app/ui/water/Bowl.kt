package com.sippo.app.ui.water

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sippo.app.data.Accessory
import com.sippo.app.data.Mascot
import com.sippo.app.ui.mascot.MascotView
import com.sippo.app.ui.mascot.Mood
import com.sippo.app.ui.theme.Icon
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.Sym
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/** The bowl's spring/wave simulation, in the prototype's 270×270 units, stepped at a fixed 60 Hz. */
class BowlSim {
    var lvl = -1f
    var tilt = 0f
    var tv = 0f
    var tt = 0f
    var amp = 3f
    var ph = 0f
    var pour = 0f
    var pouring = false
    var pt = 0f
    var yc = 270f
    var fill = 0f
    var y0 = 270f
    var slope = 0f
    var a = 0f

    fun slosh(v: Float) {
        tv += v
        amp += abs(v) * 40f
    }

    fun step(dt: Float, total: Float, goal: Int, factor: Float, sensor: Float, touching: Boolean) {
        if (pouring) {
            pt += dt
            pour += dt * (.15f + min(.15f, pt / 1000f * .1f))
        }
        if (!touching) tt = sensor
        val target = min(1f, (total + pour * factor) / goal)
        if (lvl < 0f) lvl = target
        lvl += (target - lvl) * .06f
        tv += (tt - tilt) * .05f
        tv *= .9f
        tilt += tv
        val ampT = if (pouring) 7f else 2.6f
        amp += (ampT - amp) * .04f
        ph += .05f + (if (pouring) .06f else 0f) + abs(tv) * .6f
        y0 = H - lvl * (H - 14f) + if (lvl < .01f) 14f else 0f
        slope = tan(tilt + tv * 2f)
        a = amp * min(1f, lvl * 6f)
        yc = surf(W / 2f, 0f, 1f)
        fill = H - yc
    }

    fun surf(x: Float, o: Float, k: Float) =
        y0 + slope * (x - W / 2f) + a * k * sin(x * .035f + ph + o) + a * .5f * k * sin(x * .075f - ph * 1.4f + o)

    fun path(o: Float, k: Float, dy: Float): Path = Path().apply {
        moveTo(0f, H)
        var x = 0f
        while (x <= W) { lineTo(x, surf(x, o, k) + dy); x += 6f }
        lineTo(W, H)
        close()
    }

    companion object {
        const val W = 270f
        const val H = 270f
    }
}

internal data class Bubble(val left: Float, val size: Float, val dur: Float, val delay: Float)

internal val Bubbles = listOf(
    Bubble(.18f, 7f, 3.2f, 0f), Bubble(.34f, 5f, 2.6f, -1f), Bubble(.52f, 9f, 3.8f, -2f),
    Bubble(.66f, 6f, 2.9f, -.5f), Bubble(.80f, 8f, 3.4f, -1.6f), Bubble(.44f, 4f, 2.4f, -2.2f),
)

private val EaseIn = CubicBezierEasing(.42f, 0f, 1f, 1f)

/** Rising bubbles in a strip [width] wide whose bottom is at [bottom]; drawn only above [clipTop]. */
internal fun DrawScope.bubbles(t: Float, width: Float, bottom: Float, clipTop: Float, scale: Float = 1f) {
    clipRect(0f, clipTop, width, bottom) {
        Bubbles.forEach { b ->
            val x = (t - b.delay) / b.dur
            val p = x - floor(x)
            val rise = EaseIn.transform(p) * 230f * scale
            val alpha = if (p < .15f) .9f * EaseIn.transform(p / .15f) else .9f * (1f - EaseIn.transform((p - .15f) / .85f))
            val s = b.size * scale
            val c = Offset(b.left * width + s / 2f, bottom - 4f * scale - s / 2f - rise)
            drawCircle(Color.White.copy(alpha = .2f * alpha / .9f), s / 2f, c)
            drawCircle(Color.White.copy(alpha = .8f * alpha / .9f), s / 2f - .75f * scale, c, style = Stroke(1.5f * scale))
        }
    }
}

private val SplashEase = CubicBezierEasing(.2f, .8f, .4f, 1f)
private val DropAngles = floatArrayOf(-70f, -50f, -30f, -12f, 8f, 26f, 46f, 66f)

/** Ripple ring plus eight droplets thrown outward from ([cx],[cy]); [age] is seconds since the splash. */
internal fun DrawScope.splash(age: Float, cx: Float, cy: Float) {
    if (age < 0f || age > .8f) return
    if (age < .7f) {
        val p = age / .7f
        val e = 1f - (1f - p) * (1f - p) // ease-out
        val sc = .2f + 1.4f * e
        val alpha = .9f * (1f - e)
        drawOval(Color.White.copy(alpha = alpha), Offset(cx - 38.5f * sc, cy - 10.5f * sc), Size(77f * sc, 21f * sc), style = Stroke(3f * sc))
    }
    DropAngles.forEachIndexed { i, r ->
        val local = age - (i % 3) * .04f
        val p = (local / .65f).coerceIn(0f, 1f)
        if (p >= 1f) return@forEachIndexed
        val e = SplashEase.transform(p)
        val size = (6 + (i % 3) * 2).toFloat() * (1f - .6f * e)
        rotate(r, Offset(cx, cy)) {
            val c = Offset(cx, cy - 58f * e)
            val col = Color(0xFF9FD2FF).copy(alpha = 1f - e)
            drawCircle(col, size / 2f, c)
            drawPath(Path().apply {
                moveTo(c.x - size / 2f, c.y); lineTo(c.x, c.y + size * .72f); lineTo(c.x + size / 2f, c.y); close()
            }, col)
        }
    }
}

/**
 * The home screen's fill-up bowl: layered waves, bubbles, pour stream, splash, cup and the floating mascot.
 * [onPourTick] receives the running poured amount (ml) while pouring.
 */
@Composable
fun Bowl(
    size: Dp,
    total: Float,
    goal: Int,
    pouring: Boolean,
    pourColor: Color,
    pourFactor: Float,
    splashCount: Int,
    mascot: Mascot,
    mood: Mood,
    acc: Accessory,
    tilt: Tilt,
    sim: BowlSim,
    onPourTick: (Int) -> Unit,
) {
    val th = LocalSippo.current
    val frame = remember { mutableIntStateOf(0) }
    val clock = remember { floatArrayOf(0f, -10f) } // [now, splashStart] in seconds
    val touching = remember { booleanArrayOf(false) }
    val totalNow by rememberUpdatedState(total)
    val goalNow by rememberUpdatedState(goal)
    val factorNow by rememberUpdatedState(pourFactor)
    val tick by rememberUpdatedState(onPourTick)

    LaunchedEffect(sim) {
        var last = withFrameNanos { it }
        var acc = 0f
        var lastMl = -1
        while (true) {
            withFrameNanos { now ->
                acc += min(100f, (now - last) / 1e6f)
                last = now
                while (acc >= 16.67f) {
                    sim.step(16.67f, totalNow, goalNow, factorNow, tilt.target, touching[0])
                    acc -= 16.67f
                    clock[0] += .01667f
                }
                if (sim.pouring) {
                    val ml = Math.round(sim.pour / 10f) * 10
                    if (ml != lastMl) { lastMl = ml; tick(ml) }
                } else lastMl = -1
                frame.intValue++
            }
        }
    }
    // Slosh on every logged drink, and drain-slosh when the total drops (undo, edit, delete).
    LaunchedEffect(splashCount) {
        if (splashCount > 0) {
            clock[1] = clock[0]
            sim.slosh(((Math.random() - .5) * .12).toFloat())
            sim.amp += 6f
        }
    }
    val prevTotal = remember { floatArrayOf(total) }
    LaunchedEffect(total) {
        if (total < prevTotal[0] - .5f) { sim.slosh(-.08f); sim.amp += 5f }
        prevTotal[0] = total
    }

    val cupT = remember { Animatable(0f) }
    LaunchedEffect(pouring) { cupT.animateTo(if (pouring) 1f else 0f, tween(350, easing = CubicBezierEasing(.3f, 1.4f, .5f, 1f))) }
    val cupAlpha by animateFloatAsState(if (pouring) 1f else 0f, tween(250, easing = LinearEasing), label = "cup")
    val k = size.value / BowlSim.W

    Box(Modifier.size(size)) {
        // Pour cup, centred above the bowl and tipping toward the middle.
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-44 * k).dp).size((32 * k).dp)
                .graphicsLayer {
                    val p = cupT.value
                    translationX = 7f * k * density * p
                    translationY = (-20f + 14f * p) * k * density
                    rotationZ = 145f * p
                    alpha = cupAlpha
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Sym.LOCAL_DRINK, (32 * k).toInt(), pourColor) }

        // Water layers clipped to the bowl.
        Canvas(
            Modifier.fillMaxSize().clip(CircleShape).background(th.bowl)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { touching[0] = true },
                        onDragEnd = { touching[0] = false },
                        onDragCancel = { touching[0] = false },
                    ) { change, dx ->
                        sim.tt = -((change.position.x / this.size.width) - .5f) * .55f
                        sim.slosh(-dx / density * .0015f)
                    }
                },
        ) {
            frame.intValue
            val s = this.size.width / BowlSim.W
            scale(s, s, pivot = Offset.Zero) {
                drawPath(sim.path(2.1f, 1.3f, -4f), Color(0xFFBFE0FF))
                val top = max(0f, sim.y0 - abs(sim.slope) * BowlSim.W / 2f - sim.a * 1.5f)
                drawPath(sim.path(0f, 1f, 0f), Brush.verticalGradient(0f to Color(0xFF8FCAFF), 1f to Color(0xFF3F7FF5), startY = top, endY = BowlSim.H))
                bubbles(clock[0], BowlSim.W, BowlSim.H, BowlSim.H - max(0f, sim.fill))
                if (sim.pouring) {
                    val w = 10f + sin(sim.ph * 3f) * 2f
                    val h = max(0f, sim.yc + 18f)
                    drawRoundRectStream(BowlSim.W / 2f - w / 2f, -18f, w, h, pourColor)
                }
            }
        }

        // Mascot floats on the surface, inside a circle inset 6 from the rim.
        Box(Modifier.fillMaxSize().padding((6 * k).dp).clip(CircleShape)) {
            val mSize = (140 * k).dp
            Box(
                Modifier.align(Alignment.BottomCenter).requiredSize(mSize)
                    .graphicsLayer {
                        frame.intValue
                        val bottom = (sim.fill - 36f).coerceIn(-14f, 108f)
                        translationY = -bottom * k * density
                        rotationZ = (sim.tilt + sim.tv * 3f) * -45f
                        transformOrigin = TransformOrigin(.5f, .85f)
                    },
            ) { MascotView(mascot, mood, mSize, acc = acc) }
        }

        // Front wave, rim and splash.
        Canvas(Modifier.fillMaxSize()) {
            frame.intValue
            val s = this.size.width / BowlSim.W
            scale(s, s, pivot = Offset.Zero) {
                clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, BowlSim.W, BowlSim.H)) }) {
                    drawPath(sim.path(.6f, 1f, 3f), Color(0x6B4A8DFF))
                }
                drawCircle(th.rim, BowlSim.W / 2f - 3f, Offset(BowlSim.W / 2f, BowlSim.H / 2f), style = Stroke(6f))
                splash(clock[0] - clock[1], BowlSim.W / 2f, sim.yc)
            }
        }
    }
}

private fun DrawScope.drawRoundRectStream(l: Float, t: Float, w: Float, h: Float, color: Color) {
    val r = min(8f, w / 2f)
    val path = Path().apply {
        moveTo(l, t); lineTo(l + w, t); lineTo(l + w, t + h - r)
        quadraticTo(l + w, t + h, l + w - r, t + h); lineTo(l + r, t + h)
        quadraticTo(l, t + h, l, t + h - r); close()
    }
    drawPath(path, Brush.horizontalGradient(0f to Color.White.copy(alpha = .5f), .4f to color, 1f to color, startX = l, endX = l + w))
}
