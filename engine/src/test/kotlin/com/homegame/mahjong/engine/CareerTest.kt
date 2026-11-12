package com.homegame.mahjong.engine

import com.homegame.mahjong.engine.career.CUT
import com.homegame.mahjong.engine.career.Career
import com.homegame.mahjong.engine.career.CircuitRun
import com.homegame.mahjong.engine.career.Circuits
import com.homegame.mahjong.engine.career.HANDS_PER_SESSION
import com.homegame.mahjong.engine.career.LEAGUE_ROUNDS
import com.homegame.mahjong.engine.career.MAX_LEVEL
import com.homegame.mahjong.engine.career.RunStatus
import com.homegame.mahjong.engine.career.Rivals
import com.homegame.mahjong.engine.career.YOU
import com.homegame.mahjong.engine.sim.playOut
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CareerTest {
    private val card = Card.practice()

    /** Plays a whole session where seat [winner] wins every hand off seat [loser]'s discard. */
    private fun playSession(run: CircuitRun, winner: Int, loser: Int = (winner + 1) % 4) {
        repeat(HANDS_PER_SESSION) {
            val p = IntArray(4) { -25 }
            p[loser] = -50
            p[winner] = 100
            run.recordHand(p)
        }
    }

    @Test
    fun ladderHasSixteenCircuitsGettingHarder() {
        assertEquals(16, Circuits.all.size)
        for (c in Circuits.all) {
            val levels = c.rivals.map { c.level(it) }
            assertTrue(levels.all { it in 1..MAX_LEVEL }, c.name)
            assertEquals(levels.max(), c.level(c.bossId), "${c.name} boss is the strongest")
        }
        val avg = Circuits.all.map { c -> c.rivals.sumOf { c.level(it) } }
        assertEquals(avg.sorted(), avg, "each circuit is at least as hard as the last")
    }

    @Test
    fun youMeetEveryRivalInTheLeague() {
        val run = CircuitRun(0, seed = 1)
        val met = mutableSetOf<String>()
        repeat(LEAGUE_ROUNDS) {
            met += run.table()
            assertEquals(3, run.table().toSet().size)
            playSession(run, winner = 0)
            run.finishSession()
        }
        assertEquals(run.circuit.rivals.toSet(), met)
    }

    @Test
    fun tiesShareThePlace() {
        val rows = CircuitRun.place(mapOf(YOU to 0, "a" to 50, "b" to 0, "c" to -50))
        assertEquals(listOf("a", YOU, "b", "c"), rows.map { it.id })
        assertEquals(listOf(0, 1, 1, 3), rows.map { it.place })
    }

    @Test
    fun winningEverythingWinsTheCircuitAndOpensTheNext() {
        val career = Career()
        assertFalse(career.isOpen(1))
        val run = career.start(0, seed = 7)
        repeat(LEAGUE_ROUNDS) {
            playSession(run, winner = 0)
            career.finishSession()
        }
        assertEquals(RunStatus.FINAL, run.status)
        assertEquals(12, run.points[YOU])
        assertTrue(run.circuit.bossId in run.table(), "boss is at the final")
        playSession(run, winner = 0)
        val (result, firstWin) = career.finishSession()
        assertTrue(result.final)
        assertEquals(RunStatus.WON, run.status)
        assertEquals(0, firstWin)
        assertTrue(career.isOpen(1))
        // You meet the boss in round 3 and again in the final, and beat them both times.
        assertEquals(listOf(2, 0), career.headToHead.getValue(run.circuit.bossId).toList())
    }

    @Test
    fun losingEverythingMissesTheCut() {
        val career = Career()
        val run = career.start(0, seed = 3)
        repeat(LEAGUE_ROUNDS) {
            playSession(run, winner = 1, loser = 0)
            career.finishSession()
        }
        assertEquals(RunStatus.MISSED_CUT, run.status)
        assertFalse(run.standings().take(CUT).any { it.id == YOU })
        assertFalse(career.isOpen(1))
        assertFailsWith<IllegalStateException> { run.recordHand(IntArray(4)) }
    }

    @Test
    fun lostFinalCanBeReplayed() {
        val career = Career()
        val run = career.start(0, seed = 5)
        repeat(LEAGUE_ROUNDS) { playSession(run, winner = 0); career.finishSession() }
        val finalists = run.table()
        playSession(run, winner = 1, loser = 0)
        val (_, firstWin) = career.finishSession()
        assertNull(firstWin)
        assertEquals(RunStatus.LOST_FINAL, run.status)
        run.retryFinal()
        assertEquals(finalists, run.table())
        playSession(run, winner = 0)
        career.finishSession()
        assertEquals(RunStatus.WON, run.status)
        assertEquals(2, run.finalAttempts)
    }

    @Test
    fun offTableResultsAreRepeatable() {
        fun standingsAfterRound(seed: Long): Any {
            val run = CircuitRun(4, seed)
            playSession(run, winner = 2)
            run.finishSession()
            return run.standings()
        }
        assertEquals(standingsAfterRound(42), standingsAfterRound(42))
    }

    @Test
    fun everyCircuitsFinalTablePlaysLegalGames() {
        for (c in Circuits.all) {
            val rng = Random(c.index)
            val ids = listOf(c.bossId) + c.regulars.takeLast(3)
            val bots = ids.mapIndexed { s, id -> HeuristicBot(Rivals.byId(id).profile(c.level(id), rng), seed = c.index * 10L + s) }
            val g = playOut(card, bots, seed = c.index.toLong(), rules = c.rules)
            assertEquals(152, g.tileTotal(), c.name)
            assertEquals(0, (g.phase as Phase.Over).result.payments.sum())
        }
    }

    @Test
    fun everyRivalPlaysLegally() {
        for ((i, r) in Rivals.all.withIndex()) {
            val bots = listOf(HeuristicBot(r.profile(10, Random(i)), seed = i.toLong())) +
                List(3) { HeuristicBot(Difficulty.INTERMEDIATE, seed = i * 5L + it) }
            val g = playOut(card, bots, seed = 900L + i)
            assertEquals(152, g.tileTotal(), r.name)
        }
    }
}
