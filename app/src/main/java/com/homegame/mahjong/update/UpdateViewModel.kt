package com.homegame.mahjong.update

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Updater.Release) : UpdateState
    data class NeedsPermission(val release: Updater.Release) : UpdateState
    data class Downloading(val release: Updater.Release, val progress: Float) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** Checks GitHub for a newer build once per launch, and drives the download and install when the player agrees. */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val context: Context get() = getApplication()

    var state: UpdateState by mutableStateOf(UpdateState.Idle)
        private set

    /** The player tapped "Later": no popup again this launch. Settings can still check by hand. */
    var dismissed by mutableStateOf(false)
        private set

    val versionName: String = Updater.installedVersionName(app)
    val canUpdate: Boolean = Updater.isReleaseBuild(app)

    private var downloaded: Pair<Updater.Release, File>? = null

    init {
        if (canUpdate) check(manual = false)
    }

    val showPopup: Boolean
        get() = !dismissed && when (state) {
            is UpdateState.Available, is UpdateState.NeedsPermission, is UpdateState.Downloading, is UpdateState.Failed -> true
            else -> false
        }

    fun check(manual: Boolean) {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        if (manual) { dismissed = false; state = UpdateState.Checking }
        viewModelScope.launch {
            val latest = runCatching { withContext(Dispatchers.IO) { Updater.fetchLatest() } }
            val release = latest.getOrNull()
            state = when {
                latest.isFailure -> if (manual) UpdateState.Failed("Couldn't reach the update server. Check your connection.") else UpdateState.Idle
                release != null && release.versionCode > Updater.installedVersionCode(context) -> UpdateState.Available(release)
                manual -> UpdateState.UpToDate
                else -> UpdateState.Idle
            }
        }
    }

    fun later() { dismissed = true }

    fun update() {
        val release = when (val s = state) {
            is UpdateState.Available -> s.release
            is UpdateState.NeedsPermission -> s.release
            else -> return
        }
        if (!Updater.canInstall(context)) { state = UpdateState.NeedsPermission(release); return }
        viewModelScope.launch {
            try {
                val apk = downloaded?.takeIf { it.first == release && it.second.exists() }?.second
                    ?: withContext(Dispatchers.IO) {
                        Updater.download(context, release) { p -> state = UpdateState.Downloading(release, p) }
                    }.also { downloaded = release to it }
                state = UpdateState.Downloading(release, 1f)
                withContext(Dispatchers.IO) { Updater.install(context, apk) }
                // Android's own confirm screen takes over from here. If the player backs out of it,
                // they land back on this popup and can tap Update again without re-downloading.
                state = UpdateState.Available(release)
            } catch (e: Exception) {
                state = UpdateState.Failed("Update didn't finish: ${e.message ?: "unknown error"}")
            }
        }
    }

    /** Opens Android's "Install unknown apps" switch for Mahjong. */
    fun openInstallPermission(from: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        from.startActivity(intent)
    }

    /** Back from Settings: carry on if the switch is now on. */
    fun onResume() {
        if (state is UpdateState.NeedsPermission && Updater.canInstall(context)) update()
    }

    fun clearMessage() {
        if (state is UpdateState.UpToDate || state is UpdateState.Failed) state = UpdateState.Idle
    }
}
