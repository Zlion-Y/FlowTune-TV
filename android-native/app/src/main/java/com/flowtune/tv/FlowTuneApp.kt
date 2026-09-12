package com.flowtune.tv

import android.app.Application

class FlowTuneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        AppGraph.init(this)
    }

    companion object {
        lateinit var instance: FlowTuneApp
            private set
    }
}
