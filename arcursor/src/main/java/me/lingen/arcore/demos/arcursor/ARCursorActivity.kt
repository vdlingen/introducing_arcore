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

package me.lingen.arcore.demos.arcursor

import android.os.Bundle
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.PlaneHitResult
import com.google.ar.core.Pose
import me.lingen.arcore.ARCoreActivity
import me.lingen.arcore.rendering.CameraRenderer
import me.lingen.gltf.GlTF
import me.lingen.gltf.parseGlTF
import me.lingen.gltf.renderer.GlTFRenderer
import java.io.File

class ARCursorActivity : ARCoreActivity() {

    lateinit var cameraRenderer: CameraRenderer

    var cursorModel: GlTF? = null
    var model: GlTF? = null

    var cursorRenderer: GlTFRenderer? = null
    var modelRenderer: GlTFRenderer? = null

    var modelAnchor: Anchor? = null

    var lastPose: Pose? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modelsDir = copyAssetFolder("models")
        cursorModel = parseGlTF(File(modelsDir, "cursor.gltf"))
        model = parseGlTF(File(modelsDir, "android_oreo.gltf"))

        arView.setOnClickListener {
            if (modelAnchor == null) {
                modelAnchor = lastPose?.let { arSession.addAnchor(it) }
            }
        }
    }

    override fun onBackPressed() {
        if (modelAnchor != null) {
            arSession.removeAnchors(listOf(modelAnchor))
            modelAnchor = null
        } else {
            super.onBackPressed()
        }
    }

    override fun initializeGL() {
        cameraRenderer = CameraRenderer(cameraTextureId)
        cursorModel?.let { cursorRenderer = GlTFRenderer(it) }
        model?.let { modelRenderer = GlTFRenderer(it).apply { scale = 17.5f } }
    }

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    override fun renderFrame(frame: Frame) {
        cameraRenderer.render(frame)

        if (frame.trackingState == Frame.TrackingState.NOT_TRACKING) return

        lastPose = frame.hitTest(0.5f * displayWidth, 0.5f * displayHeight)
                .filter { it is PlaneHitResult && it.isHitInPolygon }
                .firstOrNull()?.hitPose

        arSession.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        frame.getViewMatrix(viewMatrix, 0)

        val anchor = modelAnchor
        if (anchor != null) {
            val transform = FloatArray(16)
            anchor.pose.toMatrix(transform, 0)

            modelRenderer?.render(transform, viewMatrix, projectionMatrix)
        } else {
            lastPose?.let {
                val transform = FloatArray(16)
                it.toMatrix(transform, 0)

                cursorRenderer?.render(transform, viewMatrix, projectionMatrix)
            }
        }
    }
}
