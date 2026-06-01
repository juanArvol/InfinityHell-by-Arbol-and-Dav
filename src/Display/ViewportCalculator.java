package Display;

import Display.Background.FillArea;
import Display.Settings.ScalingMode;

import java.awt.Color;
import java.util.List;

/**
 * Calculador puro de viewport.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: ELIMINACIÓN DE OSCILACIÓN DE 1 PÍXEL EN FILL AREAS
 *
 * Problema original:
 *   calcFit() usaba Math.round(vW * scale) para calcular vpW y vpH.
 *   Con redondeo, cuando el canvas tiene ancho impar relativo al viewport,
 *   la división entera del offset (rW - vpW) / 2 produce offX = 0 mientras
 *   que rightGap = rW - offX - vpW = 1. Al siguiente pixel de resize,
 *   vpW se redondea diferente y offX salta a 1 mientras rightGap permanece
 *   en 1. Esto hace que el fill area derecho oscile entre 0px y 1px y 2px
 *   de forma no monotónica, produciendo el parpadeo visible.
 *
 * Causa raíz:
 *   Math.round(vW * scale) puede generar vpW que NO divide simétricamente
 *   rW. Cuando rW - vpW es impar, la barra derecha tiene 1px más que la
 *   izquierda, y la paridad cambia con cada pixel de cambio de canvas.
 *
 * Solución:
 *   Usar conversión a entero (truncamiento hacia cero, equivalente a floor
 *   para valores positivos) en lugar de Math.round para vpW y vpH.
 *   El truncamiento garantiza que vpW ≤ vW * scale siempre, por lo que
 *   el viewport NUNCA excede la escala correcta, y los fill areas cubren
 *   el resto de forma exacta y monotónica.
 *
 *   Garantía aritmética post-fix:
 *     offX = (rW - vpW) / 2            (división entera hacia cero)
 *     rightGap = rW - offX - vpW       (siempre ≥ leftGap)
 *     leftGap + vpW + rightGap = rW    (cubre el canvas exactamente)
 *
 *   Con esta garantía, los fill areas son exactos: ningún pixel del canvas
 *   queda sin cubrir, y el fill derecho nunca es menor que el izquierdo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * IDEMPOTENCIA
 *
 * El mismo par (realW, realH) produce siempre exactamente el mismo
 * ViewportInfo. No existe fuente de variación entre llamadas.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FÓRMULAS
 *
 * NATIVE        → escala 1.0, offset centrado si canvas > virtual.
 * FIT/LETTERBOX/
 * PILLARBOX     → min(realW/vW, realH/vH), vpW/vpH truncados (NO round).
 * FREE_SCALE    → igual que FIT.
 * INTEGER_SCALE → floor(min(realW/vW, realH/vH)), mínimo 1.
 * PIXEL_PERFECT → igual que INTEGER_SCALE, clamp a 1 si canvas < virtual.
 * STRETCH       → escala independiente en X e Y; sin barras.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 *   Sin estado interno → completamente thread-safe.
 */
public final class ViewportCalculator {

    private ViewportCalculator() {}

    /**
     * Calcula el ViewportInfo para los parámetros dados.
     *
     * Idempotente: los mismos parámetros producen siempre el mismo resultado.
     */
    public static ViewportInfo calculate(int virtualW, int virtualH,
                                         int realW, int realH,
                                         ScalingMode mode,
                                         Color fillColor) {
        // Sanidad: nunca calcular con dimensiones degeneradas
        if (realW <= 0 || realH <= 0) {
            realW = Math.max(1, realW);
            realH = Math.max(1, realH);
        }
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
        int offX = Math.max(0, (rW - vW) / 2);
        int offY = Math.max(0, (rH - vH) / 2);
        int vpW  = Math.min(vW, rW);
        int vpH  = Math.min(vH, rH);
        List<FillArea> areas = buildFillAreas(offX, offY, vpW, vpH, rW, rH, fill);
        return new ViewportInfo(offX, offY, vpW, vpH, 1.0f, rW, rH, vW, vH, areas);
    }

    /**
     * Modo FIT: escala uniforme para que el área virtual quepa en el canvas
     * con barras de relleno donde no cabe.
     *
     * CORRECCIÓN: usa truncamiento (int cast) en lugar de Math.round para
     * garantizar que vpW ≤ rW y vpH ≤ rH, y que los fill areas cubren
     * el canvas exactamente sin oscilaciones de 1 pixel.
     */
    private static ViewportInfo calcFit(int vW, int vH, int rW, int rH, Color fill) {
        float scale = Math.min((float) rW / vW, (float) rH / vH);

        // TRUNCAMIENTO, no redondeo. Garantiza vpW <= rW y vpH <= rH.
        // El fill area cubre el resto exactamente.
        int vpW = (int)(vW * scale);
        int vpH = (int)(vH * scale);

        // Centrado entero: el fill izquierdo/superior es floor((rW-vpW)/2).
        // El fill derecho/inferior cubre el resto: siempre >= fill izquierdo.
        int offX = (rW - vpW) / 2;
        int offY = (rH - vpH) / 2;

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
        float scale = (float) rW / vW;
        return new ViewportInfo(0, 0, rW, rH, scale, rW, rH, vW, vH, List.of());
    }

    // ── Utilidad: construir áreas de relleno ─────────────────────────────────

    /**
     * Construye la lista de FillArea para las zonas fuera del viewport.
     *
     * GARANTÍA: leftGap + vpW + rightGap == rW  (cubre el canvas exactamente).
     *           topGap + vpH + bottomGap == rH  (cubre el canvas exactamente).
     *
     * Con truncamiento para vpW/vpH y división entera para offX/offY,
     * esto siempre se cumple. No existen pixels sin cubrir.
     *
     * Si el viewport cubre exactamente el canvas: lista vacía.
     * Barras horizontales (letterbox): top + bottom.
     * Barras verticales (pillarbox):   left + right.
     * Ambas (casos extremos):          top + bottom + left + right.
     */
    static List<FillArea> buildFillAreas(int offX, int offY, int vpW, int vpH,
                                          int rW, int rH, Color fill) {
        // Calcular los tamaños reales de cada barra
        int rightW  = rW - offX - vpW;  // siempre >= offX con truncamiento
        int bottomH = rH - offY - vpH;  // siempre >= offY con truncamiento

        boolean hasHoriz = (offY > 0 || bottomH > 0);
        boolean hasVert  = (offX > 0 || rightW  > 0);

        if (!hasHoriz && !hasVert) return List.of();

        if (hasHoriz && !hasVert) {
            // Letterbox: solo barras horizontales
            var areas = new java.util.ArrayList<FillArea>(2);
            if (offY  > 0) areas.add(new FillArea(0,         0,   rW, offY,    fill));
            if (bottomH > 0) areas.add(new FillArea(0, offY + vpH, rW, bottomH, fill));
            return List.copyOf(areas);
        }

        if (hasVert && !hasHoriz) {
            // Pillarbox: solo barras verticales
            var areas = new java.util.ArrayList<FillArea>(2);
            if (offX   > 0) areas.add(new FillArea(0,          0, offX,   rH, fill));
            if (rightW > 0) areas.add(new FillArea(offX + vpW, 0, rightW, rH, fill));
            return List.copyOf(areas);
        }

        // Ambas (border): 4 barras
        var areas = new java.util.ArrayList<FillArea>(4);
        if (offY    > 0) areas.add(new FillArea(0,           0,   rW,    offY,    fill));
        if (bottomH > 0) areas.add(new FillArea(0,   offY + vpH,  rW,    bottomH, fill));
        if (offX    > 0) areas.add(new FillArea(0,           offY, offX,  vpH,     fill));
        if (rightW  > 0) areas.add(new FillArea(offX + vpW,  offY, rightW, vpH,    fill));
        return List.copyOf(areas);
    }
}
