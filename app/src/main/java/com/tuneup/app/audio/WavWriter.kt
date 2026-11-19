package com.tuneup.app.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavWriter {
    fun wrapPcmAsWav(pcm: ByteArray, sampleRate: Int, channels: Int, bits: Int): ByteArray {
        val dataSize = pcm.size
        val byteRate = sampleRate * channels * bits / 8
        val blockAlign = channels * bits / 8
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        out.write(intLE(36 + dataSize))
        out.write("WAVEfmt ".toByteArray())
        out.write(intLE(16))
        out.write(shortLE(1))
        out.write(shortLE(channels.toShort()))
        out.write(intLE(sampleRate))
        out.write(intLE(byteRate))
        out.write(shortLE(blockAlign.toShort()))
        out.write(shortLE(bits.toShort()))
        out.write("data".toByteArray())
        out.write(intLE(dataSize))
        out.write(pcm)
        return out.toByteArray()
    }
    private fun intLE(v: Int) =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
    private fun shortLE(v: Short) =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array()
}
