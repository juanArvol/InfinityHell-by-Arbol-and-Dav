package Display.Surface;

import Display.Background.DisplayBackground;
import Display.ViewportInfo;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Snapshot completo e inmutable de una superficie de render.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * PROTOCOLO DE CICLO DE VIDA — INVARIANTE
 *
 * Una RenderSurface tiene dos dimensiones de ciclo de vida independientes:
 *
 *   1. Ciclo de vida LÓGICO (refCount):
 *        acquire()      → incrementa refCount; garantiza que la surface no
 *                         se dispone mientras el consumidor la usa.
 *        release()      → decrementa refCount; si queda 0 y el sentinel
 *                         está aplicado, disposa.
 *
 *   2. Ciclo de vida AWT (BufferStrategy peer):
 *        markDisposed() → llama disposeNow() INMEDIATAMENTE e incondicionalmente
 *                         para destruir el peer AWT de la BS.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: RACE CONDITION BS-ZOMBIE DURANTE TRANSICIONES RÁPIDAS
 *
 * Problema anterior:
 *   markDisposed() solo llamaba disposeNow() si refCount == 0. Si el GameLoop
 *   tenía refCount > 0, el dispose se aplazaba hasta release(). Mientras tanto,
 *   el pipeline EDT llamaba canvas.createBufferStrategy() en buildAndPublish(),
 *   que destruye implícitamente el peer AWT de la BS anterior. La surface antigua
 *   quedaba en estado "zombie": refCount > 0 (el GameLoop la usa) pero el peer
 *   AWT ya fue destruido por canvas.createBufferStrategy().
 *
 *   Cuando el GameLoop finalmente llamaba release() → disposeNow() → bs.dispose(),
 *   llamaba dispose() sobre una BS con peer ya destruido. En algunas plataformas
 *   (especialmente Windows con DWM) esto lanzaba IllegalStateException o producía
 *   comportamiento undefined. En pulsaciones rápidas de F11, este estado zombie
 *   podía encadenarse con la siguiente transición, acumulando inconsistencias hasta
 *   producir un crash.
 *
 * Solución:
 *   markDisposed() llama disposeNow() SIEMPRE de forma inmediata, destruyendo el
 *   peer AWT de la BS antes de que canvas.createBufferStrategy() lo haga de forma
 *   no controlada. El GameLoop puede seguir teniendo refCount > 0, pero si intenta
 *   usar la BS (bs.getDrawGraphics(), bs.show()), recibirá IllegalStateException,
 *   que beginPresent() y endPresent() ya absorben. El frame se dropa y el siguiente
 *   ciclo adquiere la nueva surface publicada.
 *
 *   El refCount continúa su protocolo normal. release() decrementa y verifica si
 *   debe llamar disposeNow() de nuevo — pero disposeNow() es idempotente (absorbe
 *   excepciones) y la condición DISPOSED_SENTINEL garantiza que se llama exactamente
 *   una vez de forma significativa (la primera vez, que es la de markDisposed()).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN ANTERIOR: RACE CONDITION EN acquire() / markDisposed()
 *
 *   refCount codifica dos cosas en un solo AtomicInteger:
 *     - Valor ≥ 0 : superficie viva, valor = número de consumidores activos.
 *     - Valor < 0 : superficie marcada para dispose (sentinel DISPOSED_SENTINEL).
 *
 *   acquire(): Loop CAS — retorna false si current < 0 (sentinel aplicado).
 *   markDisposed(): suma DISPOSED_SENTINEL; llama disposeNow() inmediatamente.
 *   release(): decrementa; si llega a DISPOSED_SENTINEL, llama disposeNow()
 *              (segunda llamada; idempotente).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN ANTERIOR: FRAMEBUFFER LIMPIO AL INICIO DE CADA FRAME
 *
 *   RenderSurface guarda el background configurado. RenderFrame.beginVirtual()
 *   lo aplica al inicio de cada frame antes de retornar el Graphics2D.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 * - markDisposed()     → EDT únicamente.
 * - acquire()          → GameLoop únicamente (desde acquireFrame).
 * - release()          → GameLoop únicamente (desde releaseFrame).
 * - background.apply() → GameLoop únicamente (desde beginVirtual).
 * - Todos los campos son finales; no requieren sincronización adicional.
 */
public final class RenderSurface {

    private static final Logger LOG = Logger.getLogger(RenderSurface.class.getName());

    /**
     * Sentinel que se suma al refCount cuando la surface es marcada para dispose.
     * Valor suficientemente negativo para que ninguna cantidad realista de
     * acquire() concurrentes lo lleve de vuelta a ≥ 0.
     */
    private static final int DISPOSED_SENTINEL = Integer.MIN_VALUE / 2;

    private final BufferStrategy    bufferStrategy;
    private final BufferedImage     framebuffer;
    private final ViewportInfo      viewport;
    private final int               virtualWidth;
    private final int               virtualHeight;
    private final DisplayBackground background;

    /**
     * Codifica el estado de la surface:
     *   ≥ 0               → viva; valor = consumidores activos.
     *   DISPOSED_SENTINEL → sentinel aplicado, sin consumidores.
     *   DISPOSED_SENTINEL + N → sentinel aplicado, N consumidores activos.
     */
    private final AtomicInteger refCount = new AtomicInteger(0);

    /** Solo SurfaceBuilder construye instancias. */
    RenderSurface(BufferStrategy bs, BufferedImage fb,
                  ViewportInfo vp, int virtualWidth, int virtualHeight,
                  DisplayBackground background) {
        this.bufferStrategy = bs;
        this.framebuffer    = fb;
        this.viewport       = vp;
        this.virtualWidth   = virtualWidth;
        this.virtualHeight  = virtualHeight;
        this.background     = background;
    }

    // ── API para RenderGateway (GameLoop thread) ──────────────────────────────

    /**
     * Intenta adquirir la surface para un frame.
     *
     * Retorna false si la surface ya fue marcada como descartada (sentinel
     * aplicado). Si retorna true, el GameLoop puede usar la surface hasta
     * que llame release(). Si el EDT llama markDisposed() mientras tanto,
     * la BS será dispuesta inmediatamente, pero beginPresent()/endPresent()
     * absorben las IllegalStateException resultantes — el frame se dropa
     * limpiamente y el siguiente ciclo adquiere la nueva surface.
     *
     * GameLoop thread únicamente.
     */
    boolean acquire() {
        int current;
        do {
            current = refCount.get();
            if (current < 0) {
                return false;
            }
        } while (!refCount.compareAndSet(current, current + 1));
        return true;
    }

    /**
     * Libera la adquisición.
     *
     * Si el sentinel está aplicado y este era el último consumidor activo,
     * el contador llega a DISPOSED_SENTINEL y se llama disposeNow() de nuevo.
     * disposeNow() es idempotente: la llamada previa de markDisposed() ya
     * destruyó el peer AWT; esta segunda llamada absorbe la excepción
     * silenciosamente.
     *
     * GameLoop thread únicamente.
     */
    void release() {
        int after = refCount.decrementAndGet();
        if (after == DISPOSED_SENTINEL) {
            disposeNow(); // segunda llamada; idempotente (absorbe excepciones)
        }
    }

    // ── API para el EDT ───────────────────────────────────────────────────────

    /**
     * Marca la surface como descartada y destruye el peer AWT de la BS
     * de forma inmediata e incondicional.
     *
     * ── Por qué llamar disposeNow() siempre ─────────────────────────────────
     * El pipeline EDT llama unpublish() en FASE 3 y buildAndPublish() en
     * FASE 6+7. Entre medias, applyCommand() puede tardar decenas de ms
     * ejecutando transiciones de ventana. Durante ese tiempo el GameLoop puede
     * tener refCount > 0 sobre esta surface.
     *
     * buildAndPublish() llama canvas.createBufferStrategy(), que destruye
     * implícitamente el peer AWT de la BS anterior. Si disposeNow() no se
     * llamó antes, el objeto Java BufferStrategy queda apuntando a un peer
     * destruido — estado zombie. El GameLoop puede crashear o producir
     * comportamiento undefined cuando usa esa BS.
     *
     * Llamar disposeNow() aquí garantiza que el peer AWT se destruye de forma
     * controlada antes de que canvas.createBufferStrategy() lo haga de forma
     * implícita. Si el GameLoop intenta usar la BS después, recibirá
     * IllegalStateException, absorbida por beginPresent()/endPresent().
     *
     * EDT únicamente.
     */
    void markDisposed() {
        // Aplicar sentinel: a partir de aquí acquire() retornará false.
        refCount.getAndAdd(DISPOSED_SENTINEL);
        // Destruir el peer AWT inmediatamente, sin esperar a release().
        disposeNow();
    }

    // ── Acceso interno (package-private para RenderFrame) ─────────────────────

    BufferStrategy    getBufferStrategy() { return bufferStrategy; }
    BufferedImage     getFramebuffer()    { return framebuffer;    }
    ViewportInfo      getViewport()       { return viewport;       }
    int               getVirtualWidth()   { return virtualWidth;   }
    int               getVirtualHeight()  { return virtualHeight;  }
    DisplayBackground getBackground()     { return background;     }

    /**
     * Crea una nueva RenderSurface que comparte la misma BufferStrategy,
     * framebuffer y viewport, pero con un fondo diferente.
     *
     * Usado exclusivamente por el pipeline para implementar ChangeBackground
     * sin recrear la BufferStrategy. La surface original debe ser descartada
     * con markDisposed() DESPUÉS de publicar la nueva — el dispose inmediato
     * de markDisposed() destruiría la BS compartida, por lo que esta operación
     * es solo válida si la surface original no se usa ya más por el GameLoop.
     *
     * PRECONDICIÓN: la surface original debe haber sido retirada de publishedRef
     * mediante un swap atómico ANTES de llamar markDisposed() sobre ella.
     * El flujo correcto es:
     *   1. newSurface = existing.withBackground(bg)
     *   2. publishedRef.getAndSet(newSurface)  → retorna oldSurface
     *   3. oldSurface.markDisposed()           → llamar DESPUÉS del swap
     *
     * ADVERTENCIA: no llamar markDisposed() sobre oldSurface si newSurface
     * comparte su BufferStrategy — markDisposed() destruiría el peer AWT.
     * El swap de fondo debe hacerse via publishBackground(), que maneja este
     * protocolo correctamente.
     *
     * Package-private: solo para uso desde SurfacePublisher.publishBackground().
     */
    RenderSurface withBackground(DisplayBackground newBackground) {
        return new RenderSurface(
            this.bufferStrategy,
            this.framebuffer,
            this.viewport,
            this.virtualWidth,
            this.virtualHeight,
            newBackground != null ? newBackground : this.background
        );
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Destruye el peer AWT de la BufferStrategy.
     *
     * Idempotente: si ya fue llamado (peer destruido), la excepción se absorbe
     * silenciosamente. Diseñado para ser llamado más de una vez sin efecto
     * negativo (markDisposed() + posible release() cuando count == SENTINEL).
     */
    private void disposeNow() {
        try {
            bufferStrategy.dispose();
            LOG.fine("RenderSurface: BufferStrategy disposed.");
        } catch (Exception e) {
            // Absorber: peer ya destruido (dispose llamado dos veces) o
            // BS inválida por canvas.createBufferStrategy() concurrente.
            LOG.fine("RenderSurface.disposeNow(): absorbed — " + e.getMessage());
        }
    }
}
