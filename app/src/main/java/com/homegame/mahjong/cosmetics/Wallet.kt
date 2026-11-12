package com.homegame.mahjong.cosmetics

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.homegame.mahjong.engine.Difficulty
import com.homegame.mahjong.ui.ActiveUi
import java.time.LocalDate

/** One line of a post-game coin payout, e.g. "Win vs Advanced +80". */
data class RewardLine(val label: String, val coins: Int)

data class Reward(val lines: List<RewardLine>) {
    val total get() = lines.sumOf { it.coins }
}

/** Coins, unlocked tile sets and table styles, and what's equipped. Saved on the phone; no account. */
class Wallet(context: Context) {
    private val prefs = context.getSharedPreferences("wallet", Context.MODE_PRIVATE)

    var coins by mutableIntStateOf(prefs.getInt(KEY_COINS, STARTING_COINS))
        private set
    var owned by mutableStateOf(prefs.getStringSet(KEY_OWNED, null)?.toSet() ?: TileSets.starterIds)
        private set
    var equippedId by mutableStateOf(prefs.getString(KEY_EQUIPPED, null) ?: TileSets.all.first().id)
        private set

    var ownedUi by mutableStateOf(prefs.getStringSet(KEY_UI_OWNED, null)?.toSet() ?: UiStyles.starterIds)
        private set
    var equippedUiId by mutableStateOf(prefs.getString(KEY_UI_EQUIPPED, null) ?: UiStyles.all.first().id)
        private set

    /** The secret Expert level. Earned by beating a table of three Advanced (or better) bots. */
    var expertUnlocked by mutableStateOf(prefs.getBoolean(KEY_EXPERT, false))
        private set

    /** Unlocks Expert if this game earns it. Returns true only the first time. */
    fun checkExpertUnlock(won: Boolean, opponents: List<Difficulty>): Boolean {
        if (expertUnlocked || !won || !opponents.all { it.atLeast(Difficulty.ADVANCED) }) return false
        expertUnlocked = true
        save()
        return true
    }

    /** Opens Expert without the usual win (a circuit prize). */
    fun grantExpert() {
        if (expertUnlocked) return
        expertUnlocked = true
        save()
    }

    /** Adds coins from outside a game's payout, e.g. circuit placement and prizes. */
    fun grant(amount: Int) {
        if (amount <= 0) return
        coins += amount
        save()
    }

    val equipped get() = TileSets.byId(equippedId)
    val equippedUi get() = UiStyles.byId(equippedUiId)

    init { ActiveUi.theme = equippedUi.theme }

    fun owns(style: UiStyle) = style.id in ownedUi

    /** Spends coins on a table style. Returns false if she can't afford it or already has it. */
    fun unlock(style: UiStyle): Boolean {
        if (owns(style) || coins < style.price) return false
        coins -= style.price
        ownedUi = ownedUi + style.id
        save()
        return true
    }

    fun equip(style: UiStyle) {
        if (!owns(style)) return
        equippedUiId = style.id
        ActiveUi.theme = style.theme
        save()
    }

    fun owns(set: TileSet) = set.id in owned

    /** Spends coins on a set. Returns false if she can't afford it or already has it. */
    fun unlock(set: TileSet): Boolean {
        if (owns(set) || coins < set.price) return false
        coins -= set.price
        owned = owned + set.id
        save()
        return true
    }

    fun equip(set: TileSet) {
        if (!owns(set)) return
        equippedId = set.id
        save()
    }

    /** Testing: owns every tile set and table style, and tops up coins for trying purchases later. */
    fun unlockEverything() {
        owned = TileSets.all.map { it.id }.toSet()
        ownedUi = UiStyles.all.map { it.id }.toSet()
        coins = maxOf(coins, 100_000)
        expertUnlocked = true
        save()
    }

    /** Testing: back to a fresh install's starter items and coins, with Expert locked again. */
    fun resetShop() {
        expertUnlocked = false
        owned = TileSets.starterIds
        ownedUi = UiStyles.starterIds
        coins = STARTING_COINS
        equippedId = TileSets.all.first().id
        equippedUiId = UiStyles.all.first().id
        ActiveUi.theme = equippedUi.theme
        save()
    }

    /** Pays out for a finished game and returns the breakdown to show on the results card. */
    fun payOut(won: Boolean, jokerless: Boolean, opponents: List<Difficulty>, today: LocalDate = LocalDate.now()): Reward {
        val lines = mutableListOf(RewardLine("Game played", PER_GAME))
        if (won) {
            val vs = opponents.sumOf { WIN_PER_BOT.getValue(it) }
            lines += RewardLine("Win vs ${opponentLabel(opponents)}", WIN_BASE + vs)
            if (jokerless) lines += RewardLine("Jokerless", JOKERLESS)
        }
        if (prefs.getString(KEY_DAILY, null) != today.toString()) {
            lines += RewardLine("First game today", DAILY)
            prefs.edit().putString(KEY_DAILY, today.toString()).apply()
        }
        val reward = Reward(lines)
        coins += reward.total
        save()
        return reward
    }

    private fun opponentLabel(levels: List<Difficulty>) =
        if (levels.distinct().size == 1) levels.first().label else "mixed table"

    private fun save() {
        prefs.edit()
            .putInt(KEY_COINS, coins)
            .putStringSet(KEY_OWNED, owned)
            .putString(KEY_EQUIPPED, equippedId)
            .putStringSet(KEY_UI_OWNED, ownedUi)
            .putString(KEY_UI_EQUIPPED, equippedUiId)
            .putBoolean(KEY_EXPERT, expertUnlocked)
            .apply()
    }

    companion object {
        const val STARTING_COINS = 150
        const val PER_GAME = 15
        const val WIN_BASE = 20
        const val JOKERLESS = 25
        const val DAILY = 50
        val WIN_PER_BOT = mapOf(Difficulty.BEGINNER to 5, Difficulty.INTERMEDIATE to 12, Difficulty.ADVANCED to 20, Difficulty.EXPERT to 35)

        private const val KEY_COINS = "coins"
        private const val KEY_OWNED = "owned"
        private const val KEY_EQUIPPED = "equipped"
        private const val KEY_DAILY = "daily"
        private const val KEY_UI_OWNED = "ui_owned"
        private const val KEY_UI_EQUIPPED = "ui_equipped"
        private const val KEY_EXPERT = "expert_unlocked"
    }
}
