package com.homegame.mahjong.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * One clock for every animated cosmetic. Draw code reads [t] inside draw blocks only, so a tick
 * redraws the felt and tiles without recomposing anything.
 */
object Motion {
    /** Player setting. Off freezes every effect where it is. */
    var enabled by mutableStateOf(true)
    internal val clock = mutableFloatStateOf(0f)
    /** Seconds since launch, wrapped every hour. Read it only while drawing. */
    val t: Float get() = clock.floatValue
}

/** Drives [Motion.t]. Put it once at the root; it stops ticking when motion is off or the app is hidden. */
@Composable
fun MotionClock() {
    if (!Motion.enabled) return
    LaunchedEffect(Unit) {
        val base = Motion.clock.floatValue
        val start = withFrameNanos { it }
        while (true) withFrameNanos { now -> Motion.clock.floatValue = (base + (now - start) / 1e9f) % 3600f }
    }
}

/** Ambient animation drawn over the felt. */
enum class FeltMotion(val label: String) {
    NONE(""), BREATHE("Lamplight"), TWINKLE("Twinkling stars"), RAIN("Rainfall"), SNOW("Snowfall"),
    BUBBLES("Bubbles"), EMBERS("Embers"), PETALS("Falling petals"), FIREFLIES("Fireflies"),
    SYNTHWAVE("Neon grid"), AURORA("Northern lights"), SWEEP("Light sweep"), RAYS("Sunbeams"),
    COMETS("Shooting stars"), RIPPLES("Ripples"), LANTERNS("Sky lanterns"), PRISM("Prism shift"),
}

/** Animated finish on a tile. A set can stack several. */
enum class TileFx(val label: String) {
    SHIMMER("Shimmer"), GLINT("Sparkle"), HOLO("Holographic"), PULSE("Pulse"),
    RAINBOW_RIM("Rainbow rim"), MOLTEN("Molten"), STARFIELD("Starfield"), FROST("Frost"),
}

private const val TAU = (2 * PI).toFloat()

private fun fract(x: Float) = x - floor(x)

/** Stable pseudo-random number in [0, 1) for particle [i], property [k]. No allocation. */
internal fun hash(i: Int, k: Int): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFFFF) / 16777216f
}

private fun rainbow(shift: Float, alpha: Float, steps: Int = 7): List<Color> =
    List(steps) { k -> Color.hsv(((shift * 360f) + k * 360f / (steps - 1)) % 360f, 0.65f, 1f, alpha) }

// ---- Felt --------------------------------------------------------------------------------------

/** Particles per (100dp)², clamped so tiny swatches still move and big tablets stay cheap. */
private fun DrawScope.count(per: Float, min: Int, max: Int): Int {
    val cell = 100.dp.toPx()
    return ((size.width * size.height) / (cell * cell) * per).toInt().coerceIn(min, max)
}

internal fun DrawScope.drawFeltMotion(motion: FeltMotion, colors: List<Color>, t: Float) {
    val w = size.width
    val h = size.height
    val u = 6.dp.toPx()
    val c0 = colors.getOrElse(0) { Color.White }
    val c1 = colors.getOrElse(1) { c0 }
    val c2 = colors.getOrElse(2) { c1 }
    when (motion) {
        FeltMotion.NONE -> {}
        FeltMotion.BREATHE -> {
            val a = 0.10f + 0.08f * sin(t * TAU / 6f)
            drawRect(
                Brush.radialGradient(
                    listOf(c0.copy(alpha = a), Color.Transparent),
                    center = Offset(w / 2, h * 0.42f), radius = size.maxDimension * 0.6f,
                )
            )
        }
        FeltMotion.TWINKLE -> twinkle(count(5f, 12, 160), c0, u, t)
        FeltMotion.RAIN -> {
            val n = count(4f, 10, 140)
            val len = u * 3.2f
            for (i in 0 until n) {
                val speed = 0.9f + 0.6f * hash(i, 2)
                val y = fract(hash(i, 3) + t * speed * 0.55f) * (h + len) - len
                val x = hash(i, 1) * (w + h * 0.3f) - y * 0.25f
                drawLine(c0.copy(alpha = 0.18f + 0.25f * hash(i, 4)), Offset(x, y), Offset(x - len * 0.25f, y + len), 1.dp.toPx())
            }
        }
        FeltMotion.SNOW -> {
            for (i in 0 until count(4f, 10, 140)) {
                val r = u * (0.2f + 0.45f * hash(i, 6))
                val y = fract(hash(i, 3) + t * (0.03f + 0.05f * hash(i, 2))) * (h + 2 * r) - r
                val x = hash(i, 1) * w + sin(t * (0.5f + hash(i, 4)) + TAU * hash(i, 5)) * u * 3
                drawCircle(c0.copy(alpha = 0.45f + 0.45f * hash(i, 7)), r, Offset(x, y))
            }
        }
        FeltMotion.BUBBLES -> {
            for (i in 0 until count(1.6f, 6, 60)) {
                val r = u * (0.5f + 1.4f * hash(i, 6))
                val y = h + r - fract(hash(i, 3) + t * (0.03f + 0.05f * hash(i, 2))) * (h + 2 * r)
                val x = hash(i, 1) * w + sin(t * (0.8f + hash(i, 4)) + TAU * hash(i, 5)) * u * 1.5f
                val a = 0.35f * (y / h).coerceIn(0f, 1f)
                drawCircle(c0.copy(alpha = a), r, Offset(x, y), style = Stroke(1.dp.toPx()))
                drawCircle(c0.copy(alpha = a * 1.4f), r * 0.22f, Offset(x - r * 0.35f, y - r * 0.35f))
            }
        }
        FeltMotion.EMBERS -> {
            for (i in 0 until count(3f, 8, 100)) {
                val r = u * (0.18f + 0.35f * hash(i, 6))
                val y = h + r - fract(hash(i, 3) + t * (0.05f + 0.07f * hash(i, 2))) * (h + 2 * r)
                val x = hash(i, 1) * w + sin(t * (0.7f + hash(i, 4)) + TAU * hash(i, 5)) * u * 4
                val flicker = 0.7f + 0.3f * sin(t * 9f + i)
                val a = (y / h).coerceIn(0f, 1f) * flicker
                val c = if (hash(i, 8) < 0.5f) c0 else c1
                drawCircle(c.copy(alpha = a * 0.22f), r * 3.2f, Offset(x, y))
                drawCircle(c.copy(alpha = a), r, Offset(x, y))
            }
        }
        FeltMotion.PETALS -> {
            for (i in 0 until count(1.8f, 6, 60)) {
                val s = u * (0.8f + 0.9f * hash(i, 6))
                val y = fract(hash(i, 3) + t * (0.035f + 0.04f * hash(i, 2))) * (h + 4 * s) - 2 * s
                val x = fract(hash(i, 1) + t * 0.01f) * w + sin(t * (0.6f + hash(i, 4)) + TAU * hash(i, 5)) * u * 5
                val angle = t * (40f + 70f * hash(i, 7)) + 360f * hash(i, 5)
                val c = if (hash(i, 8) < 0.6f) c0 else c1
                rotate(angle, Offset(x, y)) {
                    drawOval(c.copy(alpha = 0.75f), Offset(x - s, y - s * 0.55f), Size(s * 2, s * 1.1f))
                }
            }
        }
        FeltMotion.FIREFLIES -> {
            for (i in 0 until count(1.1f, 5, 36)) {
                val x = w * (0.5f + 0.47f * sin(t * 0.11f * (1 + hash(i, 2)) + TAU * hash(i, 1)))
                val y = h * (0.5f + 0.47f * sin(t * 0.09f * (1 + hash(i, 3)) + TAU * hash(i, 4)))
                val p = sin(t * (0.9f + hash(i, 5)) + TAU * hash(i, 6)).coerceAtLeast(0f)
                val a = p * p
                if (a < 0.02f) continue
                drawCircle(
                    Brush.radialGradient(listOf(c0.copy(alpha = 0.45f * a), Color.Transparent), Offset(x, y), u * 4),
                    u * 4, Offset(x, y),
                )
                drawCircle(c1.copy(alpha = a), u * 0.45f, Offset(x, y))
            }
        }
        FeltMotion.SYNTHWAVE -> {
            val horizon = h * 0.6f
            // Sun behind the horizon.
            clipRect(0f, 0f, w, horizon) {
                drawCircle(
                    Brush.verticalGradient(listOf(c1.copy(alpha = 0.28f), c2.copy(alpha = 0.12f)), horizon - w * 0.22f, horizon),
                    w * 0.2f, Offset(w / 2, horizon),
                )
            }
            val line = 1.2.dp.toPx()
            val rows = 14
            for (k in 0 until rows) {
                val z = fract(k.toFloat() / rows + t * 0.12f)
                val y = horizon + (h - horizon) * z * z
                drawLine(c0.copy(alpha = 0.06f + 0.3f * z), Offset(0f, y), Offset(w, y), line)
            }
            for (j in -12..12) {
                drawLine(c0.copy(alpha = 0.16f), Offset(w / 2 + j * w * 0.025f, horizon), Offset(w / 2 + j * w * 0.2f, h), line)
            }
            drawLine(c0.copy(alpha = 0.45f), Offset(0f, horizon), Offset(w, horizon), line * 1.5f)
            // A scan bar rolling down the whole screen.
            val sy = fract(t / 7f) * (h + u * 10) - u * 5
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, c1.copy(alpha = 0.10f), Color.Transparent), sy - u * 5, sy + u * 5),
                Offset(0f, sy - u * 5), Size(w, u * 10),
            )
        }
        FeltMotion.AURORA -> {
            val bands = listOf(c0, c1, c2)
            bands.forEachIndexed { b, c ->
                val base = h * (0.10f + 0.09f * b)
                val thick = h * (0.16f + 0.04f * b)
                val path = Path()
                val steps = 28
                for (s in 0..steps) {
                    val fx = s.toFloat() / steps
                    val y = base + sin(fx * TAU * 1.1f + t * 0.28f * (1 + 0.3f * b) + b) * h * 0.05f +
                        sin(fx * TAU * 2.6f - t * 0.19f + b * 2) * h * 0.022f
                    if (s == 0) path.moveTo(0f, y) else path.lineTo(fx * w, y)
                }
                path.lineTo(w, base + thick); path.lineTo(0f, base + thick); path.close()
                val a = 0.16f + 0.06f * sin(t * 0.5f + b * 1.7f)
                drawPath(path, Brush.verticalGradient(listOf(Color.Transparent, c.copy(alpha = a), Color.Transparent), base - h * 0.07f, base + thick))
            }
        }
        FeltMotion.SWEEP -> sweep(c0, 0.13f, 7f, t)
        FeltMotion.RAYS -> {
            val origin = Offset(w * 0.5f, -h * 0.12f)
            val reach = h * 1.25f
            for (k in 0 until 9) {
                val mid = 90f + (k - 4) * 13f + sin(t * 0.12f + k) * 4f
                val spread = 3.5f + 1.5f * hash(k, 1)
                val a = 0.07f + 0.05f * sin(t * (0.3f + 0.2f * hash(k, 2)) + k)
                val r1 = Math.toRadians((mid - spread).toDouble())
                val r2 = Math.toRadians((mid + spread).toDouble())
                val p = Path().apply {
                    moveTo(origin.x, origin.y)
                    lineTo(origin.x + (reach * kotlin.math.cos(r1)).toFloat(), origin.y + (reach * kotlin.math.sin(r1)).toFloat())
                    lineTo(origin.x + (reach * kotlin.math.cos(r2)).toFloat(), origin.y + (reach * kotlin.math.sin(r2)).toFloat())
                    close()
                }
                drawPath(p, Brush.radialGradient(listOf(c0.copy(alpha = a), Color.Transparent), origin, reach))
            }
        }
        FeltMotion.COMETS -> {
            twinkle(count(3f, 10, 100), c0, u, t)
            for (k in 0 until 2) {
                val period = 4.5f + k * 3.1f
                val cyc = t / period + k * 0.37f
                val local = fract(cyc)
                if (local > 0.28f) continue
                val n = floor(cyc).toInt()
                val p = local / 0.28f
                val start = Offset(hash(n, k * 2 + 1) * w * 0.8f, hash(n, k * 2 + 2) * h * 0.45f)
                val dir = Offset(0.9f, 0.42f)
                val travel = w * 0.55f
                val head = start + dir * (travel * p)
                val tail = head - dir * (w * 0.16f)
                val a = sin(p * PI.toFloat())
                drawLine(
                    Brush.linearGradient(listOf(Color.Transparent, c1.copy(alpha = 0.8f * a)), tail, head),
                    tail, head, 2.dp.toPx(),
                )
                drawCircle(c1.copy(alpha = a), 1.8.dp.toPx(), head)
            }
        }
        FeltMotion.RIPPLES -> {
            for (i in 0 until count(0.5f, 3, 10)) {
                val period = 4f + 3f * hash(i, 1)
                val cyc = t / period + hash(i, 2)
                val local = fract(cyc)
                val n = floor(cyc).toInt()
                val center = Offset(hash(n, i * 3 + 1) * w, hash(n, i * 3 + 2) * h)
                val r = local * u * 24
                val a = (1 - local) * 0.32f
                val stroke = Stroke(1.4.dp.toPx())
                drawCircle(c0.copy(alpha = a), r, center, style = stroke)
                drawCircle(c0.copy(alpha = a * 0.6f), r * 0.62f, center, style = stroke)
            }
        }
        FeltMotion.LANTERNS -> {
            for (i in 0 until count(0.55f, 3, 18)) {
                val depth = hash(i, 6)
                val s = u * (1.0f + 1.5f * depth)
                val y = h + 3 * s - fract(hash(i, 3) + t * (0.018f + 0.03f * depth)) * (h + 6 * s)
                val x = hash(i, 1) * w + sin(t * 0.3f + TAU * hash(i, 5)) * u * 4
                val flicker = 0.85f + 0.15f * sin(t * 7f + i * 1.3f)
                val a = (0.45f + 0.55f * depth) * flicker
                drawCircle(
                    Brush.radialGradient(listOf(c0.copy(alpha = 0.35f * a), Color.Transparent), Offset(x, y), s * 3.2f),
                    s * 3.2f, Offset(x, y),
                )
                drawRoundRect(
                    Brush.verticalGradient(listOf(c1.copy(alpha = a), c0.copy(alpha = a)), y - s * 0.9f, y + s * 0.9f),
                    Offset(x - s * 0.65f, y - s * 0.9f), Size(s * 1.3f, s * 1.8f), CornerRadius(s * 0.45f),
                )
                drawRect(c2.copy(alpha = a), Offset(x - s * 0.4f, y + s * 0.8f), Size(s * 0.8f, s * 0.18f))
            }
        }
        FeltMotion.PRISM -> {
            drawRect(Brush.linearGradient(rainbow(fract(t / 14f), 0.09f), Offset.Zero, Offset(w, h)))
            sweep(Color.White, 0.10f, 6f, t)
        }
    }
}

private fun DrawScope.twinkle(n: Int, c: Color, u: Float, t: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until n) {
        val big = hash(i, 3)
        val r = u * (0.12f + 0.3f * big)
        val a = 0.5f + 0.5f * sin(t * (0.8f + 2.2f * hash(i, 4)) + TAU * hash(i, 5))
        val p = Offset(hash(i, 1) * w, hash(i, 2) * h)
        drawCircle(c.copy(alpha = c.alpha * a), r, p)
        if (big > 0.9f) {
            val arm = r * 4 * a
            drawLine(c.copy(alpha = a * 0.6f), p - Offset(arm, 0f), p + Offset(arm, 0f), 1f)
            drawLine(c.copy(alpha = a * 0.6f), p - Offset(0f, arm), p + Offset(0f, arm), 1f)
        }
    }
}

/** A diagonal band of light crossing the area every [period] seconds. */
private fun DrawScope.sweep(c: Color, alpha: Float, period: Float, t: Float) {
    val w = size.width
    val h = size.height
    val band = w * 0.28f
    val x = fract(t / period) * (w + h * 0.6f + band * 2) - band - h * 0.3f
    val p = Path().apply {
        moveTo(x, 0f); lineTo(x + band, 0f); lineTo(x + band - h * 0.3f, h); lineTo(x - h * 0.3f, h); close()
    }
    drawPath(p, Brush.horizontalGradient(listOf(Color.Transparent, c.copy(alpha = alpha), Color.Transparent), x - h * 0.15f, x + band - h * 0.15f))
}

// ---- Tiles -------------------------------------------------------------------------------------

/**
 * Animated finish over a tile. [face] is the area the design sits in, [outline] the whole tile.
 * [phase] in [0, 1) staggers tiles so a rack doesn't flash in lockstep.
 */
internal fun DrawScope.drawTileFx(
    fx: List<TileFx>, color: Color, face: Rect, outline: Rect, corner: Float, t: Float, phase: Float,
) {
    if (fx.isEmpty()) return
    val w = face.width
    val fh = face.height
    val clip = Path().apply { addRoundRect(RoundRect(face, CornerRadius(corner))) }
    for (f in fx) when (f) {
        TileFx.SHIMMER -> clipPath(clip) {
            val local = fract(t / 3.6f + phase)
            if (local < 0.45f) {
                val band = w * 0.42f
                val x = face.left - band - fh * 0.4f + (local / 0.45f) * (w + band * 2 + fh * 0.4f)
                val p = Path().apply {
                    moveTo(x + fh * 0.4f, face.top); lineTo(x + fh * 0.4f + band, face.top)
                    lineTo(x + band, face.bottom); lineTo(x, face.bottom); close()
                }
                drawPath(p, Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = 0.55f), Color.Transparent), x + fh * 0.2f, x + fh * 0.2f + band))
            }
        }
        TileFx.GLINT -> {
            val cyc = t / 2.8f + phase
            val local = fract(cyc)
            if (local < 0.32f) {
                val n = floor(cyc).toInt()
                val s = sin(local / 0.32f * PI.toFloat())
                val c = Offset(face.left + w * (0.18f + 0.64f * hash(n, 1)), face.top + fh * (0.12f + 0.55f * hash(n, 2)))
                star4(c, w * 0.3f * s, color.copy(alpha = s))
                drawCircle(color.copy(alpha = 0.35f * s), w * 0.08f * s, c)
            }
        }
        TileFx.HOLO -> clipPath(clip) {
            val shift = fract(t / 4.5f + phase)
            drawRect(
                Brush.linearGradient(rainbow(shift, 0.22f), Offset(face.left, face.top), Offset(face.right, face.bottom)),
                face.topLeft, face.size,
            )
        }
        TileFx.PULSE -> {
            val a = 0.35f + 0.35f * sin(TAU * (t / 2.2f + phase))
            val sw = 2.dp.toPx()
            drawRoundRect(color.copy(alpha = a * 0.35f), outline.topLeft - Offset(sw, sw), Size(outline.width + sw * 2, outline.height + sw * 2), CornerRadius(corner + sw), style = Stroke(sw * 2))
            drawRoundRect(color.copy(alpha = a), outline.topLeft + Offset(sw / 2, sw / 2), Size(outline.width - sw, outline.height - sw), CornerRadius(corner), style = Stroke(sw))
        }
        TileFx.RAINBOW_RIM -> {
            val sw = 2.dp.toPx()
            val cols = rainbow(fract(t / 2.5f + phase), 1f, 9)
            drawRoundRect(
                Brush.sweepGradient(cols, outline.center),
                outline.topLeft + Offset(sw / 2, sw / 2), Size(outline.width - sw, outline.height - sw), CornerRadius(corner), style = Stroke(sw),
            )
        }
        TileFx.MOLTEN -> clipPath(clip) {
            val glow = 0.28f + 0.12f * sin(t * 2.4f + phase * TAU)
            val p = Path()
            val steps = 10
            for (s in 0..steps) {
                val fx = s.toFloat() / steps
                val y = face.top + fh * (0.68f + 0.05f * sin(fx * TAU + t * 2f + phase * TAU) + 0.03f * sin(fx * TAU * 2.3f - t * 1.3f))
                if (s == 0) p.moveTo(face.left, y) else p.lineTo(face.left + fx * w, y)
            }
            p.lineTo(face.right, face.bottom); p.lineTo(face.left, face.bottom); p.close()
            drawPath(p, Brush.verticalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = glow)), face.top + fh * 0.62f, face.bottom))
        }
        TileFx.STARFIELD -> clipPath(clip) {
            for (i in 0 until 9) {
                val a = 0.5f + 0.5f * sin(t * (1.2f + 2f * hash(i, 4)) + TAU * (hash(i, 5) + phase))
                drawCircle(color.copy(alpha = a * 0.9f), w * (0.012f + 0.02f * hash(i, 3)), Offset(face.left + hash(i, 1) * w, face.top + hash(i, 2) * fh))
            }
        }
        TileFx.FROST -> clipPath(clip) {
            // Ice creeping in from the corners and melting back.
            val a = 0.5f + 0.5f * sin(TAU * (t / 5f + phase))
            val r = w * (0.35f + 0.25f * a)
            for (c in listOf(face.topLeft, Offset(face.right, face.bottom))) {
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * a), Color.Transparent), c, r), r, c)
            }
            star4(Offset(face.left + w * 0.2f, face.top + fh * 0.16f), w * 0.16f * a, color.copy(alpha = 0.8f * a))
        }
    }
}

/** A breathing glow around a picked-up tile, whatever the set. */
internal fun DrawScope.drawSelectedPulse(glow: Color, outline: Rect, corner: Float, t: Float) {
    val a = 0.25f + 0.25f * sin(t * TAU / 1.4f)
    val sw = 3.dp.toPx()
    drawRoundRect(glow.copy(alpha = a), outline.topLeft - Offset(sw, sw), Size(outline.width + sw * 2, outline.height + sw * 2), CornerRadius(corner + sw), style = Stroke(sw * 1.6f))
}

private fun DrawScope.star4(c: Offset, r: Float, color: Color) {
    if (r <= 0.5f) return
    val k = r * 0.18f
    val p = Path().apply {
        moveTo(c.x, c.y - r); lineTo(c.x + k, c.y - k); lineTo(c.x + r, c.y); lineTo(c.x + k, c.y + k)
        lineTo(c.x, c.y + r); lineTo(c.x - k, c.y + k); lineTo(c.x - r, c.y); lineTo(c.x - k, c.y - k); close()
    }
    drawPath(p, color)
}

// ---- Menu helpers ------------------------------------------------------------------------------

/** A light band that glides across whatever this draws on (text, buttons). Stays inside its pixels. */
fun Modifier.shine(color: Color = Color.White, alpha: Float = 0.45f, period: Float = 4.5f, delay: Float = 0f): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        if (!Motion.enabled) return@drawWithContent
        val local = fract(Motion.t / period + delay)
        if (local > 0.4f) return@drawWithContent
        val w = size.width
        val h = size.height
        val band = (w * 0.25f).coerceAtLeast(h)
        val x = -band + (local / 0.4f) * (w + band * 2)
        drawRect(
            Brush.linearGradient(listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent), Offset(x - band / 2, 0f), Offset(x + band / 2, h)),
            blendMode = BlendMode.SrcAtop,
        )
    }

/** Gentle float: drifts up and down by [amp] with its own [phase]. */
fun Modifier.bob(amp: Float = 4f, period: Float = 3.2f, phase: Float = 0f, tilt: Float = 0f): Modifier = graphicsLayer {
    if (!Motion.enabled) return@graphicsLayer
    val s = sin(TAU * (Motion.t / period + phase))
    translationY = s * amp * density
    rotationZ = tilt * sin(TAU * (Motion.t / (period * 1.3f) + phase + 0.25f))
}

/** Rises and fades in once, [index] steps after the screen appears. */
fun Modifier.enter(index: Int): Modifier = composed {
    val p = remember { Animatable(if (Motion.enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (p.value < 1f) {
            kotlinx.coroutines.delay(50L * index)
            p.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    graphicsLayer {
        alpha = p.value
        translationY = (1 - p.value) * 18f * density
    }
}
