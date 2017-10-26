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

package me.lingen.gltf.renderer

import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import me.lingen.gltf.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GlTFRenderer(val glTF: GlTF) {

    private val buffers = IntArray(glTF.bufferViews.size)
    private val textures = IntArray(glTF.textures.size)

    private class RenderItem(
            val node: Node,
            val primitive: Primitive,
            val material: Material,
            val shaderProgram: ShaderProgram
    )

    private val shaderManager = ShaderProgramManager()
    private val opaqueItems: MutableList<RenderItem> = mutableListOf()
    private val transparentItems: MutableList<RenderItem> = mutableListOf()

    var scale = 1.0f

    init {
        shaderManager.clear()
        opaqueItems.clear()
        transparentItems.clear()

        val bufferData = Array<ByteBuffer>(glTF.buffers.size) {
            ByteBuffer.allocateDirect(glTF.buffers[it].byteLength).apply {
                order(ByteOrder.nativeOrder())
                put(glTF.buffers[it].data)
                position(0)
            }
        }

        glTF.bufferViews.forEachIndexed { index, bufferView ->
            val target = bufferView.target

            if (target != null) {
                val buffer = bufferData[bufferView.buffer]
                buffer.position(bufferView.byteOffset)

                glGenBuffers(1, buffers, index)
                glBindBuffer(target.value, buffers[index])
                glBufferData(target.value, bufferView.byteLength, buffer, GL_STATIC_DRAW)
                glBindBuffer(target.value, 0)
            }
        }

        glTF.textures.forEachIndexed { index, texture ->
            val source = if (texture.source != null) glTF.images[texture.source] else null
            if (source != null) {
                val bitmap = when {
                    source.data != null -> {
                        BitmapFactory.decodeByteArray(source.data, 0, source.data!!.size)
                    }

                    source.bufferView != null -> {
                        val bufferView = glTF.bufferViews[source.bufferView]
                        val buffer = glTF.buffers[bufferView.buffer]
                        BitmapFactory.decodeByteArray(buffer.data, bufferView.byteOffset, bufferView.byteLength)
                    }

                    else -> null
                }

                if (bitmap != null) {
                    glGenTextures(1, textures, index)
                    glBindTexture(GL_TEXTURE_2D, textures[index])

                    val samplerId = texture.sampler
                    val sampler = if (samplerId != null) glTF.samplers[samplerId] else Sampler()

                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, sampler.minFilter.value)
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, sampler.magFilter.value)
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, sampler.wrapS.value)
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, sampler.wrapT.value)

                    GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
                }
            }
        }

        glTF.nodes.forEach {
            val mesh = if (it.mesh != null) glTF.meshes[it.mesh] else null
            mesh?.primitives?.forEach { primitive ->
                val material = if (primitive.material == null) Material() else glTF.materials[primitive.material]

                val renderItem = RenderItem(it, primitive, material, shaderManager.shaderProgramFor(it, primitive, material))

                when (material.alphaMode) {
                    AlphaMode.OPAQUE, AlphaMode.MASK -> opaqueItems.add(renderItem)
                    AlphaMode.BLEND -> transparentItems.add(renderItem)
                }
            }
        }
    }


    private val scaledTransform = FloatArray(16)
    fun render(transform: FloatArray, viewMatrix: FloatArray, projectionMatrix: FloatArray, lightIntensity: Float = 1f) {
        scaledTransform.copyFrom(transform)
        scaledTransform.scale(scale)

        glTF.update(scaledTransform)

        glDisable(GL_BLEND)
        opaqueItems.forEach { draw(viewMatrix, projectionMatrix, it, lightIntensity) }

        glEnable(GL_BLEND)
        transparentItems.forEach { draw(viewMatrix, projectionMatrix, it, lightIntensity) }
    }

    private fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray, item: RenderItem, lightIntensity: Float) {
        item.shaderProgram.apply {
            use()
            cameraProjection = projectionMatrix
            cameraMatrix = viewMatrix
            modelViewMatrix = item.node.worldMatrix
            setMaterial(item.material, textures)
            setLightIntensity(lightIntensity)
        }

        for ((attribute, accessorId) in item.primitive.attributes) {
            val accessor = glTF.accessors[accessorId]

            val bufferView = glTF.bufferViews[accessor.bufferView]
            glBindBuffer(GL_ARRAY_BUFFER, buffers[accessor.bufferView])

            item.shaderProgram.setAttribute(attribute, accessor, bufferView)
        }

        if (item.primitive.indices != null) {
            val indicesAccessor = glTF.accessors[item.primitive.indices]
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[indicesAccessor.bufferView])
            glDrawElements(item.primitive.mode.value, indicesAccessor.count, indicesAccessor.componentType.value, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        } else {
            val positionAccessorId = item.primitive.attributes[Attribute.POSITION]
            if (positionAccessorId != null) {
                val positionAccessor = glTF.accessors[positionAccessorId]
                glDrawArrays(item.primitive.mode.value, 0, positionAccessor.count)
            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}