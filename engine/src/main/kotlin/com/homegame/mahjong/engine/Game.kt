package com.homegame.mahjong.engine

import kotlin.random.Random

data class Rules(
    val secondCharleston: Boolean = true,
    val courtesyPass: Boolean = true,
    val jokerlessBonus: Boolean = true,
)

/** Pass direction as a seat offset. Seats go counter-clockwise: 0 East, 1 South, 2 West, 3 North. */
enum class PassDir(val offset: Int, val label: String) {
    RIGHT(1, "Right"), ACROSS(2, "Across"), LEFT(3, "Left")
}

val SEAT_NAMES = listOf("East", "South", "West", "North")

class Exposure(val kind: Int, val tiles: MutableList<Tile>) {
    val meld get() = Meld(kind, tiles.size)
    val jokerCount get() = tiles.count { it.isJoker }
}

data class DiscardRecord(val seat: Int, val tile: Tile)

data class GameResult(
    val winner: Int?,
    val discarder: Int?,
    val hand: CardHand?,
    val jokerless: Boolean,
    /** Net score change per seat. */
    val payments: IntArray,
) {
    val selfDrawn get() = winner != null && discarder == null
    val wallGame get() = winner == null
}

sealed interface Action {
    /** Charleston (exactly 3) or courtesy pass (0..3). */
    data class Pass(val tileIds: List<Int>) : Action
    data class Vote(val yes: Boolean) : Action
    data class Discard(val tileId: Int) : Action
    /** Self-drawn on your turn, or claiming the discard during a call window. */
    data object Mahjong : Action
    data class SwapJoker(val owner: Int, val exposureIndex: Int, val tileId: Int) : Action
    /** Claim the discard and expose it with these tiles from your hand. */
    data class Expose(val tileIds: List<Int>) : Action
    data object Decline : Action
}

sealed interface Phase {
    data class Charleston(val round: Int, val dir: PassDir) : Phase
    data object Vote : Phase
    data object Courtesy : Phase
    data class Turn(val seat: Int) : Phase
    data class Calling(val discarder: Int, val tile: Tile, val eligible: List<Int>) : Phase
    data class Over(val result: GameResult) : Phase
}

sealed interface GameEvent {
    data class Passed(val round: Int, val dir: PassDir) : GameEvent
    data class CourtesyDone(val counts: List<Int>) : GameEvent
    data class SecondCharleston(val happening: Boolean) : GameEvent
    data class Drew(val seat: Int) : GameEvent
    data class Discarded(val seat: Int, val tile: Tile) : GameEvent
    data class Exposed(val seat: Int, val from: Int, val meld: Meld) : GameEvent
    data class JokerSwapped(val seat: Int, val owner: Int, val kind: Int) : GameEvent
    data class Finished(val result: GameResult) : GameEvent
}

class Game(
    val card: Card,
    val rules: Rules = Rules(),
    seed: Long = Random.nextLong(),
) {
    private val rng = Random(seed)
    val wall = ArrayDeque<Tile>()
    val hands = Array(4) { mutableListOf<Tile>() }
    val exposures = Array(4) { mutableListOf<Exposure>() }
    val discards = mutableListOf<DiscardRecord>()
    val dead = BooleanArray(4)
    val events = mutableListOf<GameEvent>()
    var phase: Phase
        private set

    private val pending = arrayOfNulls<Action>(4)
    private val charlestonQueue = ArrayDeque<Phase.Charleston>()

    init {
        wall.addAll(TileSet.create().shuffled(rng))
        repeat(13) { for (s in 0..3) hands[s] += wall.removeFirst() }
        hands[0] += wall.removeFirst()
        listOf(PassDir.RIGHT, PassDir.ACROSS, PassDir.LEFT).forEach { charlestonQueue += Phase.Charleston(1, it) }
        phase = charlestonQueue.removeFirst()
    }

    fun melds(seat: Int): List<Meld> = exposures[seat].map { it.meld }

    /** Seats whose input the game is waiting for. */
    fun awaiting(): List<Int> = when (val p = phase) {
        is Phase.Charleston, Phase.Vote, Phase.Courtesy -> (0..3).filter { pending[it] == null }
        is Phase.Turn -> listOf(p.seat)
        is Phase.Calling -> p.eligible.filter { pending[it] == null }
        is Phase.Over -> emptyList()
    }

    /** Tile kinds [seat] can see: own hand, all exposures, all discards. */
    fun visibleCounts(seat: Int): IntArray {
        val c = countsOf(hands[seat])
        for (e in exposures) for (x in e) for (t in x.tiles) c[t.kind]++
        for (d in discards) c[d.tile.kind]++
        return c
    }

    /** Applies [action] for [seat]. Returns null on success or a reason it was refused. */
    fun act(seat: Int, action: Action): String? {
        if (seat !in awaiting()) return "Not your move"
        return when (val p = phase) {
            is Phase.Charleston -> onCharleston(seat, action)
            Phase.Vote -> onVote(seat, action)
            Phase.Courtesy -> onCourtesy(seat, action)
            is Phase.Turn -> onTurn(seat, action)
            is Phase.Calling -> onCalling(seat, action, p)
            is Phase.Over -> "Game over"
        }
    }

    // ---- Charleston -------------------------------------------------------------------------

    private fun tilesFor(seat: Int, ids: List<Int>): List<Tile>? {
        if (ids.toSet().size != ids.size) return null
        val hand = hands[seat]
        return ids.map { id -> hand.firstOrNull { it.id == id } ?: return null }
    }

    private fun onCharleston(seat: Int, action: Action): String? {
        val pass = action as? Action.Pass ?: return "Choose 3 tiles to pass"
        val tiles = tilesFor(seat, pass.tileIds) ?: return "Those tiles aren't in your hand"
        if (tiles.size != 3) return "Pass exactly 3 tiles"
        if (tiles.any { it.isJoker }) return "Jokers can't be passed"
        pending[seat] = action
        if (pending.all { it != null }) {
            val p = phase as Phase.Charleston
            exchange(p.dir.offset)
            events += GameEvent.Passed(p.round, p.dir)
            nextCharlestonStep(p.round)
        }
        return null
    }

    private fun exchange(offset: Int) {
        val outgoing = (0..3).map { s -> tilesFor(s, (pending[s] as Action.Pass).tileIds)!! }
        for (s in 0..3) hands[s].removeAll(outgoing[s].toSet())
        for (s in 0..3) hands[(s + offset) % 4] += outgoing[s]
        pending.fill(null)
    }

    private fun nextCharlestonStep(round: Int) {
        if (charlestonQueue.isNotEmpty()) {
            phase = charlestonQueue.removeFirst()
        } else if (round == 1 && rules.secondCharleston) {
            phase = Phase.Vote
        } else {
            afterCharleston()
        }
    }

    private fun onVote(seat: Int, action: Action): String? {
        val v = action as? Action.Vote ?: return "Vote yes or no"
        pending[seat] = v
        if (pending.all { it != null }) {
            val go = pending.all { (it as Action.Vote).yes }
            pending.fill(null)
            events += GameEvent.SecondCharleston(go)
            if (go) {
                listOf(PassDir.LEFT, PassDir.ACROSS, PassDir.RIGHT)
                    .forEach { charlestonQueue += Phase.Charleston(2, it) }
                phase = charlestonQueue.removeFirst()
            } else {
                afterCharleston()
            }
        }
        return null
    }

    private fun afterCharleston() {
        if (rules.courtesyPass) phase = Phase.Courtesy else startPlay()
    }

    private fun onCourtesy(seat: Int, action: Action): String? {
        val pass = action as? Action.Pass ?: return "Choose 0 to 3 tiles"
        val tiles = tilesFor(seat, pass.tileIds) ?: return "Those tiles aren't in your hand"
        if (tiles.size > 3) return "Pass at most 3 tiles"
        if (tiles.any { it.isJoker }) return "Jokers can't be passed"
        pending[seat] = action
        if (pending.all { it != null }) {
            val chosen = (0..3).map { s -> tilesFor(s, (pending[s] as Action.Pass).tileIds)!! }
            val counts = IntArray(4)
            for ((a, b) in listOf(0 to 2, 1 to 3)) {
                val n = minOf(chosen[a].size, chosen[b].size)
                counts[a] = n; counts[b] = n
                val fromA = chosen[a].take(n)
                val fromB = chosen[b].take(n)
                hands[a].removeAll(fromA.toSet()); hands[b].removeAll(fromB.toSet())
                hands[a] += fromB; hands[b] += fromA
            }
            pending.fill(null)
            events += GameEvent.CourtesyDone(counts.toList())
            startPlay()
        }
        return null
    }

    private fun startPlay() {
        phase = Phase.Turn(0)
    }

    // ---- Play -------------------------------------------------------------------------------

    fun winningHand(seat: Int, extra: Tile? = null): HandInstance? {
        val c = countsOf(hands[seat])
        if (extra != null) c[extra.kind]++
        return card.findWin(c, melds(seat))
    }

    private fun onTurn(seat: Int, action: Action): String? = when (action) {
        is Action.Discard -> {
            val tile = hands[seat].firstOrNull { it.id == action.tileId }
            if (tile == null) {
                "That tile isn't in your hand"
            } else {
                hands[seat].remove(tile)
                discards += DiscardRecord(seat, tile)
                events += GameEvent.Discarded(seat, tile)
                val eligible = (1..3).map { (seat + it) % 4 }.filter { !dead[it] }
                if (tile.isJoker || eligible.isEmpty()) advanceFrom(seat)
                else phase = Phase.Calling(seat, tile, eligible)
                null
            }
        }
        Action.Mahjong -> {
            if (dead[seat]) "Your hand is dead"
            else if (winningHand(seat) == null) "That isn't a winning hand yet"
            else { finish(seat, null); null }
        }
        is Action.SwapJoker -> swapJoker(seat, action)
        else -> "Discard a tile"
    }

    private fun swapJoker(seat: Int, a: Action.SwapJoker): String? {
        val exp = exposures.getOrNull(a.owner)?.getOrNull(a.exposureIndex) ?: return "No such exposure"
        val tile = hands[seat].firstOrNull { it.id == a.tileId } ?: return "That tile isn't in your hand"
        if (tile.kind != exp.kind) return "That tile doesn't match the exposure"
        val joker = exp.tiles.firstOrNull { it.isJoker } ?: return "No joker there"
        exp.tiles[exp.tiles.indexOf(joker)] = tile
        hands[seat].remove(tile)
        hands[seat] += joker
        events += GameEvent.JokerSwapped(seat, a.owner, exp.kind)
        return null
    }

    private fun onCalling(seat: Int, action: Action, p: Phase.Calling): String? {
        when (action) {
            Action.Decline -> pending[seat] = action
            Action.Mahjong -> {
                if (winningHand(seat, p.tile) == null) return "That tile doesn't complete a hand"
                pending[seat] = action
            }
            is Action.Expose -> {
                val tiles = tilesFor(seat, action.tileIds) ?: return "Those tiles aren't in your hand"
                val size = tiles.size + 1
                if (size < 3) return "You can only call a discard for a pung or bigger (or Mahjong)"
                if (size > 6) return "Too many tiles"
                if (tiles.any { !it.isJoker && it.kind != p.tile.kind }) return "Tiles must match the discard"
                if (!card.compatible(melds(seat) + Meld(p.tile.kind, size)))
                    return "No hand on the card uses that exposure"
                pending[seat] = action
            }
            else -> return "Call or pass"
        }
        if (p.eligible.all { pending[it] != null }) resolveCalls(p)
        return null
    }

    private fun resolveCalls(p: Phase.Calling) {
        val order = (1..3).map { (p.discarder + it) % 4 }
        val responses = order.associateWith { pending[it] }
        pending.fill(null)
        val winner = order.firstOrNull { responses[it] == Action.Mahjong }
        if (winner != null) {
            discards.removeAt(discards.lastIndex)
            hands[winner] += p.tile
            finish(winner, p.discarder)
            return
        }
        val caller = order.firstOrNull { responses[it] is Action.Expose }
        if (caller != null) {
            val ids = (responses[caller] as Action.Expose).tileIds
            val tiles = tilesFor(caller, ids)!!
            discards.removeAt(discards.lastIndex)
            hands[caller].removeAll(tiles.toSet())
            val exp = Exposure(p.tile.kind, (tiles + p.tile).toMutableList())
            exposures[caller] += exp
            events += GameEvent.Exposed(caller, p.discarder, exp.meld)
            if (!card.compatible(melds(caller))) dead[caller] = true
            phase = Phase.Turn(caller)
            return
        }
        advanceFrom(p.discarder)
    }

    private fun advanceFrom(seat: Int) {
        val next = (seat + 1) % 4
        if (wall.isEmpty()) {
            finish(null, null)
            return
        }
        hands[next] += wall.removeFirst()
        events += GameEvent.Drew(next)
        phase = Phase.Turn(next)
    }

    private fun finish(winner: Int?, discarder: Int?) {
        val pay = IntArray(4)
        var hand: CardHand? = null
        var jokerless = false
        if (winner != null) {
            val inst = winningHand(winner)!!
            hand = inst.hand
            jokerless = hand.usesJokers &&
                hands[winner].none { it.isJoker } && exposures[winner].all { it.jokerCount == 0 }
            val value = hand.points * if (jokerless && rules.jokerlessBonus) 2 else 1
            for (s in 0..3) if (s != winner) {
                val owe = if (discarder == null || s == discarder) 2 * value else value
                pay[s] -= owe
                pay[winner] += owe
            }
        }
        val result = GameResult(winner, discarder, hand, jokerless, pay)
        events += GameEvent.Finished(result)
        phase = Phase.Over(result)
    }

    /** Total tiles across wall, hands, exposures, and discards. Always 152. */
    fun tileTotal() = wall.size + hands.sumOf { it.size } +
        exposures.sumOf { e -> e.sumOf { it.tiles.size } } + discards.size
}
