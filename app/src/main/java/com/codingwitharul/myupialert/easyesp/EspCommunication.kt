package com.codingwitharul.myupialert.easyesp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface EspCommunication {
    suspend fun getAvailableNetworks(): List<EspWifiNetwork>
    suspend fun sendCredentials(ssid: String, password: String): Boolean
}

class AndroidEspCommunication : EspCommunication {
    private val baseUrl = "http://192.168.4.1"

    override suspend fun getAvailableNetworks(): List<EspWifiNetwork> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/networks")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val networks = mutableListOf<EspWifiNetwork>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    networks.add(
                        EspWifiNetwork(
                            ssid = obj.optString("ssid", ""),
                            rssi = obj.optInt("rssi", 0),
                            isSecure = obj.optBoolean("isSecure", true)
                        )
                    )
                }
                networks
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("EspCommunication", "Failed to GET networks from ESP32", e)
            emptyList()
        }
    }

    override suspend fun sendCredentials(ssid: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/credentials")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val postData = "ssid=${URLEncoder.encode(ssid, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}"
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
            }

            connection.responseCode in 200..299
        } catch (e: Exception) {
            Log.e("EspCommunication", "Failed to POST credentials to ESP32", e)
            false
        }
    }
}
