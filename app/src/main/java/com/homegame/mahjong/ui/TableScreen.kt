package com.homegame.mahjong.ui

import androidx.compose.animation.AnimatedVisibility
import com.homegame.mahjong.engine.PassDir
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.HUMAN
import com.homegame.mahjong.HintUi
import com.homegame.mahjong.TargetUi
import com.homegame.mahjong.OpponentUi
import com.homegame.mahjong.TableUi
import com.homegame.mahjong.TableViewModel
import com.homegame.mahjong.engine.Kinds
import com.homegame.mahjong.engine.Phase
import com.homegame.mahjong.engine.career.HANDS_PER_SESSION
import com.homegame.mahjong.engine.career.PLACEMENT_POINTS
import com.homegame.mahjong.engine.career.RunStatus
import com.homegame.mahjong.engine.career.Circuits
import com.homegame.mahjong.engine.Tile

@Composable
fun TableScreen(vm: TableViewModel, onHelp: () -> Unit = {}, onSettings: () -> Unit = {}) {
    val ui = vm.ui ?: return
    var showCard by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().felt()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
            TopBar(vm, onHelp, onCard = { showCard = true }, onSettings = onSettings)
            Spacer(Modifier.height(4.dp))
            Board(ui, Modifier.weight(1f), onDraw = vm::drawFromWall)
            StatusBanner(ui.status, vm.notice)
            if (vm.hintsOn && ui.phase !is Phase.Over && !ui.dead) {
                HintPanel(ui.hints, ui.target, onClearTarget = { vm.chooseTarget(null) }, onPickTarget = { showCard = true })
            }
            if (ui.myExposures.isNotEmpty()) ExposureRow("Your exposures", ui.myExposures, 30.dp)
            Rack(vm, ui)
            ActionBar(vm, ui)
        }
        Overlay(ui.phase is Phase.Vote) { VoteCard(vm) }
        Overlay(ui.phase is Phase.Over) { (ui.phase as? Phase.Over)?.let { GameOverCard(vm, it) } }
        if (showCard) CardSheet(
            vm.card, remember(ui) { vm.handDistances() }, onClose = { showCard = false },
            targetId = vm.targetHandId, onTarget = vm::chooseTarget,
        )
    }
}

@Composable
private fun TopBar(vm: TableViewModel, onHelp: () -> Unit, onCard: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill("‹ Menu", onClick = vm::requestLeave)
        Spacer(Modifier.weight(1f))
        Pill("Card", active = true, onClick = onCard)
        Pill("⇅ ${vm.sortMode.label}", onClick = vm::toggleSort)
        if (vm.hintsAllowed) Pill("Hints", active = vm.showHints, onClick = { vm.showHints = !vm.showHints })
        Pill("Rules", onClick = onHelp)
        GearButton(onClick = onSettings)
    }
}

/** The table seen from above: across player at the top, left and right players down the sides. */
@Composable
private fun Board(ui: TableUi, modifier: Modifier, onDraw: () -> Unit) {
    val bySeat = ui.opponents.associateBy { it.seat }
    Column(modifier.fillMaxWidth().padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        bySeat[2]?.let { TopSeat(it) }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            bySeat[3]?.let { SideSeat(it, Modifier.fillMaxHeight(), degrees = 90f) }
            DiscardPool(ui, Modifier.weight(1f).fillMaxHeight(), onDraw)
            bySeat[1]?.let { SideSeat(it, Modifier.fillMaxHeight(), degrees = -90f) }
        }
    }
}

/** Lays a tile on its side, rotated so its top faces the middle of the table. */
@Composable
private fun Sideways(w: Dp, degrees: Float, content: @Composable (Modifier) -> Unit) {
    Box(Modifier.size(w * 1.36f, w), contentAlignment = Alignment.Center) {
        content(Modifier.wrapContentSize(unbounded = true).rotate(degrees))
    }
}

@Composable
private fun SideSeat(o: OpponentUi, modifier: Modifier, degrees: Float) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.width(46.dp)
            .background(Panel, shape)
            .border(if (o.active) 1.5.dp else 1.dp, if (o.active) Gold else Faint, shape)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(o.short.first().toString(), o.active)
        Text(o.short, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
        if (o.dead) Text("Dead", color = Warn, fontSize = 10.sp, lineHeight = 12.sp) else LevelPips(o.level.ordinal + 1)
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (o.exposures.isEmpty()) {
                val n = minOf(o.concealed, 13).coerceAtLeast(1)
                val tw = minOf(15.dp, (maxHeight - 1.5.dp * (n - 1)) / n)
                Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    repeat(minOf(o.concealed, 13)) { i -> Sideways(tw, degrees) { m -> TileBack(tw, m, phase = i * 0.06f) } }
                }
            } else {
                val n = o.exposures.sumOf { it.size }
                val gaps = 5.dp * (o.exposures.size - 1) + 1.dp * (n - o.exposures.size)
                val tw = minOf(22.dp, (maxHeight - gaps) / n)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    o.exposures.forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            group.forEach { t -> Sideways(tw, degrees) { m -> TileView(t.kind, tw, m) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelPips(level: Int) {
    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(maxOf(3, level)) { i ->
            Box(Modifier.size(5.dp).background(if (i < level) Ivory.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f), CircleShape))
        }
    }
}

@Composable
private fun TopSeat(o: OpponentUi) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .background(Panel, shape)
            .border(if (o.active) 1.5.dp else 1.dp, if (o.active) Gold else Faint, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(o.short.first().toString(), o.active)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.width(88.dp)) {
            val side = when (o.seat) { 1 -> "right"; 2 -> "across"; else -> "left" }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(o.short, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp, maxLines = 1)
                Text(" $side", color = Soft, fontSize = 11.sp, lineHeight = 17.sp)
            }
            Text(
                if (o.dead) "Dead hand" else o.subtitle,
                color = if (o.dead) Warn else Soft, fontSize = 11.sp, lineHeight = 14.sp,
            )
        }
        if (o.exposures.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) { repeat(minOf(o.concealed, 13)) { i -> TileBack(13.dp, phase = i * 0.06f) } }
        } else {
            ExposureTiles(o.exposures, 20.dp)
        }
    }
}

@Composable
private fun Avatar(letter: String, active: Boolean) {
    Box(
        Modifier.size(28.dp)
            .background(
                if (active) Brush.verticalGradient(listOf(GoldHigh, GoldDeep))
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f))),
                CircleShape,
            )
            .border(1.dp, if (active) GoldRim else Faint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = if (active) Ink else Ivory, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExposureTiles(exposures: List<List<Tile>>, w: Dp) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        exposures.forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) { group.forEach { TileView(it.kind, w) } }
        }
    }
}

@Composable
private fun ExposureRow(label: String, exposures: List<List<Tile>>, w: Dp) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        ExposureTiles(exposures, w)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscardPool(ui: TableUi, modifier: Modifier, onDraw: () -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(ui.discards.size) { scroll.animateScrollTo(scroll.maxValue) }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .background(Color.Black.copy(alpha = 0.16f), shape)
            .border(1.dp, Gold.copy(alpha = 0.22f), shape)
            .padding(10.dp)
    ) {
        if (ui.discards.isEmpty()) {
            val charleston = ui.phase is Phase.Charleston || ui.phase is Phase.Courtesy || ui.phase is Phase.Vote
            val dir = when (val p = ui.phase) { is Phase.Charleston -> p.dir; Phase.Courtesy -> PassDir.ACROSS; else -> null }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                dir?.let {
                    val arrow = when (it) { PassDir.RIGHT -> "→"; PassDir.ACROSS -> "↑"; PassDir.LEFT -> "←" }
                    Text(arrow, color = Gold, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp)
                }
                Text(if (charleston) "The Charleston" else "No discards yet", color = Ivory.copy(alpha = 0.85f), fontFamily = Display, fontSize = 22.sp, textAlign = TextAlign.Center)
                if (dir != null) Text(if (ui.phase == Phase.Courtesy) "Courtesy pass across" else "Pass ${dir.label.lowercase()}", color = Soft, fontSize = 13.sp)
                else if (charleston) Text("Trade tiles to shape your hand", color = Soft, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            SectionLabel("Discards · ${ui.discards.size}   Wall · ${ui.wall}", Modifier.align(Alignment.TopStart))
        }
        FlowRow(
            Modifier.padding(top = if (ui.discards.isEmpty()) 0.dp else 20.dp).verticalScroll(scroll)
                .alpha(if (ui.mustDraw) 0.35f else 1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ui.discards.forEachIndexed { i, d ->
                TileView(d.tile.kind, 27.dp, highlight = i == ui.discards.lastIndex)
            }
        }
        if (ui.mustDraw) Wall(ui.wall, Modifier.align(Alignment.Center), onDraw)
    }
}

/** The face-down wall. Tapping any tile draws the next one. */
@Composable
private fun Wall(left: Int, modifier: Modifier, onDraw: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.shadow(10.dp, shape)
            .background(FeltEdge.copy(alpha = 0.92f), shape)
            .border(1.5.dp, Gold, shape)
            .clickable(onClick = onDraw)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Two-high stacks, like a real wall.
        Box {
            Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(7) { TileBack(24.dp) } }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(7) { TileBack(24.dp) } }
        }
        Text("Tap the wall to draw", color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("$left tiles left", color = Soft, fontSize = 12.sp)
    }
}

@Composable
private fun StatusBanner(status: String, notice: String?) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(status, color = Ivory, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, textAlign = TextAlign.Center)
        notice?.let { Text(it, color = Warn, fontSize = 14.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun HintPanel(hints: List<HintUi>, target: TargetUi?, onClearTarget: () -> Unit, onPickTarget: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    if (hints.isEmpty() && target == null) return
    val shape = RoundedCornerShape(14.dp)
    // With a target set, the closest-hand list skips it so the same hand isn't shown twice.
    val others = hints.filter { it.handId != target?.handId }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(Panel, shape)
            .border(1.dp, Faint, shape)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (target != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Your target", Modifier.weight(1f))
                Pill("Change", onClick = onPickTarget)
                Spacer(Modifier.width(6.dp))
                Pill("✕", onClick = onClearTarget)
            }
            val h = target.hint
            if (h == null) {
                PatternChip(target.pattern)
                Text(
                    "${target.family}: can't be made any more with your exposures. Pick another hand.",
                    color = Warn, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                HintBlock(h, label = null)
                NeedLine(h)
            }
            if (expanded && others.isNotEmpty()) Spacer(Modifier.height(10.dp))
        }
        val shown = when {
            target != null && !expanded -> emptyList()
            expanded -> others
            else -> others.take(1)
        }
        shown.forEachIndexed { i, h ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            HintBlock(h, label = if (i == 0) "Closest hand" else null)
        }
        val more = others.size - shown.size
        if (!expanded && more > 0) {
            Text(
                if (target != null) "Tap to compare with your closest hands" else "Tap to see $more more",
                color = Soft, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (target == null && expanded) {
            Text("Tip: open the Card and tap a hand to target it.", color = Soft, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun HintBlock(h: HintUi, label: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            label?.let { SectionLabel(it) }
            Text(
                "${h.family} · ${h.points} pts${if (h.concealed) " · concealed" else ""}",
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        val close = h.missing <= 2
        Text(
            if (h.missing == 0) "Complete!" else "${h.missing} away",
            color = if (close) Ink else Soft, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(if (close) Gold else Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
    Spacer(Modifier.height(4.dp))
    PatternChip(h.pattern)
    Spacer(Modifier.height(6.dp))
    HintTiles(h)
}

/** The hand's line as printed on the card, on a strip of card paper. */
@Composable
private fun PatternChip(pattern: String) {
    Text(
        cardLine(pattern, gap = " "), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 15.sp,
        maxLines = 1, softWrap = false,
        modifier = Modifier.background(Color(0xFFFFF8E7), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Progress in words: tiles in place, jokers used, and exactly which tiles are still missing. */
@Composable
private fun NeedLine(h: HintUi) {
    val total = h.groups.sumOf { it.size }
    val have = h.groups.sumOf { if (it.exposed) it.size else it.have }
    val jokers = h.groups.sumOf { it.jokers }
    val need = h.groups.filter { !it.exposed && it.have < it.size }
        .groupBy { it.kind }
        .map { (kind, gs) -> kind to gs.sumOf { it.size - it.have } }
    val done = "$have of $total in place" + if (jokers > 0) " ($jokers joker${if (jokers > 1) "s" else ""})" else ""
    Text(
        if (need.isEmpty()) "$done. Call Mahjong!"
        else "$done · need " + need.joinToString(", ") { (k, n) -> if (n > 1) "${Kinds.name(k)} ×$n" else Kinds.name(k) },
        color = Soft, fontSize = 12.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HintTiles(h: HintUi) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Shrink tiles so the whole hand fits on one line instead of wrapping.
        val n = h.groups.sumOf { it.size }
        val gaps = 6.dp * (h.groups.size - 1) + 1.dp * (n - h.groups.size)
        val tw = minOf(21.dp, (maxWidth - gaps - 4.dp) / n)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            h.groups.forEach { grp ->
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(grp.size) { i ->
                        when {
                            i < grp.real || grp.exposed -> TileView(grp.kind, tw)
                            i < grp.real + grp.jokers -> TileView(Kinds.JOKER, tw)
                            else -> TileView(grp.kind, tw, faded = true)
                        }
                    }
                }
            }
        }
    }
}

/** Two rows of tiles standing in a wooden tray. */
@Composable
private fun Rack(vm: TableViewModel, ui: TableUi) {
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)
            .shadow(8.dp, shape)
            .background(Brush.verticalGradient(listOf(WoodLight, WoodDark)), shape)
            .border(1.dp, WoodRim.copy(alpha = 0.6f), shape)
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        val gap = 4.dp
        val w = minOf(56.dp, (maxWidth - gap * 6) / 7)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ui.hand.chunked(7).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    row.forEach { t ->
                        TileView(
                            t.kind, w,
                            selected = t.id in vm.selected,
                            highlight = t.id == ui.drawnId,
                            modifier = Modifier.clickable(indication = null, interactionSource = null) { vm.tapTile(t) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionBar(vm: TableViewModel, ui: TableUi) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val call = ui.call
        when {
            call != null -> {
                // The status line above already says who discarded what; the tile sits beside the
                // buttons. Wraps onto a second line when there are many options, so Pass is never cut off.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TileView(call.tile.kind, 36.dp, highlight = true)
                    if (call.canMahjong) BigButton("Mahjong!", primary = true) { vm.mahjong() }
                    call.options.forEach { o ->
                        BigButton(o.label + if (o.jokers > 0) " (${o.jokers}J)" else "") { vm.expose(o) }
                    }
                    BigButton("Pass") { vm.declineCall() }
                }
            }
            ui.phase is Phase.Charleston -> {
                val dir = (ui.phase as Phase.Charleston).dir.label
                BigButton("Pass $dir (${vm.selected.size}/3)", Modifier.fillMaxWidth(), primary = true, enabled = vm.selected.size == 3) { vm.pass() }
            }
            ui.phase is Phase.Courtesy -> {
                val n = vm.selected.size
                BigButton(if (n == 0) "Skip courtesy pass" else "Offer $n across", Modifier.fillMaxWidth(), primary = true) { vm.pass() }
            }
            ui.mustDraw -> BigButton("Draw a tile", Modifier.fillMaxWidth(), primary = true) { vm.drawFromWall() }
            ui.phase is Phase.Turn && (ui.phase as Phase.Turn).seat == HUMAN -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (ui.canMahjong) BigButton("Mahjong!", Modifier.weight(1f), primary = true) { vm.mahjong() }
                    BigButton("Discard", Modifier.weight(1f), enabled = vm.selected.size == 1, primary = !ui.canMahjong) { vm.discard() }
                }
                ui.swaps.forEach { s ->
                    Text(
                        "Swap your ${Kinds.name(s.tile.kind)} for ${if (s.owner == HUMAN) "your" else vm.nameOf(s.owner) + "'s"} joker",
                        color = Gold, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { vm.swap(s) }.padding(10.dp),
                    )
                }
            }
            else -> Spacer(Modifier.height(52.dp))
        }
    }
}

@Composable
private fun Overlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(initialScale = 0.92f), exit = fadeOut() + scaleOut(targetScale = 0.96f)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(indication = null, interactionSource = null) {},
            contentAlignment = Alignment.Center,
        ) {
            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier.padding(24.dp).shadow(16.dp, shape)
                    .background(Brush.verticalGradient(listOf(Felt, FeltEdge)), shape)
                    .border(1.5.dp, Gold, shape)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
    }
}

@Composable
private fun VoteCard(vm: TableViewModel) {
    Text("Second Charleston?", color = Ivory, fontSize = 26.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
    Text("Everyone must agree. Pass left, across, then right again.", color = Soft, textAlign = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton("Yes", primary = true) { vm.vote(true) }
        BigButton("No, start play") { vm.vote(false) }
    }
}

@Composable
private fun GameOverCard(vm: TableViewModel, over: Phase.Over) {
    val r = over.result
    Text(
        when (r.winner) { null -> "Wall game"; HUMAN -> "Mahjong!"; else -> "${vm.nameOf(r.winner!!)} wins" },
        color = Gold, fontSize = 34.sp, fontFamily = Display, fontWeight = FontWeight.Bold,
    )
    r.hand?.let { h ->
        Text(h.family, color = Ivory, textAlign = TextAlign.Center)
        Text(
            cardLine(h.pattern), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            modifier = Modifier.background(Ivory, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        )
        val how = if (r.selfDrawn) "Self-drawn" else "Off ${vm.nameOf(r.discarder!!)}'s discard"
        Text("$how · ${h.points} pts${if (r.jokerless) " · jokerless ×2" else ""}", color = Soft, textAlign = TextAlign.Center)
    }
    if (r.winner == null) Text("The wall ran out. No points change hands.", color = Soft, textAlign = TextAlign.Center)
    Column(
        Modifier.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.width(240.dp)) {
            Spacer(Modifier.weight(1f))
            Text("Game", color = Soft, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
            Text("Session", color = Soft, fontSize = 11.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
        }
        (0..3).forEach { s ->
            Row(Modifier.width(240.dp)) {
                Text(
                    vm.nameOf(s), color = Color.White, maxLines = 1,
                    fontWeight = if (s == r.winner) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f),
                )
                val p = r.payments[s]
                Text(if (p > 0) "+$p" else "$p", color = if (p > 0) Gold else Soft, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                Text("${if (vm.circuitMode) vm.handSession[s] else vm.session[s]}", color = Soft, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
            }
        }
    }
    vm.lastReward?.let { reward ->
        Row(
            Modifier.width(264.dp).background(Gold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                reward.lines.forEach { Text("${it.label}  +${it.coins}", color = Soft, fontSize = 12.sp, lineHeight = 15.sp) }
            }
            CoinAmount("+${reward.total}")
        }
    }
    if (vm.expertJustUnlocked) {
        Column(
            Modifier.width(264.dp).background(Gold, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("SECRET UNLOCKED", color = Color.Black.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("Expert", color = Color.Black, fontSize = 24.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
            Text(
                "You beat the Advanced table. Expert bots are now on the menu.",
                color = Color.Black.copy(alpha = 0.75f), fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }
    }
    if (vm.circuitMode) CircuitFooter(vm) else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton("Play again", primary = true) { vm.newGame() }
        BigButton("Menu") { vm.leaveTable() }
    }
}

/** Bottom of the game-over card during a circuit: next hand, or how the session went. */
@Composable
private fun CircuitFooter(vm: TableViewModel) {
    val result = vm.sessionResult
    if (result == null) {
        Text("Hand ${vm.handNumber} of $HANDS_PER_SESSION done", color = Soft)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton("Next hand", primary = true) { vm.playCircuitHand() }
            BigButton("Standings") { vm.leaveTable() }
        }
        return
    }
    val run = vm.career.run
    val place = result.yourPlace
    Column(
        Modifier.width(264.dp).background(Gold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            when {
                result.final && place == 0 -> "You won the final!"
                result.final -> "Final: ${ordinal(place)} place"
                else -> "Session ${result.round + 1}: ${ordinal(place)} place"
            },
            color = Ivory, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center,
        )
        if (!result.final) Text("+${PLACEMENT_POINTS[place]} league points", color = Soft, fontSize = 13.sp)
        Text(
            when (run?.status) {
                RunStatus.LEAGUE -> "Round ${run.round + 1} of 3 is next."
                RunStatus.FINAL -> "You made the final table!"
                RunStatus.MISSED_CUT -> "You missed the cut for the final."
                RunStatus.LOST_FINAL -> "So close. You can replay the final."
                RunStatus.WON -> "Champion of ${run.circuit.name}!"
                null -> ""
            },
            color = Soft, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
        if (vm.sessionCoins > 0) CoinAmount("+${vm.sessionCoins}")
    }
    vm.circuitWon?.let { won ->
        val last = won == Circuits.all.lastIndex
        Column(
            Modifier.width(264.dp).background(Gold, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TROPHY", color = Color.Black.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(Circuits.all[won].name, color = Color.Black, fontSize = 22.sp, fontFamily = Display, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                when {
                    last -> "You are the Grand Masters champion."
                    won == 11 -> "${Circuits.all[won + 1].name} is open, and Expert bots are on the menu."
                    else -> "${Circuits.all[won + 1].name} is open."
                },
                color = Color.Black.copy(alpha = 0.75f), fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }
    }
    BigButton("Standings", primary = true) { vm.leaveTable() }
}

private fun ordinal(place: Int) = listOf("1st", "2nd", "3rd", "4th")[place]
