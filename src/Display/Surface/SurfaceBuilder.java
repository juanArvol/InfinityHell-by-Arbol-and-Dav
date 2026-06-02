package Display.Surface;

import Display.Background.DisplayBackground;
import Display.Background.SolidColorBackground;
import Display.Settings.ScalingMode;
import Display.ViewportInfo;

import java.awt.Canvas;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Construye {@link RenderSurface} completamente inicializadas.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * RESPONSABILIDAD
 *
 * SurfaceBuilder es el único lugar donde se llama canvas.createBufferStrategy().
 * Esto centraliza toda la lógica de construcción de superficies y garantiza
 * que ninguna superficie llega a SurfacePublisher en estado parcial.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * SECUENCIA DE BUILD
 *
 *   1. canvas.createBufferStrategy(bufferCount)   → inicializa la BS en AWT
 *   2. canvas.getBufferStrategy()                 → obtiene la referencia
 *   3. new BufferedImage(vw, vh, ...)             → framebuffer virtual
 *   4. new RenderSurface(bs, fb, viewport, ...)   → empaqueta todo
 *
 * La RenderSurface retornada tiene refCount=0 y disposed=false.
 * La publica SurfacePublisher (nunca SurfaceBuilder directamente).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 * build() → EDT únicamente.
 * Los campos son inmutables tras construcción; onVirtualResolutionChanged()
 * crea un nuevo SurfaceBuilder (o actualiza virtualWidth/Height) antes del
 * próximo build().
 */
public final class SurfaceBuilder {

    private static final Logger LOG = Logger.getLogger(SurfaceBuilder.class.getName());

    private final Canvas  canvas;
    private final int     bufferCount;
    private       int     virtualWidth;
    private       int     virtualHeight;

    public SurfaceBuilder(Canvas canvas, int bufferCount,
                          int virtualWidth, int virtualHeight) {
        this.canvas        = canvas;
        this.bufferCount   = bufferCount;
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    // ── Mutación controlada de resolución virtual (EDT) ───────────────────────

    /**
     * Actualiza la resolución virtual. La próxima llamada a build()
     * producirá un framebuffer con las nuevas dimensiones.
     *
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
     * Pre-condición: canvas.isDisplayable() == true (verificar antes de llamar).
     * Pre-condición: llamar únicamente desde el EDT.
     *
     * @param viewport  viewport calculado para las dimensiones actuales del canvas.
     * @param background fondo que se aplica al framebuffer al inicio de cada frame.
     * @return superficie lista para publicar, o null si la BS no pudo crearse.
     */
    public RenderSurface build(ViewportInfo viewport, DisplayBackground background) {
        assertEDT("build");

        if (!canvas.isDisplayable()) {
            LOG.warning("SurfaceBuilder.build(): canvas not displayable — returning null");
            return null;
        }

        try {
            canvas.createBufferStrategy(bufferCount);
            BufferStrategy bs = canvas.getBufferStrategy();

            BufferedImage fb = createFramebuffer(virtualWidth, virtualHeight);

            // Aplicar el fondo al framebuffer inicial para que el primer frame
            // no muestre basura de memoria.
            if (background != null) {
                var g = fb.createGraphics();
                try { background.apply(g, virtualWidth, virtualHeight); }
                finally { g.dispose(); }
            }

            LOG.fine("SurfaceBuilder: RenderSurface built ("
                + virtualWidth + "x" + virtualHeight
                + ", bufferCount=" + bufferCount + ")");

            return new RenderSurface(bs, fb, viewport, virtualWidth, virtualHeight);

        } catch (Exception e) {
            LOG.warning("SurfaceBuilder.build() failed: " + e);
            return null;
        }
    }

    /**
     * Construye sin aplicar fondo (framebuffer limpio, fondo negro por defecto).
     */
    public RenderSurface build(ViewportInfo viewport) {
        return build(viewport, SolidColorBackground.BLACK);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public int getVirtualWidth()  { return virtualWidth;  }
    public int getVirtualHeight() { return virtualHeight; }

    // ── Privados ──────────────────────────────────────────────────────────────

    private static BufferedImage createFramebuffer(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private static void assertEDT(String methodName) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "SurfaceBuilder." + methodName + "() must be called from the EDT, "
                + "but was called from: " + Thread.currentThread().getName());
        }
    }
}
