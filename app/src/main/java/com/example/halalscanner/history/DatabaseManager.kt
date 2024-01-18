package com.example.halalscanner.history

import android.content.Context
import androidx.room.Room

class DatabaseManager private constructor(context: Context) {
    val database: HistoryDatabase = Room.databaseBuilder(
        context.applicationContext,
        HistoryDatabase::class.java, "HistoryDatabase"
    ).build()

    companion object {
        @Volatile
        private var INSTANCE: DatabaseManager? = null

        fun getInstance(context: Context): DatabaseManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseManager(context).also { INSTANCE = it }
            }
    }
}