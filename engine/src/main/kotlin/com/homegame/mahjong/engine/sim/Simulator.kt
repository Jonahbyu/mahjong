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
 * Bot-vs-bot tournament. Each game seats one bot of every difficulty, rotating seats so
 * nobody keeps the East advantage. A second arg picks the table, e.g. "AAAE" for three
 * Advanced and one Expert.
 * Usage: gradlew :engine:run --args="2000"  or  --args="2000 AAAE"
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "ladder" -> return ladder(args.getOrNull(1)?.toIntOrNull() ?: 300)
        "rivals" -> return rivals(args.getOrNull(1)?.toIntOrNull() ?: 300, args.getOrNull(2)?.toIntOrNull() ?: 8)
    }
    val games = args.firstOrNull()?.toIntOrNull() ?: 1000
    val card = Card.practice()
    val lineup = args.getOrNull(1)?.map { c -> Difficulty.entries.first { it.label.first() == c } }
        ?: Difficulty.entries.toList()
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

/** One bot against three fixed opponents, rotating seats. Returns (win rate, avg points). */
private fun versus(card: Card, hero: () -> Bot, field: () -> Bot, games: Int, salt: Long): Pair<Double, Double> {
    var wins = 0
    var pts = 0L
    for (g in 0 until games) {
        val heroSeat = g % 4
        val bots = List(4) { if (it == heroSeat) hero() else field() }
        val r = (playOut(card, bots, seed = salt * 100_003 + g).phase as Phase.Over).result
        if (r.winner == heroSeat) wins++
        pts += r.payments[heroSeat]
    }
    return wins.toDouble() / games to pts.toDouble() / games
}

/** Circuit skill levels 1..18 against three Intermediate bots. Should climb steadily. */
private fun ladder(games: Int) {
    val card = Card.practice()
    println("Level   Win rate   Avg points   (vs 3 Intermediate, $games games each)")
    var salt = 1L
    for (l in 1..com.homegame.mahjong.engine.career.MAX_LEVEL) {
        val (w, p) = versus(card, { HeuristicBot(com.homegame.mahjong.engine.career.skill(l), salt++) },
            { HeuristicBot(Difficulty.INTERMEDIATE, salt++) }, games, l.toLong())
        println("%5d   %7.1f%%   %10.1f".format(l, 100 * w, p))
    }
}

/** Every rival at [level] against three plain bots of the same level. Style should cost little. */
private fun rivals(games: Int, level: Int) {
    val card = Card.practice()
    println("Rival           Win rate   Avg points   (level $level vs 3 plain level-$level bots, $games games)")
    var salt = 1L
    val (bw, bp) = versus(card, { HeuristicBot(com.homegame.mahjong.engine.career.skill(level), salt++) },
        { HeuristicBot(com.homegame.mahjong.engine.career.skill(level), salt++) }, games, 7)
    println("%-15s %7.1f%%   %10.1f".format("(plain)", 100 * bw, bp))
    for (r in com.homegame.mahjong.engine.career.Rivals.all) {
        val rng = kotlin.random.Random(salt)
        val (w, p) = versus(card, { HeuristicBot(r.profile(level, rng), salt++) },
            { HeuristicBot(com.homegame.mahjong.engine.career.skill(level), salt++) }, games, 7)
        println("%-15s %7.1f%%   %10.1f".format(r.name, 100 * w, p))
    }
}
