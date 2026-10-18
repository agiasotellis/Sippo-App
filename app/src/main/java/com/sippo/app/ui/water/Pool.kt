package com.sippo.app.ui.water

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sippo.app.data.Mascot
import com.sippo.app.ui.mascot.MascotView
import com.sippo.app.ui.mascot.Mood
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/** Three mascots dropping into a full-screen pool and bobbing; all lengths are dp. */
class PoolSim(val w: Float, val h: Float, val m: Float) {
    class Body(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f, var floating: Boolean = false)

    var tilt = 0f
    var tv = 0f
    var tt = 0f
    var amp = 4f
    var ph = 0f
    var drag = false
    var sx = 0f
    private val u = m / 110f // the prototype's constants were tuned for 110px mascots
    val bodies = listOf(Body(w * .22f, -80f * u), Body(w * .5f, -300f * u), Body(w * .78f, -520f * u))
    private val base get() = h * .5f
    var slope = 0f

    fun surf(x: Float, o: Float = 0f, k: Float = 1f) =
        base + slope * (x - w / 2f) + amp * k * sin(x * .03f / u + ph + o) + amp * .5f * k * sin(x * .07f / u - ph * 1.4f + o)

    fun step(sensor: Float) {
        if (!drag) tt = sensor
        tv += (tt - tilt) * .05f; tv *= .9f; tilt += tv
        amp += (3f - amp) * .03f
        ph += .045f + abs(tv) * .6f
        slope = tan(tilt + tv * 2f)
        bodies.forEachIndexed { i, b ->
            val sy = surf(b.x) - 64f * u
            if (!b.floating) {
                b.vy += .55f * u; b.y += b.vy
                if (b.y >= sy) { b.floating = true; b.vy *= .35f; amp += 9f; tv += (i - 1) * .03f }
            } else {
                b.vy += (sy - b.y) * .08f; b.vy *= .86f; b.y += b.vy
                b.vx += -slope * .22f * u + sin(ph * .7f + i * 2f) * .01f
            }
            b.vx *= .95f; b.x += b.vx
            val edge = 58f * u
            if (b.x < edge) { b.x = edge; b.vx = abs(b.vx) * .5f }
            if (b.x > w - edge) { b.x = w - edge; b.vx = -abs(b.vx) * .5f }
        }
        val minD = 100f * u
        for (i in 0 until 3) for (j in i + 1 until 3) {
            val a = bodies[i]; val b = bodies[j]
            if (!a.floating || !b.floating) continue
            val d = b.x - a.x
            if (abs(d) < minD) {
                val p = (minD - abs(d)) / 2f * (if (d >= 0) 1f else -1f)
                a.x -= p; b.x += p
                val t = a.vx; a.vx = b.vx * .7f; b.vx = t * .7f
            }
        }
    }

    fun angle(i: Int): Float {
        val b = bodies[i]
        return if (b.floating) {
            val dy = surf(b.x + 6f) - surf(b.x - 6f)
            Math.toDegrees(atan2(dy, 12f).toDouble()).toFloat() * .9f
        } else b.vy / u * .4f * (i - 1)
    }

    fun path(o: Float, k: Float, dy: Float) = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x <= w) { lineTo(x, surf(x, o, k) + dy); x += 6f }
        lineTo(w, surf(w, o, k) + dy)
        lineTo(w, h); close()
    }
}

/**
 * Full-screen pool. [key] restarts the drop-in (Replay onboarding). Drags outside [ignoreBottom]
 * (the CTA area) slosh the water; device tilt does too.
 */
@Composable
fun Pool(key: Int, ignoreBottom: Float, modifier: Modifier = Modifier, overlay: @Composable () -> Unit) {
    val tilt = rememberTilt()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        val mSize = (110f * w / 362f).coerceIn(96f, 150f)
        val sim = remember(key, w, h) { PoolSim(w, h, mSize) }
        val frame = remember { mutableIntStateOf(0) }
        val clock = remember { floatArrayOf(0f) }
        LaunchedEffect(sim) {
            var last = withFrameNanos { it }
            var acc = 0f
            while (true) withFrameNanos { now ->
                acc += min(100f, (now - last) / 1e6f); last = now
                while (acc >= 16.67f) { sim.step(tilt.target); acc -= 16.67f; clock[0] += .01667f }
                frame.intValue++
            }
        }

        Box(
            Modifier.fillMaxSize().pointerInput(sim) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: continue
                        val x = c.position.x / density
                        when {
                            c.pressed && !c.previousPressed -> {
                                if (c.position.y / density < h - ignoreBottom) { sim.drag = true; sim.sx = x; sim.amp += 4f }
                            }
                            c.pressed && sim.drag -> {
                                val dx = (c.position.x - c.previousPosition.x) / density
                                sim.tt = -((x - sim.sx) * .004f).coerceIn(-.42f, .42f)
                                sim.tv += -dx * .0012f
                                sim.amp += abs(dx) * .06f
                            }
                            !c.pressed -> { sim.drag = false }
                        }
                    }
                }
            },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                frame.intValue
                scale(density, density, pivot = Offset.Zero) {
                    drawPath(sim.path(2.1f, 1.3f, -5f), Color(0xFFBFE0FF))
                    drawPath(sim.path(0f, 1f, 0f), Brush.verticalGradient(.4f to Color(0xFF8FCAFF), 1f to Color(0xFF3A5FE6), startY = sim.surf(w / 2f) - 20f, endY = h))
                    bubbles(clock[0], w, h, h - 330f)
                }
            }
            Mascot.entries.forEachIndexed { i, m ->
                Box(
                    Modifier.align(Alignment.TopStart).requiredSize(mSize.dp)
                        .graphicsLayer {
                            frame.intValue
                            val b = sim.bodies[i]
                            translationX = (b.x - mSize / 2f) * density
                            translationY = (b.y - mSize / 2f) * density
                            rotationZ = sim.angle(i)
                            transformOrigin = TransformOrigin(.5f, .7f)
                        },
                ) { MascotView(m, Mood.IDLE, mSize.dp) }
            }
            Canvas(Modifier.fillMaxSize()) {
                frame.intValue
                scale(density, density, pivot = Offset.Zero) {
                    drawPath(sim.path(.6f, 1f, 4f), Color(0x734A8DFF))
                }
            }
            overlay()
        }
    }
}
