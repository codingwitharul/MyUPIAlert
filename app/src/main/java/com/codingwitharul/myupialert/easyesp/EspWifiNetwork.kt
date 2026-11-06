package com.codingwitharul.myupialert.easyesp

data class EspWifiNetwork(
    val ssid: String,
    val rssi: Int = 0,
    val isSecure: Boolean = true
)
