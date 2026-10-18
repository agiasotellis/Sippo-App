package com.sippo.app.ui.mascot

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import com.sippo.app.data.Accessory
import com.sippo.app.data.Mascot
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class Mood { IDLE, HAPPY, DRINK }

/**
 * Ottie, Pingo and Axel, drawn entirely in code on a 160×160 artboard and scaled to [size].
 * Idle bobs, happy hops, drink gulps three times; eyes blink, flippers flap and gills sway.
 */
@Composable
fun MascotView(kind: Mascot, mood: Mood, size: Dp, modifier: Modifier = Modifier, acc: Accessory = Accessory.NONE) {
    val clock = remember { mutableFloatStateOf(0f) }
    val moodStart = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.floatValue = (it - start) / 1e9f }
    }
    LaunchedEffect(mood) { moodStart.longValue = (clock.floatValue * 1000).toLong() }

    Canvas(modifier.size(size)) {
        val t = clock.floatValue
        val sinceMood = t - moodStart.longValue / 1000f
        val s = this.size.width / 160f
        scale(s, s, pivot = Offset.Zero) {
            drawMascot(kind, mood, acc, t, sinceMood)
        }
    }
}

private val CssEase = CubicBezierEasing(.42f, 0f, .58f, 1f)

/** Piecewise keyframes with CSS ease-in-out between stops. [p] in 0..1. */
private fun kf(p: Float, times: FloatArray, values: FloatArray): Float {
    for (i in 1 until times.size) {
        if (p <= times[i]) {
            val f = (p - times[i - 1]) / (times[i] - times[i - 1])
            return values[i - 1] + (values[i] - values[i - 1]) * CssEase.transform(f.coerceIn(0f, 1f))
        }
    }
    return values.last()
}

private fun phase(t: Float, period: Float, delay: Float = 0f): Float {
    val x = (t - delay) / period
    return x - kotlin.math.floor(x)
}

private fun DrawScope.drawMascot(kind: Mascot, mood: Mood, acc: Accessory, t: Float, sinceMood: Float) {
    // Body motion, transform-origin 50% 90%.
    var ty = 0f
    var sx = 1f
    var sy = 1f
    when (mood) {
        Mood.IDLE -> ty = kf(phase(t, 3.2f), floatArrayOf(0f, .5f, 1f), floatArrayOf(0f, -6f, 0f))
        Mood.HAPPY -> {
            val p = phase(t, .75f)
            val tm = floatArrayOf(0f, .3f, .62f, .8f, 1f)
            ty = kf(p, tm, floatArrayOf(0f, -18f, 0f, -3f, 0f))
            sx = kf(p, tm, floatArrayOf(1f, .95f, 1.07f, 1f, 1f))
            sy = kf(p, tm, floatArrayOf(1f, 1.06f, .93f, 1f, 1f))
        }
        Mood.DRINK -> if (sinceMood < 1.35f) {
            val p = phase(sinceMood, .45f)
            val tm = floatArrayOf(0f, .4f, .7f, 1f)
            sx = kf(p, tm, floatArrayOf(1f, 1.09f, .95f, 1f))
            sy = kf(p, tm, floatArrayOf(1f, .9f, 1.06f, 1f))
        }
    }
    withTransform({
        translate(0f, ty)
        scale(sx, sy, pivot = Offset(80f, 144f))
    }) {
        when (kind) {
            Mascot.OTTIE -> ottieBack()
            Mascot.PINGO -> pingoBack(t)
            Mascot.AXEL -> axelBack(t)
        }
        val face = when (kind) { Mascot.OTTIE -> Offset(50f, 62f); Mascot.PINGO -> Offset(50f, 58f); Mascot.AXEL -> Offset(50f, 68f) }
        translate(face.x, face.y) { face(mood, acc, t) }
        when (kind) {
            Mascot.OTTIE -> {
                oval(73f, 78f, 14f, 9f, Color(0xFF3A2418))
                oval(52f, 124f, 18f, 14f, Color(0xFF7D4F33))
                oval(90f, 124f, 18f, 14f, Color(0xFF7D4F33))
            }
            Mascot.PINGO -> {
                rotate(45f, Offset(80.5f, 80.5f)) { rr(74f, 74f, 13f, 13f, Color(0xFFFFA53D), 3f) }
                oval(50f, 140f, 22f, 11f, Color(0xFFFFA53D))
                oval(88f, 140f, 22f, 11f, Color(0xFFFFA53D))
            }
            Mascot.AXEL -> Unit
        }
        val top = when (kind) { Mascot.OTTIE -> 38f; Mascot.PINGO -> 22f; Mascot.AXEL -> 40f }
        translate(40f, top - 30f) { accessory(acc) }
    }
}

private fun DrawScope.ottieBack() {
    rrc(44f, 112f, 72f, 44f, Color(0xFF94603F), 36f, 36f, 18f, 18f)
    oval(18f, 56f, 20f, 14f, Color(0xFF7D4F33))
    oval(122f, 56f, 20f, 14f, Color(0xFF7D4F33))
    rrp(22f, 38f, 116f, 92f, radial(22f, 38f, 116f, 92f, .4f, .35f, Color(0xFFC48E62), Color(0xFFA06A45), .75f), .52f, .52f, .48f, .48f)
    oval(40f, 76f, 80f, 46f, Color(0xFFF3DCC0))
    val wh = Color(0xFF5A3A26)
    rotate(8f, Offset(43f, 91f)) { rr(30f, 90f, 26f, 2f, wh, 2f) }
    rotate(-6f, Offset(43f, 99f)) { rr(30f, 98f, 26f, 2f, wh, 2f) }
    rotate(-8f, Offset(117f, 91f)) { rr(104f, 90f, 26f, 2f, wh, 2f) }
    rotate(6f, Offset(117f, 99f)) { rr(104f, 98f, 26f, 2f, wh, 2f) }
}

private fun DrawScope.pingoBack(t: Float) {
    val p = phase(t, 1.6f)
    val tm = floatArrayOf(0f, .5f, 1f)
    val flipper = Color(0xFF1C2040)
    rotate(kf(p, tm, floatArrayOf(18f, 38f, 18f)), Offset(31f, 66f)) { oval(20f, 66f, 22f, 52f, flipper) }
    rotate(kf(p, tm, floatArrayOf(-18f, -38f, -18f)), Offset(129f, 66f)) { oval(118f, 66f, 22f, 52f, flipper) }
    rrc(30f, 20f, 100f, 126f, radial(30f, 20f, 100f, 126f, .4f, .3f, Color(0xFF3A4170), Color(0xFF20243F), .75f), 50f, 50f, 44f, 44f)
    rrp(42f, 44f, 76f, 96f, SolidColor(Color(0xFFF7F8FF)), .5f, .5f, .45f, .45f)
}

private fun DrawScope.axelBack(t: Float) {
    rotate(-22f, Offset(121f, 126f)) { oval(98f, 116f, 46f, 20f, Color(0xFFFFC2D3)) }
    rrc(46f, 106f, 68f, 44f, Color(0xFFFFB3C7), 34f, 34f, 20f, 20f)
    oval(60f, 116f, 40f, 28f, Color(0xFFFFDBE5))
    val tm = floatArrayOf(0f, .5f, 1f)
    val sway = floatArrayOf(-8f, 8f, -8f)
    rotate(kf(phase(t, 2.6f), tm, sway), Offset(44f, 69f)) { gills(0f, 44f) }
    scale(-1f, 1f, pivot = Offset(138f, 69f)) {
        rotate(kf(phase(t, 2.6f, -1.3f), tm, sway), Offset(160f, 69f)) { gills(116f, 44f) }
    }
    oval(26f, 38f, 108f, 82f, radial(26f, 38f, 108f, 82f, .4f, .35f, Color(0xFFFFDCE5), Color(0xFFFF9FBA), .78f))
    rotate(-20f, Offset(53f, 53f)) { oval(44f, 48f, 18f, 10f, Color(0x99FFFFFF)) }
}

private fun DrawScope.gills(x: Float, y: Float) {
    translate(x, y) {
        rotate(28f, Offset(24f, 8.5f)) { rr(10f, 4f, 28f, 9f, Color(0xFFFF6F99), 6f) }
        rr(4f, 20f, 32f, 9f, Color(0xFFFF85A8), 6f)
        rotate(-28f, Offset(24f, 40.5f)) { rr(10f, 36f, 28f, 9f, Color(0xFFFF6F99), 6f) }
    }
}

private val Ink = Color(0xFF1F2350)

private fun DrawScope.face(mood: Mood, acc: Accessory, t: Float) {
    if (mood != Mood.HAPPY) {
        val b = phase(t, 4.6f)
        val blink = if (b < .91f) 1f else kf(b, floatArrayOf(.91f, .95f, 1f), floatArrayOf(1f, .1f, 1f))
        for (x in floatArrayOf(8f, 40f)) {
            scale(1f, blink, pivot = Offset(x + 6f, 7.5f)) {
                oval(x, 0f, 12f, 15f, Ink)
                oval(x + 2f, 2f, 5f, 5f, Color.White)
            }
        }
    }
    when (mood) {
        Mood.HAPPY -> {
            // ^ ^ eyes: 18×10 boxes with a 3.5px top border and fully rounded top corners.
            for (x in floatArrayOf(5f, 37f)) {
                drawArc(Ink, 180f, 180f, false, Offset(x + 1.75f, 5.75f), Size(14.5f, 14.5f), style = Stroke(3.5f, cap = StrokeCap.Butt))
            }
            val mouth = rrPath(21f, 19f, 18f, 12f, 2f, 2f, 12f, 12f)
            drawPath(mouth, Ink)
            clipPath(mouth) { oval(25f, 25f, 10f, 8f, Color(0xFFFF7F9E)) }
        }
        Mood.IDLE -> drawPath(rrPath(24f, 20f, 12f, 7f, 0f, 0f, 8f, 8f), Ink)
        Mood.DRINK -> oval(25f, 19f, 10f, 10f, Ink)
    }
    oval(-6f, 15f, 13f, 7f, Color(0x80FF6E96))
    oval(53f, 15f, 13f, 7f, Color(0x80FF6E96))
    if (acc == Accessory.SHADES) {
        for (x in floatArrayOf(0f, 34f)) {
            oval(x, -5f, 26f, 24f, Color(0x8C282E6E))
            drawOval(Ink, Offset(x + 1.5f, -3.5f), Size(23f, 21f), style = Stroke(3f))
        }
        drawRect(Ink, Offset(24f, 3f), Size(12f, 3f))
    }
}

private fun DrawScope.accessory(acc: Accessory) {
    when (acc) {
        Accessory.CAP -> {
            rrc(14f, 8f, 52f, 30f, Color(0xFF5B63EE), 26f, 26f, 4f, 4f)
            rr(44f, 31f, 34f, 8f, Color(0xFF3F47D6), 8f)
            oval(37f, 4f, 7f, 7f, Color(0xFF3F47D6))
        }
        Accessory.CROWN -> {
            val gold = Color(0xFFFFC93D)
            for ((x, y) in listOf(21f to 14f, 34f to 9f, 47f to 14f)) {
                rotate(45f, Offset(x + 6.5f, y + 6.5f)) { drawRect(gold, Offset(x, y), Size(13f, 13f)) }
            }
            rr(20f, 21f, 41f, 17f, gold, 3f)
            oval(36f, 25f, 9f, 9f, Color(0xFFE85D75))
        }
        Accessory.BOW -> {
            for (x in floatArrayOf(44f, 62f)) {
                rotate(45f, Offset(x + 8.5f, 26.5f)) { rr(x, 18f, 17f, 17f, Color(0xFFFF6B8B), 4f) }
            }
            oval(56f, 21f, 11f, 11f, Color(0xFFE8476D))
        }
        else -> Unit
    }
}

// ---- CSS box helpers -------------------------------------------------------------------------

private fun DrawScope.oval(l: Float, t: Float, w: Float, h: Float, c: Color) = drawOval(c, Offset(l, t), Size(w, h))

private fun DrawScope.oval(l: Float, t: Float, w: Float, h: Float, b: Brush) = drawOval(b, Offset(l, t), Size(w, h))

private fun DrawScope.rr(l: Float, t: Float, w: Float, h: Float, c: Color, r: Float) =
    drawPath(rrPath(l, t, w, h, r, r, r, r), c)

private fun DrawScope.rrc(l: Float, t: Float, w: Float, h: Float, c: Color, tl: Float, tr: Float, br: Float, bl: Float) =
    drawPath(rrPath(l, t, w, h, tl, tr, br, bl), c)

private fun DrawScope.rrc(l: Float, t: Float, w: Float, h: Float, b: Brush, tl: Float, tr: Float, br: Float, bl: Float) =
    drawPath(rrPath(l, t, w, h, tl, tr, br, bl), b)

/** Percentage corner radii (elliptical, like `border-radius: 52% 52% 48% 48%`). */
private fun DrawScope.rrp(l: Float, t: Float, w: Float, h: Float, b: Brush, tl: Float, tr: Float, br: Float, bl: Float) {
    drawPath(cssRoundRect(l, t, w, h, CornerRadius(tl * w, tl * h), CornerRadius(tr * w, tr * h), CornerRadius(br * w, br * h), CornerRadius(bl * w, bl * h)), b)
}

private fun rrPath(l: Float, t: Float, w: Float, h: Float, tl: Float, tr: Float, br: Float, bl: Float) =
    cssRoundRect(l, t, w, h, CornerRadius(tl), CornerRadius(tr), CornerRadius(br), CornerRadius(bl))

/** Rounded rect that shrinks over-large radii the way CSS does (one shared factor for all corners). */
internal fun cssRoundRect(l: Float, t: Float, w: Float, h: Float, tl: CornerRadius, tr: CornerRadius, br: CornerRadius, bl: CornerRadius): Path {
    fun ratio(len: Float, a: Float, b: Float) = if (a + b > len) len / (a + b) else 1f
    val f = min(
        min(ratio(w, tl.x, tr.x), ratio(w, bl.x, br.x)),
        min(ratio(h, tl.y, bl.y), ratio(h, tr.y, br.y)),
    )
    fun CornerRadius.s() = CornerRadius(x * f, y * f)
    return Path().apply { addRoundRect(RoundRect(l, t, l + w, t + h, tl.s(), tr.s(), br.s(), bl.s())) }
}

/** `radial-gradient(circle at cx cy, a, b stop)` over a box; radius reaches the farthest corner. */
private fun radial(l: Float, t: Float, w: Float, h: Float, cx: Float, cy: Float, a: Color, b: Color, stop: Float): Brush {
    val c = Offset(l + cx * w, t + cy * h)
    val r = hypot(max(c.x - l, l + w - c.x), max(c.y - t, t + h - c.y))
    return Brush.radialGradient(0f to a, stop to b, 1f to b, center = c, radius = r)
}
