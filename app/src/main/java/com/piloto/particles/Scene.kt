package com.piloto.particles

import android.graphics.Canvas

/** Una escena animada e interactiva; la comparten el fondo animado y la vista previa. */
interface Scene {
    fun resize(width: Int, height: Int)
    fun touchDown(px: Float, py: Float)
    fun touchMove(px: Float, py: Float)
    fun touchUp()
    fun update(dt: Float)
    fun draw(canvas: Canvas)
}
