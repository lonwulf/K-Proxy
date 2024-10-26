package com.lonwulf.iproxyclone.service

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.lonwulf.iproxyclone.ProxyConfiguration
import com.lonwulf.iproxyclone.TunnelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import javax.inject.Singleton

@Singleton
class ProxyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    private val proxyConfig = ProxyConfiguration()
    private val tunnelManager = TunnelManager()

    override fun onCreate() {
        super.onCreate()
    }

    private fun setupVpnService() {
        vpnInterface =
            Builder().addAddress("10.0.0.2", 32).addRoute("0.0.0.0", 0).addDnsServer("8.8.8.8")
                .setSession("ProxyVPN").establish()
        isRunning = true
        startTunnel()
    }

    private fun startTunnel() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isRunning) {
                try {
                    handleConnection()
                } catch (ex: Exception) {

                }
            }
        }
    }

    private fun handleConnection() {
        val tunnel = DatagramChannel.open()
        tunnel.connect(InetSocketAddress(proxyConfig.host, proxyConfig.port))
        protect(tunnel.socket())
        tunnelManager.startTunnelling(tunnel, vpnInterface)
    }
}