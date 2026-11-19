package com.tuneup.app.audio

class ReferencePitchExtractor(
    private val sampleRate: Int = 44100,
    private val hopSize: Int = sampleRate / 50,
    private val windowSize: Int = 2048
) {
    fun extract(pcm: ShortArray): List<PitchSample> {
        val det = PitchDetector(sampleRate)
        val result = ArrayList<PitchSample>()
        var pos = 0
        while (pos + windowSize <= pcm.size) {
            val w = pcm.copyOfRange(pos, pos + windowSize)
            val ts = pos * 1000L / sampleRate
            val r = det.detect(w)
            result.add(
                when (r) {
                    is PitchResult.Pitched -> PitchSample(ts, r.frequency, r.clarity)
                    else -> PitchSample(ts, null, 0f)
                }
            )
            pos += hopSize
        }
        return result
    }
}
