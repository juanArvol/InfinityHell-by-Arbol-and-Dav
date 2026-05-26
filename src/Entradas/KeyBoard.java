package Entradas;

import Entradas.Listeners.KeyActionListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Teclado del juego — refactorizado con sistema de listeners desacoplado.
 *
 * ARQUITECTURA:
 *
 *  1. ESTADO CONTINUO (consulta directa, igual que antes):
 *     Los campos estáticos (up, left, right, down, shift, space, c, r)
 *     siguen disponibles para sistemas que los consultan cada frame,
 *     como el MovementComponent del player. No es necesario un listener
 *     para "¿está pulsada la tecla de moverse a la izquierda?" porque
 *     eso se comprueba en cada update() de física.
 *
 *  2. EVENTOS EDGE (listeners desacoplados):
 *     Acciones puntuales como recargar, saltar, toggle fullscreen o pausar
 *     se notifican a través de KeyActionListener. Los suscriptores no
 *     necesitan saber qué tecla está mapeada ni leer campos estáticos.
 *
 *     Esto permite:
 *      - Remapeo de teclas sin tocar la lógica del juego.
 *      - Múltiples suscriptores independientes (p.ej. PlayerCombat Y
 *        UIManager pueden reaccionar a onReload() de forma independiente).
 *      - Testabilidad: disparar eventos sin simular KeyEvents de AWT.
 *
 * THREAD SAFETY:
 *  Los KeyEvents llegan en el EDT (Event Dispatch Thread).
 *  Los listeners se notifican desde update(), que corre en el GameLoop thread.
 *  CopyOnWriteArrayList garantiza iteración segura sin locks visibles.
 *
 * USO:
 *   // Suscribirse a eventos (en constructor de PlayerCombat, GameLoop, etc.)
 *   keyboard.addKeyActionListener(new KeyActionListener() {
 *       public void onReload()            { combat.startReload(); }
 *       public void onToggleFullscreen()  { display.toggleFullscreen(); }
 *   });
 *
 *   // Consulta de estado continuo (en MovementComponent.update())
 *   if (KeyBoard.left)  velocity.addX(-speed);
 *   if (KeyBoard.right) velocity.addX(+speed);
 */
public class KeyBoard implements KeyListener {

    // ─── Estado continuo (consulta directa por frame) ─────────────────────────

    /** Moverse arriba / saltar (W). */
    public static boolean up;
    /** Moverse izquierda (A). */
    public static boolean left;
    /** Moverse derecha (D). */
    public static boolean right;
    /** Moverse abajo / agacharse (S). */
    public static boolean down;
    /** Correr (Shift). */
    public static boolean shift;
    /** Saltar / acción secundaria (Space). */
    public static boolean space;
    /** Habilidad especial (C). */
    public static boolean c;
    /** Recargar — estado continuo para animaciones (R). */
    public static boolean r;

    // ─── Buffer interno ───────────────────────────────────────────────────────

    /** Estado raw de cada tecla AWT. Actualizado en EDT. */
    private final boolean[] keys     = new boolean[256];
    /** Snapshot del frame anterior para detección de edge. */
    private final boolean[] lastKeys = new boolean[256];

    // ─── Listeners ────────────────────────────────────────────────────────────

    private final List<KeyActionListener> listeners = new CopyOnWriteArrayList<>();

    public void addKeyActionListener(KeyActionListener l)    { listeners.add(l);    }
    public void removeKeyActionListener(KeyActionListener l) { listeners.remove(l); }

    // ─── Update (llamado desde GameLoop thread, 1 vez por frame) ─────────────

    /**
     * Sincroniza los campos estáticos y dispara los eventos edge.
     * Llamar exactamente una vez al inicio de cada frame, antes de update().
     */
    public void update() {

        // ── Estado continuo ───────────────────────────────────────────────────
        up    = keys[KeyEvent.VK_W];
        left  = keys[KeyEvent.VK_A];
        right = keys[KeyEvent.VK_D];
        down  = keys[KeyEvent.VK_S];
        shift = keys[KeyEvent.VK_SHIFT];
        space = keys[KeyEvent.VK_SPACE];
        c     = keys[KeyEvent.VK_C];
        r     = keys[KeyEvent.VK_R];

        // ── Detección de edge (pressed este frame, no el anterior) ────────────
        boolean jumpEdge      = keys[KeyEvent.VK_SPACE]  && !lastKeys[KeyEvent.VK_SPACE];
        boolean crouchEdge    = keys[KeyEvent.VK_S]      && !lastKeys[KeyEvent.VK_S];
        boolean reloadEdge    = keys[KeyEvent.VK_R]      && !lastKeys[KeyEvent.VK_R];
        boolean specialEdge   = keys[KeyEvent.VK_C]      && !lastKeys[KeyEvent.VK_C];
        boolean f11Edge       = keys[KeyEvent.VK_F11]    && !lastKeys[KeyEvent.VK_F11];
        boolean escEdge       = keys[KeyEvent.VK_ESCAPE] && !lastKeys[KeyEvent.VK_ESCAPE];

        // ── Disparar listeners ────────────────────────────────────────────────
        if (jumpEdge || crouchEdge || reloadEdge || specialEdge || f11Edge || escEdge) {
            for (KeyActionListener l : listeners) {
                if (jumpEdge)    l.onJump();
                if (crouchEdge)  l.onCrouch();
                if (reloadEdge)  l.onReload();
                if (specialEdge) l.onSpecial();
                if (f11Edge)     l.onToggleFullscreen();
                if (escEdge)     l.onPause();
            }
        }

        // ── Snapshot para el próximo frame ────────────────────────────────────
        System.arraycopy(keys, 0, lastKeys, 0, keys.length);
    }

    // ─── KeyListener (llamado en EDT) ─────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length)
            keys[code] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length)
            keys[code] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
