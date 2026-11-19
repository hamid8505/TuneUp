package com.tuneup.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PracticeScreen(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasMic = it }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.loadReference(it, it.lastPathSegment ?: "reference") }
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { mediaPlayer?.release() } }

    LaunchedEffect(state) {
        if (state is PracticeUiState.Recording) {
            mediaPlayer?.release()
            vm.currentRefUri?.let { uri ->
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(ctx, uri); prepare(); start()
                }
            }
        } else {
            mediaPlayer?.let { try { it.stop() } catch (_: Exception) {} }
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val s = state) {
            is PracticeUiState.Idle -> {
                Text("Pick a reference audio to start",
                    style = MaterialTheme.typography.titleMedium)
                Button(onClick = { picker.launch(arrayOf("audio/*")) }) {
                    Text("Choose Reference Audio")
                }
            }
            is PracticeUiState.Ready -> {
                Text("Reference: ${s.referenceName}",
                    style = MaterialTheme.typography.titleMedium)
                Text("Duration: ${s.durationMs / 1000}s")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                        Text("Change")
                    }
                    Button(onClick = {
                        if (!hasMic) permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        else vm.startRecording()
                    }, enabled = hasMic) { Text("Start Recording") }
                }
                if (!hasMic)
                    Text("Microphone permission is required",
                        color = MaterialTheme.colorScheme.error)
            }
            is PracticeUiState.Recording -> {
                Text("● Recording  ${s.elapsedMs / 1000}s",
                    style = MaterialTheme.typography.titleMedium)
                PitchChart(s.referencePitch, s.livePitch,
                    Modifier.fillMaxWidth().height(260.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.stopAndScore() }) { Text("Stop & Score") }
                    OutlinedButton(onClick = { vm.reset() }) { Text("Cancel") }
                }
            }
            is PracticeUiState.Finished -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Score", style = MaterialTheme.typography.labelLarge)
                        Text("%.1f / 100".format(s.score),
                            style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Errors > 50¢: ${s.errors.count { it.centsOff > 50f }} / ${s.errors.size}")
                    }
                }
                PitchChart(s.referenceSamples, s.userSamples,
                    Modifier.fillMaxWidth().height(260.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(s.recordedPath); prepare(); start()
                        }
                    }) { Text("Play Recording") }
                    OutlinedButton(onClick = { vm.reset() }) { Text("Try Again") }
                }
            }
            is PracticeUiState.Error -> {
                Text("Error: ${s.message}",
                    color = MaterialTheme.colorScheme.error)
                Button(onClick = { vm.reset() }) { Text("Reset") }
            }
        }
    }
}
