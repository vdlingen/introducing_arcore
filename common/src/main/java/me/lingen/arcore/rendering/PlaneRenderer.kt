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

package me.lingen.arcore.rendering

import android.content.Context
import android.graphics.BitmapFactory
import com.google.ar.core.Plane
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.opengl.Matrix
import me.lingen.common.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private val vertexShader = """
    uniform mat4 uModelViewProjectionMatrix;

    attribute vec2 aPosition;

    varying vec2 vTexCoord;

    void main() {
        vTexCoord = aPosition;
        gl_Position = uModelViewProjectionMatrix * vec4(aPosition.x, 0, aPosition.y, 1);
    }
"""

private val fragmentShader = """
    precision mediump float;

    uniform sampler2D uTexture;

    varying vec2 vTexCoord;
    uniform vec4 uColor;

    void main() {
        vec4 color = texture2D(uTexture, vTexCoord);
        if (color.a < 0.1) discard;

        gl_FragColor = uColor * color;
    }
"""


class PlaneRenderer(context: Context) {

    private val programId: Int
    private val textureId: Int

    private val aPosition: Int
    private val uModelViewProjectionMatrix: Int
    private val uColor: Int

    private var vertexBuffer: FloatBuffer

    init {
        val textures = IntArray(1)
        glGenTextures(1, textures, 0)
        textureId = textures[0]

        glBindTexture(GL_TEXTURE_2D, textureId)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.plane_grid)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

        val vertexShaderId = loadShader(GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GL_FRAGMENT_SHADER, fragmentShader)

        programId = glCreateProgram()
        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)
        glUseProgram(programId)

        checkGLError("CAMERA", "program creation")

        aPosition = glGetAttribLocation(programId, "aPosition")
        uModelViewProjectionMatrix = glGetUniformLocation(programId, "uModelViewProjectionMatrix")
        uColor = glGetUniformLocation(programId, "uColor")

        checkGLError("CAMERA", "program parameters")

        vertexBuffer = ByteBuffer.allocateDirect(4 * 32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(0f)
            put(0f)
            position(0)
            limit(2)
        }
    }

    private val model = FloatArray(16)
    private val modelView = FloatArray(16)
    private val modelViewProjection = FloatArray(16)

    fun render(plane: Plane, viewMatrix: FloatArray, projectionMatrix: FloatArray) {

        if (plane.planePolygon == null) return
        updateVertexData(plane.planePolygon)

        plane.centerPose.toMatrix(model, 0)

        Matrix.multiplyMM(modelView, 0, viewMatrix, 0, model, 0)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelView, 0)

        glUseProgram(programId)

        glVertexAttribPointer(aPosition, 2, GL_FLOAT, false, 0, vertexBuffer)
        glEnableVertexAttribArray(aPosition)

        glUniformMatrix4fv(uModelViewProjectionMatrix, 1, false, modelViewProjection, 0)
        glUniform4f(uColor, 0f, 0f, 1f, 0.25f)

        glBindTexture(GL_TEXTURE_2D, textureId)
        glDrawArrays(GL_TRIANGLE_FAN, 0, vertexBuffer.limit() / 2)

        glDisableVertexAttribArray(aPosition)
    }

    private fun updateVertexData(polygon: FloatBuffer) {
        if (vertexBuffer.capacity() < polygon.remaining() + 4) {
            var size = vertexBuffer.capacity()
            while (size < polygon.remaining() + 4) {
                size *= 2
            }

            vertexBuffer = ByteBuffer.allocateDirect(4 * size).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(0f)
                put(0f)
                rewind()
            }
        }

        vertexBuffer.apply {
            rewind()
            limit(polygon.remaining() + 4)
            position(2)
            put(polygon)
            polygon.rewind()
            put(polygon.get())
            put(polygon.get())
            rewind()
            polygon.rewind()
        }
    }

}