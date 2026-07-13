package Inputs;

import Inputs.Listeners.KeyActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Teclado del juego.
 *
 * ─── ARQUITECTURA ─────────────────────────────────────────────────────────────
 *
 *  Toda la configuración de teclas vive en BINDINGS. Para agregar una tecla
 *  nueva basta con añadir un KeyBinding ahí. No hay que tocar update(),
 *  focusLost(), ni KeyActionListener.
 *
 *  Estados continuos → consultados por poll: keyboard.getState("stateKey")
 *  Acciones de edge  → notificadas por push: KeyActionListener.onKeyAction(action)
 *
 * ─── MIGRACIÓN DE ESTADO ESTÁTICO ─────────────────────────────────────────────
 *
 *  El Map de estados era estático, lo que hacía que:
 *    - Dos instancias de KeyBoard compartieran el mismo estado (confuso).
 *    - El estado de un test contaminara los siguientes.
 *    - Las dependencias fueran implícitas: cualquier clase podía leer
 *      KeyBoard.getState() sin declarar ninguna dependencia explícita.
 *
 *  Ahora el mapa de estados es de instancia. Para compatibilidad con el
 *  código existente se mantiene KeyBoard.getState(key) como delegador a
 *  la instancia activa (igual que GameEventBus.GLOBAL).
 *
 *  Plan de migración:
 *    Hoy: KeyBoard.getState(key) → delega a la instancia en GameOrquester.
 *    Futuro: pasar la instancia explícita a cada sistema que la necesite.
 */
public class KeyBoard implements KeyListener, FocusListener {

    // ─── Tabla de bindings ────────────────────────────────────────────────────

    public static final KeyBinding[] BINDINGS = {

        // ── Movimiento ────────────────────────────────────────────────────────
        KeyBinding.stateOnly     (KeyEvent.VK_W,      "up"),
        KeyBinding.stateOnly     (KeyEvent.VK_A,      "left"),
        KeyBinding.stateOnly     (KeyEvent.VK_D,      "right"),
        KeyBinding.stateOnly     (KeyEvent.VK_SHIFT,  "shift"),

        // ── Movimiento con edge ───────────────────────────────────────────────
        KeyBinding.stateAndEdge  (KeyEvent.VK_S,      "down",  "crouch"),
        KeyBinding.stateAndEdge  (KeyEvent.VK_SPACE,  "space", "jump"),

        // ── Combate ───────────────────────────────────────────────────────────
        KeyBinding.stateAndEdge  (KeyEvent.VK_R,      "r",     "reload"),
        KeyBinding.stateAndEdge  (KeyEvent.VK_C,      "c",     "special"),

        // ── Sistema ───────────────────────────────────────────────────────────
        KeyBinding.edgeOnly      (KeyEvent.VK_F11,    "toggleFullscreen"),
        KeyBinding.edgeOnly      (KeyEvent.VK_ESCAPE, "pause"),
        KeyBinding.edgeOnly      (KeyEvent.VK_F3,     "toggleFps"),
    };

    // ─── Estado continuo indexado por stateKey ────────────────────────────────

    /**
     * Mapa de estados continuos de esta instancia.
     * Ahora es de instancia, no estático, para que cada KeyBoard tenga
     * su propio estado independiente.
     *
     * Se mantiene el método estático getState() como delegador a la instancia
     * activa registrada en setActiveInstance() para compatibilidad con el
     * código existente que usa KeyBoard.getState(key) directamente.
     */
    private final java.util.Map<String, Boolean> stateMap = new java.util.HashMap<>();

    /**
     * Instancia activa global para compatibilidad con el código existente.
     * GameOrquester registra la instancia al crear el KeyBoard.
     * Usar getState(key) es correcto para juego single-player con una sola instancia.
     * Para aislamiento real (tests, multiplayer) usar getInstanceState(key) en la instancia.
     */
    private static volatile KeyBoard activeInstance;

    public static void setActiveInstance(KeyBoard instance) {
        activeInstance = instance;
    }

    /**
     * Consulta el estado continuo de la instancia activa global.
     * Retorna false si no hay instancia activa registrada.
     */
    public static boolean getState(String stateKey) {
        KeyBoard inst = activeInstance;
        if (inst == null) return false;
        return inst.getInstanceState(stateKey);
    }

    /** Consulta el estado continuo de esta instancia específica. */
    public boolean getInstanceState(String stateKey) {
        Boolean v = stateMap.get(stateKey);
        return v != null && v;
    }

    // ─── Lock dedicado ────────────────────────────────────────────────────────

    private final Object keyLock = new Object();

    // ─── Buffers internos ─────────────────────────────────────────────────────

    private final boolean[] rawKeys  = new boolean[256];
    private final boolean[] snapshot = new boolean[256];
    private final boolean[] lastKeys = new boolean[256];



    // ─── Listeners ────────────────────────────────────────────────────────────

    private final List<KeyActionListener> listeners = new CopyOnWriteArrayList<>();

    public void addKeyActionListener(KeyActionListener l)    { listeners.add(l);    }
    public void removeKeyActionListener(KeyActionListener l) { listeners.remove(l); }

    // ─── Update (GameLoop thread, 1 vez por frame) ────────────────────────────

    public void update() {

        // ── Snapshot atómico bajo keyLock ─────────────────────────────────────
        synchronized (keyLock) {
            System.arraycopy(rawKeys, 0, snapshot, 0, rawKeys.length);
        }

        // ── Actualizar estados continuos ──────────────────────────────────────
        for (KeyBinding b : BINDINGS) {
            if (b.stateKey != null) {
                stateMap.put(b.stateKey, snapshot[b.keyCode]);
            }
        }

        // ── Disparar edge actions (rising edge: off → on) ─────────────────────
        // Se dispara una sola vez en el frame en que la tecla pasa de
        // no-pulsada (lastKeys=false) a pulsada (snapshot=true).
        // lastKeys se actualiza al FINAL de update(), garantizando que la
        // comparación usa siempre el estado del frame anterior.
        if (!listeners.isEmpty()) {
            for (KeyBinding b : BINDINGS) {
                if (b.edgeAction == null) continue;
                if (!lastKeys[b.keyCode] && snapshot[b.keyCode]) {
                    for (KeyActionListener l : listeners) {
                        l.onKeyAction(b.edgeAction);
                    }
                }
            }
        }

        // ── Guardar snapshot para el próximo frame ────────────────────────────
        System.arraycopy(snapshot, 0, lastKeys, 0, snapshot.length);
    }

    // ─── KeyListener (EDT) ────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < rawKeys.length) {
            synchronized (keyLock) {
                rawKeys[code] = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < rawKeys.length) {
            synchronized (keyLock) {
                rawKeys[code] = false;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ─── FocusListener (EDT) ──────────────────────────────────────────────────

    @Override
    public void focusLost(FocusEvent e) {
        // Limpiar teclas al perder foco para evitar estados pegados.
        synchronized (keyLock) {
            java.util.Arrays.fill(rawKeys, false);
            java.util.Arrays.fill(lastKeys, false);
        }

        // Limpiar estados continuos
        for (KeyBinding b : BINDINGS) {
            if (b.stateKey != null) {
                stateMap.put(b.stateKey, false);
            }
        }
    }

    @Override
    public void focusGained(FocusEvent e) {}
}
