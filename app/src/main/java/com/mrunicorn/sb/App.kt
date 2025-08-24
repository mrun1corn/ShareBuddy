package com.mrunicorn.sb

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mrunicorn.sb.data.AppDb
import com.mrunicorn.sb.data.Repository
import com.mrunicorn.sb.reminder.ReminderReceiver

class App : Application() {
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        // ✅ init Repository with your Room DB/DAO
        repo = Repository(this, AppDb.get(this).itemDao())

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
