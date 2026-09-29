package com.piloto.particles

import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder

/** Base de todos los fondos: cada subclase solo decide qué escena dibujar. */
abstract class SceneWallpaperService : WallpaperService() {

    abstract fun createScene(density: Float): Scene

    override fun onCreateEngine(): Engine = SceneEngine()

    private inner class SceneEngine : Engine(), Choreographer.FrameCallback {
        private lateinit var scene: Scene
        private var visible = false
        private var lastNanos = 0L

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            scene = createScene(resources.displayMetrics.density)
            scene.configure(getSharedPreferences(Prefs.NAME, android.content.Context.MODE_PRIVATE))
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            scene.resize(width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            Choreographer.getInstance().removeFrameCallback(this)
            if (visible) {
                scene.configure(getSharedPreferences(Prefs.NAME, android.content.Context.MODE_PRIVATE))
                lastNanos = 0L
                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            Choreographer.getInstance().removeFrameCallback(this)
        }

        override fun onDestroy() {
            super.onDestroy()
            Choreographer.getInstance().removeFrameCallback(this)
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> scene.touchDown(event.x, event.y)
                MotionEvent.ACTION_MOVE -> scene.touchMove(event.x, event.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scene.touchUp()
            }
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!visible) return
            val dt = if (lastNanos == 0L) 0.016f else (frameTimeNanos - lastNanos) / 1_000_000_000f
            lastNanos = frameTimeNanos
            scene.update(dt)
            val holder = surfaceHolder
            val canvas = try { holder.lockCanvas() } catch (e: Exception) { null }
            if (canvas != null) {
                try { scene.draw(canvas) } finally { holder.unlockCanvasAndPost(canvas) }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
}
