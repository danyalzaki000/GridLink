package com.tether.depin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traffic_log")
data class TrafficLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mbTransferred: Double = 0.0,
    val usdcEarned: Double = 0.0,
    val destinationNode: String = ""
)
