package Entradas;

import Entradas.Listeners.KeyActionListener;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *  Estados continuos  → consultados por poll: KeyBoard.getState("stateKey")
 *  Acciones de edge   → notificadas por push:  KeyActionListener.onKeyAction(action)
 *
 * ─── CONCURRENCIA ─────────────────────────────────────────────────────────────
 *
 *  EDT escribe rawKeys bajo keyLock.
 *  GameLoop (update) copia rawKeys → snapshot bajo keyLock (scope mínimo),
 *  luego lee snapshot sin lock.
 *
 * ─── BUGS MANTENIDOS ─────────────────────────────────────────────────────────
 *
 *  BUG-06  · focusLost() limpia rawKeys y estados — MANTENIDO.
 *  BUG-07  · snapshot atómico con lock dedicado (keyLock) — MANTENIDO.
 *  BUG-F11 · guard fsTogglePending para doble-toggle de fullscreen — MANTENIDO.
 *            Sigue operativo: el binding de F11 tiene edgeAction "toggleFullscreen".
 *            La lógica del guard está en update() detectando esa acción concreta.
 */
public class KeyBoard implements KeyListener, FocusListener {

    // ─── Tabla de bindings — ÚNICO lugar a editar para añadir/cambiar teclas ──

    /**
     * Agrega aquí nuevos KeyBinding para extender el sistema.
     *
     * Factory statics disponibles en KeyBinding:
     *   · stateOnly(keyCode, stateKey)               → solo estado continuo
     *   · edgeOnly(keyCode, edgeAction)               → solo edge semántico
     *   · stateAndEdge(keyCode, stateKey, edgeAction) → ambos
     *
     * Nombres de stateKey sugeridos: usar camelCase del campo que sustituyen.
     * Nombres de edgeAction sugeridos: verbos en camelCase ("jump", "reload").
     */
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
     * Mapa de estados continuos actualizado cada frame por update().
     * Consulta thread-safe desde el GameLoop (solo el GameLoop lo lee).
     * Clave = KeyBinding.stateKey.
     *
     * Uso: KeyBoard.getState("up"), KeyBoard.getState("shift"), etc.
     */
    private static final Map<String, Boolean> states = new HashMap<>();

    /** @return true si la tecla con ese stateKey está pulsada este frame. */
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

    // ─── Guard F11 doble-toggle (BUG-F11 mantenido) ───────────────────────────

    /**
     * Protege contra doble-edge de "toggleFullscreen" cuando el toggle tarda
     * más de un frame (hardware lento, compositor DWM en Windows 11).
     * volatile: escrito desde GameLoop, puesto a false desde EDT.
     */
    private volatile boolean fsTogglePending = false;

    /** Llamar desde el EDT cuando el toggle de fullscreen haya terminado. */
    public void clearFsTogglePending() {
        fsTogglePending = false;
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private final List<KeyActionListener> listeners = new CopyOnWriteArrayList<>();

    public void addKeyActionListener(KeyActionListener l)    { listeners.add(l);    }
    public void removeKeyActionListener(KeyActionListener l) { listeners.remove(l); }

    // ─── Update (GameLoop thread, 1 vez por frame) ────────────────────────────

    public void update() {

        // ── Snapshot atómico bajo keyLock (scope mínimo) ──────────────────────
        synchronized (keyLock) {
            System.arraycopy(rawKeys, 0, snapshot, 0, rawKeys.length);
        }

        // ── Actualizar estados continuos desde snapshot ───────────────────────
        for (KeyBinding b : BINDINGS) {
            if (b.stateKey != null) {
                states.put(b.stateKey, snapshot[b.keyCode]);
            }
        }

        // ── Detectar edges y disparar listeners ───────────────────────────────
        for (KeyBinding b : BINDINGS) {
            if (b.edgeAction == null) continue;

            boolean risingEdge = snapshot[b.keyCode] && !lastKeys[b.keyCode];
            if (!risingEdge) continue;

            // Guard F11 doble-toggle (BUG-F11 mantenido)
            if ("toggleFullscreen".equals(b.edgeAction) && fsTogglePending) continue;

            if ("toggleFullscreen".equals(b.edgeAction)) {
                fsTogglePending = true; // activar ANTES de notificar
            }

            for (KeyActionListener l : listeners) {
                l.onKeyAction(b.edgeAction);
            }
        }

        // ── Snapshot para el próximo frame ────────────────────────────────────
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

    // ─── FocusListener (EDT) — BUG-06 mantenido ──────────────────────────────

    @Override
    public void focusLost(FocusEvent e) {
        synchronized (keyLock) {
            java.util.Arrays.fill(rawKeys, false);
        }
        // Limpiar todos los estados continuos registrados
        for (KeyBinding b : BINDINGS) {
            if (b.stateKey != null) {
                states.put(b.stateKey, false);
            }
        }
    }

    @Override
    public void focusGained(FocusEvent e) {}
}
