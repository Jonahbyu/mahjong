package com.homegame.mahjong

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.homegame.mahjong.career.CareerStore
import com.homegame.mahjong.cosmetics.Reward
import com.homegame.mahjong.cosmetics.Wallet
import androidx.lifecycle.viewModelScope
import com.homegame.mahjong.engine.Action
import com.homegame.mahjong.engine.Bot
import com.homegame.mahjong.engine.Card
import com.homegame.mahjong.engine.Difficulty
import com.homegame.mahjong.engine.DiscardRecord
import com.homegame.mahjong.engine.Evaluation
import com.homegame.mahjong.engine.Game
import com.homegame.mahjong.engine.GameEvent
import com.homegame.mahjong.engine.GroupProgress
import com.homegame.mahjong.engine.HeuristicBot
import com.homegame.mahjong.engine.Kinds
import com.homegame.mahjong.engine.Meld
import com.homegame.mahjong.engine.Phase
import com.homegame.mahjong.engine.Rules
import com.homegame.mahjong.engine.SEAT_NAMES
import com.homegame.mahjong.engine.Tile
import com.homegame.mahjong.engine.countsOf
import com.homegame.mahjong.engine.career.Career
import com.homegame.mahjong.engine.career.Circuits
import com.homegame.mahjong.engine.career.HANDS_PER_SESSION
import com.homegame.mahjong.engine.career.Rivals
import com.homegame.mahjong.engine.career.RunStatus
import com.homegame.mahjong.engine.career.SessionResult
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

const val HUMAN = 0

/** What leaving a circuit hand early costs, paid to each rival. */
const val FORFEIT = 25

enum class SortMode(val label: String) { SUIT("Suit"), NUMBER("Number") }

enum class BotSpeed(val label: String, val turnMs: Long) { RELAXED("Relaxed", 1100), NORMAL("Normal", 650), FAST("Fast", 250) }

data class OpponentUi(
    val seat: Int,
    val name: String,
    val level: Difficulty,
    /** Name that fits the narrow side seats. */
    val short: String = name,
    /** Shown under the name: a rival's title, else the difficulty. */
    val subtitle: String = level.label,
    val concealed: Int,
    val exposures: List<List<Tile>>,
    val dead: Boolean,
    val active: Boolean,
)

data class ExposeOption(val size: Int, val tileIds: List<Int>, val jokers: Int) {
    val label get() = when (size) { 3 -> "Pung"; 4 -> "Kong"; 5 -> "Quint"; else -> "Sextet" }
}

data class CallPrompt(val from: Int, val tile: Tile, val canMahjong: Boolean, val options: List<ExposeOption>)

data class SwapOption(val owner: Int, val exposureIndex: Int, val tile: Tile)

data class HintUi(
    val handId: String,
    val family: String,
    val pattern: String,
    val points: Int,
    val concealed: Boolean,
    val missing: Int,
    val groups: List<GroupProgress>,
)

/** The hand the player picked on the card. [hint] is null when it can't be made any more. */
data class TargetUi(val handId: String, val family: String, val pattern: String, val hint: HintUi?)

data class TableUi(
    val phase: Phase,
    val hand: List<Tile>,
    val myExposures: List<List<Tile>>,
    val opponents: List<OpponentUi>,
    val discards: List<DiscardRecord>,
    val wall: Int,
    val drawnId: Int?,
    val canMahjong: Boolean,
    val dead: Boolean,
    val call: CallPrompt?,
    val swaps: List<SwapOption>,
    val hints: List<HintUi>,
    val target: TargetUi? = null,
    val status: String,
    /** Manual draw is on and it's the player's turn: she has to tap the wall first. */
    val mustDraw: Boolean = false,
)

class TableViewModel(app: Application) : AndroidViewModel(app) {
    val wallet = Wallet(app)
    /** Coins paid out for the game that just ended. */
    var lastReward by mutableStateOf<Reward?>(null)
        private set

    val card: Card = Card.practice()

    var ui by mutableStateOf<TableUi?>(null)
        private set
    var selected by mutableStateOf(setOf<Int>())
        private set
    var sortMode by mutableStateOf(SortMode.SUIT)
        private set
    private val settings = app.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    var speed by mutableStateOf(BotSpeed.NORMAL)
    /** When on, the player taps the wall to draw instead of the tile arriving on its own. */
    var manualDraw by mutableStateOf(settings.getBoolean("manualDraw", false))
        private set
    fun chooseManualDraw(on: Boolean) {
        manualDraw = on
        settings.edit().putBoolean("manualDraw", on).apply()
    }

    init { com.homegame.mahjong.ui.Motion.enabled = settings.getBoolean("motion", true) }
    /** Animated table and tile effects. */
    val motionOn get() = com.homegame.mahjong.ui.Motion.enabled
    fun chooseMotion(on: Boolean) {
        com.homegame.mahjong.ui.Motion.enabled = on
        settings.edit().putBoolean("motion", on).apply()
    }
    val music = Music(app).also { it.volume = settings.getFloat("musicVolume", 0.7f) }
    var musicVolume by mutableStateOf(music.volume)
        private set
    fun chooseMusicVolume(v: Float) {
        musicVolume = v
        music.volume = v
        settings.edit().putFloat("musicVolume", v).apply()
    }

    override fun onCleared() { music.release() }

    // The engine draws for her at the start of her turn; with manual draw we keep that tile out of
    // sight until she taps the wall. The wall is shuffled up front, so it's the same tile either way.
    private var heldDrawId: Int? = null
    private var revealedDrawId: Int? = null
    var showHints by mutableStateOf(true)
    /** Card hand the player is aiming for (by hand id), picked from the card sheet. */
    var targetHandId by mutableStateOf<String?>(null)
        private set
    fun chooseTarget(handId: String?) {
        targetHandId = handId
        refresh()
    }
    var levels by mutableStateOf(List(3) { Difficulty.INTERMEDIATE })
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    /** Running score across games this session, per seat. */
    var session by mutableStateOf(IntArray(4))
        private set
    var gamesPlayed by mutableStateOf(0)
        private set

    /** House rules for the game in progress (and for "Play again"). */
    var rules by mutableStateOf(Rules())
        private set

    // The Custom table: each bot's level plus house rules. Saved so it's still set up next time.
    private var savedCustomLevels by mutableStateOf(
        (1..3).map { s -> Difficulty.entries.getOrElse(settings.getInt("customLevel$s", 1)) { Difficulty.INTERMEDIATE } }
    )
    /** Levels the player can pick right now: Expert only once it's unlocked. */
    val availableLevels get() = Difficulty.entries.filter { it != Difficulty.EXPERT || wallet.expertUnlocked }
    /** Saved custom levels, with Expert stepped down to Advanced while it's locked. */
    val customLevels get() = savedCustomLevels.map { if (it in availableLevels) it else Difficulty.ADVANCED }
    /** True on the game-over screen of the win that unlocked Expert. */
    var expertJustUnlocked by mutableStateOf(false)
        private set
    var customRules by mutableStateOf(
        Rules(
            secondCharleston = settings.getBoolean("customSecondCharleston", true),
            courtesyPass = settings.getBoolean("customCourtesy", true),
            jokerlessBonus = settings.getBoolean("customJokerless", true),
        )
    )
        private set

    fun chooseCustomLevel(index: Int, level: Difficulty) {
        savedCustomLevels = customLevels.toMutableList().also { it[index] = level }
        settings.edit().putInt("customLevel${index + 1}", level.ordinal).apply()
    }

    fun chooseCustomRules(r: Rules) {
        customRules = r
        settings.edit()
            .putBoolean("customSecondCharleston", r.secondCharleston)
            .putBoolean("customCourtesy", r.courtesyPass)
            .putBoolean("customJokerless", r.jokerlessBonus)
            .apply()
    }

    private var game: Game? = null
    private var bots: List<Bot?> = emptyList()
    private val mutex = Mutex()
    private var botJob: Job? = null

    fun newGame(opponents: List<Difficulty> = levels, houseRules: Rules = rules) {
        circuitMode = false
        seatNames = SEAT_NAMES
        seatTitles = List(4) { null }
        start(opponents.mapIndexed { i, d -> HeuristicBot(d, seed = System.nanoTime() + i) }, opponents, houseRules)
    }

    private fun start(opponentBots: List<Bot>, opponents: List<Difficulty>, houseRules: Rules) {
        levels = opponents
        rules = houseRules
        botJob?.cancel()
        game = Game(card, houseRules)
        bots = listOf(null) + opponentBots
        sessionResult = null
        circuitWon = null
        leaveArmed = false
        selected = emptySet()
        lastReward = null
        expertJustUnlocked = false
        heldDrawId = null
        revealedDrawId = null
        notice = null
        targetHandId = null
        refresh()
        runBots()
    }

    private var leaveArmed = false

    /** Menu or Back at the table. Mid-hand in a circuit, the first press only warns about the forfeit. */
    fun requestLeave() {
        val g = game
        if (circuitMode && g != null && g.phase !is Phase.Over && !leaveArmed) {
            leaveArmed = true
            notice = "Leaving forfeits this hand: you pay $FORFEIT to each rival. Press again to leave."
            return
        }
        leaveTable()
    }

    fun leaveTable() {
        leaveArmed = false
        botJob?.cancel()
        val g = game
        // Walking away from a circuit hand mid-play forfeits it: you pay each rival 25. A free wall game
        // would let you skip bad hands, or skip a whole session into a four-way tie for first.
        if (circuitMode && g != null && g.phase !is Phase.Over) {
            career.run?.takeIf { !it.over && !it.sessionDone }?.let { run ->
                run.recordHand(intArrayOf(-3 * FORFEIT, FORFEIT, FORFEIT, FORFEIT))
                if (run.sessionDone) finishCircuitSession()
                saveCareer()
            }
        }
        if (circuitMode) openHub = true
        game = null
        ui = null
    }

    // ---- Circuits ---------------------------------------------------------------------------

    private val careerStore = CareerStore(app)
    var career: Career = careerStore.load()
        private set
    /** Bumped on every career change; screens read it so they redraw. */
    var careerTick by mutableIntStateOf(0)
        private set
    /** True while the table is a circuit session. */
    var circuitMode by mutableStateOf(false)
        private set
    /** Set when leaving a circuit table, so the menu opens on the circuit hub. */
    var openHub by mutableStateOf(false)
    /** The session that just ended (on the last hand's game-over card). */
    var sessionResult by mutableStateOf<SessionResult?>(null)
        private set
    /** Coins from session placement and any circuit prize, shown with [sessionResult]. */
    var sessionCoins by mutableStateOf(0)
        private set
    /** Index of a circuit won for the first time by the session that just ended. */
    var circuitWon by mutableStateOf<Int?>(null)
        private set
    /** Session scores by seat as of the last finished hand. */
    var handSession by mutableStateOf(IntArray(4))
        private set
    private var seatNames: List<String> = SEAT_NAMES
    private var seatTitles: List<String?> = List(4) { null }

    fun nameOf(seat: Int): String = if (seat == HUMAN) "You" else seatNames[seat]

    /** Hints are off at the top circuits whatever the setting says. */
    val hintsAllowed get() = !(circuitMode && career.run?.circuit?.hints == false)
    val hintsOn get() = showHints && hintsAllowed

    private fun saveCareer() {
        careerStore.save(career)
        careerTick++
    }

    fun enterCircuit(index: Int) {
        career.start(index)
        saveCareer()
    }

    fun retryFinal() {
        career.run?.retryFinal()
        saveCareer()
    }

    /** Testing: wipes circuit progress. */
    fun resetCareer() {
        careerStore.reset()
        career = Career()
        careerTick++
    }

    /** Testing: opens every circuit. */
    fun unlockAllCircuits() {
        career.unlocked = Circuits.all.size
        saveCareer()
    }

    /** Deals the next hand of the current circuit session. */
    fun playCircuitHand() {
        val run = career.run ?: return
        if (run.over) return
        val seats = run.seats()
        val c = run.circuit
        val rng = kotlin.random.Random(System.nanoTime())
        val lead = run.sessionScores.max()
        val rivals = seats.drop(1).map { Rivals.byId(it) }
        val opponentBots = rivals.mapIndexed { i, r ->
            HeuristicBot(r.profile(c.level(r.id), rng), seed = rng.nextLong()).also {
                it.pressure = ((lead - run.sessionScores[i + 1]) / 200.0).coerceIn(0.0, 1.0)
            }
        }
        circuitMode = true
        seatNames = listOf("You") + rivals.map { it.name }
        seatTitles = listOf(null) + rivals.map { it.title }
        handSession = run.sessionScores.copyOf()
        start(opponentBots, rivals.map { levelBand(c.level(it.id)) }, c.rules)
    }

    private fun levelBand(level: Int) = when {
        level <= 3 -> Difficulty.BEGINNER
        level <= 7 -> Difficulty.INTERMEDIATE
        level <= 11 -> Difficulty.ADVANCED
        else -> Difficulty.EXPERT
    }

    private fun onCircuitHandOver(payments: IntArray) {
        val run = career.run ?: return
        if (run.over || run.sessionDone) return
        run.recordHand(payments)
        handSession = run.sessionScores.copyOf()
        if (run.sessionDone) finishCircuitSession()
        saveCareer()
    }

    private fun finishCircuitSession() {
        val run = career.run ?: return
        val (result, firstWin) = career.finishSession()
        var coins = run.circuit.placementCoins(result.yourPlace)
        if (run.status == RunStatus.WON) coins += if (firstWin != null) run.circuit.prize else run.circuit.prize / 4
        wallet.grant(coins)
        // Winning the Regional Open opens Expert for quick play too.
        if (firstWin == 11) wallet.grantExpert()
        sessionResult = result
        sessionCoins = coins
        circuitWon = firstWin
    }

    /** Hand number (1-based) of the hand on the table in the current circuit session. */
    val handNumber get() = career.run?.let { if (sessionResult != null) HANDS_PER_SESSION else it.hand + (if (ui?.phase is Phase.Over) 0 else 1) } ?: 0

    // ---- Human input ------------------------------------------------------------------------

    fun drawFromWall() {
        revealedDrawId = heldDrawId ?: return
        heldDrawId = null
        refresh()
    }

    fun tapTile(tile: Tile) {
        val phase = ui?.phase ?: return
        notice = null
        leaveArmed = false
        if (heldDrawId != null) { notice = "Draw from the wall first"; return }
        when (phase) {
            is Phase.Charleston, Phase.Courtesy -> {
                if (tile.isJoker) { notice = "Jokers can't be passed"; return }
                selected = when {
                    tile.id in selected -> selected - tile.id
                    selected.size < 3 -> selected + tile.id
                    else -> { notice = "You've already picked 3"; selected }
                }
            }
            is Phase.Turn -> if (phase.seat == HUMAN) {
                if (tile.id in selected) discard() else selected = setOf(tile.id)
            }
            else -> {}
        }
    }

    fun toggleSort() {
        sortMode = if (sortMode == SortMode.SUIT) SortMode.NUMBER else SortMode.SUIT
        refresh()
    }

    fun pass() = human(Action.Pass(selected.toList()))
    fun vote(yes: Boolean) = human(Action.Vote(yes))
    fun discard() { if (heldDrawId != null) return; selected.singleOrNull()?.let { human(Action.Discard(it)) } }
    fun mahjong() = human(Action.Mahjong)
    fun declineCall() = human(Action.Decline)
    fun expose(option: ExposeOption) = human(Action.Expose(option.tileIds))
    fun swap(option: SwapOption) = human(Action.SwapJoker(option.owner, option.exposureIndex, option.tile.id))

    private fun human(action: Action) {
        val g = game ?: return
        viewModelScope.launch {
            val err = mutex.withLock { g.act(HUMAN, action) }
            if (err != null) {
                notice = err
                return@launch
            }
            selected = emptySet()
            refresh()
            runBots()
        }
    }

    // ---- Bots -------------------------------------------------------------------------------

    private fun runBots() {
        if (botJob?.isActive == true) return
        val g = game ?: return
        botJob = viewModelScope.launch {
            while (true) {
                mutex.withLock { autoDeclineIfPointless(g) }
                val seat = mutex.withLock { g.awaiting().firstOrNull { it != HUMAN } } ?: break
                if (g.phase is Phase.Turn) delay(speed.turnMs)
                val action = withContext(Dispatchers.Default) {
                    mutex.withLock { bots[seat]!!.decide(g, seat) }
                }
                mutex.withLock {
                    val err = g.act(seat, action)
                    check(err == null) { "Bot $seat refused: $err" }
                }
                refresh()
            }
            refresh()
        }
    }

    /** Don't stop the game to ask about a discard the player can't use. */
    private fun autoDeclineIfPointless(g: Game) {
        val p = g.phase as? Phase.Calling ?: return
        if (HUMAN !in g.awaiting()) return
        if (callPrompt(g, p) == null) g.act(HUMAN, Action.Decline)
    }

    private fun callPrompt(g: Game, p: Phase.Calling): CallPrompt? {
        val canWin = g.winningHand(HUMAN, p.tile) != null
        val hand = g.hands[HUMAN]
        val real = hand.filter { it.kind == p.tile.kind }
        val jokers = hand.filter { it.isJoker }
        val melds = g.melds(HUMAN)
        val counts = countsOf(hand)
        // Only offer calls that bring the hand closer than just drawing would. Holding a few jokers
        // makes almost any discard callable, and asking every time is exactly the nagging we want to avoid.
        val current = card.bestMissing(counts, melds)
        val options = mutableListOf<ExposeOption>()
        for (size in 3..6) {
            val need = size - 1
            if (real.size + jokers.size < need) break
            val useReal = minOf(real.size, need)
            val after = counts.copyOf()
            after[p.tile.kind] -= useReal
            after[Kinds.JOKER] -= need - useReal
            if (card.bestMissing(after, melds + Meld(p.tile.kind, size)) >= current) continue
            options += ExposeOption(size, real.take(useReal).map { it.id } + jokers.take(need - useReal).map { it.id }, need - useReal)
        }
        if (!canWin && options.isEmpty()) return null
        return CallPrompt(p.discarder, p.tile, canWin, options)
    }

    /**
     * For the card sheet: tiles still needed for each card hand (by hand id) given the player's
     * tiles and exposures. Hands missing from the map can't be made any more.
     */
    fun handDistances(): Map<String, Int> {
        val g = game ?: return emptyMap()
        return card.ranked(countsOf(g.hands[HUMAN].filter { it.id != heldDrawId }), g.melds(HUMAN), limit = Int.MAX_VALUE)
            .associate { it.instance.hand.id to it.missing }
    }

    // ---- Snapshot ---------------------------------------------------------------------------

    private fun sortKey(t: Tile): Int = when (sortMode) {
        SortMode.SUIT -> t.kind
        SortMode.NUMBER -> if (Kinds.isSuited(t.kind)) Kinds.rankOf(t.kind) * 3 + Kinds.suitOf(t.kind).ordinal else 100 + t.kind
    }

    fun refresh() {
        val g = game ?: return
        val phase = g.phase
        val myTurn = phase is Phase.Turn && phase.seat == HUMAN
        val justDrew = if (myTurn && g.events.lastOrNull() == GameEvent.Drew(HUMAN)) g.hands[HUMAN].last() else null
        heldDrawId = when {
            justDrew == null -> null
            manualDraw && justDrew.id != revealedDrawId -> justDrew.id
            justDrew.id == heldDrawId -> justDrew.id
            else -> null
        }
        val held = heldDrawId
        val visible = g.hands[HUMAN].filter { it.id != held }
        val hand = visible.sortedWith(compareBy({ sortKey(it) }, { it.id }))
        val drawnId = if (held == null) justDrew?.id else null
        val counts = countsOf(visible)
        val melds = g.melds(HUMAN)

        val swaps = if (!myTurn || held != null) emptyList() else buildList {
            for (owner in 0..3) for ((i, e) in g.exposures[owner].withIndex()) {
                if (e.jokerCount == 0) continue
                g.hands[HUMAN].firstOrNull { it.kind == e.kind }?.let { add(SwapOption(owner, i, it)) }
            }
        }

        fun hintOf(e: Evaluation) = e.instance.hand.let { h ->
            HintUi(h.id, h.family, h.pattern, h.points, h.concealed, e.missing, card.breakdown(e.instance, counts, melds))
        }
        val hints = card.ranked(counts, melds, limit = 3).map(::hintOf)
        val target = targetHandId?.let { id ->
            card.hands.firstOrNull { it.id == id }?.let { h ->
                TargetUi(h.id, h.family, h.pattern, card.bestFor(id, counts, melds)?.let(::hintOf))
            }
        }

        if (phase is Phase.Over && ui?.phase !is Phase.Over) {
            if (circuitMode) onCircuitHandOver(phase.result.payments)
            else session = IntArray(4) { session[it] + phase.result.payments[it] }
            gamesPlayed++
            lastReward = wallet.payOut(phase.result.winner == HUMAN, phase.result.jokerless, levels)
            expertJustUnlocked = wallet.checkExpertUnlock(phase.result.winner == HUMAN, levels)
        }

        ui = TableUi(
            phase = phase,
            hand = hand,
            myExposures = g.exposures[HUMAN].map { it.tiles.toList() },
            opponents = (1..3).map { s ->
                OpponentUi(
                    seat = s, name = seatNames[s], level = levels[s - 1],
                    short = seatNames[s].substringAfterLast(' '),
                    subtitle = seatTitles[s] ?: levels[s - 1].label,
                    concealed = g.hands[s].size,
                    exposures = g.exposures[s].map { it.tiles.toList() },
                    dead = g.dead[s],
                    active = (phase is Phase.Turn && phase.seat == s) || (phase is Phase.Calling && phase.discarder == s),
                )
            },
            discards = g.discards.toList(),
            wall = g.wall.size,
            drawnId = drawnId,
            canMahjong = myTurn && held == null && !g.dead[HUMAN] && g.winningHand(HUMAN) != null,
            dead = g.dead[HUMAN],
            call = (phase as? Phase.Calling)?.takeIf { HUMAN in g.awaiting() }?.let { callPrompt(g, it) },
            swaps = swaps,
            hints = hints,
            target = target,
            status = if (held != null) "Your turn: tap the wall to draw" else status(g, phase),
            mustDraw = held != null,
        )
    }

    private fun status(g: Game, phase: Phase): String = when (phase) {
        is Phase.Charleston -> {
            val round = if (phase.round == 1) "First" else "Second"
            "$round Charleston: pick 3 tiles to pass ${phase.dir.label.lowercase()}"
        }
        Phase.Vote -> "Do another Charleston?"
        Phase.Courtesy -> "Courtesy pass: pick 0–3 tiles to trade with West"
        is Phase.Turn -> if (phase.seat == HUMAN) {
            if (g.dead[HUMAN]) "Your hand is dead. Keep discarding." else "Your turn: tap a tile, tap again to discard"
        } else lastEventText(g) ?: "${nameOf(phase.seat)} is thinking…"
        is Phase.Calling -> "${nameOf(phase.discarder)} discarded ${Kinds.name(phase.tile.kind)}"
        is Phase.Over -> when (val w = phase.result.winner) {
            null -> "Wall game: nobody wins"
            HUMAN -> "Mahjong! You win"
            else -> "${nameOf(w)} wins"
        }
    }

    private fun lastEventText(g: Game): String? = when (val e = g.events.lastOrNull()) {
        is GameEvent.Exposed -> "${who(e.seat)} called ${Kinds.name(e.meld.kind)} from ${who(e.from)}"
        is GameEvent.JokerSwapped -> "${who(e.seat)} swapped for a joker"
        is GameEvent.Discarded -> "${who(e.seat)} discarded ${Kinds.name(e.tile.kind)}"
        else -> null
    }

    private fun who(seat: Int) = nameOf(seat)
}
