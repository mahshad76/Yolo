package com.mahshad.yolo.ui

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahshad.yolo.ObjectDetectorAnalyzer
import com.mahshad.yolo.ui.model.Detection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class CameraPreviewScreenViewModel @Inject constructor(
    private val objectDetectorAnalyzer: ObjectDetectorAnalyzer
) :
    ViewModel() {
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest

    private val _detectedBoxes = MutableStateFlow<List<Detection>>(emptyList())
    val detectedBoxes: StateFlow<List<Detection>> = _detectedBoxes.asStateFlow()

    init {
        objectDetectorAnalyzer.onResultListener = { detectedBoxes: List<Detection> ->
            _detectedBoxes.value = detectedBoxes
        }
        viewModelScope.launch {
            detectedBoxes.collect { value ->
                Log.d("TAG", "detected boxes: ${value}")
            }
        }
    }

    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update { newSurfaceRequest }
        }
    }

    fun setAnalyzer() {
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), objectDetectorAnalyzer)
    }
}