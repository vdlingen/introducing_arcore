# Introducing ARCore

This repository contains some sample apps for ARCore written in Kotlin. It is used for live coding demonstrations and workshops accompanying my 'Introducing ARCore' talk.

## common module
The common module contains the shared utility code used in the different sample apps.

### ARCoreActivity
The base class 'ARCoreActivity' provides an easy starting point for an ARCore app and is used by the sample apps. It handles the setup of ARCore and OpenGL, requests camera permissions, and puts the activity in immersive fullscreen mode. You will need to implement 2 abstract methods (initializeGL and renderFrame). Note that the camera image rendering still needs to be handled by the actual implementation.

### glTF rendering
An experimental glTF model parser and renderer is included for rendering 3D models in the demo apps.

## demo apps
### helloarcore
Insprired by the Hello AR samples included in the official ARCore SDK. This demo visualizes the point cloud with color coding for depth, and the detected planes with simple grids. Tapping the screen triggers a hit test, and if you click on a plane, a new anchor will be added with an Android robot 3D model.

### arcursor
Demonstrates a continuously updating 3D cursor whenever we point at a detected plane. Tapping the screen will place a big Android Oreo statue on the location of the cursor. Hitting the back button will reset the exerpience.

### measuretape
Simple test app for exploring the possibilities of distance measurements with ARCore. Point your phone at flat surfaces and tap on the screen to measure the distance between 2 points on the plane.

## disclaimer
Most of the code in this repository is experimental and untested. Use at your own risk.
