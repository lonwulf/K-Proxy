package com.lonwulf.kproxy

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.delay
import javax.inject.Inject

class NetworkRotator @Inject constructor(private val ctx:Context){
    private val cM = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager= ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun rotateIP(){
        try {
            disableConnections()
            delay(1000)
            enableConnections()
            delay(2000)
        }catch (ex:Exception){
            throw SecurityException("")
        }
    }

    private fun disableConnections(){
        wifiManager.isWifiEnabled = false
        toggleMobileData(false)
    }

    private fun enableConnections(){
        toggleMobileData(true)
        wifiManager.isWifiEnabled = true
    }

    private fun toggleMobileData(enable:Boolean){
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        if (enable){
            cM.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback(){
                override fun onAvailable(network: Network) {
                    cM.bindProcessToNetwork(network)
                }
            })
        }else{
            cM.unregisterNetworkCallback(ConnectivityManager.NetworkCallback())
        }
    }
}