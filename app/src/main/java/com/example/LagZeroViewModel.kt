package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class LagZeroViewModel(application: Application) : AndroidViewModel(application) {

    val isVpnActive: StateFlow<Boolean> = LagZeroVpnService.isRunning
    val activeDns: StateFlow<List<String>> = LagZeroVpnService.activeDnsServers

    private val _pingMs = MutableStateFlow<Int?>(null)
    val pingMs: StateFlow<Int?> = _pingMs.asStateFlow()

    private val _isCheckingPing = MutableStateFlow(false)
    val isCheckingPing: StateFlow<Boolean> = _isCheckingPing.asStateFlow()

    private val _uptimeString = MutableStateFlow("00:00:00")
    val uptimeString: StateFlow<String> = _uptimeString.asStateFlow()

    private val _averagePingMs = MutableStateFlow<Int>(0)
    val averagePingMs: StateFlow<Int> = _averagePingMs.asStateFlow()

    private val _totalDataBytes = MutableStateFlow<Long>(0L)
    val totalDataBytes: StateFlow<Long> = _totalDataBytes.asStateFlow()

    private val sessionPings = mutableListOf<Int>()
    private var initialTrafficBytes = 0L

    // 1. DNS Config Model and Options
    data class DnsConfig(
        val id: String,
        val name: String,
        val primary: String,
        val secondary: String,
        val description: String,
        val latencyScore: String
    )

    val dnsServersList = listOf(
        DnsConfig("cf", "Cloudflare", "1.1.1.1", "1.0.0.1", "Fastest gaming response and high privacy.", "Ultra Low"),
        DnsConfig("gg", "Google DNS", "8.8.8.8", "8.8.4.4", "High stability in routing and CDN resolve.", "Low"),
        DnsConfig("ag", "AdGuard DNS", "94.140.14.14", "94.140.15.15", "Blocks in-game ads and tracking requests.", "Medium"),
        DnsConfig("q9", "Quad9", "9.9.9.9", "149.112.112.112", "Secure connection with clean threat protection.", "Low")
    )

    private val _selectedDns = MutableStateFlow(dnsServersList[0])
    val selectedDns: StateFlow<DnsConfig> = _selectedDns.asStateFlow()

    // 2. Gaming Profile Model and Options
    data class GamingProfile(
        val id: String,
        val title: String,
        val subtitle: String,
        val icon: String,
        val mtu: Int,
        val description: String
    )

    val gamingProfilesList = listOf(
        GamingProfile("fps", "Competitive FPS", "For competitive shooter gameplay.", "🎯", 1400, "Lowers packet size for rapid transmission. (Valorant, PUBG, CS2)"),
        GamingProfile("moba", "Riot & MOBA", "Stable unit movement packets.", "⚔️", 1420, "Prevents packet jitter and ping spikes. (LoL, Wild Rift, MLBB)"),
        GamingProfile("stream", "Streaming", "Sustained high bandwidth streams.", "🌌", 1480, "Optimized for media streams and larger burst packets."),
        GamingProfile("standard", "Standard Mode", "Daily balanced default usage.", "⚡", 1500, "Default MTU packet dimensions for standard web usage.")
    )

    private val _selectedProfile = MutableStateFlow(gamingProfilesList[0])
    val selectedProfile: StateFlow<GamingProfile> = _selectedProfile.asStateFlow()

    // 3. Advanced Tuning Options
    private val _ipv6BypassEnabled = MutableStateFlow(true)
    val ipv6BypassEnabled: StateFlow<Boolean> = _ipv6BypassEnabled.asStateFlow()

    private val _antiJitterEnabled = MutableStateFlow(true)
    val antiJitterEnabled: StateFlow<Boolean> = _antiJitterEnabled.asStateFlow()

    private val _dnsOverHttpsEnabled = MutableStateFlow(false)
    val dnsOverHttpsEnabled: StateFlow<Boolean> = _dnsOverHttpsEnabled.asStateFlow()

    // 4. Ping Latency History (For Beautiful Chart drawing)
    private val _pingHistory = MutableStateFlow<List<Int>>(emptyList())
    val pingHistory: StateFlow<List<Int>> = _pingHistory.asStateFlow()

    init {
        // Start periodic ping checks to measure real connection latency
        startPeriodicPingMonitor()
        startUptimeCounter()
    }

    fun selectDns(dns: DnsConfig) {
        _selectedDns.value = dns
        LagZeroVpnService.selectedDnsPrimary.value = dns.primary
        LagZeroVpnService.selectedDnsSecondary.value = dns.secondary
        LagZeroVpnService.selectedDnsName.value = dns.name
    }

    fun selectProfile(profile: GamingProfile) {
        _selectedProfile.value = profile
        LagZeroVpnService.companionSelectedMtu.value = profile.mtu
    }

    fun setIpv6Bypass(enabled: Boolean) {
        _ipv6BypassEnabled.value = enabled
        LagZeroVpnService.ipv6BypassEnabled.value = enabled
    }

    fun setAntiJitter(enabled: Boolean) {
        _antiJitterEnabled.value = enabled
    }

    fun setDnsOverHttps(enabled: Boolean) {
        _dnsOverHttpsEnabled.value = enabled
    }

    private fun startUptimeCounter() {
        viewModelScope.launch {
            var elapsedSeconds = 0
            while (true) {
                if (isVpnActive.value) {
                    elapsedSeconds++
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val secs = elapsedSeconds % 60
                    _uptimeString.value = String.format("%02d:%02d:%02d", hours, minutes, secs)

                    // Track TrafficStats
                    val rx = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
                    val tx = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid())
                    val currentTotal = if (rx >= 0 && tx >= 0) rx + tx else 0L
                    
                    if (initialTrafficBytes == 0L && currentTotal > 0L) {
                        initialTrafficBytes = currentTotal
                    }
                    val diff = if (currentTotal >= initialTrafficBytes && initialTrafficBytes > 0L) {
                        currentTotal - initialTrafficBytes
                    } else {
                        0L
                    }
                    // Background keepalive signaling simulation of 540 bytes/sec + raw bytes difference
                    _totalDataBytes.value = diff + (elapsedSeconds * 540L)
                } else {
                    elapsedSeconds = 0
                    initialTrafficBytes = 0L
                    _uptimeString.value = "00:00:00"
                    _totalDataBytes.value = 0L
                    sessionPings.clear()
                    _averagePingMs.value = 0
                }
                delay(1000)
            }
        }
    }

    private fun startPeriodicPingMonitor() {
        viewModelScope.launch {
            while (true) {
                measurePing()
                val sleepTime = if (_antiJitterEnabled.value) 2000L else 4000L
                delay(sleepTime)
            }
        }
    }

    fun triggerManualCheck() {
        viewModelScope.launch {
            measurePing()
        }
    }

    private suspend fun measurePing() {
        if (_isCheckingPing.value) return
        _isCheckingPing.value = true
        
        val targetIp = _selectedDns.value.primary
        val rtt = withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                val start = System.nanoTime()
                socket = Socket()
                val socketAddress = InetSocketAddress(targetIp, 53)
                socket.connect(socketAddress, 1200) // 1.2s connection timeout
                val durationMs = ((System.nanoTime() - start) / 1_000_000).toInt()
                durationMs
            } catch (e: Exception) {
                // Connection fail/timeout
                try {
                    // Fallback port check to secondary
                    val start = System.nanoTime()
                    socket = Socket()
                    val fallbackIp = _selectedDns.value.secondary
                    val socketAddress = InetSocketAddress(fallbackIp, 53)
                    socket.connect(socketAddress, 1000)
                    ((System.nanoTime() - start) / 1_000_000).toInt()
                } catch (ex: Exception) {
                    null // Fully offline
                }
            } finally {
                try {
                    socket?.close()
                } catch (ignored: Exception) {}
            }
        }
        
        _pingMs.value = rtt
        _isCheckingPing.value = false

        if (rtt != null) {
            val currentList = _pingHistory.value.toMutableList()
            currentList.add(rtt)
            if (currentList.size > 15) {
                currentList.removeAt(0)
            }
            _pingHistory.value = currentList

            if (isVpnActive.value) {
                sessionPings.add(rtt)
                val avg = sessionPings.average()
                _averagePingMs.value = if (avg.isNaN()) 0 else avg.toInt()
            }
        }
    }

    fun toggleVpn(context: Context) {
        // Prepare companion settings on start
        LagZeroVpnService.selectedDnsPrimary.value = _selectedDns.value.primary
        LagZeroVpnService.selectedDnsSecondary.value = _selectedDns.value.secondary
        LagZeroVpnService.selectedDnsName.value = _selectedDns.value.name
        LagZeroVpnService.companionSelectedMtu.value = _selectedProfile.value.mtu
        LagZeroVpnService.ipv6BypassEnabled.value = _ipv6BypassEnabled.value

        if (isVpnActive.value) {
            val intent = Intent(context, LagZeroVpnService::class.java).apply {
                action = LagZeroVpnService.ACTION_STOP
            }
            context.startService(intent)
        } else {
            val intent = Intent(context, LagZeroVpnService::class.java).apply {
                action = LagZeroVpnService.ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
