package com.ifindme.homelabmonitor

data class ServerStatus(
    val cpu: Double,
    val ram: Double,
    val disk: Double,
    val uptime: Long,
    val rxBytes: Long,
    val txBytes: Long
)
