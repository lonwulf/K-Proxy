package com.lonwulf.kproxy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class NetworkRotator @Inject constructor(private val ctx: Context) {
    private val cM = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun rotateIP() {
        try {
            toggleConnection(false)
            delay(1000)
            toggleConnection(true)
            delay(2000)
        } catch (ex: Exception) {
            throw SecurityException("")
        }
    }

    private fun toggleConnection(enable: Boolean) {
        try {
            toggleMobileData(enable)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {

                    val settingsIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(settingsIntent)
                }

                else -> {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = enable
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle connection")
        }
    }

    private fun toggleMobileData(enable: Boolean) {
        try {
            if (ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.CHANGE_NETWORK_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("Missing MODIFY_PHONE_STATE permission")
                return
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            if (enable) {
                cM.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        cM.bindProcessToNetwork(network)
                    }
                })
            } else {
                cM.unregisterNetworkCallback(ConnectivityManager.NetworkCallback())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle mobile data")
        }

    }
}