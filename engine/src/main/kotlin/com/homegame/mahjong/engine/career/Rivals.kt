package com.homegame.mahjong.engine.career

import com.homegame.mahjong.engine.BotProfile
import kotlin.random.Random

/** Highest skill level a rival can play at (the last circuit's boss). */
const val MAX_LEVEL = 18

/**
 * Evaluation slop for levels 8..18. Tuned with `gradlew :engine:run --args="ladder 400"`.
 * Zero slop tested worse than a little (21% vs 30% wins against Intermediate), so the top
 * levels keep 0.03 and differ only by defense. Strength plateaus from about level 13.
 */
private val TOP_NOISE = listOf(0.35, 0.28, 0.22, 0.17, 0.13, 0.1, 0.07, 0.05, 0.03, 0.03, 0.03)

/**
 * Raw playing strength for a level, with no personality. Level 1 is sloppier than the Beginner
 * preset; level 4 is about Intermediate, 8 about Advanced, 12 and up use Expert's evaluation.
 */
fun skill(level: Int): BotProfile {
    val l = level.coerceIn(1, MAX_LEVEL)
    return when {
        l <= 3 -> BotProfile(
            topK = if (l < 3) 1 else 2, noise = 1.2 - 0.25 * (l - 1), trackLive = false, weightedEval = false,
            swapJokers = l >= 3, calls = l >= 3, callLimit = 3, courtesy = l >= 2,
            alwaysSecondCharleston = l < 3, defense = 0.0,
        )
        l <= 7 -> BotProfile(
            topK = if (l < 6) 3 else 4, noise = listOf(0.5, 0.35, 0.25, 0.15)[l - 4], trackLive = false,
            weightedEval = false, swapJokers = true, calls = true, callLimit = 4, courtesy = true,
            alwaysSecondCharleston = false, defense = 0.0,
        )
        // From here on rivals use Expert's evaluation; the plain one stops getting stronger past
        // Intermediate (the ladder sim showed Advanced-style levels no better than level 7).
        // Noise is what separates the top levels: Expert's evaluation with no slop is the ceiling.
        else -> BotProfile(
            topK = minOf(8, l - 2), noise = TOP_NOISE[l - 8], trackLive = true,
            weightedEval = true, swapJokers = true, calls = true, callLimit = 5, courtesy = true,
            alwaysSecondCharleston = false, defense = minOf(0.5, 0.2 + 0.03 * (l - 8)),
        )
    }
}

/**
 * A named opponent with one clear habit and one clear way to beat it. Their strength comes from
 * the circuit they're playing in; [style] bends that strength into their personality.
 */
class Rival(
    val id: String,
    val name: String,
    /** Two or three words, shown under the name. */
    val title: String,
    /** How they play, in a sentence. */
    val blurb: String,
    /** How to beat them, in a sentence. Shown once you've played them. */
    val tip: String,
    /** A tiny bit of attitude for the table. */
    val quip: String,
    private val style: BotProfile.() -> BotProfile = { this },
    /** Picks a different rival's style every hand. */
    val chameleon: Boolean = false,
) {
    fun profile(level: Int, rng: Random = Random): BotProfile {
        val base = skill(level)
        if (!chameleon) return base.style()
        val pick = Rivals.all.filter { !it.chameleon && it.id != "nora" }.random(rng)
        return pick.profile(level, rng)
    }
}

object Rivals {
    private fun fam(name: String, w: Double = 2.0): BotProfile.() -> BotProfile = { copy(familyPref = mapOf(name to w)) }

    val all: List<Rival> = listOf(
        Rival(
            "penny", "Penny", "Evens only",
            "Plays the 2468 family every chance she gets.",
            "Hang on to your 2s, 4s, 6s and 8s. Without them she stalls.",
            "Even is the only way.", fam("2468"),
        ),
        Rival(
            "gus", "Gus", "Calls everything",
            "Grabs almost any discard he can use and exposes early.",
            "His exposures show his hand. Don't throw him what matches them.",
            "Mine! I'll take that.", { copy(calls = true, callSlack = 1, callLimit = callLimit + 2) },
        ),
        Rival(
            "ida", "Ida", "Never calls",
            "Keeps her hand to herself and never calls a discard except to win.",
            "She's slow. Go for quick, cheap hands and win before she's ready.",
            "I'll wait, thank you.", { copy(calls = false, concealedPref = 0.5) },
        ),
        Rival(
            "bernie", "Bernie", "Winds & dragons",
            "Collects winds and dragons and builds honor hands.",
            "Keep your winds and dragons. Number hands are safe from him.",
            "North wind's blowing my way.", fam("Winds & Dragons"),
        ),
        Rival(
            "sunny", "Sunny", "Odd numbers",
            "Loves the 13579 family.",
            "Hold your odd numbers, and toss evens freely.",
            "Odds are in my favor.", fam("13579"),
        ),
        Rival(
            "rosa", "Rosa", "Like numbers",
            "Wants the same number in every suit.",
            "When she has three 5s showing, every other 5 is dangerous.",
            "Twins, triplets, why not quads?", fam("Like Numbers"),
        ),
        Rival(
            "theo", "Theo", "Runs",
            "Builds consecutive runs: 1-2-3, 4-5-6.",
            "Break his runs by holding the middle numbers.",
            "One, two, three, mahjong.", fam("Consecutive"),
        ),
        Rival(
            "mabel", "Mabel", "3-6-9",
            "Plays 369 hands whenever she can.",
            "Keep your 3s, 6s and 9s close.",
            "Three, six, nine, all mine.", fam("369"),
        ),
        Rival(
            "dot", "Dot", "Adds it up",
            "Plays Addition hands, where the numbers add up across the row.",
            "Watch for 3-4-7, 2-6-8 and 1-5-6 showing up together.",
            "It all adds up.", fam("Addition"),
        ),
        Rival(
            "hank", "Hank", "Tilts",
            "Plays fine while he's winning. Gets sloppy when he falls behind.",
            "Get an early lead and he'll start making mistakes.",
            "That's it, no more Mr. Nice Hank.", { copy(tilt = 0.9) },
        ),
        Rival(
            "june", "June", "Clutch",
            "Plays her best when she's behind.",
            "Don't relax with a lead. She gets sharper as the session goes.",
            "Now it gets interesting.", { copy(noise = noise + 0.15, tilt = -0.6) },
        ),
        Rival(
            "lou", "Aunt Lou", "Joker hound",
            "Swaps for every joker she sees on the table.",
            "Expose with real tiles. Any joker you put down is hers.",
            "I'll just borrow that joker, dear.", { copy(swapJokers = true, swapSlack = 0.4) },
        ),
        Rival(
            "marco", "Marco", "Joker thief",
            "Swaps for jokers even when it hurts his own hand, just to keep them from you.",
            "Never leave a joker in an exposure if you can help it.",
            "Finders keepers.", { copy(swapJokers = true, swapSlack = 1.0, defense = defense + 0.2) },
        ),
        Rival(
            "delphine", "Delphine", "Big hands only",
            "Only goes for expensive hands. Slow, but she makes you pay.",
            "Win fast with anything before she finishes one of hers.",
            "I don't do cheap.", { copy(valuePref = 1.5) },
        ),
        Rival(
            "ricky", "Ricky", "Speedster",
            "Takes the closest hand every time and calls to get there faster.",
            "Play defense once he has two exposures. He's always close.",
            "Blink and you'll miss it.",
            { copy(topK = maxOf(1, topK / 2), valuePref = -0.6, calls = true, callSlack = 1) },
        ),
        Rival(
            "harriet", "Harriet", "The Treasurer",
            "Counts every tile on the table and never chases a dead hand.",
            "Hard to fool. Mix families so your discards don't give you away.",
            "I've been counting.", { copy(trackLive = true, defense = maxOf(defense, 0.3) + 0.3, topK = maxOf(topK, 6)) },
        ),
        Rival(
            "wally", "Wally", "The Wall",
            "Plays pure defense. Happy to see the wall run out.",
            "He won't feed you anything. Draw your own tiles and win clean.",
            "Nothing for you today.", { copy(defense = maxOf(defense, 0.6) + 0.8, defenseFrom = 1, readDiscards = 0.4) },
        ),
        Rival(
            "agnes", "Sister Agnes", "Concealed purist",
            "Never exposes and loves concealed hands, which pay well.",
            "She never calls, so she never shows her hand. Watch what she never throws.",
            "Patience is a virtue.", { copy(calls = false, concealedPref = 2.5) },
        ),
        Rival(
            "vivian", "Vivian", "Jokerless",
            "Builds hands without jokers for double points.",
            "Slow to start. Pressure her early and she runs out of time.",
            "Jokers are for amateurs.", { copy(jokerlessPref = 0.7) },
        ),
        Rival(
            "odette", "Madame Odette", "Quints",
            "Hunts Quints with every joker she can find, and defends well.",
            "Hoard jokers and fifth copies. Her hands need both.",
            "Five of a kind, darling.", { copy(familyPref = mapOf("Quints" to 2.5), defense = defense + 0.3) },
        ),
        Rival(
            "sterling", "Sterling", "The Reader",
            "Reads your discards, works out your hand, and holds what you need.",
            "Throw a few tiles that mislead him, or switch hands halfway.",
            "I know what you're holding.", { copy(readDiscards = 1.2, defense = maxOf(defense, 0.6), defenseFrom = 2) },
        ),
        Rival(
            "kit", "Kit", "Chameleon",
            "Plays a different style every hand.",
            "Read the first few discards to work out who Kit is this time.",
            "Who am I today?", chameleon = true,
        ),
        Rival(
            "nora", "Nora", "Ice",
            "No habits, no mistakes, no nerves.",
            "No trick works on her. Just play well.",
            "...", { copy(noise = 0.0, tilt = 0.0) },
        ),
        Rival(
            "dowager", "The Dowager", "Champion",
            "Reads every table, defends everything, and swaps every joker.",
            "Everything you've learned, all at once.",
            "Shall we begin?",
            { copy(readDiscards = 1.0, defense = maxOf(defense, 0.6) + 0.3, defenseFrom = 2, swapSlack = 0.3, valuePref = 0.3) },
        ),
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): Rival = byId[id] ?: error("No rival '$id'")
}
