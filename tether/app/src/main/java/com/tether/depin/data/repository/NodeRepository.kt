package com.tether.depin.data.repository

import com.tether.depin.data.local.AppDatabase
import com.tether.depin.data.local.NodeStats
import com.tether.depin.data.local.TrafficLog
import com.tether.depin.data.remote.MatchmakerApi
import com.tether.depin.data.remote.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NodeRepository(
    private val database: AppDatabase,
    private val api: MatchmakerApi
) {

    private val nodeStatsDao = database.nodeStatsDao()
    private val trafficLogDao = database.trafficLogDao()

    // --- Observe Flows ---

    val nodeStats: Flow<NodeStats> = nodeStatsDao.observeStats().map { it ?: NodeStats() }

    val trafficLogs: Flow<List<TrafficLog>> = trafficLogDao.observeAll()

    val recentTrafficLogs: Flow<List<TrafficLog>> = trafficLogDao.observeRecent(10)

    // --- DB Operations ---

    suspend fun initializeStats() {
        val existing = nodeStatsDao.observeStats()
        nodeStatsDao.upsert(NodeStats())
    }

    suspend fun addBytesAndEarnings(bytes: Long, usdc: Double) {
        nodeStatsDao.addBytesAndEarnings(bytes, usdc)
    }

    suspend fun logTraffic(mbTransferred: Double, usdcEarned: Double, destinationNode: String) {
        trafficLogDao.insert(
            TrafficLog(
                mbTransferred = mbTransferred,
                usdcEarned = usdcEarned,
                destinationNode = destinationNode
            )
        )
    }

    // --- API Operations ---

    suspend fun registerNode(ipAddress: String, walletAddress: String): Result<String> {
        return try {
            val response = api.registerNode(RegisterRequest(ipAddress, walletAddress))
            if (response.isSuccessful) {
                Result.success(response.body()?.nodeId ?: "")
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pingStatus(): Result<Boolean> {
        return try {
            val response = api.getNodeStatus()
            if (response.isSuccessful) {
                Result.success(response.body()?.isActive ?: false)
            } else {
                Result.failure(Exception("Status check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
