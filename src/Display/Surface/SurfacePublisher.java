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
 *
 * ──────────────────────────────────────────────────────────────────────────
 * SWAP ATÓMICO (publish)
 *
 *   EDT:
 *     newSurface = surfaceBuilder.build(viewport)   // construye completamente
 *     publish(newSurface)                            // getAndSet: newSurface reemplaza old
 *     // old.markDisposed() → se libera cuando refCount llegue a 0
 *
 *   GameLoop (puede estar corriendo en paralelo):
 *     RenderSurface s = publishedRef.get()          // lectura atómica
 *     if (!s.acquire()) return null                 // ya descartada: no la usa
 *     // usa la superficie — s permanece viva mientras refCount > 0
 *     s.release()                                   // si disposed && refCount==0 → dispose
 *
 * ──────────────────────────────────────────────────────────────────────────
 * ¿POR QUÉ EL CRASH ES IMPOSIBLE?
 *
 * Antes: bsRef volatile → EDT setea null → GameLoop llama bs.show() sobre
 * el objeto ya dispuesto → NPE.
 *
 * Ahora: el GameLoop nunca tiene acceso directo a BufferStrategy.
 * Solo tiene RenderFrame, que referencia RenderSurface con refCount >= 1.
 * markDisposed() no puede llamar disposeNow() mientras refCount > 0.
 * El EDT construye la nueva superficie en una instancia completamente diferente.
 * No hay objeto compartido mutable entre los dos threads.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   publish() / unpublish() / hasPublishedSurface() → EDT únicamente.
 *   acquireFrame() / releaseFrame() → GameLoop únicamente.
 *   publishedRef → AtomicReference: lectura/escritura atómica sin locks.
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

    private final ScalingMode scalingMode;
    private final boolean     useInterpolation;

    public SurfacePublisher(ScalingMode scalingMode, boolean useInterpolation) {
        this.scalingMode      = scalingMode;
        this.useInterpolation = useInterpolation;
    }

    // ── API para el EDT ───────────────────────────────────────────────────────

    /**
     * Publica una nueva superficie como la activa para los próximos frames.
     *
     * La superficie anterior queda marcada como descartada pero permanece viva
     * mientras el GameLoop tenga frames activos sobre ella. Cuando el último
     * frame la libera, se disposa automáticamente.
     *
     * Si newSurface es null (ej. build() falló), equivale a unpublish().
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
        }
    }

    /**
     * Retira la superficie publicada. Los acquireFrame() subsiguientes
     * retornarán null hasta la próxima llamada a publish().
     *
     * EDT únicamente.
     */
    public void unpublish() {
        publish(null);
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

    // ── RenderGateway (GameLoop thread) ───────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * Lee publishedRef atómicamente. Si la superficie puede adquirirse
     * (no fue marcada disposed entre la lectura y el acquire), retorna
     * un RenderFrame válido. En caso contrario, retorna null.
     *
     * GameLoop thread únicamente.
     */
    @Override
    public RenderFrame acquireFrame() {
        RenderSurface s = publishedRef.get();
        if (s == null) return null;
        if (!s.acquire()) {
            // Carrera extremadamente improbable: fue marcada disposed
            // justo después del get(). El siguiente frame lo reintentará.
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
}
