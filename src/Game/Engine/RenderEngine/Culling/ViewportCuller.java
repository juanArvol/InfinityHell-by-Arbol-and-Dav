package Game.Engine.RenderEngine.Culling;

/**
 * ViewportCuller — descarta sprites completamente fuera del área visible.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Frustum culling 2D: determina si un rectángulo de mundo es visible
 * dentro del área virtual del Display, aplicando el offset de cámara.
 *
 * ── POR QUÉ IMPORTA ───────────────────────────────────────────────────────
 * Sin culling, todos los objetos del mundo se pasan por el pipeline de
 * render aunque estén fuera de pantalla. Con mundos de 1280x1280px y
 * viewport de 1280x720px, en cualquier momento hay objetos fuera.
 * El culling evita:
 *   - drawImage() con imágenes fuera del framebuffer
 *   - Composición de transforms innecesaria
 *   - Iteración de componentes de objetos invisibles
 *
 * ── CORRECTITUD ───────────────────────────────────────────────────────────
 * El test es conservador: si un sprite está parcialmente dentro del viewport
 * (aunque solo 1 píxel sea visible) isVisible() retorna true.
 * Solo retorna false cuando el sprite está COMPLETAMENTE fuera.
 *
 * ── MARGEN ────────────────────────────────────────────────────────────────
 * El parámetro margin añade píxeles extra al viewport antes del test.
 * Útil para evitar pop-in con sprites que tienen efectos que sobresalen
 * de su bounding box (sombras, glows). Default: 32px.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Test sin margen
 *   if (!ViewportCuller.isVisible(x, y, w, h, camX, camY, vw, vh)) return;
 *
 *   // Test con margen de 32px
 *   if (!ViewportCuller.isVisible(x, y, w, h, camX, camY, vw, vh, 32)) return;
 *
 * ── NOTA SOBRE ROTACIÓN ───────────────────────────────────────────────────
 * Este culler opera en AABB (axis-aligned bounding box).
 * Con rotación activa, el bounding box del sprite rotado puede ser mayor
 * que el original. Para sprites con rotación, el margen extra compensa
 * parcialmente este error. Para precisión total con rotación se requiere
 * un OBB culler, que es trabajo de una refactorización futura.
 */
public final class ViewportCuller {

    /** Margen por defecto en píxeles para el test de visibilidad. */
    public static final int DEFAULT_MARGIN = 32;

    // Clase utilitaria — no instanciable.
    private ViewportCuller() {}

    // ── API principal ─────────────────────────────────────────────────────────

    /**
     * Determina si un sprite en coordenadas de MUNDO es visible en el viewport.
     *
     * @param worldX       posición X del sprite en el mundo
     * @param worldY       posición Y del sprite en el mundo
     * @param spriteWidth  ancho del sprite en píxeles
     * @param spriteHeight alto del sprite en píxeles
     * @param cameraX      posición X de la cámara (offset del viewport)
     * @param cameraY      posición Y de la cámara
     * @param virtualWidth  ancho del framebuffer virtual
     * @param virtualHeight alto del framebuffer virtual
     * @return true si el sprite es (parcialmente) visible
     */
    public static boolean isVisible(double worldX,    double worldY,
                                    int spriteWidth,  int spriteHeight,
                                    double cameraX,   double cameraY,
                                    int virtualWidth, int virtualHeight) {
        return isVisible(worldX, worldY, spriteWidth, spriteHeight,
            cameraX, cameraY, virtualWidth, virtualHeight, DEFAULT_MARGIN);
    }

    /**
     * Determina si un sprite en coordenadas de MUNDO es visible en el viewport,
     * con un margen configurable.
     *
     * @param margin píxeles adicionales en cada borde del viewport antes del test
     */
    public static boolean isVisible(double worldX,    double worldY,
                                    int spriteWidth,  int spriteHeight,
                                    double cameraX,   double cameraY,
                                    int virtualWidth, int virtualHeight,
                                    int margin) {

        // Posición del sprite en coordenadas de pantalla
        double screenX = worldX - cameraX;
        double screenY = worldY - cameraY;

        // Viewport expandido con margen
        int vpLeft   = -margin;
        int vpTop    = -margin;
        int vpRight  = virtualWidth  + margin;
        int vpBottom = virtualHeight + margin;

        // El sprite es invisible si está completamente fuera del viewport
        if (screenX + spriteWidth  < vpLeft)   return false;
        if (screenY + spriteHeight < vpTop)    return false;
        if (screenX                > vpRight)  return false;
        if (screenY                > vpBottom) return false;

        return true;
    }

    /**
     * Variante con coordenadas de pantalla ya calculadas.
     * Útil cuando el componente ya convirtió worldPos → screenPos.
     *
     * @param screenX      posición X en pantalla (ya con offset de cámara)
     * @param screenY      posición Y en pantalla
     * @param spriteWidth  ancho del sprite
     * @param spriteHeight alto del sprite
     * @param virtualWidth  ancho virtual
     * @param virtualHeight alto virtual
     * @param margin       margen extra
     */
    public static boolean isVisibleOnScreen(int screenX,     int screenY,
                                            int spriteWidth,  int spriteHeight,
                                            int virtualWidth, int virtualHeight,
                                            int margin) {
        int vpLeft   = -margin;
        int vpTop    = -margin;
        int vpRight  = virtualWidth  + margin;
        int vpBottom = virtualHeight + margin;

        if (screenX + spriteWidth  < vpLeft)   return false;
        if (screenY + spriteHeight < vpTop)    return false;
        if (screenX                > vpRight)  return false;
        if (screenY                > vpBottom) return false;

        return true;
    }

    /**
     * Variante sin margen extra para el test en pantalla.
     */
    public static boolean isVisibleOnScreen(int screenX,     int screenY,
                                            int spriteWidth,  int spriteHeight,
                                            int virtualWidth, int virtualHeight) {
        return isVisibleOnScreen(screenX, screenY, spriteWidth, spriteHeight,
            virtualWidth, virtualHeight, DEFAULT_MARGIN);
    }
}
