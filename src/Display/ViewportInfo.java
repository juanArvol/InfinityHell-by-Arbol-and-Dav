package Display;

import Display.Background.FillArea;

import java.util.List;

/**
 * Resultado inmutable del cálculo de viewport.
 *
 * Contiene TODOS los datos necesarios para:
 *  - Posicionar y escalar el framebuffer virtual en pantalla.
 *  - Transformar coordenadas pantalla ↔ mundo virtual.
 *  - Calcular bounds de UI.
 *  - Pintar las áreas de relleno (letterbox / pillarbox).
 *
 * Es un value object: sin estado mutable, sin lógica de cálculo.
 * Se recalcula completo en cada onResize() a través de ViewportCalculator.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: ELIMINACIÓN DEL CONSTRUCTOR DE COMPATIBILIDAD CON Color.WHITE
 *
 * Problema anterior:
 *   El constructor de compatibilidad sin fillAreas llamaba a
 *   computeDefaultFillAreas(..., Color.WHITE) con blanco hardcodeado.
 *   Cualquier código que construyera ViewportInfo sin especificar fillColor
 *   obtenía barras blancas en lugar del fillColor configurado.
 *   Esto producía flashes blancos durante transiciones cuando el viewport
 *   se construía fuera del flujo normal del ViewportCalculator.
 *
 * Solución:
 *   El constructor de compatibilidad ha sido eliminado. Toda instancia de
 *   ViewportInfo debe construirse a través de ViewportCalculator.calculate(),
 *   que es la única fuente válida de ViewportInfo en el subsistema.
 *   No existe camino alternativo que pueda producir fill areas con color
 *   incorrecto.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 * Inmutable. Seguro compartir entre threads sin sincronización.
 */
public final class ViewportInfo {

    /** Offset X del viewport dentro del canvas (pillarbox). */
    public final int x;

    /** Offset Y del viewport dentro del canvas (letterbox). */
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

    /** Ancho virtual del juego (constante durante la sesión). */
    public final int virtualWidth;

    /** Alto virtual del juego (constante durante la sesión). */
    public final int virtualHeight;

    /**
     * Áreas de relleno calculadas para este viewport.
     *
     * Lista vacía si el modo de escalado no produce barras (STRETCH, NATIVE exacto).
     * Construida exclusivamente por ViewportCalculator; el color proviene del
     * fillColor configurado en DisplaySettings — nunca hardcodeado.
     */
    public final List<FillArea> fillAreas;

    /**
     * Constructor canónico. Solo debe ser invocado por ViewportCalculator.
     */
    public ViewportInfo(int x, int y, int width, int height, float scale,
                        int realWidth, int realHeight,
                        int virtualWidth, int virtualHeight,
                        List<FillArea> fillAreas) {
        this.x             = x;
        this.y             = y;
        this.width         = width;
        this.height        = height;
        this.scale         = scale;
        this.realWidth     = realWidth;
        this.realHeight    = realHeight;
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
        this.fillAreas     = List.copyOf(fillAreas);
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * True si el punto de pantalla está dentro del área de juego
     * (no en las barras de relleno).
     */
    public boolean containsScreenPoint(int sx, int sy) {
        return sx >= x && sx < x + width &&
               sy >= y && sy < y + height;
    }

    // ── Transformaciones de coordenadas ──────────────────────────────────────

    /** Convierte X de pantalla real a X virtual del juego. */
    public float toVirtualX(int screenX) { return (screenX - x) / scale; }

    /** Convierte Y de pantalla real a Y virtual del juego. */
    public float toVirtualY(int screenY) { return (screenY - y) / scale; }

    /** Convierte X virtual a X de pantalla real. */
    public int toScreenX(float virtualX) { return (int)(virtualX * scale) + x; }

    /** Convierte Y virtual a Y de pantalla real. */
    public int toScreenY(float virtualY) { return (int)(virtualY * scale) + y; }

    @Override
    public String toString() {
        return String.format(
            "ViewportInfo[x=%d y=%d w=%d h=%d scale=%.4f real=%dx%d virtual=%dx%d fills=%d]",
            x, y, width, height, scale, realWidth, realHeight,
            virtualWidth, virtualHeight, fillAreas.size()
        );
    }
}
