package com.piloto.particles

class PlantWallpaperService : SceneWallpaperService() {
    override fun createScene(density: Float): Scene = PlantScene(density)
}
