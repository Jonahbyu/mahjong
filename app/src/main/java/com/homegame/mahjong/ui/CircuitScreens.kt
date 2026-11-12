package com.homegame.mahjong.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.TableViewModel
import com.homegame.mahjong.engine.career.CUT
import com.homegame.mahjong.engine.career.Circuit
import com.homegame.mahjong.engine.career.CircuitRun
import com.homegame.mahjong.engine.career.Circuits
import com.homegame.mahjong.engine.career.HANDS_PER_SESSION
import com.homegame.mahjong.engine.career.LEAGUE_ROUNDS
import com.homegame.mahjong.engine.career.Rival
import com.homegame.mahjong.engine.career.Rivals
import com.homegame.mahjong.engine.career.RunStatus
import com.homegame.mahjong.engine.career.Tier
import com.homegame.mahjong.engine.career.YOU

/** Start-screen entry to the circuits: where you are on the ladder. */
@Composable
fun CircuitsCard(vm: TableViewModel, onClick: () -> Unit) {
    vm.careerTick
    val career = vm.career
    val run = career.run
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(Gold.copy(alpha = 0.12f), shape)
            .border(1.5.dp, Gold, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrophyBadge(career.trophies.size.toString(), won = career.trophies.isNotEmpty(), size = 30)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Circuits", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                when {
                    run != null && !run.over -> "${run.circuit.name} · ${stageLabel(run)}"
                    career.champion -> "Champion · ${career.trophies.size} of ${Circuits.all.size} trophies"
                    else -> "${career.trophies.size} of ${Circuits.all.size} trophies · climb the ladder"
                },
                color = Soft, fontSize = 13.sp,
            )
        }
        Text("  ›", color = Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

private fun stageLabel(run: CircuitRun): String = when (run.status) {
    RunStatus.LEAGUE -> "round ${run.round + 1} of $LEAGUE_ROUNDS" + if (run.inSession) ", hand ${run.hand + 1}" else ""
    RunStatus.FINAL -> "final" + if (run.inSession) ", hand ${run.hand + 1}" else ""
    RunStatus.WON -> "won"
    RunStatus.MISSED_CUT -> "missed the cut"
    RunStatus.LOST_FINAL -> "lost the final"
}

/** The ladder: sixteen circuits in four tiers. */
@Composable
fun CircuitLadderScreen(vm: TableViewModel, onClose: () -> Unit, onOpen: (Int) -> Unit) {
    vm.careerTick
    val career = vm.career
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Row(Modifier.fillMaxWidth()) { Pill("‹ Menu", onClick = onClose) } }
        item {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text("Circuits", color = Ivory, fontSize = 28.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
                Text(
                    "Win a circuit to open the next. ${career.trophies.size} of ${Circuits.all.size} trophies.",
                    color = Soft, fontSize = 13.sp,
                )
            }
        }
        for (tier in Tier.entries) {
            item(key = tier.name) { SectionLabel(tier.label, Modifier.padding(top = 8.dp)) }
            items(Circuits.all.filter { it.tier == tier }, key = { it.index }) { c ->
                LadderRow(c, vm) { if (career.isOpen(c.index)) onOpen(c.index) }
            }
        }
    }
}

@Composable
private fun LadderRow(c: Circuit, vm: TableViewModel, onClick: () -> Unit) {
    val career = vm.career
    val open = career.isOpen(c.index)
    val won = c.index in career.trophies
    val running = career.run?.takeIf { it.circuitIndex == c.index && !it.over }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(if (running != null) Gold.copy(alpha = 0.12f) else Panel, shape)
            .border(if (running != null) 1.5.dp else 1.dp, if (running != null) Gold else Faint, shape)
            .clickable(enabled = open, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrophyBadge(c.number.toString(), won = won, size = 34, dim = !open)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, color = if (open) Color.White else Soft, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (c.major) Text("  MAJOR", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text(
                when {
                    !open -> "Locked · win ${Circuits.all[c.index - 1].name} to open"
                    running != null -> "In progress · ${stageLabel(running)}"
                    won -> "Won · ${c.venue}"
                    else -> c.venue
                },
                color = Soft, fontSize = 12.sp,
            )
        }
        if (open) Text("›", color = Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

/** Round badge with a number, gold once the circuit is won. */
@Composable
private fun TrophyBadge(text: String, won: Boolean, size: Int, dim: Boolean = false) {
    Box(
        Modifier.size(size.dp)
            .background(if (won) Gold else Color.Black.copy(alpha = if (dim) 0.15f else 0.3f), CircleShape)
            .border(1.dp, if (won) GoldRim else Faint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = if (won) Ink else if (dim) Soft.copy(alpha = 0.5f) else Ivory,
            fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = (size * 0.42f).sp,
        )
    }
}

/** One circuit: who's in it, the standings, and the next table. */
@Composable
fun CircuitHubScreen(vm: TableViewModel, index: Int, onBack: () -> Unit) {
    vm.careerTick
    val c = Circuits.all[index]
    val career = vm.career
    val run = career.run?.takeIf { it.circuitIndex == index }
    var confirmRestart by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth()) { Pill("‹ Circuits", onClick = onBack) }
        Column {
            Text("CIRCUIT ${c.number} · ${c.tier.label.uppercase()}${if (c.major) " · MAJOR" else ""}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(c.name, color = Ivory, fontSize = 30.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
            Text(c.venue, color = Soft, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("$LEAGUE_ROUNDS rounds + final")
            if (!c.rules.courtesyPass) Pill("No courtesy pass")
            if (!c.hints) Pill("No hints")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Prize ", color = Soft, fontSize = 13.sp)
            CoinAmount("${c.prize}", fontSize = 14.sp)
            if (index in career.trophies) Text("   Won before: replays pay a quarter", color = Soft, fontSize = 12.sp)
        }

        if (run == null) {
            Intro(vm, c)
        } else {
            RunPanel(vm, run, confirmRestart, onRestartAsk = { confirmRestart = it })
        }
    }
}

@Composable
private fun Intro(vm: TableViewModel, c: Circuit) {
    val other = vm.career.run?.takeIf { !it.over }
    Text(
        "Three sessions of $HANDS_PER_SESSION hands against the field. Placings earn league points (4, 2, 1, 0). " +
            "Finish in the top $CUT to reach the final table against the boss. Win the final to take the trophy.",
        color = Soft, fontSize = 13.sp,
    )
    SectionLabel("The field")
    c.rivals.forEach { RivalCard(vm, Rivals.byId(it), c.level(it), boss = it == c.bossId) }
    if (other != null) {
        Text("Entering ends your run at ${other.circuit.name}.", color = Warn, fontSize = 13.sp)
    }
    BigButton("Enter circuit", Modifier.fillMaxWidth(), primary = true) {
        vm.enterCircuit(c.index)
        vm.playCircuitHand()
    }
}

@Composable
private fun RunPanel(vm: TableViewModel, run: CircuitRun, confirmRestart: Boolean, onRestartAsk: (Boolean) -> Unit) {
    val c = run.circuit
    run.last?.let { last -> LastSession(vm, last) }

    when (run.status) {
        RunStatus.LEAGUE, RunStatus.FINAL -> {
            val final = run.status == RunStatus.FINAL
            SectionLabel(if (final) "Final table" else "Round ${run.round + 1} of $LEAGUE_ROUNDS · your table")
            run.table().forEach { RivalCard(vm, Rivals.byId(it), c.level(it), boss = it == c.bossId) }
            val label = when {
                run.inSession -> "Continue · hand ${run.hand + 1} of $HANDS_PER_SESSION"
                final -> "Play the final"
                else -> "Play session"
            }
            BigButton(label, Modifier.fillMaxWidth(), primary = true) { vm.playCircuitHand() }
        }
        RunStatus.WON -> {
            Banner("Champion", "You won ${c.name}.")
            if (c.index + 1 < Circuits.all.size) {
                val next = Circuits.all[c.index + 1]
                BigButton("On to ${next.name} ›", Modifier.fillMaxWidth(), primary = true) {
                    vm.enterCircuit(next.index)
                    vm.playCircuitHand()
                }
            }
        }
        RunStatus.MISSED_CUT -> Banner("Missed the cut", "Only the top $CUT reach the final. Start the circuit again.")
        RunStatus.LOST_FINAL -> {
            Banner("Lost the final", "Same finalists, another try.")
            BigButton("Replay the final", Modifier.fillMaxWidth(), primary = true) {
                vm.retryFinal()
                vm.playCircuitHand()
            }
        }
    }

    SectionLabel("Standings")
    Standings(vm, run)

    if (run.over) {
        BigButton(if (run.status == RunStatus.WON) "Play it again" else "Restart circuit", Modifier.fillMaxWidth()) {
            vm.enterCircuit(c.index)
            vm.playCircuitHand()
        }
    } else if (confirmRestart) {
        Text("Restart from round 1? This session's progress is lost.", color = Warn, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Restart", Modifier.weight(1f)) { onRestartAsk(false); vm.enterCircuit(c.index) }
            BigButton("Keep going", Modifier.weight(1f), primary = true) { onRestartAsk(false) }
        }
    } else {
        Text(
            "Restart circuit",
            color = Soft, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { onRestartAsk(true) }.padding(vertical = 8.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
    SectionLabel("The field")
    c.rivals.filter { it !in run.table() || run.over }.forEach { RivalCard(vm, Rivals.byId(it), c.level(it), boss = it == c.bossId) }
}

@Composable
private fun LastSession(vm: TableViewModel, last: com.homegame.mahjong.engine.career.SessionResult) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(if (last.final) "Last: the final" else "Last: round ${last.round + 1}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        last.rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                Text(listOf("1st", "2nd", "3rd", "4th")[row.place], color = Soft, modifier = Modifier.width(40.dp))
                Text(nameFor(row.id), color = Color.White, fontWeight = if (row.id == YOU) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                Text(signed(row.score), color = if (row.score > 0) Gold else Soft)
            }
        }
    }
}

@Composable
private fun Standings(vm: TableViewModel, run: CircuitRun) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Faint, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("", modifier = Modifier.width(28.dp))
            Text("Player", color = Soft, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("Pts", color = Soft, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            Text("Score", color = Soft, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
        }
        run.standings().forEachIndexed { i, s ->
            if (i == CUT) Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(Gold.copy(alpha = 0.5f)))
            val me = s.id == YOU
            Row(Modifier.fillMaxWidth()) {
                Text("${i + 1}", color = Soft, modifier = Modifier.width(28.dp))
                Text(
                    nameFor(s.id) + if (s.id == run.circuit.bossId) "  ★" else "",
                    color = if (me) Gold else Color.White, fontWeight = if (me) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text("${s.points}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text(signed(s.score), color = Soft, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
            }
        }
        Text("Top $CUT make the final. ★ boss", color = Soft, fontSize = 11.sp)
    }
}

/** A rival's card: style, strength, record, and how to beat them once you've played them. */
@Composable
private fun RivalCard(vm: TableViewModel, r: Rival, level: Int, boss: Boolean) {
    val record = vm.career.headToHead[r.id]
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(Panel, shape)
            .border(if (boss) 1.5.dp else 1.dp, if (boss) Gold else Faint, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            Modifier.size(36.dp).background(Color.White.copy(alpha = 0.12f), CircleShape).border(1.dp, if (boss) Gold else Faint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(r.name.substringAfterLast(' ').first().toString(), color = Ivory, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("  ${r.title}", color = Gold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Lv $level", color = Soft, fontSize = 12.sp)
            }
            Text(r.blurb, color = Soft, fontSize = 13.sp, lineHeight = 17.sp)
            if (record != null) {
                Text("Beat them: ${r.tip}", color = Ivory, fontSize = 12.sp, lineHeight = 16.sp)
                Text("Your record: ${record[0]}–${record[1]}${if (boss) " · Boss" else ""}", color = Soft, fontSize = 11.sp)
            } else {
                Text(if (boss) "Boss · play them to learn how to beat them" else "Play them to learn how to beat them", color = Soft.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Banner(title: String, line: String) {
    Column(
        Modifier.fillMaxWidth().background(Gold, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Color.Black, fontSize = 24.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
        Text(line, color = Color.Black.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

private fun nameFor(id: String) = if (id == YOU) "You" else Rivals.byId(id).name

private fun signed(n: Int) = if (n > 0) "+$n" else "$n"
