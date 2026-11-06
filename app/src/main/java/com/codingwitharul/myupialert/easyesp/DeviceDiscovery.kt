package com.codingwitharul.myupialert.easyesp

import kotlinx.coroutines.flow.Flow

interface DeviceDiscovery {
    fun startScanning(): Flow<List<DiscoveredDevice>>
    fun stopScanning()
}
