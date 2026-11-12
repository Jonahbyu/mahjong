package com.homegame.mahjong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.engine.Card
import com.homegame.mahjong.engine.Dragon
import com.homegame.mahjong.engine.Kinds
import com.homegame.mahjong.engine.Suit
import com.homegame.mahjong.engine.Wind
import com.homegame.mahjong.engine.Kinds.JOKER

private val Body = Color.White.copy(alpha = 0.88f)
private val Dim = Color.White.copy(alpha = 0.65f)

/** [drills] = how many drills the lesson has; its tab gets a check once all are finished. */
private class Lesson(val tab: String, val title: String, val drills: Int = 1, val content: @Composable ColumnScope.() -> Unit)

private val lessons = listOf(
    Lesson("Goal", "What you're trying to do") { GoalLesson() },
    Lesson("Tiles", "The 152 tiles") { TilesLesson() },
    Lesson("The card", "Reading the card") { CardLesson() },
    Lesson("Charleston", "Passing tiles before play") { CharlestonLesson() },
    Lesson("Your turn", "Draw, then discard") { TurnLesson() },
    Lesson("Calling", "Grabbing someone's discard") { CallingLesson() },
    Lesson("Jokers", "How jokers work", drills = 2) { JokersLesson() },
    Lesson("Scoring", "Winning and paying") { ScoringLesson() },
    Lesson("Tips", "Strategy for new players", drills = 0) { TipsLesson() },
    Lesson("Hands", "Every hand on the practice card", drills = 0) { HandsLesson() },
)

@Composable
fun HowToPlayScreen(onClose: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    // Finished drills, as "lessonIndex/drillTitle".
    var finished by rememberSaveable { mutableStateOf(emptySet<String>()) }
    fun lessonDone(i: Int) = lessons[i].drills > 0 && finished.count { it.startsWith("$i/") } >= lessons[i].drills
    val scroll = rememberScrollState()
    val tabScroll = rememberScrollState()
    LaunchedEffect(index) {
        scroll.scrollTo(0)
        tabScroll.animateScrollTo(tabScroll.maxValue * index / lessons.lastIndex)
    }
    val lesson = lessons[index]

    Column(Modifier.fillMaxSize().felt().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹ Back", color = Color.White) }
            Spacer(Modifier.weight(1f))
            Text("How to play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text("${index + 1}/${lessons.size}", color = Dim, fontSize = 13.sp, modifier = Modifier.padding(end = 14.dp))
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(tabScroll).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            lessons.forEachIndexed { i, l -> TabChip(if (lessonDone(i)) "✓ ${l.tab}" else l.tab, i == index) { index = i } }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(lesson.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            // key: each lesson's drills start fresh instead of inheriting another lesson's state.
            key(index) {
                CompositionLocalProvider(LocalDrillSolved provides { drill -> finished = finished + "$index/$drill" }) {
                    lesson.content(this)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                if (index > 0) BigButton("‹ ${lessons[index - 1].tab}") { index-- }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                if (index < lessons.lastIndex) BigButton("${lessons[index + 1].tab} ›", primary = true) { index++ }
                else BigButton("Done", primary = true, onClick = onClose)
            }
        }
    }
}

// ---- Lessons ----------------------------------------------------------------------------------

@Composable
private fun GoalLesson() {
    P("American mahjong is a four-player game. Everyone starts with 13 tiles (East gets 14 and goes first).")
    P("You win by being first to collect 14 tiles that exactly match one hand printed on the card. Calling it out is called \"Mahjong!\"")
    TileRow(listOf(Kinds.FLOWER, Kinds.FLOWER, s(Suit.BAM, 2), s(Suit.BAM, 2), s(Suit.BAM, 2)), 34.dp)
    P("Unlike other styles of mahjong, you can't make up your own hand. If it isn't on the card, it doesn't count.")
    Steps(
        "Deal: everyone gets 13 tiles, East gets 14.",
        "Charleston: swap unwanted tiles with other players.",
        "Play: take turns drawing a tile and discarding one.",
        "First to match a card hand wins. If the wall runs out, nobody wins.",
    )
    Callout("In this app you sit East, at the bottom. The three bots are South, West and North.")
    QuizDrill("Quick check", listOf(
        Question("How many tiles make a winning hand?", listOf("13", "14", "15"), 1,
            "14. You hold 13 and win on the 14th, whether you draw it or call a discard."),
        Question("You've built a hand that isn't printed on the card. Can you call Mahjong?",
            listOf("Yes, if it's all pungs", "No, it has to be on the card"), 1,
            "Only hands on the card count. That's the big difference from other mahjong styles."),
        Question("Who goes first?", listOf("East", "Whoever is youngest", "North"), 0,
            "East starts with 14 tiles and opens by discarding."),
    ))
}

@Composable
private fun TilesLesson() {
    P("There are three suits, numbered 1 to 9. Four copies of every tile.")
    Labeled("Bams", Suit.BAM)
    Labeled("Craks", Suit.CRAK)
    Labeled("Dots", Suit.DOT)
    P("Winds: North, East, West, South. Four of each.")
    TileRow(Wind.entries.map { Kinds.wind(it) })
    P("Dragons: four of each. Each dragon belongs to a suit, which matters when the card says \"matching dragon\".")
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DragonPair(Dragon.GREEN, "Green → Bams")
        DragonPair(Dragon.RED, "Red → Craks")
        DragonPair(Dragon.WHITE, "Soap → Dots")
    }
    P("The Soap (white dragon) also stands in for zero in year hands like 2026.")
    P("Flowers (8) and Jokers (8). All flowers are interchangeable.")
    TileRow(listOf(Kinds.FLOWER, Kinds.FLOWER, Kinds.JOKER, Kinds.JOKER))
    Callout("That's 108 suit tiles + 16 winds + 12 dragons + 8 flowers + 8 jokers = 152.")
    TapDrill("Spot the tile", listOf(
        TapRound("Tap the Crak.",
            listOf(s(Suit.BAM, 5), s(Suit.DOT, 5), s(Suit.CRAK, 5), Kinds.wind(Wind.EAST)), setOf(2),
            "That's the 5 Crak."),
        TapRound("Tap the Soap.",
            listOf(Kinds.dragon(Dragon.RED), Kinds.FLOWER, Kinds.dragon(Dragon.WHITE), JOKER), setOf(2),
            "The Soap is the white dragon. It goes with Dots and doubles as 0."),
        TapRound("Tap the dragon that goes with Bams.",
            listOf(Kinds.dragon(Dragon.WHITE), Kinds.dragon(Dragon.RED), Kinds.dragon(Dragon.GREEN)), setOf(2),
            "Green goes with Bams.",
            whyNot = { if (it == Kinds.dragon(Dragon.RED)) "Red goes with Craks." else "Soap goes with Dots." }),
        TapRound("Tap the tile you can never pass in the Charleston.",
            listOf(Kinds.FLOWER, Kinds.wind(Wind.NORTH), JOKER, s(Suit.DOT, 1)), setOf(2),
            "Jokers stay with you. More on that later."),
    ))
}

@Composable
private fun CardLesson() {
    P("Each line on the card is one hand. Hands are grouped into families like 2468, Consecutive, or Winds & Dragons.")
    P("Read a hand left to right. Repeated characters make one group:")
    Glossary(
        "11" to "a pair (2 of a kind)",
        "222" to "a pung (3 of a kind)",
        "3333" to "a kong (4 of a kind)",
        "44444" to "a quint (5 of a kind, needs jokers)",
        "2026" to "single tiles in a row",
    )
    P("Letters after a group tell you the suit. a, b and c are always three different suits, but you pick which.")
    Example("222a 4444a 666a 8888a", "All one suit. Could be all Bams, all Craks or all Dots.")
    TileRow(listOf(2, 2, 2, 4, 4, 4, 4, 6, 6, 6, 8, 8, 8, 8).map { s(Suit.DOT, it) }, 20.dp)
    Example("FF 2222a 6666b 8888c", "Two flowers, then three suits: e.g. 2s in Bams, 6s in Craks, 8s in Dots.")
    Glossary(
        "F" to "Flower",
        "D" to "the dragon matching that group's suit",
        "0" to "Soap (white dragon)",
        "N E W S" to "the winds",
    )
    P("Some hands slide. In Consecutive and Like Numbers hands, any run of numbers works. 111a 2222a 333b 4444b also allows 3-4-5-6, 5-6-7-8, and so on.")
    P("C means concealed: you must build the whole hand yourself and can't call discards for it (except the final Mahjong tile). X means exposed hands are fine.")
    Callout("This app uses an original practice card, not the official NMJL card. The patterns follow the same families. The last lesson lists every hand.")
    QuizDrill("Does it match?", listOf(
        Question("Card line: 222a 4444a 666a 8888a. Does this rack match?", listOf("Yes", "No"), 0,
            "All four groups are Dots, so a = Dots. Any one suit works.",
            listOf(2, 2, 2, 4, 4, 4, 4, 6, 6, 6, 8, 8, 8, 8).map { s(Suit.DOT, it) }),
        Question("Same line: 222a 4444a 666a 8888a. This rack?", listOf("Yes", "No"), 1,
            "No. Every group is marked a, so they all need the same suit. The 2s here are Bams.",
            List(3) { s(Suit.BAM, 2) } + listOf(4, 4, 4, 4, 6, 6, 6, 8, 8, 8, 8).map { s(Suit.DOT, it) }),
        Question("Card line: FF 2222a 6666b 8888c. This rack?", listOf("Yes", "No"), 0,
            "Yes. a, b and c just need to be three different suits: Bams, Craks, Dots here.",
            listOf(Kinds.FLOWER, Kinds.FLOWER) + List(4) { s(Suit.BAM, 2) } + List(4) { s(Suit.CRAK, 6) } + List(4) { s(Suit.DOT, 8) }),
        Question("Same line: FF 2222a 6666b 8888c. This rack?", listOf("Yes", "No"), 1,
            "No. The 2s and 6s are both Bams, but a and b must be different suits.",
            listOf(Kinds.FLOWER, Kinds.FLOWER) + List(4) { s(Suit.BAM, 2) } + List(4) { s(Suit.BAM, 6) } + List(4) { s(Suit.DOT, 8) }),
    ), tileWidth = 20.dp)
}

@Composable
private fun CharlestonLesson() {
    P("Before play, everyone trades away tiles they don't want. Pick 3 tiles each time. Everyone passes at once.")
    Steps("Pass right", "Pass across", "Pass left")
    P("Then a vote: a second Charleston happens only if all four players say yes. It goes the other way:")
    Steps("Pass left", "Pass across", "Pass right")
    P("Last, the courtesy pass: you and the player across from you each offer 0 to 3 tiles. You trade the smaller number. If either of you offers 0, nothing changes.")
    Callout("You can never pass a joker. Keep them.")
    H("How to choose")
    Bullets(
        "Look at your tiles and find 2 or 3 hands on the card you're closest to. The hint panel shows these.",
        "Pass tiles none of those hands use.",
        "Lone winds and dragons are usually safe to pass early.",
        "Don't lock into one hand yet. The Charleston can change everything.",
    )
    Callout("In the app: tap 3 tiles in your rack, then press Pass.")
    CharlestonDrill()
}

@Composable
private fun TurnLesson() {
    P("East starts with 14 tiles and just discards. After that, play goes around the table. On your turn:")
    Steps(
        "Draw one tile from the wall (the app does this for you).",
        "If you now match a card hand, press Mahjong!",
        "Otherwise, discard one tile face up in the middle.",
    )
    P("After each discard, the other players get a chance to call it (next lesson). If nobody does, the next player draws.")
    P("The Wall counter at the top shows how many tiles are left. At zero, the game is a draw and no points change hands.")
    Callout("In the app: tap a tile to select it (it lifts up), then press Discard. Use Sort to group your rack by suit or number.")
    DiscardDrill()
}

@Composable
private fun CallingLesson() {
    P("When someone discards, you can take it if it lets you expose a group of 3 or more matching tiles.")
    TileRow(listOf(s(Suit.CRAK, 5), s(Suit.CRAK, 5), Kinds.JOKER), 30.dp)
    P("You put the group face up in front of you. Then it's your turn: discard a tile. Play continues from you, so players between can lose their turn.")
    Bullets(
        "You can't call a discard for a pair or a single. Only for Mahjong.",
        "You can call any discard to win, even for a pair.",
        "The group has to fit a hand on the card. The app won't let you call something no hand uses.",
        "Once exposed, a group can't go back. It narrows which hands you can still make.",
        "Concealed (C) hands can't call discards except for the winning tile.",
        "A discarded joker can't be called.",
    )
    P("If two players want the same tile, Mahjong beats an exposure. If both want it for the same reason, the player next in turn order gets it.")
    Callout("In the app: a call prompt appears with your options, or press Pass. The app skips the prompt when you can't use the tile.")
    QuizDrill("Call or pass?", listOf(
        Question("You hold these. West discards a 5 Crak, and your hand needs a pung of 5 Craks. Call it?",
            listOf("Call", "Pass"), 0,
            "Call. The discard completes a group of 3, so you can expose it.",
            listOf(s(Suit.CRAK, 5), s(Suit.CRAK, 5))),
        Question("You hold one 3 Dot. North discards a 3 Dot, and your hand needs a pair of 3 Dots. Call it?",
            listOf("Call", "Pass"), 1,
            "Pass. You can't call a discard to make a pair, unless it wins the game.",
            listOf(s(Suit.DOT, 3))),
        Question("Same pair, but that 3 Dot is the very last tile you need. Now?",
            listOf("Call Mahjong", "Pass"), 0,
            "Call Mahjong. Any discard can be called for the winning tile, even for a pair or single."),
        Question("South discards a joker. You hold two 7 Bams. Call it?",
            listOf("Call", "Pass"), 1,
            "Pass. A discarded joker is dead. Nobody can call it.",
            listOf(s(Suit.BAM, 7), s(Suit.BAM, 7))),
        Question("You want a discard for a pung. The player after you wants it for Mahjong. Who gets it?",
            listOf("You, you're first in turn order", "Them, Mahjong wins"), 1,
            "Mahjong beats any exposure call, no matter who sits where."),
    ))
}

@Composable
private fun JokersLesson() {
    P("A joker can stand in for any tile in a group of 3 or more: pungs, kongs, quints.")
    TileRow(listOf(s(Suit.BAM, 7), s(Suit.BAM, 7), Kinds.JOKER, Kinds.JOKER), 30.dp)
    P("Jokers can never be used in a pair or as a single tile. In 11 or 2026, every tile has to be real.")
    H("Swapping jokers")
    P("On your turn, if any exposed group on the table has a joker in it and you hold the real tile it stands for, you can trade your tile for that joker.")
    TileRow(listOf(s(Suit.DOT, 4), s(Suit.DOT, 4), Kinds.JOKER), 30.dp)
    P("Here, a 4 Dot from your rack takes that joker. This works on anyone's exposures, including your own.")
    Callout("In the app: when a swap is possible, a gold line appears above your buttons. Tap it to swap.")
    QuizDrill("Legal joker?", listOf(
        Question("A pung of 7 Bams with one joker. Legal?", listOf("Legal", "Not allowed"), 0,
            "Legal. Pungs, kongs and quints can take jokers.",
            listOf(s(Suit.BAM, 7), s(Suit.BAM, 7), JOKER)),
        Question("A pair of North winds, one of them a joker. Legal?", listOf("Legal", "Not allowed"), 1,
            "Not allowed. Pairs need two real tiles.",
            listOf(Kinds.wind(Wind.NORTH), JOKER)),
        Question("The year 2026, with a joker standing in for the 6. Legal?", listOf("Legal", "Not allowed"), 1,
            "Not allowed. 2026 is four singles, and singles can't be jokers.",
            listOf(s(Suit.BAM, 2), Kinds.dragon(Dragon.WHITE), s(Suit.BAM, 2), JOKER)),
        Question("A kong of Green Dragons with two jokers. Legal?", listOf("Legal", "Not allowed"), 0,
            "Legal. Even half a kong can be jokers.",
            listOf(Kinds.dragon(Dragon.GREEN), Kinds.dragon(Dragon.GREEN), JOKER, JOKER)),
    ))
    JokerSwapDrill()
}

@Composable
private fun ScoringLesson() {
    P("Each hand on the card has a point value. When someone wins:")
    Bullets(
        "Win off a discard: the player who threw it pays double. The other two pay the hand's value.",
        "Self-drawn win (you drew it yourself): all three pay double.",
        "Jokerless: if you win without any jokers, the value doubles. Doesn't apply to hands that can't use jokers anyway.",
        "Wall game (wall runs out): nobody pays.",
    )
    Example("A 25-point hand off a discard", "Discarder pays 50, the other two pay 25 each. Winner gets +100.")
    P("The menu shows your running total for the session.")
    QuizDrill("Who pays what?", listOf(
        Question("You win a 25-point hand off West's discard. What does West pay?",
            listOf("25", "50", "75"), 1, "50. The discarder pays double. The other two pay 25 each."),
        Question("Same 25-point hand, but you drew the winning tile yourself. What does each player pay?",
            listOf("25", "50", "75"), 1, "50 each, so you collect 150."),
        Question("A 30-point hand with no jokers anywhere, won off a discard. What does the discarder pay?",
            listOf("30", "60", "120"), 2, "120. Jokerless doubles it to 60, then the discarder pays double that."),
    ))
}

@Composable
private fun TipsLesson() {
    Bullets(
        "Pick your hand late. Keep 2 or 3 options open through the Charleston.",
        "Jokers are gold. Use them in big groups. Never pass or discard one unless you have no choice.",
        "Pairs and singles are the hardest part since jokers can't help. Hands full of pungs and kongs are easier for beginners.",
        "Don't call too early. Each exposure shows the table what you're building and locks you in.",
        "Watch the discards. If three of a tile are already out, a pung of it is nearly impossible.",
        "Watch exposures. If someone shows 2468 groups, don't discard them an even number in their suit late in the game.",
        "Start with Beginner bots and hints on. Turn hints off once the card feels familiar.",
    )
    H("Learning together")
    Bullets(
        "Take turns: one person holds the phone and plays, the other reads the hint panel and argues about discards.",
        "Each play one game against the bots and compare session scores.",
        "When you move to real tiles, the rules here carry over. Buy the current year's NMJL card for real play; its hands change every year.",
    )
}

@Composable
private fun HandsLesson() {
    val card = remember { Card.practice() }
    P("Every hand on this app's practice card. Each color is a different suit (any suits you like). X = exposed OK, C = concealed only.")
    card.hands.groupBy { it.family }.forEach { (family, hands) ->
        H(family)
        hands.forEach { h ->
            Row(
                Modifier.fillMaxWidth().background(Ivory, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(cardLine(h.pattern), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(if (h.concealed) "C" else "X", color = Color(0x991E2733), fontSize = 13.sp, modifier = Modifier.width(22.dp))
                Text("${h.points}", color = Color(0xFF1E2733), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ---- Hands-on drills --------------------------------------------------------------------------

@Composable
private fun CharlestonDrill() {
    val target = listOf(2, 4, 6, 8).map { s(Suit.DOT, it) }.toSet()
    val rack = listOf(
        s(Suit.DOT, 2), s(Suit.DOT, 2), s(Suit.BAM, 3), s(Suit.DOT, 4), s(Suit.DOT, 4), Kinds.wind(Wind.NORTH),
        s(Suit.DOT, 6), JOKER, s(Suit.CRAK, 7), s(Suit.DOT, 8), s(Suit.DOT, 8), Kinds.dragon(Dragon.GREEN), s(Suit.BAM, 5),
    )
    PickDrill(
        title = "Your first pass",
        prompt = "You're leaning toward this hand in Dots. Pick 3 tiles to pass right.",
        target = "222a 4444a 666a 8888a",
        rack = rack, count = 3, tileWidth = 30.dp,
        success = "Good pass. None of those help an all-Dots 2468 hand, and you kept your joker.",
    ) { picked ->
        when {
            JOKER in picked -> "Never pass a joker. It's not even allowed."
            picked.any { it in target } -> "One of those is an even Dot. Your hand needs it, so keep it."
            else -> null
        }
    }
}

@Composable
private fun DiscardDrill() {
    val rack = List(3) { s(Suit.DOT, 2) } + List(3) { s(Suit.DOT, 4) } + listOf(s(Suit.CRAK, 5)) +
        List(2) { s(Suit.DOT, 6) } + List(3) { s(Suit.DOT, 8) } + listOf(JOKER, JOKER)
    PickDrill(
        title = "Pick the discard",
        prompt = "You just drew and hold 14 tiles. Same target hand. Which tile do you throw?",
        target = "222a 4444a 666a 8888a",
        rack = rack, count = 1, tileWidth = 24.dp,
        success = "Right. The 5 Crak fits nothing. Now you're one tile from Mahjong: any 8 Dot or joker wins.",
    ) { picked ->
        when (picked.single()) {
            JOKER -> "Keep jokers. They're filling your kongs."
            s(Suit.CRAK, 5) -> null
            else -> "Your hand needs every even Dot you have."
        }
    }
}

@Composable
private fun JokerSwapDrill() {
    Text("West has exposed this:", color = Dim, fontSize = 14.sp)
    TileRow(listOf(s(Suit.DOT, 4), s(Suit.DOT, 4), JOKER), 30.dp)
    PickDrill(
        title = "Steal the joker",
        prompt = "It's your turn. Tap the tile from your rack you'd trade for West's joker.",
        rack = listOf(s(Suit.BAM, 4), s(Suit.CRAK, 9), s(Suit.DOT, 4), Kinds.wind(Wind.SOUTH), s(Suit.DOT, 5)),
        count = 1, tileWidth = 34.dp,
        success = "Yes. Your real 4 Dot goes into West's pung, and the joker comes to you.",
    ) { picked ->
        when (picked.single()) {
            s(Suit.DOT, 4) -> null
            s(Suit.BAM, 4) -> "Close, but that's a 4 Bam. The joker is standing in for a 4 Dot."
            else -> "The joker is standing in for a 4 Dot. Only that exact tile can take it."
        }
    }
}

// ---- Building blocks --------------------------------------------------------------------------

private fun s(suit: Suit, rank: Int) = Kinds.suited(suit, rank)

@Composable
private fun P(text: String) = Text(text, color = Body, fontSize = 16.sp, lineHeight = 23.sp)

@Composable
private fun H(text: String) =
    Text(text.uppercase(), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))

@Composable
private fun Steps(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, t ->
            Row {
                Text("${i + 1}", color = Felt, fontWeight = FontWeight.Black, fontSize = 13.sp,
                    modifier = Modifier.background(Gold, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
                Spacer(Modifier.width(10.dp))
                Text(t, color = Body, fontSize = 15.sp, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun Bullets(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { t ->
            Row {
                Text("•", color = Gold, fontSize = 16.sp, modifier = Modifier.width(16.dp))
                Text(t, color = Body, fontSize = 15.sp, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun Callout(text: String) {
    Text(
        text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth()
            .background(FeltDark, RoundedCornerShape(10.dp))
            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    )
}

@Composable
private fun Glossary(vararg rows: Pair<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { (k, v) ->
            Row {
                Text(k, color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.width(76.dp))
                Text(v, color = Body, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun Example(pattern: String, explain: String) {
    Column(Modifier.fillMaxWidth().background(FeltDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp)).padding(12.dp)) {
        Text(pattern, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(explain, color = Dim, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TileRow(kinds: List<Int>, width: Dp = 32.dp) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        kinds.forEach { TileView(it, width) }
    }
}

@Composable
private fun Labeled(label: String, suit: Suit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Dim, fontSize = 13.sp)
        TileRow((1..9).map { s(suit, it) }, 30.dp)
    }
}

@Composable
private fun DragonPair(d: Dragon, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TileView(Kinds.dragon(d), 36.dp)
        Text(caption, color = Dim, fontSize = 12.sp)
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Ink else Color.White,
        fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        modifier = Modifier
            .background(if (selected) Gold else FeltDark, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
