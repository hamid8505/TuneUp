package com.tuneup.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Insert
    suspend fun insert(s: PracticeSession): Long

    @Query("SELECT * FROM practice_sessions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PracticeSession>>
}
