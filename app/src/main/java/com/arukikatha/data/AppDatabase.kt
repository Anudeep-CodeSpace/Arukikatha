package com.arukikatha.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SessionHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
