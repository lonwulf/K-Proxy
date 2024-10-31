package com.lonwulf.kproxy

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ProxyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) {
            setupCrashReporting()
        }
        setupLogging()
        registerNetworkCallback()
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    private fun setupCrashReporting() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, TAG.plus("Uncaught exception in thread ${thread.name}"))
        }
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun registerNetworkCallback() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Timber.d(TAG.plus("Network available"))
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Timber.d(TAG.plus("Network lost"))
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val unMetered =
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    val vpn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    Timber.d(
                        TAG.plus(" Network capabilities changed. UnMetered: $unMetered, VPN: $vpn")
                    )
                }
            })
    }

    companion object {
        private const val TAG = "ProxyApplication"
    }
}