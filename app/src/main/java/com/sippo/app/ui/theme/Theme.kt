package com.sippo.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sippo.app.R

@Immutable
data class SippoColors(
    val key: String,
    val name: String,
    val dark: Boolean,
    val rim: Color,
    val bg: Color,
    val card: Color,
    val ink: Color,
    val inkBg: Color,
    val sub: Color,
    val sub2: Color,
    val acc: Color,
    val soft: Color,
    val soft2: Color,
    val bowl: Color,
    val line: Color,
)

object Themes {
    val lavender = SippoColors("lavender", "Lavender", false, Color.White, Color(0xFFF4F3FB), Color.White, Color(0xFF1F2350), Color(0xFF1F2350), Color(0xFF6B6F93), Color(0xFF8A8EB0), Color(0xFF5B63EE), Color(0xFFEEEDF7), Color(0xFFF6F6FB), Color(0xFFE4E6FB), Color(0xFFD9D9E8))
    val midnight = SippoColors("midnight", "Midnight", true, Color(0xFF2C2F50), Color(0xFF0F1020), Color(0xFF1B1D33), Color(0xFFEEF0FF), Color(0xFF5B63EE), Color(0xFFA3A7CC), Color(0xFF7D81A8), Color(0xFF7D84FF), Color(0xFF262945), Color(0xFF22243C), Color(0xFF1F2240), Color(0xFF33365A))
    val sunset = SippoColors("sunset", "Sunset", false, Color.White, Color(0xFFFFF4EF), Color.White, Color(0xFF3A1F2B), Color(0xFF3A1F2B), Color(0xFF8A6470), Color(0xFFA88893), Color(0xFFFF6F61), Color(0xFFFBE6DF), Color(0xFFFFF0EA), Color(0xFFFFE4DA), Color(0xFFF0D2C8))
    val ocean = SippoColors("ocean", "Ocean", false, Color.White, Color(0xFFEEF8F8), Color.White, Color(0xFF0F2F3A), Color(0xFF0F2F3A), Color(0xFF557580), Color(0xFF7A97A0), Color(0xFF12A3A8), Color(0xFFDCF0F0), Color(0xFFE8F6F6), Color(0xFFD6EEF0), Color(0xFFCFE5E6))
    val all = listOf(lavender, midnight, sunset, ocean)
    fun of(key: String) = all.firstOrNull { it.key == key } ?: lavender
}

/** Fixed palette shared by every theme. */
object Pal {
    val gold = Color(0xFFFFC93D)
    val water = Color(0xFF4A8DFF)
    val waterLight = Color(0xFF9FD2FF)
    val mute = Color(0xFFA0A3C2)
    val editIcon = Color(0xFFB4B7D2)
    val navIdle = Color(0xFF7D80A0)
    val scrim = Color(0x73141632) // rgba(20,22,50,.45)
    val green = Color(0xFF7BE0A8)
    val navy = Color(0xFF1F2350)
    val navyLight = Color(0xFF3B3F9E)
}

val LocalSippo = staticCompositionLocalOf { Themes.lavender }

val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val SymbolsFont = FontFamily(Font(R.font.material_symbols_rounded))

@Composable
fun SippoTheme(colors: SippoColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSippo provides colors,
        LocalTextStyle provides TextStyle(
            fontFamily = Jakarta,
            color = colors.ink,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        content = content,
    )
}

/** Shorthand for the CSS-like type ramp used throughout the design. */
fun ts(size: Int, weight: Int = 700, color: Color = Color.Unspecified, tracking: Float = 0f, lineHeight: Float = 0f) = TextStyle(
    fontFamily = Jakarta,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    color = color,
    letterSpacing = tracking.em,
    lineHeight = if (lineHeight > 0f) lineHeight.em else TextUnit.Unspecified,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/** A Material Symbols glyph. Sized in dp so icons keep their layout under large font scales. */
@Composable
fun Icon(glyph: String, size: Int, color: Color, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.dp.toSp() }
    // Glyphs are private-use code points; keep them out of the accessibility tree.
    Box(modifier.wrapContentSize().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Text(
            glyph,
            style = TextStyle(
                fontFamily = SymbolsFont,
                fontSize = px,
                lineHeight = px,
                color = color,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}
