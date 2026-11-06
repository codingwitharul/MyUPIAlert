package com.codingwitharul.myupialert.easyesp

data class DiscoveredDevice(
    val ssid: String,
    val macAddress: String,
    val signalStrength: Int
)
