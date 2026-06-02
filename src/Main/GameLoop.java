package Main;

import Display.Surface.RenderFrame;
import Display.Surface.RenderGateway;
import Inputs.KeyBoard;
import Inputs.MouseInput;
import Main.States.GameState;

import java.awt.Graphics2D;

/**
 * Loop principal del juego.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIO ARQUITECTÓNICO PRINCIPAL
 *
 * Antes: GameLoop dependía de DisplayManager y llamaba directamente a
 * beginFrame() / endFrame(), que internamente accedían a RenderSurfaceManager
 * y BufferStrategy. El GameLoop conocía indirectamente el ciclo de vida
 * gráfico a través de estos métodos.
 *
 * Ahora: GameLoop solo conoce RenderGateway. No conoce DisplayManager,
 * BufferStrategy, RenderSurfaceManager, resize, fullscreen ni ningún
 * otro detalle del subsistema gráfico.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FLUJO DE RENDER
 *
 *   1. acquireFrame()       → obtiene un frame o null (drop silencioso).
 *   2. frame.beginVirtual() → Graphics2D del framebuffer off-screen.
 *   3. gameState.draw()     → render de la escena al framebuffer.
 *   4. frame.endVirtual()   → cierra el contexto virtual.
 *   5. frame.beginPresent() → abre el contexto de pantalla (puede ser false).
 *   6. frame.present()      → copia framebuffer → pantalla con escalado.
 *   7. frame.endPresent()   → flip (bs.show()) + cierra el contexto.
 *   8. releaseFrame()       → libera el frame (en finally, siempre).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * GARANTÍAS
 *
 * - Un frame adquirido permanece válido hasta releaseFrame().
 * - resize y fullscreen no afectan un frame ya adquirido.
 * - Si no hay superficie publicada (transición), acquireFrame() retorna null
 *   y el frame se descarta silenciosamente. El siguiente tick lo reintentará.
 * - No hay null checks defensivos: el contrato de RenderGateway los elimina.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 * run() y render() → GameLoop thread únicamente.
 * stop() → puede llamarse desde cualquier thread.
 */
public final class GameLoop implements Runnable {

    private final RenderGateway renderGateway;
    private final GameState     gameState;
    private final KeyBoard      keyboard;
    private final MouseInput    mouse;

    private final double  targetTime;
    private Thread        thread;
    private volatile boolean running = false;

    public GameLoop(RenderGateway renderGateway,
                    GameState gameState,
                    KeyBoard keyboard,
                    MouseInput mouse,
                    int fps) {
        this.renderGateway = renderGateway;
        this.gameState     = gameState;
        this.keyboard      = keyboard;
        this.mouse         = mouse;
        this.targetTime    = 1_000_000_000.0 / fps;
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

    // ── Update ────────────────────────────────────────────────────────────────

    private void update() {
        keyboard.update();
        mouse.flushEvents();
        gameState.update();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void render() {
        RenderFrame frame = renderGateway.acquireFrame();
        if (frame == null) return; // sin superficie: drop silencioso

        try {
            // Fase 1: render al framebuffer virtual
            Graphics2D virtualG = frame.beginVirtual();
            try {
                gameState.draw(virtualG);
            } finally {
                frame.endVirtual(virtualG);
            }

            // Fase 2: presentación a pantalla
            if (frame.beginPresent()) {
                try {
                    frame.present();
                } finally {
                    frame.endPresent();
                }
            }

        } finally {
            // Siempre liberar el frame, incluso si lanzó una excepción.
            // Esto permite que la superficie antigua sea dispuesta por el EDT
            // cuando ya no tiene consumidores activos.
            renderGateway.releaseFrame(frame);
        }
    }
}
