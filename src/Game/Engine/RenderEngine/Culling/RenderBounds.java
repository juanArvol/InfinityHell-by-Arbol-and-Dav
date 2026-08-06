package Game.Engine.RenderEngine.Culling;

import Game.Engine.Camera.GameCamera;

/**
 * Límites de render: el área del mundo que el renderer debe procesar.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * RenderBounds define el rectángulo del mundo dentro del cual los objetos
 * son visibles y deben ser renderizados. Reemplaza el uso hardcodeado de
 * virtualWidth/virtualHeight simétrico en ViewportCuller.
 *
 * ── DIFERENCIA CON EL CULLING ANTERIOR ────────────────────────────────────
 * Antes: ViewportCuller siempre usaba [cameraX, cameraY, vw, vh] — el
 * rectángulo exacto del framebuffer. Simétrico e inflexible.
 *
 * Ahora: RenderBounds puede ser asimétrico y extendido en cualquier dirección.
 * Un RenderBoundsStrategy calcula el RenderBounds apropiado cada frame.
 *
 * ── CAMPOS ────────────────────────────────────────────────────────────────
 * left, top     → esquina superior izquierda en coords de mundo
 * right, bottom → esquina inferior derecha en coords de mundo
 *
 * El área de render es el rectángulo [left, top, right, bottom].
 *
 * ── EQUIVALENTE AL CULLING ANTERIOR ──────────────────────────────────────
 * RenderBounds.symmetric(camera) produce el mismo resultado que el culling
 * anterior: el rectángulo exacto del framebuffer virtual.
 */
public final class RenderBounds {

    public final double left;
    public final double top;
    public final double right;
    public final double bottom;

    // ── Construcción ──────────────────────────────────────────────────────

    public RenderBounds(double left, double top, double right, double bottom) {
        this.left   = left;
        this.top    = top;
        this.right  = right;
        this.bottom = bottom;
    }

    /**
     * Equivalente exacto al culling anterior: el framebuffer virtual completo.
     * Retrocompatible con todo el código existente.
     */
    public static RenderBounds symmetric(GameCamera camera) {
        double x = camera.getX();
        double y = camera.getY();
        double vw = camera.getVirtualWidth()  / (double) camera.getZoom();
        double vh = camera.getVirtualHeight() / (double) camera.getZoom();
        return new RenderBounds(x, y, x + vw, y + vh);
    }

    /**
     * RenderBounds extendido con márgenes asimétricos.
     * Permite ampliar el área de render en cualquier dirección.
     *
     * @param camera        cámara base
     * @param extraLeft     píxeles extra hacia la izquierda
     * @param extraTop      píxeles extra hacia arriba
     * @param extraRight    píxeles extra hacia la derecha
     * @param extraBottom   píxeles extra hacia abajo
     */
    public static RenderBounds extended(GameCamera camera,
                                         double extraLeft,  double extraTop,
                                         double extraRight, double extraBottom) {
        double x = camera.getX();
        double y = camera.getY();
        double vw = camera.getVirtualWidth()  / (double) camera.getZoom();
        double vh = camera.getVirtualHeight() / (double) camera.getZoom();
        return new RenderBounds(
            x - extraLeft,
            y - extraTop,
            x + vw + extraRight,
            y + vh + extraBottom
        );
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public double getWidth()  { return right  - left; }
    public double getHeight() { return bottom - top;  }

    /**
     * True si el sprite es (parcialmente) visible dentro de estos bounds.
     *
     * @param worldX       posición X del sprite en el mundo
     * @param worldY       posición Y del sprite en el mundo
     * @param spriteWidth  ancho del sprite
     * @param spriteHeight alto del sprite
     * @param margin       margen extra en píxeles
     */
    public boolean isVisible(double worldX, double worldY,
                              int spriteWidth, int spriteHeight,
                              int margin) {
        if (worldX + spriteWidth  < left   - margin) return false;
        if (worldY + spriteHeight < top    - margin) return false;
        if (worldX                > right  + margin) return false;
        if (worldY                > bottom + margin) return false;
        return true;
    }

    public boolean isVisible(double worldX, double worldY,
                              int spriteWidth, int spriteHeight) {
        return isVisible(worldX, worldY, spriteWidth, spriteHeight,
                         ViewportCuller.DEFAULT_MARGIN);
    }

    /**
     * Equivalente al ViewportCuller.isVisible() anterior para compatibilidad.
     * Los componentes de render pueden llamar a este método directamente
     * en lugar de usar ViewportCuller.
     */
    public boolean isVisibleAt(double worldX, double worldY,
                                int spriteWidth, int spriteHeight,
                                double cameraX, double cameraY,
                                int virtualWidth, int virtualHeight) {
        // Usar los bounds propios de esta instancia (ignora los parámetros de cámara legacy)
        return isVisible(worldX, worldY, spriteWidth, spriteHeight);
    }

    @Override
    public String toString() {
        return "RenderBounds[" + left + "," + top + " → " + right + "," + bottom + "]";
    }
}
