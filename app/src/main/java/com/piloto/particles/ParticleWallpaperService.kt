package com.piloto.particles

import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder

class ParticleWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParticleEngine()

    private inner class ParticleEngine : Engine(), Choreographer.FrameCallback {
        private lateinit var system: ParticleSystem
        private var visible = false
        private var lastNanos = 0L

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            system = ParticleSystem(resources.displayMetrics.density)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            system.resize(width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            Choreographer.getInstance().removeFrameCallback(this)
            if (visible) {
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
                MotionEvent.ACTION_DOWN -> system.touchDown(event.x, event.y)
                MotionEvent.ACTION_MOVE -> system.touchMove(event.x, event.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> system.touchUp()
            }
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!visible) return
            val dt = if (lastNanos == 0L) 0.016f else (frameTimeNanos - lastNanos) / 1_000_000_000f
            lastNanos = frameTimeNanos
            system.update(dt)
            val holder = surfaceHolder
            val canvas = try { holder.lockCanvas() } catch (e: Exception) { null }
            if (canvas != null) {
                try { system.draw(canvas) } finally { holder.unlockCanvasAndPost(canvas) }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
}
