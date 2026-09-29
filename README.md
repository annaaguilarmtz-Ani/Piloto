# Piloto – Fondo de pantalla interactivo de partículas (Android)

Live wallpaper: fondo negro, cientos de partículas diminutas multicolor en suspensión.
- Al tocar la pantalla, las partículas acuden a tu dedo y giran a su alrededor.
- Las partículas cercanas al dedo se dividen y multiplican (las nuevas desaparecen suavemente a los ~15–25 s).

## Instalar
1. En GitHub: pestaña **Actions → Build APK →** último run **→ Artifacts → particulas-apk**.
2. Instala `app-debug.apk` en el móvil (permite "instalar apps desconocidas").
3. Abre la app **Partículas**: puedes jugar con la vista previa y pulsar **Establecer como fondo**.

## Compilar en local
`gradle assembleDebug` (JDK 17 + Android SDK 34).

Ajustes en `ParticleSystem.kt`: `BASE_COUNT`, `MAX_COUNT`, tamaño y radio de división.

Nota: algunos launchers solo envían al fondo los toques sobre zonas vacías del escritorio.
