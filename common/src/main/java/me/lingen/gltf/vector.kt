/*
 * Copyright 2017 Ronald van der Lingen. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.lingen.gltf

infix fun FloatArray.dot(other: FloatArray): Float {
    if (size != other.size) throw IllegalArgumentException("Vectors not of same size")

    var result = 0f
    for (i in 0 until size) {
        result += this[i] * other[i]
    }

    return result
}

infix fun FloatArray.cross(other: FloatArray) = floatArrayOf(
        this[1] * other[2] - other[1] * this[2],
        this[2] * other[0] - other[2] * this[0],
        this[0] * other[1] - other[0] * this[1])

operator fun FloatArray.minus(other: FloatArray): FloatArray {
    if (size != other.size) throw IllegalArgumentException("Vectors not of same size")

    val result = FloatArray(size)
    for (i in 0 until size) {
        result[i] = this[i] - other[i]
    }
    return result
}

operator fun FloatArray.plus(other: FloatArray): FloatArray {
    if (size != other.size) throw IllegalArgumentException("Vectors not of same size")

    val result = FloatArray(size)
    for (i in 0 until size) {
        result[i] = this[i] + other[i]
    }
    return result
}

class BoundingBox {

    var empty: Boolean = true
        private set

    val min = FloatArray(3)
    val max = FloatArray(3)

    fun expand(point: FloatArray) {
        if (empty) {
            for (i in 0 until 3) {
                min[i] = point[i]
                max[i] = point[i]
            }
            empty = false
        } else {
            for (i in 0 until 3) {
                if (point[i] < min[i]) min[i] = point[i]
                if (point[i] > max[i]) max[i] = point[i]
            }
        }
    }

    fun expand(bbox: BoundingBox) {
        if (!bbox.empty) {
            expand(bbox.min)
            expand(bbox.max)
        }
    }

    fun expand(size: Float) {
        for (i in 0 until 3) {
            min[i] -= size
            max[i] += size
        }
        empty = false
    }

    fun transformed(matrix: FloatArray): BoundingBox {
        val result = BoundingBox()

        if (!empty) {
            val transformedPoint = FloatArray(3)
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(min[0], min[1], min[2])))
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(min[0], min[1], max[2])))
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(min[0], max[1], min[2])))
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(min[0], max[1], max[2])))
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(max[0], min[1], min[2])))
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(max[0], min[1], max[2])))
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(max[0], max[1], min[2])))
            result.expand(transformedPoint.setProductOf(matrix, floatArrayOf(max[0], max[1], max[2])))
        }

        return result
    }
}