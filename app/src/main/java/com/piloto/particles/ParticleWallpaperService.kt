package com.piloto.particles

class ParticleWallpaperService : SceneWallpaperService() {
    override fun createScene(density: Float): Scene = ParticleSystem(density)
}
