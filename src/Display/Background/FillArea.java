package Display.Background;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Describe y renderiza un área de relleno (letterbox / pillarbox).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * Problema original:
 *   RenderSurfaceManager.present() calculaba y pintaba las barras de relleno
 *   con un único fillRect sobre toda la superficie:
 *
 *     screenG.setColor(letterboxColor);
 *     screenG.fillRect(0, 0, viewport.realWidth, viewport.realHeight);
 *
 *   Esta estrategia sobreescribe la zona de juego antes de dibujar el
 *   framebuffer encima, lo que funciona pero:
 *   (a) El color de relleno estaba hardcodeado en la construcción.
 *   (b) No había forma de pintar las barras laterales y superiores
 *       con colores distintos (e.g. debug visual).
 *   (c) El concepto de "barra de relleno" no tenía representación propia.
 *
 * Solución:
 *   FillArea es un value object que representa una de las áreas de relleno.
 *   ViewportInfo puede exponer las áreas de relleno calculadas.
 *   DisplayFillRenderer usa estas áreas para pintar con pleno control.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * EXTENSIBILIDAD
 *
 * En el futuro FillArea puede soportar:
 *   - Gradiente o textura en lugar de color sólido.
 *   - Opacidad variable (fade en transiciones).
 *   - Render delegado a una DisplayBackground específica para cada barra.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * INMUTABILIDAD
 *
 *   FillArea es inmutable. Si el viewport cambia, se crean nuevas instancias.
 */
public final class FillArea {

    /** Posición X del área de relleno en el canvas físico. */
    public final int x;

    /** Posición Y del área de relleno en el canvas físico. */
    public final int y;

    /** Ancho del área de relleno en píxeles físicos. */
    public final int width;

    /** Alto del área de relleno en píxeles físicos. */
    public final int height;

    /** Color con el que se rellena esta área. */
    public final Color color;

    public FillArea(int x, int y, int width, int height, Color color) {
        if (color == null) throw new IllegalArgumentException("color cannot be null");
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
        this.color  = color;
    }

    /** True si el área es vacía (sin tamaño). */
    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    /**
     * Pinta este área sobre el Graphics2D dado.
     * No modifica el estado de color del contexto fuera de esta llamada.
     */
    public void paint(Graphics2D g) {
        if (isEmpty()) return;
        Color saved = g.getColor();
        g.setColor(color);
        g.fillRect(x, y, width, height);
        g.setColor(saved);
    }

    @Override
    public String toString() {
        return String.format("FillArea[x=%d y=%d w=%d h=%d color=%s]", x, y, width, height, color);
    }
}
