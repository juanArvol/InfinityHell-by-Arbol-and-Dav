package Main;

import Display.Managers.DisplayManager;
import Inputs.KeyBoard;
import Inputs.MouseInput;
import Main.States.GameState;

import java.awt.Graphics2D;

public final class GameLoop implements Runnable {

    private final DisplayManager display;
    private final GameState      gameState;
    private final KeyBoard       keyboard;
    private final MouseInput     mouse;

    private final double  targetTime;
    private Thread        thread;
    private volatile boolean running = false;

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
                gameState.setFps(frames);
                frames = 0;
                timer  = 0;
            }
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    private void update() {
        keyboard.update();
        mouse.flushEvents();
        // Las dimensiones virtuales no cambian en cada frame; GameState
        // las recibió en construcción y las recibe de nuevo solo cuando
        // ocurre un resize real (vía DisplayManager.addResizeListener).
        gameState.update();
    }

    // ─── Render ───────────────────────────────────────────────────────────────

    private void render() {
        // beginFrame() siempre retorna un Graphics2D válido (BufferedImage en memoria).
        Graphics2D virtualG = display.beginFrame();

        try {
            gameState.draw(virtualG);
        } finally {
            // endFrame() descarta virtualG y presenta a pantalla si el BS está listo.
            // Si el BS no está disponible (transición fullscreen, resize), el frame
            // se descarta silenciosamente — el siguiente frame lo reemplazará.
            display.endFrame(virtualG);
        }
    }
}
