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
package me.lingen.arcore

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.WindowManager
import com.google.ar.core.Session

class DisplayRotationHelper(val context: Context) {

    private val display = context.getSystemService(WindowManager::class.java).defaultDisplay

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var viewportChanged = false

    fun onResume() = context.getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener, null)
    fun onPause() = context.getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)

    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
    }

    fun updateSessionIfNeeded(session: Session) {
        if (viewportChanged) {
            session.setDisplayGeometry(display.rotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            viewportChanged = true
        }
    }
}