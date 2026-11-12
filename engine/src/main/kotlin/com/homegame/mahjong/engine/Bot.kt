package com.homegame.mahjong.engine

import kotlin.random.Random

enum class Difficulty(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced"),
    /** Secret level, unlocked in the app by beating an all-Advanced table. */
    EXPERT("Expert");

    fun atLeast(d: Difficulty) = ordinal >= d.ordinal

    val profile: BotProfile get() = BotProfile.of(this)
}

interface Bot {
    /** Called when [seat] is in [Game.awaiting]. Must return a legal action. */
    fun decide(game: Game, seat: Int): Action
}

/**
 * Everything that makes one bot play differently from another. The four [Difficulty] presets
 * come from [of]; circuit rivals start from a skill level and add style on top.
 *
 * Skill knobs:
 * - [topK]: how many candidate hands it keeps in view.
 * - [noise]: random slop added to every evaluation (bigger = sloppier).
 * - [trackLive]: counts dead tiles, so it stops chasing hands whose tiles are gone.
 * - [weightedEval]: Expert's evaluation, which prices each missing tile by how it can arrive.
 * - [defense]: how hard it avoids discarding what opponents still need.
 *
 * Style knobs (all zero for the presets):
 * - [familyPref]: family name to bonus in tiles. Positive = chases that family.
 * - [valuePref]: bonus per 25 points above 25. Positive = chases expensive hands.
 * - [concealedPref]: bonus for concealed hands.
 * - [jokerlessPref]: 0..1, how much it plans as if its jokers weren't there.
 * - [readDiscards]: reads every opponent's discards to guess their hand, and holds what they need.
 * - [swapSlack]: takes a joker from an exposure even when the swap costs it this much.
 * - [tilt]: extra noise per unit of [HeuristicBot.pressure] (being behind). Negative = clutch.
 */
data class BotProfile(
    val topK: Int,
    val noise: Double,
    val trackLive: Boolean,
    val weightedEval: Boolean,
    val swapJokers: Boolean,
    /** False: only ever calls a discard for Mahjong. */
    val calls: Boolean,
    /** Won't call an exposure that leaves it further than this from a hand. */
    val callLimit: Int,
    /** 0: a call must bring it a tile closer. 1: calls even when it only keeps pace. */
    val callSlack: Int = 0,
    val courtesy: Boolean,
    val alwaysSecondCharleston: Boolean,
    val defense: Double,
    val defenseFrom: Int = 3,
    val familyPref: Map<String, Double> = emptyMap(),
    val valuePref: Double = 0.0,
    val concealedPref: Double = 0.0,
    val jokerlessPref: Double = 0.0,
    val readDiscards: Double = 0.0,
    val swapSlack: Double = 0.0,
    val tilt: Double = 0.0,
) {
    val styled get() = familyPref.isNotEmpty() || valuePref != 0.0 || concealedPref != 0.0 || jokerlessPref != 0.0

    companion object {
        fun of(d: Difficulty): BotProfile = when (d) {
            Difficulty.BEGINNER -> BotProfile(
                topK = 1, noise = 1.2, trackLive = false, weightedEval = false, swapJokers = false,
                calls = false, callLimit = 4, courtesy = false, alwaysSecondCharleston = true, defense = 0.0,
            )
            Difficulty.INTERMEDIATE -> BotProfile(
                topK = 4, noise = 0.15, trackLive = false, weightedEval = false, swapJokers = true,
                calls = true, callLimit = 4, courtesy = true, alwaysSecondCharleston = false, defense = 0.0,
            )
            Difficulty.ADVANCED -> BotProfile(
                topK = 8, noise = 0.02, trackLive = true, weightedEval = false, swapJokers = true,
                calls = true, callLimit = 5, courtesy = true, alwaysSecondCharleston = false, defense = 0.6,
            )
            Difficulty.EXPERT -> BotProfile(
                topK = 8, noise = 0.0, trackLive = true, weightedEval = true, swapJokers = true,
                calls = true, callLimit = 5, courtesy = true, alwaysSecondCharleston = false, defense = 0.6,
            )
        }
    }
}

/**
 * One bot driven by a [BotProfile]. The presets:
 * - Beginner: chases its single closest hand, sloppy discards, only calls for Mahjong.
 * - Intermediate: keeps several hands in view, calls exposures that clearly help, swaps jokers.
 * - Advanced: also tracks dead tiles and avoids feeding opponents who look close.
 * - Expert: plays like Advanced with no randomness, but judges hands by how each missing tile
 *   can arrive (see [weightedMissing]) instead of just counting them.
 */
class HeuristicBot(val profile: BotProfile, seed: Long = Random.nextLong()) : Bot {
    constructor(difficulty: Difficulty, seed: Long = Random.nextLong()) : this(BotProfile.of(difficulty), seed)

    private val rng = Random(seed)
    private val topK = profile.topK

    /** 0 = level or ahead, 1 = far behind in the session. Scales [BotProfile.tilt]. */
    var pressure = 0.0
    private val noise get() = maxOf(0.0, profile.noise + profile.tilt * pressure)

    override fun decide(game: Game, seat: Int): Action = when (val p = game.phase) {
        is Phase.Charleston -> Action.Pass(chooseToss(game, seat, 3, slack = Double.MAX_VALUE))
        Phase.Vote -> Action.Vote(wantSecondCharleston(game, seat))
        Phase.Courtesy -> Action.Pass(
            if (!profile.courtesy) emptyList() else chooseToss(game, seat, 3, slack = 0.05)
        )
        is Phase.Turn -> turn(game, seat)
        is Phase.Calling -> call(game, seat, p)
        is Phase.Over -> error("Game is over")
    }

    // ---- Scoring ----------------------------------------------------------------------------

    private fun live(game: Game, seat: Int): IntArray? {
        if (!profile.trackLive) return null
        val seen = game.visibleCounts(seat)
        return IntArray(Kinds.COUNT) { k -> Kinds.copies(k) - seen[k] }
    }

    /** Lower is better: distance to the closest hand, nudged by how many good options remain. */
    private fun score(game: Game, counts: IntArray, mi: MeldInfo, live: IntArray?): Double = when {
        profile.styled -> styledScore(game, counts, mi, live)
        profile.weightedEval -> weightedScore(game, counts, mi, live)
        else -> plainScore(game, counts, mi, live)
    }

    /** Folds per-hand costs into one number: the best, plus a share of how good the next few are. */
    private fun aggregate(costs: DoubleArray, n: Int): Double {
        if (n == 0) return 30.0
        java.util.Arrays.sort(costs, 0, n)
        val k = minOf(topK, n)
        var sum = 0.0
        for (i in 0 until k) sum += costs[i]
        sum += (topK - k) * 15.0
        val best = costs[0]
        return best + SHAPE_W * (sum / topK - best)
    }

    /**
     * Expert's evaluation. Like [plainScore], but a missing tile's cost depends on how it can arrive:
     * a tile for a pung or bigger can be a joker or a called discard, so it's cheaper than a single
     * or pair tile that must be drawn; and a tile with few live copies left costs more.
     */
    private fun weightedScore(game: Game, counts: IntArray, mi: MeldInfo, live: IntArray?): Double {
        val costs = DoubleArray(game.card.instances.size)
        var n = 0
        for (inst in game.card.instances) {
            val c = weightedMissing(inst, counts, mi, live)
            if (c < IMPOSSIBLE_COST) costs[n++] = c
        }
        return aggregate(costs, n)
    }

    /** A rival's evaluation: the usual cost per hand, minus what its style likes about that hand. */
    private fun styledScore(game: Game, counts: IntArray, mi: MeldInfo, live: IntArray?): Double {
        val jl = profile.jokerlessPref.coerceIn(0.0, 1.0)
        val noJokers = if (jl > 0) counts.copyOf().also { it[Kinds.JOKER] = 0 } else counts
        val costs = DoubleArray(game.card.instances.size)
        var n = 0
        for (inst in game.card.instances) {
            var c = base(game, inst, counts, mi, live)
            if (c >= IMPOSSIBLE_COST) continue
            if (jl > 0) c = (1 - jl) * c + jl * minOf(base(game, inst, noJokers, mi, live), 20.0)
            costs[n++] = c - preference(inst.hand)
        }
        return aggregate(costs, n)
    }

    private fun base(game: Game, inst: HandInstance, counts: IntArray, mi: MeldInfo, live: IntArray?): Double =
        if (profile.weightedEval) weightedMissing(inst, counts, mi, live)
        else game.card.missing(inst, counts, mi, live).let { if (it >= Card.IMPOSSIBLE) IMPOSSIBLE_COST else it.toDouble() }

    private fun preference(hand: CardHand): Double {
        var p = profile.familyPref[hand.family] ?: 0.0
        p += profile.valuePref * (hand.points - 25) / 25.0
        if (hand.concealed) p += profile.concealedPref
        return p
    }

    private fun weightedMissing(inst: HandInstance, counts: IntArray, mi: MeldInfo, live: IntArray?): Double {
        if (!mi.isEmpty) {
            if (inst.hand.concealed) return IMPOSSIBLE_COST
            for ((key, cnt) in mi.keyCounts) if (inst.groupCount(key) < cnt) return IMPOSSIBLE_COST
        }
        var cost = 0.0
        var softNeed = 0.0
        var jokerNeed = 0
        for (k in 0 until Kinds.NON_JOKER) {
            val h = inst.hard[k]
            val s = inst.soft[k] - mi.soft[k]
            if (h == 0 && s == 0) continue
            val a = counts[k]
            val uh = minOf(a, h)
            val dh = h - uh
            if (dh > 0) {
                val l = live?.get(k) ?: Kinds.copies(k)
                if (dh > l) return IMPOSSIBLE_COST
                cost += dh * (HARD_W + SCARCE_W * dh / l)
            }
            val us = minOf(a - uh, s)
            val ds = s - us
            if (ds > 0) {
                jokerNeed += ds
                val l = live?.get(k) ?: Kinds.copies(k)
                softNeed += ds * (SOFT_W + SCARCE_W * ds / (l + 1.0))
            }
        }
        // Jokers in hand fill soft gaps; scale the soft cost by the share still uncovered.
        if (jokerNeed > 0) cost += softNeed * maxOf(0, jokerNeed - counts[Kinds.JOKER]) / jokerNeed
        return cost
    }

    private fun plainScore(game: Game, counts: IntArray, mi: MeldInfo, live: IntArray?): Double {
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
        return best + SHAPE_W * (sum / topK - best)
    }

    private fun jitter(): Double {
        val n = noise
        return if (n > 0) rng.nextDouble() * n else 0.0
    }

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
        if (profile.alwaysSecondCharleston) return true
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
        if (profile.swapJokers) findJokerSwap(game, seat, counts, mi, live)?.let { return it }

        val danger = if (profile.defense > 0) dangerKinds(game, seat) else emptyMap()
        val myDistance = game.card.bestMissing(counts, melds, live)

        var bestKind = -1
        var bestScore = Double.MAX_VALUE
        val kinds = hand.map { it.kind }.distinct().filter { it != Kinds.JOKER }.ifEmpty { listOf(Kinds.JOKER) }
        for (k in kinds) {
            counts[k]--
            var s = score(game, counts, mi, live) + jitter()
            counts[k]++
            val d = danger[k]
            if (d != null && myDistance >= profile.defenseFrom) s += profile.defense * d
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
                if (after <= now + profile.swapSlack) {
                    val tile = game.hands[seat].first { it.kind == exp.kind }
                    return Action.SwapJoker(owner, i, tile.id)
                }
            }
        }
        return null
    }

    /**
     * Kinds that opponents may still need, weighted by how many want them. Every defender watches
     * opponents with 2+ exposures; a bot with [BotProfile.readDiscards] also guesses at every
     * opponent's hand from what they've thrown away.
     */
    private fun dangerKinds(game: Game, seat: Int): Map<Int, Double> {
        val out = HashMap<Int, Double>()
        for (o in 0..3) {
            if (o == seat || game.dead[o]) continue
            val melds = game.melds(o)
            if (melds.size >= 2) for (k in game.card.openKinds(melds)) out.merge(k, 1.0, Double::plus)
            if (profile.readDiscards > 0) {
                for ((k, w) in readHand(game, o, melds)) out.merge(k, profile.readDiscards * w, Double::plus)
            }
        }
        return out
    }

    /**
     * Guesses what [o] is collecting: the hands that fit their exposures and use the fewest of the
     * tiles they've discarded. Returns each kind those hands need, weighted 0..1 by how many do.
     */
    private fun readHand(game: Game, o: Int, melds: List<Meld>): Map<Int, Double> {
        val thrown = IntArray(Kinds.COUNT)
        var n = 0
        for (d in game.discards) if (d.seat == o) { thrown[d.tile.kind]++; n++ }
        if (n < 4) return emptyMap()
        val mi = MeldInfo(melds)
        val zero = IntArray(Kinds.COUNT)
        val insts = game.card.instances
        val clash = IntArray(insts.size) { Int.MAX_VALUE }
        var best = Int.MAX_VALUE
        for ((i, inst) in insts.withIndex()) {
            if (game.card.missing(inst, zero, mi) >= Card.IMPOSSIBLE) continue
            var c = 0
            for (k in 0 until Kinds.NON_JOKER) if (thrown[k] > 0 && (inst.hard[k] > 0 || inst.soft[k] > 0)) c += thrown[k]
            clash[i] = c
            if (c < best) best = c
        }
        if (best == Int.MAX_VALUE) return emptyMap()
        val need = IntArray(Kinds.NON_JOKER)
        var plausible = 0
        for ((i, inst) in insts.withIndex()) {
            if (clash[i] != best) continue
            plausible++
            for (k in 0 until Kinds.NON_JOKER) if (inst.hard[k] > 0 || inst.soft[k] - mi.soft[k] > 0) need[k]++
        }
        val out = HashMap<Int, Double>()
        for (k in 0 until Kinds.NON_JOKER) if (need[k] > 0) out[k] = need[k].toDouble() / plausible
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
        if (!profile.calls || game.dead[seat]) return Action.Decline

        val real = hand.filter { it.kind == k }
        val jokers = if (profile.jokerlessPref >= 0.5) emptyList() else hand.filter { it.isJoker }
        if (real.size + jokers.size < 2) return Action.Decline

        val live = live(game, seat)
        val current = game.card.bestMissing(counts, melds, live)
        val limit = profile.callLimit
        var bestIds: List<Int>? = null
        var bestMiss = current - 1 + profile.callSlack
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

    private companion object {
        /** How much the spread of good options counts next to the single closest hand. */
        const val SHAPE_W = 0.25
        // Expert's tile costs, tuned by bot-vs-bot simulation (see sim/Simulator.kt).
        const val HARD_W = 1.0
        const val SOFT_W = 0.5
        const val SCARCE_W = 1.0
        const val IMPOSSIBLE_COST = 1e9
    }
}
