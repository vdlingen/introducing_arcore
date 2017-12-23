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

import com.google.ar.core.PointCloud
import com.google.ar.core.Pose
import android.opengl.GLES20.*
import android.opengl.Matrix

private val vertexShader = """
    uniform mat4 uModelViewMatrix;
    uniform mat4 uProjectionMatrix;

    attribute vec4 aPosition;

    varying vec4 vColor;

    vec4 colorForDepth(float depth) {
        vec4 red = vec4(1.0, 0.0, 0.0, 1.0);
        vec4 yellow = vec4(1.0, 1.0, 0.0, 1.0);
        vec4 green = vec4(0.0, 1.0, 0.0, 1.0);
        vec4 test = vec4(0.0, 1.0, 1.0, 1.0);
        vec4 blue = vec4(0.0, 0.0, 1.0, 1.0);

        float step1 = 0.0;
        float step2 = 0.5;
        float step3 = 1.0;
        float step4 = 1.5;
        float step5 = 2.0;

        vec4 color = mix(red, yellow, smoothstep(step1, step2, -depth));
        color = mix(color, green, smoothstep(step2, step3, -depth));
        color = mix(color, test, smoothstep(step3, step4, -depth));
        color = mix(color, blue, smoothstep(step4, step5, -depth));

        return color;
    }

    void main() {
        vec4 position = uModelViewMatrix * vec4(aPosition.xyz, 1.0);

        vColor = colorForDepth(position.z /position.w);

        gl_Position = uProjectionMatrix * position;
        gl_PointSize = 10.0;
    }
"""

private val fragmentShader = """
    precision mediump float;

    varying vec4 vColor;

    void main() {
        gl_FragColor = vColor;
    }
"""

private val BYTES_PER_FLOAT = java.lang.Float.SIZE / 8
private val FLOATS_PER_POINT = 4  // X,Y,Z,confidence.
private val BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT
private val INITIAL_BUFFER_POINTS = 1000

class PointCloudRenderer {

    private val programId: Int
    private val aPosition: Int
    private val uModelViewMatrix: Int
    private val uProjectionMatrix: Int

    private val vboId: Int
    private var vboSize: Int

    private var numPoints = 0


    private var lastPointCloud: PointCloud? = null

    init {
        checkGLError("PointCloudRenderer", "before create")

        val buffers = IntArray(1)
        glGenBuffers(1, buffers, 0)
        vboId = buffers[0]
        glBindBuffer(GL_ARRAY_BUFFER, vboId)

        vboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT
        glBufferData(GL_ARRAY_BUFFER, vboSize, null, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        checkGLError("PointCloudRenderer", "buffer alloc")

        val vertexShaderId = loadShader(GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GL_FRAGMENT_SHADER, fragmentShader)

        programId = glCreateProgram()
        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)
        glUseProgram(programId)

        checkGLError("PointCloudRenderer", "program creation")

        aPosition = glGetAttribLocation(programId, "aPosition")
        uModelViewMatrix = glGetUniformLocation(programId, "uModelViewMatrix")
        uProjectionMatrix = glGetUniformLocation(programId, "uProjectionMatrix")

        checkGLError("PointCloudRenderer", "program parameters")
    }

    private fun update(pointCloud: PointCloud) {
        if (lastPointCloud == pointCloud) return

        checkGLError("PointCloudRenerer", "before update")

        glBindBuffer(GL_ARRAY_BUFFER, vboId)

        numPoints = pointCloud.points.remaining() / FLOATS_PER_POINT
        if (numPoints * BYTES_PER_POINT > vboSize) {
            while (numPoints * BYTES_PER_POINT > vboSize) {
                vboSize *= 2
            }
            glBufferData(GL_ARRAY_BUFFER, vboSize, null, GL_DYNAMIC_DRAW)
        }

        glBufferSubData(GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT, pointCloud.points)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        checkGLError("PointCloudRenderer", "after update")

        lastPointCloud = pointCloud
    }

    private val modelMatrix = FloatArray(16)
    private val modelView = FloatArray(16)

    fun render(pointCloud: PointCloud, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        update(pointCloud)

//        pose.toMatrix(modelMatrix, 0)
//        Matrix.multiplyMM(modelView, 0, viewMatrix, 0, modelMatrix, 0)

        checkGLError("PointCloudRenderer", "before render")

        glUseProgram(programId)
        glEnableVertexAttribArray(aPosition)
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glVertexAttribPointer(aPosition, 4, GL_FLOAT, false, BYTES_PER_POINT, 0)
        glUniformMatrix4fv(uModelViewMatrix, 1, false, viewMatrix, 0)
        glUniformMatrix4fv(uProjectionMatrix, 1, false, projectionMatrix, 0)

        glDrawArrays(GL_POINTS, 0, numPoints)

        glDisableVertexAttribArray(aPosition)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        checkGLError("PointCloudRenderer", "after render")
    }

}