package com.faisal.routine

import android.content.Context
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/** One block of the routine. Minutes from midnight of the routine day; after midnight is 1440+. */
data class Block(val s: Int, val e: Int, val a: String, val k: String, val key: Boolean)

object Routine {
    /** Before 6:00 AM still counts as the previous night. */
    const val DAY_START = 6 * 60

    val DAY_KEYS = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

    data class Now(val dayIdx: Int, val min: Int, val dayStart: Long)

    /** Which routine day it is (Sat=0 … Fri=6) and the minute within it. */
    fun now(ms: Long = System.currentTimeMillis()): Now {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        var m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val day = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (m < DAY_START) {
            m += 1440
            day.add(Calendar.DATE, -1)
        }
        // Calendar: Sun=1 … Sat=7  ->  Sat=0, Sun=1 … Fri=6
        val idx = day.get(Calendar.DAY_OF_WEEK) % 7
        return Now(idx, m, day.timeInMillis)
    }

    fun addDays(dayStart: Long, n: Int): Long =
        Calendar.getInstance().apply { timeInMillis = dayStart; add(Calendar.DATE, n) }.timeInMillis

    /** Wall-clock time of a routine minute. */
    fun at(dayStart: Long, minute: Int): Long =
        Calendar.getInstance().apply { timeInMillis = dayStart; add(Calendar.MINUTE, minute) }.timeInMillis

    fun fmt(min: Int): String {
        val m = ((min % 1440) + 1440) % 1440
        var h = m / 60
        val ap = if (h >= 12) "PM" else "AM"
        h = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.US, "%d:%02d %s", h, m % 60, ap)
    }

    fun dur(min: Int): String {
        val h = min / 60
        val m = min % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }
}

/** State sent from the app screen (web app) through the bridge, with a bundled default. */
object RoutineStore {
    private const val PREFS = "routine_state"
    private const val KEY = "state"

    @Volatile
    private var cache: JSONObject? = null

    fun save(ctx: Context, json: String) {
        val obj = try { JSONObject(json) } catch (e: Exception) { return }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
        cache = obj
    }

    private fun state(ctx: Context): JSONObject {
        cache?.let { return it }
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        val obj = try {
            JSONObject(saved ?: defaultJson(ctx))
        } catch (e: Exception) {
            JSONObject(defaultJson(ctx))
        }
        cache = obj
        return obj
    }

    private fun defaultJson(ctx: Context): String =
        ctx.assets.open("default_state.json").bufferedReader().use { it.readText() }

    fun person(ctx: Context): String =
        if (state(ctx).optString("person", "faisal") == "akash") "akash" else "faisal"

    fun notifyOn(ctx: Context): Boolean = state(ctx).optBoolean("notify", false)

    fun streak(ctx: Context, person: String): Int =
        state(ctx).optJSONObject("streak")?.optInt(person, 0) ?: 0

    fun schedule(ctx: Context, person: String, dayIdx: Int): List<Block> {
        val arr = state(ctx).optJSONObject("schedules")
            ?.optJSONObject(person)
            ?.optJSONArray(Routine.DAY_KEYS[dayIdx]) ?: return emptyList()
        val out = ArrayList<Block>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(Block(o.optInt("s"), o.optInt("e"), o.optString("a"), o.optString("k", "rest"), o.optBoolean("key")))
        }
        return out.sortedBy { it.s }
    }
}
