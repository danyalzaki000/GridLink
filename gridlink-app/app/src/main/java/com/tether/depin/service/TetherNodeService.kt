package com.tether.depin.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tether.depin.MainActivity
import com.tether.depin.TetherApplication
import com.tether.depin.data.local.TrafficLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TetherNodeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var proxyServer: LocalProxyServer? = null
    private var bandwidthTracker: BandwidthTracker? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // === KILL-SWITCH: intercept stop action from notification button ===
        if (intent?.action == ACTION_STOP_SERVICE || intent?.action == ACTION_STOP) {
            stopProxy()
            isRunning = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startProxy()
        startDemoTrafficPumper()

        return START_STICKY
    }

    private fun startProxy() {
        val app = application as TetherApplication
        val db = app.database

        bandwidthTracker = BandwidthTracker(
            nodeStatsDao = db.nodeStatsDao(),
            trafficLogDao = db.trafficLogDao(),
            scope = serviceScope,
            onBillingEvent = { usdc -> }
        )

        proxyServer = LocalProxyServer(
            port = 8080,
            onBytesTransferred = { bytes ->
                bandwidthTracker?.onBytesRouted(bytes)
            }
        )
        proxyServer?.start()
    }

    // Injects simulated traffic into Room DB for live UI demo.
    private fun startDemoTrafficPumper() {
        val app = application as TetherApplication
        val db = app.database
        val nodeStatsDao = db.nodeStatsDao()
        val trafficLogDao = db.trafficLogDao()

        val destinations = listOf(
            "Rajabazar Grid", "SoHo Relay", "Devnet Bridge",
            "AI Pipeline", "Data Scraping", "CDN Cache Node",
            "Search Indexer", "ML Inference", "Secure Tunnel"
        )

        serviceScope.launch {
            var tickCount = 0
            while (isRunning) {
                delay(1500) // Every 1.5 seconds

                val bytesPerTick = 2_500_000L  // 2.5 MB
                val mbPerTick = bytesPerTick.toDouble() / (1024.0 * 1024.0) // ~2.38 MB
                val usdcPerTick = mbPerTick * 0.003 // ~$0.00714

                // Update aggregate stats in Room DB
                nodeStatsDao.addBytesAndEarnings(bytesPerTick, usdcPerTick)

                // Write traffic log entry every 4th tick (~6 seconds)
                if (tickCount % 4 == 0) {
                    val dest = destinations[tickCount / 4 % destinations.size]
                    trafficLogDao.insert(
                        TrafficLog(
                            mbTransferred = mbPerTick * 4,
                            usdcEarned = usdcPerTick * 4,
                            destinationNode = dest
                        )
                    )
                }
                tickCount++
            }
        }
    }

    private fun stopProxy() {
        proxyServer?.stop()
        proxyServer = null
        bandwidthTracker?.reset()
        bandwidthTracker = null
    }

    private fun createNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopServiceIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TetherNodeService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TetherApplication.CHANNEL_ID)
            .setContentTitle("GridLink Node is Active")
            .setContentText("Routing Traffic • Earning SOL")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Node",
                stopServiceIntent
            )
            .build()
    }

    override fun onDestroy() {
        stopProxy()
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.tether.depin.STOP_SERVICE"
        const val ACTION_STOP_SERVICE = "com.tether.depin.ACTION_STOP_SERVICE"
        const val NOTIFICATION_ID = 1001
        var isRunning = false
            private set
    }
}
