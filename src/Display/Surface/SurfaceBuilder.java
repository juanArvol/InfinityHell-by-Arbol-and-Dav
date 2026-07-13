package Display.Surface;

import Display.Backend.AwtWindowBackend;
import Display.Background.DisplayBackground;
import Display.Background.SolidColorBackground;
import Display.ViewportInfo;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Construye {@link RenderSurface} completamente inicializadas.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-003: createBufferStrategy() MOVIDO AL BACKEND
 *
 * Antes del HRFC-003, SurfaceBuilder llamaba canvas.createBufferStrategy()
 * directamente. Esa llamada ha sido movida a AwtWindowBackend, que es el
 * único punto autorizado de contacto con AWT.
 *
 * SurfaceBuilder ahora recibe la BufferStrategy ya creada mediante
 * backend.createBufferStrategy(bufferCount), la combina con el framebuffer
 * virtual y el viewport para construir la RenderSurface.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * SECUENCIA DE BUILD
 *
 *   1. backend.createBufferStrategy(bufferCount)   → crea BS en AWT
 *   2. Si null (canvas no displayable): retorna null sin continuar.
 *   3. new BufferedImage(vw, vh, ...)              → framebuffer virtual
 *   4. bg.apply(g, vw, vh)                         → limpiar framebuffer
 *   5. new RenderSurface(bs, fb, viewport, ...)    → ensamblar surface
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   build() → EDT únicamente (backend.createBufferStrategy() es EDT-only).
 *   onVirtualResolutionChanged() → EDT únicamente.
 */
public final class SurfaceBuilder {

    private static final Logger LOG =
        Logger.getLogger(SurfaceBuilder.class.getName());

    private final AwtWindowBackend backend;
    private final int              bufferCount;
    private       int              virtualWidth;
    private       int              virtualHeight;

    public SurfaceBuilder(AwtWindowBackend backend, int bufferCount,
                          int virtualWidth, int virtualHeight) {
        this.backend       = backend;
        this.bufferCount   = bufferCount;
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    // ── Mutación controlada de resolución virtual ─────────────────────────────

    /**
     * Actualiza la resolución virtual. La próxima llamada a build()
     * producirá un framebuffer con las nuevas dimensiones.
     * EDT únicamente.
     */
    public void onVirtualResolutionChanged(int newWidth, int newHeight) {
        if (newWidth > 0 && newHeight > 0) {
            this.virtualWidth  = newWidth;
            this.virtualHeight = newHeight;
        }
    }

    // ── Construcción ──────────────────────────────────────────────────────────

    /**
     * Construye una {@link RenderSurface} completamente inicializada.
     *
     * Delega la creación de BufferStrategy al Backend, que es el único
     * punto autorizado para llamar canvas.createBufferStrategy().
     *
     * @param viewport   viewport calculado para las dimensiones actuales.
     * @param background fondo que se aplica al framebuffer al inicio de cada frame.
     * @return superficie lista para publicar, o null si el canvas no es
     *         displayable o si la creación de BS falló.
     *
     * EDT únicamente.
     */
    public RenderSurface build(ViewportInfo viewport, DisplayBackground background) {
        assertEDT("build");

        // El Backend verifica isDisplayable() internamente y retorna null si falla.
        BufferStrategy bs = backend.createBufferStrategy(bufferCount);
        if (bs == null) {
            LOG.warning("SurfaceBuilder.build(): backend.createBufferStrategy() returned null — canvas not ready");
            return null;
        }

        try {
            BufferedImage fb = new BufferedImage(
                virtualWidth, virtualHeight, BufferedImage.TYPE_INT_ARGB);

            DisplayBackground bg = background != null ? background : SolidColorBackground.BLACK;
            var g = fb.createGraphics();
            try { bg.apply(g, virtualWidth, virtualHeight); }
            finally { g.dispose(); }

            LOG.fine("SurfaceBuilder: RenderSurface built ("
                + virtualWidth + "x" + virtualHeight
                + ", buffers=" + bufferCount + ")");

            return new RenderSurface(bs, fb, viewport, virtualWidth, virtualHeight, bg);

        } catch (Exception e) {
            LOG.warning("SurfaceBuilder.build() failed after BS creation: " + e);
            // BS was created but framebuffer construction failed: dispose it cleanly.
            backend.disposeBufferStrategy();
            return null;
        }
    }

    /**
     * @deprecated Preferir {@link #build(ViewportInfo, DisplayBackground)}.
     */
    @Deprecated(since = "hrfc-003", forRemoval = false)
    public RenderSurface build(ViewportInfo viewport) {
        return build(viewport, SolidColorBackground.BLACK);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public int getVirtualWidth()  { return virtualWidth;  }
    public int getVirtualHeight() { return virtualHeight; }

    // ── Privados ──────────────────────────────────────────────────────────────

    private static void assertEDT(String methodName) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "SurfaceBuilder." + methodName + "() must be called from the EDT, "
                + "but was called from: " + Thread.currentThread().getName());
        }
    }
}
