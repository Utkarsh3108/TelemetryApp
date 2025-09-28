package com.example.telemetryapp.compute

import kotlin.math.sqrt

object Convolution {
    const val SIZE = 256
    private val kernel = arrayOf(
        floatArrayOf(0.0625f, 0.125f, 0.0625f),
        floatArrayOf(0.125f,  0.25f,  0.125f),
        floatArrayOf(0.0625f, 0.125f, 0.0625f)
    )

    // Create deterministic input
    fun makeInput(): FloatArray {
        val arr = FloatArray(SIZE * SIZE)
        for (i in arr.indices) arr[i] = ((i and 0xFF).toFloat() / 255f)
        return arr
    }

    // Single-pass 3x3 convolution (returns new array)
    fun convolveSinglePass(input: FloatArray, w: Int = SIZE, h: Int = SIZE): FloatArray {
        val out = FloatArray(input.size)
        for (y in 1 until h - 1) {
            val rowOff = y * w
            for (x in 1 until w - 1) {
                var sum = 0f
                sum += input[rowOff + x - w - 1] * kernel[0][0]
                sum += input[rowOff + x - w]     * kernel[0][1]
                sum += input[rowOff + x - w + 1] * kernel[0][2]
                sum += input[rowOff + x - 1]     * kernel[1][0]
                sum += input[rowOff + x]         * kernel[1][1]
                sum += input[rowOff + x + 1]     * kernel[1][2]
                sum += input[rowOff + x + w - 1] * kernel[2][0]
                sum += input[rowOff + x + w]     * kernel[2][1]
                sum += input[rowOff + x + w + 1] * kernel[2][2]
                out[rowOff + x] = sum
            }
        }
        return out
    }

    // Repeated passes - simple implementation (allocates). For performance, consider buffer reuse.
    fun repeatedConvolve(inputBase: FloatArray, passes: Int): FloatArray {
        if (passes <= 0) return inputBase
        var cur = inputBase
        var next: FloatArray
        for (i in 0 until passes) {
            next = convolveSinglePass(cur)
            cur = next
        }
        return cur
    }

    // Optional small utility to compute mean/std for a float array
    fun summaryMeanStd(arr: FloatArray): Pair<Double, Double> {
        if (arr.isEmpty()) return 0.0 to 0.0
        val mean = arr.map { it.toDouble() }.average()
        val std = sqrt(arr.map { (it - mean) * (it - mean) }.average())
        return mean to std
    }
}
