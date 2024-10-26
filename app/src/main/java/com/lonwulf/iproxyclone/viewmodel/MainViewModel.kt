package com.lonwulf.iproxyclone.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonwulf.iproxyclone.NetworkRotator
import com.lonwulf.iproxyclone.ProxyConfiguration
import com.lonwulf.iproxyclone.repository.ProxyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val rotator: NetworkRotator,
    private val proxyRepository: ProxyRepository
) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val connectionState
        get() = _connectionState.asStateFlow()
    private val _currentIp = MutableStateFlow<String?>(null)
    val currentIp
        get() = _currentIp.asStateFlow()

    fun connectToProxy(config: ProxyConfiguration) = viewModelScope.launch {
        try {
            _connectionState.value = ConnectionState.Connecting
            proxyRepository.connect(config)
            checkCurrentIp()
            _connectionState.value = ConnectionState.Connected
            // Check connection speed
            val speed = proxyRepository.measureSpeed()
            Log.d("Proxy", "Connection speed: ${speed.speedMbps} Mbps")
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "IP rotation failed")
        }
    }

    fun rotateIp() = viewModelScope.launch {
        try {
            _connectionState.value = ConnectionState.Rotating
            rotator.rotateIP()
            checkCurrentIp()
            _connectionState.value = ConnectionState.Connected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "IP rotation failed")
        }
    }

    private suspend fun checkCurrentIp() {
        _currentIp.value = proxyRepository.getCurrentIp()
    }

    fun disconnectProxy() = viewModelScope.launch {
        try {
            _connectionState.value = ConnectionState.Disconnecting
            proxyRepository.disconnect()
            _currentIp.value = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Disconnection failed")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            proxyRepository.disconnect()
        }
    }
}

sealed class ConnectionState {
    object Disconnecting : ConnectionState()
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Rotating : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}