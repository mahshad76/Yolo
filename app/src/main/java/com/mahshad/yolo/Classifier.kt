package com.mahshad.yolo

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(private val context: Context) {
    private lateinit var interpreter: Interpreter
    private var inputImageWidth: Int
    private var inputImageHeight: Int

    init {
        val assetManager = context.assets
        val model = loadModelFile(assetManager, "yolov5s-fp16.tflite")
        val interpreter = Interpreter(model)
        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        this.interpreter = interpreter
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun bitmapToFloatBufferNHWC(bitmap: Bitmap): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height

        val buffer = ByteBuffer
            .allocateDirect(4 * width * height * 3)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Normalize to 0..1 (YOLO TFLite default)
        var idx = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val p = pixels[idx++]
                val r = ((p shr 16) and 0xFF) / 255f
                val g = ((p shr 8) and 0xFF) / 255f
                val b = (p and 0xFF) / 255f
                buffer.putFloat(r)
                buffer.putFloat(g)
                buffer.putFloat(b)
            }
        }

        buffer.rewind()
        return buffer
    }

    fun closeInterpreter() {
        this.interpreter.close()
    }

    fun classify(bitmap: Bitmap): String {
        val resizedImage = bitmap.scale(inputImageWidth, inputImageHeight)
        val byteBuffer = bitmapToFloatBufferNHWC(resizedImage)
        val outShape = interpreter.getOutputTensor(0).shape() // e.g. [1, 25200, 85]
        val n = outShape[1]
        val c = outShape[2]
        val output = Array(outShape[0]) { Array(n) { FloatArray(c) } }
        interpreter.run(byteBuffer, output)
        val result = output[0]
        // Find the index of the box with the highest confidence
        val maxIndex: Int = result.indices.maxByOrNull { result[it][4] } ?: -1

// Get the actual confidence value
        val confidence = result[maxIndex][4]

// Find which class (out of the 80+ classes) has the highest score for THAT box
// Assuming classes start at index 5
        val classScores = result[maxIndex].sliceArray(5 until c)
        val classId = classScores.indices.maxByOrNull { classScores[it] } ?: -1
        val classConfidence = classScores[classId]

        val resultString = "Box Index: $maxIndex\nClass ID: $classId\nConfidence: $classConfidence"
        Log.d("TAG", resultString)
        return resultString
    }
}