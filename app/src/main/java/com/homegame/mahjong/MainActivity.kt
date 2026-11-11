package com.homegame.mahjong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.engine.Card
import com.homegame.mahjong.engine.Game
import com.homegame.mahjong.engine.Kinds
import com.homegame.mahjong.engine.Suit
import com.homegame.mahjong.engine.Tile
import com.homegame.mahjong.engine.countsOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val card = Card.practice()
        setContent { MaterialTheme { DealPreview(card) } }
    }
}

private val Felt = Color(0xFF1F5C46)

/** Phase-1 smoke screen: deals a hand from the real engine and shows the closest card hands. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DealPreview(card: Card) {
    var game by remember { mutableStateOf(Game(card)) }
    val hand = game.hands[0].sortedBy { it.kind }
    val closest = remember(game) { card.ranked(countsOf(game.hands[0]), emptyList(), limit = 3) }

    Column(
        Modifier.fillMaxSize().background(Felt).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Mahjong", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Your opening hand (East, 14 tiles)", color = Color.White.copy(alpha = 0.8f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            hand.forEach { TileFace(it) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Closest hands on the practice card", color = Color.White, fontWeight = FontWeight.SemiBold)
        closest.forEach { e ->
            Text(
                "${e.instance.hand.family}: ${e.instance.describe()}  ·  ${e.missing} away  ·  ${e.instance.hand.points} pts",
                color = Color.White, fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { game = Game(card) }) { Text("Deal again") }
    }
}

@Composable
fun TileFace(tile: Tile) {
    val k = tile.kind
    val ink = when {
        Kinds.isSuited(k) -> when (Kinds.suitOf(k)) {
            Suit.BAM -> Color(0xFF1B7A3A)
            Suit.CRAK -> Color(0xFFB3261E)
            Suit.DOT -> Color(0xFF1E4FB3)
        }
        k == Kinds.JOKER -> Color(0xFF7B1FA2)
        k == Kinds.FLOWER -> Color(0xFFD81B60)
        else -> Color(0xFF222222)
    }
    Box(
        Modifier.size(width = 44.dp, height = 60.dp)
            .background(Color(0xFFFFFBF0), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFBFB7A0), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(Kinds.short(k), color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
