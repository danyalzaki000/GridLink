package com.tether.depin.service

import com.tether.depin.data.local.TrafficLog
import com.tether.depin.data.local.TrafficLogDao
import com.tether.depin.data.local.NodeStatsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class BandwidthTracker(
    private val nodeStatsDao: NodeStatsDao,
    private val trafficLogDao: TrafficLogDao,
    private val scope: CoroutineScope,
    private val onBillingEvent: (Double) -> Unit = {}
) {

    private val accumulatedBytes = AtomicLong(0L)
    private val thresholdBytes = 10L * 1024 * 1024 // 10 MB

    // USDC rate per MB (configurable)
    private val usdcPerMb = 0.003

    fun onBytesRouted(bytes: Long) {
        val total = accumulatedBytes.addAndGet(bytes)

        if (total >= thresholdBytes) {
            // Reset and trigger billing
            val billedBytes = accumulatedBytes.getAndSet(0L)
            val mbTransferred = billedBytes.toDouble() / (1024 * 1024)
            val usdcEarned = mbTransferred * usdcPerMb

            scope.launch(Dispatchers.IO) {
                // Update aggregate stats
                nodeStatsDao.addBytesAndEarnings(billedBytes, usdcEarned)

                // Write traffic log entry
                trafficLogDao.insert(
                    TrafficLog(
                        mbTransferred = mbTransferred,
                        usdcEarned = usdcEarned,
                        destinationNode = "Rajabazar Grid"
                    )
                )

                onBillingEvent(usdcEarned)
            }
        }
    }

    fun reset() {
        accumulatedBytes.set(0L)
    }
}
