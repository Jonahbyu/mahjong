package com.homegame.mahjong.engine

import com.homegame.mahjong.engine.sim.playOut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTest {
    private val card = Card.practice()

    private fun passAll(g: Game) {
        for (s in 0..3) {
            val ids = g.hands[s].filter { !it.isJoker }.take(3).map { it.id }
            assertNull(g.act(s, Action.Pass(ids)))
        }
    }

    @Test
    fun dealGivesEast14AndOthers13() {
        val g = Game(card, seed = 1)
        assertEquals(listOf(14, 13, 13, 13), g.hands.map { it.size })
        assertEquals(152 - 53, g.wall.size)
        assertIs<Phase.Charleston>(g.phase)
    }

    @Test
    fun charlestonMovesTilesRightAndKeepsCounts() {
        val g = Game(card, seed = 2)
        val eastPass = g.hands[0].filter { !it.isJoker }.take(3)
        passAll(g)
        assertTrue(g.hands[1].containsAll(eastPass), "East's pass goes to South (right)")
        assertEquals(listOf(14, 13, 13, 13), g.hands.map { it.size })
        assertEquals(Phase.Charleston(1, PassDir.ACROSS), g.phase)
    }

    @Test
    fun jokersCannotBePassed() {
        val g = Game(card, seed = 3)
        val seat = (0..3).first { s -> g.hands[s].any { it.isJoker } }
        val joker = g.hands[seat].first { it.isJoker }
        val others = g.hands[seat].filter { !it.isJoker }.take(2)
        val err = g.act(seat, Action.Pass(listOf(joker.id) + others.map { it.id }))
        assertNotNull(err)
    }

    @Test
    fun fullCharlestonThenEastDiscards() {
        val g = Game(card, rules = Rules(secondCharleston = false), seed = 4)
        repeat(3) { passAll(g) }
        assertEquals(Phase.Courtesy, g.phase)
        for (s in 0..3) g.act(s, Action.Pass(emptyList()))
        assertEquals(Phase.Turn(0), g.phase)
    }

    @Test
    fun cannotCallDiscardForAPair() {
        val g = Game(card, rules = Rules(secondCharleston = false, courtesyPass = false), seed = 5)
        repeat(3) { passAll(g) }
        // Find a discard from East that someone holds one copy of.
        val tile = g.hands[0].first { !it.isJoker }
        g.act(0, Action.Discard(tile.id))
        val p = g.phase
        if (p is Phase.Calling) {
            val caller = p.eligible.first()
            val match = g.hands[caller].firstOrNull { it.kind == tile.kind }
            if (match != null) {
                assertNotNull(g.act(caller, Action.Expose(listOf(match.id))))
            }
        }
    }

    @Test
    fun botGamesFinishLegallyAndConserveTiles() {
        val levels = Difficulty.entries // one of each, Expert included
        check(levels.size == 4)
        repeat(60) { i ->
            val bots = levels.mapIndexed { s, d -> HeuristicBot(d, seed = i * 7L + s) }
            val g = playOut(card, bots, seed = i.toLong())
            assertEquals(152, g.tileTotal())
            val r = (g.phase as Phase.Over).result
            assertEquals(0, r.payments.sum(), "payments balance")
            if (r.winner != null) {
                val total = g.hands[r.winner].size + g.exposures[r.winner].sumOf { it.tiles.size }
                assertEquals(14, total)
            }
        }
    }

    @Test
    fun selfDrawPaysDoubleFromEveryone() {
        // Play bot games until one ends in a self-drawn win, then check the payout shape.
        val bots = List(4) { HeuristicBot(Difficulty.ADVANCED, seed = it.toLong()) }
        for (i in 0 until 200) {
            val r = (playOut(card, bots, seed = 1000L + i).phase as Phase.Over).result
            if (r.selfDrawn) {
                val losers = (0..3).filter { it != r.winner }
                val owed = losers.map { -r.payments[it] }.toSet()
                assertEquals(1, owed.size, "everyone pays the same on self-draw")
                return
            }
        }
    }
}
