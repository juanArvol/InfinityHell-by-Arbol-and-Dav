package Inputs;

import Display.ViewportInfo;
import Inputs.Listeners.MouseActionListener;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Input de ratón.
 *
 * ─── ARQUITECTURA ─────────────────────────────────────────────────────────────
 *
 *  Toda la configuración de botones vive en BUTTONS. Para agregar un botón
 *  nuevo basta con añadir un MouseButton ahí. No hay que tocar mousePressed(),
 *  mouseReleased(), flushEvents(), ni MouseActionListener.
 *
 *  Estados continuos  → consultados por poll: mouse.getButtonState("stateKey")
 *  Acciones de edge   → notificadas por push:  MouseActionListener.onMouseAction(action, vx, vy)
 *
 * ─── MIGRACIÓN DE ESTADO ESTÁTICO ─────────────────────────────────────────────
 *
 *  mouseX/Y, mouseVirtualX/Y y buttonStates eran campos estáticos compartidos
 *  por todas las instancias. Los problemas son los mismos que se resolvieron
 *  en KeyBoard y GameEventBus:
 *    - Dos instancias compartían estado (no aislable para tests).
 *    - Dependencias implícitas: cualquier clase podía leer MouseInput.mouseX
 *      sin declarar ninguna dependencia explícita.
 *
 *  Ahora todos los campos son de instancia. Para compatibilidad con el código
 *  existente se mantienen los métodos estáticos como delegadores a la instancia
 *  registrada con setActiveInstance() en GameOrquester.
 *
 * ─── CONCURRENCIA ─────────────────────────────────────────────────────────────
 *
 *  BUG-09  (mantenido) · mouseVirtualX/Y son volatile.
 *  REGRESIÓN-3a (mantenida) · queueLock cubre solo el snapshot de índices;
 *    el procesamiento de eventos (transform + listener calls) ocurre fuera del lock.
 *  REGRESIÓN-3b (mantenida) · ring buffer preallocado — sin allocations en el hot path.
 */
public class MouseInput implements MouseListener, MouseMotionListener, MouseWheelListener {

    // ─── Tabla de botones — ÚNICO lugar a editar para añadir/cambiar botones ──

    /**
     * Agrega aquí nuevos MouseButton para extender el sistema.
     *
     * new MouseButton(awtButton, stateKey, pressAction, releaseAction)
     *   · awtButton     = MouseEvent.BUTTON1 / BUTTON2 / BUTTON3
     *   · stateKey      = nombre del estado continuo, o null
     *   · pressAction   = acción semántica en press, o null
     *   · releaseAction = acción semántica en release, o null
     */
    public static final MouseButton[] BUTTONS = {
        new MouseButton(MouseEvent.BUTTON1, "leftPressed",  "leftClick",   "leftRelease"),
        new MouseButton(MouseEvent.BUTTON3, "rightPressed", "rightClick",  "rightRelease"),
        // Ejemplo para agregar botón central:
        // new MouseButton(MouseEvent.BUTTON2, null, "middleClick", null),
    };

    // ─── Estado continuo de coordenadas en píxeles REALES del canvas (raw AWT) ─

    /** X del ratón en píxeles reales del canvas. Solo GameLoop thread tras flushEvents(). */
    public int mouseX;
    /** Y del ratón en píxeles reales del canvas. Solo GameLoop thread tras flushEvents(). */
    public int mouseY;

    // ─── Estado continuo en coordenadas VIRTUALES (volatile: leído desde GameLoop) ─

    /** X del ratón en coordenadas virtuales del juego. Thread-safe: volatile. */
    public volatile float mouseVirtualX;

    /** Y del ratón en coordenadas virtuales del juego. Thread-safe: volatile. */
    public volatile float mouseVirtualY;

    // ─── Estado continuo de botones indexado por stateKey ─────────────────────

    /**
     * Mapa de estados de botones de esta instancia.
     * Ahora es de instancia para que cada MouseInput tenga estado independiente.
     */
    private final Map<String, Boolean> buttonStateMap = new HashMap<>();

    /**
     * Instancia activa global para compatibilidad con el código existente.
     * GameOrquester llama a setActiveInstance() justo después de crear el MouseInput.
     */
    private static volatile MouseInput activeInstance;

    public static void setActiveInstance(MouseInput instance) {
        activeInstance = instance;
    }

    /**
     * Consulta el estado de un botón en la instancia activa global.
     * Retorna false si no hay instancia activa registrada.
     */
    public static boolean getButtonState(String stateKey) {
        MouseInput inst = activeInstance;
        if (inst == null) return false;
        return inst.getInstanceButtonState(stateKey);
    }

    /** Consulta el estado de un botón de esta instancia específica. */
    public boolean getInstanceButtonState(String stateKey) {
        Boolean v = buttonStateMap.get(stateKey);
        return v != null && v;
    }

    // ─── Viewport ─────────────────────────────────────────────────────────────

    private volatile ViewportInfo viewport;

    public void setViewport(ViewportInfo vp) {
        this.viewport = vp;
    }

    // ─── Cola de eventos pendientes (EDT → GameLoop) ──────────────────────────

    /**
     * Evento mutable y reciclable.
     * REGRESIÓN-3b mantenida: campos mutados en lugar de new por evento.
     */
    private static final class PendingEvent {
        enum Type { BUTTON_PRESS, BUTTON_RELEASE, SCROLL, MOVE }

        Type   type;
        int    awtButton;   // para BUTTON_PRESS / BUTTON_RELEASE
        int    rawX, rawY;
        int    scrollDelta;
    }

    private static final int QUEUE_CAP = 64;

    /** Array preallocado — sin allocations en el hot path. */
    private final PendingEvent[] queue = new PendingEvent[QUEUE_CAP];

    private int writeIdx = 0;
    private int readIdx  = 0;

    private final Object queueLock = new Object();

    public MouseInput() {
        for (int i = 0; i < QUEUE_CAP; i++) {
            queue[i] = new PendingEvent();
        }
    }

    /**
     * Encola un evento mutando el slot preallocado.
     * Solo llamar desde EDT.
     */
    private void enqueueButton(PendingEvent.Type type, int awtButton, int rawX, int rawY) {
        synchronized (queueLock) {
            int next = (writeIdx + 1) % QUEUE_CAP;
            if (next == readIdx) return;
            PendingEvent ev = queue[writeIdx];
            ev.type      = type;
            ev.awtButton = awtButton;
            ev.rawX      = rawX;
            ev.rawY      = rawY;
            ev.scrollDelta = 0;
            writeIdx = next;
        }
    }

    private void enqueueScroll(int delta) {
        synchronized (queueLock) {
            int next = (writeIdx + 1) % QUEUE_CAP;
            if (next == readIdx) return;
            PendingEvent ev = queue[writeIdx];
            ev.type        = PendingEvent.Type.SCROLL;
            ev.scrollDelta = delta;
            ev.rawX        = mouseX;
            ev.rawY        = mouseY;
            writeIdx = next;
        }
    }

    private void enqueueMove(int rawX, int rawY) {
        synchronized (queueLock) {
            int next = (writeIdx + 1) % QUEUE_CAP;
            if (next == readIdx) return;
            PendingEvent ev = queue[writeIdx];
            ev.type   = PendingEvent.Type.MOVE;
            ev.rawX   = rawX;
            ev.rawY   = rawY;
            writeIdx = next;
        }
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private final List<MouseActionListener> listeners = new CopyOnWriteArrayList<>();

    public void addMouseActionListener(MouseActionListener l)    { listeners.add(l);    }
    public void removeMouseActionListener(MouseActionListener l) { listeners.remove(l); }

    // ─── flushEvents (GameLoop thread, 1 vez por frame) ──────────────────────

    /**
     * Procesa todos los eventos pendientes y notifica listeners.
     * Solo llamar desde el GameLoop thread.
     *
     * REGRESIÓN-3a mantenida: lock solo para capturar el writeIdx.
     * El procesamiento ocurre fuera del lock.
     */
    public void flushEvents() {
        final int capturedWriteIdx;
        synchronized (queueLock) {
            capturedWriteIdx = writeIdx;
        }

        while (readIdx != capturedWriteIdx) {
            PendingEvent ev = queue[readIdx];

            switch (ev.type) {
                case BUTTON_PRESS, BUTTON_RELEASE -> {
                    float vx = toVirtualX(ev.rawX);
                    float vy = toVirtualY(ev.rawY);
                    boolean isPress = ev.type == PendingEvent.Type.BUTTON_PRESS;

                    for (MouseButton mb : BUTTONS) {
                        if (mb.awtButton != ev.awtButton) continue;

                        // Actualizar estado continuo
                        if (mb.stateKey != null) {
                            buttonStateMap.put(mb.stateKey, isPress);
                        }

                        // Disparar acción semántica
                        String action = isPress ? mb.pressAction : mb.releaseAction;
                        if (action != null) {
                            for (MouseActionListener l : listeners) {
                                l.onMouseAction(action, vx, vy);
                            }
                        }
                    }
                }
                case SCROLL -> {
                    for (MouseActionListener l : listeners) {
                        l.onScroll(ev.scrollDelta);
                    }
                }
                case MOVE -> {
                    float vx = toVirtualX(ev.rawX);
                    float vy = toVirtualY(ev.rawY);
                    for (MouseActionListener l : listeners) {
                        l.onMouseMoved(vx, vy);
                    }
                }
            }

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
        mouseVirtualX = toVirtualX(rawX);
        mouseVirtualY = toVirtualY(rawY);
    }

    // ─── MouseListener (EDT) ──────────────────────────────────────────────────

    @Override
    public void mousePressed(MouseEvent e) {
        enqueueButton(PendingEvent.Type.BUTTON_PRESS, e.getButton(), e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        enqueueButton(PendingEvent.Type.BUTTON_RELEASE, e.getButton(), e.getX(), e.getY());
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
        enqueueMove(mouseX, mouseY);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateVirtualCoords(mouseX, mouseY);
        enqueueMove(mouseX, mouseY);
    }

    // ─── MouseWheelListener (EDT) ─────────────────────────────────────────────

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        enqueueScroll(e.getWheelRotation());
    }
}
