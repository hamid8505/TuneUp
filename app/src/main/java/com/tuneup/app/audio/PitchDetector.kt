package com.tuneup.app.audio

class PitchDetector(
    private val sampleRate: Int = 44100,
    private val minFrequency: Float = 80f,
    private val maxFrequency: Float = 1000f,
    private val threshold: Float = 0.15f
) {
    private val windowSize = 2048

    fun detect(input: ShortArray): PitchResult {
        val samples = if (input.size < windowSize) input.copyOf(windowSize) else input
        val size = windowSize
        val minTau = (sampleRate / maxFrequency).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / minFrequency).toInt().coerceAtMost(size / 2)

        val diff = FloatArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0f
            for (i in 0 until size - tau) {
                val d = samples[i].toFloat() - samples[i + tau].toFloat()
                sum += d * d
            }
            diff[tau] = sum
        }

        val cmnd = FloatArray(maxTau + 1)
        cmnd[0] = 1f
        var running = 0f
        for (tau in 1..maxTau) {
            running += diff[tau]
            cmnd[tau] = if (running == 0f) 1f else diff[tau] * tau / running
        }

        var tau = -1
        for (t in minTau..maxTau) {
            if (cmnd[t] < threshold) {
                var local = t
                while (local + 1 <= maxTau && cmnd[local + 1] < cmnd[local]) local++
                tau = local
                break
            }
        }
        if (tau == -1) {
            var minVal = Float.MAX_VALUE
            var minIdx = -1
            for (t in minTau..maxTau) if (cmnd[t] < minVal) { minVal = cmnd[t]; minIdx = t }
            if (minIdx == -1 || minVal > 0.5f) return PitchResult.None
            tau = minIdx
        }

        val better = if (tau > 0 && tau < maxTau) {
            val s0 = cmnd[tau - 1]; val s1 = cmnd[tau]; val s2 = cmnd[tau + 1]
            val denom = 2f * (2f * s1 - s0 - s2)
            if (denom != 0f) tau + (s2 - s0) / denom else tau.toFloat()
        } else tau.toFloat()

        if (better <= 0f) return PitchResult.None
        val frequency = sampleRate / better
        val clarity = 1f - cmnd[tau]
        if (frequency < minFrequency || frequency > maxFrequency) return PitchResult.None
        if (clarity < 0.5f) return PitchResult.None
        return PitchResult.Pitched(frequency, clarity)
    }
}

sealed class PitchResult {
    data object None : PitchResult()
    data class Pitched(val frequency: Float, val clarity: Float) : PitchResult()
}
