package com.tuneup.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tuneup.app.audio.AudioDecoder
import com.tuneup.app.audio.AudioRecorder
import com.tuneup.app.audio.DtwComparator
import com.tuneup.app.audio.PitchError
import com.tuneup.app.audio.PitchSample
import com.tuneup.app.audio.ReferencePitchExtractor
import com.tuneup.app.data.db.AppDatabase
import com.tuneup.app.data.db.PracticeSession
import com.tuneup.app.data.db.StoredPitchError
import com.tuneup.app.service.RecordingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface PracticeUiState {
    data object Idle : PracticeUiState
    data class Ready(val referenceName: String, val durationMs: Long) : PracticeUiState
    data class Recording(
        val referenceName: String,
        val referencePitch: List<PitchSample>,
        val livePitch: List<PitchSample>,
        val elapsedMs: Long
    ) : PracticeUiState
    data class Finished(
        val score: Float,
        val errors: List<PitchError>,
        val userSamples: List<PitchSample>,
        val referenceSamples: List<PitchSample>,
        val recordedPath: String
    ) : PracticeUiState
    data class Error(val message: String) : PracticeUiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).practiceSessionDao()
    private val recorder = AudioRecorder()
    private val extractor = ReferencePitchExtractor()

    private val _state = MutableStateFlow<PracticeUiState>(PracticeUiState.Idle)
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    val history: StateFlow<List<PracticeSession>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentRefUri: Uri? = null
        private set
    private var currentRefPitch: List<PitchSample> = emptyList()
    private var currentRefName: String = ""
    private var currentLive: MutableList<PitchSample> = mutableListOf()
    private var startMs: Long = 0

    fun loadReference(uri: Uri, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pcm = AudioDecoder.decodeToPcm(getApplication(), uri)
                if (pcm.isEmpty()) {
                    _state.value = PracticeUiState.Error("Could not decode audio file")
                    return@launch
                }
                currentRefPitch = extractor.extract(pcm)
                currentRefName = name
                currentRefUri = uri
                _state.value = PracticeUiState.Ready(name, pcm.size * 1000L / 44100)
            } catch (e: Exception) {
                _state.value = PracticeUiState.Error("Load failed: ${e.message}")
            }
        }
    }

    fun startRecording() {
        if (currentRefUri == null) {
            _state.value = PracticeUiState.Error("Load a reference first")
            return
        }
        currentLive = mutableListOf()
        startMs = System.currentTimeMillis()
        _state.value = PracticeUiState.Recording(
            currentRefName, currentRefPitch, emptyList(), 0
        )
        RecordingService.start(getApplication())

        viewModelScope.launch(Dispatchers.Default) {
            recorder.pitchFlow.collect { sample ->
                currentLive.add(sample)
                val cur = _state.value
                if (cur is PracticeUiState.Recording) {
                    _state.value = cur.copy(
                        livePitch = currentLive.toList(),
                        elapsedMs = System.currentTimeMillis() - startMs
                    )
                }
            }
        }
        recorder.start(viewModelScope)
    }

    fun stopAndScore() {
        recorder.stop()
        RecordingService.stop(getApplication())
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(getApplication<Application>().filesDir, "recordings")
            dir.mkdirs()
            val out = File(dir, "rec_${System.currentTimeMillis()}.wav")
            recorder.writeWav(out.absolutePath)

            val user = currentLive.toList()
            val res = DtwComparator.compare(currentRefPitch, user)
            val avgCents = if (res.errors.isEmpty()) 0f
                else res.errors.map { it.centsOff }.average().toFloat()

            dao.insert(
                PracticeSession(
                    referenceName = currentRefName,
                    recordedFilePath = out.absolutePath,
                    score = res.score,
                    timestamp = System.currentTimeMillis(),
                    errorCents = avgCents,
                    errors = res.errors.map {
                        StoredPitchError(
                            it.timestampMs, it.userFreq, it.refFreq, it.centsOff
                        )
                    }
                )
            )
            _state.value = PracticeUiState.Finished(
                res.score, res.errors, user, currentRefPitch, out.absolutePath
            )
        }
    }

    fun reset() {
        recorder.stop()
        RecordingService.stop(getApplication())
        _state.value = if (currentRefUri != null)
            PracticeUiState.Ready(
                currentRefName,
                currentRefPitch.lastOrNull()?.timestampMs ?: 0
            )
        else PracticeUiState.Idle
    }

    override fun onCleared() {
        recorder.stop()
        RecordingService.stop(getApplication())
        super.onCleared()
    }
}
