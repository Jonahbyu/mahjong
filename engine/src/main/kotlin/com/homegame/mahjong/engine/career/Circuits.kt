package com.homegame.mahjong.engine.career

import com.homegame.mahjong.engine.Rules

/** Four circuits make a tier. The last circuit of each tier is a major with a bigger prize. */
enum class Tier(val label: String) {
    NEIGHBORHOOD("Neighborhood"), CLUB("Club"), REGIONAL("Regional"), MASTERS("Masters")
}

/**
 * One rung of the ladder: a short league against six rivals and a boss, then a final table.
 * [rivals] has the six regulars first and the boss last.
 */
class Circuit(
    /** 0-based position on the ladder. */
    val index: Int,
    val name: String,
    val venue: String,
    val tier: Tier,
    val rivals: List<String>,
    val rules: Rules = Rules(),
    /** False: hand hints are switched off at this table. */
    val hints: Boolean = true,
) {
    init { require(rivals.size == 7) { "$name needs 6 rivals and a boss" } }

    val number get() = index + 1
    val major get() = index % 4 == 3
    val bossId get() = rivals.last()
    val regulars get() = rivals.dropLast(1)

    /** Skill level each rival plays at here. Regulars spread around the circuit's level; the boss sits above. */
    fun level(id: String): Int {
        val i = rivals.indexOf(id)
        require(i >= 0) { "$id isn't in $name" }
        val offset = if (i == 6) (if (major) 3 else 2) else REGULAR_OFFSETS[i]
        return (number + offset).coerceIn(1, MAX_LEVEL)
    }

    /** Coins for winning the circuit. */
    val prize get() = (100 + 40 * index) * (if (major) 2 else 1)

    /** Coins for 1st..4th in a session. */
    fun placementCoins(place: Int): Int = (listOf(40, 20, 10, 0)[place] * (1 + index / 8.0)).toInt()

    private companion object {
        val REGULAR_OFFSETS = listOf(-1, -1, 0, 0, 0, 1)
    }
}

object Circuits {
    private val noCourtesy = Rules(courtesyPass = false)

    val all: List<Circuit> = listOf(
        Circuit(0, "Rec Room", "Folding tables and free coffee", Tier.NEIGHBORHOOD,
            listOf("penny", "ida", "bernie", "sunny", "rosa", "theo", "gus")),
        Circuit(1, "Library Basement", "Quiet please", Tier.NEIGHBORHOOD,
            listOf("ida", "sunny", "mabel", "dot", "theo", "penny", "rosa")),
        Circuit(2, "Church Hall", "Bake sale at the break", Tier.NEIGHBORHOOD,
            listOf("bernie", "mabel", "dot", "rosa", "hank", "gus", "penny")),
        Circuit(3, "Community Cup", "The neighborhood's big night", Tier.NEIGHBORHOOD,
            listOf("ida", "rosa", "hank", "gus", "june", "penny", "lou")),

        Circuit(4, "Thursday Club", "Members and their guests", Tier.CLUB,
            listOf("theo", "dot", "bernie", "june", "hank", "marco", "delphine")),
        Circuit(5, "Garden Society", "Tables under the pergola", Tier.CLUB,
            listOf("mabel", "sunny", "june", "lou", "ricky", "delphine", "marco")),
        Circuit(6, "Lakeside Lodge", "Fireplace, felt and fierce play", Tier.CLUB,
            listOf("rosa", "theo", "hank", "ricky", "delphine", "marco", "june")),
        Circuit(7, "Club Championship", "The clubs send their best", Tier.CLUB,
            listOf("penny", "june", "lou", "ricky", "delphine", "marco", "harriet")),

        Circuit(8, "County Fair", "Blue ribbons for the winners", Tier.REGIONAL,
            listOf("bernie", "dot", "hank", "ricky", "vivian", "harriet", "wally"), rules = noCourtesy),
        Circuit(9, "Riverboat Social", "Paddle wheels and high stakes", Tier.REGIONAL,
            listOf("sunny", "mabel", "marco", "delphine", "agnes", "wally", "vivian"), rules = noCourtesy),
        Circuit(10, "Harbor Hotel Open", "A ballroom full of strangers", Tier.REGIONAL,
            listOf("theo", "rosa", "june", "harriet", "vivian", "wally", "agnes"), rules = noCourtesy),
        Circuit(11, "Regional Open", "Players from three states", Tier.REGIONAL,
            listOf("marco", "delphine", "harriet", "vivian", "agnes", "wally", "odette"), rules = noCourtesy),

        Circuit(12, "Mountain Retreat", "Invitation only", Tier.MASTERS,
            listOf("penny", "ricky", "agnes", "odette", "kit", "nora", "sterling"), hints = false),
        Circuit(13, "Grand Ballroom", "Chandeliers and silence", Tier.MASTERS,
            listOf("lou", "harriet", "wally", "sterling", "nora", "odette", "kit"), hints = false),
        Circuit(14, "The Invitational", "The best players you've met, all at once", Tier.MASTERS,
            listOf("june", "marco", "vivian", "kit", "sterling", "odette", "nora"), hints = false),
        Circuit(15, "Grand Masters", "One title", Tier.MASTERS,
            listOf("harriet", "wally", "agnes", "odette", "kit", "nora", "dowager"), hints = false),
    )

    init {
        all.forEachIndexed { i, c -> require(c.index == i) }
        all.forEach { c -> c.rivals.forEach { Rivals.byId(it) }; require(c.rivals.toSet().size == 7) { c.name } }
    }
}
