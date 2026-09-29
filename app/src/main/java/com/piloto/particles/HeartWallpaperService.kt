package com.piloto.particles

class HeartWallpaperService : SceneWallpaperService() {
    override fun createScene(density: Float): Scene = HeartScene(density)
}
