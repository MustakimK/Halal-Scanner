package com.example.halalscanner.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Query("SELECT * FROM historydata")
    fun getAll(): List<HistoryData>

    @Insert
    fun insertAll(vararg historyData: HistoryData)

    @Query("DELETE FROM historydata WHERE id IN (SELECT id FROM historydata ORDER BY id ASC LIMIT :n)")
    suspend fun deleteOldest(n: Int)


    @Query("SELECT COUNT(*) FROM historydata")
    suspend fun getCount(): Int

    @Query("SELECT MAX(id) FROM historydata")
    suspend fun getNewestItemId(): Int

}