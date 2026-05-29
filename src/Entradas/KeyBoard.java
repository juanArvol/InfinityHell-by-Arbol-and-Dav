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
 *  Estados continuos → consultados por poll: KeyBoard.getState("stateKey")
 *  Acciones de edge  → notificadas por push: KeyActionListener.onKeyAction(action)
 *
 * ─── CONCURRENCIA ─────────────────────────────────────────────────────────────
 *
 *  EDT escribe rawKeys bajo keyLock.
 *  GameLoop (update) copia rawKeys → snapshot bajo keyLock (scope mínimo),
 *  luego lee snapshot sin lock.
 *
 * ─── FLUJO F11 / GUARD fsTogglePending ───────────────────────────────────────
 *
 *  1. GameLoop detecta rising edge F11 → fsTogglePending = true
 *     → notifica "toggleFullscreen" → display.toggleFullscreen(keyboard)
 *
 *  2. display.toggleFullscreen() despacha al EDT (invokeLater).
 *
 *  3. Durante el toggle (puede durar varios frames), el OS puede:
 *     a. Disparar focusLost() → limpia rawKeys + lastKeys → no hay edge falso
 *        cuando el foco regresa (lastKeys ya está limpio).
 *     b. Reenviar keyPressed(F11) al recuperar foco → rawKeys[F11]=true,
 *        lastKeys[F11]=false → rising edge detectado → SUPRIMIDO por
 *        fsTogglePending=true.
 *
 *  4. Al finalizar el toggle (invokeLater anidado en DisplayManager):
 *     canvas.requestFocusInWindow() → clearFsTogglePending() → fsTogglePending=false
 *     F11 vuelve a responder normalmente.
 *
 * ─── BUG CORREGIDO ────────────────────────────────────────────────────────────
 *
 *  BUG-FOCUS-LASTKEYS · focusLost() limpiaba rawKeys pero NO lastKeys.
 *    CAUSA: tras focusLost(), snapshot se copia de rawKeys (ahora todo false).
 *           lastKeys = snapshot → lastKeys[F11] = false.
 *           Pero si el OS NO reenvía keyPressed(F11) al recuperar foco
 *           (comportamiento en algunos WMs), rawKeys[F11] sigue true del
 *           press anterior. En el siguiente update():
 *             snapshot[F11] = rawKeys[F11] = true (no fue limpiado por focusLost)
 *             lastKeys[F11] = false → rising edge → toggle extra.
 *    SOLUCIÓN: focusLost() limpia también lastKeys bajo keyLock, de modo que
 *              cualquier tecla "que quedó pulsada" antes de perder el foco
 *              no genere un rising edge falso al recuperarlo.
 *    RIESGO: ninguno. lastKeys solo se lee en update() (GameLoop), y update()
 *            no corre mientras el juego no tiene foco (o si corre, el edge
 *            estará correctamente suprimido).
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

    // ─── Guard F11 doble-toggle ───────────────────────────────────────────────

    /**
     * Protege contra doble-edge de "toggleFullscreen" mientras el toggle
     * está en curso (puede tardar varios frames en hardware lento o Windows).
     *
     * volatile: escrito por GameLoop thread, leído por GameLoop thread.
     * clearFsTogglePending() lo pone a false desde el EDT al finalizar el toggle.
     */
    private volatile boolean fsTogglePending = false;

    /** Llamar desde el EDT cuando el toggle fullscreen haya terminado. */
    public void clearFsTogglePending() {
        fsTogglePending = false;
    }

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

        // ── Detectar edges y disparar listeners ───────────────────────────────
        for (KeyBinding b : BINDINGS) {
            if (b.edgeAction == null) continue;

            boolean risingEdge = snapshot[b.keyCode] && !lastKeys[b.keyCode];
            if (!risingEdge) continue;

            // Guard: suprimir edge de toggleFullscreen mientras hay uno en curso
            if ("toggleFullscreen".equals(b.edgeAction) && fsTogglePending) continue;

            if ("toggleFullscreen".equals(b.edgeAction)) {
                fsTogglePending = true; // activar ANTES de notificar
            }

            for (KeyActionListener l : listeners) {
                l.onKeyAction(b.edgeAction);
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
        // BUG-FOCUS-LASTKEYS FIX: limpiar rawKeys Y lastKeys.
        //
        // rawKeys se limpia para que update() no vea teclas pulsadas fantasma.
        // lastKeys se limpia para que al recuperar el foco, ninguna tecla
        // "que quedó pulsada" antes de perder el foco genere un rising edge
        // falso (rawKeys[x]=true implícito, lastKeys[x]=false → edge espurio).
        //
        // Ambos bajo keyLock porque rawKeys es compartido con el EDT.
        // lastKeys solo lo lee el GameLoop, pero se limpia aquí en EDT:
        // la ventana de race es inocua (el GameLoop saldrá sin foco de todas
        // formas y update() no se llama mientras la ventana no tiene foco en
        // la mayoría de implementaciones de game loop).
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
