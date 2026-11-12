package com.homegame.mahjong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.engine.Card
import com.homegame.mahjong.engine.CardHand
import com.homegame.mahjong.engine.TileSpec

// Card inks, printed on ivory like a paper card. Each color is one suit; which suit is up to you.
private val InkBlue = Color(0xFF1F4FA8)
private val InkRed = Color(0xFFC62828)
private val InkGreen = Color(0xFF1E7B45)
private val InkPlain = Color(0xFF1E2733)
private val Paper = Color(0xFFFFF8E7)

private fun suitInk(letter: Char?) = when (letter) {
    'a' -> InkBlue
    'b' -> InkRed
    'c' -> InkGreen
    else -> InkPlain
}

/**
 * A card line with the suit letters dropped and each group colored by suit instead,
 * the way a printed card shows it: same color = same suit, different colors = different suits.
 */
fun cardLine(pattern: String, gap: String = "  "): AnnotatedString = buildAnnotatedString {
    pattern.trim().split(Regex("\\s+")).forEachIndexed { i, token ->
        if (i > 0) append(gap)
        val letter = token.last().takeIf { it in "abc" }
        val body = if (letter != null) token.dropLast(1) else token
        withStyle(SpanStyle(color = suitInk(letter))) { append(body) }
    }
}

/** What the numbers on a sliding hand are allowed to be, in plain words. Null if they're fixed. */
private fun runNote(h: CardHand): String? {
    if (h.run == 0) return null
    val ranks = h.groups.mapNotNull { (it.spec as? TileSpec.Number)?.rank }
    if (ranks.isEmpty()) return null
    val lo = ranks.min()
    val hi = ranks.max()
    return if (lo == hi) "any #" else "up to ${lo + h.run}–${hi + h.run}"
}

/**
 * The whole card, shown over the table. [distances] marks which hands are still possible for the
 * player and how many tiles each needs; null means no game is running.
 */
@Composable
fun CardSheet(
    card: Card,
    distances: Map<String, Int>?,
    onClose: () -> Unit,
    targetId: String? = null,
    onTarget: ((String?) -> Unit)? = null,
) {
    BackHandler(onBack = onClose)
    val best = distances?.values?.minOrNull()
    Column(Modifier.fillMaxSize().felt().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("The card", color = Ivory, fontSize = 26.sp, fontFamily = Display, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Pill("Close", onClick = onClose)
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(InkBlue, InkRed, InkGreen).forEach {
                    Box(Modifier.size(14.dp).background(Paper, CircleShape).padding(2.dp).background(it, CircleShape))
                }
                Text("Each color is a different suit, any suits you like.", color = Soft, fontSize = 13.sp)
            }
            Text("F flower · NEWS winds · D dragon of that suit · 0 Soap · X exposed OK · C concealed · \"any #\" = any number · \"up to\" = slide the whole run higher", color = Soft, fontSize = 13.sp, lineHeight = 17.sp)
            if (distances != null) {
                Text(
                    if (distances.isEmpty()) "None of these hands fit your exposures any more."
                    else "${distances.size} of ${card.hands.size} hands still possible. Badge = tiles you still need; gold = your closest." +
                        if (onTarget != null) " Tap a hand to target it; tap it again to clear." else "",
                    color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        val families = card.hands.groupBy { it.family }.toList()
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            families.forEach { (family, hands) ->
                item(key = "h-$family", span = { GridItemSpan(maxLineSpan) }) { SectionLabel(family, Modifier.padding(top = 6.dp)) }
                items(hands, key = { it.id }) { h ->
                    val away = distances?.get(h.id)
                    val targeted = h.id == targetId
                    HandRow(
                        h, away, possible = distances == null || away != null, closest = away != null && away == best,
                        targeted = targeted,
                        onClick = onTarget?.let { pick ->
                            {
                                if (targeted) pick(null) else { pick(h.id); onClose() }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HandRow(
    h: CardHand,
    away: Int?,
    possible: Boolean,
    closest: Boolean,
    targeted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val (rim, rimColor) = when {
        targeted -> 3.dp to InkBlue
        closest -> 2.dp to Gold
        else -> 0.dp to Color.Transparent
    }
    Column(
        Modifier.fillMaxWidth().alpha(if (possible || targeted) 1f else 0.4f)
            .background(Paper, shape)
            .border(rim, rimColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        if (targeted) {
            Text("◎ TARGET", color = InkBlue, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            cardLine(h.pattern, gap = " "), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 17.sp,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${h.points} · ${if (h.concealed) "C" else "X"}", color = InkPlain.copy(alpha = 0.75f),
                fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold,
            )
            runNote(h)?.let { Text("  $it", color = InkPlain.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)) }
            Spacer(Modifier.weight(1f))
            val tag = when {
                away == 0 -> "✓"
                away != null -> "$away"
                !possible -> "Out"
                else -> null
            }
            tag?.let {
                // Tiles still needed, as a small badge.
                val bg = when { away == 0 -> InkGreen; closest -> GoldDeep; else -> InkPlain.copy(alpha = 0.12f) }
                Text(
                    it, color = if (away == 0 || closest) Color.White else InkPlain.copy(alpha = 0.8f),
                    fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 6.dp),
                )
            }
        }
    }
}
