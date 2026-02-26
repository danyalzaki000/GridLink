package com.tether.depin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeStatsDao {

    @Query("SELECT * FROM node_stats WHERE id = 1")
    fun observeStats(): Flow<NodeStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: NodeStats)

    @Query("UPDATE node_stats SET totalBytesShared = totalBytesShared + :bytes WHERE id = 1")
    suspend fun addBytes(bytes: Long)

    @Query("UPDATE node_stats SET totalEarnedUsdc = totalEarnedUsdc + :usdc WHERE id = 1")
    suspend fun addEarnings(usdc: Double)

    @Query("UPDATE node_stats SET totalBytesShared = totalBytesShared + :bytes, totalEarnedUsdc = totalEarnedUsdc + :usdc WHERE id = 1")
    suspend fun addBytesAndEarnings(bytes: Long, usdc: Double)
}
