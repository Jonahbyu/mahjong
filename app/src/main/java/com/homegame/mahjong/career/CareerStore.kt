package com.homegame.mahjong.career

import android.content.Context
import com.homegame.mahjong.engine.career.Career
import com.homegame.mahjong.engine.career.CircuitRun
import com.homegame.mahjong.engine.career.RunStatus
import com.homegame.mahjong.engine.career.SessionResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves the circuits career on the phone as one JSON string. Saved after every hand, so quitting
 * mid-session picks up at the next hand. A hand in progress isn't saved.
 */
class CareerStore(context: Context) {
    private val prefs = context.getSharedPreferences("career", Context.MODE_PRIVATE)

    fun load(): Career = runCatching { prefs.getString(KEY, null)?.let { decode(JSONObject(it)) } }.getOrNull() ?: Career()

    fun save(c: Career) {
        prefs.edit().putString(KEY, encode(c).toString()).apply()
    }

    fun reset() { prefs.edit().remove(KEY).apply() }

    private fun encode(c: Career) = JSONObject().apply {
        put("unlocked", c.unlocked)
        put("trophies", JSONArray(c.trophies.sorted()))
        put("h2h", JSONObject().apply { c.headToHead.forEach { (id, r) -> put(id, JSONArray(r.toList())) } })
        c.run?.let { put("run", encodeRun(it)) }
    }

    private fun encodeRun(r: CircuitRun) = JSONObject().apply {
        put("circuit", r.circuitIndex)
        put("seed", r.seed)
        put("round", r.round)
        put("status", r.status.name)
        put("points", JSONObject(r.points as Map<*, *>))
        put("scores", JSONObject(r.scores as Map<*, *>))
        put("hand", r.hand)
        put("session", JSONArray(r.sessionScores.toList()))
        put("finalAttempts", r.finalAttempts)
        r.last?.let { last ->
            put("last", JSONObject().apply {
                put("round", last.round)
                put("final", last.final)
                put("rows", JSONArray(last.rows.map { JSONObject().put("id", it.id).put("score", it.score).put("place", it.place) }))
            })
        }
    }

    private fun decode(o: JSONObject): Career {
        val h2h = mutableMapOf<String, IntArray>()
        o.optJSONObject("h2h")?.let { j -> j.keys().forEach { id -> val a = j.getJSONArray(id); h2h[id] = intArrayOf(a.getInt(0), a.getInt(1)) } }
        val trophies = o.optJSONArray("trophies")?.let { a -> (0 until a.length()).map { a.getInt(it) }.toMutableSet() } ?: mutableSetOf()
        return Career(
            unlocked = o.optInt("unlocked", 1),
            trophies = trophies,
            run = o.optJSONObject("run")?.let(::decodeRun),
            headToHead = h2h,
        )
    }

    private fun intMap(o: JSONObject?): MutableMap<String, Int> {
        val m = mutableMapOf<String, Int>()
        o?.keys()?.forEach { m[it] = o.getInt(it) }
        return m
    }

    private fun decodeRun(o: JSONObject): CircuitRun {
        val s = o.getJSONArray("session")
        val last = o.optJSONObject("last")?.let { l ->
            val rows = l.getJSONArray("rows")
            SessionResult(
                round = l.getInt("round"), final = l.getBoolean("final"),
                rows = (0 until rows.length()).map { i ->
                    val r = rows.getJSONObject(i)
                    SessionResult.Row(r.getString("id"), r.getInt("score"), r.getInt("place"))
                },
            )
        }
        return CircuitRun(
            circuitIndex = o.getInt("circuit"),
            seed = o.getLong("seed"),
            round = o.getInt("round"),
            status = RunStatus.valueOf(o.getString("status")),
            points = intMap(o.optJSONObject("points")),
            scores = intMap(o.optJSONObject("scores")),
            hand = o.getInt("hand"),
            sessionScores = IntArray(4) { s.getInt(it) },
            finalAttempts = o.optInt("finalAttempts", 0),
            last = last,
        )
    }

    private companion object {
        const val KEY = "career_v1"
    }
}
