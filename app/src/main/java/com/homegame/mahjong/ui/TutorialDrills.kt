package com.homegame.mahjong.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Called when the learner finishes the current lesson's drill. Provided by HowToPlayScreen. */
val LocalDrillSolved = compositionLocalOf<(drill: String) -> Unit> { {} }

private val Good = Color(0xFF9BE59B)
private val DrillBody = Color.White.copy(alpha = 0.9f)
private val DrillDim = Color.White.copy(alpha = 0.65f)

/** One multiple-choice question. [tiles] is shown above the options when present. */
class Question(
    val prompt: String,
    val options: List<String>,
    val answer: Int,
    val explain: String,
    val tiles: List<Int> = emptyList(),
)

/** One "tap the right tile" round. Any index in [correct] is accepted. */
class TapRound(
    val prompt: String,
    val tiles: List<Int>,
    val correct: Set<Int>,
    val explain: String,
    /** Optional per-tile message for a wrong tap; falls back to a generic one. */
    val whyNot: (Int) -> String? = { null },
)

/** Gold-rimmed "Try it" frame that holds a drill. */
@Composable
fun DrillFrame(title: String, progress: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp)
            .background(Color.Black.copy(alpha = 0.25f), shape)
            .border(1.5.dp, Gold.copy(alpha = 0.75f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TRY IT", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.background(Gold, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (progress != null) Text(progress, color = DrillDim, fontSize = 12.sp)
        }
        content()
    }
}

/** A run of multiple-choice questions, one at a time, with feedback after each answer. */
@Composable
fun QuizDrill(title: String, questions: List<Question>, tileWidth: Dp = 30.dp) {
    val solved = LocalDrillSolved.current
    var step by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var firstTry by remember { mutableIntStateOf(0) }
    var missed by remember { mutableStateOf(false) }
    val done = step >= questions.size
    LaunchedEffect(done) { if (done) solved(title) }

    DrillFrame(title, if (done) null else "${step + 1}/${questions.size}") {
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "quiz") { s ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (s >= questions.size) {
                    Finished("$firstTry of ${questions.size} right on the first try.") {
                        step = 0; picked = null; firstTry = 0; missed = false
                    }
                    return@Column
                }
                val q = questions[s]
                Text(q.prompt, color = DrillBody, fontSize = 15.sp, lineHeight = 21.sp)
                if (q.tiles.isNotEmpty()) Tiles(q.tiles, tileWidth)
                val p = picked
                q.options.forEachIndexed { i, opt ->
                    val state = when {
                        p == null -> OptionState.Idle
                        i == q.answer && p == q.answer -> OptionState.Right
                        i == p -> OptionState.Wrong
                        else -> OptionState.Idle
                    }
                    OptionButton(opt, state, enabled = p != q.answer) {
                        picked = i
                        if (i == q.answer) { if (!missed) firstTry++ } else missed = true
                    }
                }
                if (p != null) {
                    Feedback(p == q.answer, if (p == q.answer) q.explain else "Not quite. Try another answer.")
                    if (p == q.answer) {
                        NextButton(if (s == questions.lastIndex) "Finish" else "Next question") {
                            step++; picked = null; missed = false
                        }
                    }
                }
            }
        }
    }
}

/** Rounds of "tap the tile that ...". */
@Composable
fun TapDrill(title: String, rounds: List<TapRound>, tileWidth: Dp = 36.dp) {
    val solved = LocalDrillSolved.current
    var step by remember { mutableIntStateOf(0) }
    var tapped by remember { mutableStateOf<Int?>(null) }
    val done = step >= rounds.size
    LaunchedEffect(done) { if (done) solved(title) }

    DrillFrame(title, if (done) null else "${step + 1}/${rounds.size}") {
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tap") { s ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (s >= rounds.size) {
                    Finished("All ${rounds.size} found.") { step = 0; tapped = null }
                    return@Column
                }
                val r = rounds[s]
                val t = tapped
                val right = t != null && t in r.correct
                Text(r.prompt, color = DrillBody, fontSize = 15.sp, lineHeight = 21.sp)
                TapTiles(r.tiles, tileWidth, selected = { it == t }, highlight = { right && it == t }) { i ->
                    if (!right) tapped = i
                }
                if (t != null) {
                    Feedback(right, if (right) r.explain else r.whyNot(r.tiles[t]) ?: "That's the ${tileName(r.tiles[t])}. Try again.")
                    if (right) NextButton(if (s == rounds.lastIndex) "Finish" else "Next") { step++; tapped = null }
                }
            }
        }
    }
}

/**
 * Pick exactly [count] tiles from a rack, then check. [judge] returns null when the pick is
 * right, or a message explaining what's wrong.
 */
@Composable
fun PickDrill(
    title: String,
    prompt: String,
    rack: List<Int>,
    count: Int,
    success: String,
    tileWidth: Dp = 32.dp,
    target: String? = null,
    judge: (List<Int>) -> String?,
) {
    val solved = LocalDrillSolved.current
    val picks: SnapshotStateList<Int> = remember { emptyList<Int>().toMutableStateList() }
    var verdict by remember { mutableStateOf<String?>(null) }
    var checked by remember { mutableStateOf(false) }
    val right = checked && verdict == null

    DrillFrame(title) {
        Text(prompt, color = DrillBody, fontSize = 15.sp, lineHeight = 21.sp)
        if (target != null) {
            Text(
                cardLine(target), fontWeight = FontWeight.Bold, fontSize = 15.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().background(Ivory, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        TapTiles(rack, tileWidth, selected = { it in picks }, highlight = { right && it in picks }) { i ->
            if (right) return@TapTiles
            checked = false
            if (i in picks) picks.remove(i)
            else if (count == 1) { picks.clear(); picks.add(i) }
            else if (picks.size < count) picks.add(i)
        }
        if (!checked) {
            Text("${picks.size} of $count picked", color = DrillDim, fontSize = 13.sp)
            if (picks.size == count) NextButton("Check") {
                verdict = judge(picks.map { rack[it] })
                checked = true
                if (verdict == null) solved(title)
            }
        } else {
            Feedback(right, verdict ?: success)
            if (right) NextButton("Reset") { picks.clear(); checked = false; verdict = null }
        }
    }
}

// ---- Pieces -----------------------------------------------------------------------------------

private enum class OptionState { Idle, Right, Wrong }

@Composable
private fun OptionButton(text: String, state: OptionState, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val border by animateColorAsState(
        when (state) { OptionState.Right -> Good; OptionState.Wrong -> Warn; OptionState.Idle -> Color.White.copy(alpha = 0.3f) },
        label = "optBorder",
    )
    val fill = when (state) {
        OptionState.Right -> Good.copy(alpha = 0.18f)
        OptionState.Wrong -> Warn.copy(alpha = 0.15f)
        OptionState.Idle -> Color.Black.copy(alpha = 0.2f)
    }
    Text(
        text, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth()
            .background(fill, shape)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    )
}

@Composable
private fun Feedback(right: Boolean, text: String) {
    Row(Modifier.revealOnShow(text)) {
        Text(if (right) "✓" else "✗", color = if (right) Good else Warn, fontWeight = FontWeight.Black, fontSize = 16.sp,
            modifier = Modifier.width(22.dp))
        Text(text, color = if (right) Color.White else Warn, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun NextButton(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().revealOnShow(text), contentAlignment = Alignment.CenterEnd) {
        BigButton(text, primary = true, onClick = onClick)
    }
}

@Composable
private fun Finished(summary: String, onAgain: () -> Unit) {
    Text("Done! $summary", color = Good, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Box(Modifier.fillMaxWidth().revealOnShow(summary), contentAlignment = Alignment.CenterEnd) {
        BigButton("Try again", onClick = onAgain)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tiles(kinds: List<Int>, width: Dp) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        kinds.forEach { TileView(it, width) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TapTiles(
    kinds: List<Int>,
    width: Dp,
    selected: (Int) -> Boolean,
    highlight: (Int) -> Boolean,
    onTap: (Int) -> Unit,
) {
    // Top padding leaves room for the selected tile's lift.
    FlowRow(
        Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        kinds.forEachIndexed { i, k ->
            TileView(
                k, width, selected = selected(i), highlight = highlight(i),
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { onTap(i) },
            )
        }
    }
}

/** Scrolls this element into view when it first appears (or [key] changes), so feedback below the fold isn't missed. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.revealOnShow(key: Any): Modifier {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(key) {
        delay(120) // let the layout settle first
        requester.bringIntoView()
    }
    return bringIntoViewRequester(requester)
}

private fun tileName(k: Int) = com.homegame.mahjong.engine.Kinds.name(k)
