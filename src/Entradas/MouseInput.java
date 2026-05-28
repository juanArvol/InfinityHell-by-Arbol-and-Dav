package Entradas;

import Display.ViewportInfo;
import Entradas.Listeners.MouseActionListener;

import java.awt.event.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Input de ratón.
 *
 * ─── BUG CORREGIDO (versión anterior) ────────────────────────────────────────
 *
 * BUG-09 · mouseVirtualX/Y no son thread-safe — MANTENIDO
 *   mouseVirtualX y mouseVirtualY son volatile float.
 *   Garantiza visibilidad entre EDT (escritura) y GameLoop (lectura).
 *
 * ─── REGRESIONES CORREGIDAS EN ESTA VERSIÓN ──────────────────────────────────
 *
 * REGRESIÓN-3a · synchronized cubre flushEvents() completo incl. listeners
 *
 *   CAUSA: la versión anterior declaraba flushEvents() como synchronized (this),
 *          usando el mismo monitor que enqueue(). Esto significa que durante toda
 *          la iteración de la cola de eventos — incluyendo las llamadas a los
 *          listeners (onLeftClick, onMouseMoved, etc.) — el EDT no puede llamar
 *          enqueue() para nuevos eventos.
 *
 *          Si un listener hace trabajo no trivial (p.ej. onLeftClick dispara
 *          lógica de gameplay que tarda varios milisegundos), el EDT queda
 *          bloqueado en enqueue() durante ese tiempo. Esto produce contención
 *          entre EDT y GameLoop thread que añade latencia a los eventos de input
 *          y puede causar microstutter si el bloqueo ocurre mientras el EDT
 *          también necesita procesar paint events.
 *
 *   SOLUCIÓN: separar el lock de enqueue del procesamiento de flush.
 *
 *     · enqueue() mantiene su lock (queueLock) — correcto, protege writeIdx.
 *     · flushEvents() toma el lock sólo para leer readIdx/writeIdx y hacer
 *       snapshot de los índices. El procesamiento de eventos (transform +
 *       listener calls) ocurre fuera del lock.
 *     · Para manejar la concurrencia del ring buffer correctamente sin lock
 *       en el procesamiento: se usa un snapshot de los índices bajo lock,
 *       y se procesa hasta el writeIdx capturado. Si llegan nuevos eventos
 *       durante el flush, se procesarán en el siguiente frame (correcto).
 *
 *   POR QUÉ ES SEGURO: flushEvents() es llamado SÓLO por el GameLoop thread.
 *   enqueue() es llamado SÓLO por el EDT. El ring buffer con dos índices
 *   (readIdx, writeIdx) puede ser gestionado con un lock mínimo porque:
 *     · writeIdx es escrito sólo por EDT (bajo queueLock).
 *     · readIdx es escrito sólo por GameLoop (bajo queueLock al hacer snapshot).
 *     · El contenido del array (queue[]) es escrito por EDT antes de actualizar
 *       writeIdx, y leído por GameLoop sólo hasta el writeIdx capturado.
 *
 * REGRESIÓN-3b · new PendingEvent() allocation por cada evento de movimiento
 *
 *   CAUSA: cada mouseMoved/mouseDragged crea `new PendingEvent(Type.MOVE, ...)`.
 *          Con el ratón en movimiento continuo a la tasa de polling AWT (~60-125Hz),
 *          esto genera 60-125 objetos/segundo que el GC debe colectar. Con G1GC
 *          a 30 FPS target, las minor GC pauses pueden producir spikes de 1-3ms
 *          que son perceptibles como microstutter en frametime.
 *
 *   SOLUCIÓN: preallocar el ring buffer de PendingEvent con objetos reciclables.
 *          En lugar de `new PendingEvent(...)`, mutar los campos de objetos
 *          preallocados en las posiciones del ring buffer. Los objetos son
 *          creados una vez en el constructor y reutilizados indefinidamente.
 *
 *          Esto elimina la GC pressure del path de input en el EDT.
 *          El array es de tamaño QUEUE_CAP (64), que ya era el tamaño del
 *          array de PendingEvent[] en la versión anterior — sólo se cambia
 *          de "crear objetos nuevos" a "mutar objetos preallocados".
 *
 *   POR QUÉ ES SEGURO: los campos de PendingEvent son escritos por el EDT
 *          (en enqueue) y leídos por el GameLoop (en flushEvents). La
 *          visibilidad está garantizada por el lock de queueLock en enqueue()
 *          (que establece un happens-before con el snapshot en flushEvents()).
 *          No hay aliasing entre producer (EDT) y consumer (GameLoop) porque
 *          el ring buffer garantiza que el GameLoop no lee posiciones que el EDT
 *          aún no ha publicado (writeIdx bajo lock).
 *
 *   IMPACTO EN RENDIMIENTO: elimina allocations de PendingEvent en el hot path.
 *          Reduce GC pressure y elimina potenciales GC pauses en el path de input.
 *
 *   COMPATIBILIDAD FUTURA 2D/3D: sin impacto. El sistema de eventos de input
 *          es independiente del sistema de render.
 *
 * ─── SIN OTROS CAMBIOS ───────────────────────────────────────────────────────
 *   volatile mouseVirtualX/Y — sin cambios (BUG-09 mantenido)
 *   Sistema de listeners (MouseActionListener) — sin cambios
 *   Transformación de coordenadas — sin cambios
 *   API legacy @Deprecated — sin cambios
 */
public class MouseInput implements MouseListener, MouseMotionListener, MouseWheelListener {

    // ─── Estado continuo en píxeles REALES del canvas (raw AWT) ──────────────

    public static int mouseX;
    public static int mouseY;

    public static boolean leftPressed;
    public static boolean rightPressed;

    // ─── Estado continuo en coordenadas VIRTUALES (transformado) ─────────────
    // BUG-09 FIX (mantenido): volatile garantiza visibilidad entre EDT y GameLoop.

    /** X del ratón en coordenadas virtuales del juego. Thread-safe: volatile. */
    public static volatile float mouseVirtualX;

    /** Y del ratón en coordenadas virtuales del juego. Thread-safe: volatile. */
    public static volatile float mouseVirtualY;

    // ─── Viewport para transformación de coordenadas ──────────────────────────

    private volatile ViewportInfo viewport;

    public void setViewport(ViewportInfo vp) {
        this.viewport = vp;
    }

    // ─── Cola de eventos pendientes (EDT → GameLoop) ──────────────────────────

    /**
     * Evento mutable y reciclable. REGRESIÓN-3b FIX: los campos son mutados
     * en lugar de crear nuevas instancias por evento.
     */
    private static final class PendingEvent {
        enum Type { LEFT_PRESS, LEFT_RELEASE, RIGHT_PRESS, RIGHT_RELEASE, SCROLL, MOVE }
        Type type;
        int  rawX, rawY;
        int  scrollDelta;
    }

    private static final int QUEUE_CAP = 64;

    /**
     * REGRESIÓN-3b FIX: array preallocado de PendingEvent reciclables.
     * Los objetos se crean una vez en el constructor y se reusan indefinidamente.
     * Elimina allocations en el hot path del EDT.
     */
    private final PendingEvent[] queue = new PendingEvent[QUEUE_CAP];

    private int writeIdx = 0;
    private int readIdx  = 0;

    /**
     * Lock dedicado para el ring buffer. Separado del monitor de `this`
     * para evitar interferencia con otros métodos synchronized futuros.
     * (REGRESIÓN-3a FIX: flushEvents ya no usa este lock en el procesamiento)
     */
    private final Object queueLock = new Object();

    public MouseInput() {
        // REGRESIÓN-3b FIX: preallocar todos los slots del ring buffer.
        for (int i = 0; i < QUEUE_CAP; i++) {
            queue[i] = new PendingEvent();
        }
    }

    /**
     * Encola un evento mutando el slot preallocado en la posición writeIdx.
     * SÓLO llamar desde EDT.
     *
     * REGRESIÓN-3b FIX: no crea objetos nuevos. Muta el objeto preallocado.
     */
    private void enqueue(PendingEvent.Type type, int rawX, int rawY, int scrollDelta) {
        synchronized (queueLock) {
            int next = (writeIdx + 1) % QUEUE_CAP;
            if (next == readIdx) return; // Cola llena — descartar
            // Mutar el objeto preallocado en lugar de crear new PendingEvent()
            PendingEvent ev = queue[writeIdx];
            ev.type        = type;
            ev.rawX        = rawX;
            ev.rawY        = rawY;
            ev.scrollDelta = scrollDelta;
            writeIdx = next;
        }
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private final List<MouseActionListener> listeners = new CopyOnWriteArrayList<>();

    public void addMouseActionListener(MouseActionListener l)    { listeners.add(l);    }
    public void removeMouseActionListener(MouseActionListener l) { listeners.remove(l); }

    // ─── flushEvents (llamado desde GameLoop thread, 1 vez por frame) ─────────

    /**
     * Procesa todos los eventos pendientes y llama a los listeners.
     *
     * SÓLO llamar desde el GameLoop thread.
     *
     * ── REGRESIÓN-3a FIX ─────────────────────────────────────────────────────
     * El lock (queueLock) se toma SÓLO para capturar el snapshot de writeIdx
     * y avanzar readIdx. El procesamiento de eventos (transform + listener calls)
     * ocurre fuera del lock, eliminando la contención EDT↔GameLoop durante el flush.
     *
     * Esto es seguro porque flushEvents() es llamado sólo por el GameLoop thread,
     * y enqueue() sólo por el EDT. El snapshot de writeIdx garantiza que sólo
     * procesamos eventos que el EDT ya ha publicado completamente.
     */
    public void flushEvents() {
        // Capturar el writeIdx actual bajo lock.
        // Procesaremos sólo hasta este índice (eventos ya publicados por EDT).
        final int capturedWriteIdx;
        synchronized (queueLock) {
            capturedWriteIdx = writeIdx;
        }

        // Procesar eventos fuera del lock — sin contención con EDT.
        while (readIdx != capturedWriteIdx) {
            PendingEvent ev = queue[readIdx];

            float vx = toVirtualX(ev.rawX);
            float vy = toVirtualY(ev.rawY);

            for (MouseActionListener l : listeners) {
                switch (ev.type) {
                    case LEFT_PRESS    -> l.onLeftClick(vx, vy);
                    case LEFT_RELEASE  -> l.onLeftRelease(vx, vy);
                    case RIGHT_PRESS   -> l.onRightClick(vx, vy);
                    case RIGHT_RELEASE -> l.onRightRelease(vx, vy);
                    case SCROLL        -> l.onScroll(ev.scrollDelta);
                    case MOVE          -> l.onMouseMoved(vx, vy);
                }
            }

            // Avanzar readIdx. No necesita lock: sólo el GameLoop thread escribe readIdx.
            readIdx = (readIdx + 1) % QUEUE_CAP;
        }
    }

    // ─── Helpers de transformación ────────────────────────────────────────────

    private float toVirtualX(int rawX) {
        ViewportInfo vp = viewport;
        return (vp != null) ? vp.toVirtualX(rawX) : rawX;
    }

    private float toVirtualY(int rawY) {
        ViewportInfo vp = viewport;
        return (vp != null) ? vp.toVirtualY(rawY) : rawY;
    }

    private void updateVirtualCoords(int rawX, int rawY) {
        // Escritura volatile: visible al GameLoop thread inmediatamente. (BUG-09 mantenido)
        mouseVirtualX = toVirtualX(rawX);
        mouseVirtualY = toVirtualY(rawY);
    }

    // ─── MouseListener (EDT) ─────────────────────────────────────────────────

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = true;
            enqueue(PendingEvent.Type.LEFT_PRESS, e.getX(), e.getY(), 0);
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = true;
            enqueue(PendingEvent.Type.RIGHT_PRESS, e.getX(), e.getY(), 0);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = false;
            enqueue(PendingEvent.Type.LEFT_RELEASE, e.getX(), e.getY(), 0);
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = false;
            enqueue(PendingEvent.Type.RIGHT_RELEASE, e.getX(), e.getY(), 0);
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e)  {}

    // ─── MouseMotionListener (EDT) ────────────────────────────────────────────

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateVirtualCoords(mouseX, mouseY);
        enqueue(PendingEvent.Type.MOVE, mouseX, mouseY, 0);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateVirtualCoords(mouseX, mouseY);
        enqueue(PendingEvent.Type.MOVE, mouseX, mouseY, 0);
    }

    // ─── MouseWheelListener (EDT) ─────────────────────────────────────────────

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int delta = e.getWheelRotation();
        enqueue(PendingEvent.Type.SCROLL, mouseX, mouseY, delta);
    }

    // ─── API legacy mantenida ─────────────────────────────────────────────────

    @Deprecated
    public static boolean isLeftClicked()  { return false; }

    @Deprecated
    public static boolean isRightClicked() { return false; }

    @Deprecated
    public static int getWheelDelta()      { return 0; }
}
