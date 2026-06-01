package Display;

import Display.Background.FillArea;

import java.awt.Color;
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
 * Se recalcula completo en cada onResize().
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIO RESPECTO A LA VERSIÓN ANTERIOR
 *
 * Se añade {@code fillAreas}: lista de {@link FillArea} que describe
 * exactamente las zonas de relleno que deben pintarse en la presentación.
 *
 * Esto permite:
 *   (a) Pintar solo las zonas que realmente necesitan relleno, sin sobreescribir
 *       toda la superficie (más preciso y más eficiente en surface grandes).
 *   (b) Configurar el color de cada área de forma independiente.
 *   (c) El modo STRETCH puede devolver una lista vacía → sin relleno necesario.
 *   (d) Futura extensión: cada FillArea podría tener su propio DisplayBackground.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * COMPATIBILIDAD
 *
 * El constructor completo acepta fillAreas. Los sistemas que no necesitan
 * gestionar barras de relleno (cámara, UI, input) no tocan este campo.
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
     * Lista con 1 o 2 elementos en modos FIT, INTEGER_SCALE, etc.
     *
     * El color de cada área proviene del {@code fillColor} con que se calculó
     * el viewport. Es inmutable; si cambia el color, se recalcula el viewport.
     */
    public final List<FillArea> fillAreas;

    /**
     * Constructor completo (con áreas de relleno).
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
        this.fillAreas     = List.copyOf(fillAreas); // inmutable
    }

    /**
     * Constructor de compatibilidad sin áreas de relleno.
     * Genera automáticamente las áreas de relleno con color negro.
     * Útil para código existente que no gestiona colores de relleno.
     */
    public ViewportInfo(int x, int y, int width, int height, float scale,
                        int realWidth, int realHeight,
                        int virtualWidth, int virtualHeight) {
        this(x, y, width, height, scale, realWidth, realHeight, virtualWidth, virtualHeight,
             computeDefaultFillAreas(x, y, width, height, realWidth, realHeight, Color.WHITE));
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

    // ── Privados ──────────────────────────────────────────────────────────────

    private static List<FillArea> computeDefaultFillAreas(
            int x, int y, int vpW, int vpH,
            int realW, int realH, Color color) {

        // Barras horizontales (letterbox): arriba y abajo
        if (y > 0) {
            return List.of(
                new FillArea(0, 0, realW, y, color),                        // top
                new FillArea(0, y + vpH, realW, realH - y - vpH, color)   // bottom
            );
        }
        // Barras verticales (pillarbox): izquierda y derecha
        if (x > 0) {
            return List.of(
                new FillArea(0, 0, x, realH, color),                        // left
                new FillArea(x + vpW, 0, realW - x - vpW, realH, color)   // right
            );
        }
        return List.of();
    }

    @Override
    public String toString() {
        return String.format(
            "ViewportInfo[x=%d y=%d w=%d h=%d scale=%.4f real=%dx%d virtual=%dx%d fills=%d]",
            x, y, width, height, scale, realWidth, realHeight,
            virtualWidth, virtualHeight, fillAreas.size()
        );
    }
}
