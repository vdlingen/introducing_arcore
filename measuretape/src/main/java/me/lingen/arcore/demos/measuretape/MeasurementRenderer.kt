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

package me.lingen.arcore.demos.measuretape

import android.graphics.Color
import android.opengl.GLES20.*
import android.opengl.Matrix
import com.google.ar.core.Pose
import me.lingen.arcore.rendering.checkGLError
import me.lingen.arcore.rendering.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private val vertexShader = """
    uniform mat4 uModelViewProjectionMatrix;

    attribute vec4 aPosition;
    attribute vec4 aColor;

    varying vec4 vColor;

    void main() {
        vColor = aColor;
        gl_Position = uModelViewProjectionMatrix * aPosition;
    }
"""

private val fragmentShader = """
    precision mediump float;

    varying vec4 vColor;

    void main() {
        gl_FragColor = vColor;
    }
"""

private val circleSegments = 16
private val circleRadius = 0.015f
private val vertexElementCount = 7

class MeasurementRenderer {

    private val programId: Int

    private val uModelViewProjectionMatrix: Int
    private val aPosition: Int
    private val aColor: Int

    private val vertexBuffer: FloatBuffer


    init {
        val maxLines = 2 * circleSegments + 3
        vertexBuffer = ByteBuffer.allocateDirect(maxLines * 2 * vertexElementCount * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        val vertexShaderId = loadShader(GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GL_FRAGMENT_SHADER, fragmentShader)

        programId = glCreateProgram()
        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)
        glUseProgram(programId)

        checkGLError("MeasurementRenderer", "program creation")

        uModelViewProjectionMatrix = glGetUniformLocation(programId, "uModelViewProjectionMatrix")
        aPosition = glGetAttribLocation(programId, "aPosition")
        aColor = glGetAttribLocation(programId, "aColor")

        checkGLError("MeasurementRenderer", "program parameters")
    }

    private val modelViewProjection = FloatArray(16)

    fun render(startPose: Pose, endPose: Pose?, finished: Boolean, viewMatrix: FloatArray, projectionMatrix: FloatArray): Float {

        checkGLError("MeasurementRender", "render start")

        glLineWidth(5f)

        val startColor = if (endPose != null) Color.BLUE else Color.GREEN
        val endColor = if (finished) Color.BLUE else Color.GREEN

        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, viewMatrix, 0)

        vertexBuffer.apply {
            rewind()

            addCircleSegments(startPose, vertexBuffer, startColor)

            put(startPose.tx())
            put(startPose.ty())
            put(startPose.tz())

            putColor(startColor)

            put(startPose.tx())
            put(startPose.ty() + 0.05f)
            put(startPose.tz())

            putColor(startColor)

            if (endPose != null) {
                put(startPose.tx())
                put(startPose.ty())
                put(startPose.tz())
                putColor(startColor)

                put(endPose.tx())
                put(endPose.ty())
                put(endPose.tz())
                putColor(endColor)

                put(endPose.tx())
                put(endPose.ty())
                put(endPose.tz())
                putColor(endColor)

                put(endPose.tx())
                put(endPose.ty() + 0.05f)
                put(endPose.tz())
                putColor(endColor)

                addCircleSegments(endPose, vertexBuffer, endColor)
            }

        }

        val count = vertexBuffer.position() / vertexElementCount;
        vertexBuffer.rewind()

        glUseProgram(programId)
        glEnableVertexAttribArray(aPosition)
        glEnableVertexAttribArray(aColor)
        glVertexAttribPointer(aPosition, 3, GL_FLOAT, false, vertexElementCount * 4, vertexBuffer)

        vertexBuffer.position(3)
        glVertexAttribPointer(aColor, 4, GL_FLOAT, false, vertexElementCount * 4, vertexBuffer)

        glUniformMatrix4fv(uModelViewProjectionMatrix, 1, false, modelViewProjection, 0)

        checkGLError("MeasurementRender", "before draw")

        glDrawArrays(GL_LINES, 0, count)

        checkGLError("MeasurementRender", "after draw")

        glDisableVertexAttribArray(aPosition)
        glDisableVertexAttribArray(aColor)

        if (endPose == null) return 0f

        val dx = endPose.tx() - startPose.tx()
        val dy = endPose.ty() - startPose.ty()
        val dz = endPose.tz() - startPose.tz()

        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    private fun addCircleSegments(pose: Pose, buffer: FloatBuffer, color: Int) {
        val increment = Math.PI * 2 / circleSegments

        var x = Math.sin(0.0) * circleRadius
        var z = Math.cos(0.0) * circleRadius

        for (i in (1..circleSegments)) {
            buffer.put(pose.tx() + x.toFloat())
            buffer.put(pose.ty())
            buffer.put(pose.tz() + z.toFloat())
            buffer.putColor(color)

            x = Math.sin(i * increment) * circleRadius
            z = Math.cos(i * increment) * circleRadius

            buffer.put(pose.tx() + x.toFloat())
            buffer.put(pose.ty())
            buffer.put(pose.tz() + z.toFloat())
            buffer.putColor(color)
        }
    }

    private fun FloatBuffer.putColor(color: Int) {
        put(Color.red(color) / 255f)
        put(Color.green(color) / 255f)
        put(Color.blue(color) / 255f)
        put(Color.alpha(color) / 255f)
    }
}