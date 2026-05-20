package com.arukikatha.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val completedBriskCount: Int,
    val completedNormalCount: Int,
    val successfulMinutes: Int,
    val resetsTriggered: Int,
    val totalPauses: Int,
    val goalCompleted: Boolean
)
