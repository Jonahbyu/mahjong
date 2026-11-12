package com.homegame.mahjong.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardTest {
    private val card = Card.practice()
    private fun b(r: Int) = Kinds.suited(Suit.BAM, r)
    private fun c(r: Int) = Kinds.suited(Suit.CRAK, r)
    private fun d(r: Int) = Kinds.suited(Suit.DOT, r)
    private val soap = Kinds.dragon(Dragon.WHITE)

    private fun counts(vararg kinds: Pair<Int, Int>): IntArray {
        val a = IntArray(Kinds.COUNT)
        for ((k, n) in kinds) a[k] += n
        return a
    }

    @Test
    fun fullSetHas152Tiles() {
        val tiles = TileSet.create()
        assertEquals(152, tiles.size)
        val h = countsOf(tiles)
        assertEquals(8, h[Kinds.JOKER])
        assertEquals(8, h[Kinds.FLOWER])
        assertEquals(4, h[b(5)])
        assertEquals(152, tiles.map { it.id }.toSet().size)
    }

    @Test
    fun practiceCardParsesAndEveryHandIs14Tiles() {
        assertTrue(card.hands.size >= 35)
        card.hands.forEach { assertEquals(14, it.tileCount, it.pattern) }
        assertTrue(card.instances.size > card.hands.size)
    }

    @Test
    fun parserGroupsRunsAndSuits() {
        val g = PatternParser.parse("FF 2026a 222b")
        assertEquals(listOf(2, 1, 1, 1, 1, 3), g.map { it.size })
        assertEquals(TileSpec.Fixed(soap), g[2].spec)
        assertEquals(TileSpec.Number(2, SuitVar.B), g[5].spec)
    }

    // FFFF 2026a 222b 666c
    private val yearWin = counts(Kinds.FLOWER to 4, b(2) to 2, soap to 1, b(6) to 1, c(2) to 3, d(6) to 3)

    @Test
    fun recognisesExactWin() {
        val win = card.findWin(yearWin, emptyList())
        assertNotNull(win)
        assertEquals("FFFF 2026a 222b 666c", win.hand.pattern)
    }

    @Test
    fun jokerCanFillAPung() {
        val h = yearWin.copyOf()
        h[d(6)] -= 1; h[Kinds.JOKER] += 1
        assertNotNull(card.findWin(h, emptyList()))
    }

    @Test
    fun jokerCannotFillASingle() {
        val h = yearWin.copyOf()
        h[b(6)] -= 1; h[Kinds.JOKER] += 1 // the 6 in "2026" is a single
        assertNull(card.findWin(h, emptyList()))
    }

    @Test
    fun jokerCannotFillAPair() {
        // FF 2222a 44a 66b 8888b with a joker in place of one 4
        val h = counts(Kinds.FLOWER to 2, b(2) to 4, b(4) to 1, Kinds.JOKER to 1, c(6) to 2, c(8) to 4)
        assertNull(card.findWin(h, emptyList()))
        h[Kinds.JOKER] -= 1; h[b(4)] += 1
        assertNotNull(card.findWin(h, emptyList()))
    }

    @Test
    fun exposuresMustFitTheHand() {
        // Exposed pung of 6 Dot + 222 Crak: the rest is concealed.
        val concealed = counts(Kinds.FLOWER to 4, b(2) to 2, soap to 1, b(6) to 1)
        val melds = listOf(Meld(c(2), 3), Meld(d(6), 3))
        assertNotNull(card.findWin(concealed, melds))
        assertTrue(card.compatible(melds))
        assertFalse(card.compatible(listOf(Meld(b(1), 6))), "no hand has a sextet")
    }

    @Test
    fun concealedHandsRejectExposures() {
        val concealedOnly = card.instances.filter { it.hand.concealed }
        assertTrue(concealedOnly.isNotEmpty())
        val mi = MeldInfo(listOf(Meld(Kinds.FLOWER, 3)))
        concealedOnly.forEach { assertEquals(Card.IMPOSSIBLE, card.missing(it, IntArray(Kinds.COUNT), mi)) }
    }

    @Test
    fun bestForTracksOneChosenHand() {
        val year = card.hands.first { it.pattern == "FFFF 2026a 222b 666c" }
        val h = yearWin.copyOf()
        h[Kinds.FLOWER] -= 2
        val e = card.bestFor(year.id, h, emptyList())
        assertNotNull(e)
        assertEquals(year.id, e.instance.hand.id)
        assertEquals(2, e.missing)
        // A concealed hand can't be targeted once something is exposed.
        val concealed = card.hands.first { it.concealed }
        assertNull(card.bestFor(concealed.id, h, listOf(Meld(c(2), 3))))
    }

    @Test
    fun runHandsShift() {
        // "11a 222a 3333a 444a 55a" run 4 -> 55 666 7777 888 99 in dots is valid
        val h = counts(d(5) to 2, d(6) to 3, d(7) to 4, d(8) to 3, d(9) to 2)
        assertNotNull(card.findWin(h, emptyList()))
    }

    @Test
    fun breakdownShowsProgressPerGroup() {
        val h = yearWin.copyOf()
        h[d(6)] -= 2; h[Kinds.JOKER] += 1 // 666 Dot now one real 6 + a joker
        val inst = card.instances.first { it.hand.pattern == "FFFF 2026a 222b 666c" && card.missing(it, yearWin, MeldInfo.NONE) == 0 }
        val parts = card.breakdown(inst, h, emptyList())
        val six = parts.first { it.kind == d(6) }
        assertEquals(1, six.real)
        assertEquals(1, six.jokers)
        assertEquals(14 - 1, parts.sumOf { it.have })
    }

    @Test
    fun missingCountsTilesNeeded() {
        val h = yearWin.copyOf()
        h[c(2)] -= 2 // now 12 tiles, two short of 222b
        assertEquals(2, card.bestMissing(h, emptyList()))
    }
}
