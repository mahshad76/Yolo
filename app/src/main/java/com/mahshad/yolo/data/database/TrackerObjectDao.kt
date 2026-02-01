package com.mahshad.yolo.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mahshad.yolo.data.database.model.TrackerObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerObjectDao {
    @Query("SELECT * FROM tracker_objects")
    fun getAll(): Flow<List<TrackerObjectEntity>>

    @Update(TrackerObjectEntity::class)
    suspend fun partialUpdate(trackerObjectEntity: TrackerObjectEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trackerObject: TrackerObjectEntity): Long

    //    delete the tracker which haven't been seen for a period of time, indicating the back and
    //    forth movements of the camera
    @Delete
    suspend fun delete(trackerObject: TrackerObjectEntity): Int
}