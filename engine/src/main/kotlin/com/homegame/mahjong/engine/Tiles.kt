package com.homegame.mahjong.engine

enum class Suit(val label: String) { BAM("Bam"), CRAK("Crak"), DOT("Dot") }

enum class Wind(val letter: Char, val label: String) {
    NORTH('N', "North"), EAST('E', "East"), WEST('W', "West"), SOUTH('S', "South")
}

enum class Dragon(val label: String) { GREEN("Green"), RED("Red"), WHITE("Soap") }

/**
 * Every tile face maps to a "kind" index 0..35 so hands can be counted in a flat array.
 *   0..26  suited tiles (suit * 9 + rank - 1)
 *   27..30 winds (N, E, W, S)
 *   31..33 dragons (Green, Red, White/Soap)
 *   34     flower
 *   35     joker
 */
object Kinds {
    const val COUNT = 36
    const val NON_JOKER = 35
    const val FLOWER = 34
    const val JOKER = 35

    fun suited(suit: Suit, rank: Int): Int {
        require(rank in 1..9)
        return suit.ordinal * 9 + rank - 1
    }

    fun wind(w: Wind) = 27 + w.ordinal
    fun dragon(d: Dragon) = 31 + d.ordinal

    /** American convention: Green goes with Bams, Red with Craks, Soap with Dots. */
    fun dragonOf(suit: Suit) = dragon(
        when (suit) {
            Suit.BAM -> Dragon.GREEN
            Suit.CRAK -> Dragon.RED
            Suit.DOT -> Dragon.WHITE
        }
    )

    fun isSuited(k: Int) = k in 0..26
    fun isWind(k: Int) = k in 27..30
    fun isDragon(k: Int) = k in 31..33
    fun suitOf(k: Int): Suit = Suit.entries[k / 9]
    fun rankOf(k: Int) = k % 9 + 1
    fun windOf(k: Int): Wind = Wind.entries[k - 27]
    fun dragonOfKind(k: Int): Dragon = Dragon.entries[k - 31]

    /** Physical copies of each kind in a full set. */
    fun copies(k: Int) = when (k) {
        FLOWER, JOKER -> 8
        else -> 4
    }

    fun name(k: Int): String = when {
        isSuited(k) -> "${rankOf(k)} ${suitOf(k).label}"
        isWind(k) -> windOf(k).label
        k == 33 -> "Soap"
        isDragon(k) -> "${dragonOfKind(k).label} Dragon"
        k == FLOWER -> "Flower"
        k == JOKER -> "Joker"
        else -> "?"
    }

    fun short(k: Int): String = when {
        isSuited(k) -> "${rankOf(k)}${suitOf(k).label.first()}"
        isWind(k) -> windOf(k).letter.toString()
        isDragon(k) -> when (dragonOfKind(k)) {
            Dragon.GREEN -> "GD"
            Dragon.RED -> "RD"
            Dragon.WHITE -> "0"
        }
        k == FLOWER -> "F"
        k == JOKER -> "J"
        else -> "?"
    }
}

/** One physical tile. [id] is unique within a game so the UI can track it. */
data class Tile(val id: Int, val kind: Int) {
    val isJoker get() = kind == Kinds.JOKER
    override fun toString() = Kinds.short(kind)
}

object TileSet {
    /** The 152-tile American set. */
    fun create(): List<Tile> {
        val tiles = ArrayList<Tile>(152)
        var id = 0
        for (k in 0 until Kinds.COUNT) repeat(Kinds.copies(k)) { tiles += Tile(id++, k) }
        return tiles
    }
}

fun countsOf(tiles: Collection<Tile>): IntArray {
    val c = IntArray(Kinds.COUNT)
    for (t in tiles) c[t.kind]++
    return c
}
