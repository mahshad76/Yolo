package com.mahshad.yolo.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mahshad.yolo.data.database.model.TrackerObjectEntity

@Database(entities = [TrackerObjectEntity::class], version = 1, exportSchema = true)
abstract class TrackerObjectDataBase : RoomDatabase() {
    abstract fun trackerObjectDao(): TrackerObjectDao
}