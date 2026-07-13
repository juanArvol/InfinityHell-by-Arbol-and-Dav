package Display.Surface;

import Display.Settings.ScalingMode;
import Display.ViewportInfo;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.logging.Logger;

/**
 * Contrato de acceso a una superficie durante exactamente un frame de render.
 *
 * ── Sistema de capas ─────────────────────────────────────────────────────
 *
 * RenderFrame expone un sistema de capas explícitas ({@link LayerIndex}).
 * Cada capa es un BufferedImage con canal alfa, compuesta sobre el framebuffer
 * en orden ordinal creciente mediante flushLayers().
 *
 * ── Protocolo de presentación correcto (AWT Audit) ───────────────────────
 *
 * El protocolo Oracle para BufferStrategy exige un do-while anidado:
 *
 *   do {                                    // outer: repetir si show() perdió buffer
 *       do {                                // inner: redibujar si buffer fue restaurado
 *           Graphics g = bs.getDrawGraphics();
 *           try { render(g); } finally { g.dispose(); }
 *       } while (bs.contentsRestored());    // buffer restaurado a blanco → redibujar
 *       bs.show();
 *   } while (bs.contentsLost());            // show() invalidó el buffer → repetir todo
 *
 * Por qué importa:
 *   contentsRestored() == true significa que AWT reinicializó el buffer a blanco
 *   (fondo por defecto) porque fue recuperado tras una pérdida. Si no se comprueba,
 *   se presenta el buffer blanco en lugar del frame dibujado → pantalla blanca.
 *
 *   contentsLost() después de show() significa que la presentación invalidó el buffer
 *   (frecuente en page-flip). Si no se repite el ciclo, el próximo frame puede
 *   leer un buffer con contenido incorrecto.
 *
 * ── Integración con el sistema de capas ──────────────────────────────────
 *
 * El loop interno del protocolo Oracle requiere redibujar cuando contentsRestored().
 * El framebuffer virtual (BufferedImage) no es volátil — siempre contiene el
 * último frame dibujado. Por lo tanto, si contentsRestored() es true, el loop
 * interno simplemente repite getDrawGraphics() y copia el framebuffer ya
 * compuesto (no necesita llamar a draw() ni flushLayers() de nuevo).
 *
 * Esto significa que la llamada a draw() y flushLayers() ocurre UNA SOLA VEZ
 * fuera del loop BS, dejando el framebuffer listo. El loop BS solo se ocupa
 * de la copia framebuffer → pantalla, repitiendo si AWT lo requiere.
 *
 * ── Nuevo flujo en GameLoop ───────────────────────────────────────────────
 *
 *   RenderFrame frame = gateway.acquireFrame();
 *   if (frame == null) return;
 *   try {
 *       gameState.draw(frame);          // dibuja en capas
 *       frame.flushLayers();            // compone capas sobre el framebuffer
 *       frame.present();               // loop BS completo → pantalla
 *   } finally {
 *       gateway.releaseFrame(frame);
 *       if (frame.isContentLost()) gateway.notifyContentLost();
 *   }
 *
 * beginPresent() / endPresent() quedan como API de compatibilidad pero
 * el método recomendado para nuevo código es present() sin argumentos.
 *
 * ── Garantías ────────────────────────────────────────────────────────────
 *
 * - La superficie subyacente no puede ser dispuesta mientras este objeto existe.
 * - La BufferStrategy no cambia entre acquireFrame() y releaseFrame().
 * - present() absorbe IllegalStateException de la BS (surface reemplazada).
 * - present() ejecuta el loop completo Oracle incluyendo contentsRestored.
 * - flushLayers() es idempotente.
 * - isContentLost() es válido después de present().
 *
 * ── Threading ────────────────────────────────────────────────────────────
 *
 * GameLoop thread únicamente. No compartir entre threads.
 */
public final class RenderFrame {

    private static final Logger LOG = Logger.getLogger(RenderFrame.class.getName());

    /**
     * Número máximo de iteraciones del loop de presentación antes de abandonar.
     * Protege contra bucles infinitos en caso de plataformas con BS inestable.
     */
    private static final int MAX_PRESENT_ATTEMPTS = 5;

    private final RenderSurface surface;
    private final ScalingMode   scalingMode;
    private final boolean       useInterpolation;

    // ── Sistema de capas ──────────────────────────────────────────────────────

    private final EnumMap<LayerIndex, BufferedImage> layerBuffers =
        new EnumMap<>(LayerIndex.class);
    private final EnumMap<LayerIndex, Graphics2D> layerGraphics =
        new EnumMap<>(LayerIndex.class);

    // ── Estado de presentación ────────────────────────────────────────────────

    private boolean contentsLostDetected = false;

    // ── Estado de presentación legado (beginPresent/endPresent) ──────────────
    private boolean    legacyPresentActive = false;
    private Graphics2D legacyScreenG       = null;

    RenderFrame(RenderSurface surface, ScalingMode scalingMode, boolean useInterpolation) {
        this.surface          = surface;
        this.scalingMode      = scalingMode;
        this.useInterpolation = useInterpolation;
    }

    // ── API de capas ──────────────────────────────────────────────────────────

    /**
     * Devuelve el Graphics2D de la capa indicada, listo para dibujar.
     * La capa se crea la primera vez que se solicita (lazy).
     * El contexto devuelto NO debe ser dispuesto por el caller.
     */
    public Graphics2D getLayerGraphics(LayerIndex layer) {
        if (layer == LayerIndex.WORLD_BACKGROUND) {
            if (!layerGraphics.containsKey(LayerIndex.WORLD_BACKGROUND)) {
                Graphics2D g = surface.getFramebuffer().createGraphics();
                applyRenderHints(g);
                surface.getBackground().apply(g,
                    surface.getVirtualWidth(), surface.getVirtualHeight());
                layerGraphics.put(LayerIndex.WORLD_BACKGROUND, g);
            }
            return layerGraphics.get(LayerIndex.WORLD_BACKGROUND);
        }

        if (!layerBuffers.containsKey(layer)) {
            BufferedImage buf = new BufferedImage(
                surface.getVirtualWidth(), surface.getVirtualHeight(),
                BufferedImage.TYPE_INT_ARGB);
            layerBuffers.put(layer, buf);

            Graphics2D g = buf.createGraphics();
            applyRenderHints(g);
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, surface.getVirtualWidth(), surface.getVirtualHeight());
            g.setComposite(AlphaComposite.SrcOver);
            layerGraphics.put(layer, g);
        }

        return layerGraphics.get(layer);
    }

    /**
     * Compone todas las capas activas sobre el framebuffer en orden ordinal.
     * Debe llamarse ANTES de present(). Idempotente.
     */
    public void flushLayers() {
        Graphics2D base;
        if (layerGraphics.containsKey(LayerIndex.WORLD_BACKGROUND)) {
            base = layerGraphics.get(LayerIndex.WORLD_BACKGROUND);
        } else {
            base = surface.getFramebuffer().createGraphics();
            applyRenderHints(base);
            layerGraphics.put(LayerIndex.WORLD_BACKGROUND, base);
        }

        for (LayerIndex layer : LayerIndex.values()) {
            if (layer == LayerIndex.WORLD_BACKGROUND) continue;
            BufferedImage buf = layerBuffers.get(layer);
            if (buf == null) continue;

            Graphics2D layerG = layerGraphics.remove(layer);
            if (layerG != null) layerG.dispose();

            base.setComposite(AlphaComposite.SrcOver);
            base.drawImage(buf, 0, 0, null);

            Graphics2D clearG = buf.createGraphics();
            clearG.setComposite(AlphaComposite.Clear);
            clearG.fillRect(0, 0, surface.getVirtualWidth(), surface.getVirtualHeight());
            clearG.dispose();
        }

        layerGraphics.remove(LayerIndex.WORLD_BACKGROUND);
        if (base != null) base.dispose();
    }

    // ── Presentación: API principal ───────────────────────────────────────────

    /**
     * Presenta el framebuffer a pantalla siguiendo el protocolo Oracle completo.
     *
     * Implementa el do-while anidado recomendado por Oracle:
     *
     *   do {
     *       do {
     *           g = bs.getDrawGraphics();
     *           blit framebuffer → g;
     *           g.dispose();
     *       } while (bs.contentsRestored());
     *       bs.show();
     *   } while (bs.contentsLost());
     *
     * El framebuffer virtual (BufferedImage) ya contiene el frame compuesto
     * por flushLayers(). El loop interno simplemente copia el framebuffer al
     * buffer de la BS en cada iteración que lo requiera.
     *
     * Absorbe IllegalStateException si la BS fue reemplazada durante la
     * presentación (transición concurrente). En ese caso marca contentsLost.
     *
     * GameLoop thread únicamente.
     */
    public void present() {
        BufferStrategy bs = surface.getBufferStrategy();
        int attempts = 0;

        try {
            do {
                // Loop interno: redibujar si AWT restauró el buffer a blanco.
                do {
                    Graphics2D screenG = null;
                    try {
                        screenG = (Graphics2D) bs.getDrawGraphics();
                        blitFramebuffer(screenG);
                    } catch (IllegalStateException e) {
                        // BS reemplazada o inválida durante la transición.
                        contentsLostDetected = true;
                        LOG.fine("RenderFrame.present(): getDrawGraphics() failed — " + e.getMessage());
                        return;
                    } finally {
                        if (screenG != null) screenG.dispose();
                    }
                } while (bs.contentsRestored());  // buffer restaurado → redibujar

                try {
                    bs.show();
                } catch (IllegalStateException e) {
                    contentsLostDetected = true;
                    LOG.fine("RenderFrame.present(): show() failed — " + e.getMessage());
                    return;
                }

                attempts++;
            } while (bs.contentsLost() && attempts < MAX_PRESENT_ATTEMPTS);  // show() perdió buffer → repetir

            // Si contentsLost sigue true al salir del loop (se alcanzó MAX_PRESENT_ATTEMPTS),
            // señalizar para que el pipeline reconstruya la surface en el EDT.
            if (bs.contentsLost()) {
                contentsLostDetected = true;
                LOG.fine("RenderFrame.present(): contentsLost after " + attempts + " attempts — signaling rebuild");
            }

        } catch (Exception e) {
            // Cualquier otro error inesperado de la BS.
            contentsLostDetected = true;
            LOG.warning("RenderFrame.present(): unexpected exception — " + e.getMessage());
        }
    }

    /**
     * True si la BufferStrategy perdió su contenido durante este frame.
     * Consultar después de present() o endPresent().
     * Cuando es true, el GameLoop debe llamar gateway.notifyContentLost().
     */
    public boolean isContentLost() {
        return contentsLostDetected;
    }

    // ── API heredada de compatibilidad ────────────────────────────────────────
    //
    // beginPresent() / present(void) / endPresent() se mantienen para
    // compatibilidad con código existente durante la transición.
    // El GameLoop ya ha sido migrado al nuevo API present() sin argumentos.
    // Estos métodos NO implementan el loop Oracle correcto — solo la primera
    // iteración. Preferir present() para todo código nuevo.

    /**
     * Abre un contexto de render al framebuffer virtual.
     * @deprecated Usar el sistema de capas (getLayerGraphics + flushLayers).
     */
    @Deprecated(since = "audit-awt", forRemoval = false)
    public Graphics2D beginVirtual() {
        Graphics2D g = surface.getFramebuffer().createGraphics();
        applyRenderHints(g);
        surface.getBackground().apply(g, surface.getVirtualWidth(), surface.getVirtualHeight());
        return g;
    }

    /**
     * @deprecated Usar endVirtual(g).
     */
    @Deprecated(since = "audit-awt", forRemoval = false)
    public void endVirtual(Graphics2D virtualG) {
        if (virtualG != null) virtualG.dispose();
    }

    /**
     * Abre la fase de presentación (API legado — una sola iteración, sin loop).
     * Retorna false si la BS no pudo proporcionar un contexto.
     *
     * @deprecated Usar {@link #present()} que implementa el protocolo Oracle completo.
     */
    @Deprecated(since = "audit-awt", forRemoval = false)
    public boolean beginPresent() {
        BufferStrategy bs = surface.getBufferStrategy();
        try {
            legacyScreenG = (Graphics2D) bs.getDrawGraphics();
            legacyPresentActive = true;
            return true;
        } catch (IllegalStateException e) {
            LOG.fine("RenderFrame.beginPresent(): BS not ready — " + e.getMessage());
            legacyScreenG = null;
            legacyPresentActive = false;
            return false;
        }
    }

    /**
     * Copia el framebuffer a pantalla (API legado).
     *
     * @deprecated Usar {@link #present()}.
     */
    @Deprecated(since = "audit-awt", forRemoval = false)
    public void present(Graphics2D ignored) {
        if (legacyScreenG != null) blitFramebuffer(legacyScreenG);
    }

    /**
     * Finaliza la presentación legado.
     *
     * @deprecated Usar {@link #present()}.
     */
    @Deprecated(since = "audit-awt", forRemoval = false)
    public void endPresent() {
        if (legacyScreenG != null) {
            legacyScreenG.dispose();
            legacyScreenG = null;
        }
        legacyPresentActive = false;

        BufferStrategy bs = surface.getBufferStrategy();
        try {
            bs.show();
        } catch (IllegalStateException e) {
            LOG.fine("RenderFrame.endPresent(): show() failed — " + e.getMessage());
        }

        try {
            if (bs.contentsLost()) {
                contentsLostDetected = true;
                LOG.fine("RenderFrame.endPresent(): contentsLost() detected");
            }
        } catch (Exception e) {
            contentsLostDetected = true;
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Viewport del frame actual. */
    public ViewportInfo getViewport()  { return surface.getViewport();      }
    /** Ancho virtual del framebuffer. */
    public int getVirtualWidth()       { return surface.getVirtualWidth();  }
    /** Alto virtual del framebuffer. */
    public int getVirtualHeight()      { return surface.getVirtualHeight(); }

    // ── API interna ───────────────────────────────────────────────────────────

    void releaseInternal() { surface.release(); }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Copia el framebuffer virtual al contexto de pantalla aplicando la
     * transformación de escala y las áreas de relleno.
     */
    private void blitFramebuffer(Graphics2D screenG) {
        ViewportInfo vp = surface.getViewport();
        int vw = surface.getVirtualWidth();
        int vh = surface.getVirtualHeight();

        for (var area : vp.fillAreas) {
            area.paint(screenG);
        }

        AffineTransform saved = screenG.getTransform();
        applyScalingHints(screenG);

        if (scalingMode == ScalingMode.STRETCH) {
            screenG.scale((double) vp.realWidth / vw, (double) vp.realHeight / vh);
        } else {
            screenG.translate(vp.x, vp.y);
            screenG.scale(vp.scale, vp.scale);
        }

        screenG.drawImage(surface.getFramebuffer(), 0, 0, null);
        screenG.setTransform(saved);
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
