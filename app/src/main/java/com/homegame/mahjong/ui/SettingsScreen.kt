package com.homegame.mahjong.ui

import android.content.pm.ApplicationInfo
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homegame.mahjong.TableViewModel
import com.homegame.mahjong.update.UpdateState
import com.homegame.mahjong.update.UpdateViewModel

@Composable
fun SettingsScreen(vm: TableViewModel, updates: UpdateViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val hasMusic = remember { vm.music.hasAnyTracks }
    val context = LocalContext.current
    val debuggable = remember { context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    Column(Modifier.fillMaxSize().felt().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", color = Ivory, fontSize = 26.sp, fontFamily = Display, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Pill("Done", onClick = onClose)
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ShareSection()
            Section("Music volume") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = vm.musicVolume,
                        onValueChange = vm::chooseMusicVolume,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Faint),
                    )
                    Text(
                        if (vm.musicVolume == 0f) "Off" else "${(vm.musicVolume * 100).toInt()}%",
                        color = Ivory, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(48.dp),
                    )
                }
                if (!hasMusic) Text("No music added yet.", color = Soft, fontSize = 13.sp)
            }
            Section("Drawing tiles") {
                Segmented(listOf("Automatic", "I'll tap the wall"), if (vm.manualDraw) 1 else 0) { vm.chooseManualDraw(it == 1) }
                Text(
                    if (vm.manualDraw) "On your turn, tap the wall to pick up your tile." else "Your tile arrives on its own at the start of your turn.",
                    color = Soft, fontSize = 13.sp,
                )
            }
            Section("Animated effects") {
                Segmented(listOf("On", "Off"), if (vm.motionOn) 0 else 1) { vm.chooseMotion(it == 0) }
                Text(
                    if (vm.motionOn) "Tables and tile sets that move will animate." else "Everything holds still. Saves a little battery.",
                    color = Soft, fontSize = 13.sp,
                )
            }
            Section("Bot speed") {
                Segmented(com.homegame.mahjong.BotSpeed.entries.map { it.label }, com.homegame.mahjong.BotSpeed.entries.indexOf(vm.speed)) {
                    vm.speed = com.homegame.mahjong.BotSpeed.entries[it]
                }
            }
            Section("Updates") {
                Text("Version ${updates.versionName}", color = Ivory, fontWeight = FontWeight.Bold)
                if (updates.canUpdate) {
                    val s = updates.state
                    BigButton(
                        if (s is UpdateState.Checking) "Checking…" else "Check for updates",
                        Modifier.fillMaxWidth(),
                        enabled = s !is UpdateState.Checking && s !is UpdateState.Downloading,
                    ) { updates.check(manual = true) }
                    when (s) {
                        is UpdateState.UpToDate -> Text("You have the latest version.", color = Soft, fontSize = 13.sp)
                        is UpdateState.Available -> Text("Version ${s.release.name} is available.", color = Gold, fontSize = 13.sp)
                        else -> Text("Mahjong checks for a new version each time it opens.", color = Soft, fontSize = 13.sp)
                    }
                } else {
                    Text("Test build. Updates only apply to builds downloaded from the release page.", color = Soft, fontSize = 13.sp)
                }
            }
            if (debuggable) Section("Testing") {
                Text("${vm.wallet.owned.size} tile sets · ${vm.wallet.ownedUi.size} table styles · ${vm.wallet.coins} coins", color = Soft, fontSize = 13.sp)
                BigButton("Unlock everything", Modifier.fillMaxWidth(), primary = true) { vm.wallet.unlockEverything() }
                BigButton("Reset shop", Modifier.fillMaxWidth()) { vm.wallet.resetShop() }
                BigButton("Open all circuits", Modifier.fillMaxWidth()) { vm.unlockAllCircuits() }
                BigButton("Reset circuits", Modifier.fillMaxWidth()) { vm.resetCareer() }
                Text("Only shows in test builds.", color = Soft, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Faint, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(title)
        content()
    }
}
