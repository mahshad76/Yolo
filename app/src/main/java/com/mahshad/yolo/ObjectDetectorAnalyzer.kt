package com.mahshad.yolo

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.mahshad.yolo.ui.model.Detection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectDetectorAnalyzer @Inject constructor(private val classifier: Classifier) :
    ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    var onResultListener: ((List<Detection>) -> Unit)? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        classifier.onResultListener = this.onResultListener
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            classifier.classify(imageProxy.toBitmap())
            Log.d(
                "Analyzer",
                "Image format: ${imageProxy.format}, Size: ${imageProxy.width}x${imageProxy.height}"
            )
            imageProxy.close()
        } else {
            imageProxy.close()
        }
    }
}