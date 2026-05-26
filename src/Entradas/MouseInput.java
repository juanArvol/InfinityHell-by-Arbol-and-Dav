package Entradas;

import Display.ViewportInfo;
import Entradas.Listeners.MouseActionListener;

import java.awt.event.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Input de ratón — refactorizado con sistema de listeners desacoplado
 * y transformación automática a coordenadas virtuales.
 *
 * ARQUITECTURA:
 *
 *  1. ESTADO CONTINUO (consulta directa, igual que antes):
 *     mouseX, mouseY, leftPressed, rightPressed siguen disponibles
 *     para sistemas que los necesitan cada frame (p.ej. aim del player).
 *     NUEVO: mouseVirtualX/Y son las coordenadas ya transformadas al
 *     espacio virtual del juego — evita que cada sistema haga la conversión.
 *
 *  2. EVENTOS EDGE (listeners desacoplados):
 *     Clicks, releases y scroll se notifican a través de MouseActionListener.
 *     Las coordenadas se entregan ya en espacio VIRTUAL.
 *
 * TRANSFORMACIÓN DE COORDENADAS:
 *  El ratón reporta coordenadas en píxeles reales del canvas. El juego vive
 *  en coordenadas virtuales (p.ej. 1280×720). MouseInput usa el ViewportInfo
 *  del DisplayManager para convertir automáticamente.
 *
 *  Actualizar el viewport con setViewport() cuando DisplayManager notifique
 *  un resize (vía ResizeListener). Ejemplo en GameOrquester:
 *
 *    display.addResizeListener((rw, rh, vp) -> mouse.setViewport(vp));
 *    display.init(keyboard, mouse, mouse, mouse);
 *
 * THREAD SAFETY:
 *  Los MouseEvents llegan en EDT. Los listeners se notifican desde
 *  flushEvents(), llamado en el GameLoop thread al inicio de cada frame.
 *  Se usa una cola de eventos (array circular ligero) para no perder
 *  clicks aunque lleguen varios entre frames.
 *
 * USO:
 *   // Suscribirse a eventos
 *   mouse.addMouseActionListener(new MouseActionListener() {
 *       public void onLeftClick(float vx, float vy)  { combat.shoot(vx, vy); }
 *       public void onScroll(int delta)               { inventory.scroll(delta); }
 *   });
 *
 *   // Consulta de estado continuo (en AimComponent.update())
 *   float ax = MouseInput.mouseVirtualX;
 *   float ay = MouseInput.mouseVirtualY;
 */
public class MouseInput implements MouseListener, MouseMotionListener, MouseWheelListener {

    // ─── Estado continuo en píxeles REALES del canvas (raw AWT) ──────────────

    /** X del ratón en píxeles reales del canvas. */
    public static int mouseX;
    /** Y del ratón en píxeles reales del canvas. */
    public static int mouseY;

    /** Botón izquierdo actualmente pulsado. */
    public static boolean leftPressed;
    /** Botón derecho actualmente pulsado. */
    public static boolean rightPressed;

    // ─── Estado continuo en coordenadas VIRTUALES (transformado) ─────────────

    /** X del ratón en coordenadas virtuales del juego. */
    public static float mouseVirtualX;
    /** Y del ratón en coordenadas virtuales del juego. */
    public static float mouseVirtualY;

    // ─── Viewport para transformación de coordenadas ──────────────────────────

    /** Actualizar en cada resize via display.addResizeListener(). */
    private volatile ViewportInfo viewport;

    /**
     * Actualiza el viewport para transformación de coordenadas.
     * Llamar cuando DisplayManager notifique un resize.
     *
     * @param vp nuevo ViewportInfo (inmutable — seguro cachear)
     */
    public void setViewport(ViewportInfo vp) {
        this.viewport = vp;
    }

    // ─── Cola de eventos pendientes (EDT → GameLoop) ──────────────────────────

    /**
     * Evento raw pendiente de procesar.
     * Se acumulan en EDT y se consumen en flushEvents() (GameLoop thread).
     */
    private static final class PendingEvent {
        enum Type { LEFT_PRESS, LEFT_RELEASE, RIGHT_PRESS, RIGHT_RELEASE, SCROLL, MOVE }
        final Type  type;
        final int   rawX, rawY;
        final int   scrollDelta;

        PendingEvent(Type type, int rawX, int rawY, int scrollDelta) {
            this.type        = type;
            this.rawX        = rawX;
            this.rawY        = rawY;
            this.scrollDelta = scrollDelta;
        }
    }

    // Cola ligera: array circular de tamaño fijo (evita allocaciones en steady state)
    private static final int QUEUE_CAP = 64;
    private final PendingEvent[] queue  = new PendingEvent[QUEUE_CAP];
    private int writeIdx = 0; // escrito en EDT
    private int readIdx  = 0; // leído  en GameLoop thread

    private synchronized void enqueue(PendingEvent ev) {
        int next = (writeIdx + 1) % QUEUE_CAP;
        if (next == readIdx) return; // cola llena — descartar (no debería ocurrir a 60 FPS)
        queue[writeIdx] = ev;
        writeIdx = next;
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private final List<MouseActionListener> listeners = new CopyOnWriteArrayList<>();

    public void addMouseActionListener(MouseActionListener l)    { listeners.add(l);    }
    public void removeMouseActionListener(MouseActionListener l) { listeners.remove(l); }

    // ─── flushEvents (llamado desde GameLoop thread, 1 vez por frame) ─────────

    /**
     * Procesa todos los eventos acumulados desde el último frame y
     * notifica a los listeners con coordenadas virtuales.
     *
     * Llamar al inicio de update(), después de keyboard.update().
     */
    public synchronized void flushEvents() {
        while (readIdx != writeIdx) {
            PendingEvent ev = queue[readIdx];
            queue[readIdx]  = null; // liberar referencia
            readIdx = (readIdx + 1) % QUEUE_CAP;

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

    // ─── MouseListener (EDT) ─────────────────────────────────────────────────

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = true;
            enqueue(new PendingEvent(PendingEvent.Type.LEFT_PRESS, e.getX(), e.getY(), 0));
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = true;
            enqueue(new PendingEvent(PendingEvent.Type.RIGHT_PRESS, e.getX(), e.getY(), 0));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = false;
            enqueue(new PendingEvent(PendingEvent.Type.LEFT_RELEASE, e.getX(), e.getY(), 0));
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = false;
            enqueue(new PendingEvent(PendingEvent.Type.RIGHT_RELEASE, e.getX(), e.getY(), 0));
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
        enqueue(new PendingEvent(PendingEvent.Type.MOVE, mouseX, mouseY, 0));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateVirtualCoords(mouseX, mouseY);
        enqueue(new PendingEvent(PendingEvent.Type.MOVE, mouseX, mouseY, 0));
    }

    // ─── MouseWheelListener (EDT) ─────────────────────────────────────────────

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int delta = e.getWheelRotation();
        enqueue(new PendingEvent(PendingEvent.Type.SCROLL, mouseX, mouseY, delta));
    }

    // ─── API de consumo legacy (mantenida para compatibilidad) ────────────────

    /**
     * @deprecated Preferir suscribirse con addMouseActionListener().
     *             Mantenido para código existente que aún consuma clicks
     *             como booleano puntual.
     */
    @Deprecated
    public static boolean isLeftClicked() {
        // En el nuevo sistema, los clicks se despachan via listeners.
        // Este método sigue funcionando pero devuelve false siempre
        // porque el estado ya fue consumido por flushEvents().
        // Migrar a MouseActionListener.onLeftClick().
        return false;
    }

    /**
     * @deprecated Preferir suscribirse con addMouseActionListener().
     */
    @Deprecated
    public static boolean isRightClicked() {
        return false;
    }

    /**
     * @deprecated Preferir suscribirse con addMouseActionListener().
     */
    @Deprecated
    public static int getWheelDelta() {
        return 0;
    }
}
