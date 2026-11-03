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
 * ── HRFC — Refactor con RectangleH ────────────────────────────────────────
 * Las versiones de viewport refactorizadas usan RectangleH internamente para
 * eliminar duplicación de lógica de intersección. Las variantes legacy se
 * mantienen por compatibilidad pero delegan a las versiones RectangleH.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Test sin margen
 *   if (!ViewportCuller.isVisible(x, y, w, h, camX, camY, vw, vh)) return;
 *
 *   // Test con margen de 32px
 *   if (!ViewportCuller.isVisible(x, y, w, h, camX, camY, vw, vh, 32)) return;
 *
 *   // Test con RectangleH (nuevo)
 *   RectangleH viewport = new RectangleH(camX, camY, vw, vh).expanded(margin);
 *   RectangleH sprite = new RectangleH(worldX, worldY, w, h);
 *   if (!ViewportCuller.isVisible(viewport, sprite)) return;
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

    // ── API basada en RectangleH (recomendada) ────────────────────────────

    /**
     * Determina si un sprite es visible usando RectangleH para viewport y sprite.
     * Versión más eficiente cuando los bounds ya están precalculados.
     * 
     * @param viewport bounds del viewport (ya con margen aplicado si corresponde)
     * @param sprite   bounds del sprite en coordenadas de pantalla
     * @return true si el sprite es (parcialmente) visible
     */
    public static boolean isVisible(Game.Engine.GameMath.Logic2D.RectangleH viewport,
                                    Game.Engine.GameMath.Logic2D.RectangleH sprite) {
        return viewport.intersects(sprite);
    }

    /**
     * Determina si un sprite es visible usando coordenadas directas.
     * Crea RectangleH internamente — usar sobrecarga anterior si los bounds
     * ya están disponibles para evitar allocations.
     * 
     * @param viewport     bounds del viewport
     * @param spriteX      posición X del sprite en pantalla
     * @param spriteY      posición Y del sprite en pantalla
     * @param spriteWidth  ancho del sprite
     * @param spriteHeight alto del sprite
     * @return true si es visible
     */
    public static boolean isVisible(Game.Engine.GameMath.Logic2D.RectangleH viewport,
                                    double spriteX, double spriteY,
                                    int spriteWidth, int spriteHeight) {
        Game.Engine.GameMath.Logic2D.RectangleH sprite = 
            new Game.Engine.GameMath.Logic2D.RectangleH(spriteX, spriteY, spriteWidth, spriteHeight);
        return viewport.intersects(sprite);
    }

    // ── API principal (compatibilidad) ────────────────────────────────────

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

        // HRFC: Usar RectangleH internamente para eliminar duplicación de lógica
        Game.Engine.GameMath.Logic2D.RectangleH viewport = 
            new Game.Engine.GameMath.Logic2D.RectangleH(
                cameraX - margin,
                cameraY - margin,
                virtualWidth + 2 * margin,
                virtualHeight + 2 * margin
            );
        
        Game.Engine.GameMath.Logic2D.RectangleH sprite = 
            new Game.Engine.GameMath.Logic2D.RectangleH(worldX, worldY, spriteWidth, spriteHeight);
        
        return viewport.intersects(sprite);
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
        // HRFC: Usar RectangleH para consistencia
        Game.Engine.GameMath.Logic2D.RectangleH viewport = 
            new Game.Engine.GameMath.Logic2D.RectangleH(
                -margin,
                -margin,
                virtualWidth + 2 * margin,
                virtualHeight + 2 * margin
            );
        
        Game.Engine.GameMath.Logic2D.RectangleH sprite = 
            new Game.Engine.GameMath.Logic2D.RectangleH(screenX, screenY, spriteWidth, spriteHeight);
        
        return viewport.intersects(sprite);
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
