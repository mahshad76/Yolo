package com.mahshad.yolo

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ObjectDetectorAnalyzer() : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // 1. Get the actual media image
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            // 2. This is where you'd convert mediaImage to a TensorImage for YOLO
            // For now, let's just log that we are receiving frames
            Log.d(
                "Analyzer",
                "Image format: ${imageProxy.format}, Size: ${imageProxy.width}x${imageProxy.height}"
            )

            // 3. IMPORTANT: When finished, you MUST close the imageProxy
            // If you don't, the analyzer will stop receiving new frames.
            imageProxy.close()
        } else {
            imageProxy.close()
        }
    }
}