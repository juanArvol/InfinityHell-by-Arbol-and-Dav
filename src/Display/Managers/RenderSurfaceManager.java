package Display.Managers;

import Display.Settings.DisplaySettings;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Gestiona el framebuffer virtual (BufferedImage de resolución fija).
 *
 * RESPONSABILIDAD: crear y mantener el framebuffer virtual al que
 * se renderiza TODO el juego, antes de escalarlo a pantalla.
 *
 * El framebuffer tiene SIEMPRE el tamaño de la resolución virtual.
 * Nunca cambia de tamaño — la pantalla se adapta a él, no al revés.
 *
 * Uso:
 *   Graphics2D vg = surfaceManager.beginFrame();
 *   // ... render del juego a vg ...
 *   surfaceManager.endFrame(vg);
 *   // Luego ScalingManager dibuja getFramebuffer() a la pantalla real.
 */
public class RenderSurfaceManager {

    private final DisplaySettings settings;
    private BufferedImage framebuffer;

    public RenderSurfaceManager(DisplaySettings settings) {
        this.settings = settings;
        this.framebuffer = createFramebuffer();
    }

    /**
     * Inicia un frame: devuelve el Graphics2D del framebuffer virtual listo para usar.
     *
     * IMPORTANTE: siempre llamar endFrame(g) cuando termines de renderizar.
     * No almacenar el Graphics2D entre frames.
     */
    public Graphics2D beginFrame() {
        Graphics2D g = framebuffer.createGraphics();
        applyRenderHints(g);

        // Limpiar el framebuffer al inicio de cada frame
        g.setBackground(java.awt.Color.WHITE);
        g.clearRect(0, 0, settings.virtualWidth, settings.virtualHeight);

        return g;
    }

    /**
     * Termina el frame: dispone el Graphics2D.
     * NO presentar a pantalla — eso lo hace ScalingManager.
     */
    public void endFrame(Graphics2D g) {
        if (g != null) g.dispose();
    }

    /**
     * Acceso al framebuffer completado para que ScalingManager lo escale.
     * Solo leer — no dibujar directamente aquí fuera del ciclo beginFrame/endFrame.
     */
    public BufferedImage getFramebuffer() {
        return framebuffer;
    }

    /**
     * Ancho del framebuffer (= virtualWidth de settings).
     */
    public int getWidth() {
        return settings.virtualWidth;
    }

    /**
     * Alto del framebuffer (= virtualHeight de settings).
     */
    public int getHeight() {
        return settings.virtualHeight;
    }

    // ─── Internos ─────────────────────────────────────────────────────────────

    private BufferedImage createFramebuffer() {
        // TYPE_INT_ARGB ofrece compatibilidad con transparencias y
        // mejor performance en la mayoría de JVMs modernas.
        return new BufferedImage(
            settings.virtualWidth,
            settings.virtualHeight,
            BufferedImage.TYPE_INT_ARGB
        );
    }

    private void applyRenderHints(Graphics2D g) {
        if (settings.useInterpolation) {
            // Para juegos HD: suavizado activado
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else {
            // Para pixel art: sin suavizado, nearest neighbor
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }
    }
}
