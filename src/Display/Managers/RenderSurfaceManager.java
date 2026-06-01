package Display.Managers;

import Display.Background.DisplayBackground;
import Display.Background.FillArea;
import Display.Background.SolidColorBackground;
import Display.Settings.ScalingMode;
import Display.State.SurfaceState;
import Display.ViewportInfo;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Gestiona el framebuffer virtual y el ciclo de vida del BufferStrategy.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: ELIMINACIÓN DEL POLLING EN beginFrame()
 *
 * Problema anterior:
 *   beginFrame() comprobaba surfaceState == LOST en cada frame y si era
 *   cierto encolaba invokeLater(recreateBufferStrategy). Esto ejecutaba
 *   en cada tick del GameLoop (60fps), lo que significaba que 60 invocaciones
 *   al EDT por segundo en caso de superficie perdida. Si la recreación
 *   tardaba más de 1 frame, se apilaban múltiples recreaciones simultáneas.
 *
 * Causa raíz:
 *   Usar "preguntar si hay algo que hacer" (polling) en lugar de reaccionar
 *   a una transición de estado (event-driven).
 *
 * Solución:
 *   Introducir un flag atómico recreationScheduled que se activa SOLO
 *   cuando se produce la transición a LOST. beginFrame() ya NO comprueba
 *   el estado ni encola nada. La re-planificación ocurre UNA SOLA VEZ
 *   por transición a LOST, no por frame.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: SUPRESIÓN DE CICLO BS → COMPONENTRESIZED → BS
 *
 * Problema anterior:
 *   destroyBS() y createBS() llamados durante onCanvasResized() podían
 *   provocar eventos AWT adicionales del peer nativo que disparaban otro
 *   componentResized, creando un bucle.
 *
 * Solución:
 *   El resize pasa ahora por la cola de comandos como ResizeCanvas.
 *   El pipeline suprime resize durante la ejecución (suppressResize=true).
 *   Esto rompe el bucle en su origen.
 *   RenderSurfaceManager no necesita cambios para esto, pero la arquitectura
 *   que lo rodea ya lo garantiza.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   createBufferStrategy() / destroyBufferStrategy() → EDT únicamente.
 *   beginFrame() / endFrame() / beginPresent() / present() / endPresent()
 *       → GameLoop thread únicamente.
 *   surfaceState → AtomicReference: escrito en EDT y GameLoop.
 *   recreationScheduled → AtomicBoolean: garantiza exactamente un schedule.
 *   background → volatile: thread-safe.
 */
public final class RenderSurfaceManager {

    private static final Logger LOG = Logger.getLogger(RenderSurfaceManager.class.getName());

    private static final int MAX_RECREATE_ATTEMPTS = 3;

    private final Canvas      canvas;
    private final int         bufferCount;
    private final boolean     useInterpolation;
    private final ScalingMode scalingMode;

    private volatile int            virtualWidth;
    private volatile int            virtualHeight;
    private volatile BufferedImage  framebuffer;
    private volatile BufferStrategy bsRef = null;

    private final AtomicReference<SurfaceState> surfaceState =
        new AtomicReference<>(SurfaceState.LOST);

    /**
     * Flag atómico: true si ya hay una recreación programada en el EDT.
     * Garantiza que solo se programa UN invokeLater por transición a LOST.
     * Elimina el comportamiento de polling del beginFrame() anterior.
     */
    private final AtomicBoolean recreationScheduled = new AtomicBoolean(false);

    private volatile int recreateAttempts = 0;

    private volatile DisplayBackground background;

    public RenderSurfaceManager(Canvas canvas,
                                int virtualWidth,
                                int virtualHeight,
                                int bufferCount,
                                boolean useInterpolation,
                                ScalingMode scalingMode,
                                DisplayBackground background) {
        this.canvas           = canvas;
        this.virtualWidth     = virtualWidth;
        this.virtualHeight    = virtualHeight;
        this.bufferCount      = bufferCount;
        this.useInterpolation = useInterpolation;
        this.scalingMode      = scalingMode;
        this.background       = background != null ? background : SolidColorBackground.BLACK;
        this.framebuffer      = createFramebuffer(virtualWidth, virtualHeight);
    }

    // ── Lifecycle del BufferStrategy ──────────────────────────────────────────

    public void createBufferStrategy() {
        assertEDT("createBufferStrategy");
        recreationScheduled.set(false); // la recreación ya está en curso en EDT

        if (!canvas.isDisplayable()) {
            LOG.warning("createBufferStrategy(): canvas not displayable — surface FAILED");
            transitionState(SurfaceState.FAILED);
            bsRef = null;
            return;
        }

        transitionState(SurfaceState.RECREATING);
        try {
            canvas.createBufferStrategy(bufferCount);
            bsRef = canvas.getBufferStrategy();
            recreateAttempts = 0;
            transitionState(SurfaceState.READY);
            LOG.fine("BufferStrategy created (bufferCount=" + bufferCount + ") → READY");
        } catch (Exception e) {
            bsRef = null;
            recreateAttempts++;
            if (recreateAttempts >= MAX_RECREATE_ATTEMPTS) {
                LOG.warning("createBufferStrategy() failed " + recreateAttempts
                            + " times → FAILED: " + e);
                transitionState(SurfaceState.FAILED);
            } else {
                LOG.warning("createBufferStrategy() attempt " + recreateAttempts
                            + "/" + MAX_RECREATE_ATTEMPTS + ": " + e);
                transitionState(SurfaceState.LOST);
            }
        }
    }

    public void destroyBufferStrategy() {
        assertEDT("destroyBufferStrategy");

        BufferStrategy bs = bsRef;
        bsRef = null;
        recreateAttempts = 0;
        recreationScheduled.set(false);
        transitionState(SurfaceState.LOST);

        if (bs != null) {
            bs.dispose();
            LOG.fine("BufferStrategy destroyed → LOST");
        }
    }

    public void recreateBufferStrategy() {
        assertEDT("recreateBufferStrategy");
        destroyBufferStrategy();
        createBufferStrategy();
    }

    // ── Cambio de resolución virtual ──────────────────────────────────────────

    public void onVirtualResolutionChanged(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        if (newWidth == this.virtualWidth && newHeight == this.virtualHeight) return;
        this.virtualWidth  = newWidth;
        this.virtualHeight = newHeight;
        this.framebuffer   = createFramebuffer(newWidth, newHeight);
        LOG.info("Virtual framebuffer resized to " + newWidth + "x" + newHeight);
    }

    // ── Framebuffer virtual ───────────────────────────────────────────────────

    /**
     * Abre un frame de render en el framebuffer virtual.
     *
     * CORRECCIÓN: ya NO comprueba el estado ni encola recreaciones.
     * La recreación se activa únicamente en la transición a LOST
     * (ver scheduleRecreationIfNeeded). beginFrame() es ahora puro.
     *
     * Llamar solo desde el GameLoop thread.
     */
    public Graphics2D beginFrame() {
        Graphics2D g = framebuffer.createGraphics();
        applyRenderHints(g);
        background.apply(g, virtualWidth, virtualHeight);
        return g;
    }

    public void endFrame(Graphics2D g) {
        if (g != null) g.dispose();
    }

    // ── Presentación a pantalla ───────────────────────────────────────────────

    public Graphics2D beginPresent() {
        if (surfaceState.get() != SurfaceState.READY) return null;

        BufferStrategy bs = bsRef;
        if (bs == null || !canvas.isDisplayable()) {
            markLost();
            return null;
        }

        if (bs.contentsRestored()) {
            LOG.fine("BufferStrategy contentsRestored");
        }

        try {
            return (Graphics2D) bs.getDrawGraphics();
        } catch (IllegalStateException e) {
            LOG.fine("beginPresent(): BS invalid → LOST: " + e.getMessage());
            markLost();
            return null;
        }
    }

    public void present(Graphics2D screenG, ViewportInfo viewport) {
        if (screenG == null || viewport == null) return;

        for (FillArea area : viewport.fillAreas) {
            area.paint(screenG);
        }

        AffineTransform saved = screenG.getTransform();
        applyScalingHints(screenG);

        if (scalingMode == ScalingMode.STRETCH) {
            screenG.scale(
                (double) viewport.realWidth  / virtualWidth,
                (double) viewport.realHeight / virtualHeight
            );
        } else {
            screenG.translate(viewport.x, viewport.y);
            screenG.scale(viewport.scale, viewport.scale);
        }

        screenG.drawImage(framebuffer, 0, 0, null);
        screenG.setTransform(saved);
    }

    public void endPresent(Graphics2D screenG) {
        if (screenG != null) screenG.dispose();

        BufferStrategy bs = bsRef;
        if (bs == null) return;

        try {
            bs.show();
        } catch (IllegalStateException e) {
            LOG.fine("endPresent(): show() failed → LOST");
            markLost();
            return;
        }

        if (bs.contentsLost()) {
            LOG.fine("endPresent(): contentsLost after show() → LOST");
            markLost();
        }
    }

    // ── Configuración en runtime ──────────────────────────────────────────────

    public void setBackground(DisplayBackground bg) {
        this.background = bg != null ? bg : SolidColorBackground.BLACK;
    }

    public DisplayBackground getBackground()  { return background; }
    public SurfaceState getSurfaceState()     { return surfaceState.get(); }
    public boolean hasSurface()              { return surfaceState.get() == SurfaceState.READY; }
    public int getVirtualWidth()             { return virtualWidth;  }
    public int getVirtualHeight()            { return virtualHeight; }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Marca la superficie como LOST y programa UNA recreación en el EDT.
     *
     * El flag atómico recreationScheduled garantiza que solo se programa
     * un invokeLater por transición, no uno por frame.
     * Esto elimina el polling implícito del beginFrame() anterior.
     */
    private void markLost() {
        transitionState(SurfaceState.LOST);
        if (recreateAttempts < MAX_RECREATE_ATTEMPTS) {
            if (recreationScheduled.compareAndSet(false, true)) {
                javax.swing.SwingUtilities.invokeLater(this::recreateBufferStrategy);
            }
        }
    }

    private void transitionState(SurfaceState next) {
        SurfaceState prev = surfaceState.getAndSet(next);
        if (prev != next) {
            LOG.fine("SurfaceState: " + prev + " → " + next);
        }
    }

    private static BufferedImage createFramebuffer(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private static void assertEDT(String methodName) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "RenderSurfaceManager." + methodName + "() must be called from the EDT, "
                + "but was called from: " + Thread.currentThread().getName());
        }
    }

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
