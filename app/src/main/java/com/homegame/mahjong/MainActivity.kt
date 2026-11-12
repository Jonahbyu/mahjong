package com.homegame.mahjong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.homegame.mahjong.ui.HomeScreen
import com.homegame.mahjong.ui.HowToPlayScreen
import com.homegame.mahjong.ui.LocalTileSkin
import com.homegame.mahjong.ui.MotionClock
import com.homegame.mahjong.ui.SettingsScreen
import com.homegame.mahjong.ui.TableScreen

class MainActivity : ComponentActivity() {
    private val vm: TableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalTileSkin provides vm.wallet.equipped.skin) {
                    var guide by rememberSaveable { mutableStateOf(false) }
                    var settings by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) { vm.music.start() }
                    MotionClock()
                    when {
                        settings -> SettingsScreen(vm, onClose = { settings = false })
                        guide -> {
                            BackHandler { guide = false }
                            HowToPlayScreen(onClose = { guide = false })
                        }
                        vm.ui == null -> HomeScreen(vm, onHowToPlay = { guide = true }, onSettings = { settings = true })
                        else -> {
                            BackHandler { vm.requestLeave() }
                            TableScreen(vm, onHelp = { guide = true }, onSettings = { settings = true })
                        }
                    }
                }
            }
        }
    }

    // Music stops when the app goes to the background and picks up again on return.
    override fun onStart() { super.onStart(); vm.music.resume() }
    override fun onStop() { vm.music.pause(); super.onStop() }
}
