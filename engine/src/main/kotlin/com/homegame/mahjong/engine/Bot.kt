package com.homegame.mahjong.engine

import kotlin.random.Random

enum class Difficulty(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced")
}

interface Bot {
    /** Called when [seat] is in [Game.awaiting]. Must return a legal action. */
    fun decide(game: Game, seat: Int): Action
}

/**
 * One bot, tuned per difficulty:
 * - Beginner: chases its single closest hand, sloppy discards, only calls for Mahjong.
 * - Intermediate: keeps several hands in view, calls exposures that clearly help, swaps jokers.
 * - Advanced: also tracks dead tiles and avoids feeding opponents who look close.
 */
class HeuristicBot(val difficulty: Difficulty, seed: Long = Random.nextLong()) : Bot {
    private val rng = Random(seed)

    private val topK = when (difficulty) {
        Difficulty.BEGINNER -> 1
        Difficulty.INTERMEDIATE -> 4
        Difficulty.ADVANCED -> 8
    }
    private val noise = when (difficulty) {
        Difficulty.BEGINNER -> 1.2
        Difficulty.INTERMEDIATE -> 0.15
        Difficulty.ADVANCED -> 0.02
    }

    override fun decide(game: Game, seat: Int): Action = when (val p = game.phase) {
        is Phase.Charleston -> Action.Pass(chooseToss(game, seat, 3, slack = Double.MAX_VALUE))
        Phase.Vote -> Action.Vote(wantSecondCharleston(game, seat))
        Phase.Courtesy -> Action.Pass(
            if (difficulty == Difficulty.BEGINNER) emptyList() else chooseToss(game, seat, 3, slack = 0.05)
        )
        is Phase.Turn -> turn(game, seat)
        is Phase.Calling -> call(game, seat, p)
        is Phase.Over -> error("Game is over")
    }

    // ---- Scoring ----------------------------------------------------------------------------

    private fun live(game: Game, seat: Int): IntArray? {
        if (difficulty != Difficulty.ADVANCED) return null
        val seen = game.visibleCounts(seat)
        return IntArray(Kinds.COUNT) { k -> Kinds.copies(k) - seen[k] }
    }

    /** Lower is better: distance to the closest hand, nudged by how many good options remain. */
    private fun score(game: Game, counts: IntArray, mi: MeldInfo, live: IntArray?): Double {
        val h = game.card.histogram(counts, mi, live)
        var taken = 0
        var sum = 0.0
        var best = -1
        for (d in 0 until 15) {
            if (h[d] == 0) continue
            if (best < 0) best = d
            val n = minOf(h[d], topK - taken)
            sum += n * d
            taken += n
            if (taken >= topK) break
        }
        if (best < 0) return 30.0
        if (taken < topK) sum += (topK - taken) * 15.0
        return best + 0.25 * (sum / topK - best)
    }

    private fun jitter() = if (noise > 0) rng.nextDouble() * noise else 0.0

    // ---- Charleston -------------------------------------------------------------------------

    /**
     * Greedily picks up to [n] tiles to give away. With finite [slack], stops early once a pass
     * would make the hand worse by more than that (used for the optional courtesy pass).
     */
    private fun chooseToss(game: Game, seat: Int, n: Int, slack: Double): List<Int> {
        val hand = game.hands[seat].toMutableList()
        val counts = countsOf(hand)
        val chosen = mutableListOf<Int>()
        var base = score(game, counts, MeldInfo.NONE, null)
        repeat(n) {
            var bestKind = -1
            var bestScore = Double.MAX_VALUE
            for (k in hand.map { it.kind }.distinct()) {
                if (k == Kinds.JOKER) continue
                counts[k]--
                val s = score(game, counts, MeldInfo.NONE, null) + jitter()
                counts[k]++
                if (s < bestScore) { bestScore = s; bestKind = k }
            }
            if (bestKind < 0 || bestScore - base > slack) return chosen
            counts[bestKind]--
            val tile = hand.first { it.kind == bestKind }
            hand.remove(tile)
            chosen += tile.id
            base = bestScore
        }
        return chosen
    }

    private fun wantSecondCharleston(game: Game, seat: Int): Boolean {
        if (difficulty == Difficulty.BEGINNER) return true
        val best = game.card.bestMissing(countsOf(game.hands[seat]), emptyList())
        return best >= 6
    }

    // ---- Turn -------------------------------------------------------------------------------

    private fun turn(game: Game, seat: Int): Action {
        val hand = game.hands[seat]
        val counts = countsOf(hand)
        val melds = game.melds(seat)
        if (!game.dead[seat] && game.card.findWin(counts, melds) != null) return Action.Mahjong

        val mi = MeldInfo(melds)
        val live = live(game, seat)
        if (difficulty != Difficulty.BEGINNER) findJokerSwap(game, seat, counts, mi, live)?.let { return it }

        val danger = if (difficulty == Difficulty.ADVANCED) dangerKinds(game, seat) else emptyMap()
        val myDistance = game.card.bestMissing(counts, melds, live)

        var bestKind = -1
        var bestScore = Double.MAX_VALUE
        val kinds = hand.map { it.kind }.distinct().filter { it != Kinds.JOKER }.ifEmpty { listOf(Kinds.JOKER) }
        for (k in kinds) {
            counts[k]--
            var s = score(game, counts, mi, live) + jitter()
            counts[k]++
            val d = danger[k]
            if (d != null && myDistance >= 3) s += 0.6 * d
            if (s < bestScore) { bestScore = s; bestKind = k }
        }
        return Action.Discard(hand.first { it.kind == bestKind }.id)
    }

    private fun findJokerSwap(game: Game, seat: Int, counts: IntArray, mi: MeldInfo, live: IntArray?): Action? {
        val now = score(game, counts, mi, live)
        for (owner in 0..3) {
            for ((i, exp) in game.exposures[owner].withIndex()) {
                if (exp.jokerCount == 0 || counts[exp.kind] == 0) continue
                counts[exp.kind]--; counts[Kinds.JOKER]++
                val after = score(game, counts, mi, live)
                counts[exp.kind]++; counts[Kinds.JOKER]--
                if (after <= now) {
                    val tile = game.hands[seat].first { it.kind == exp.kind }
                    return Action.SwapJoker(owner, i, tile.id)
                }
            }
        }
        return null
    }

    /** Kinds that opponents with 2+ exposures may still need, weighted by how many want them. */
    private fun dangerKinds(game: Game, seat: Int): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        for (o in 0..3) {
            if (o == seat || game.dead[o]) continue
            val melds = game.melds(o)
            if (melds.size < 2) continue
            for (k in game.card.openKinds(melds)) out.merge(k, 1, Int::plus)
        }
        return out
    }

    // ---- Calling ----------------------------------------------------------------------------

    private fun call(game: Game, seat: Int, p: Phase.Calling): Action {
        val hand = game.hands[seat]
        val counts = countsOf(hand)
        val melds = game.melds(seat)
        val k = p.tile.kind

        counts[k]++
        val wins = game.card.findWin(counts, melds) != null
        counts[k]--
        if (wins) return Action.Mahjong
        if (difficulty == Difficulty.BEGINNER || game.dead[seat]) return Action.Decline

        val real = hand.filter { it.kind == k }
        val jokers = hand.filter { it.isJoker }
        if (real.size + jokers.size < 2) return Action.Decline

        val live = live(game, seat)
        val current = game.card.bestMissing(counts, melds, live)
        val limit = if (difficulty == Difficulty.ADVANCED) 5 else 4
        var bestIds: List<Int>? = null
        var bestMiss = current - 1
        for (size in 3..6) {
            val need = size - 1
            if (real.size + jokers.size < need) break
            val useReal = minOf(real.size, need)
            val ids = real.take(useReal).map { it.id } + jokers.take(need - useReal).map { it.id }
            val after = counts.copyOf()
            after[k] -= useReal
            after[Kinds.JOKER] -= need - useReal
            val m = game.card.bestMissing(after, melds + Meld(k, size), live)
            if (m <= bestMiss && m <= limit) {
                bestMiss = m
                bestIds = ids
            }
        }
        return bestIds?.let { Action.Expose(it) } ?: Action.Decline
    }
}
