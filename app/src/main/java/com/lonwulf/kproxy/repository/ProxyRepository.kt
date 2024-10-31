package com.lonwulf.kproxy.repository

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.lonwulf.kproxy.ProxyConfiguration
import com.lonwulf.kproxy.ProxyType
import com.lonwulf.kproxy.domain.model.SpeedTestResult
import com.lonwulf.kproxy.service.ProxyVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ProxyRepository @Inject constructor(private val ctx: Context) {

    private var proxyClient: OkHttpClient? = null
    private var currentConfig: ProxyConfiguration? = null

    companion object {
        private const val CONNECTION_TIMEOUT = 30L
        private const val BUFFER_SIZE = 8192
        private const val IP_CHECK_URL = "https://api.ipify.org?format=json"
        private const val SPEED_TEST_URL =
            "https://speed.cloudflare.com/__down?bytes=25000000" // 25MB file for speed test
    }

    suspend fun connect(config: ProxyConfiguration) {
        withContext(Dispatchers.IO) {
            try {
                // Store current configuration
                currentConfig = config
                // Set up proxy client
                setupProxyClient(config)
                // Test connection
                testConnection()
                // Start VPN service
                startVpnService(config)

            } catch (e: Exception) {
                throw SecurityException("Failed to connect: ${e.message}")
            }
        }
    }

    private fun setupProxyClient(proxyConfiguration: ProxyConfiguration) {
        val proxy = when (proxyConfiguration.proxyType) {
            ProxyType.HTTP -> Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress(proxyConfiguration.host, proxyConfiguration.port)
            )

            ProxyType.SOCKS5 -> Proxy(
                Proxy.Type.SOCKS,
                InetSocketAddress(proxyConfiguration.host, proxyConfiguration.port)
            )
        }

        val builder = OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })

        addCertificatePinningIfNeeded(builder, proxyConfiguration)

        // Add authentication if credentials are provided
        proxyConfiguration.username?.let { username ->
            proxyConfiguration.password?.let { password ->
                builder.proxyAuthenticator { _, response ->
                    response.request.newBuilder()
                        .header(
                            "Proxy-Authorization",
                            okhttp3.Credentials.basic(username, password)
                        )
                        .build()
                }
            }
        }
        proxyClient = builder.build()
    }

    private fun addCertificatePinningIfNeeded(
        builder: OkHttpClient.Builder,
        config: ProxyConfiguration
    ) {
        config.certificateFingerprint?.let { fingerprint ->
            builder.certificatePinner(
                CertificatePinner.Builder()
                    .add(config.host, "sha256/$fingerprint")
                    .build()
            )
        }
    }

    private fun testConnection() {
        val request = Request.Builder()
            .url(IP_CHECK_URL)
            .build()

        val response = proxyClient?.newCall(request)?.execute()
            ?: throw SecurityException("Proxy client not initialized")

        if (!response.isSuccessful) {
            throw SecurityException("Connection test failed: ${response.code}")
        }
    }

    private suspend fun startVpnService(proxyConfiguration: ProxyConfiguration) {
        withContext(Dispatchers.Main) {
            val vpnIntent = VpnService.prepare(ctx)
            if (vpnIntent != null) {
                throw SecurityException("VPN permission not granted")
            }

            val serviceIntent = Intent(ctx, ProxyVpnService::class.java).apply {
                putExtra("host", proxyConfiguration.host)
                putExtra("port", proxyConfiguration.port)
                putExtra("proxyType", proxyConfiguration.proxyType.name)
                proxyConfiguration.username?.let { putExtra("username", it) }
                proxyConfiguration.password?.let { putExtra("password", it) }
            }
            ctx.startService(serviceIntent)
        }
    }

    suspend fun getCurrentIp(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(IP_CHECK_URL)
                .build()

            proxyClient?.newCall(request)?.execute()?.use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                        ?: throw SecurityException("Empty response")
                } else {
                    throw SecurityException("Failed to get IP: ${response.code}")
                }
            } ?: throw SecurityException("Proxy client not initialized")
        } catch (e: Exception) {
            throw SecurityException("Failed to get current IP: ${e.message}")
        }
    }

    suspend fun measureSpeed(): SpeedTestResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(SPEED_TEST_URL)
                .build()

            val startTime = System.currentTimeMillis()
            var bytesRead = 0L

            proxyClient?.newCall(request)?.execute()?.use { response ->
                if (!response.isSuccessful) {
                    throw SecurityException("Speed test failed: ${response.code}")
                }

                response.body?.let { body ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytes: Int

                    while (body.source().read(buffer).also { bytes = it } != -1) {
                        bytesRead += bytes
                    }
                }
            }

            val duration = (System.currentTimeMillis() - startTime) / 1000.0 // seconds
            val speedMbps = (bytesRead * 8.0 / 1_000_000.0) / duration // Mbps

            SpeedTestResult(
                bytesTransferred = bytesRead,
                durationSeconds = duration,
                speedMbps = speedMbps
            )
        } catch (e: Exception) {
            throw SecurityException("Speed test failed: ${e.message}")
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            // Stop VPN service
            ctx.stopService(Intent(ctx, ProxyVpnService::class.java))

            // Clear current configuration
            currentConfig = null

            // Close proxy client
            proxyClient?.dispatcher?.executorService?.shutdown()
            proxyClient?.connectionPool?.evictAll()
            proxyClient = null

        } catch (e: Exception) {
            throw SecurityException("Failed to disconnect: ${e.message}")
        }
    }

    suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            proxyClient != null && testConnection().let { true }
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentConfig(): ProxyConfiguration? = currentConfig
}

