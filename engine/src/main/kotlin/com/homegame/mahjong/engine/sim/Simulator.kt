package com.homegame.mahjong.engine.sim

import com.homegame.mahjong.engine.Bot
import com.homegame.mahjong.engine.Card
import com.homegame.mahjong.engine.Difficulty
import com.homegame.mahjong.engine.Game
import com.homegame.mahjong.engine.GameResult
import com.homegame.mahjong.engine.HeuristicBot
import com.homegame.mahjong.engine.Phase
import com.homegame.mahjong.engine.Rules

/** Plays one full game between bots. Throws if a bot makes an illegal move or the game stalls. */
fun playOut(card: Card, bots: List<Bot>, seed: Long, rules: Rules = Rules()): Game {
    val game = Game(card, rules, seed)
    var steps = 0
    while (game.phase !is Phase.Over) {
        for (s in game.awaiting()) {
            if (s !in game.awaiting()) continue
            val action = bots[s].decide(game, s)
            val err = game.act(s, action)
            check(err == null) { "Seat $s ($action) refused in ${game.phase}: $err" }
        }
        check(++steps < 5000) { "Game stalled in ${game.phase}" }
    }
    return game
}

/**
 * Bot-vs-bot tournament. Each game seats one bot of every difficulty plus a second
 * Intermediate, rotating seats so nobody keeps the East advantage.
 * Usage: gradlew :engine:run --args="2000"
 */
fun main(args: Array<String>) {
    val games = args.firstOrNull()?.toIntOrNull() ?: 1000
    val card = Card.practice()
    val lineup = listOf(Difficulty.BEGINNER, Difficulty.INTERMEDIATE, Difficulty.ADVANCED, Difficulty.INTERMEDIATE)
    val wins = HashMap<Difficulty, Int>()
    val seats = HashMap<Difficulty, Int>()
    val points = HashMap<Difficulty, Long>()
    val handsWon = HashMap<String, Int>()
    var wallGames = 0
    var selfDraws = 0
    val start = System.currentTimeMillis()

    for (g in 0 until games) {
        val seating = List(4) { lineup[(it + g) % 4] }
        val bots = seating.mapIndexed { i, d -> HeuristicBot(d, seed = g * 31L + i) }
        val result: GameResult = (playOut(card, bots, seed = g.toLong()).phase as Phase.Over).result
        seating.forEachIndexed { i, d ->
            seats.merge(d, 1, Int::plus)
            points.merge(d, result.payments[i].toLong(), Long::plus)
        }
        if (result.wallGame) wallGames++ else {
            wins.merge(seating[result.winner!!], 1, Int::plus)
            handsWon.merge(result.hand!!.family, 1, Int::plus)
            if (result.selfDrawn) selfDraws++
        }
    }

    val secs = (System.currentTimeMillis() - start) / 1000.0
    println("Played $games games in ${"%.1f".format(secs)}s")
    println("Wall games: ${pct(wallGames, games)}   Self-drawn wins: ${pct(selfDraws, games - wallGames)}")
    println()
    println("%-13s %8s %10s %12s".format("Bot", "Seats", "Win rate", "Avg points"))
    for (d in Difficulty.entries) {
        val n = seats[d] ?: 0
        println(
            "%-13s %8d %10s %12.1f".format(
                d.label, n, pct(wins[d] ?: 0, n), (points[d] ?: 0L).toDouble() / maxOf(1, n)
            )
        )
    }
    println()
    println("Winning hands by family:")
    handsWon.entries.sortedByDescending { it.value }.forEach { (f, n) -> println("  %-16s %d".format(f, n)) }
}

private fun pct(a: Int, b: Int) = if (b == 0) "-" else "%.1f%%".format(100.0 * a / b)
