package com.lonwulf.kproxy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.lonwulf.kproxy.ProxyConfiguration
import com.lonwulf.kproxy.ProxyType
import com.lonwulf.kproxy.TunnelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import javax.inject.Singleton

@Singleton
class ProxyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var tunnelManager: TunnelManager? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ProxyVpnChannel"
        private const val NOTIFICATION_ID = 1
        private const val MTU = 1500
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val config = ProxyConfiguration(
            host = intent.getStringExtra("host") ?: "",
            port = intent.getIntExtra("port", 0),
            proxyType = ProxyType.valueOf(
                intent.getStringExtra("proxyType") ?: ProxyType.HTTP.name
            ),
            username = intent.getStringExtra("username"),
            password = intent.getStringExtra("password")
        )

        startForeground(NOTIFICATION_ID, createNotification())
        //init VPN
        serviceScope.launch {
            try {
                setupVpnService(config)
            } catch (e: Exception) {
                stopSelf()
            }
        }
        return START_STICKY
    }


    private fun setupVpnService(configuration: ProxyConfiguration) {
        try {
            vpnInterface = Builder().apply {
                addAddress("10.0.0.2", 32)
                addRoute("0.0.0.0", 0)
                addDnsServer("8.8.8.8")
                setMtu(MTU)
                setSession("ProxyVPN").establish()
            }.establish() ?: throw IllegalStateException("Failed to establish VPN interface")
            isRunning = true
            tunnelManager = TunnelManager()
            startTunnel(configuration)
        } catch (ex: Exception) {
            isRunning = false
            vpnInterface?.close()
            vpnInterface = null
            throw ex
        }
    }

    private fun startTunnel(configuration: ProxyConfiguration) {
        serviceScope.launch {
            while (isRunning) {
                try {
                    handleConnection(configuration)
                } catch (ex: Exception) {
                    //log error and retry after delay
                    delay(5000)
                }
            }
        }
    }

    private fun handleConnection(configuration: ProxyConfiguration) {
        val tunnel = DatagramChannel.open()
        tunnel.connect(InetSocketAddress(configuration.host, configuration.port))
        protect(tunnel.socket())
        tunnelManager?.startTunnelling(tunnel, vpnInterface)
    }

    private fun createNotification() = NotificationCompat.Builder(this).apply {
        createNotificationChannel()
        setContentTitle("Proxy VPN Service")
        setContentText("VPN is active")
        setSmallIcon(android.R.drawable.ic_lock_lock)
        priority = NotificationCompat.PRIORITY_LOW
        val stopIntent =
            PendingIntent.getService(
                this@ProxyVpnService, 0,
                Intent(this@ProxyVpnService, ProxyVpnService::class.java).apply {
                    action = "STOP_VPN"
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP VPN", stopIntent)
    }.build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Proxy VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the VPN service running"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        tunnelManager = null
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }

    override fun onRevoke() {
        onDestroy()
    }
}