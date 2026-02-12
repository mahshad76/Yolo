package com.mahshad.yolo.ui.model

import android.graphics.RectF

data class Detection(
    val boxN: RectF,
    val classId: Int,
    val confidence: Float,
    val noticeTime: Double,
)
