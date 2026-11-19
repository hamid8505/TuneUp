package com.tuneup.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

data class StoredPitchError(
    val timestampMs: Long,
    val userFreq: Float,
    val refFreq: Float,
    val centsOff: Float
)

@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val referenceName: String,
    val recordedFilePath: String,
    val score: Float,
    val timestamp: Long,
    val errorCents: Float,
    val errors: List<StoredPitchError> = emptyList()
)
