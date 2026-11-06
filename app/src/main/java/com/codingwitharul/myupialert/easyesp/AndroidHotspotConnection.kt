package com.codingwitharul.myupialert.easyesp

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.net.wifi.WifiManager
import android.net.wifi.WifiConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidHotspotConnection(
    context: Context
) : HotspotConnection {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    override suspend fun connectToHotspot(ssid: String, password: String?): Boolean {
        // Cancel previous connection if any
        disconnect()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectViaSpecifier(ssid, password)
        } else {
            connectViaWifiManager(ssid, password)
        }
    }

    override suspend fun disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            networkCallback?.let {
                try {
                    connectivityManager.unregisterNetworkCallback(it)
                } catch (e: Exception) {
                    // Ignore if already unregistered
                }
                networkCallback = null
            }
        } else {
            // Revert changes or disconnect via WifiManager
            @Suppress("DEPRECATION")
            wifiManager.disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectViaSpecifier(ssid: String, password: String?): Boolean =
        suspendCancellableCoroutine { continuation ->
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            val specifierBuilder = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)

            if (!password.isNullOrBlank()) {
                specifierBuilder.setWpa2Passphrase(password)
            }

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifierBuilder.build())
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // Bind process to this network so all HTTP requests go through it
                    connectivityManager.bindProcessToNetwork(network)
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onUnavailable() {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            networkCallback = callback

            continuation.invokeOnCancellation {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                    networkCallback = null
                } catch (e: Exception) {}
            }

            connectivityManager.requestNetwork(request, callback)
        }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun connectViaWifiManager(ssid: String, password: String?): Boolean {
        // Legacy connection for API < 29
        val wifiConfig = WifiConfiguration().apply {
            this.SSID = "\"$ssid\""
            if (!password.isNullOrBlank()) {
                this.preSharedKey = "\"$password\""
            } else {
                this.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
        }

        val netId = wifiManager.addNetwork(wifiConfig)
        if (netId == -1) return false

        wifiManager.disconnect()
        val enabled = wifiManager.enableNetwork(netId, true)
        val reconnected = wifiManager.reconnect()
        
        if (enabled && reconnected) {
            bindToCurrentWifi()
        }
        
        return enabled && reconnected
    }

    @SuppressLint("MissingPermission")
    override fun bindToCurrentWifi() {
        // Find the currently active Wi-Fi network and bind the process to it
        @Suppress("DEPRECATION")
        val networks = connectivityManager.allNetworks
        for (network in networks) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                connectivityManager.bindProcessToNetwork(network)
                break
            }
        }
    }
}
