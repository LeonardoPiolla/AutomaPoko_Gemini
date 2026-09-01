package com.automapoko.app

import android.app.Application

class AutomapokoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: AutomapokoApp
            private set
    }
}
