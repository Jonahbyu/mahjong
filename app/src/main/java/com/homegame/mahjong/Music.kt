package com.homegame.mahjong

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * Background music. Every audio file under app/src/main/assets/music/ (including its
 * menu/ and game/ subfolders) goes into one shuffled playlist that plays straight through,
 * menu and table alike, then reshuffles. Any file names work. No files means silence.
 */
class Music(private val context: Context) {
    private var player: MediaPlayer? = null
    private var started = false
    private var queue = ArrayDeque<String>()
    private var lastPlayed: String? = null
    private var paused = false

    /** 0..1 as shown on the slider. */
    var volume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            applyVolume()
        }

    fun tracks(): List<String> = listOf("music", "music/menu", "music/game").flatMap { folder ->
        (context.assets.list(folder) ?: emptyArray())
            .filter { it.substringAfterLast('.').lowercase() in AUDIO }
            .map { "$folder/$it" }
    }

    val hasAnyTracks get() = tracks().isNotEmpty()

    /** Starts the playlist; does nothing if it's already going. */
    fun start() {
        if (started) return
        started = true
        startNext()
    }

    fun pause() {
        paused = true
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        paused = false
        val p = player
        if (p != null) { if (volume > 0f) p.start() } else if (started) startNext()
    }

    fun release() {
        player?.release()
        player = null
    }

    private fun startNext() {
        release()
        if (queue.isEmpty()) {
            val all = tracks()
            if (all.isEmpty()) return
            // Reshuffle, but don't play the same song twice in a row across the seam.
            var next = all.shuffled()
            if (all.size > 1 && next.first() == lastPlayed) next = next.drop(1) + next.first()
            queue.addAll(next)
        }
        val path = queue.removeFirst()
        lastPlayed = path
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            context.assets.openFd(path).use { fd -> p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            p.isLooping = tracks().size == 1
            p.setOnCompletionListener { startNext() }
            p.prepare()
            player = p
            applyVolume()
            if (!paused && volume > 0f) p.start()
        } catch (e: Exception) {
            // A bad or compressed asset shouldn't take the game down; skip it.
            Log.w("Music", "Can't play $path", e)
            player = null
        }
    }

    private fun applyVolume() {
        val p = player ?: return
        // Squared so the slider feels even to the ear.
        val v = volume * volume
        p.setVolume(v, v)
        if (volume == 0f) { if (p.isPlaying) p.pause() } else if (!paused && !p.isPlaying) p.start()
    }

    companion object {
        private val AUDIO = setOf("mp3", "m4a", "aac", "ogg", "wav", "flac")
    }
}
