package Display.Surface;

import Display.Settings.ScalingMode;
import Display.ViewportInfo;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;

/**
 * Contrato de acceso a una superficie durante exactamente un frame de render.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CICLO DE VIDA
 *
 *   RenderFrame frame = gateway.acquireFrame();   // superficie adquirida
 *   if (frame == null) return;                    // sin superficie: drop frame
 *   try {
 *       Graphics2D vg = frame.beginVirtual();
 *       try   { scene.draw(vg); }
 *       finally { frame.endVirtual(); }
 *
 *       if (frame.beginPresent()) {
 *           frame.present();
 *           frame.endPresent();
 *       }
 *   } finally {
 *       gateway.releaseFrame(frame);              // superficie liberada
 *   }
 *
 * ──────────────────────────────────────────────────────────────────────────
 * GARANTÍAS
 *
 * - La superficie subyacente no puede ser dispuesta mientras este objeto existe.
 * - La BufferStrategy no cambia entre beginPresent() y endPresent().
 * - endPresent() no lanza excepciones: los errores de bs.show() se absorben
 *   limpiamente y la superficie se marca inválida para el frame siguiente
 *   (sin impacto en el frame actual, que ya finalizó).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 * GameLoop thread únicamente. No compartir entre threads.
 */
public final class RenderFrame {

    private static final Logger LOG = Logger.getLogger(RenderFrame.class.getName());

    private final RenderSurface surface;
    private final ScalingMode   scalingMode;
    private final boolean       useInterpolation;

    /** true si beginPresent() abrió un screenG válido. */
    private boolean presentActive = false;

    /** Contexto de screen abierto entre beginPresent() y endPresent(). */
    private Graphics2D screenG = null;

    /** Solo RenderGateway / SurfacePublisher construye instancias. */
    RenderFrame(RenderSurface surface, ScalingMode scalingMode, boolean useInterpolation) {
        this.surface          = surface;
        this.scalingMode      = scalingMode;
        this.useInterpolation = useInterpolation;
    }

    // ── Fase virtual (render al framebuffer off-screen) ───────────────────────

    /**
     * Abre un contexto de render al framebuffer virtual.
     * El llamador DEBE cerrar el contexto con endVirtual() en un finally.
     */
    public Graphics2D beginVirtual() {
        Graphics2D g = surface.getFramebuffer().createGraphics();
        applyRenderHints(g);
        return g;
    }

    /**
     * Cierra el contexto de render virtual.
     * Seguro llamar con null (no-op).
     */
    public void endVirtual(Graphics2D virtualG) {
        if (virtualG != null) virtualG.dispose();
    }

    // ── Fase de presentación (copia framebuffer → pantalla) ───────────────────

    /**
     * Abre la fase de presentación a pantalla.
     *
     * Retorna false si la BufferStrategy no pudo proporcionar un contexto
     * (estado ilegal transitorio). En ese caso NO llamar endPresent().
     *
     * Retorna true si el contexto está disponible. En ese caso SIEMPRE
     * llamar endPresent() en un finally.
     */
    public boolean beginPresent() {
        BufferStrategy bs = surface.getBufferStrategy();
        try {
            screenG = (Graphics2D) bs.getDrawGraphics();
            presentActive = true;
            return true;
        } catch (IllegalStateException e) {
            LOG.fine("RenderFrame.beginPresent(): BS not ready — " + e.getMessage());
            screenG = null;
            presentActive = false;
            return false;
        }
    }

    /**
     * Escala y copia el framebuffer virtual a la superficie de pantalla.
     * Llamar solo después de beginPresent() exitoso.
     * Las áreas de relleno (letterbox/pillarbox) se pintan aquí también.
     */
    public void present() {
        if (screenG == null) return;

        ViewportInfo vp = surface.getViewport();
        int vw = surface.getVirtualWidth();
        int vh = surface.getVirtualHeight();

        // Áreas de relleno
        for (var area : vp.fillAreas) {
            area.paint(screenG);
        }

        AffineTransform saved = screenG.getTransform();
        applyScalingHints(screenG);

        if (scalingMode == ScalingMode.STRETCH) {
            screenG.scale(
                (double) vp.realWidth  / vw,
                (double) vp.realHeight / vh
            );
        } else {
            screenG.translate(vp.x, vp.y);
            screenG.scale(vp.scale, vp.scale);
        }

        screenG.drawImage(surface.getFramebuffer(), 0, 0, null);
        screenG.setTransform(saved);
    }

    /**
     * Finaliza la presentación, disposa el contexto de pantalla y llama bs.show().
     * Siempre seguro de llamar; absorbe IllegalStateException de show()
     * sin propagar al GameLoop.
     *
     * Llamar en un finally tras beginPresent() exitoso.
     */
    public void endPresent() {
        if (screenG != null) {
            screenG.dispose();
            screenG = null;
        }
        presentActive = false;

        BufferStrategy bs = surface.getBufferStrategy();
        try {
            bs.show();
        } catch (IllegalStateException e) {
            // La superficie fue descartada entre beginPresent() y ahora.
            // No es un error: el siguiente acquireFrame() obtendrá null o
            // la nueva superficie publicada por el EDT.
            LOG.fine("RenderFrame.endPresent(): show() failed (surface replaced) — " + e.getMessage());
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Viewport del frame actual. Inmutable durante toda la vida del frame. */
    public ViewportInfo getViewport() {
        return surface.getViewport();
    }

    // ── API interna (solo SurfacePublisher) ───────────────────────────────────

    /**
     * Libera la referencia a la superficie subyacente.
     * Solo debe llamar SurfacePublisher.releaseFrame().
     */
    void releaseInternal() {
        surface.release();
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void applyRenderHints(Graphics2D g) {
        if (useInterpolation) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }
    }

    private void applyScalingHints(Graphics2D g) {
        if (useInterpolation) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        } else {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_SPEED);
        }
    }
}
