package net.aucutt.circuits

import android.app.Application
import net.aucutt.circuits.sync.WearStatePublisher

class CircuitsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WearStatePublisher.getInstance(this)
    }
}
