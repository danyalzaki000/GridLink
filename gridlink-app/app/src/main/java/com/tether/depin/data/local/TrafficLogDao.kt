package com.tether.depin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrafficLogDao {

    @Query("SELECT * FROM traffic_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TrafficLog>>

    @Query("SELECT * FROM traffic_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<TrafficLog>>

    @Insert
    suspend fun insert(log: TrafficLog)

    @Query("DELETE FROM traffic_log")
    suspend fun clearAll()
}
