package com.homegame.mahjong.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.homegame.mahjong.engine.Dragon
import com.homegame.mahjong.engine.Kinds
import com.homegame.mahjong.engine.Suit
import kotlin.math.cos
import kotlin.math.sin

/**
 * Everything that makes tiles look the way they do. Every color a tile uses comes from here, so the
 * cosmetics system (shop, daily unbox) can produce new sets as data.
 */
@Immutable
data class TileSkin(
    val name: String,
    val faceTop: Color,
    val faceBottom: Color,
    val edge: Color,
    val back: Color,
    val backDeep: Color,
    val bam: Color,
    val bamAccent: Color,
    val crak: Color,
    val crakInk: Color,
    val dot: Color,
    val dotAccent: Color,
    val honor: Color,
    val flower: Color,
    val leaf: Color,
    val joker: Color,
    val jokerAccent: Color,
    val selectedGlow: Color,
    /** Decoration on the tile back. */
    val pattern: BackPattern = BackPattern.PLAIN,
    val patternColor: Color = Color.White.copy(alpha = 0.22f),
    /** Strength of the light rim on the face; low for dark faces. */
    val shine: Float = 0.8f,
    /** Diagonal gloss across the face (Epic and up). */
    val sheen: Boolean = false,
    /** Metallic outline (Legendary). */
    val rim: Color? = null,
    /** Animated finishes (Epic and up). */
    val fx: List<TileFx> = emptyList(),
    /** Light color the animated finishes use. */
    val fxColor: Color = Color.White,
) {
    companion object {
        val Classic = TileSkin(
            name = "Classic Ivory",
            faceTop = Color(0xFFFFFCF3),
            faceBottom = Color(0xFFF2E8CF),
            edge = Color(0xFFCDBE97),
            back = Color(0xFF248A64),
            backDeep = Color(0xFF125238),
            bam = Color(0xFF1E7B45),
            bamAccent = Color(0xFFC62828),
            crak = Color(0xFFC62828),
            crakInk = Color(0xFF1B2A4A),
            dot = Color(0xFF1F4FA8),
            dotAccent = Color(0xFFC62828),
            honor = Color(0xFF1E2733),
            flower = Color(0xFFD23C77),
            leaf = Color(0xFF2E7D32),
            joker = Color(0xFF6A1B9A),
            jokerAccent = Color(0xFFE0A100),
            selectedGlow = Color(0xFFFFC94D),
        )
    }
}

enum class BackPattern { PLAIN, LATTICE, DOTS, STRIPES, WAVES, STARS, DIAMONDS, SCALES, PLAID, SUNBURST, HEX, CHEVRON, CHECKER, RINGS }

/** The equipped tile set. Every TileView/TileBack reads it unless given a skin explicitly. */
val LocalTileSkin = staticCompositionLocalOf { TileSkin.Classic }

/** Width at or above which tiles get full traditional faces and a corner index. */
private val FullFaceMin = 34.dp

@Composable
fun TileView(
    kind: Int,
    width: Dp,
    modifier: Modifier = Modifier,
    skin: TileSkin = LocalTileSkin.current,
    selected: Boolean = false,
    highlight: Boolean = false,
    faded: Boolean = false,
) {
    val tm = rememberTextMeasurer()
    val lift by animateDpAsState(if (selected) (-12).dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 700f), label = "lift")
    val full = width >= FullFaceMin
    val depthRatio = if (full) 0.065f else 0.05f
    Box(
        modifier.offset(y = lift).size(width, width * 1.36f).alpha(if (faded) 0.36f else 1f)
            .semantics { contentDescription = Kinds.name(kind) }
    ) {
        // Own layer, so the animated overlay below redraws without repainting the tile art.
        Canvas(Modifier.matchParentSize().graphicsLayer()) {
            val face = tileBody(skin, selected, highlight, depthRatio)
            drawFace(kind, skin, tm, face, full)
        }
        if (Motion.enabled && !faded && (skin.fx.isNotEmpty() || selected)) {
            val phase = (kind * 0.618034f) % 1f
            Canvas(Modifier.matchParentSize()) {
                val t = Motion.t
                val w = size.width
                val outline = Rect(0f, 0f, w, size.height)
                drawTileFx(skin.fx, skin.fxColor, Rect(0f, 0f, w, size.height * (1 - depthRatio)), outline, w * 0.14f, t, phase)
                if (selected) drawSelectedPulse(skin.selectedGlow, outline, w * 0.14f, t)
            }
        }
    }
}

/** Face-down tile: jade back with a lattice, used for opponents' concealed tiles. */
@Composable
fun TileBack(width: Dp, modifier: Modifier = Modifier, skin: TileSkin = LocalTileSkin.current, phase: Float = 0f) {
    Box(modifier.size(width, width * 1.36f)) {
        TileBackArt(skin, Modifier.matchParentSize().graphicsLayer())
        if (Motion.enabled && skin.fx.isNotEmpty()) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val back = Rect(0f, 0f, w, size.height * 0.92f)
                drawTileFx(skin.fx, skin.fxColor, back, back, w * 0.16f, Motion.t, phase)
            }
        }
    }
}

@Composable
private fun TileBackArt(skin: TileSkin, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = CornerRadius(w * 0.16f)
        val depth = h * 0.08f
        drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(0f, depth * 0.8f), Size(w, h), r)
        drawRoundRect(skin.faceBottom, size = Size(w, h), cornerRadius = r)
        drawRoundRect(Brush.verticalGradient(listOf(skin.back, skin.backDeep)), size = Size(w, h - depth), cornerRadius = r)
        val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h - depth, r)) }
        clipPath(clip) { backPattern(skin, Rect(0f, 0f, w, h - depth)) }
        skin.rim?.let { drawRoundRect(it, Offset(0.6f, 0.6f), Size(w - 1.2f, h - 1.2f), r, style = Stroke(1.4.dp.toPx())) }
    }
}

// ---- Body ------------------------------------------------------------------------------------

/** Draws shadow, jade back, ivory side and face. Returns the face rect to draw the design into. */
private fun DrawScope.tileBody(skin: TileSkin, selected: Boolean, highlight: Boolean, depthRatio: Float): Rect {
    val w = size.width
    val h = size.height
    val r = CornerRadius(w * 0.14f)
    val depth = h * depthRatio
    val faceH = h - depth

    if (selected) {
        drawRoundRect(skin.selectedGlow.copy(alpha = 0.45f), Offset(-w * 0.06f, -w * 0.06f), Size(w * 1.12f, h + w * 0.12f), CornerRadius(w * 0.2f))
    }
    drawRoundRect(
        Color.Black.copy(alpha = if (selected) 0.32f else 0.24f),
        Offset(w * 0.03f, depth * (if (selected) 2.6f else 1.1f)), Size(w * 0.97f, h - depth * 0.3f), r,
    )
    // Jade back peeking out under the ivory, like a real two-layer tile.
    drawRoundRect(Brush.verticalGradient(listOf(skin.back, skin.backDeep)), size = Size(w, h), cornerRadius = r)
    drawRoundRect(skin.edge, size = Size(w, h - depth * 0.45f), cornerRadius = r)
    drawRoundRect(Brush.verticalGradient(listOf(skin.faceTop, skin.faceBottom), 0f, faceH), size = Size(w, faceH), cornerRadius = r)
    if (skin.sheen) {
        val gloss = Path().apply {
            moveTo(w * 0.52f, 0f); lineTo(w * 0.78f, 0f); lineTo(w * 0.3f, faceH); lineTo(w * 0.04f, faceH); close()
        }
        clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, w, faceH, r)) }) {
            drawPath(gloss, Color.White.copy(alpha = 0.16f))
        }
    }
    // Soft highlight along the top edge of the face.
    val hl = 1.dp.toPx()
    drawRoundRect(
        Color.White.copy(alpha = skin.shine), Offset(hl, hl), Size(w - hl * 2, faceH - hl * 2),
        CornerRadius(w * 0.12f), style = Stroke(hl),
    )
    val outline = when {
        selected -> skin.selectedGlow
        highlight -> skin.selectedGlow
        else -> skin.rim ?: skin.edge.copy(alpha = 0.9f)
    }
    val ow = if (selected || highlight) 2.5.dp.toPx() else if (skin.rim != null) 1.5.dp.toPx() else 0.8.dp.toPx()
    drawRoundRect(outline, Offset(ow / 2, ow / 2), Size(w - ow, h - ow), r, style = Stroke(ow))
    return Rect(0f, 0f, w, faceH)
}

// ---- Back patterns ---------------------------------------------------------------------------

private fun DrawScope.backPattern(skin: TileSkin, a: Rect) {
    val w = a.width
    val h = a.height
    val c = skin.patternColor
    val line = (w * 0.045f).coerceAtLeast(1f)
    when (skin.pattern) {
        BackPattern.PLAIN -> drawRoundRect(
            c, Offset(w * 0.16f, h * 0.13f), Size(w * 0.68f, h * 0.72f), CornerRadius(w * 0.08f), style = Stroke(w * 0.07f),
        )
        BackPattern.LATTICE -> {
            val step = w / 3.5f
            var x = -h
            while (x < w + h) {
                drawLine(c, Offset(x, 0f), Offset(x + h, h), line)
                drawLine(c, Offset(x + h, 0f), Offset(x, h), line)
                x += step
            }
        }
        BackPattern.DOTS -> {
            val step = w / 4f
            var row = 0
            var y = step / 2
            while (y < h) {
                var x = if (row % 2 == 0) step / 2 else step
                while (x < w) { drawCircle(c, w * 0.06f, Offset(x, y)); x += step }
                y += step * 0.85f; row++
            }
        }
        BackPattern.STRIPES -> {
            val step = w / 3f
            var x = -h
            while (x < w) {
                val p = Path().apply { moveTo(x, h); lineTo(x + step * 0.45f, h); lineTo(x + step * 0.45f + h, 0f); lineTo(x + h, 0f); close() }
                drawPath(p, c)
                x += step
            }
        }
        BackPattern.WAVES -> {
            val step = h / 5f
            var y = step / 2
            while (y < h + step) {
                val p = Path()
                p.moveTo(0f, y)
                var x = 0f
                val seg = w / 2f
                while (x < w) {
                    p.quadraticTo(x + seg / 4, y - step * 0.35f, x + seg / 2, y)
                    p.quadraticTo(x + seg * 3 / 4, y + step * 0.35f, x + seg, y)
                    x += seg
                }
                drawPath(p, c, style = Stroke(line * 1.2f))
                y += step
            }
        }
        BackPattern.STARS -> {
            val spots = listOf(.25f to .2f, .72f to .3f, .45f to .52f, .2f to .75f, .78f to .78f, .55f to .9f, .8f to .07f)
            spots.forEachIndexed { i, (x, y) -> star(Offset(w * x, h * y), w * if (i % 2 == 0) 0.13f else 0.08f, c) }
        }
        BackPattern.DIAMONDS -> {
            val dw = w / 3f
            val dh = dw * 1.3f
            var row = 0
            var y = 0f
            while (y < h + dh) {
                var x = if (row % 2 == 0) 0f else dw / 2
                while (x < w + dw) {
                    val p = Path().apply {
                        moveTo(x, y - dh * 0.32f); lineTo(x + dw * 0.32f, y); lineTo(x, y + dh * 0.32f); lineTo(x - dw * 0.32f, y); close()
                    }
                    drawPath(p, c)
                    x += dw
                }
                y += dh / 2; row++
            }
        }
        BackPattern.SCALES -> {
            val sr = w / 6f
            var row = 0
            var y = 0f
            while (y < h + sr) {
                var x = if (row % 2 == 0) 0f else sr
                while (x < w + sr) {
                    drawArc(c, 0f, 180f, false, Offset(x - sr, y - sr), Size(sr * 2, sr * 2), style = Stroke(line))
                    x += sr * 2
                }
                y += sr * 0.9f; row++
            }
        }
        BackPattern.PLAID -> {
            val band = w * 0.14f
            for (f in listOf(0.22f, 0.62f)) drawRect(c, Offset(w * f, 0f), Size(band, h))
            for (f in listOf(0.18f, 0.5f, 0.82f)) drawRect(c, Offset(0f, h * f - band / 2), Size(w, band))
            for (f in listOf(0.4f, 0.85f)) drawRect(c, Offset(w * f, 0f), Size(line * 0.7f, h))
        }
        BackPattern.SUNBURST -> {
            val center = Offset(w / 2, h / 2)
            val rr = h
            for (k in 0 until 16 step 2) {
                val a1 = Math.toRadians(k * 22.5)
                val a2 = Math.toRadians((k + 1) * 22.5)
                val p = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + (rr * cos(a1)).toFloat(), center.y + (rr * sin(a1)).toFloat())
                    lineTo(center.x + (rr * cos(a2)).toFloat(), center.y + (rr * sin(a2)).toFloat())
                    close()
                }
                drawPath(p, c)
            }
            drawCircle(c, w * 0.14f, center)
        }
        BackPattern.HEX -> {
            val hr = w / 5.5f
            val dx = hr * 1.732f
            var row = 0
            var y = 0f
            while (y < h + hr) {
                var x = if (row % 2 == 0) 0f else dx / 2
                while (x < w + hr) {
                    val p = Path()
                    for (i in 0 until 6) {
                        val ang = Math.toRadians(30.0 + i * 60.0)
                        val px = x + (hr * cos(ang)).toFloat()
                        val py = y + (hr * sin(ang)).toFloat()
                        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                    }
                    p.close()
                    drawPath(p, c, style = Stroke(line))
                    x += dx
                }
                y += hr * 1.5f; row++
            }
        }
        BackPattern.CHEVRON -> {
            val step = h / 5f
            var y = -step
            while (y < h + step) {
                val p = Path().apply { moveTo(0f, y); lineTo(w / 2, y + step * 0.45f); lineTo(w, y) }
                drawPath(p, c, style = Stroke(line * 1.6f))
                y += step
            }
        }
        BackPattern.CHECKER -> {
            val cell = w / 4f
            var row = 0
            var y = 0f
            while (y < h) {
                var x = if (row % 2 == 0) 0f else cell
                while (x < w) { drawRect(c, Offset(x, y), Size(cell, cell)); x += cell * 2 }
                y += cell; row++
            }
        }
        BackPattern.RINGS -> {
            val center = Offset(w / 2, h / 2)
            var r = w * 0.1f
            while (r < h) { drawCircle(c, r, center, style = Stroke(line)); r += w * 0.14f }
        }
    }
}

// ---- Faces -----------------------------------------------------------------------------------

private val CN_NUM = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九")

private fun windChar(letter: Char) = when (letter) { 'N' -> "北"; 'E' -> "東"; 'W' -> "西"; else -> "南" }

private fun DrawScope.drawFace(kind: Int, skin: TileSkin, tm: TextMeasurer, face: Rect, full: Boolean) {
    val w = face.width
    val index: Pair<String, Color>? = if (!full) null else when {
        Kinds.isSuited(kind) -> Kinds.rankOf(kind).toString() to suitColor(kind, skin)
        Kinds.isWind(kind) -> Kinds.windOf(kind).letter.toString() to skin.honor
        Kinds.isDragon(kind) -> when (Kinds.dragonOfKind(kind)) {
            Dragon.RED -> "R" to skin.crak
            Dragon.GREEN -> "G" to skin.bam
            Dragon.WHITE -> "0" to skin.dot
        }
        kind == Kinds.FLOWER -> "F" to skin.flower
        else -> null
    }
    index?.let { (s, c) ->
        text(tm, s, Offset(face.left + w * 0.17f, face.top + w * 0.17f), w * 0.21f, c, FontWeight.ExtraBold)
    }
    // Content area: leave the top-left corner to the index on full faces.
    val area = if (full) Rect(face.left + w * 0.12f, face.top + face.height * 0.2f, face.right - w * 0.12f, face.bottom - face.height * 0.06f)
    else Rect(face.left + w * 0.1f, face.top + face.height * 0.08f, face.right - w * 0.1f, face.bottom - face.height * 0.08f)

    when {
        Kinds.isSuited(kind) && !full -> compactSuited(kind, skin, tm, face)
        Kinds.isSuited(kind) -> when (Kinds.suitOf(kind)) {
            Suit.DOT -> dots(Kinds.rankOf(kind), skin, area)
            Suit.BAM -> bams(Kinds.rankOf(kind), skin, area)
            Suit.CRAK -> {
                text(tm, CN_NUM[Kinds.rankOf(kind) - 1], Offset(area.center.x, area.top + area.height * 0.26f), area.width * 0.62f, skin.crakInk, FontWeight.Bold)
                text(tm, "萬", Offset(area.center.x, area.top + area.height * 0.72f), area.width * 0.66f, skin.crak, FontWeight.Bold)
            }
        }
        Kinds.isWind(kind) -> {
            val letter = Kinds.windOf(kind).letter
            if (full) text(tm, windChar(letter), area.center, area.width * 0.86f, skin.honor, FontWeight.Bold)
            else text(tm, letter.toString(), face.center, w * 0.62f, skin.honor, FontWeight.Black)
        }
        Kinds.isDragon(kind) -> when (Kinds.dragonOfKind(kind)) {
            Dragon.RED -> text(tm, "中", area.center, area.width * 0.9f, skin.crak, FontWeight.Black)
            Dragon.GREEN -> text(tm, "發", area.center, area.width * 0.9f, skin.bam, FontWeight.Black)
            Dragon.WHITE -> soapFrame(skin, area)
        }
        kind == Kinds.FLOWER -> flower(skin, area)
        kind == Kinds.JOKER -> joker(skin, tm, if (full) Rect(face.left + w * 0.08f, face.top + face.height * 0.08f, face.right - w * 0.08f, face.bottom - face.height * 0.06f) else area, full)
    }
}

private fun suitColor(kind: Int, skin: TileSkin) = when (Kinds.suitOf(kind)) {
    Suit.BAM -> skin.bam
    Suit.CRAK -> skin.crak
    Suit.DOT -> skin.dot
}

/** Small tiles (discards, hints): big number plus a tiny suit mark, so they read at a glance. */
private fun DrawScope.compactSuited(kind: Int, skin: TileSkin, tm: TextMeasurer, face: Rect) {
    val w = face.width
    val c = suitColor(kind, skin)
    text(tm, Kinds.rankOf(kind).toString(), Offset(face.center.x, face.top + face.height * 0.38f), w * 0.62f, c, FontWeight.Black)
    val mark = Offset(face.center.x, face.top + face.height * 0.8f)
    when (Kinds.suitOf(kind)) {
        Suit.DOT -> {
            drawCircle(c, w * 0.13f, mark)
            drawCircle(skin.faceTop, w * 0.07f, mark)
        }
        Suit.BAM -> stick(mark, w * 0.13f, w * 0.34f, c, skin)
        Suit.CRAK -> text(tm, "萬", mark, w * 0.3f, c, FontWeight.Bold)
    }
}

private fun DrawScope.dots(rank: Int, skin: TileSkin, a: Rect) {
    val pos: List<Pair<Float, Float>> = when (rank) {
        1 -> listOf(.5f to .5f)
        2 -> listOf(.5f to .25f, .5f to .75f)
        3 -> listOf(.22f to .18f, .5f to .5f, .78f to .82f)
        4 -> listOf(.28f to .25f, .72f to .25f, .28f to .75f, .72f to .75f)
        5 -> listOf(.24f to .2f, .76f to .2f, .5f to .5f, .24f to .8f, .76f to .8f)
        6 -> listOf(.28f to .17f, .72f to .17f, .28f to .5f, .72f to .5f, .28f to .83f, .72f to .83f)
        7 -> listOf(.2f to .1f, .5f to .27f, .8f to .44f, .3f to .67f, .7f to .67f, .3f to .9f, .7f to .9f)
        8 -> listOf(.3f to .11f, .7f to .11f, .3f to .37f, .7f to .37f, .3f to .63f, .7f to .63f, .3f to .89f, .7f to .89f)
        else -> listOf(.2f to .17f, .5f to .17f, .8f to .17f, .2f to .5f, .5f to .5f, .8f to .5f, .2f to .83f, .5f to .83f, .8f to .83f)
    }
    val rf = floatArrayOf(.36f, .22f, .19f, .19f, .16f, .15f, .13f, .13f, .12f)[rank - 1]
    val r = a.width * rf
    pos.forEachIndexed { i, (x, y) ->
        val c = Offset(a.left + a.width * x, a.top + a.height * y)
        // Traditional sets mix colors; here the middle pip of odd counts is red.
        val ring = if (rank == 1 || (rank % 2 == 1 && i == pos.size / 2 && rank != 7)) skin.dotAccent else skin.dot
        pip(c, r, ring, skin)
    }
}

private fun DrawScope.pip(c: Offset, r: Float, color: Color, skin: TileSkin) {
    drawCircle(color, r, c)
    drawCircle(skin.faceTop, r * 0.7f, c)
    drawCircle(color, r * 0.52f, c)
    drawCircle(skin.faceTop, r * 0.2f, c)
    if (r > 14f) {
        // Petal ring on the big single dot.
        for (k in 0 until 8) {
            val ang = Math.toRadians(k * 45.0)
            drawCircle(color, r * 0.08f, Offset(c.x + (r * 0.61f * cos(ang)).toFloat(), c.y + (r * 0.61f * sin(ang)).toFloat()))
        }
    }
}

private fun DrawScope.bams(rank: Int, skin: TileSkin, a: Rect) {
    if (rank == 1) { bigBamboo(skin, a); return }
    data class S(val x: Float, val y: Float, val red: Boolean = false)
    val rows3 = rank == 7 || rank == 9
    val sticks = when (rank) {
        2 -> listOf(S(.5f, .25f), S(.5f, .75f))
        3 -> listOf(S(.5f, .25f), S(.27f, .75f), S(.73f, .75f))
        4 -> listOf(S(.3f, .25f), S(.7f, .25f), S(.3f, .75f), S(.7f, .75f))
        5 -> listOf(S(.18f, .25f), S(.82f, .25f), S(.5f, .5f, true), S(.18f, .75f), S(.82f, .75f))
        6 -> listOf(.18f, .5f, .82f).flatMap { x -> listOf(S(x, .25f), S(x, .75f)) }
        7 -> listOf(S(.5f, .15f, true)) + listOf(.18f, .5f, .82f).flatMap { x -> listOf(S(x, .5f), S(x, .85f)) }
        8 -> listOf(.13f, .38f, .62f, .87f).flatMap { x -> listOf(S(x, .25f), S(x, .75f)) }
        else -> listOf(.18f, .5f, .82f).flatMap { x -> listOf(.15f, .5f, .85f).map { y -> S(x, y, x == .5f) } }
    }
    val sw = a.width * if (rank == 8) 0.17f else 0.2f
    val sh = a.height * if (rows3) 0.29f else 0.45f
    sticks.forEach { s ->
        stick(Offset(a.left + a.width * s.x, a.top + a.height * s.y), sw, sh, if (s.red) skin.bamAccent else skin.bam, skin)
    }
}

private fun DrawScope.stick(c: Offset, sw: Float, sh: Float, color: Color, skin: TileSkin) {
    val tl = Offset(c.x - sw / 2, c.y - sh / 2)
    drawRoundRect(color, tl, Size(sw, sh), CornerRadius(sw / 2))
    // Lighter left half gives it some roundness.
    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(tl.x + sw * 0.18f, tl.y + sh * 0.08f), Size(sw * 0.22f, sh * 0.84f), CornerRadius(sw))
    // Nodes.
    val band = (sh * 0.05f).coerceAtLeast(1f)
    listOf(0.5f).forEach { f ->
        drawRect(skin.faceTop.copy(alpha = 0.9f), Offset(tl.x, tl.y + sh * f - band / 2), Size(sw, band))
    }
    drawRect(color, Offset(tl.x - sw * 0.12f, tl.y + sh * 0.5f + band / 2), Size(sw * 1.24f, band * 1.4f))
}

private fun DrawScope.bigBamboo(skin: TileSkin, a: Rect) {
    val c = a.center
    val sw = a.width * 0.22f
    val sh = a.height * 0.9f
    // Leaves first so the stalk sits over them.
    listOf(-38f to -1f, 38f to 1f, -25f to -1f).forEachIndexed { i, (deg, side) ->
        val y = a.top + a.height * (if (i == 2) 0.68f else 0.34f)
        val lw = a.width * 0.42f
        val lh = a.width * 0.16f
        val cx = c.x + side * lw * 0.42f
        rotate(deg, Offset(cx, y)) {
            drawOval(skin.leaf, Offset(cx - lw / 2, y - lh / 2), Size(lw, lh))
            drawLine(skin.faceTop.copy(alpha = 0.6f), Offset(cx - lw * 0.4f, y), Offset(cx + lw * 0.4f, y), (lh * 0.08f).coerceAtLeast(1f))
        }
    }
    val tl = Offset(c.x - sw / 2, c.y - sh / 2)
    drawRoundRect(skin.bam, tl, Size(sw, sh), CornerRadius(sw / 2))
    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(tl.x + sw * 0.18f, tl.y + sh * 0.05f), Size(sw * 0.2f, sh * 0.9f), CornerRadius(sw))
    for (f in listOf(0.3f, 0.62f)) {
        drawRect(skin.bamAccent, Offset(tl.x - sw * 0.1f, tl.y + sh * f), Size(sw * 1.2f, sh * 0.045f))
    }
}

private fun DrawScope.soapFrame(skin: TileSkin, a: Rect) {
    val inset = a.width * 0.08f
    val r = Rect(a.left + inset, a.top + inset * 0.5f, a.right - inset, a.bottom - inset * 0.5f)
    val sw = a.width * 0.09f
    drawRoundRect(skin.dot, r.topLeft, r.size, CornerRadius(a.width * 0.08f), style = Stroke(sw))
    val r2 = Rect(r.left + sw * 1.8f, r.top + sw * 1.8f, r.right - sw * 1.8f, r.bottom - sw * 1.8f)
    if (r2.width > 0 && r2.height > 0) drawRoundRect(skin.dot.copy(alpha = 0.7f), r2.topLeft, r2.size, CornerRadius(a.width * 0.04f), style = Stroke(sw * 0.45f))
}

private fun DrawScope.flower(skin: TileSkin, a: Rect) {
    val c = Offset(a.center.x, a.top + a.height * 0.4f)
    val pr = a.width * 0.2f
    // Stem and leaf.
    drawLine(skin.leaf, c, Offset(c.x, a.bottom - a.height * 0.04f), (a.width * 0.06f).coerceAtLeast(1.5f))
    rotate(-35f, Offset(c.x + a.width * 0.16f, a.top + a.height * 0.78f)) {
        drawOval(skin.leaf, Offset(c.x + a.width * 0.02f, a.top + a.height * 0.74f), Size(a.width * 0.3f, a.width * 0.12f))
    }
    for (k in 0 until 5) {
        val deg = k * 72f
        rotate(deg, c) {
            drawOval(skin.flower, Offset(c.x - pr * 0.55f, c.y - pr * 1.9f), Size(pr * 1.1f, pr * 1.55f))
            drawOval(Color.White.copy(alpha = 0.25f), Offset(c.x - pr * 0.2f, c.y - pr * 1.75f), Size(pr * 0.4f, pr * 0.9f))
        }
    }
    drawCircle(skin.jokerAccent, pr * 0.62f, c)
    drawCircle(Color.White.copy(alpha = 0.35f), pr * 0.25f, Offset(c.x - pr * 0.15f, c.y - pr * 0.15f))
}

private fun DrawScope.joker(skin: TileSkin, tm: TextMeasurer, a: Rect, full: Boolean) {
    val starC = Offset(a.center.x, a.top + a.height * if (full) 0.36f else 0.32f)
    star(starC, a.width * if (full) 0.36f else 0.3f, skin.jokerAccent)
    star(starC, a.width * if (full) 0.15f else 0.12f, Color.White.copy(alpha = 0.55f))
    if (full) {
        text(tm, "JOKER", Offset(a.center.x, a.top + a.height * 0.8f), a.width * 0.27f, skin.joker, FontWeight.Black)
    } else {
        text(tm, "J", Offset(a.center.x, a.top + a.height * 0.78f), a.width * 0.5f, skin.joker, FontWeight.Black)
    }
}

private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val ang = Math.toRadians(-90.0 + i * 36.0)
        val x = c.x + (rr * cos(ang)).toFloat()
        val y = c.y + (rr * sin(ang)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}

private fun DrawScope.text(tm: TextMeasurer, s: String, center: Offset, sizePx: Float, color: Color, weight: FontWeight, family: FontFamily? = null) {
    val layout = tm.measure(s, TextStyle(color = color, fontSize = sizePx.toSp(), fontWeight = weight, fontFamily = family))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}
