package `in`.app.connect.utils

import android.app.Application
import android.content.Intent

class ConnectAppApplication : Application() {
    private var isAppUsageTrackingServiceRunning = false

    override fun onCreate() {
        super.onCreate()
    }

    fun startAppUsageTracking() {
        if (!isAppUsageTrackingServiceRunning) {
            val serviceIntent = Intent(this, UsageTrackingService::class.java)
            startService(serviceIntent)
            isAppUsageTrackingServiceRunning = true
        }
    }

    fun stopAppUsageTracking() {
        if (isAppUsageTrackingServiceRunning) {
            val serviceIntent = Intent(this, UsageTrackingService::class.java)
            stopService(serviceIntent)
            isAppUsageTrackingServiceRunning = false
        }
    }
}
