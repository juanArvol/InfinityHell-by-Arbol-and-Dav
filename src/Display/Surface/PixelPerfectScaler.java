package Display.Surface;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * PixelPerfectScaler — escalador manual para pixel-art con factores no-enteros.
 *
 * ── PROBLEMA ──────────────────────────────────────────────────────────────
 * Cuando el factor de escala no es entero (ej: 2.3x, 3.7x), Graphics2D.drawImage()
 * SIEMPRE interpola píxeles, incluso con NEAREST_NEIGHBOR y STROKE_NORMALIZE.
 * Esto causa artefactos visuales: píxeles duplicados, líneas remarcadas, etc.
 *
 * ── SOLUCIÓN ──────────────────────────────────────────────────────────────
 * Usar drawImage() con source/dest rectangles explícitos en lugar de
 * AffineTransform. Esto fuerza a Java2D a mapear cada píxel source a un
 * rectángulo dest específico sin sub-pixel positioning.
 *
 * Además, activamos todos los hints pixel-perfect antes del blit para
 * garantizar que no hay interpolación en ninguna etapa.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   PixelPerfectScaler.blit(
 *       screenG,
 *       framebuffer,
 *       destX, destY, destWidth, destHeight
 *   );
 *
 * ── RENDIMIENTO ──────────────────────────────��────────────────────────────
 * Este método usa la misma llamada a drawImage() que el path normal, por lo
 * que el rendimiento es idéntico. La diferencia está en NO usar AffineTransform,
 * lo cual evita cálculos de matriz de transformación en el rasterizer de AWT.
 */
public final class PixelPerfectScaler {

    private PixelPerfectScaler() {}

    /**
     * Dibuja el framebuffer en el contexto de pantalla con escalado pixel-perfect.
     *
     * @param screenG     Graphics2D de la pantalla (BufferStrategy)
     * @param framebuffer BufferedImage del framebuffer virtual
     * @param destX       X de destino en pantalla
     * @param destY       Y de destino en pantalla
     * @param destWidth   Ancho de destino en pantalla
     * @param destHeight  Alto de destino en pantalla
     */
    public static void blit(Graphics2D screenG,
                           BufferedImage framebuffer,
                           int destX, int destY,
                           int destWidth, int destHeight) {

        // Guardar hints actuales para restaurar después
        Object prevInterp = screenG.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object prevAA     = screenG.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object prevRender = screenG.getRenderingHint(RenderingHints.KEY_RENDERING);
        Object prevStroke = screenG.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        Object prevAlpha  = screenG.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
        Object prevDither = screenG.getRenderingHint(RenderingHints.KEY_DITHERING);

        // ⭐ CRITICAL: Configurar hints pixel-perfect ANTES del blit
        screenG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        screenG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        screenG.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_SPEED);
        screenG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_NORMALIZE);
        screenG.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        screenG.setRenderingHint(RenderingHints.KEY_DITHERING,
            RenderingHints.VALUE_DITHER_DISABLE);

        // ⭐ CRITICAL: Dibujar SIN AffineTransform activo
        // drawImage con src/dest rectangles explícitos evita sub-pixel positioning
        int srcWidth = framebuffer.getWidth();
        int srcHeight = framebuffer.getHeight();

        screenG.drawImage(
            framebuffer,
            destX, destY, destX + destWidth, destY + destHeight,  // dest rect
            0, 0, srcWidth, srcHeight,                            // source rect
            null  // no ImageObserver
        );

        // Restaurar hints previos
        if (prevInterp != null) screenG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevInterp);
        if (prevAA != null)     screenG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
        if (prevRender != null) screenG.setRenderingHint(RenderingHints.KEY_RENDERING, prevRender);
        if (prevStroke != null) screenG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, prevStroke);
        if (prevAlpha != null)  screenG.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, prevAlpha);
        if (prevDither != null) screenG.setRenderingHint(RenderingHints.KEY_DITHERING, prevDither);
    }

    /**
     * Dibuja el framebuffer con escalado entero (fast path optimizado).
     * Usa AffineTransform simple ya que no hay riesgo de interpolación.
     *
     * @param screenG     Graphics2D de la pantalla
     * @param framebuffer BufferedImage del framebuffer virtual
     * @param offsetX     Offset X en pantalla
     * @param offsetY     Offset Y en pantalla
     * @param scale       Factor de escala (debe ser entero)
     */
    public static void blitIntegerScale(Graphics2D screenG,
                                       BufferedImage framebuffer,
                                       int offsetX, int offsetY,
                                       int scale) {

        // Guardar hints
        Object prevInterp = screenG.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object prevAA     = screenG.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object prevRender = screenG.getRenderingHint(RenderingHints.KEY_RENDERING);

        // Hints mínimos para escalado entero (el transform es exacto)
        screenG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        screenG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        screenG.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_SPEED);

        // Fast path: usar translate + scale (es exacto para enteros)
        var savedTransform = screenG.getTransform();
        screenG.translate(offsetX, offsetY);
        screenG.scale(scale, scale);
        screenG.drawImage(framebuffer, 0, 0, null);
        screenG.setTransform(savedTransform);

        // Restaurar hints
        if (prevInterp != null) screenG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevInterp);
        if (prevAA != null)     screenG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
        if (prevRender != null) screenG.setRenderingHint(RenderingHints.KEY_RENDERING, prevRender);
    }
}
