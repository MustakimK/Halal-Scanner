package com.example.halalscanner.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HistoryData(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val image: String,
    val name: String?,
    val status: String
)

