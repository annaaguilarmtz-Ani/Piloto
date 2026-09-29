package com.piloto.particles

class BloodWallpaperService : SceneWallpaperService() {
    override fun createScene(density: Float): Scene = BloodScene(density)
}
