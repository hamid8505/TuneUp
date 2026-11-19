package com.tuneup.app

import com.tuneup.app.audio.DtwComparator
import com.tuneup.app.audio.PitchSample
import org.junit.Assert.assertTrue
import org.junit.Test

class DtwComparatorTest {

    private fun sample(ts: Long, f: Float?) =
        PitchSample(ts, f, if (f != null) 0.9f else 0f)

    @Test
    fun identicalSequencesYieldHighScore() {
        val ref = listOf(
            sample(0, 440f), sample(20, 440f), sample(40, 440f), sample(60, 440f)
        )
        val result = DtwComparator.compare(ref, ref.toList())
        assertTrue("Score should be ~100, got ${result.score}", result.score > 95f)
    }

    @Test
    fun oneOctaveOffYieldsLowScore() {
        val ref = listOf(
            sample(0, 440f), sample(20, 440f), sample(40, 440f), sample(60, 440f)
        )
        val user = listOf(
            sample(0, 880f), sample(20, 880f), sample(40, 880f), sample(60, 880f)
        )
        val result = DtwComparator.compare(ref, user)
        assertTrue("Score should be low, got ${result.score}", result.score < 20f)
    }

    @Test
    fun emptyUserReturnsZeroScore() {
        val ref = listOf(sample(0, 440f))
        val result = DtwComparator.compare(ref, emptyList())
        assertTrue(result.score == 0f)
    }
}
