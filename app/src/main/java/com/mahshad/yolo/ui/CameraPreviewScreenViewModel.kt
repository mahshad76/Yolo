package com.mahshad.yolo.ui

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.lifecycle.ViewModel

class CameraPreviewScreenViewModel() : ViewModel() {
    val preview = Preview.Builder().build()
    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

}