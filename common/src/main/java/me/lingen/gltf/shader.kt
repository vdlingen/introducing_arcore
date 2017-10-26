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

import android.opengl.GLES20.*
import android.util.Log
import java.nio.ByteBuffer

private val vertexShaderSrc = """
    attribute vec4 aPosition;
    attribute vec3 aNormal;
    attribute vec2 aTexCoord0;

    uniform mat4 uCameraProjection;
    uniform mat4 uCameraMatrix;

    uniform mat4 uModelViewMatrix;

    varying vec3 vPosition;
    varying vec3 vNormal;
    varying vec3 vNormalCamera;
    varying vec2 vTexCoord0;

    void main() {
        vec4 position = uCameraMatrix * uModelViewMatrix * aPosition;
        vPosition = vec3(position) / position.w;

        vNormal = vec3(uModelViewMatrix * vec4(aNormal, 0.0));
        vNormalCamera = vec3(uCameraMatrix * vec4(vNormal, 0.0));
        vTexCoord0 = aTexCoord0;

        gl_Position = uCameraProjection * position;
    }
    """.trimIndent()

private val fragmentShaderSrc = """
    precision mediump float;

    uniform vec4 uBaseColorFactor;
    uniform bool uBaseColorTextureEnabled;
    uniform sampler2D uBaseColorTexture;
    uniform float uLightIntensity;

    varying vec3 vPosition;
    varying vec3 vNormal;
    varying vec3 vNormalCamera;
    varying vec2 vTexCoord0;

    void main() {
        float diffuse = 0.5 + max(dot(normalize(vNormalCamera), vec3(0, 0, 1)), 0.1) / 2.0;
        vec4 color = uBaseColorFactor;

        if (uBaseColorTextureEnabled) {
             color = color * texture2D(uBaseColorTexture, vTexCoord0);
        }

        gl_FragColor = uLightIntensity * diffuse * color;
    }
    """.trimIndent()



class ShaderProgram {
    private var programId = glCreateProgram()

    private var aPosition = -1
    private var aNormal = -1
    private var aTexCoord0 = -1
    private var uCameraProjection = -1
    private var uCameraMatrix = -1
    private var uModelViewMatrix = -1
    private var uBaseColorFactor = -1
    private var uBaseColorTextureEnabled = -1
    private var uBaseColorTexture = -1
    private var uLightIntensity = -1

    init {
        val status = IntArray(1)

        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, vertexShaderSrc)
        glCompileShader(vertexShader)
        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("ShaderProgram", "Failed to compile vertex shader:\n${glGetShaderInfoLog(vertexShader)}")
        }

        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, fragmentShaderSrc)
        glCompileShader(fragmentShader)

        glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("ShaderProgram", "Failed to compile fragment shader:\n${glGetShaderInfoLog(fragmentShader)}")
        }

        programId = glCreateProgram()
        glAttachShader(programId, vertexShader)
        glAttachShader(programId, fragmentShader)
        glLinkProgram(programId)

        glGetProgramiv(programId, GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("ShaderProgram", "Failed to link shader program:\n${glGetProgramInfoLog(programId)}")
        }

        glUseProgram(programId)

        aPosition = glGetAttribLocation(programId, "aPosition")
        aNormal = glGetAttribLocation(programId, "aNormal")
        aTexCoord0 = glGetAttribLocation(programId, "aTexCoord0")
        uCameraProjection = glGetUniformLocation(programId, "uCameraProjection")
        uCameraMatrix = glGetUniformLocation(programId, "uCameraMatrix")
        uModelViewMatrix = glGetUniformLocation(programId, "uModelViewMatrix")
        uBaseColorFactor = glGetUniformLocation(programId, "uBaseColorFactor")
        uBaseColorTextureEnabled = glGetUniformLocation(programId, "uBaseColorTextureEnabled")
        uBaseColorTexture = glGetUniformLocation(programId, "uBaseColorTexture")
        uLightIntensity = glGetUniformLocation(programId, "uLightIntensity")
    }

    fun use() {
        glUseProgram(programId)
    }

    var cameraProjection = FloatArray(16)
        set(value) {
            field = value
            glUniformMatrix4fv(uCameraProjection, 1, false, value, 0)
        }

    var cameraMatrix = FloatArray(16)
        set(value) {
            field = value
            glUniformMatrix4fv(uCameraMatrix, 1, false, value, 0)
        }

    var modelViewMatrix = FloatArray(16)
        set(value) {
            field = value
            glUniformMatrix4fv(uModelViewMatrix, 1, false, value, 0)
        }

    fun setMaterial(material: Material, textures: IntArray) {
        val pbr = material.pbrMetallicRoughness ?: PbrMetallicRoughness()

        glUniform4f(uBaseColorFactor, pbr.baseColorFactor[0], pbr.baseColorFactor[1], pbr.baseColorFactor[2], pbr.baseColorFactor[3])
        if (pbr.baseColorTexture != null) {

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, textures[pbr.baseColorTexture.index])

            glUniform1i(uBaseColorTextureEnabled, GL_TRUE)
            glUniform1i(uBaseColorTexture, 0)
        } else {
            glUniform1i(uBaseColorTextureEnabled, GL_FALSE)
        }
    }

    fun setLightIntensity(lightIntensity: Float) {
        glUniform1f(uLightIntensity, lightIntensity)
    }

    fun setAttribute(attribute: Attribute, accessor: Accessor, bufferView: BufferView) {
        val index = when (attribute) {
            Attribute.POSITION -> aPosition
            Attribute.NORMAL -> aNormal
            Attribute.TEXCOORD_0 -> aTexCoord0
            else -> null
        }

        index?.let {
            glEnableVertexAttribArray(it)
            glVertexAttribPointer(it,
                    accessor.type.size, accessor.componentType.value, false,
                    bufferView.byteStride, accessor.byteOffset)
        }
    }

    fun setAttribute(attribute: Attribute, size: Int, type: ComponentType, stride: Int, buffer: ByteBuffer) {
        val index = when(attribute) {
            Attribute.POSITION -> aPosition
            Attribute.NORMAL -> aNormal
            Attribute.TEXCOORD_0 -> aTexCoord0
            else -> null
        }

        index?.let {
            glEnableVertexAttribArray(it)
            glVertexAttribPointer(it, size, type.value, false, stride, buffer)
        }
    }
}

class ShaderProgramManager {
    private var shaderProgram: ShaderProgram? = null

    fun clear() {
        shaderProgram = null
    }

    @Suppress("UNUSED_PARAMETER")
    fun shaderProgramFor(node: Node, primitive: Primitive, material: Material): ShaderProgram {
        if (shaderProgram == null)
            shaderProgram = ShaderProgram()

        return shaderProgram!!;
    }
}