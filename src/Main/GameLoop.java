package Main;

import Display.Managers.DisplayManager;
import Entradas.KeyBoard;
import Entradas.MouseInput;
import States.GameState;

import java.awt.Graphics2D;

/**
 * Game loop — actualizado para el sistema de listeners de input.
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *  1. Recibe MouseInput para llamar mouse.flushEvents() al inicio de cada
 *     frame. Esto despacha todos los eventos acumulados en el EDT
 *     (clicks, scroll, movimiento) a los listeners suscritos.
 *
 *  2. Ya NO lee KeyBoard.f11 directamente. El toggle fullscreen está
 *     registrado como KeyActionListener en GameOrquester, de forma que
 *     el GameLoop no conoce nada de fullscreen.
 *
 *  3. keyboard.update() sigue siendo la primera llamada en update():
 *     sincroniza los campos estáticos y dispara los KeyActionListeners.
 *     mouse.flushEvents() va justo después.
 *
 * ORDEN DE UPDATE POR FRAME:
 *   1. keyboard.update()        — estado continuo + edge listeners teclado
 *   2. mouse.flushEvents()      — edge listeners ratón (clicks, scroll)
 *   3. gameState.update(vw, vh) — lógica del juego en coordenadas virtuales
 */
public class GameLoop implements Runnable {

    private final DisplayManager display;
    private final GameState      gameState;
    private final KeyBoard       keyboard;
    private final MouseInput     mouse;

    private final double targetTime;
    private Thread  thread;
    private boolean running = false;
    private int     fpsPorSegundo = 0;

    public GameLoop(DisplayManager display,
                    GameState gameState,
                    KeyBoard keyboard,
                    MouseInput mouse,
                    int fps) {
        this.display   = display;
        this.gameState = gameState;
        this.keyboard  = keyboard;
        this.mouse     = mouse;
        this.targetTime = 1_000_000_000.0 / fps;
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.start();
    }

    public void stop() {
        running = false;
        try { thread.join(); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        long   lastTime = System.nanoTime();
        long   timer    = 0;
        double delta    = 0;
        int    frames   = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / targetTime;
            timer += (now - lastTime);
            lastTime = now;

            if (delta >= 1) {
                update();
                render();
                delta--;
                frames++;
            }

            long frameTime = System.nanoTime() - now;
            long sleepMs   = (long)(targetTime - frameTime) / 1_000_000;
            if (sleepMs > 0) {
                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            if (timer >= 1_000_000_000L) {
                fpsPorSegundo = frames;
                frames = 0;
                timer  = 0;
            }
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    private void update() {
        // 1. Teclado: sincroniza estado estático + dispara KeyActionListeners
        keyboard.update();

        // 2. Ratón: despacha eventos acumulados en EDT → MouseActionListeners
        //    (clicks, releases, scroll, movimiento)
        mouse.flushEvents();

        // 3. Lógica del juego en coordenadas virtuales
        //    (KeyBoard.f11 / fullscreen ya no se lee aquí — ver GameOrquester)
        gameState.update(
            display.getVirtualWidth(),
            display.getVirtualHeight()
        );
    }

    // ─── Render ───────────────────────────────────────────────────────────────

    private void render() {
        Graphics2D virtualG = display.beginFrame();
        if (virtualG == null) return;

        try {
            gameState.draw(virtualG, display.getViewport());

            virtualG.setColor(java.awt.Color.WHITE);
            virtualG.drawString("FPS: " + fpsPorSegundo, 6, 14);

        } finally {
            display.endFrame(virtualG);
        }
    }
}
