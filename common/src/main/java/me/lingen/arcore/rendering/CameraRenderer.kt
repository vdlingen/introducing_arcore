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

import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private val vertexShader = """
    attribute vec4 aPosition;
    attribute vec2 aTexCoord;

    varying vec2 vTexCoord;

    void main() {
        gl_Position = aPosition;
        vTexCoord = aTexCoord;
    }
"""

private val fragmentShader = """
    #extension GL_OES_EGL_image_external : require
    precision mediump float;

    varying vec2 vTexCoord;
    uniform samplerExternalOES uTexture;

    void main() {
        gl_FragColor = texture2D(uTexture, vTexCoord);
    }
"""

class CameraRenderer(val textureId: Int) {

    private val programId: Int
    private val aPosition: Int
    private val aTexCoord: Int

    private val vertices: FloatBuffer
    private val texCoord: FloatBuffer
    private val texCoordTransformed: FloatBuffer

    init {
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        vertices = ByteBuffer.allocateDirect(4 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(
                    -1.0f, -1.0f, 0.0f,
                    -1.0f, +1.0f, 0.0f,
                    +1.0f, -1.0f, 0.0f,
                    +1.0f, +1.0f, 0.0f))
            position(0)
        }

        texCoord = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(
                    0.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f))
            position(0)
        }

        texCoordTransformed = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        val vertexShaderId = loadShader(GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GL_FRAGMENT_SHADER, fragmentShader)

        programId = glCreateProgram()
        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)
        glUseProgram(programId)

        checkGLError("CAMERA", "program creation")

        aPosition = glGetAttribLocation(programId, "aPosition")
        aTexCoord = glGetAttribLocation(programId, "aTexCoord")

        checkGLError("CAMERA", "program parameters")
    }

    fun render(frame: Frame) {
        if (frame.isDisplayRotationChanged)
            frame.transformDisplayUvCoords(texCoord, texCoordTransformed)

        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        glUseProgram(programId)

        glVertexAttribPointer(aPosition, 3, GL_FLOAT, false, 0, vertices)
        glVertexAttribPointer(aTexCoord, 2, GL_FLOAT, false, 0, texCoordTransformed)

        glEnableVertexAttribArray(aPosition)
        glEnableVertexAttribArray(aTexCoord)

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        glDisableVertexAttribArray(aPosition)
        glDisableVertexAttribArray(aTexCoord)

        glDepthMask(true)
        glEnable(GL_DEPTH_TEST)

        checkGLError("CAMERA", "render")
    }

}