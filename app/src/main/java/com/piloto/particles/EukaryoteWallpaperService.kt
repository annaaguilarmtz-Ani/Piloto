package com.piloto.particles

class EukaryoteWallpaperService : SceneWallpaperService() {
    override fun createScene(density: Float): Scene = EukaryoteScene(density)
}
