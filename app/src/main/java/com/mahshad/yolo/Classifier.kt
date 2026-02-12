package com.mahshad.yolo

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.scale
import com.mahshad.yolo.ui.model.Detection
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Classifier @Inject constructor(@ApplicationContext private val context: Context) {
    private var interpreter: Interpreter
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
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY, startOffset,
            declaredLength
        )
    }

    private fun bitmapToFloatBufferNHWC(bitmap: Bitmap): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val buffer = ByteBuffer
            .allocateDirect(4 * width * height * 3)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
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

    fun iou(a: RectF, b: RectF): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)
        val interW = (right - left).coerceAtLeast(0f)
        val interH = (bottom - top).coerceAtLeast(0f)
        val interArea = interW * interH
        val areaA = (a.right - a.left).coerceAtLeast(0f) * (a.bottom - a.top)
            .coerceAtLeast(0f)
        val areaB = (b.right - b.left).coerceAtLeast(0f) * (b.bottom - b.top)
            .coerceAtLeast(0f)
        val union = areaA + areaB - interArea
        return if (union <= 0f) 0f else interArea / union
    }

    fun nonMaxSuppression(
        dets: List<Detection>,
        iouThreshold: Float = 0.45f
    ): List<Detection> {
        val sorted = dets.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)
            val it = sorted.iterator()
            while (it.hasNext()) {
                val d = it.next()
                if (d.classId == best.classId && iou(d.boxN, best.boxN) > iouThreshold) {
                    it.remove()
                }
            }
        }
        return kept
    }

    fun classify(bitmap: Bitmap): List<Detection> {
        val detections = mutableListOf<Detection>()
        val resized = bitmap.scale(inputImageWidth, inputImageHeight)
        val input = bitmapToFloatBufferNHWC(resized)
        val outShape = interpreter.getOutputTensor(0).shape()
        val n = outShape[1]
        val c = outShape[2]
        val output = Array(1) { Array(n) { FloatArray(c) } }
        interpreter.run(input, output)
        val result = output[0]
        val scoreThreshold = 0.15f
        var maxObj = 0f
        var maxProb = 0f
        var maxScore = 0f
        for (row in result) {
            val obj = row[4]
            if (obj > maxObj) maxObj = obj
            if (obj <= 0f) continue
            var bestClass = -1
            var bestProb = 0f
            for (cls in 0 until (c - 5)) {
                val p = row[5 + cls]
                if (p > bestProb) {
                    bestProb = p
                    bestClass = cls
                }
            }
            if (bestProb > maxProb) maxProb = bestProb
            val score = obj * bestProb
            if (score > maxScore) maxScore = score
            if (score < scoreThreshold) continue
            val cx = row[0]
            val cy = row[1]
            val w = row[2]
            val h = row[3]
            val left = (cx - w / 2f).coerceIn(0f, 1f)
            val top = (cy - h / 2f).coerceIn(0f, 1f)
            val right = (cx + w / 2f).coerceIn(0f, 1f)
            val bottom = (cy + h / 2f).coerceIn(0f, 1f)
            val boxN = RectF(left, top, right, bottom)
            detections.add(Detection(boxN, bestClass, score))
        }
        val sorted = detections.sortedByDescending { it.confidence }
        val topK = sorted.take(200)
        val final = nonMaxSuppression(topK, 0.45f)
            .sortedByDescending { it.confidence }
        for (d in final) {
            Log.d(
                "YOLO",
                "DETECTION: classId=${d.classId}, confidence=${d.confidence}, boxN=${d.boxN}"
            )
        }
        return final
    }
}

