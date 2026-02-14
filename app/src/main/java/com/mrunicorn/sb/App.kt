package com.mrunicorn.sb

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mrunicorn.sb.data.AppDb
import com.mrunicorn.sb.data.Repository
import com.mrunicorn.sb.reminder.ReminderReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var repo: Repository

    override fun onCreate() {
        super.onCreate()

        // (optional) reminder channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Share Buddy reminders"
                setShowBadge(false)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
