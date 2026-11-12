package com.homegame.mahjong.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.homegame.mahjong.cosmetics.FeltTexture
import com.homegame.mahjong.cosmetics.UiStyles
import com.homegame.mahjong.cosmetics.UiTheme
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The table style in use. Every palette value below reads from it, so changing it restyles the app. */
object ActiveUi {
    var theme: UiTheme by mutableStateOf(UiStyles.all.first().theme)
}

// Table palette. Felt is lit from the middle; panels are darker felt; the accent is the one bright color.
val FeltLight: Color get() = ActiveUi.theme.feltLight
val Felt: Color get() = ActiveUi.theme.felt
val FeltEdge: Color get() = ActiveUi.theme.feltEdge
val FeltDark: Color get() = ActiveUi.theme.feltDark
val Panel: Color get() = ActiveUi.theme.feltEdge.copy(alpha = 0.55f)
val Gold: Color get() = ActiveUi.theme.accent
val GoldDeep: Color get() = ActiveUi.theme.accentDeep
val GoldHigh: Color get() = ActiveUi.theme.accentHigh
val GoldRim: Color get() = ActiveUi.theme.accentRim
val Ivory: Color get() = ActiveUi.theme.ivory
val Ink: Color get() = ActiveUi.theme.ink
val WoodLight: Color get() = ActiveUi.theme.woodLight
val WoodDark: Color get() = ActiveUi.theme.woodDark
val WoodRim: Color get() = ActiveUi.theme.woodRim
val Display: FontFamily get() = ActiveUi.theme.display
val Soft = Color.White.copy(alpha = 0.72f)
val Faint = Color.White.copy(alpha = 0.14f)
val Warn = Color(0xFFFFB4A9)

// Coins stay gold whatever the table style.
private val CoinGold = Color(0xFFF2C24F)
private val CoinHigh = Color(0xFFFFE28A)
private val CoinDeep = Color(0xFFC8912A)

/**
 * Felt that's brighter in the middle, like a lamp over the table. The trailing layer holds the
 * content, so an animated felt redraws only itself, not everything sitting on it.
 */
fun Modifier.felt(theme: UiTheme? = null): Modifier = feltBackground(theme).graphicsLayer()

private fun Modifier.feltBackground(theme: UiTheme?): Modifier = drawBehind {
    val t = theme ?: ActiveUi.theme
    drawRect(t.feltEdge)
    drawRect(
        Brush.radialGradient(
            listOf(t.feltLight, t.felt, t.feltEdge),
            center = Offset(size.width / 2, size.height * 0.42f),
            radius = size.maxDimension * 0.7f,
        )
    )
    drawTexture(t.texture, t.textureColor)
    // Read the clock only for animated styles, so static felt never redraws.
    if (t.motion != FeltMotion.NONE && Motion.enabled) drawFeltMotion(t.motion, t.motionColors, Motion.t)
}

private fun DrawScope.drawTexture(texture: FeltTexture, color: Color) {
    val w = size.width
    val h = size.height
    val unit = 6.dp.toPx()
    when (texture) {
        FeltTexture.NONE -> {}
        FeltTexture.WEAVE -> {
            var x = 0f
            while (x < w + h) {
                drawLine(color, Offset(x, 0f), Offset(x - h, h), 1f)
                drawLine(color, Offset(x - h, 0f), Offset(x, h), 1f)
                x += unit
            }
        }
        FeltTexture.PINSTRIPE -> {
            var x = 0f
            while (x < w) { drawLine(color, Offset(x, 0f), Offset(x, h), 1.5f); x += unit * 4 }
        }
        FeltTexture.DOTS -> {
            val step = unit * 4
            var y = step / 2
            var row = 0
            while (y < h) {
                var x = if (row % 2 == 0) step / 2 else step
                while (x < w) { drawCircle(color, unit * 0.35f, Offset(x, y)); x += step }
                y += step; row++
            }
        }
        FeltTexture.LATTICE -> {
            val step = unit * 5
            var x = 0f
            while (x < w) { drawLine(color, Offset(x, 0f), Offset(x, h), 1.5f); x += step }
            var y = 0f
            while (y < h) { drawLine(color, Offset(0f, y), Offset(w, y), 1.5f); y += step }
        }
        FeltTexture.WAVES -> {
            val step = unit * 4
            var y = 0f
            while (y < h + step) {
                val path = Path()
                path.moveTo(0f, y)
                var x = 0f
                while (x < w) {
                    path.quadraticTo(x + step / 2, y - step / 3, x + step, y)
                    path.quadraticTo(x + step * 1.5f, y + step / 3, x + step * 2, y)
                    x += step * 2
                }
                drawPath(path, color, style = Stroke(1.5f))
                y += step
            }
        }
        FeltTexture.STARS -> {
            // Fixed pseudo-random sky so it doesn't shimmer between frames.
            val rnd = java.util.Random(7L)
            val count = ((w * h) / (unit * unit * 60)).toInt().coerceIn(20, 400)
            repeat(count) {
                val r = unit * (0.12f + rnd.nextFloat() * 0.3f)
                drawCircle(color.copy(alpha = color.alpha * (0.4f + rnd.nextFloat() * 0.6f)), r, Offset(rnd.nextFloat() * w, rnd.nextFloat() * h))
            }
        }
    }
}

@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(14.dp)
    val base = modifier
        .scale(if (pressed && enabled) 0.97f else 1f)
        .alpha(if (enabled) 1f else 0.6f)
        .defaultMinSize(minWidth = 96.dp)
        .height(52.dp)
    val styled = if (primary && enabled) {
        base.shadow(if (enabled) 6.dp else 0.dp, shape)
            .background(Brush.verticalGradient(listOf(GoldHigh, Gold, GoldDeep)), shape)
            .border(1.dp, GoldRim, shape)
            .shine(GoldRim, alpha = 0.55f)
    } else {
        base.background(Color.Black.copy(alpha = 0.28f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
    }
    Box(
        styled.clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (primary && enabled) Ink else Color.White,
            fontWeight = if (primary) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 1,
        )
    }
}

/** Small rounded control for the top bar. */
@Composable
fun Pill(text: String, active: Boolean = false, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(50)
    var m = Modifier
        .background(if (active) Gold.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.22f), shape)
        .border(1.dp, if (active) Gold.copy(alpha = 0.7f) else Faint, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Text(
        text,
        color = if (active) Gold else Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = m.padding(horizontal = 11.dp, vertical = 8.dp),
    )
}

/** A small gold coin. */
@Composable
fun Coin(size: Dp = 16.dp) {
    Box(
        Modifier.size(size)
            .background(Brush.linearGradient(listOf(CoinHigh, CoinGold, CoinDeep)), CircleShape)
            .border(1.dp, Color(0xFF9A6A12), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size * 0.5f).border(1.dp, Color(0xFF9A6A12).copy(alpha = 0.6f), CircleShape))
    }
}

/** Coin plus an amount, e.g. the wallet balance or a payout. */
@Composable
fun CoinAmount(text: String, fontSize: TextUnit = 16.sp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Coin((fontSize.value + 1).dp)
        Text(text, color = CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = fontSize)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = modifier)
}

/** Pill-shaped choice between a few options. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Faint, shape).padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label,
                color = if (on) Ink else Color.White,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
                    .background(if (on) Gold else Color.Transparent, shape)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/** Round button with a drawn gear, for settings. */
@Composable
fun GearButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(38.dp)
            .background(Color.Black.copy(alpha = 0.22f), CircleShape)
            .border(1.dp, Faint, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Settings" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val c = center
            val r = size.minDimension / 2
            for (k in 0 until 8) {
                rotate(k * 45f, c) {
                    drawRoundRect(Ivory, Offset(c.x - r * 0.17f, c.y - r), Size(r * 0.34f, r * 0.5f), CornerRadius(r * 0.06f))
                }
            }
            drawCircle(Ivory, r * 0.68f, c)
            drawCircle(FeltEdge, r * 0.28f, c)
        }
    }
}
