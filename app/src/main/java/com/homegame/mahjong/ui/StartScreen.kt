package com.homegame.mahjong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.TableViewModel
import com.homegame.mahjong.cosmetics.TileSets
import com.homegame.mahjong.cosmetics.UiStyles
import com.homegame.mahjong.engine.Difficulty
import com.homegame.mahjong.engine.Dragon
import com.homegame.mahjong.engine.Kinds
import com.homegame.mahjong.engine.Rules

private enum class Lineup(val label: String, val blurb: String, val levels: List<Difficulty>, val pips: List<Int>, val aura: Aura) {
    BEGINNER("Beginner", "Relaxed bots that only chase one hand", List(3) { Difficulty.BEGINNER }, listOf(1), Aura.CALM),
    INTERMEDIATE("Intermediate", "Solid players who call and swap jokers", List(3) { Difficulty.INTERMEDIATE }, listOf(2), Aura.DRIFT),
    ADVANCED("Advanced", "Track dead tiles and play defense", List(3) { Difficulty.ADVANCED }, listOf(3), Aura.FOCUS),
    // Hidden until unlocked by beating an all-Advanced table.
    EXPERT("Expert", "Secret level. The strongest bots there are", List(3) { Difficulty.EXPERT }, listOf(4), Aura.STORM),
    // Always last. Levels and pips come from the Custom table setup, not from here.
    CUSTOM("Custom", "Your bots, your house rules", emptyList(), emptyList(), Aura.WORKSHOP),
}

@Composable
fun StartScreen(vm: TableViewModel, onHowToPlay: () -> Unit, onSettings: () -> Unit, onShop: () -> Unit, onCustom: () -> Unit, onCircuits: () -> Unit) {
    val lineups = Lineup.entries.filter { it != Lineup.EXPERT || vm.wallet.expertUnlocked }
    val auras = lineups.map { it.aura }
    val pager = rememberPagerState(initialPage = lineups.indexOf(Lineup.INTERMEDIATE)) { lineups.size }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    fun go(page: Int) { scope.launch { pager.animateScrollToPage(page.coerceIn(0, lineups.lastIndex)) } }
    // Pager position including a swipe in progress. Read it only while drawing, so effects crossfade without recomposing.
    fun pos() = pager.currentPage + pager.currentPageOffsetFraction
    LaunchedEffect(pager) {
        var first = true
        snapshotFlow { pager.currentPage }.collect {
            if (!first) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            first = false
        }
    }

    Box(Modifier.fillMaxSize().drawBehind { drawMenuAura(auras, pos(), ActiveUi.theme, if (Motion.enabled) Motion.t else 0f) }) {
    // No scrolling: spare height goes to the two flexible gaps, and on short screens the whole menu scales down to fit.
    FitToHeight(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            TileFan { auraEnergy(auras, pos()) }
            GearButton(Modifier.align(Alignment.TopEnd), onClick = onSettings)
        }
        Text(
            "Mahjong", color = Ivory, fontSize = 40.sp, fontFamily = Display, fontWeight = FontWeight.Bold,
            modifier = Modifier.shine(GoldHigh, alpha = 0.8f, period = 5f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp).height(1.dp).background(Gold.copy(alpha = 0.6f)))
            Text("  American rules · unlimited games  ", color = Gold, fontSize = 13.sp, letterSpacing = 0.5.sp)
            Box(Modifier.width(28.dp).height(1.dp).background(Gold.copy(alpha = 0.6f)))
        }

        Spacer(Modifier.weight(1f))
        SectionLabel("Career", Modifier.fillMaxWidth())
        Box(Modifier.enter(0)) { CircuitsCard(vm, onCircuits) }

        Spacer(Modifier.height(4.dp))
        SectionLabel("Quick game", Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().enter(1), verticalAlignment = Alignment.CenterVertically) {
            Arrow("‹", "Easier", enabled = pager.currentPage > 0) { go(pager.currentPage - 1) }
            HorizontalPager(pager, Modifier.weight(1f), pageSpacing = 12.dp) { page ->
                val l = lineups[page]
                val pips = if (l == Lineup.CUSTOM) vm.customLevels.map { it.ordinal + 1 } else l.pips
                LineupCard(
                    l, pips,
                    // Cards shrink and fade as they slide away.
                    Modifier.graphicsLayer {
                        val d = abs(pos() - page).coerceAtMost(1f)
                        scaleX = 1f - 0.08f * d
                        scaleY = 1f - 0.08f * d
                        alpha = 1f - 0.5f * d
                    },
                ) { if (l == Lineup.CUSTOM) onCustom() else go(page) }
            }
            Arrow("›", "Harder", enabled = pager.currentPage < lineups.lastIndex) { go(pager.currentPage + 1) }
        }
        Dots(lineups.size, Modifier.enter(2)) { pos() }

        Spacer(Modifier.height(4.dp))
        SectionLabel("Shop", Modifier.fillMaxWidth())
        Box(Modifier.enter(3)) { ShopCard(vm, onShop) }

        if (vm.gamesPlayed > 0) {
            Text(
                "This session: ${vm.gamesPlayed} game${if (vm.gamesPlayed == 1) "" else "s"} · your score ${vm.session[0]}",
                color = Soft, textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BigButton("Deal me in", Modifier.fillMaxWidth(), primary = true) {
                val l = lineups[pager.currentPage]
                if (l == Lineup.CUSTOM) vm.newGame(vm.customLevels, vm.customRules) else vm.newGame(l.levels, Rules())
            }
            Text(
                if (vm.gamesPlayed == 0) "New to mahjong? How to play ›" else "How to play ›",
                color = Gold, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onHowToPlay).padding(vertical = 10.dp),
            )
        }
    }
    }
    }
}

/** Step one level easier or harder. */
@Composable
private fun Arrow(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 36.dp, height = 64.dp)
            .alpha(if (enabled) 1f else 0.25f)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = Gold, fontSize = 38.sp, fontWeight = FontWeight.Light)
    }
}

/** One opponent level, centered: name, what the bots do, and their strength as pips. */
@Composable
private fun LineupCard(lineup: Lineup, pips: List<Int>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().height(120.dp)
            .background(Brush.verticalGradient(listOf(Gold.copy(alpha = 0.16f), Panel)), shape)
            .border(1.5.dp, Gold.copy(alpha = 0.8f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(lineup.label, color = Ivory, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp, maxLines = 1)
        Text(lineup.blurb, color = Soft, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
        // Difficulty as filled pips, one cluster per bot.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            pips.forEach { level ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(maxOf(3, level)) { i ->
                        Box(Modifier.size(8.dp).background(if (i < level) Gold else Color.White.copy(alpha = 0.15f), CircleShape))
                    }
                }
            }
            if (lineup == Lineup.CUSTOM) Text("Set up ›", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

/** Page dots. The current one stretches, and it slides along while swiping. */
@Composable
private fun Dots(count: Int, modifier: Modifier = Modifier, pos: () -> Float) {
    Canvas(modifier.size(width = (count * 14 + 10).dp, height = 8.dp)) {
        val r = size.height / 2
        val step = 14.dp.toPx()
        val p = pos()
        val x0 = (size.width - step * (count - 1)) / 2
        for (i in 0 until count) {
            val near = (1f - abs(p - i)).coerceIn(0f, 1f)
            val half = r + near * 5.dp.toPx()
            val cx = x0 + i * step
            drawRoundRect(
                lerp(Color.White.copy(alpha = 0.25f), Gold, near),
                topLeft = Offset(cx - half, 0f), size = Size(half * 2, size.height), cornerRadius = CornerRadius(r),
            )
        }
    }
}

/**
 * Lays [content] out at least as tall as the space given, so weighted spacers inside soak up spare height.
 * If the content still needs more room than there is, it is scaled down (from the top centre) to fit.
 */
@Composable
private fun FitToHeight(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier.clipToBounds()) { measurables, c ->
        val w = c.maxWidth
        val h = c.maxHeight
        // Min height = screen height: a Column with weights fills it; max unbounded: it can overflow, and we measure by how much.
        val p = measurables.single().measure(Constraints(minWidth = w, maxWidth = w, minHeight = h, maxHeight = Constraints.Infinity))
        val s = if (p.height > h) h.toFloat() / p.height else 1f
        layout(w, h) {
            p.placeWithLayer(0, 0) {
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
        }
    }
}

/** Entry to the tile-set shop: coin balance and unlock progress. Opens the collection. */
@Composable
private fun ShopCard(vm: TableViewModel, onClick: () -> Unit) {
    val wallet = vm.wallet
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(Panel, shape)
            .border(1.dp, Faint, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TileBack(26.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Tiles & tables", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${wallet.owned.size} of ${TileSets.all.size} sets · ${wallet.ownedUi.size} of ${UiStyles.all.size} tables", color = Soft, fontSize = 13.sp)
        }
        CoinAmount("${wallet.coins}")
        Text("  ›", color = Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

/** A loose hand of tiles fanned out above the title. */
@Composable
private fun TileFan(energy: () -> Float) {
    val kinds = listOf(Kinds.FLOWER, 22, Kinds.dragon(Dragon.RED), Kinds.JOKER, 4)
    Box(Modifier.height(74.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        kinds.forEachIndexed { i, k ->
            val d = i - 2
            TileView(
                k, 44.dp,
                modifier = Modifier
                    .offset(x = (d * 38).dp, y = (d * d * 3).dp)
                    .rotate(d * 9f)
                    .graphicsLayer {
                        if (!Motion.enabled) return@graphicsLayer
                        // Easy levels float slowly; hard ones bob faster and higher.
                        val e = energy()
                        val period = 3.4f - 2.2f * e
                        val ph = i * 0.17f
                        translationY = sin(2 * PI.toFloat() * (Motion.t / period + ph)) * (3f + 3f * e) * density
                        rotationZ = (2.5f + 3f * e) * sin(2 * PI.toFloat() * (Motion.t / (period * 1.3f) + ph + 0.25f))
                    },
            )
        }
    }
}

