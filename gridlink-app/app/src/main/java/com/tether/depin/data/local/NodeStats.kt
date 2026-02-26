package com.tether.depin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "node_stats")
data class NodeStats(
    @PrimaryKey
    val id: Int = 1,
    val totalBytesShared: Long = 0L,
    val totalEarnedUsdc: Double = 0.0
)
