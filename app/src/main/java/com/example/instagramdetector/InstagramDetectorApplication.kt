package com.example.instagramdetector

import android.app.Application
import com.example.instagramdetector.datastore.AppStatsRepository
import com.example.instagramdetector.datastore.OverlaySelectionRepository

class InstagramDetectorApplication : Application() {

    lateinit var overlaySelectionRepository: OverlaySelectionRepository
        private set

    lateinit var appStatsRepository: AppStatsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        overlaySelectionRepository = OverlaySelectionRepository(this)
        appStatsRepository = AppStatsRepository(this)
    }
}
