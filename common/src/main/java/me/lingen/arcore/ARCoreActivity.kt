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

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import me.lingen.common.R
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Helper Activity class for simple ARCore applications. This takes care of setting up the ARCore
 * session, including requesting camera permissions.
 */
abstract class ARCoreActivity(
        @LayoutRes val layout: Int = R.layout.activity_ar,
        @IdRes val glSurfaceViewId: Int = R.id.glSurfaceView)
    : AppCompatActivity() {

    lateinit var arConfig: Config
    lateinit var arSession: Session

    lateinit var arView: GLSurfaceView
    var displayWidth = 0
    var displayHeight = 0

    var cameraTextureId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(layout)

        arView = findViewById<GLSurfaceView>(glSurfaceViewId).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        arSession = Session(this)
        arConfig = Config.createDefaultConfig()

        if (!arSession.isSupported(arConfig)) {
            Toast.makeText(this, "This device does not support ARCore", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()

        if (hasCameraPermission()) {
            arSession.resume(arConfig)
            arView.onResume()
        } else {
            requestCameraPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        arView.onPause()
        arSession.pause()
    }

    private val renderer = object : GLSurfaceView.Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

            val textures = IntArray(1)
            glGenTextures(1, textures, 0)
            cameraTextureId = textures[0]

            glBindTexture(GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

            arSession.setCameraTextureName(cameraTextureId)

            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

            initializeGL()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            glViewport(0, 0, width, height)
            arSession.setDisplayGeometry(width.toFloat(), height.toFloat())
            displayWidth = width
            displayHeight = height
        }

        override fun onDrawFrame(gl: GL10?) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            val frame = arSession.update()
            renderFrame(frame)
        }
    }

    abstract fun initializeGL()
    abstract fun renderFrame(frame: Frame)


    //
    // Force app to run in full screen immersive mode for best AR experience.
    //

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    //
    // Utility methods to handle camera permission checks and requests.
    //

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!hasCameraPermission()) {
            Toast.makeText(this, "This app needs camera permission to run.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    //
    // Disable setContentView methods as they would mess up with setting up the arView for AR
    //

    final override fun setContentView(view: View?) = throw IllegalStateException("Use constructor for setting layout")

    final override fun setContentView(layoutResID: Int) = throw IllegalStateException("Use constructor for setting layout")

    //
    // Helper methods to show status message as a Snackbar
    //

    var statusMessage: Snackbar? = null

    fun showStatusMessage(message: String) = runOnUiThread {
        statusMessage?.dismiss()
        statusMessage = Snackbar.make(findViewById<View>(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE).apply {
            view.setBackgroundColor(Color.argb(180, 40, 40, 40))
            show()
        }

    }

    fun hideStatusMessage() = runOnUiThread {
        statusMessage?.dismiss()
        statusMessage = null
    }


    /**
     * Helper method to copy all the assets in a folder to the file system.
     *
     * @return File object for copied asset directory on the file system
     */
    fun copyAssetFolder(folderName: String): File {
        val assetDir = File(filesDir, folderName).apply {
            if (exists()) deleteRecursively()
            mkdir()
        }

        for (asset in assets.list(folderName)) {
            File(assetDir, asset).apply {
                writeBytes(assets.open("models/$asset").readBytes())
                Log.d("ASSET", "copied asset to $absolutePath")
            }
        }

        return assetDir
    }
}