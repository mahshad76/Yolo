package com.mahshad.yolo.data.database

import androidx.room.Query
import com.mahshad.yolo.data.database.model.TrackerObjectEntity
import kotlinx.coroutines.flow.Flow

interface TrackerObjectDao {
    @Query("SELECT * FROM tracker_objects")
    fun getAll(): Flow<List<TrackerObjectEntity>>
}