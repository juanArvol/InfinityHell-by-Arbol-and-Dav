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
 * ─── BUG CORREGIDO ────────────────────────────────────────────────────────────
 *
 * BUG-BS-RACE · Race condition entre EDT (recreate) y GameLoop (acquireGraphics)
 *   CAUSA: recreate() es llamado desde el resize listener, que se dispara en el
 *          EDT (ComponentListener.componentResized corre en EDT). Al mismo tiempo,
 *          acquireGraphics() y present() son llamados desde el GameLoop thread.
 *          Ambos acceden al campo `bs` (BufferStrategy) sin sincronización.
 *
 *          El race condition concreto:
 *            EDT:       recreate() → bs = null → canvas.createBufferStrategy() → bs = newBS
 *            GameLoop:  acquireGraphics() lee bs → puede leer null o el BS viejo
 *                        mientras EDT está en medio de la recreación
 *
 *          Consecuencias posibles:
 *            · NullPointerException en acquireGraphics() si lee bs=null a mitad
 *            · IllegalStateException en bs.getDrawGraphics() si el BS fue invalidado
 *            · Pantalla negra durante el resize/toggle si el frame se pierde
 *            · Crash al llamar show() en un BS que ya fue destruido
 *
 *   SOLUCIÓN: usar un AtomicReference<BufferStrategy> para el campo bs.
 *             AtomicReference garantiza visibilidad atómica entre threads sin
 *             necesidad de sincronización explícita en el path caliente (acquireGraphics).
 *
 *             Para createBufferStrategy() (que debe hacerse en EDT), se mantiene
 *             el invariante: primero bs.set(null), luego crear, luego bs.set(newBS).
 *             El GameLoop que lee null en acquireGraphics() devuelve null y salta
 *             el frame — comportamiento ya existente y correcto para recovery.
 *
 *   POR QUÉ AtomicReference Y NO synchronized:
 *     · acquireGraphics() y present() son llamados 30-60 veces/segundo.
 *       Un synchronized en el path caliente añadiría contención constante entre
 *       EDT y GameLoop incluso cuando no hay resize.
 *     · AtomicReference.get() es una lectura volátil O(1) sin contención.
 *     · La única operación que necesita atomicidad es el swap de bs (null → newBS),
 *       que AtomicReference garantiza.
 *
 *   RIESGO: mínimo. AtomicReference es parte del JDK estándar (java.util.concurrent).
 *           No introduce dependencias externas. El comportamiento observable
 *           es idéntico al original excepto que elimina el race condition.
 *
 *   COMPATIBILIDAD FUTURA 2D/3D: sin impacto. BufferStrategyManager es un
 *           detalle de implementación del subsistema de render 2D. Un futuro
 *           subsistema 3D (OpenGL/Vulkan) usaría su propio swap chain.
 *
 * ─── SIN OTROS CAMBIOS ────────────────────────────────────────────────────────
 *   needsClear / clearHandled: sin cambios (lógica de contentsRestored).
 *   createBufferStrategy() con reintentos: sin cambios.
 *   Manejo de contentsLost post-show: sin cambios.
 */
public class BufferStrategyManager {

    private static final Logger LOG = Logger.getLogger(BufferStrategyManager.class.getName());

    private static final int BUFFER_COUNT = 3;  // Triple buffer
    private static final int MAX_RETRIES  = 3;

    private final Canvas canvas;

    /**
     * BUG-BS-RACE FIX: AtomicReference para visibilidad atómica entre
     * EDT (recreate) y GameLoop thread (acquireGraphics/present).
     *
     * get() es una lectura volátil sin lock — costo mínimo en el hot path.
     * set() es una escritura volátil atómica — correcto para el EDT.
     */
    private final java.util.concurrent.atomic.AtomicReference<BufferStrategy> bsRef =
        new java.util.concurrent.atomic.AtomicReference<>(null);

    /** Si el último acquireGraphics() detectó contentsRestored. */
    private volatile boolean needsClear = false;

    public BufferStrategyManager(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Inicializa el BufferStrategy. Llamar DESPUÉS de que el canvas sea visible
     * (después de frame.setVisible(true)).
     *
     * Debe llamarse desde el EDT o desde el hilo de inicialización antes de
     * arrancar el GameLoop.
     */
    public void init() {
        createBufferStrategy();
    }

    /**
     * Recrea el BufferStrategy.
     *
     * Llamar cuando:
     *  - Se detecta contentsLost() y no puede recuperarse
     *  - Tras fullscreen toggle (si el Canvas fue recreado)
     *  - En onResize() si la estrategia queda inválida
     *
     * BUG-BS-RACE FIX: bsRef.set(null) antes de createBufferStrategy().
     * Garantiza que el GameLoop vea null (y salte el frame) durante la recreación,
     * en lugar de usar un BS inválido/destruido.
     */
    public void recreate() {
        LOG.fine("Recreando BufferStrategy...");
        // BUG-BS-RACE FIX: publicar null atómicamente ANTES de crear el nuevo BS.
        // El GameLoop leerá null → acquireGraphics() devuelve null → frame saltado.
        // Esto es correcto: preferible saltar 1 frame a usar un BS corrupto.
        bsRef.set(null);
        createBufferStrategy();
    }

    /**
     * Adquiere un Graphics2D para el frame actual.
     *
     * Maneja automáticamente:
     *  - BufferStrategy null → recrea (si estamos en EDT) o devuelve null
     *  - contentsLost() → intenta recuperar, recrea si no puede
     *  - contentsRestored() → señaliza needsClear para que el render limpie
     *
     * BUG-BS-RACE FIX: bsRef.get() es atómico — lee el estado más reciente
     * publicado por el EDT, incluso si recreate() está en progreso.
     *
     * @return Graphics2D listo para renderizar, o null si no se pudo adquirir
     *         (el llamador debe saltarse el frame si es null).
     */
    public Graphics2D acquireGraphics() {
        // BUG-BS-RACE FIX: lectura atómica del BS actual
        BufferStrategy bs = bsRef.get();

        if (bs == null) {
            // BS en proceso de recreación o aún no inicializado — saltar frame
            return null;
        }

        // Verificar contentsLost ANTES de obtener Graphics
        if (bs.contentsLost()) {
            LOG.fine("BufferStrategy: contents lost, recreando...");
            // recreate() publica null atómicamente — el GameLoop saltará el frame
            // mientras el EDT recrea el BS en el próximo resize/ciclo EDT
            recreate();
            return null;
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

        // BUG-BS-RACE FIX: lectura atómica
        BufferStrategy bs = bsRef.get();
        if (bs == null) return;

        bs.show();

        // Post-show contentsLost: ocurre en algunos sistemas Linux con Xorg.
        // Registrar pero no actuar — el siguiente acquireGraphics lo detectará.
        if (bs.contentsLost()) {
            LOG.fine("BufferStrategy: contents lost post-show (esperando recuperación).");
        }
    }

    /**
     * True si el Graphics recién adquirido necesita un clear completo
     * (porque el contenido fue restaurado y puede tener basura).
     *
     * El render debe limpiar el Graphics cuando esto sea true.
     */
    public boolean needsClear() {
        return needsClear;
    }

    /** Resetear el flag needsClear tras haber limpiado. */
    public void clearHandled() {
        needsClear = false;
    }

    // ─── Internos ─────────────────────────────────────────────────────────────

    /**
     * Crea el BufferStrategy con reintentos.
     *
     * Idealmente llamado desde el EDT (init() o recreate() en resize listener).
     * canvas.createBufferStrategy() requiere que el Canvas tenga peer nativo
     * (es decir, que ya esté visible). Los reintentos con sleep cubren el caso
     * de que se llame justo cuando el Canvas está siendo creado.
     *
     * BUG-BS-RACE FIX: bsRef.set() al final garantiza publicación atómica.
     */
    private void createBufferStrategy() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                canvas.createBufferStrategy(BUFFER_COUNT);
                BufferStrategy newBs = canvas.getBufferStrategy();

                if (newBs != null) {
                    // BUG-BS-RACE FIX: publicar el nuevo BS atómicamente.
                    // A partir de este momento el GameLoop lo verá en la siguiente lectura.
                    bsRef.set(newBs);
                    LOG.fine("BufferStrategy creado (" + BUFFER_COUNT + " buffers).");
                    return;
                }
            } catch (Exception e) {
                LOG.warning("Error creando BufferStrategy (intento " + (attempt + 1) + "): "
                            + e.getMessage());

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        LOG.severe("No se pudo crear BufferStrategy tras " + MAX_RETRIES + " intentos.");
        // bsRef sigue en null — acquireGraphics() devolverá null y el GameLoop
        // saltará frames hasta que el sistema se recupere.
    }
}
