package com.tuneup.app.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

object AudioDecoder {
    fun decodeToPcm(context: Context, uri: Uri, targetRate: Int = 44100): ShortArray {
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            extractor.setDataSource(it.fileDescriptor)
        } ?: return ShortArray(0)

        var track = -1
        var fmt: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) { track = i; fmt = f; break }
        }
        if (track == -1 || fmt == null) { extractor.release(); return ShortArray(0) }
        extractor.selectTrack(track)

        val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(fmt, null, null, 0)
        codec.start()

        val out = ArrayList<Short>()
        val info = MediaCodec.BufferInfo()
        var inDone = false
        var outDone = false
        val srcRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        while (!outDone) {
            if (!inDone) {
                val idx = codec.dequeueInputBuffer(10_000)
                if (idx >= 0) {
                    val b = codec.getInputBuffer(idx)!!
                    val sz = extractor.readSampleData(b, 0)
                    if (sz < 0) {
                        codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inDone = true
                    } else {
                        codec.queueInputBuffer(idx, 0, sz, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val idx = codec.dequeueOutputBuffer(info, 10_000)
            if (idx >= 0) {
                val b = codec.getOutputBuffer(idx)!!
                b.order(ByteOrder.LITTLE_ENDIAN)
                val shorts = ShortArray(info.size / 2)
                b.asShortBuffer().get(shorts)
                codec.releaseOutputBuffer(idx, false)
                if (ch > 1) {
                    val frames = shorts.size / ch
                    for (i in 0 until frames) {
                        var sum = 0
                        for (c in 0 until ch) sum += shorts[i * ch + c]
                        out.add((sum / ch).toShort())
                    }
                } else out.addAll(shorts.toList())
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
            }
        }
        codec.stop(); codec.release(); extractor.release()

        val pcm = out.toShortArray()
        return if (srcRate != targetRate) resample(pcm, srcRate, targetRate) else pcm
    }

    private fun resample(inp: ShortArray, src: Int, dst: Int): ShortArray {
        val ratio = dst.toDouble() / src
        val outSize = (inp.size * ratio).toInt()
        val out = ShortArray(outSize)
        for (i in 0 until outSize) {
            val pos = i / ratio
            val idx = pos.toInt()
            val frac = pos - idx
            val a = if (idx < inp.size) inp[idx].toDouble() else 0.0
            val b = if (idx + 1 < inp.size) inp[idx + 1].toDouble() else a
            out[i] = (a + (b - a) * frac).toInt().toShort()
        }
        return out
    }
}
