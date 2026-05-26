package Display.Managers;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;

/**
 * Gestiona el BufferStrategy del Canvas con manejo robusto de
 * contentsLost() y contentsRestored().
 *
 * PROBLEMAS que resuelve:
 *  1. contentsLost() — el buffer se invalida tras alt-tab, resize,
 *     cambio de monitor, bloqueo de pantalla. Si no se detecta,
 *     el juego renderiza en un buffer muerto → pantalla negra.
 *
 *  2. contentsRestored() — el buffer fue restaurado pero puede
 *     tener contenido de frame anterior corrompido. Hay que
 *     rellenar (clear) antes de presentar.
 *
 *  3. BufferStrategy null / no inicializado — puede ocurrir
 *     tras fullscreen toggle si el Canvas se re-añade al frame.
 *
 * Uso en el game loop:
 *
 *   Graphics2D g = bsManager.acquireGraphics();
 *   if (g != null) {
 *       // ... render a g ...
 *       bsManager.present();
 *   }
 */
public class BufferStrategyManager {

    private static final Logger LOG = Logger.getLogger(BufferStrategyManager.class.getName());

    private static final int BUFFER_COUNT   = 3;  // Triple buffer
    private static final int MAX_RETRIES    = 3;

    private final Canvas canvas;
    private BufferStrategy bs;

    /** Si el último acquireGraphics() detectó contentsRestored. */
    private boolean needsClear = false;

    public BufferStrategyManager(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Inicializa el BufferStrategy. Llamar DESPUÉS de que el canvas sea visible
     * (después de frame.setVisible(true)).
     */
    public void init() {
        createBufferStrategy();
    }

    /**
     * Recrea el BufferStrategy.
     * Llamar cuando:
     *  - Se detecta contentsLost() y no puede recuperarse
     *  - Tras fullscreen toggle (si el Canvas fue recreado)
     *  - En onResize() si la estrategia queda inválida
     */
    public void recreate() {
        LOG.fine("Recreando BufferStrategy...");
        bs = null;
        createBufferStrategy();
    }

    /**
     * Adquiere un Graphics2D para el frame actual.
     *
     * Maneja automáticamente:
     *  - BufferStrategy null → recrea
     *  - contentsLost() → intenta recuperar, recrea si no puede
     *  - contentsRestored() → señaliza needsClear para que el render limpie
     *
     * @return Graphics2D listo para renderizar, o null si no se pudo adquirir
     *         (el llamador debe saltarse el frame si es null).
     */
    public Graphics2D acquireGraphics() {
        ensureBufferStrategy();
        if (bs == null) return null;

        // Verificar contentsLost ANTES de obtener Graphics
        if (bs.contentsLost()) {
            LOG.fine("BufferStrategy: contents lost, recreando...");
            recreate();
            if (bs == null) return null;
        }

        // Verificar contentsRestored — el contenido puede ser basura
        if (bs.contentsRestored()) {
            LOG.fine("BufferStrategy: contents restored, se hará clear.");
            needsClear = true;
        }

        try {
            return (Graphics2D) bs.getDrawGraphics();
        } catch (IllegalStateException e) {
            LOG.warning("No se pudo obtener DrawGraphics: " + e.getMessage());
            recreate();
            return null;
        }
    }

    /**
     * Presenta el frame al display.
     *
     * Llama a show() y verifica contentsLost() post-show.
     * Si hay contentsLost post-show, el frame fue perdido — normal en algunos
     * sistemas; el siguiente frame lo recuperará.
     *
     * @param g el Graphics2D obtenido de acquireGraphics() — será disposed aquí.
     */
    public void present(Graphics2D g) {
        if (g != null) g.dispose();
        if (bs == null) return;

        bs.show();

        // Post-show contentsLost: ocurre en algunos sistemas Linux con Xorg
        // Registrar pero no actuar — el siguiente acquireGraphics lo detectará
        if (bs.contentsLost()) {
            LOG.fine("BufferStrategy: contents lost post-show (esperando recuperación).");
        }
    }

    /**
     * True si el Graphics recién adquirido necesita un clear completo
     * (porque el contenido fue restaurado y puede tener basura).
     *
     * El GameLoop debe limpiar el Graphics cuando esto sea true.
     */
    public boolean needsClear() {
        return needsClear;
    }

    /** Resetear el flag needsClear tras haber limpiado. */
    public void clearHandled() {
        needsClear = false;
    }

    // ─── Internos ─────────────────────────────────────────────────────────────

    private void ensureBufferStrategy() {
        if (bs == null) {
            createBufferStrategy();
        }
    }

    private void createBufferStrategy() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                canvas.createBufferStrategy(BUFFER_COUNT);
                bs = canvas.getBufferStrategy();

                if (bs != null) {
                    LOG.fine("BufferStrategy creado (" + BUFFER_COUNT + " buffers).");
                    return;
                }
            } catch (Exception e) {
                LOG.warning("Error creando BufferStrategy (intento " + (attempt+1) + "): " + e.getMessage());

                // Pequeña espera antes de reintentar (el Canvas puede no estar
                // completamente inicializado aún)
                try { Thread.sleep(10); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        LOG.severe("No se pudo crear BufferStrategy tras " + MAX_RETRIES + " intentos.");
    }
}
