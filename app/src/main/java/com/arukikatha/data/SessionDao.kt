package com.arukikatha.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(item: SessionHistoryEntity)

    @Query("SELECT * FROM session_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SessionHistoryEntity>>
}
