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

package me.lingen.arcore.demos.helloarcore

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.PlaneHitResult
import me.lingen.arcore.ARCoreActivity
import me.lingen.arcore.rendering.CameraRenderer
import me.lingen.arcore.rendering.PlaneRenderer
import me.lingen.arcore.rendering.PointCloudRenderer
import me.lingen.gltf.GlTF
import me.lingen.gltf.parseGlTF
import me.lingen.gltf.renderer.GlTFRenderer
import java.io.File

class HelloARActivity : ARCoreActivity() {

    lateinit var cameraRenderer: CameraRenderer
    lateinit var pointCloudRenderer: PointCloudRenderer
    lateinit var planeRenderer: PlaneRenderer

    lateinit var gestureDetector: GestureDetector
    var pendingTap: MotionEvent? = null

    var model: GlTF? = null
    var modelRenderer: GlTFRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modelsDir = copyAssetFolder("models")
        model = parseGlTF(File(modelsDir, "android.gltf"))

        gestureDetector = GestureDetector(this, gestureListener)
        arView.setOnTouchListener { _, motionEvent -> gestureDetector.onTouchEvent(motionEvent) }
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?) = true
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            pendingTap = e
            return true
        }
    }

    override fun onResume() {
        super.onResume()
        showStatusMessage("Looking for flat surfaces...")
    }

    override fun initializeGL() {
        cameraRenderer = CameraRenderer(cameraTextureId)
        pointCloudRenderer = PointCloudRenderer()
        planeRenderer = PlaneRenderer(this)

        model?.let { modelRenderer = GlTFRenderer(it) }
    }

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    override fun renderFrame(frame: Frame) {
        cameraRenderer.render(frame)

        if (frame.trackingState != Frame.TrackingState.TRACKING) return
        if (frame.updatedPlanes.isNotEmpty()) hideStatusMessage()

        pendingTap?.let {
            frame.hitTest(it)
                    .firstOrNull { it is PlaneHitResult && it.isHitInPolygon }
                    ?.let { arSession.addAnchor(it.hitPose) }

            pendingTap = null
        }

        arSession.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        frame.getViewMatrix(viewMatrix, 0)

        pointCloudRenderer.render(frame.pointCloud, frame.pointCloudPose, viewMatrix, projectionMatrix)

        arSession.allPlanes
                .filter { it.trackingState == Plane.TrackingState.TRACKING }
                .forEach { planeRenderer.render(it, viewMatrix, projectionMatrix)}

        arSession.allAnchors
                .filter { it.trackingState == Anchor.TrackingState.TRACKING }
                .forEach {
                    it.pose.toMatrix(modelMatrix, 0)
                    modelRenderer?.render(modelMatrix, viewMatrix, projectionMatrix)
                }
    }

}
