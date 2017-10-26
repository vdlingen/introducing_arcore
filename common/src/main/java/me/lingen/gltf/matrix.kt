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

import android.opengl.Matrix

fun FloatArray.copyFrom(other: FloatArray): FloatArray {
    if (this.size != other.size) throw IllegalArgumentException("Different array size")
    System.arraycopy(other, 0, this, 0, this.size)
    return this
}

fun FloatArray.setIdentity(): FloatArray {
    Matrix.setIdentityM(this, 0)
    return this
}

fun FloatArray.setPerspective(fovy: Float, aspect: Float, zNear: Float, zFar: Float): FloatArray {
    Matrix.perspectiveM(this, 0, fovy, aspect, zNear, zFar)
    return this
}

fun FloatArray.setOrthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): FloatArray {
    Matrix.orthoM(this, 0, left, right, bottom, top, near, far)
    return this
}

fun FloatArray.setQuaternion(quat: FloatArray): FloatArray {
    if (quat.size != 4) throw IllegalArgumentException("Quaternion array not of size 4")

    val (x, y, z, w) = quat
    return this.setQuaternion(x, y, z, w)
}

fun FloatArray.setInverseOf(matrix: FloatArray): FloatArray {
    Matrix.invertM(this, 0, matrix, 0)
    return this
}

fun FloatArray.inverse(): FloatArray = FloatArray(16).setInverseOf(this)

fun FloatArray.setQuaternion(x: Float, y: Float, z: Float, w: Float): FloatArray {
    this.copyFrom(floatArrayOf (
            1.0f - 2.0f*y*y - 2.0f*z*z, 2.0f*x*y - 2.0f*z*w, 2.0f*x*z + 2.0f*y*w, 0.0f,
            2.0f*x*y + 2.0f*z*w, 1.0f - 2.0f*x*x - 2.0f*z*z, 2.0f*y*z - 2.0f*x*w, 0.0f,
            2.0f*x*z - 2.0f*y*w, 2.0f*y*z + 2.0f*x*w, 1.0f - 2.0f*x*x - 2.0f*y*y, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    ))
    return this
}

fun FloatArray.setProductOf(lhs: FloatArray, rhs: FloatArray): FloatArray {
    when {
        this.size == 16 && lhs.size == 16 && rhs.size == 16 ->
                Matrix.multiplyMM(this, 0, lhs, 0, rhs, 0)

        this.size == 4 && lhs.size == 16 && rhs.size == 4 ->
                Matrix.multiplyMV(this, 0, lhs, 0, rhs, 0)

        this.size == 3 && lhs.size == 16 && rhs.size == 3 -> {
            val out = FloatArray(4)
            val rhsv = floatArrayOf(rhs[0], rhs[1], rhs[2], 1f)

            Matrix.multiplyMV(out, 0, lhs, 0, rhsv, 0)

            this[0] = out[0] / out[3]
            this[1] = out[1] / out[3]
            this[2] = out[2] / out[3]
        }

        else -> throw IllegalArgumentException("Incompatible array sizes")
    }

    return this
}

fun FloatArray.translate(x: Float, y: Float, z: Float): FloatArray {
    Matrix.translateM(this, 0, x, y, z)
    return this
}

fun FloatArray.rotate(degrees: Float, x: Float, y: Float, z: Float): FloatArray {
    Matrix.rotateM(this, 0, degrees, x, y, z)
    return this
}

fun FloatArray.scale(x: Float, y: Float, z: Float): FloatArray {
    Matrix.scaleM(this, 0, x, y, z)
    return this
}

fun FloatArray.scale(scale: Float): FloatArray {
    Matrix.scaleM(this, 0, scale, scale, scale)
    return this
}

fun FloatArray.multiply(other: FloatArray): FloatArray {
    this.copyFrom(FloatArray(size).setProductOf(this, other))
    return this
}

fun FloatArray.toVec4() = when (this.size) {
    3 -> floatArrayOf(this[0], this[1], this[2], 1f)
    4 -> this
    else -> throw IllegalArgumentException("Incompatible array size")
}

fun FloatArray.toMat4() = when (this.size) {
    9 -> floatArrayOf(
            this[0], this[1], this[2], 0f,
            this[3], this[4], this[5], 0f,
            this[6], this[7], this[8], 0f,
            0f, 0f, 0f, 1f)
    16 -> this
    else -> throw IllegalArgumentException("Incompatible array size")
}