package Display.Surface;

import Display.Settings.ScalingMode;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Implementación de {@link RenderGateway} basada en swap atómico de superficies.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MODELO DE OWNERSHIP
 *
 *   - El EDT es el único productor: construye RenderSurface y llama publish().
 *   - El GameLoop es el único consumidor: llama acquireFrame() y releaseFrame().
 *   - publishedRef es el punto de transferencia; su escritura es atómica (CAS).
 *   - gate es la barrera explícita de readiness; controla si el GameLoop
 *     puede adquirir frames en cada ciclo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-002: WINDOWREADINESSGATE
 *
 * Problema anterior (RC-A / RC-E):
 *   La única barrera era publishedRef != null. Si buildAndPublish() fallaba,
 *   publishedRef quedaba null para siempre. El mecanismo de recuperación
 *   (notifyContentLost) nunca disparaba porque endPresent() nunca se llamaba.
 *   El GameLoop descartaba frames permanentemente → pantalla negra permanente.
 *
 *   Para el caso SUSPENDED (Alt+Tab) tampoco había solución: no se podía
 *   pausar el GameLoop sin destruir la surface.
 *
 * Solución: WindowReadinessGate + contrato explícito de publish/unpublish.
 *
 *   acquireFrame() comprueba gate.isOpen() ANTES de leer publishedRef.
 *   Si la gate está cerrada → drop silencioso inmediato, sin tocar publishedRef.
 *
 *   El pipeline:
 *     - Cierra la gate en Phase 2 (antes de unpublish en Phase 3).
 *     - Abre la gate en Phase 8 (después de publicar la surface READY).
 *     - Para SUSPENDED: cierra la gate sin vaciar publishedRef.
 *       La surface sigue publicada. Al reanudar (ResumeRendering), si la BS
 *       sigue válida, basta con abrir la gate sin reconstruir nada.
 *     - Para FAILED: cierra la gate, vacía publishedRef, y llama onRecoveryNeeded.
 *       onRecoveryNeeded encola RecreateBufferStrategy con backoff para salir
 *       del deadlock que RC-E producía.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * SWAP ATÓMICO (publish)
 *
 *   EDT:
 *     newSurface = surfaceBuilder.build(viewport)    // completamente construida
 *     publish(newSurface)                             // swap atómico
 *     gate.open()                                     // ahora el GameLoop puede adquirir
 *
 *   GameLoop (puede correr en paralelo):
 *     if (!gate.isOpen()) return null                // barrera explícita
 *     RenderSurface s = publishedRef.get()           // lectura atómica
 *     if (s == null || !s.acquire()) return null     // s fue marcada disposed
 *     return new RenderFrame(s, ...)
 *
 * ──────────────────────────────────────────────────────────────────────────
 * ¿POR QUÉ EL CRASH ES IMPOSIBLE?
 *
 * - El GameLoop nunca tiene acceso directo a BufferStrategy.
 * - Solo tiene RenderFrame, que referencia RenderSurface con refCount >= 1.
 * - markDisposed() destruye el peer AWT inmediatamente (HRFC-001 RC-1).
 * - Si el GameLoop intenta usar la BS después de markDisposed(), recibe
 *   IllegalStateException absorbida en beginPresent()/endPresent().
 * - La gate garantiza que el GameLoop nunca adquiere una surface en
 *   proceso de transición, suspendida o parcialmente reconstruida.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   publish() / unpublish() / openGate() / closeGate() → EDT únicamente.
 *   acquireFrame() / releaseFrame()                    → GameLoop únicamente.
 *   notifyContentLost()                                → GameLoop; callback thread-safe.
 *   publishedRef → AtomicReference: lectura/escritura atómica.
 *   gate         → WindowReadinessGate: AtomicReference interno, lock-free.
 */
public final class SurfacePublisher implements RenderGateway {

    private static final Logger LOG = Logger.getLogger(SurfacePublisher.class.getName());

    /**
     * La superficie publicada actualmente. null si no hay ninguna.
     *
     * Escritura: solo EDT (a través de publish/unpublish).
     * Lectura: cualquier thread, atómica.
     */
    private final AtomicReference<RenderSurface> publishedRef =
        new AtomicReference<>(null);

    /**
     * Barrera explícita de readiness.
     *
     * Comienza cerrada. Se abre en openGate() (llamado por el pipeline
     * tras publicar la primera surface READY). Se cierra en closeGate()
     * al iniciar cualquier transición, suspender o detectar un fallo.
     */
    private final WindowReadinessGate gate = new WindowReadinessGate();

    private final ScalingMode scalingMode;
    private final boolean     useInterpolation;

    /**
     * Callback para encolar RecreateBufferStrategy cuando el GameLoop detecta
     * contentsLost(). Thread-safe: usa invokeLater internamente.
     */
    private final Runnable onContentLost;

    /**
     * Callback para encolar una recuperación cuando la gate está cerrada y
     * no hay surface (estado permanentemente bloqueado tras un build fallido).
     * Thread-safe: usa invokeLater internamente.
     *
     * Disparado desde notifyContentLost() cuando gate está cerrada, y desde
     * acquireFrame() cuando gate está abierta pero publishedRef es null
     * (estado inconsistente: gate abierta sin surface — no debería ocurrir
     * en producción, pero se maneja defensivamente).
     */
    private final Runnable onRecoveryNeeded;

    public SurfacePublisher(ScalingMode scalingMode, boolean useInterpolation,
                            Runnable onContentLost,
                            Runnable onRecoveryNeeded) {
        this.scalingMode       = scalingMode;
        this.useInterpolation  = useInterpolation;
        this.onContentLost     = onContentLost     != null ? onContentLost     : () -> {};
        this.onRecoveryNeeded  = onRecoveryNeeded  != null ? onRecoveryNeeded  : () -> {};
    }

    /**
     * Constructor de compatibilidad — sin callback de recuperación.
     * En producción preferir el constructor completo con onRecoveryNeeded.
     */
    public SurfacePublisher(ScalingMode scalingMode, boolean useInterpolation,
                            Runnable onContentLost) {
        this(scalingMode, useInterpolation, onContentLost, null);
    }

    /**
     * Constructor sin callbacks (compatibilidad / tests).
     */
    public SurfacePublisher(ScalingMode scalingMode, boolean useInterpolation) {
        this(scalingMode, useInterpolation, null, null);
    }

    // ── API para el EDT ───────────────────────────────────────────────────────

    /**
     * Publica una nueva superficie como la activa para los próximos frames.
     *
     * La superficie anterior queda marcada como descartada pero permanece viva
     * mientras el GameLoop tenga frames activos sobre ella.
     *
     * Si newSurface es null (ej. build() falló), equivale a unpublish().
     * En ese caso NO se abre la gate: el GameLoop seguirá haciendo drops.
     *
     * Nota: no llama gate.open(). La gate se abre explícitamente mediante
     * openGate() para que el pipeline controle exactamente en qué momento
     * el GameLoop puede volver a adquirir frames.
     *
     * EDT únicamente.
     */
    public void publish(RenderSurface newSurface) {
        RenderSurface old = publishedRef.getAndSet(newSurface);
        if (old != null) {
            old.markDisposed();
            LOG.fine("SurfacePublisher: old surface marked for disposal.");
        }
        if (newSurface != null) {
            LOG.fine("SurfacePublisher: new surface published.");
        } else {
            LOG.fine("SurfacePublisher: publish(null) — surface slot cleared.");
        }
    }

    /**
     * Retira la superficie publicada. Los acquireFrame() subsiguientes retornarán
     * null (si la gate está abierta y publishedRef es null se activa onRecoveryNeeded,
     * pero normalmente la gate se cierra antes de unpublish en el pipeline).
     *
     * EDT únicamente.
     */
    public void unpublish() {
        publish(null);
    }

    /**
     * Abre la ReadinessGate.
     *
     * Llamar desde el pipeline después de publicar una surface READY y antes
     * de liberar suppressResize. El GameLoop podrá adquirir frames a partir
     * del siguiente ciclo.
     *
     * EDT únicamente.
     */
    public void openGate() {
        gate.open();
    }

    /**
     * Cierra la ReadinessGate.
     *
     * Llamar desde el pipeline antes de iniciar cualquier transición,
     * al suspender (SuspendRendering) o al detectar un estado inválido.
     * El GameLoop descartará frames desde el siguiente ciclo.
     *
     * Llamar ANTES de unpublish() para garantizar que el GameLoop no adquiere
     * la surface en el instante entre la apertura de la gate y el unpublish.
     *
     * EDT únicamente.
     */
    public void closeGate() {
        gate.close();
    }

    /**
     * True si hay una superficie publicada actualmente.
     * Usado por el pipeline para calcular SurfaceState en publishState().
     *
     * EDT únicamente (para coherencia con publishState).
     */
    public boolean hasPublishedSurface() {
        return publishedRef.get() != null;
    }

    /**
     * True si la ReadinessGate está abierta.
     * Thread-safe (AtomicReference interno). Útil para diagnóstico.
     */
    public boolean isGateOpen() {
        return gate.isOpen();
    }

    // ── RenderGateway (GameLoop thread) ───────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * Comprueba la gate ANTES de leer publishedRef. Si la gate está cerrada,
     * retorna null sin acceder a ningún recurso gráfico.
     *
     * Si la gate está abierta pero publishedRef es null (estado inconsistente
     * que no debería ocurrir en producción tras el HRFC-002), dispara
     * onRecoveryNeeded para salir del deadlock.
     *
     * GameLoop thread únicamente.
     */
    @Override
    public RenderFrame acquireFrame() {
        // Barrera explícita: la gate cierra el paso durante transiciones,
        // suspensión, estados LOST/FAILED/RECREATING.
        if (!gate.isOpen()) return null;

        RenderSurface s = publishedRef.get();

        if (s == null) {
            // Gate abierta pero sin surface: estado inconsistente.
            // Disparar recuperación para que el pipeline lo corrija.
            LOG.warning("SurfacePublisher: gate open but no surface published — triggering recovery");
            triggerRecovery();
            return null;
        }

        if (!s.acquire()) {
            // Carrera: surface marcada disposed justo entre gate.isOpen() y acquire().
            // El siguiente ciclo lo reintentará con la nueva surface.
            return null;
        }

        return new RenderFrame(s, scalingMode, useInterpolation);
    }

    /**
     * {@inheritDoc}
     *
     * Seguro llamar con null (no-op).
     * GameLoop thread únicamente.
     */
    @Override
    public void releaseFrame(RenderFrame frame) {
        if (frame != null) {
            frame.releaseInternal();
        }
    }

    /**
     * {@inheritDoc}
     *
     * El GameLoop llama este método cuando detecta contentsLost() en la BS.
     * Si la gate está abierta: encola RecreateBufferStrategy (flujo normal).
     * Si la gate está cerrada: la transición ya está gestionando el rebuild;
     *   no se encola nada para evitar comandos duplicados. El pipeline, al
     *   terminar la transición, publicará una surface nueva.
     *
     * Thread-safe.
     */
    @Override
    public void notifyContentLost() {
        LOG.fine("SurfacePublisher: notifyContentLost() received");
        if (gate.isOpen()) {
            LOG.fine("SurfacePublisher: gate open — requesting surface rebuild via onContentLost");
            triggerContentLost();
        } else {
            LOG.fine("SurfacePublisher: gate closed — transition in progress, skipping duplicate rebuild");
        }
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void triggerContentLost() {
        try {
            onContentLost.run();
        } catch (Exception e) {
            LOG.warning("SurfacePublisher: onContentLost callback threw: " + e.getMessage());
        }
    }

    private void triggerRecovery() {
        try {
            onRecoveryNeeded.run();
        } catch (Exception e) {
            LOG.warning("SurfacePublisher: onRecoveryNeeded callback threw: " + e.getMessage());
        }
    }
}
