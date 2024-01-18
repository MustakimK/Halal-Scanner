package com.example.halalscanner.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HistoryData::class], version = 1)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}