package Display.Surface;

import Display.ViewportInfo;

import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Snapshot completo e inmutable de una superficie de render.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: RACE CONDITION EN acquire() / markDisposed()
 *
 * Problema anterior:
 *   acquire() leía disposed.get() y luego hacía refCount.incrementAndGet()
 *   en dos operaciones separadas. Entre ellas el EDT podía ejecutar
 *   markDisposed() que, al ver refCount == 0, llamaba disposeNow().
 *   El GameLoop entonces incrementaba refCount sobre una surface ya
 *   dispuesta y usaba una BufferStrategy inválida.
 *
 *   Este escenario era especialmente probable con refresh rates altos
 *   (120/144 Hz) donde el GameLoop y el EDT compiten con mayor frecuencia,
 *   o en equipos con scheduling más agresivo — explicando por qué el
 *   problema es consistente en unos equipos y ausente en otros.
 *
 * Solución — protocolo de refCount con sentinel negativo:
 *
 *   refCount codifica dos cosas en un solo AtomicInteger:
 *     - Valor ≥ 0 : superficie viva, valor = número de consumidores activos.
 *     - Valor < 0 : superficie marcada para dispose (sentinel DISPOSED_SENTINEL).
 *
 *   acquire():
 *     Loop CAS: lee current. Si current < 0 → ya dispuesta, retorna false.
 *     Si current ≥ 0 → intenta CAS(current, current+1).
 *     Si CAS falla (otro thread cambió el valor), reintenta.
 *     Cuando CAS tiene éxito, el GameLoop tiene la garantía de que la
 *     surface NO puede ser dispuesta mientras refCount > 0, porque
 *     markDisposed() solo llama disposeNow() cuando el count efectivo
 *     llega a 0 tras aplicar el sentinel.
 *
 *   markDisposed():
 *     Suma DISPOSED_SENTINEL (Integer.MIN_VALUE/2) al refCount con getAndAdd.
 *     Si el valor anterior era 0 (nadie la tenía adquirida), disposa ahora.
 *     Si era > 0, el dispose ocurrirá en el release() del último consumidor.
 *
 *   release():
 *     Decrementa refCount. Si el resultado es exactamente DISPOSED_SENTINEL
 *     (es decir, el count efectivo llegó a 0 y el sentinel está aplicado),
 *     disposa ahora.
 *
 *   Invariante: disposeNow() se llama exactamente una vez, en el momento
 *   en que el último consumidor libera la surface Y el sentinel está activo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 * - markDisposed()  → EDT únicamente.
 * - acquire()       → GameLoop únicamente (desde acquireFrame).
 * - release()       → GameLoop únicamente (desde releaseFrame).
 * - Todos los campos son finales o atómicos; no requieren sincronización adicional.
 */
public final class RenderSurface {

    private static final Logger LOG = Logger.getLogger(RenderSurface.class.getName());

    /**
     * Sentinel que se suma al refCount cuando la surface es marcada para dispose.
     * Valor suficientemente negativo para que ninguna cantidad realista de
     * acquire() concurrentes lo lleve de vuelta a ≥ 0.
     */
    private static final int DISPOSED_SENTINEL = Integer.MIN_VALUE / 2;

    private final BufferStrategy bufferStrategy;
    private final BufferedImage  framebuffer;
    private final ViewportInfo   viewport;
    private final int            virtualWidth;
    private final int            virtualHeight;

    /**
     * Codifica el estado de la surface:
     *   ≥ 0               → viva; valor = consumidores activos.
     *   DISPOSED_SENTINEL → marcada para dispose, sin consumidores.
     *   DISPOSED_SENTINEL + N → marcada para dispose, N consumidores activos.
     */
    private final AtomicInteger refCount = new AtomicInteger(0);

    /** Solo SurfaceBuilder construye instancias. */
    RenderSurface(BufferStrategy bs, BufferedImage fb,
                  ViewportInfo vp, int virtualWidth, int virtualHeight) {
        this.bufferStrategy = bs;
        this.framebuffer    = fb;
        this.viewport       = vp;
        this.virtualWidth   = virtualWidth;
        this.virtualHeight  = virtualHeight;
    }

    // ── API para RenderGateway (GameLoop thread) ──────────────────────────────

    /**
     * Intenta adquirir la surface para un frame.
     *
     * Retorna false si la surface ya fue marcada como descartada por el EDT.
     * Si retorna true, la surface permanece válida hasta que se llame release(),
     * sin excepción — incluso si el EDT llama markDisposed() mientras tanto.
     *
     * GameLoop thread únicamente.
     */
    boolean acquire() {
        int current;
        do {
            current = refCount.get();
            if (current < 0) {
                // El sentinel ya fue aplicado: surface dispuesta o en proceso.
                return false;
            }
            // CAS: solo incrementa si el valor no cambió desde la lectura.
            // Si el EDT aplicó el sentinel entre el get() y el CAS, el CAS falla
            // y el siguiente ciclo leerá current < 0 → retorna false.
        } while (!refCount.compareAndSet(current, current + 1));
        return true;
    }

    /**
     * Libera la adquisición.
     *
     * Si la surface fue marcada para dispose y este era el último consumidor,
     * llama disposeNow() exactamente una vez.
     *
     * GameLoop thread únicamente.
     */
    void release() {
        int after = refCount.decrementAndGet();
        // Si after == DISPOSED_SENTINEL, el sentinel está aplicado y no quedan
        // consumidores activos (count efectivo = 0). Es el momento de disponer.
        if (after == DISPOSED_SENTINEL) {
            disposeNow();
        }
    }

    // ── API para el EDT ───────────────────────────────────────────────────────

    /**
     * Marca la surface como descartada de forma atómica.
     *
     * Suma DISPOSED_SENTINEL al refCount en una sola operación atómica.
     * Si el count anterior era 0 (nadie la tenía adquirida), el resultado
     * es DISPOSED_SENTINEL y se disposa ahora mismo. Si había consumidores
     * activos, el dispose ocurrirá cuando el último llame release().
     *
     * Esta operación es completamente libre de carreras con acquire():
     * si acquire() ya incrementó el count antes de que markDisposed() lo lea,
     * el resultado será DISPOSED_SENTINEL + N (N > 0) y el dispose se aplazará.
     * Si markDisposed() corrió primero, acquire() leerá current < 0 y fallará.
     *
     * EDT únicamente.
     */
    void markDisposed() {
        int prev = refCount.getAndAdd(DISPOSED_SENTINEL);
        if (prev == 0) {
            // Nadie la tenía adquirida: disponer ahora mismo.
            disposeNow();
        }
        // Si prev > 0: el dispose ocurrirá en el último release().
    }

    // ── Acceso interno (package-private para RenderFrame) ─────────────────────

    BufferStrategy getBufferStrategy() { return bufferStrategy; }
    BufferedImage  getFramebuffer()    { return framebuffer;    }
    ViewportInfo   getViewport()       { return viewport;       }
    int            getVirtualWidth()   { return virtualWidth;   }
    int            getVirtualHeight()  { return virtualHeight;  }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void disposeNow() {
        try {
            bufferStrategy.dispose();
            LOG.fine("RenderSurface disposed.");
        } catch (Exception e) {
            LOG.fine("RenderSurface.disposeNow(): " + e.getMessage());
        }
    }
}
