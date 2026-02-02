package com.mahshad.yolo.data.database.di

import android.content.Context
import androidx.room.Room
import com.mahshad.yolo.data.database.TrackerObjectDataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideTrackerObjectDataBase(@ApplicationContext context: Context): TrackerObjectDataBase =
        Room.databaseBuilder(
            context, TrackerObjectDataBase::class.java,
            "tracker-object-database"
        )
            .build()
}