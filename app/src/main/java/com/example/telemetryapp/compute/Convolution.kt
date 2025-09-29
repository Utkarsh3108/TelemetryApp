package com.example.telemetryapp.compute

import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min

object Convolution {
    private const val SIZE = 256
    private val kernel = arrayOf(
        floatArrayOf(0.0f, 0.25f, 0.0f),
        floatArrayOf(0.25f, 0.0f, 0.25f),
        floatArrayOf(0.0f, 0.25f, 0.0f)
    )

    // Reusable buffers to avoid allocation
    private val bufferA = Array(SIZE) { FloatArray(SIZE) }
    private val bufferB = Array(SIZE) { FloatArray(SIZE) }

    fun makeInput(): Array<FloatArray> {
        val input = Array(SIZE) { FloatArray(SIZE) }
        for (i in 0 until SIZE) for (j in 0 until SIZE) input[i][j] = (i * j % 256).toFloat()
        return input
    }

    fun repeatedConvolve(input: Array<FloatArray>, repeat: Int): Array<FloatArray> {
        val src = bufferA
        val dst = bufferB
        // Copy input to bufferA
        for (i in 0 until SIZE) System.arraycopy(input[i], 0, src[i], 0, SIZE)

        var inBuf = src
        var outBuf = dst
        repeat(repeat) {
            for (i in 1 until SIZE - 1) {
                val rowPrev = inBuf[i - 1]
                val rowCurr = inBuf[i]
                val rowNext = inBuf[i + 1]
                val outRow = outBuf[i]
                for (j in 1 until SIZE - 1) {
                    outRow[j] =
                        rowPrev[j - 1] * kernel[0][0] + rowPrev[j] * kernel[0][1] + rowPrev[j + 1] * kernel[0][2] +
                                rowCurr[j - 1] * kernel[1][0] + rowCurr[j] * kernel[1][1] + rowCurr[j + 1] * kernel[1][2] +
                                rowNext[j - 1] * kernel[2][0] + rowNext[j] * kernel[2][1] + rowNext[j + 1] * kernel[2][2]
                }
            }
            // Swap buffers
            val tmp = inBuf
            inBuf = outBuf
            outBuf = tmp
        }
        return inBuf
    }

    fun summaryMeanStd(data: Array<FloatArray>): Pair<Double, Double> {
        var sum = 0.0
        var count = 0
        for (row in data) {
            for (v in row) {
                sum += v
                count++
            }
        }
        val mean = sum / count
        var sumSq = 0.0
        for (row in data) for (v in row) sumSq += (v - mean) * (v - mean)
        return mean to kotlin.math.sqrt(sumSq / count)
    }
}
