package com.mrunicorn.sb.di

import android.content.Context
import com.mrunicorn.sb.data.AppDb
import com.mrunicorn.sb.data.ItemDao
import com.mrunicorn.sb.data.Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDb {
        return AppDb.get(context)
    }

    @Provides
    @Singleton
    fun provideItemDao(db: AppDb): ItemDao {
        return db.itemDao()
    }



    @Provides
    @Singleton
    fun provideRepository(@ApplicationContext context: Context, dao: ItemDao): Repository {
        return Repository(context, dao)
    }
}
