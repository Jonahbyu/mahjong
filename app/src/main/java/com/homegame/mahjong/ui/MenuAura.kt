package com.homegame.mahjong.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.homegame.mahjong.cosmetics.UiTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * The menu's mood for each opponent level, drawn over the felt and under the menu.
 * Colors come from the equipped table style, so the same level looks different per style.
 */
enum class Aura { CALM, DRIFT, FOCUS, STORM, WORKSHOP }

private const val TAU = (2 * PI).toFloat()
private fun fract(x: Float) = x - floor(x)

/**
 * Draws every aura with weight `1 - distance` from [pos], the pager position (page + swipe offset),
 * so effects crossfade live while the player swipes.
 */
internal fun DrawScope.drawMenuAura(auras: List<Aura>, pos: Float, theme: UiTheme, t: Float) {
    auras.forEachIndexed { i, aura ->
        val a = max(0f, 1f - abs(pos - i))
        if (a <= 0.01f) return@forEachIndexed
        when (aura) {
            Aura.CALM -> calm(a, theme, t)
            Aura.DRIFT -> motes(a, theme, t, n = 34, speed = 0.035f)
            Aura.FOCUS -> focus(a, theme, t)
            Aura.STORM -> storm(a, theme, t)
            Aura.WORKSHOP -> workshop(a, theme, t)
        }
    }
}

/** How lively the tiles above the title move: 0 calm, 1 frantic. */
internal fun auraEnergy(auras: List<Aura>, pos: Float): Float {
    var e = 0f
    auras.forEachIndexed { i, aura ->
        val a = max(0f, 1f - abs(pos - i))
        e += a * when (aura) {
            Aura.CALM -> 0f
            Aura.DRIFT -> 0.3f
            Aura.FOCUS -> 0.6f
            Aura.STORM -> 1f
            Aura.WORKSHOP -> 0.45f
        }
    }
    return e
}

private fun UiTheme.spark(): Color = motionColors.firstOrNull()?.takeIf { motion != FeltMotion.NONE } ?: accentHigh

/** Beginner: a slow breathing lamp glow and a few big soft lights. */
private fun DrawScope.calm(a: Float, th: UiTheme, t: Float) {
    val w = size.width
    val h = size.height
    val breathe = 0.5f + 0.5f * sin(t * TAU / 8f)
    drawRect(
        Brush.radialGradient(
            listOf(th.accentHigh.copy(alpha = a * (0.10f + 0.07f * breathe)), Color.Transparent),
            center = Offset(w / 2, h * 0.2f), radius = size.maxDimension * 0.55f,
        )
    )
    val r0 = 40.dp.toPx()
    for (i in 0 until 7) {
        val x = fract(hash(i, 11) + t * 0.006f * (0.5f + hash(i, 12))) * (w + 2 * r0) - r0
        val y = h * (0.1f + 0.8f * hash(i, 13)) + sin(t * 0.3f + i) * 12.dp.toPx()
        val r = r0 * (0.6f + 0.8f * hash(i, 14))
        drawCircle(
            Brush.radialGradient(listOf(th.accent.copy(alpha = a * 0.07f), Color.Transparent), Offset(x, y), r),
            r, Offset(x, y),
        )
    }
}

/** Small lights rising and swaying. Intermediate on its own; the busier levels add to it. */
private fun DrawScope.motes(a: Float, th: UiTheme, t: Float, n: Int, speed: Float) {
    val w = size.width
    val h = size.height
    val u = 1.dp.toPx()
    val c1 = th.accentHigh
    val c2 = th.spark()
    for (i in 0 until n) {
        val y = h - fract(hash(i, 3) + t * speed * (0.6f + hash(i, 2))) * (h + 20 * u)
        val x = hash(i, 1) * w + sin(t * (0.4f + hash(i, 5)) + i) * 14 * u
        val tw = 0.55f + 0.45f * sin(t * (1.5f + 2f * hash(i, 6)) + i * 2.1f)
        val c = lerp(c1, c2, hash(i, 7))
        drawCircle(c.copy(alpha = a * 0.55f * tw), u * (1.2f + 2.2f * hash(i, 4)), Offset(x, y))
    }
}

/** Advanced: slow rings radiating from the picker, over quicker motes. */
private fun DrawScope.focus(a: Float, th: UiTheme, t: Float) {
    motes(a, th, t, n = 44, speed = 0.07f)
    val c = Offset(size.width / 2, size.height * 0.52f)
    for (k in 0 until 3) {
        val f = fract(t / 4.5f + k / 3f)
        drawCircle(
            th.accent.copy(alpha = a * 0.22f * (1f - f)), size.maxDimension * 0.65f * f, c,
            style = Stroke(1.5.dp.toPx()),
        )
    }
}

/** Expert: a dark edge that swells slowly, and sparks streaking up. No flashes. */
private fun DrawScope.storm(a: Float, th: UiTheme, t: Float) {
    val w = size.width
    val h = size.height
    // Smooth 4-second swell, so the edge never blinks.
    val pulse = 0.5f + 0.5f * sin(t * TAU / 4f)
    drawRect(
        Brush.radialGradient(
            listOf(Color.Transparent, th.accentDeep.copy(alpha = a * (0.30f + 0.10f * pulse))),
            center = Offset(w / 2, h / 2), radius = size.maxDimension * 0.62f,
        )
    )
    val u = 1.dp.toPx()
    val c = th.spark()
    for (i in 0 until 46) {
        val len = u * (10f + 18f * hash(i, 8))
        val y = h - fract(hash(i, 3) + t * (0.22f + 0.25f * hash(i, 2))) * (h + len)
        val x = hash(i, 1) * w + (h - y) * 0.06f * (hash(i, 9) - 0.5f)
        drawLine(c.copy(alpha = a * (0.25f + 0.45f * hash(i, 4))), Offset(x, y), Offset(x, y + len), u * 1.4f)
    }
}

/** Custom: slowly turning light rays in the style's colors, and twinkles. */
private fun DrawScope.workshop(a: Float, th: UiTheme, t: Float) {
    val w = size.width
    val h = size.height
    val colors = (listOf(th.accentHigh, th.accent) + th.motionColors).distinct()
    val origin = Offset(w / 2, -h * 0.05f)
    val reach = size.maxDimension * 1.2f
    for (k in 0 until 10) {
        rotate(t * 3f + k * 36f, origin) {
            drawLine(
                Brush.linearGradient(
                    listOf(colors[k % colors.size].copy(alpha = a * 0.09f), Color.Transparent),
                    origin, Offset(origin.x, origin.y + reach),
                ),
                origin, Offset(origin.x, origin.y + reach), strokeWidth = 34.dp.toPx(),
            )
        }
    }
    val u = 1.dp.toPx()
    for (i in 0 until 24) {
        val tw = max(0f, sin(t * (0.8f + hash(i, 6)) + i * 1.7f))
        drawCircle(colors[i % colors.size].copy(alpha = a * 0.7f * tw * tw), u * (1.5f + 1.5f * hash(i, 4)), Offset(hash(i, 1) * w, hash(i, 2) * h))
    }
}
