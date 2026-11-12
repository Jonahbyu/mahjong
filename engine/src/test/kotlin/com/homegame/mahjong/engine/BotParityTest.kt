package com.homegame.mahjong.engine

import com.homegame.mahjong.engine.sim.playOut
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the four difficulty presets to the exact play they had before bots took a [BotProfile].
 * Same seeds must give the same games, move for move. If a change to the bot is meant to alter
 * how a difficulty plays, update the fingerprints and say why in the commit.
 */
class BotParityTest {
    private val card = Card.practice()

    private fun fingerprint(table: List<Difficulty>, games: Int): Int {
        var h = 17
        repeat(games) { i ->
            val bots = table.mapIndexed { s, d -> HeuristicBot(d, seed = i * 13L + s) }
            val g = playOut(card, bots, seed = 500L + i)
            h = 31 * h + g.events.joinToString().hashCode()
        }
        return h
    }

    @Test
    fun mixedTableUnchanged() {
        assertEquals(EXPECTED_MIXED, fingerprint(Difficulty.entries.toList(), 12))
    }

    @Test
    fun eachLevelUnchanged() {
        val got = Difficulty.entries.map { d -> fingerprint(List(4) { d }, 4) }
        assertEquals(EXPECTED_EACH, got)
    }

    private companion object {
        const val EXPECTED_MIXED = -1776107394
        val EXPECTED_EACH = listOf(1332075857, 1281793708, -411179924, -1876904954)
    }
}
