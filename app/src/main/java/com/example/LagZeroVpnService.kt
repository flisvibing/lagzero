package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

class LagZeroVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        private const val TAG = "LagZeroVpnService"
        
        const val ACTION_START = "com.example.lagzero.START"
        const val ACTION_STOP = "com.example.lagzero.STOP"
        
        val isRunning = MutableStateFlow(false)
        val activeDnsServers = MutableStateFlow<List<String>>(emptyList())

        // Configurable Live Custom Options
        val selectedDnsPrimary = MutableStateFlow("1.1.1.1")
        val selectedDnsSecondary = MutableStateFlow("1.0.0.1")
        val selectedDnsName = MutableStateFlow("Cloudflare")
        val companionSelectedMtu = MutableStateFlow(1400)
        val ipv6BypassEnabled = MutableStateFlow(true)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LagZeroVpnService Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand action: $action")
        
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning.value) {
            Log.d(TAG, "VPN is already running")
            return
        }

        try {
            createNotificationChannel()
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    2026,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                )
            } else {
                startForeground(2026, notification)
            }

            val pDns = selectedDnsPrimary.value
            val sDns = selectedDnsSecondary.value
            val activeMtu = companionSelectedMtu.value

            // Configure builder dynamically
            val builder = Builder()
                .setSession("LagZero DNS Optimization")
                .addAddress("10.8.0.2", 32)
                .addDnsServer(pDns)
                .addDnsServer(sDns)
                .addRoute("10.8.0.0", 24)
                .setMtu(activeMtu)
                
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.allowBypass()
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning.value = true
                activeDnsServers.value = listOf(pDns, sDns)
                Log.d(TAG, "LagZero VPN Tunnel Established to $pDns, $sDns with MTU $activeMtu")
            } else {
                Log.e(TAG, "Failed to establish VPN interface descriptor")
                stopVpn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception establishing VPN tunnel", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VpnService")
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        } finally {
            vpnInterface = null
        }
        
        isRunning.value = false
        activeDnsServers.value = emptyList()
        
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "LagZeroVpnService Destroyed")
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        Log.d(TAG, "VPN access revoked by system")
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "lagzero_dns_channel",
                "LagZero Latency Optimizer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active latency optimization details."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, LagZeroVpnService::class.java).apply {
            action = ACTION_STOP
        }
        
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, pendingFlags)

        val openActivityIntent = Intent(this, MainActivity::class.java)
        val openActivityPendingIntent = PendingIntent.getActivity(this, 0, openActivityIntent, pendingFlags)

        val dnsName = selectedDnsName.value
        val primaryIp = selectedDnsPrimary.value

        return NotificationCompat.Builder(this, "lagzero_dns_channel")
            .setContentTitle("LagZero Network Active")
            .setContentText("DNS routing optimized through $dnsName ($primaryIp)")
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentIntent(openActivityPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "STOP OPTIMIZATION",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
