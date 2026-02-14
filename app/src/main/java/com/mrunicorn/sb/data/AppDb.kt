package com.mrunicorn.sb.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Item::class], version = 3, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "sharebuddy.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
