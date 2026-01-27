package com.mahshad.yolo

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(private val context: Context) {
    private var interpreter: Interpreter
    private var inputImageWidth: Int
    private var inputImageHeight: Int
    val detections = mutableListOf<Detection>()
    val confidenceThreshold = 0.7f

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

    private fun closeInterpreter() {
        this.interpreter.close()
    }

    fun classify(bitmap: Bitmap) {
        val resizedImage = bitmap.scale(inputImageWidth, inputImageHeight)
        val byteBuffer = bitmapToFloatBufferNHWC(resizedImage)
        val outShape = interpreter.getOutputTensor(0).shape()
        val n = outShape[1]
        val c = outShape[2]
        val output = Array(outShape[0]) { Array(n) { FloatArray(c) } }
        interpreter.run(byteBuffer, output)
        val result = output[0]
        for (i in 0 until result.size) {
            val confidence = result[i][4]

            if (confidence > confidenceThreshold) {
                val cx = result[i][0]
                val cy = result[i][1]
                val w = result[i][2]
                val h = result[i][3]
                val left = cx - (w / 2)
                val top = cy - (h / 2)
                val right = cx + (w / 2)
                val bottom = cy + (h / 2)
                val box = RectF(
                    left * bitmap.width,
                    top * bitmap.height,
                    right * bitmap.width,
                    bottom * bitmap.height
                )
                val classScores = result[i].sliceArray(5 until c)
                val classId = classScores.indices.maxByOrNull { classScores[it] } ?: -1
                detections.add(Detection(box, classId, confidence))
            }
        }
        Log.d("TAG", "classify: detection result ${detections.toString()} ")
        closeInterpreter()
    }
}

data class Detection(
    val boundingBox: RectF,
    val classId: Int,
    val confidence: Float
)