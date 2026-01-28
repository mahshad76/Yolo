package com.mahshad.yolo

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CameraAnalyzerFlow {
    fun getFrameFlow(imageAnalysis: ImageAnalysis): Flow<ImageProxy> = callbackFlow {

        imageAnalysis.setAnalyzer(Runnable::run) { imageProxy ->
            // Try to send the frame to the Flow.
            // If the flow is busy, it drops the frame (backpressure).
            trySend(imageProxy)
        }

        // Keep the flow open until the lifecycle is destroyed
        awaitClose {
            imageAnalysis.clearAnalyzer()
        }
    }
}