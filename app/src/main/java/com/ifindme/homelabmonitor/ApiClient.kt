package com.ifindme.homelabmonitor

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(
    private val endpoint: String = "http://100.100.1.2:8181/api/status"
) {
    fun fetch(): ServerStatus {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2500
            readTimeout = 2500
            requestMethod = "GET"
            useCaches = false
        }
        return try {
            if (connection.responseCode !in 200..299) error("HTTP " + connection.responseCode)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val network = json.optJSONObject("network")
            ServerStatus(
                cpu = json.optDouble("cpu", 0.0),
                ram = json.optDouble("ram", 0.0),
                disk = json.optDouble("disk", 0.0),
                uptime = json.optLong("uptime", 0L),
                rxBytes = network?.optLong("rx", 0L) ?: 0L,
                txBytes = network?.optLong("tx", 0L) ?: 0L
            )
        } finally {
            connection.disconnect()
        }
    }
}
