package Entradas;

import Entradas.Listeners.KeyActionListener;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Teclado del juego.
 *
 * ─── BUGS CORREGIDOS ──────────────────────────────────────────────────────────
 *
 * BUG-CRÍTICO · keyReleased tenía un literal \n embebido en el código fuente
 *   CAUSA: la línea `int code = e.getKeyCode();\n        if (code >= 0...` contenía
 *          el escape de texto `\n` como caracteres literales (backslash + n) en
 *          el fuente Java, no como salto de línea. Esto es un error de edición/
 *          guardado que deja el método sintácticamente roto en ciertos compiladores
 *          o produce comportamiento inesperado dependiendo del parser.
 *          Resultado: rawKeys[code] nunca se ponía a false → las teclas quedaban
 *          permanentemente presionadas hasta que el juego perdía el foco.
 *   SOLUCIÓN: reescribir keyReleased correctamente. Sin cambios de lógica.
 *   RIESGO: ninguno. Es corrección de código corrupto.
 *
 * BUG-06 · Teclas pegadas al perder foco — MANTENIDO
 *   KeyBoard implementa FocusListener. focusLost() limpia rawKeys y campos
 *   estáticos. Se registra como FocusListener en el Canvas via WindowManager.
 *
 * BUG-07 · Race condition en keys[] — MANTENIDO (con mejora de scope)
 *   Snapshot atómico: EDT escribe en rawKeys bajo lock, update() copia
 *   rawKeys → snapshot bajo el mismo lock, luego lee snapshot sin lock.
 *
 * REGRESIÓN-2 · synchronized(this) en update() compite innecesariamente con EDT
 *   Lock dedicado (keyLock) con scope mínimo — MANTENIDO.
 *
 * ─── AÑADIDO EN VERSIÓN ANTERIOR (MANTENIDO) ─────────────────────────────────
 *
 * F3 → onToggleFps()
 *   Detección de edge para VK_F3 y disparo de listener.onToggleFps()
 *   en todos los KeyActionListeners suscritos.
 */
public class KeyBoard implements KeyListener, FocusListener {

    // ─── Estado continuo (consulta directa por frame) ─────────────────────────

    public static boolean up;
    public static boolean left;
    public static boolean right;
    public static boolean down;
    public static boolean shift;
    public static boolean space;
    public static boolean c;
    public static boolean r;

    // ─── Lock dedicado para rawKeys ────────────────────────────────────────────

    private final Object keyLock = new Object();

    // ─── Buffer interno ───────────────────────────────────────────────────────

    private final boolean[] rawKeys  = new boolean[256];
    private final boolean[] snapshot = new boolean[256];
    private final boolean[] lastKeys = new boolean[256];

    // ─── Listeners ────────────────────────────────────────────────────────────

    private final List<KeyActionListener> listeners = new CopyOnWriteArrayList<>();

    public void addKeyActionListener(KeyActionListener l)    { listeners.add(l);    }
    public void removeKeyActionListener(KeyActionListener l) { listeners.remove(l); }

    // ─── Update (llamado desde GameLoop thread, 1 vez por frame) ─────────────

    public void update() {

        // ── Snapshot atómico bajo keyLock (scope mínimo) ──────────────────────
        synchronized (keyLock) {
            System.arraycopy(rawKeys, 0, snapshot, 0, rawKeys.length);
        }

        // ── Estado continuo (desde snapshot, sin lock) ────────────────────────
        up    = snapshot[KeyEvent.VK_W];
        left  = snapshot[KeyEvent.VK_A];
        right = snapshot[KeyEvent.VK_D];
        down  = snapshot[KeyEvent.VK_S];
        shift = snapshot[KeyEvent.VK_SHIFT];
        space = snapshot[KeyEvent.VK_SPACE];
        c     = snapshot[KeyEvent.VK_C];
        r     = snapshot[KeyEvent.VK_R];

        // ── Detección de edge ─────────────────────────────────────────────────
        boolean jumpEdge    = snapshot[KeyEvent.VK_SPACE]  && !lastKeys[KeyEvent.VK_SPACE];
        boolean crouchEdge  = snapshot[KeyEvent.VK_S]      && !lastKeys[KeyEvent.VK_S];
        boolean reloadEdge  = snapshot[KeyEvent.VK_R]      && !lastKeys[KeyEvent.VK_R];
        boolean specialEdge = snapshot[KeyEvent.VK_C]      && !lastKeys[KeyEvent.VK_C];
        boolean f11Edge     = snapshot[KeyEvent.VK_F11]    && !lastKeys[KeyEvent.VK_F11];
        boolean escEdge     = snapshot[KeyEvent.VK_ESCAPE] && !lastKeys[KeyEvent.VK_ESCAPE];
        boolean f3Edge      = snapshot[KeyEvent.VK_F3]     && !lastKeys[KeyEvent.VK_F3];

        // ── Disparar listeners ────────────────────────────────────────────────
        if (jumpEdge || crouchEdge || reloadEdge || specialEdge || f11Edge || escEdge || f3Edge) {
            for (KeyActionListener l : listeners) {
                if (jumpEdge)    l.onJump();
                if (crouchEdge)  l.onCrouch();
                if (reloadEdge)  l.onReload();
                if (specialEdge) l.onSpecial();
                if (f11Edge)     l.onToggleFullscreen();
                if (escEdge)     l.onPause();
                if (f3Edge)      l.onToggleFps();
            }
        }

        // ── Snapshot para el próximo frame ────────────────────────────────────
        System.arraycopy(snapshot, 0, lastKeys, 0, snapshot.length);
    }

    // ─── KeyListener (llamado en EDT) ─────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < rawKeys.length) {
            synchronized (keyLock) {
                rawKeys[code] = true;
            }
        }
    }

    /**
     * BUG-CRÍTICO FIX: el código original tenía un literal \n (backslash+n)
     * embebido en esta línea, corrompiendo el método y haciendo que
     * rawKeys[code] nunca se pusiera a false.
     */
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

    // ─── FocusListener (llamado en EDT) — BUG-06 FIX ─────────────────────────

    @Override
    public void focusLost(FocusEvent e) {
        synchronized (keyLock) {
            java.util.Arrays.fill(rawKeys, false);
        }
        up = false; left = false; right = false; down = false;
        shift = false; space = false; c = false; r = false;
    }

    @Override
    public void focusGained(FocusEvent e) {}
}
