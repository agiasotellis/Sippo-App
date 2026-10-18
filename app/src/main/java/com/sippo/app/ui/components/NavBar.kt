package com.sippo.app.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sippo.app.ui.theme.Icon
import com.sippo.app.ui.theme.LocalSippo
import com.sippo.app.ui.theme.Pal
import com.sippo.app.ui.theme.ts

data class NavTab(val key: Any, val icon: String, val label: String)

private val Spring = CubicBezierEasing(.45f, 1.4f, .4f, 1f)
private val SpringStrong = CubicBezierEasing(.45f, 1.55f, .4f, 1f)
private val IconSpring = CubicBezierEasing(.45f, 1.6f, .4f, 1f)
private val Pop = CubicBezierEasing(.3f, 1.6f, .5f, 1f)

/** Blur mask filters only render on hardware canvases from API 28; skip the glow below that. */
private val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

/**
 * Floating black tab bar. A notch is cut out of the top edge under the active tab and slides between
 * tabs; a glowing accent circle sits in it and the active icon lifts into the circle.
 */
@Composable
fun NavBar(tabs: List<NavTab>, active: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val th = LocalSippo.current
    BoxWithConstraints(modifier.fillMaxWidth().height(58.dp)) {
        val w = maxWidth.value
        val pad = 28f
        val tw = (w - pad * 2) / tabs.size
        val notch by animateFloatAsState(pad + (active + .5f) * tw, tween(550, easing = Spring), label = "notch")
        val ind by animateFloatAsState(pad + (active + .5f) * tw, tween(550, easing = SpringStrong), label = "ind")
        val pop = remember { Animatable(1f) }
        LaunchedEffect(active) { pop.snapTo(.6f); pop.animateTo(1f, tween(550, easing = Pop)) }

        Canvas(Modifier.fillMaxSize()) {
            val d = density
            val c = notch
            val path = Path().apply {
                moveTo(22f * d, 0f)
                lineTo((c - 34f) * d, 0f)
                cubicTo((c - 22f) * d, 0f, (c - 26f) * d, 28f * d, c * d, 28f * d)
                cubicTo((c + 26f) * d, 28f * d, (c + 22f) * d, 0f, (c + 34f) * d, 0f)
                lineTo((w - 22f) * d, 0f)
                quadraticTo(w * d, 0f, w * d, 22f * d)
                lineTo(w * d, 36f * d)
                quadraticTo(w * d, 58f * d, (w - 22f) * d, 58f * d)
                lineTo(22f * d, 58f * d)
                quadraticTo(0f, 58f * d, 0f, 36f * d)
                lineTo(0f, 22f * d)
                quadraticTo(0f, 0f, 22f * d, 0f)
                close()
            }
            // drop-shadow(0 10px 14px rgba(31,35,80,.4))
            if (canBlur) drawIntoCanvas { canvas ->
                val p = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = Color(0x661F2350).toArgb()
                    maskFilter = BlurMaskFilter(14f * d, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.save()
                canvas.translate(0f, 10f * d)
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), p)
                canvas.restore()
            }
            drawPath(path, Brush.verticalGradient(listOf(Color(0xFF1A1B2E), Color(0xFF07070C))))
        }

        // Glowing circle + dot under the active tab.
        Box(Modifier.offset(x = (ind - 22f).dp, y = (-20).dp).size(44.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.width / 2f
                val ctr = Offset(r, r)
                if (canBlur) drawIntoCanvas { canvas ->
                    val p = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = th.acc.copy(alpha = .9f).toArgb()
                        maskFilter = BlurMaskFilter(14f * density, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawCircle(ctr.x, ctr.y + 8f * density, r - 4f * density, p)
                }
                drawCircle(th.acc, r, ctr)
                drawCircle(Brush.radialGradient(0f to Color.White.copy(alpha = .35f), .55f to Color.Transparent, center = Offset(r * .7f, r * .6f), radius = r * 1.3f), r, ctr)
                drawCircle(Brush.verticalGradient(.6f to Color.Transparent, 1f to Color.Black.copy(alpha = .18f)), r, ctr)
            }
        }
        Box(Modifier.offset(x = (ind - 2f).dp, y = 48.dp).size(4.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                if (canBlur) drawIntoCanvas { canvas ->
                    val p = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = th.acc.toArgb()
                        maskFilter = BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawCircle(size.width / 2, size.height / 2, size.width, p)
                }
                drawCircle(th.acc)
            }
        }

        Row(Modifier.fillMaxSize().padding(horizontal = pad.dp)) {
            tabs.forEachIndexed { i, tab ->
                val on = i == active
                val lift by animateFloatAsState(if (on) -27f else 0f, tween(500, easing = IconSpring), label = "lift")
                val lop by animateFloatAsState(if (on) 1f else 0f, tween(300), label = "lop")
                val lty by animateFloatAsState(if (on) 0f else 6f, tween(400), label = "lty")
                val col by androidx.compose.animation.animateColorAsState(if (on) Color.White else Pal.navIdle, tween(300), label = "col")
                Box(
                    Modifier.width(tw.dp).fillMaxHeight().tap { onTab(i) }
                        .semantics(mergeDescendants = true) { contentDescription = tab.label; role = Role.Tab; selected = on },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(tab.icon, 20, col, Modifier.graphicsLayer { translationY = lift * density })
                    Text(
                        tab.label,
                        style = ts(10, 800, Color.White, tracking = .03f),
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).clearAndSetSemantics { }
                            .graphicsLayer { alpha = lop; translationY = lty * density },
                    )
                }
            }
        }
    }
}
