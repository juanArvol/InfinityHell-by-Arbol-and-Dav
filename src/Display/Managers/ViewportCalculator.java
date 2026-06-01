package Display.Managers;

import Display.Background.FillArea;
import Display.Settings.ScalingMode;
import Display.ViewportInfo;

import java.awt.Color;
import java.util.List;

/**
 * Calculador puro de viewport.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * Problema original:
 *   ViewportManager combinaba dos responsabilidades:
 *   (a) Mantener el estado del viewport actual (currentViewport, volatile).
 *   (b) Calcular matemáticamente el viewport para cada ScalingMode.
 *
 *   Esto mezclaba lógica de estado/threading con lógica de cálculo puro.
 *   Añadir un modo nuevo implicaba modificar ViewportManager directamente.
 *   Tampoco era posible testear los cálculos sin instanciar el manager completo.
 *
 * Solución:
 *   ViewportCalculator extrae la responsabilidad de CÁLCULO puro.
 *   Es una clase sin estado: solo recibe parámetros y devuelve ViewportInfo.
 *   ViewportManager usa ViewportCalculator; ya no contiene la lógica matemática.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * EXTENSIBILIDAD
 *
 * Para agregar un modo nuevo:
 *   1. Añadir la constante en ScalingMode.
 *   2. Añadir un case en calculate() que llame al nuevo método privado.
 *   3. Ningún otro fichero del módulo requiere modificación.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FÓRMULAS
 *
 * NATIVE        → escala 1.0, offset centrado si canvas > virtual.
 * FIT/LETTERBOX/
 * PILLARBOX     → min(realW/vW, realH/vH), centrado.
 * FREE_SCALE    → igual que FIT (escala libre).
 * INTEGER_SCALE → floor(min(realW/vW, realH/vH)), mínimo 1.
 * PIXEL_PERFECT → igual que INTEGER_SCALE, con clamp a 1 si canvas < virtual.
 * STRETCH       → escala independiente en X e Y; sin barras.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 *   Sin estado interno → completamente thread-safe. Puede llamarse desde
 *   cualquier thread sin sincronización.
 */
public final class ViewportCalculator {

    private ViewportCalculator() {}

    /**
     * Calcula el ViewportInfo para los parámetros dados.
     *
     * @param virtualW   ancho virtual del juego
     * @param virtualH   alto virtual del juego
     * @param realW      ancho real del canvas
     * @param realH      alto real del canvas
     * @param mode       modo de escalado
     * @param fillColor  color de relleno para las barras de letterbox/pillarbox
     * @return           ViewportInfo inmutable con todos los datos de presentación
     */
    public static ViewportInfo calculate(int virtualW, int virtualH,
                                         int realW, int realH,
                                         ScalingMode mode,
                                         Color fillColor) {
        return switch (mode) {
            case NATIVE                       -> calcNative(virtualW, virtualH, realW, realH, fillColor);
            case FIT, LETTERBOX, PILLARBOX,
                 FREE_SCALE                   -> calcFit(virtualW, virtualH, realW, realH, fillColor);
            case INTEGER_SCALE                -> calcIntegerScale(virtualW, virtualH, realW, realH, fillColor);
            case PIXEL_PERFECT                -> calcPixelPerfect(virtualW, virtualH, realW, realH, fillColor);
            case STRETCH                      -> calcStretch(virtualW, virtualH, realW, realH);
        };
    }

    // ── Modos ─────────────────────────────────────────────────────────────────

    private static ViewportInfo calcNative(int vW, int vH, int rW, int rH, Color fill) {
        // El framebuffer se presenta a 1:1. Si el canvas es más grande, queda centrado.
        int offX = Math.max(0, (rW - vW) / 2);
        int offY = Math.max(0, (rH - vH) / 2);
        int vpW  = Math.min(vW, rW);
        int vpH  = Math.min(vH, rH);
        List<FillArea> areas = buildFillAreas(offX, offY, vpW, vpH, rW, rH, fill);
        return new ViewportInfo(offX, offY, vpW, vpH, 1.0f, rW, rH, vW, vH, areas);
    }

    private static ViewportInfo calcFit(int vW, int vH, int rW, int rH, Color fill) {
        float scale = Math.min((float) rW / vW, (float) rH / vH);
        int vpW     = Math.round(vW * scale);
        int vpH     = Math.round(vH * scale);
        int offX    = (rW - vpW) / 2;
        int offY    = (rH - vpH) / 2;
        List<FillArea> areas = buildFillAreas(offX, offY, vpW, vpH, rW, rH, fill);
        return new ViewportInfo(offX, offY, vpW, vpH, scale, rW, rH, vW, vH, areas);
    }

    private static ViewportInfo calcIntegerScale(int vW, int vH, int rW, int rH, Color fill) {
        int scale = Math.min(
            Math.max(1, rW / vW),
            Math.max(1, rH / vH)
        );
        int vpW  = vW * scale;
        int vpH  = vH * scale;
        int offX = (rW - vpW) / 2;
        int offY = (rH - vpH) / 2;
        List<FillArea> areas = buildFillAreas(offX, offY, vpW, vpH, rW, rH, fill);
        return new ViewportInfo(offX, offY, vpW, vpH, (float) scale, rW, rH, vW, vH, areas);
    }

    private static ViewportInfo calcPixelPerfect(int vW, int vH, int rW, int rH, Color fill) {
        // Igual que INTEGER_SCALE pero con garantía de ≥ 1× incluso si el canvas
        // es más pequeño que el virtual (sin downscaling).
        int scale = (rW < vW || rH < vH)
            ? 1
            : Math.min(Math.max(1, rW / vW), Math.max(1, rH / vH));
        int vpW  = vW * scale;
        int vpH  = vH * scale;
        int offX = Math.max(0, (rW - vpW) / 2);
        int offY = Math.max(0, (rH - vpH) / 2);
        List<FillArea> areas = buildFillAreas(offX, offY, vpW, vpH, rW, rH, fill);
        return new ViewportInfo(offX, offY, vpW, vpH, (float) scale, rW, rH, vW, vH, areas);
    }

    private static ViewportInfo calcStretch(int vW, int vH, int rW, int rH) {
        // Sin barras. El scale en X e Y es diferente; se usa scaleX solo como
        // referencia (el renderizado real usa translate+scale separado en present()).
        float scale = (float) rW / vW;
        return new ViewportInfo(0, 0, rW, rH, scale, rW, rH, vW, vH, List.of());
    }

    // ── Utilidad: construir áreas de relleno ─────────────────────────────────

    /**
     * Construye la lista de FillArea para las zonas fuera del viewport.
     *
     * Si el viewport cubre exactamente el canvas (sin barras), devuelve vacío.
     * Si hay barras horizontales (letterbox): top + bottom.
     * Si hay barras verticales (pillarbox): left + right.
     */
    static List<FillArea> buildFillAreas(int offX, int offY, int vpW, int vpH,
                                          int rW, int rH, Color fill) {
        boolean hasTop    = offY > 0;
        boolean hasLeft   = offX > 0;

        if (!hasTop && !hasLeft) return List.of();

        // Letterbox: barras horizontales (arriba y abajo)
        if (hasTop && !hasLeft) {
            FillArea top    = new FillArea(0, 0, rW, offY, fill);
            FillArea bottom = new FillArea(0, offY + vpH, rW, rH - offY - vpH, fill);
            return List.of(top, bottom);
        }

        // Pillarbox: barras verticales (izquierda y derecha)
        if (hasLeft && !hasTop) {
            FillArea left  = new FillArea(0, 0, offX, rH, fill);
            FillArea right = new FillArea(offX + vpW, 0, rW - offX - vpW, rH, fill);
            return List.of(left, right);
        }

        // Ambos (ratio exactamente igual → no debería ocurrir en FIT,
        // pero puede ocurrir en PIXEL_PERFECT si el factor entero deja resto en ambos ejes)
        FillArea top    = new FillArea(0, 0, rW, offY, fill);
        FillArea bottom = new FillArea(0, offY + vpH, rW, rH - offY - vpH, fill);
        FillArea left   = new FillArea(0, offY, offX, vpH, fill);
        FillArea right  = new FillArea(offX + vpW, offY, rW - offX - vpW, vpH, fill);
        return List.of(top, bottom, left, right);
    }
}
