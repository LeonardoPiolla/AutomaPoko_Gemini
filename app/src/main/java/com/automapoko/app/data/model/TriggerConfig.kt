package com.automapoko.app.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class TriggerConfig {
    @Serializable
    data class Bluetooth(val deviceName: String) : TriggerConfig()

    @Serializable
    data class Wifi(val ssid: String) : TriggerConfig()

    @Serializable
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float = 100f,
        val transitionType: GeofenceTransitionType = GeofenceTransitionType.ENTER
    ) : TriggerConfig()
}
