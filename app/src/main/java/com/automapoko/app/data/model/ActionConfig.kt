package com.automapoko.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ActionConfig {
    
    @Serializable
    @SerialName("OpenApp")
    data class OpenApp(
        val packageName: String, 
        val appName: String
    ) : ActionConfig()

    @Serializable
    @SerialName("SetVolume")
    data class SetVolume(
        val streamType: Int, 
        val volumePercentage: Int
    ) : ActionConfig()
}
