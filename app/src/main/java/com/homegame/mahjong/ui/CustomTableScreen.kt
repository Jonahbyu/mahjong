package com.homegame.mahjong.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.BotSpeed
import com.homegame.mahjong.TableViewModel
import com.homegame.mahjong.engine.Difficulty
import com.homegame.mahjong.engine.SEAT_NAMES

/** Set up the Custom table: each bot's level, house rules, and how play feels. */
@Composable
fun CustomTableScreen(vm: TableViewModel, onClose: () -> Unit, onDeal: () -> Unit) {
    val rules = vm.customRules
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("‹ Menu", onClick = onClose)
        }
        Text(
            "Custom table", color = Ivory, fontSize = 28.sp, fontFamily = Display, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("Opponents") {
                val levels = vm.availableLevels
                // Four full names don't fit across a phone, so shorten them once Expert is in the list.
                val short = mapOf(Difficulty.BEGINNER to "Easy", Difficulty.INTERMEDIATE to "Medium", Difficulty.ADVANCED to "Hard")
                val labels = levels.map { if (levels.size > 3) short[it] ?: it.label else it.label }
                vm.customLevels.forEachIndexed { i, level ->
                    Text(SEAT_NAMES[i + 1], color = Ivory, fontWeight = FontWeight.SemiBold)
                    Segmented(labels, levels.indexOf(level)) { vm.chooseCustomLevel(i, levels[it]) }
                }
            }
            Section("Charleston") {
                Text("Second Charleston", color = Ivory, fontWeight = FontWeight.SemiBold)
                Segmented(listOf("Vote on it", "Skip it"), if (rules.secondCharleston) 0 else 1) {
                    vm.chooseCustomRules(rules.copy(secondCharleston = it == 0))
                }
                Text("Courtesy pass", color = Ivory, fontWeight = FontWeight.SemiBold)
                Segmented(listOf("On", "Off"), if (rules.courtesyPass) 0 else 1) {
                    vm.chooseCustomRules(rules.copy(courtesyPass = it == 0))
                }
            }
            Section("Scoring") {
                Text("Jokerless hands pay double", color = Ivory, fontWeight = FontWeight.SemiBold)
                Segmented(listOf("On", "Off"), if (rules.jokerlessBonus) 0 else 1) {
                    vm.chooseCustomRules(rules.copy(jokerlessBonus = it == 0))
                }
            }
            Section("Play") {
                Text("Bot speed", color = Ivory, fontWeight = FontWeight.SemiBold)
                Segmented(BotSpeed.entries.map { it.label }, BotSpeed.entries.indexOf(vm.speed)) { vm.speed = BotSpeed.entries[it] }
                Text("Drawing tiles", color = Ivory, fontWeight = FontWeight.SemiBold)
                Segmented(listOf("Automatic", "Tap the wall"), if (vm.manualDraw) 1 else 0) { vm.chooseManualDraw(it == 1) }
                Text("Hand hints", color = Ivory, fontWeight = FontWeight.SemiBold)
                Segmented(listOf("Show", "Hide"), if (vm.showHints) 0 else 1) { vm.showHints = it == 0 }
                Text("Speed, drawing and hints apply to every table.", color = Soft, fontSize = 13.sp)
            }
        }
        BigButton(
            "Deal me in", Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), primary = true, onClick = onDeal,
        )
    }
}
