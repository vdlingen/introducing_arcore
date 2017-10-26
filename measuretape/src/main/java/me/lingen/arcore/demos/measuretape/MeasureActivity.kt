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

import android.os.Bundle
import android.view.View
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.PlaneHitResult
import com.google.ar.core.Pose
import kotlinx.android.synthetic.main.activity_measure.*
import me.lingen.arcore.ARCoreActivity
import me.lingen.arcore.rendering.CameraRenderer

class MeasureActivity : ARCoreActivity(R.layout.activity_measure, R.id.glSurfaceView) {

    lateinit var cameraRenderer: CameraRenderer
    lateinit var measurementRenderer: MeasurementRenderer

    var startAnchor: Anchor? = null
    var endAnchor: Anchor? = null

    var lastPose: Pose? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arView.setOnClickListener {
            val start = startAnchor
            val end = endAnchor

            if (start != null && end != null) {
                arSession.removeAnchors(listOf(start, end))
                startAnchor = null
                endAnchor = null
                hideMeasurement()
            } else if (start == null) {
                startAnchor = lastPose?.let { arSession.addAnchor(it) }
            } else {
                endAnchor = lastPose?.let { arSession.addAnchor(it)}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        startAnchor = null
        endAnchor = null
        lastPose = null
    }

    override fun initializeGL() {
        cameraRenderer = CameraRenderer(cameraTextureId)
        measurementRenderer = MeasurementRenderer()
    }

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    override fun renderFrame(frame: Frame) {
        cameraRenderer.render(frame)

        if (frame.trackingState != Frame.TrackingState.TRACKING) {
            hideMeasurement()
            return
        }

        lastPose = frame.hitTest(0.5f * displayWidth, 0.5f * displayHeight)
                .filter { it is PlaneHitResult && it.isHitInPolygon }
                .firstOrNull()?.hitPose

        arSession.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        frame.getViewMatrix(viewMatrix, 0)

        val start = startAnchor
        val end = endAnchor

        var distance: Float? = null

        if (start != null) {
            distance = measurementRenderer.render(start.pose, end?.pose ?: lastPose, end != null, viewMatrix, projectionMatrix)
        } else {
            lastPose?.let {
                distance = measurementRenderer.render(it, null, false, viewMatrix, projectionMatrix)
            }
        }

        val d = distance
        if (d != null) {
            setMeasurement(d)
        } else {
            hideMeasurement()
        }
    }

    fun setMeasurement(measurement: Float) = runOnUiThread {
        measurementText.text = "%.2f meter".format(measurement)
        measurementText.visibility = View.VISIBLE
    }

    fun hideMeasurement() = runOnUiThread {
        measurementText.visibility = View.GONE
    }
}
