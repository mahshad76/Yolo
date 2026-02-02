package com.mahshad.yolo.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracker_objects")
data class TrackerObjectEntity(
    @PrimaryKey val label: String,
    @ColumnInfo(name = "bbox_left") val left: Float,
    @ColumnInfo(name = "bbox_top") val top: Float,
    @ColumnInfo(name = "bbox_right") val right: Float,
    @ColumnInfo(name = "bbox_bottom") val bottom: Float,
    @ColumnInfo("notice_time") val noticeTime: Double,
    @ColumnInfo("confidence_level") val confidenceLevel: Float,
    @ColumnInfo("object_count") val count: Int = 0
)
