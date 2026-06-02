package Display;

import Display.Background.FillArea;
import Display.Settings.ScalingMode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculador puro de viewport. Única fuente de verdad en el subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: UNIFICACIÓN DE ViewportCalculator
 *
 * Problema anterior:
 *   Existían DOS clases con el mismo nombre y responsabilidad:
 *     - Display.ViewportCalculator          (con truncamiento, corregida)
 *     - Display.Managers.ViewportCalculator (con Math.round, original)
 *
 *   ViewportManager importaba Display.Managers.ViewportCalculator (Math.round).
 *   El pipeline y DisplayManager usaban el viewport producido por ViewportManager,
 *   que calculaba FillAreas con Math.round. Esto hacía que la barra derecha
 *   oscilara entre 0, 1 y 2px de forma no monotónica con cada pixel de resize,
 *   produciendo el parpadeo visible en la barra lateral.
 *
 *   Display.Managers.ViewportCalculator ha sido eliminado. Esta clase es la
 *   única implementación del cálculo de viewport en todo el subsistema.
 *
 * Solución — truncamiento (floor) en lugar de Math.round:
 *
 *   Con Math.round(vW * scale):
 *     vpW puede ser mayor que la proporción exacta → fill derecho = 0 o negativo.
 *     La paridad de (rW - vpW) cambia con cada pixel → oscilación de 1px.
 *
 *   Con (int)(vW * scale) [truncamiento hacia cero]:
 *     vpW ≤ vW * scale siempre (nunca excede).
 *     offX = (rW - vpW) / 2 es siempre ≥ 0.
 *     rightGap = rW - offX - vpW es siempre ≥ offX.
 *     La suma offX + vpW + rightGap = rW exactamente, sin pixels sin cubrir.
 *     El comportamiento es monótonamente creciente con el tamaño del canvas:
 *     nunca hay oscilaciones.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * IDEMPOTENCIA
 *
 * El mismo conjunto de parámetros produce siempre exactamente el mismo
 * ViewportInfo. No existe fuente de variación entre llamadas.
 * El viewport es completamente determinista.
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
 *   Puede llamarse desde cualquier thread sin sincronización.
 */
public final class ViewportCalculator {

    private ViewportCalculator() {}

    /**
     * Calcula el ViewportInfo para los parámetros dados.
     *
     * Idempotente y determinista: los mismos parámetros producen siempre
     * el mismo resultado.
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
                                         int realW,    int realH,
                                         ScalingMode mode,
                                         Color fillColor) {
        // Sanidad: nunca calcular con dimensiones degeneradas.
        realW = Math.max(1, realW);
        realH = Math.max(1, realH);
        virtualW = Math.max(1, virtualW);
        virtualH = Math.max(1, virtualH);

        return switch (mode) {
            case NATIVE ->
                calcNative(virtualW, virtualH, realW, realH, fillColor);
            case FIT, LETTERBOX, PILLARBOX, FREE_SCALE ->
                calcFit(virtualW, virtualH, realW, realH, fillColor);
            case INTEGER_SCALE ->
                calcIntegerScale(virtualW, virtualH, realW, realH, fillColor);
            case PIXEL_PERFECT ->
                calcPixelPerfect(virtualW, virtualH, realW, realH, fillColor);
            case STRETCH ->
                calcStretch(virtualW, virtualH, realW, realH);
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
     * Modo FIT: escala uniforme para que el área virtual quepa en el canvas,
     * con barras de relleno donde no cabe.
     *
     * TRUNCAMIENTO — no Math.round — para garantizar vpW ≤ rW y vpH ≤ rH,
     * y que los fill areas cubren el canvas exactamente sin oscilaciones de 1px.
     */
    private static ViewportInfo calcFit(int vW, int vH, int rW, int rH, Color fill) {
        float scale = Math.min((float) rW / vW, (float) rH / vH);

        // (int) trunca hacia cero = floor para valores positivos.
        // Garantiza vpW <= rW y vpH <= rH sin excepción.
        int vpW  = (int)(vW * scale);
        int vpH  = (int)(vH * scale);
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

    // ── Construcción de áreas de relleno ─────────────────────────────────────

    /**
     * Construye la lista de FillArea para las zonas fuera del viewport.
     *
     * GARANTÍA aritmética (con truncamiento para vpW/vpH):
     *   offX + vpW + rightW  == rW   (cubre el canvas exactamente en horizontal)
     *   offY + vpH + bottomH == rH   (cubre el canvas exactamente en vertical)
     *
     * No existen pixels sin cubrir. Los fill areas nunca se solapan con
     * el área de juego.
     *
     * Si el viewport cubre exactamente el canvas: lista vacía.
     * Barras horizontales (letterbox): top + bottom.
     * Barras verticales  (pillarbox):  left + right.
     * Ambas (esquinas visibles):       top + bottom + left + right.
     */
    public static List<FillArea> buildFillAreas(int offX, int offY,
                                                 int vpW,  int vpH,
                                                 int rW,   int rH,
                                                 Color fill) {
        int rightW  = rW - offX - vpW;
        int bottomH = rH - offY - vpH;

        boolean hasHoriz = (offY > 0 || bottomH > 0);
        boolean hasVert  = (offX > 0 || rightW  > 0);

        if (!hasHoriz && !hasVert) return List.of();

        List<FillArea> areas = new ArrayList<>(4);

        if (hasHoriz) {
            if (offY    > 0) areas.add(new FillArea(0,           0,    rW,     offY,    fill));
            if (bottomH > 0) areas.add(new FillArea(0,   offY + vpH,   rW,     bottomH, fill));
        }
        if (hasVert) {
            // Las barras laterales cubren solo la franja entre las barras horizontales
            // (o todo el alto si no hay barras horizontales) para no solaparse.
            int sideY = hasHoriz ? offY : 0;
            int sideH = hasHoriz ? vpH  : rH;
            if (offX   > 0) areas.add(new FillArea(0,           sideY, offX,   sideH,   fill));
            if (rightW > 0) areas.add(new FillArea(offX + vpW,  sideY, rightW, sideH,   fill));
        }

        return List.copyOf(areas);
    }
}
