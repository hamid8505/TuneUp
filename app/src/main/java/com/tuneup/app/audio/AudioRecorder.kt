package com.tuneup.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class PitchSample(val timestampMs: Long, val frequencyHz: Float?, val clarity: Float)

class AudioRecorder(
    private val sampleRate: Int = 44100,
    private val pitchDetector: PitchDetector = PitchDetector(sampleRate)
) {
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private val rawBuffer = ByteArrayOutputStream()
    private val _pitchFlow = MutableSharedFlow<PitchSample>(extraBufferCapacity = 128)
    val pitchFlow: SharedFlow<PitchSample> = _pitchFlow

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        stop()
        synchronized(rawBuffer) { rawBuffer.reset() }

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, sampleRate / 10 * 2)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            throw IllegalStateException("AudioRecord init failed")
        }
        audioRecord = rec
        rec.startRecording()

        job = scope.launch(Dispatchers.Default) {
            val buf = ShortArray(2048)
            var ts = 0L
            while (isActive) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    val bytes = ByteArray(n * 2)
                    for (i in 0 until n) {
                        val s = buf[i].toInt()
                        bytes[i * 2] = (s and 0xFF).toByte()
                        bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                    }
                    synchronized(rawBuffer) { rawBuffer.write(bytes) }

                    val pcm = if (n < buf.size) buf.copyOf(buf.size) else buf
                    val r = pitchDetector.detect(pcm)
                    ts += n * 1000L / sampleRate
                    val sample = when (r) {
                        is PitchResult.Pitched -> PitchSample(ts, r.frequency, r.clarity)
                        else -> PitchSample(ts, null, 0f)
                    }
                    _pitchFlow.tryEmit(sample)
                }
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        audioRecord?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        audioRecord = null
    }

    fun writeWav(path: String) {
        val pcm = synchronized(rawBuffer) { rawBuffer.toByteArray() }
        if (pcm.isEmpty()) return
        java.io.File(path).writeBytes(WavWriter.wrapPcmAsWav(pcm, sampleRate, 1, 16))
    }
}
