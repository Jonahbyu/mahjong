# Circuits — plan

A career mode. You climb a ladder of circuits, each harder than the last. Each circuit is a
short league against a fixed cast of named rivals with distinct styles. Win the circuit to
unlock the next one.

## What exists today (what this builds on)

- `HeuristicBot(difficulty)` — one bot, four tunings via `topK`, `noise`, live-tile tracking,
  defense, call limits, joker swaps. All knobs are hard-coded off `Difficulty`.
- `GameResult.payments` — net score per seat per hand, American style (discarder pays 2x,
  self-draw everyone pays 2x). Enough to keep match standings with no engine changes.
- `Wallet` — coins, cosmetics, Expert unlock, all in SharedPreferences.
- `Simulator` — bot-vs-bot harness. Use it to balance every persona.
- Card families: 2026, 2468, Like Numbers, Addition, Quints, Consecutive, ... — personas can
  have favorite families.

## Structure

```
Circuit 1  Rec Room          (Beginner-ish)      ── win ──▶
Circuit 2  Thursday Club     (Intermediate)      ── win ──▶
Circuit 3  Regional Open     (Advanced)          ── win ──▶
Circuit 4  Grand Masters     (Expert, secret)    ── win ──▶  Champion
```

**Inside a circuit:**

1. **League stage.** You + 7 rivals. 4 rounds. Each round you sit with 3 rivals (rotating
   draw so you meet everyone); the other 4 rivals play their own table off-screen.
2. **Session = 4 hands.** Score = sum of `payments` over the 4 hands. Wall game = 0.
3. **Placement points per session:** 1st 4 pts, 2nd 2, 3rd 1, 4th 0. Tiebreak: total score.
4. **Final table.** Top 4 in the league standings play one 4-hand final. Win the final = win
   the circuit. Miss the top 4, or lose the final, and you retry the circuit (or the final).
5. **Boss.** Each circuit's champion persona is seeded into the final if you make it.

Off-screen tables: simulate by rating (fast, controllable, no CPU hit), not by running real
games. Each persona has a hidden rating; results are a weighted roll. Real-engine simulation is
possible later but Expert bots on four background tables would be slow on a phone.

## Engine work

### 1. `BotProfile` (refactor, no behavior change)

Pull every hard-coded knob out of `HeuristicBot` into a data class. `Difficulty` becomes a
preset that returns a profile. Same seed + same preset must produce identical decisions — lock
that with a test before adding anything.

```kotlin
data class BotProfile(
    val topK: Int,
    val noise: Double,
    val trackLive: Boolean,
    val weightedEval: Boolean,          // Expert's scoring
    val defense: Double,                // 0.6 today for Advanced+, 0 below
    val defenseThreshold: Int,          // only defend when my distance >= this (3 today)
    val callLimit: Int?,                // null = only calls for Mahjong
    val swapJokers: Boolean,
    val courtesy: CourtesyPolicy,
    // New style knobs:
    val familyBias: Map<String, Double> = emptyMap(),  // negative = favored
    val valueBias: Double = 0.0,        // >0 prefers higher-point hands
    val concealedBias: Double = 0.0,    // >0 prefers C hands
    val jokerlessBias: Double = 0.0,
    val swapFromHumanFirst: Boolean = false,
    val readsHuman: Boolean = false,    // infers human's hand from discards and starves it
    val tilt: Double = 0.0,             // noise added per point behind in the session
)
```

Family/value/concealed bias need per-hand-instance scoring. `weightedScore` already loops
instances, so bias goes there. `plainScore` goes through `card.histogram` — either add a
per-instance cost hook to `histogram`, or route biased personas through the instance loop.

### 2. Reading the human (`readsHuman`)

Extend `dangerKinds`: today it only fires on opponents with 2+ exposures. The reader also
uses discards: tiles a seat throws early are tiles it doesn't want, so the families those
tiles belong to are cold; families it never discards from are hot. Hold hot tiles, feed cold.

### 3. `Match` (pure engine, testable)

```kotlin
class Match(val seats: List<SeatSpec>, val hands: Int = 4, val rules: Rules, seed: Long) {
    val scores: IntArray
    val results: List<GameResult>
    fun nextGame(): Game
    fun record(result: GameResult)
    val over: Boolean
    fun placements(): List<Int>
}
```

### 4. Balance with the simulator

For every persona, run N thousand games vs a baseline table and record win rate and average
score. Each circuit has a target band; a persona that beats its band gets more noise. Bosses
sit at the top of their band. Check this into a test that fails if a persona drifts out of band.

## App work

- **`Career` state** (new, `career/`): unlocked circuits, current circuit progress, league
  standings, head-to-head records, trophies, scouting progress. JSON in its own
  SharedPreferences file. Save between hands so quitting mid-session resumes at the next hand.
- **Start screen:** new "Circuits" card next to "Deal me in".
- **Circuit ladder screen:** the four circuits, locked/unlocked, trophies.
- **Circuit hub:** standings table, next table's three rivals with portraits and scouting
  cards, "Play session" button.
- **Between-hands scoreboard:** running session scores, hand N of 4.
- **Session results:** placement, points earned, standings movement.
- **Final / trophy screen.**
- **`OpponentUi`:** add `name` and `persona` so the table shows rival names, not seat winds.
- **Rewards:** wire into `Wallet` — prize purse per placement, circuit-exclusive tile sets and
  table styles as trophies (not buyable in the shop).

## Roster (first pass)

Original characters. Each has one clear style and one clear counter, so beating them teaches
something.

| Circuit | Rival | Style | Counter |
|---|---|---|---|
| Rec Room | **Penny** | Always chases 2468. Heavy family bias. | Hold evens. She stalls. |
| | **Gus** | Calls everything, exposes early, often gets stuck. | Read his exposures; never feed what he shows. |
| | **Ida** | Never calls. Slow, concealed, cheap hands. | Race her. Speed wins. |
| | *Boss:* **Aunt Lou** | Intermediate generalist, swaps jokers. | Solid fundamentals. |
| Thursday Club | **Marco** | Joker thief. Swaps from your exposures first. | Expose with real tiles, not jokers. |
| | **Delphine** | Point chaser. Only 30+ hands. Slow but costly. | Win fast and cheap before she lands one. |
| | **Ricky** | Speedster. Closest hand, calls aggressively. | Defense late in the wall. |
| | *Boss:* **The Treasurer** | Tracks dead tiles, balanced. | Mix families so you're hard to read. |
| Regional Open | **Wally** | Maximum defense. Happy with wall games. | You can't steal points; you must win clean. |
| | **Sister Agnes** | Concealed purist. Never exposes. Double-value hands. | Watch for tiles she *never* discards. |
| | **Vivian** | Jokerless snob. Builds hands without jokers for double value. | She's slow; pressure early. |
| | *Boss:* **Madame Quint** | Quints specialist with Advanced defense. | Hoard the 5th-copy tiles / keep jokers. |
| Grand Masters | **The Reader** | Infers your hand from your discards and starves it. | Discard misleadingly; switch hands mid-game. |
| | **Chameleon** | New style each hand (draws a profile at deal). | Scout from the first few discards. |
| | **Ice** | Expert eval, zero noise, no tilt. | Pure skill. |
| | *Boss:* **The Champion** | Reader + Expert eval + full defense. | Everything you've learned. |

Filler rivals (to make 7 per league): reuse personas from lower circuits with tighter profiles
("Penny, now Intermediate") so returning faces show growth.

## Phases

1. `BotProfile` refactor + parity test. No visible change.
2. Style knobs + 3 Rec Room personas + simulator bands.
3. `Match` in the engine + tests.
4. `Career` state + persistence.
5. UI: ladder, hub, scoreboard, results. Playable Circuit 1 end to end.
6. Circuits 2–4, bosses, rewards, trophies.
7. Flavor: scouting cards, banter lines, head-to-head.

Ship after phase 5 with one circuit to test whether the loop is fun before building the rest.

## Open decisions

- League + final (above), or a straight gauntlet of fixed tables ending in a boss? Gauntlet is
  half the work and more linear; league gives rivals and standings.
- Hands per session: 4 (one per wind) or fewer for a phone-length session?
- Does Grand Masters replace the current "beat an all-Advanced table" Expert unlock, or sit
  alongside it?
- Losing a circuit: retry the whole circuit, or just the final?
- Relegation on/off?

## More ideas

- **Scouting cards** that fill in as you play a rival: favorite family, call rate, defense,
  tells. Fully revealed after you beat them.
- **Banter lines.** One-liners on events (calls, being fed a tile, Mahjong). Cheap, big
  personality payoff.
- **Tilt.** Some rivals get sloppier when behind, others tighten up. Visible as a mood icon.
- **Venue house rules.** Rec Room: hints on. Regional: no courtesy pass. Grand Masters: hints
  off, faster bots. Uses the existing `Rules` and hint setting.
- **Venue looks.** Each circuit forces a table style from `UiStyles` for atmosphere.
- **Entry fees and prize purses.** Coin sink, raises stakes.
- **Event challenges.** "Win with a Quints hand," "Never feed a Mahjong this session." Bonus
  coins or a badge.
- **Gold circuits (New Game+).** Replay every circuit with tighter profiles after winning
  Grand Masters.
- **Daily Cup.** One seeded session per day, same deal for everyone (seed from the date, no
  server). Personal best tracking.
- **Practice vs a rival.** Pick any met persona for a free game to learn their style.
- **Post-loss review.** "You fed Penny her 2468 tile on turn 14." Pulls from the event log.
- **Rating ladder.** Endless mode with an Elo-style number after the circuits are done.
- **Head-to-head rivalries.** A recurring nemesis who shows up in multiple circuits and
  remembers your record.
