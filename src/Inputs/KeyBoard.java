package Inputs;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import Inputs.Listeners.KeyActionListener;

/**
 * Teclado del juego.
 *
 * ─── ARQUITECTURA ─────────────────────────────────────────────────────────────
 *
 *  Toda la configuración de teclas vive en BINDINGS. Para agregar una tecla
 *  nueva basta con añadir un KeyBinding ahí. No hay que tocar update(),
 *  focusLost(), ni KeyActionListener.
 *
 *  Estados continuos → consultados por poll: KeyBoard.getState("stateKey")
 *  Acciones de edge  → notificadas por push: KeyActionListener.onKeyAction(action)
 *
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

    private static final Map<String, Boolean> states = new HashMap<>();

    public static boolean getState(String stateKey) {
        Boolean v = states.get(stateKey);
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
                states.put(b.stateKey, snapshot[b.keyCode]);
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
                states.put(b.stateKey, false);
            }
        }
    }

    @Override
    public void focusGained(FocusEvent e) {}
}
