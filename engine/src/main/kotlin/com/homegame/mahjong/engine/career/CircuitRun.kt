package com.homegame.mahjong.engine.career

import kotlin.math.exp
import kotlin.random.Random

/** The player's id in standings and seat lists. */
const val YOU = "you"

/** Hands in one session. */
const val HANDS_PER_SESSION = 4

/** League rounds before the final. */
const val LEAGUE_ROUNDS = 3

/** Players who make the final from the league. You have to be one of them. */
const val CUT = 4

/** League points for 1st..4th in a session. Tied players all get the better place. */
val PLACEMENT_POINTS = listOf(4, 2, 1, 0)

enum class RunStatus { LEAGUE, FINAL, WON, MISSED_CUT, LOST_FINAL }

data class Standing(val id: String, val points: Int, val score: Int)

/** How one session went: everyone at your table, best first, with what they scored. */
data class SessionResult(
    val round: Int,
    val final: Boolean,
    /** Ids at your table in finishing order, with session score and 0-based place (ties share). */
    val rows: List<Row>,
) {
    data class Row(val id: String, val score: Int, val place: Int)
    val yourPlace get() = rows.first { it.id == YOU }.place
}

/**
 * One attempt at a circuit. A league of [LEAGUE_ROUNDS] four-hand sessions decides who makes
 * the final; you need to finish in the top [CUT]. The final is you, the boss and the two best
 * other rivals. Finish first in the final (ties count) to win the circuit.
 *
 * Schedule: round 1 you meet the first three regulars, round 2 the other three, round 3 the boss
 * and the two regulars leading the standings. The four rivals not at your table play their own
 * session off-screen, simulated from their levels.
 */
class CircuitRun(
    val circuitIndex: Int,
    val seed: Long,
    /** 0-based league round. [LEAGUE_ROUNDS] once the league is done. */
    var round: Int = 0,
    var status: RunStatus = RunStatus.LEAGUE,
    val points: MutableMap<String, Int> = mutableMapOf(),
    val scores: MutableMap<String, Int> = mutableMapOf(),
    /** Hands finished in the current session. */
    var hand: Int = 0,
    /** Running scores for the current session, by seat (0 = you). */
    val sessionScores: IntArray = IntArray(4),
    var finalAttempts: Int = 0,
    var last: SessionResult? = null,
) {
    val circuit get() = Circuits.all[circuitIndex]
    val everyone get() = listOf(YOU) + circuit.rivals
    val over get() = status == RunStatus.WON || status == RunStatus.MISSED_CUT || status == RunStatus.LOST_FINAL
    val inSession get() = hand > 0

    init {
        for (id in everyone) { points.putIfAbsent(id, 0); scores.putIfAbsent(id, 0) }
    }

    fun standings(): List<Standing> = everyone
        .map { Standing(it, points.getValue(it), scores.getValue(it)) }
        .sortedWith(compareByDescending<Standing> { it.points }.thenByDescending { it.score }.thenBy { if (it.id == YOU) 0 else 1 })

    /** The three rivals at your table this session, in seat order (seats 1, 2, 3). */
    fun table(): List<String> {
        val c = circuit
        return when {
            status == RunStatus.FINAL -> finalists()
            round == 0 -> c.regulars.take(3)
            round == 1 -> c.regulars.drop(3)
            else -> listOf(c.bossId) + leaders(c.regulars, 2)
        }
    }

    /** Seat order for the current session: you, then [table]. */
    fun seats(): List<String> = listOf(YOU) + table()

    private fun leaders(pool: List<String>, n: Int): List<String> {
        val order = standings().map { it.id }
        return pool.sortedBy { order.indexOf(it) }.take(n)
    }

    private fun finalists(): List<String> =
        listOf(circuit.bossId) + leaders(circuit.rivals.filter { it != circuit.bossId }, 2)

    /** Records one finished hand. [payments] is by seat, as the game reports it. */
    fun recordHand(payments: IntArray) {
        check(!over && hand < HANDS_PER_SESSION) { "No session in progress" }
        for (s in 0..3) sessionScores[s] += payments[s]
        hand++
    }

    val sessionDone get() = hand >= HANDS_PER_SESSION

    /** Closes the session: places everyone, simulates the other table, and moves the run on. */
    fun finishSession(): SessionResult {
        check(sessionDone) { "Session isn't over" }
        val seats = seats()
        val rows = place(seats.indices.associate { seats[it] to sessionScores[it] })
        val final = status == RunStatus.FINAL
        if (final) {
            finalAttempts++
            status = if (rows.first { it.id == YOU }.place == 0) RunStatus.WON else RunStatus.LOST_FINAL
        } else {
            award(rows)
            val others = circuit.rivals.filter { it !in seats }
            award(place(simulate(others, Random(seed * 31 + round))))
            round++
            if (round >= LEAGUE_ROUNDS) {
                val made = standings().take(CUT).any { it.id == YOU }
                status = if (made) RunStatus.FINAL else RunStatus.MISSED_CUT
            }
        }
        val result = SessionResult(round = if (final) LEAGUE_ROUNDS else round - 1, final = final, rows = rows)
        last = result
        hand = 0
        sessionScores.fill(0)
        return result
    }

    /** After losing the final: play it again with the same finalists. */
    fun retryFinal() {
        check(status == RunStatus.LOST_FINAL)
        status = RunStatus.FINAL
        hand = 0
        sessionScores.fill(0)
    }

    private fun award(rows: List<SessionResult.Row>) {
        for (r in rows) {
            points.merge(r.id, PLACEMENT_POINTS[r.place], Int::plus)
            scores.merge(r.id, r.score, Int::plus)
        }
    }

    /**
     * A believable four-hand session for rivals you aren't sitting with. Stronger rivals win more
     * often; hand values, self-draws and wall games follow roughly what real games produce.
     */
    private fun simulate(ids: List<String>, rng: Random): Map<String, Int> {
        val total = IntArray(ids.size)
        val weight = ids.map { exp(0.3 * circuit.level(it)) }
        repeat(HANDS_PER_SESSION) {
            if (rng.nextDouble() < 0.18) return@repeat
            var r = rng.nextDouble() * weight.sum()
            var w = 0
            while (r > weight[w]) { r -= weight[w]; w++ }
            var value = HAND_VALUES.random(rng)
            if (rng.nextDouble() < 0.1) value *= 2
            val selfDraw = rng.nextDouble() < 0.3
            val discarder = if (selfDraw) -1 else ids.indices.filter { it != w }.random(rng)
            for (s in ids.indices) if (s != w) {
                val owe = if (selfDraw || s == discarder) 2 * value else value
                total[s] -= owe
                total[w] += owe
            }
        }
        return ids.indices.associate { ids[it] to total[it] }
    }

    companion object {
        private val HAND_VALUES = listOf(25, 25, 25, 25, 30, 30, 30, 35, 40, 45, 50)

        /** Orders a table by score. Tied scores share the better place. */
        fun place(scores: Map<String, Int>): List<SessionResult.Row> {
            val sorted = scores.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { if (it.key == YOU) 0 else 1 })
            return sorted.map { e -> SessionResult.Row(e.key, e.value, sorted.indexOfFirst { it.value == e.value }) }
        }
    }
}

/**
 * Everything the circuits remember between launches: how far up the ladder you are, trophies,
 * the run in progress, and your record against each rival.
 */
class Career(
    /** How many circuits are open. Starts at 1. */
    var unlocked: Int = 1,
    val trophies: MutableSet<Int> = mutableSetOf(),
    var run: CircuitRun? = null,
    /** Rival id to [times you finished ahead, times they finished ahead]. */
    val headToHead: MutableMap<String, IntArray> = mutableMapOf(),
) {
    val champion get() = Circuits.all.size - 1 in trophies

    fun isOpen(index: Int) = index < unlocked

    /** Starts (or restarts) a circuit. Throws away any run in progress. */
    fun start(index: Int, seed: Long = Random.nextLong()): CircuitRun {
        require(isOpen(index)) { "Circuit ${index + 1} is locked" }
        return CircuitRun(index, seed).also { run = it }
    }

    fun met(id: String) = id in headToHead

    /**
     * Closes the current session and books the results. Returns the result and, if this session
     * won the circuit for the first time, the circuit's index.
     */
    fun finishSession(): Pair<SessionResult, Int?> {
        val r = run ?: error("No run")
        val result = r.finishSession()
        val mine = result.yourPlace
        for (row in result.rows) if (row.id != YOU) {
            val rec = headToHead.getOrPut(row.id) { IntArray(2) }
            if (mine < row.place) rec[0]++ else if (row.place < mine) rec[1]++
        }
        var firstWin: Int? = null
        if (r.status == RunStatus.WON) {
            if (trophies.add(r.circuitIndex)) firstWin = r.circuitIndex
            unlocked = maxOf(unlocked, minOf(Circuits.all.size, r.circuitIndex + 2))
        }
        return result to firstWin
    }
}
