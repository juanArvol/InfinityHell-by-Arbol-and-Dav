package Display;

/**
 * Resultado inmutable del cálculo de viewport.
 *
 * Contiene TODOS los datos necesarios para:
 *  - Posicionar y escalar el framebuffer virtual en pantalla
 *  - Transformar coordenadas pantalla ↔ mundo virtual
 *  - Calcular bounds de UI
 *
 * Es un value object: sin estado mutable, sin lógica. Solo datos.
 * Se recalcula completo en cada onResize().
 */
public final class ViewportInfo {

    /** Offset X del viewport dentro del canvas (barras de pillarbox). */
    public final int x;

    /** Offset Y del viewport dentro del canvas (barras de letterbox). */
    public final int y;

    /** Ancho del viewport escalado en píxeles reales. */
    public final int width;

    /** Alto del viewport escalado en píxeles reales. */
    public final int height;

    /** Factor de escala aplicado (virtual → real). */
    public final float scale;

    /** Ancho real del canvas (pantalla o ventana). */
    public final int realWidth;

    /** Alto real del canvas (pantalla o ventana). */
    public final int realHeight;

    /** Ancho virtual del juego (siempre constante, de DisplaySettings). */
    public final int virtualWidth;

    /** Alto virtual del juego (siempre constante, de DisplaySettings). */
    public final int virtualHeight;

    public ViewportInfo(int x, int y, int width, int height, float scale,
                        int realWidth, int realHeight,
                        int virtualWidth, int virtualHeight) {
        this.x             = x;
        this.y             = y;
        this.width         = width;
        this.height        = height;
        this.scale         = scale;
        this.realWidth     = realWidth;
        this.realHeight    = realHeight;
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    // ─── Transformaciones de coordenadas ──────────────────────────────────────

    /**
     * Convierte X de pantalla real a X virtual del juego.
     * Útil para input de mouse: mouseX → worldX.
     */
    public float toVirtualX(int screenX) {
        return (screenX - x) / scale;
    }

    /**
     * Convierte Y de pantalla real a Y virtual del juego.
     */
    public float toVirtualY(int screenY) {
        return (screenY - y) / scale;
    }

    /**
     * Convierte X virtual a X de pantalla real.
     * Útil para posicionar elementos de debug overlay.
     */
    public int toScreenX(float virtualX) {
        return (int)(virtualX * scale) + x;
    }

    /**
     * Convierte Y virtual a Y de pantalla real.
     */
    public int toScreenY(float virtualY) {
        return (int)(virtualY * scale) + y;
    }

    /**
     * True si el punto de pantalla está dentro del área del viewport
     * (no en las barras negras).
     */
    public boolean containsScreenPoint(int sx, int sy) {
        return sx >= x && sx < x + width &&
               sy >= y && sy < y + height;
    }

    @Override
    public String toString() {
        return String.format(
            "ViewportInfo[x=%d y=%d w=%d h=%d scale=%.4f real=%dx%d virtual=%dx%d]",
            x, y, width, height, scale, realWidth, realHeight, virtualWidth, virtualHeight
        );
    }
}
