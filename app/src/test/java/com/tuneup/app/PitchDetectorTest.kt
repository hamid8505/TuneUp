package com.tuneup.app

import com.tuneup.app.audio.PitchDetector
import com.tuneup.app.audio.PitchResult
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PitchDetectorTest {

    private val sampleRate = 44100

    private fun generateSine(freq: Float, durationMs: Int = 100): ShortArray {
        val n = sampleRate * durationMs / 1000
        return ShortArray(n) { i ->
            (sin(2 * PI * freq * i / sampleRate) * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
    }

    @Test
    fun detects440HzAccurately() {
        val detector = PitchDetector(sampleRate)
        val samples = generateSine(440f)
        val result = detector.detect(samples)
        assertTrue(result is PitchResult.Pitched)
        val freq = (result as PitchResult.Pitched).frequency
        assertTrue("Expected ~440, got $freq", abs(freq - 440f) < 5f)
    }

    @Test
    fun returnsNoneForSilence() {
        val detector = PitchDetector(sampleRate)
        val silence = ShortArray(2048) { 0 }
        val result = detector.detect(silence)
        assertTrue(result is PitchResult.None)
    }

    @Test
    fun detectsLowFrequency100Hz() {
        val detector = PitchDetector(sampleRate, minFrequency = 80f, maxFrequency = 1000f)
        val samples = generateSine(100f)
        val result = detector.detect(samples)
        assertTrue(result is PitchResult.Pitched)
        val freq = (result as PitchResult.Pitched).frequency
        assertTrue("Expected ~100, got $freq", abs(freq - 100f) < 5f)
    }
}
