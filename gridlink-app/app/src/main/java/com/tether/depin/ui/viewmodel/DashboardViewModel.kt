package com.tether.depin.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tether.depin.TetherApplication
import com.tether.depin.data.local.NodeStats
import com.tether.depin.data.local.TrafficLog
import com.tether.depin.data.remote.ApiClient
import com.tether.depin.data.repository.NodeRepository
import com.tether.depin.service.TetherNodeService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NodeRepository(
        database = (application as TetherApplication).database,
        api = ApiClient.matchmakerApi
    )

    val nodeStats: StateFlow<NodeStats> = repository.nodeStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NodeStats())

    val recentLogs: StateFlow<List<TrafficLog>> = repository.recentTrafficLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProxyRunning = MutableStateFlow(TetherNodeService.isRunning)
    val isProxyRunning: StateFlow<Boolean> = _isProxyRunning.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeStats()
        }
        // Uptime counter
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isProxyRunning.value) {
                    _uptimeSeconds.value += 1
                }
            }
        }
    }

    fun setProxyRunning(running: Boolean) {
        _isProxyRunning.value = running
        if (!running) {
            _uptimeSeconds.value = 0
        }
    }

    fun formatUptime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }

    fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1.0) "%.1f".format(gb) else "%.0f".format(bytes.toDouble() / (1024 * 1024))
    }

    fun formatBytesUnit(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1.0) "GB" else "MB"
    }

    fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }
}
