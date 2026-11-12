package com.homegame.mahjong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homegame.mahjong.update.UpdateState
import com.homegame.mahjong.update.UpdateViewModel

/** Popup offering a newer build. Shown over whatever screen is open. */
@Composable
fun UpdateDialog(updates: UpdateViewModel) {
    val state = updates.state
    val busy = state is UpdateState.Downloading
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { if (!busy) updates.later() },
        properties = DialogProperties(dismissOnClickOutside = !busy, dismissOnBackPress = !busy),
    ) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier.fillMaxWidth().background(Color(0xFF0F2A1E), shape).border(1.dp, Gold.copy(alpha = 0.6f), shape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (state) {
                is UpdateState.Available -> {
                    Title("Update available")
                    Body("Version ${state.release.name} is ready. You're on ${updates.versionName}. Your progress and coins carry over.")
                    Buttons(
                        primary = "Update" to updates::update,
                        secondary = "Later" to updates::later,
                    )
                }
                is UpdateState.NeedsPermission -> {
                    Title("One-time step")
                    Body("Android needs your OK before Mahjong can install its own updates. On the next screen, turn on \"Allow from this source\", then come back.")
                    Buttons(
                        primary = "Allow" to { updates.openInstallPermission(context) },
                        secondary = "Later" to updates::later,
                    )
                }
                is UpdateState.Downloading -> {
                    Title("Downloading ${state.release.name}")
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Gold,
                        trackColor = Faint,
                    )
                    Body(if (state.progress >= 1f) "Opening the installer…" else "${(state.progress * 100).toInt()}%")
                }
                is UpdateState.Failed -> {
                    Title("Update problem")
                    Body(state.message)
                    Buttons(primary = "OK" to updates::later, secondary = null)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun Title(text: String) =
    Text(text, color = Ivory, fontSize = 22.sp, fontFamily = Display, fontWeight = FontWeight.Bold)

@Composable
private fun Body(text: String) = Text(text, color = Soft, fontSize = 15.sp)

@Composable
private fun Buttons(primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (secondary != null) BigButton(secondary.first, Modifier.weight(1f), onClick = secondary.second)
        BigButton(primary.first, Modifier.weight(1f), primary = true, onClick = primary.second)
    }
}
