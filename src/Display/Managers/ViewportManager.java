package Display.Managers;

import Display.ViewportInfo;
import Display.Settings.DisplaySettings;
import Display.Settings.ScalingMode;

/**
 * Calcula y mantiene el viewport actual.
 *
 * RESPONSABILIDAD ÚNICA: dado un tamaño real de pantalla y la configuración
 * virtual, calcular dónde y a qué escala dibujar el framebuffer.
 *
 * REGLA: NINGUNA otra clase calcula escalas o viewports.
 * Todas las clases que necesiten estos datos deben obtenerlos de aquí.
 *
 * Thread-safety: getViewport() devuelve referencia inmutable.
 * onResize() debe llamarse desde el EDT o con sincronización externa.
 */
public class ViewportManager {

    private final DisplaySettings settings;
    private volatile ViewportInfo currentViewport;

    public ViewportManager(DisplaySettings settings) {
        this.settings = settings;
        // Inicializar con la resolución virtual como base
        this.currentViewport = calculate(
            settings.virtualWidth,
            settings.virtualHeight
        );
    }

    /**
     * Recalcula el viewport cuando cambia el tamaño real de pantalla.
     * Llamar desde el resize handler del canvas/frame.
     *
     * @param realWidth  ancho real actual del canvas
     * @param realHeight alto real actual del canvas
     */
    public void onResize(int realWidth, int realHeight) {
        if (realWidth <= 0 || realHeight <= 0) return;
        this.currentViewport = calculate(realWidth, realHeight);
    }

    /**
     * Obtiene el viewport actual calculado.
     * Inmutable — seguro compartir sin copiar.
     */
    public ViewportInfo getViewport() {
        return currentViewport;
    }

    // ─── Cálculo según ScalingMode ────────────────────────────────────────────

    private ViewportInfo calculate(int realW, int realH) {
        return switch (settings.scalingMode) {
            case FIT            -> calculateFit(realW, realH);
            case FILL           -> calculateFill(realW, realH);
            case STRETCH        -> calculateStretch(realW, realH);
            case INTEGER_SCALE  -> calculateIntegerScale(realW, realH);
            case PIXEL_PERFECT  -> calculatePixelPerfect(realW, realH);
        };
    }

    /**
     * FIT: escala manteniendo aspect ratio.
     * El eje "sobrante" recibe barras negras (letterbox o pillarbox).
     *
     * scale = min(realW/vW, realH/vH)
     */
    private ViewportInfo calculateFit(int realW, int realH) {
        float vW = settings.virtualWidth;
        float vH = settings.virtualHeight;

        float scaleX = realW / vW;
        float scaleY = realH / vH;
        float scale  = Math.min(scaleX, scaleY);

        int vpW = Math.round(vW * scale);
        int vpH = Math.round(vH * scale);
        int vpX = (realW - vpW) / 2;
        int vpY = (realH - vpH) / 2;

        return new ViewportInfo(vpX, vpY, vpW, vpH, scale, realW, realH,
                                settings.virtualWidth, settings.virtualHeight);
    }

    /**
     * FILL: escala para cubrir toda la pantalla manteniendo aspect ratio.
     * Puede recortar el juego por los bordes.
     *
     * scale = max(realW/vW, realH/vH)
     */
    private ViewportInfo calculateFill(int realW, int realH) {
        float vW = settings.virtualWidth;
        float vH = settings.virtualHeight;

        float scaleX = realW / vW;
        float scaleY = realH / vH;
        float scale  = Math.max(scaleX, scaleY);

        int vpW = Math.round(vW * scale);
        int vpH = Math.round(vH * scale);
        // Centrado (partes recortadas salen por los bordes)
        int vpX = (realW - vpW) / 2;
        int vpY = (realH - vpH) / 2;

        return new ViewportInfo(vpX, vpY, vpW, vpH, scale, realW, realH,
                                settings.virtualWidth, settings.virtualHeight);
    }

    /**
     * STRETCH: deforma el juego para llenar exactamente la pantalla.
     * No hay barras, pero el aspect ratio NO se preserva.
     */
    private ViewportInfo calculateStretch(int realW, int realH) {
        // En STRETCH el "scale" uniforme no tiene sentido.
        // Usamos scaleX para reportar, pero el render usará
        // AffineTransform con scaleX y scaleY separados.
        // ViewportInfo reporta scale = scaleX como referencia.
        float scale = (float) realW / settings.virtualWidth;

        return new ViewportInfo(0, 0, realW, realH, scale, realW, realH,
                                settings.virtualWidth, settings.virtualHeight);
    }

    /**
     * INTEGER_SCALE: solo factores enteros (1x, 2x, 3x…).
     * Ideal para pixel art — cada píxel virtual = N píxeles exactos.
     */
    private ViewportInfo calculateIntegerScale(int realW, int realH) {
        float vW = settings.virtualWidth;
        float vH = settings.virtualHeight;

        // Factor entero máximo que cabe
        int scaleX = Math.max(1, (int)(realW / vW));
        int scaleY = Math.max(1, (int)(realH / vH));
        int scale  = Math.min(scaleX, scaleY);

        int vpW = (int)(vW * scale);
        int vpH = (int)(vH * scale);
        int vpX = (realW - vpW) / 2;
        int vpY = (realH - vpH) / 2;

        return new ViewportInfo(vpX, vpY, vpW, vpH, (float) scale, realW, realH,
                                settings.virtualWidth, settings.virtualHeight);
    }

    /**
     * PIXEL_PERFECT: como INTEGER_SCALE pero nunca amplia (máximo 1x si no cabe).
     * Para retro puro donde "1 píxel virtual = exactamente 1 píxel real" es necesario.
     */
    private ViewportInfo calculatePixelPerfect(int realW, int realH) {
        float vW = settings.virtualWidth;
        float vH = settings.virtualHeight;

        // Si la pantalla es más pequeña que la virtual, escala hacia abajo (fracción)
        // Si es más grande, usa enteros hacia arriba
        int scaleX = Math.max(1, (int)(realW / vW));
        int scaleY = Math.max(1, (int)(realH / vH));
        int scale  = Math.min(scaleX, scaleY);

        // Si la pantalla es más pequeña que virtual, scale=1 (no ampliar el framebuffer)
        if (realW < settings.virtualWidth || realH < settings.virtualHeight) {
            scale = 1;
        }

        int vpW = (int)(vW * scale);
        int vpH = (int)(vH * scale);
        int vpX = Math.max(0, (realW - vpW) / 2);
        int vpY = Math.max(0, (realH - vpH) / 2);

        return new ViewportInfo(vpX, vpY, vpW, vpH, (float) scale, realW, realH,
                                settings.virtualWidth, settings.virtualHeight);
    }
}
