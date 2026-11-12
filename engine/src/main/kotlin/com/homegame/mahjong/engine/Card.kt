package com.homegame.mahjong.engine

/** A suit placeholder on the card: A, B, C are always three different suits. */
enum class SuitVar { A, B, C }

sealed interface TileSpec {
    data class Number(val rank: Int, val suit: SuitVar) : TileSpec
    data class MatchingDragon(val suit: SuitVar) : TileSpec
    data class Fixed(val kind: Int) : TileSpec
}

data class GroupSpec(val spec: TileSpec, val size: Int)

/**
 * One line on the card.
 *
 * Pattern notation: space-separated tokens. Within a token, runs of the same character are one
 * group (`222` = pung, `11` = pair, `2026` = four singles). A trailing `a`/`b`/`c` assigns the
 * token's numbers/dragons to suit A/B/C. `D` = dragon matching that suit, `0` = Soap,
 * `F` = flower, `N E W S` = winds. [run] lets every number shift up by 0..run
 * (consecutive-run and like-number hands).
 */
data class CardHand(
    val id: String,
    val family: String,
    val pattern: String,
    val points: Int,
    val concealed: Boolean,
    val run: Int,
    val groups: List<GroupSpec>,
) {
    val tileCount get() = groups.sumOf { it.size }
    /** Hands with no group of 3+ can't use jokers, so they get no jokerless bonus. */
    val usesJokers get() = groups.any { it.size >= 3 }
}

object PatternParser {
    fun parse(pattern: String): List<GroupSpec> {
        val groups = mutableListOf<GroupSpec>()
        for (token in pattern.trim().split(Regex("\\s+"))) {
            var body = token
            var sv: SuitVar? = null
            val last = token.last()
            if (last in "abc") {
                sv = SuitVar.entries["abc".indexOf(last)]
                body = token.dropLast(1)
            }
            require(body.isNotEmpty()) { "Empty token in '$pattern'" }
            var i = 0
            while (i < body.length) {
                val c = body[i]
                var j = i
                while (j < body.length && body[j] == c) j++
                groups += GroupSpec(specFor(c, sv, token), j - i)
                i = j
            }
        }
        return groups
    }

    private fun specFor(c: Char, sv: SuitVar?, token: String): TileSpec = when (c) {
        in '1'..'9' -> TileSpec.Number(c - '0', sv ?: error("Number needs a suit letter in '$token'"))
        '0' -> TileSpec.Fixed(Kinds.dragon(Dragon.WHITE))
        'D' -> TileSpec.MatchingDragon(sv ?: error("Dragon needs a suit letter in '$token'"))
        'F' -> TileSpec.Fixed(Kinds.FLOWER)
        'N' -> TileSpec.Fixed(Kinds.wind(Wind.NORTH))
        'E' -> TileSpec.Fixed(Kinds.wind(Wind.EAST))
        'W' -> TileSpec.Fixed(Kinds.wind(Wind.WEST))
        'S' -> TileSpec.Fixed(Kinds.wind(Wind.SOUTH))
        else -> error("Unknown tile '$c' in '$token'")
    }
}

/** A concrete group: a kind and how many of it. Exposed melds use the same shape. */
data class Meld(val kind: Int, val size: Int) {
    val key get() = kind * 8 + size
}

/** A card hand with suits and run offset filled in. */
class HandInstance(val hand: CardHand, val groups: List<Meld>) {
    /** Tiles needed in singles/pairs (real tiles only). */
    val hard = IntArray(Kinds.NON_JOKER)
    /** Tiles needed in groups of 3+ (jokers allowed). */
    val soft = IntArray(Kinds.NON_JOKER)
    private val keyCounts: Map<Int, Int>

    init {
        for (g in groups) if (g.size >= 3) soft[g.kind] += g.size else hard[g.kind] += g.size
        keyCounts = groups.groupingBy { it.key }.eachCount()
    }

    fun groupCount(key: Int) = keyCounts[key] ?: 0

    fun describe() = groups.joinToString(" ") { m -> Kinds.short(m.kind).let { s -> List(m.size) { s }.joinToString("") } }

    companion object {
        private val suitPerms: List<List<Suit>> = run {
            val s = Suit.entries
            listOf(
                listOf(s[0], s[1], s[2]), listOf(s[0], s[2], s[1]), listOf(s[1], s[0], s[2]),
                listOf(s[1], s[2], s[0]), listOf(s[2], s[0], s[1]), listOf(s[2], s[1], s[0]),
            )
        }

        fun expand(hand: CardHand): List<HandInstance> {
            val seen = HashSet<List<Meld>>()
            val out = mutableListOf<HandInstance>()
            for (perm in suitPerms) {
                offsets@ for (off in 0..hand.run) {
                    val melds = ArrayList<Meld>(hand.groups.size)
                    for (g in hand.groups) {
                        val kind = when (val sp = g.spec) {
                            is TileSpec.Number -> {
                                val r = sp.rank + off
                                if (r > 9) continue@offsets
                                Kinds.suited(perm[sp.suit.ordinal], r)
                            }
                            is TileSpec.MatchingDragon -> Kinds.dragonOf(perm[sp.suit.ordinal])
                            is TileSpec.Fixed -> sp.kind
                        }
                        melds += Meld(kind, g.size)
                    }
                    val key = melds.sortedWith(compareBy({ it.kind }, { it.size }))
                    if (seen.add(key)) out += HandInstance(hand, melds)
                }
            }
            return out
        }
    }
}

/** Pre-digested exposures, computed once per evaluation pass. */
class MeldInfo(melds: List<Meld>) {
    val soft = IntArray(Kinds.NON_JOKER)
    val keyCounts: Map<Int, Int> = melds.groupingBy { it.key }.eachCount()
    val tileCount = melds.sumOf { it.size }
    val isEmpty = melds.isEmpty()

    init {
        for (m in melds) soft[m.kind] += m.size
    }

    companion object {
        val NONE = MeldInfo(emptyList())
    }
}

class Evaluation(val instance: HandInstance, val missing: Int)

data class GroupProgress(val kind: Int, val size: Int, val real: Int, val jokers: Int, val exposed: Boolean) {
    val have get() = real + jokers
}

class Card(val name: String, val hands: List<CardHand>) {
    val instances: List<HandInstance> = hands.flatMap { HandInstance.expand(it) }

    /**
     * Tiles still needed to complete [inst] from concealed [counts] plus exposures [mi], or
     * [IMPOSSIBLE] if the exposures don't fit it. With [live] (copies not yet seen by this
     * player), hands needing real tiles that are all gone are also impossible.
     */
    fun missing(inst: HandInstance, counts: IntArray, mi: MeldInfo, live: IntArray? = null): Int {
        if (!mi.isEmpty) {
            if (inst.hand.concealed) return IMPOSSIBLE
            for ((key, n) in mi.keyCounts) if (inst.groupCount(key) < n) return IMPOSSIBLE
        }
        var missHard = 0
        var jokerNeed = 0
        val hard = inst.hard
        val soft = inst.soft
        for (k in 0 until Kinds.NON_JOKER) {
            val h = hard[k]
            val s = soft[k] - mi.soft[k]
            if (h == 0 && s == 0) continue
            val a = counts[k]
            val uh = minOf(a, h)
            val dh = h - uh
            if (live != null && dh > live[k]) return IMPOSSIBLE
            missHard += dh
            val us = minOf(a - uh, s)
            jokerNeed += s - us
        }
        return missHard + maxOf(0, jokerNeed - counts[Kinds.JOKER])
    }

    fun bestMissing(counts: IntArray, melds: List<Meld>, live: IntArray? = null): Int {
        val mi = MeldInfo(melds)
        var best = IMPOSSIBLE
        for (inst in instances) {
            val m = missing(inst, counts, mi, live)
            if (m < best) best = m
        }
        return best
    }

    /** Count of instances at each distance 0..14; index 15 = impossible. Cheap input for bots. */
    fun histogram(counts: IntArray, mi: MeldInfo, live: IntArray? = null): IntArray {
        val h = IntArray(16)
        for (inst in instances) h[minOf(15, missing(inst, counts, mi, live))]++
        return h
    }

    /** Closest hands first, highest points breaking ties. */
    fun ranked(counts: IntArray, melds: List<Meld>, limit: Int = 5): List<Evaluation> {
        val mi = MeldInfo(melds)
        return instances.asSequence()
            .map { Evaluation(it, missing(it, counts, mi)) }
            .filter { it.missing < IMPOSSIBLE }
            .sortedWith(compareBy<Evaluation> { it.missing }.thenByDescending { it.instance.hand.points })
            .distinctBy { it.instance.hand.id }
            .take(limit)
            .toList()
    }

    /**
     * The closest way to make one specific card hand (best suits and run offset), or null if
     * no version of it fits the exposures any more.
     */
    fun bestFor(handId: String, counts: IntArray, melds: List<Meld>): Evaluation? {
        val mi = MeldInfo(melds)
        return instances.asSequence()
            .filter { it.hand.id == handId }
            .map { Evaluation(it, missing(it, counts, mi)) }
            .filter { it.missing < IMPOSSIBLE }
            .minByOrNull { it.missing }
    }

    /** The winning instance (highest points) if these 14 tiles make a hand, else null. */
    fun findWin(counts: IntArray, melds: List<Meld>): HandInstance? {
        val mi = MeldInfo(melds)
        if (counts.sum() + mi.tileCount != 14) return null
        return instances.filter { missing(it, counts, mi) == 0 }.maxByOrNull { it.hand.points }
    }

    /** True if at least one hand on the card still fits these exposures. */
    fun compatible(melds: List<Meld>): Boolean {
        val mi = MeldInfo(melds)
        val zero = IntArray(Kinds.COUNT)
        return instances.any { missing(it, zero, mi) < IMPOSSIBLE }
    }

    /**
     * Group-by-group progress toward [inst] for the hint panel: how many real tiles and jokers
     * the player already has toward each group. Exposed groups come back complete.
     */
    fun breakdown(inst: HandInstance, counts: IntArray, melds: List<Meld>): List<GroupProgress> {
        val c = counts.copyOf()
        val openMelds = melds.toMutableList()
        val out = arrayOfNulls<GroupProgress>(inst.groups.size)
        inst.groups.forEachIndexed { i, g ->
            val m = openMelds.indexOf(g)
            if (m >= 0) {
                openMelds.removeAt(m)
                out[i] = GroupProgress(g.kind, g.size, g.size, 0, exposed = true)
            }
        }
        // Singles and pairs first: they can only use real tiles.
        for (pass in 0..1) inst.groups.forEachIndexed { i, g ->
            if (out[i] != null || (g.size >= 3) != (pass == 1)) return@forEachIndexed
            val take = minOf(c[g.kind], g.size)
            c[g.kind] -= take
            out[i] = GroupProgress(g.kind, g.size, take, 0, exposed = false)
        }
        inst.groups.forEachIndexed { i, g ->
            val p = out[i]!!
            if (g.size >= 3 && !p.exposed && p.real < g.size) {
                val j = minOf(c[Kinds.JOKER], g.size - p.real)
                c[Kinds.JOKER] -= j
                out[i] = p.copy(jokers = j)
            }
        }
        return out.map { it!! }
    }

    /** Kinds that some hand fitting these exposures still needs (for reading opponents). */
    fun openKinds(melds: List<Meld>): Set<Int> {
        val mi = MeldInfo(melds)
        val zero = IntArray(Kinds.COUNT)
        val out = HashSet<Int>()
        for (inst in instances) {
            if (missing(inst, zero, mi) >= IMPOSSIBLE) continue
            for (k in 0 until Kinds.NON_JOKER) if (inst.hard[k] > 0 || inst.soft[k] - mi.soft[k] > 0) out += k
        }
        return out
    }

    companion object {
        const val IMPOSSIBLE = 99

        /** Lines: `family | pattern | points | X or C | run`. Blank lines and # comments ignored. */
        fun parse(name: String, text: String): Card {
            val hands = mutableListOf<CardHand>()
            val perFamily = HashMap<String, Int>()
            for ((lineNo, raw) in text.lines().withIndex()) {
                val line = raw.substringBefore('#').trim()
                if (line.isEmpty()) continue
                val parts = line.split('|').map { it.trim() }
                require(parts.size == 5) { "Line ${lineNo + 1}: expected 5 fields" }
                val (family, pattern, pts, xc, run) = parts
                val n = perFamily.merge(family, 1, Int::plus)!!
                val groups = PatternParser.parse(pattern)
                val hand = CardHand(
                    id = "$family #$n", family = family, pattern = pattern,
                    points = pts.toInt(), concealed = xc.equals("C", true),
                    run = run.toInt(), groups = groups,
                )
                require(hand.tileCount == 14) { "Line ${lineNo + 1} '$pattern' has ${hand.tileCount} tiles" }
                hands += hand
            }
            return Card(name, hands)
        }

        fun practice(): Card {
            val text = Card::class.java.getResourceAsStream("/cards/practice.card")!!
                .bufferedReader().use { it.readText() }
            return parse("Practice Card", text)
        }
    }
}
