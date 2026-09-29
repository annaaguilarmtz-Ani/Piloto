package com.piloto.particles

class CellWallpaperService : SceneWallpaperService() {
    override fun createScene(density: Float): Scene = ProkaryoteScene(density)
}
