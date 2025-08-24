package com.mrunicorn.sb

import android.app.Application
import com.mrunicorn.sb.data.AppDb
import com.mrunicorn.sb.data.Repository

class App : Application() {
    lateinit var repo: Repository
        private set
    override fun onCreate() {
        super.onCreate()
        val db = AppDb.get(this)
        repo = Repository(this, db.itemDao())
    }
}
