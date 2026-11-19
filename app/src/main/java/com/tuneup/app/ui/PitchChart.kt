package com.tuneup.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tuneup.app.audio.PitchSample
import kotlin.math.log2

@Composable
fun PitchChart(
    referenceSamples: List<PitchSample>,
    userSamples: List<PitchSample>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Canvas(Modifier.fillMaxSize()) {
            val all = referenceSamples + userSamples
            if (all.isEmpty()) return@Canvas
            val pitched = all.filter { it.frequencyHz != null }
            if (pitched.isEmpty()) return@Canvas

            val minMs = all.minOf { it.timestampMs }.toFloat()
            val maxMs = all.maxOf { it.timestampMs }.toFloat().coerceAtLeast(minMs + 500f)
            val minMidi = pitched.minOf { log2(it.frequencyHz!! / 440f) * 12 + 69 }
            val maxMidi = pitched.maxOf { log2(it.frequencyHz!! / 440f) * 12 + 69 }
            val range = (maxMidi - minMidi).coerceAtLeast(4f)

            fun x(ms: Long) = (ms - minMs) / (maxMs - minMs) * size.width
            fun y(midi: Float) = size.height * (1f - ((midi - minMidi) / range))

            for (i in 0..4) {
                val yy = size.height * i / 4f
                drawLine(gridColor, Offset(0f, yy), Offset(size.width, yy), 1f)
            }
            drawPitchPath(referenceSamples, ::x, { y(log2(it / 440f) * 12 + 69) }, primary, 4f)
            drawPitchPath(userSamples, ::x, { y(log2(it / 440f) * 12 + 69) }, secondary, 4f)
        }
        Text(
            "REF ▬  YOU ▬",
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        )
    }
}

private fun DrawScope.drawPitchPath(
    samples: List<PitchSample>,
    x: (Long) -> Float,
    y: (Float) -> Float,
    color: Color,
    width: Float
) {
    var path: Path? = null
    for (s in samples) {
        val f = s.frequencyHz
        if (f == null) {
            path?.let { drawPath(it, color, style = Stroke(width)) }
            path = null
            continue
        }
        val px = x(s.timestampMs)
        val py = y(f)
        if (path == null) path = Path().apply { moveTo(px, py) }
        else path.lineTo(px, py)
    }
    path?.let { drawPath(it, color, style = Stroke(width)) }
}
