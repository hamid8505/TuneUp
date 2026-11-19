package com.tuneup.app.audio

import kotlin.math.abs
import kotlin.math.ln

data class PitchPoint(val timeMs: Long, val freq: Float)
data class PitchError(
    val timestampMs: Long,
    val userFreq: Float,
    val refFreq: Float,
    val centsOff: Float
)
data class CompareResult(val score: Float, val errors: List<PitchError>)

object DtwComparator {
    fun compare(reference: List<PitchSample>, user: List<PitchSample>): CompareResult {
        val ref = reference.filter { it.frequencyHz != null && it.clarity > 0.5f }
            .map { PitchPoint(it.timestampMs, it.frequencyHz!!) }
        val usr = user.filter { it.frequencyHz != null && it.clarity > 0.5f }
            .map { PitchPoint(it.timestampMs, it.frequencyHz!!) }
        if (ref.isEmpty() || usr.isEmpty()) return CompareResult(0f, emptyList())

        val n = ref.size; val m = usr.size
        val inf = Float.MAX_VALUE / 4
        val cost = Array(n + 1) { FloatArray(m + 1) { inf } }
        cost[0][0] = 0f
        val back = Array(n + 1) { IntArray(m + 1) { -1 } }

        for (i in 1..n) for (j in 1..m) {
            val d = cents(ref[i - 1].freq, usr[j - 1].freq)
            val a = cost[i - 1][j]; val b = cost[i][j - 1]; val c = cost[i - 1][j - 1]
            val min = minOf(a, b, c)
            cost[i][j] = d + min
            back[i][j] = when (min) { a -> 0; b -> 1; else -> 2 }
        }

        var i = n; var j = m
        val pairs = ArrayList<Pair<PitchPoint, PitchPoint>>()
        while (i > 0 && j > 0) {
            pairs.add(ref[i - 1] to usr[j - 1])
            when (back[i][j]) {
                0 -> i--
                1 -> j--
                else -> { i--; j-- }
            }
        }

        val good = pairs.count { cents(it.first.freq, it.second.freq) < 50f }
        val score = 100f * good / pairs.size.coerceAtLeast(1)
        val errors = pairs.map { (r, u) ->
            PitchError(u.timeMs, u.freq, r.freq, cents(r.freq, u.freq))
        }
        return CompareResult(score, errors)
    }

    private fun cents(f1: Float, f2: Float): Float {
        if (f1 <= 0f || f2 <= 0f) return 1200f
        return abs(1200f * ln(f2 / f1) / ln(2f))
    }
}
