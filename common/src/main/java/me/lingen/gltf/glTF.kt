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

import com.squareup.moshi.FromJson
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import java.io.File

data class GlTF(
        val extensionsUsed: List<String> = emptyList(),
        val extensionsRequired: List<String> = emptyList(),
        val accessors: List<Accessor> = emptyList(),
        val animations: List<Animation> = emptyList(),
        val asset: Asset,
        val buffers: List<Buffer> = emptyList(),
        val bufferViews: List<BufferView> = emptyList(),
        val cameras: List<Camera> = emptyList(),
        val images: List<Image> = emptyList(),
        val materials: List<Material> = emptyList(),
        val meshes: List<Mesh> = emptyList(),
        val nodes: List<Node> = emptyList(),
        val samplers: List<Sampler> = emptyList(),
        val scene: Int = 0,
        val scenes: List<Scene> = emptyList(),
        val skins: List<Skin> = emptyList(),
        val textures: List<Texture> = emptyList()
) {
    fun update(transform: FloatArray = FloatArray(16).setIdentity(), selectScene: Int = -1) {
        val currentScene = scenes[if (selectScene >= 0) selectScene else this.scene]
        for (node in currentScene.nodes) {
            nodes[node].update(this, transform)
        }
    }
}

enum class ComponentType(val value: Int) {
    BYTE(5120),
    UNSIGNED_BYTE(5121),
    SHORT(5122),
    UNSIGNED_SHORT(5123),
    UNSIGNED_INT(5125),
    FLOAT(5126),

    INVALID(-1)
}

enum class AccessorType(val size: Int) {
    SCALAR(1), VEC2(2), VEC3(3), VEC4(4), MAT2(4), MAT3(9), MAT4(16)
}

data class Accessor(
        val bufferView: Int = -1,
        val byteOffset: Int = 0,
        val componentType: ComponentType,
        val normalized: Boolean = false,
        val count: Int,
        val type: AccessorType,
        val max: FloatArray? = null,
        val min: FloatArray? = null,
        val sparse: Sparse? = null,
        val name: String? = null
)

data class Animation(
        val channels: Array<Channel>,
        val samplers: Array<AnimationSampler>,
        val name: String? = null
)

enum class Interpolation { LINEAR, STEP, CATMULLROMSPLINE, CUBICSPLINE }
data class AnimationSampler(
        val input: Int,
        val interpolation: Interpolation = Interpolation.LINEAR,
        val output: Int
)

data class Channel(
        val sampler: Int
)

data class Sparse(
        val count: Int,
        val indices: Indices,
        val values: Values
)

data class Indices(
        val bufferView: Int,
        val byteOffset: Int = 0,
        val componentType: ComponentType
)

data class Values(
        val bufferView: Int,
        val byteOffset: Int = 0
)

data class Asset(
        val copyright: String? = null,
        val generator: String? = null,
        val version: String,
        val minVersion: String? = null
)

data class Buffer(
        val uri: String? = null,
        val byteLength: Int,
        val name: String? = null
) {
    @Transient var data: ByteArray? = null
    fun load(path: File?) {
        if (uri != null) data = loadUri(uri, path)
    }
}

enum class Target(val value: Int) {
    ARRAY_BUFFER(34962),
    ELEMENT_ARRAY_BUFFER(34963)
}

data class BufferView(
        val buffer: Int,
        val byteOffset: Int = 0,
        val byteLength: Int,
        val byteStride: Int = 0,
        val target: Target? = null,
        val name: String? = null
)

enum class CameraType { orthographic, perspective }
data class Camera(
        val orthographic: Orthographic? = null,
        val perspective: Perspective? = null,
        val type: CameraType,
        val name: String? = null
)

data class Orthographic(
        val xmag: Float,
        val ymag: Float,
        val zfar: Float,
        val znear: Float
)

data class Perspective(
        val aspectRatio: Float? = null,
        val yfov: Float,
        val zfar: Float? = null,
        val znear: Float
)

data class Image(
        val uri: String? = null,
        val mimeType: String? = null,
        val bufferView: Int? = null,
        val name: String? = null
) {
    @Transient var data: ByteArray? = null
    fun load(path: File?) {
        if (bufferView != null) return
        if (uri != null) data = loadUri(uri, path)
    }
}

private fun loadUri(uri: String, path: File?): ByteArray? {
    if (path != null) {
        val file = File(path, uri)
        if (file.exists()) {
            return file.readBytes()
        }
    }

    return null
}

enum class AlphaMode { OPAQUE, MASK, BLEND }
data class Material(
        val name: String? = null,
        val pbrMetallicRoughness: PbrMetallicRoughness? = null,
        val normalTexture: NormalTextureInfo? = null,
        val occlusionTexture: OcclusionTextureInfo? = null,
        val emissiveTexture: TextureInfo? = null,
        val emissiveFacttor: FloatArray = floatArrayOf(0f, 0f, 0f),
        val alphaMode: AlphaMode = AlphaMode.OPAQUE,
        val alphaCutoff: Float = 0.5f,
        val doubleSided: Boolean = false
)

data class NormalTextureInfo(
        val index: Int,
        val texCoord: Int = 0,
        val scale: Float = 1f
)

data class OcclusionTextureInfo(
        val index: Int,
        val texCoord: Int = 0,
        val strenght: Float = 1f
)

data class TextureInfo(
        val index: Int,
        val texCoord: Int = 0
)

data class PbrMetallicRoughness(
        val baseColorFactor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
        val baseColorTexture: TextureInfo? = null,
        val metallicFactor: Float = 1f,
        val roughnessFactor: Float = 1f,
        val metallicRoughnessTexture: TextureInfo? = null
)

data class Texture(
        val sampler: Int? = null,
        val source: Int? = null,
        val name: String? = null
)

enum class Filter(val value: Int) {
    NEAREST(9728),
    LINEAR(9729),
    NEAREST_MIPMAP_NEAREST(9984),
    LINEAR_MIPMAP_NEAREST(9985),
    NEAREST_MIPMAP_LINEAR(9986),
    LINEAR_MIPMAP_LINEAR(9987)
}

enum class Wrap(val value: Int) {
    CLAMP_TO_EDGE(33071),
    MIRRORED_REPEAT(33648),
    REPEAT(10497)
}

data class Sampler(
        val magFilter: Filter = Filter.NEAREST,
        val minFilter: Filter = Filter.NEAREST,
        val wrapS: Wrap = Wrap.REPEAT,
        val wrapT: Wrap = Wrap.REPEAT,
        val name: String? = null
)

data class Mesh(
        val primitives: List<Primitive>,
        val weights: FloatArray? = null,
        val name: String? = null
)

enum class Attribute {
    POSITION,
    NORMAL,
    TANGENT,
    TEXCOORD_0,
    TEXCOORD_1,
    COLOR_0,
    JOINTS_0,
    WEIGHTS_0
}

enum class PrimitiveMode(val value: Int) {
    POINTS(0),
    LINES(1),
    LINE_LOOP(2),
    LINE_STRIP(3),
    TRIANGLES(4),
    TRIANGLE_STRIP(5),
    TRIANGLE_FAN(6)
}

data class Primitive(
        val attributes: Map<Attribute, Int>,
        val indices: Int? = null,
        val material: Int? = null,
        val mode: PrimitiveMode = PrimitiveMode.TRIANGLES,
        val targets: Array<Map<String, Int>>? = null
)

data class Node(
        val camera: Int? = null,
        val children: List<Int> = emptyList(),
        val skin: Int? = null,
        val matrix: FloatArray? = null,
        val mesh: Int? = null,
        var rotation: FloatArray? = null,
        var scale: FloatArray? = null,
        var translation: FloatArray? = null,
        var weights: FloatArray? = null,
        val name: String? = null
) {
    @Transient
    val worldMatrix = FloatArray(16)

    fun update(glTF: GlTF, parentMatrix: FloatArray) {
        if (matrix != null) {
            worldMatrix.setProductOf(parentMatrix, matrix)
            return
        }

        val trs = FloatArray(16).setIdentity()
        translation?.let {
            trs.translate(it[0], it[1], it[2])
        }
        rotation?.let {
            trs.multiply(
                    FloatArray(16).setInverseOf(
                            FloatArray(16).setQuaternion(it[0], it[1], it[2], it[3])))
        }
        scale?.let {
            trs.scale(it[0], it[1], it[2])
        }

        worldMatrix.setProductOf(parentMatrix, trs)

        children.forEach {
            glTF.nodes[it].update(glTF, worldMatrix)
        }
    }
}

data class Scene(
        val nodes: List<Int> = emptyList(),
        val name: String? = null
)

data class Skin(
        val inverseBindMatrices: Int? = null,
        val skeleton: Int? = null,
        val joints: List<Int>,
        val name: String? = null
)

private val adapter by lazy {
    Moshi.Builder()
            .add(ComponentTypeAdapter())
            .add(TargetAdapter())
            .add(PrimitiveModeAdapter())
            .add(FilterAdapter())
            .add(WrapAdapter())
            .add(KotlinJsonAdapterFactory())
            .build().adapter(GlTF::class.java)
}

class ComponentTypeAdapter {
    @ToJson
    fun toJson(componentType: ComponentType) = componentType.value

    @FromJson
    fun fromJson(value: Int) = ComponentType.values().find { it.value == value } ?: ComponentType.INVALID
}

class TargetAdapter {
    @ToJson
    fun toJson(target: Target) = target.value

    @FromJson
    fun fromJson(value: Int) = Target.values().find { it.value == value }
}

class PrimitiveModeAdapter {
    @ToJson
    fun toJson(primiveMode: PrimitiveMode) = primiveMode.value

    @FromJson
    fun fromJson(value: Int) = PrimitiveMode.values().find { it.value == value } ?: PrimitiveMode.TRIANGLES
}

class FilterAdapter {
    @ToJson
    fun toJson(filter: Filter) = filter.value

    @FromJson
    fun fromJson(value: Int) = Filter.values().find { it.value == value } ?: Filter.NEAREST
}

class WrapAdapter {
    @ToJson
    fun toJson(wrap: Wrap) = wrap.value

    @FromJson
    fun fromJson(value: Int) = Wrap.values().find { it.value == value } ?: Wrap.REPEAT
}

fun parseGlTF(file: File) = parseGlTF(file.readText(), file.parentFile)
fun parseGlTF(json: String, path: File? = null) = adapter.fromJson(json)?.apply {
    buffers.forEach { it.load(path) }
    images.forEach { it.load(path) }
}