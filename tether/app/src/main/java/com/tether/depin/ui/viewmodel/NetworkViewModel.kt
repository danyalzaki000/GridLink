package com.tether.depin.ui.viewmodel

import android.app.Application
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tether.depin.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val api = ApiClient.matchmakerApi

    private val _neighborhoodName = MutableStateFlow("Rajabazar")
    val neighborhoodName: StateFlow<String> = _neighborhoodName.asStateFlow()

    private val _cityName = MutableStateFlow("Kolkata")
    val cityName: StateFlow<String> = _cityName.asStateFlow()

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _locationLoaded = MutableStateFlow(false)
    val locationLoaded: StateFlow<Boolean> = _locationLoaded.asStateFlow()

    private val _latencyMs = MutableStateFlow(24)
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _activePeers = MutableStateFlow(3)
    val activePeers: StateFlow<Int> = _activePeers.asStateFlow()

    private val _isNodeActive = MutableStateFlow(true)
    val isNodeActive: StateFlow<Boolean> = _isNodeActive.asStateFlow()

    private val _signalQuality = MutableStateFlow("Excellent")
    val signalQuality: StateFlow<String> = _signalQuality.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    init {
        // Start polling VPS Oracle every 10 seconds
        startVpsPolling()
        // Start uptime counter
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isNodeActive.value) {
                    _uptimeSeconds.value += 1
                }
            }
        }
    }

    private fun startVpsPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val response = api.getNodeStatus()
                    if (response.isSuccessful) {
                        response.body()?.let { status ->
                            _isNodeActive.value = status.isActive
                            _activePeers.value = status.connectedPeers
                            _latencyMs.value = status.latencyMs
                            _signalQuality.value = when {
                                status.latencyMs < 30 -> "Excellent"
                                status.latencyMs < 60 -> "Good"
                                status.latencyMs < 100 -> "Fair"
                                else -> "Poor"
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Network error — keep last known values
                }
                delay(3_000) // Poll every 3 seconds for snappy live updates
            }
        }
    }

    fun fetchLocation() {
        viewModelScope.launch {
            try {
                val cancellationToken = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationToken.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        _latitude.value = location.latitude
                        _longitude.value = location.longitude
                        _locationLoaded.value = true
                        reverseGeocode(location.latitude, location.longitude)
                    }
                }.addOnFailureListener {
                    _locationLoaded.value = true
                }
            } catch (e: SecurityException) {
                _locationLoaded.value = true
            }
        }
    }

    fun formatUptime(): String {
        val s = _uptimeSeconds.value
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val secs = s % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${secs}s"
    }

    private fun reverseGeocode(lat: Double, lon: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(getApplication(), Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val addr = addresses[0]
                                _neighborhoodName.value = addr.subLocality
                                    ?: addr.locality
                                    ?: addr.adminArea
                                    ?: "Local"
                                _cityName.value = addr.locality ?: addr.adminArea ?: "Unknown"
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            _neighborhoodName.value = addr.subLocality
                                ?: addr.locality
                                ?: addr.adminArea
                                ?: "Local"
                            _cityName.value = addr.locality ?: addr.adminArea ?: "Unknown"
                        }
                    }
                } catch (_: Exception) {
                    // Keep defaults
                }
            }
        }
    }
}
