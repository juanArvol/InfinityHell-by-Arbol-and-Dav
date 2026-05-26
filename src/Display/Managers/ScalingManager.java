package Display.Managers;

import Display.Settings.DisplaySettings;
import Display.Settings.ScalingMode;
import Display.ViewportInfo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Escala el framebuffer virtual hacia el canvas real.
 *
 * RESPONSABILIDAD ÚNICA: dada una imagen (framebuffer virtual) y un
 * ViewportInfo, dibujarla correctamente escalada + las barras de
 * letterbox/pillarbox si aplica.
 *
 * REGLA: NINGUNA otra clase hace AffineTransform para escalar el juego.
 * Todo el escalado pasa por aquí.
 *
 * ScalingManager NO conoce el Canvas ni el BufferStrategy.
 * Recibe un Graphics2D ya preparado para dibujar en él.
 */
public class ScalingManager {

    private final DisplaySettings settings;

    public ScalingManager(DisplaySettings settings) {
        this.settings = settings;
    }

    /**
     * Presenta el framebuffer virtual al Graphics2D de pantalla real.
     *
     * Pasos:
     *  1. Rellenar toda la pantalla con color letterbox (barras negras)
     *  2. Escalar y posicionar el framebuffer según el viewport
     *  3. Restaurar la transformación original
     *
     * @param screenG    Graphics2D del canvas real (de BufferStrategy)
     * @param framebuffer imagen virtual ya renderizada
     * @param viewport   cálculo actual de viewport (de ViewportManager)
     */
    public void present(Graphics2D screenG,
                        BufferedImage framebuffer,
                        ViewportInfo viewport) {

        // 1. Rellenar con letterbox color (cubre barras negras)
        screenG.setColor(settings.letterboxColor);
        screenG.fillRect(0, 0, viewport.realWidth, viewport.realHeight);

        // 2. Aplicar transformación para escalar el framebuffer al viewport
        AffineTransform originalTransform = screenG.getTransform();

        applyScalingHints(screenG);

        if (settings.scalingMode == ScalingMode.STRETCH) {
            presentStretch(screenG, framebuffer, viewport);
        } else {
            presentUniform(screenG, framebuffer, viewport);
        }

        // 3. Restaurar transformación para que el llamador no quede afectado
        screenG.setTransform(originalTransform);
    }

    // ─── Modos de presentación ────────────────────────────────────────────────

    /**
     * Presentación uniforme (FIT, FILL, INTEGER_SCALE, PIXEL_PERFECT).
     * Escala con factor uniforme y posiciona con offset (barras negras).
     */
    private void presentUniform(Graphics2D g, BufferedImage fb, ViewportInfo vp) {
        // translate al offset del viewport (barra izquierda / barra arriba)
        g.translate(vp.x, vp.y);

        // scale uniforme
        g.scale(vp.scale, vp.scale);

        // drawImage a coordenadas 0,0 del espacio virtual (ya transformado)
        g.drawImage(fb, 0, 0, null);
    }

    /**
     * Presentación STRETCH: escala X e Y independientes para llenar pantalla.
     */
    private void presentStretch(Graphics2D g, BufferedImage fb, ViewportInfo vp) {
        // Sin translate (parte desde 0,0)
        // Scale separado X e Y
        double scaleX = (double) vp.realWidth  / settings.virtualWidth;
        double scaleY = (double) vp.realHeight / settings.virtualHeight;

        g.scale(scaleX, scaleY);
        g.drawImage(fb, 0, 0, null);
    }

    // ─── Hints de calidad para el escalado ────────────────────────────────────

    private void applyScalingHints(Graphics2D g) {
        if (settings.useInterpolation) {
            // HD / juegos con assets de alta resolución
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_QUALITY);
        } else {
            // Pixel art: nearest neighbor preserva los píxeles duros
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_SPEED);
        }
    }
}
