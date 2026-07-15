package Main;

import Display.Surface.RenderFrame;
import Display.Surface.RenderGateway;
import Inputs.KeyBoard;
import Inputs.MouseInput;
import Main.States.GameState;

/**
 * Loop principal del juego.
 *
 * ── Cambio arquitectónico principal ──────────────────────────────────────
 *
 * GameLoop solo conoce RenderGateway. No conoce DisplayManager,
 * BufferStrategy, resize, fullscreen ni ningún detalle del ciclo de vida
 * gráfico. Toda comunicación con el subsistema gráfico ocurre a través
 * de los tres métodos de RenderGateway: acquireFrame, releaseFrame,
 * notifyContentLost.
 *
 * ── HRFC-001: flujo de render con sistema de capas ───────────────────────
 *
 * El GameLoop ya no pasa un Graphics2D a gameState.draw(). En su lugar
 * pasa el RenderFrame completo. GameState decide en qué capa dibuja cada
 * subsistema. Al terminar el dibujado, flushLayers() compone las capas
 * sobre el framebuffer antes de present().
 *
 * Flujo (AWT Audit — protocolo Oracle correcto):
 *   1. acquireFrame()            → frame o null (drop silencioso).
 *   2. gameState.draw(frame)     → cada subsistema dibuja en su capa.
 *   3. frame.flushLayers()       → componer capas sobre el framebuffer.
 *   4. frame.present()           → loop do-while Oracle completo:
 *                                    inner: getDrawGraphics + blit + dispose,
 *                                    repetir si contentsRestored() (buffer restaurado a blanco).
 *                                    outer: show() + repetir si contentsLost().
 *   5. releaseFrame(frame)       → liberar surface (siempre, en finally).
 *   6. notifyContentLost()       → si BS requiere rebuild estructural, señalizar al EDT.
 *
 * ── Garantías ────────────────────────────────────────────────────────────
 *
 * - Un frame adquirido permanece válido hasta releaseFrame().
 * - resize y fullscreen no afectan un frame ya adquirido.
 * - acquireFrame() retorna null durante transiciones → drop silencioso.
 * - notifyContentLost() es thread-safe y no bloquea el GameLoop.
 *
 * ── Threading ────────────────────────────────────────────────────────────
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

    /**
     * Número máximo de updates de catch-up por ciclo de loop.
     *
     * Si el sistema se retrasa (GC pause, OS preemption, depurador), delta
     * puede acumularse más allá de un frame. Sin un límite, el loop
     * ejecutaría tantos updates como delta acumulado, produciendo un sprint
     * de lógica sin renders intercalados que puede durar cientos de ms.
     *
     * Con este límite, tras un lag spike el juego "pierde" esos frames de
     * lógica en lugar de intentar recuperarlos todos de golpe. Para un juego
     * de acción esto es el comportamiento correcto: la simulación avanza de
     * forma continua aunque pierda frames durante el spike.
     */
    private static final double MAX_DELTA_CATCH_UP = 5.0;

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
        Thread t = thread;
        if (t == null) return;
        try {
            t.join(5_000); // espera máximo 5 segundos para shutdown limpio
        } catch (InterruptedException e) {
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

            // Limitar el catch-up para evitar sprints de lógica ilimitados
            // tras lag spikes (GC pause, depurador, OS preemption).
            if (delta > MAX_DELTA_CATCH_UP) {
                delta = MAX_DELTA_CATCH_UP;
            }

            // Desacoplar lógica de render:
            // Ejecutar todos los ticks de lógica del catch-up primero,
            // y renderizar solo UNA vez al final, con el estado más reciente.
            // Esto evita renders intermedios innecesarios que acumulan frames
            // sin beneficio visual y saturan la CPU durante lag spikes.
            boolean needsRender = false;
            while (delta >= 1) {
                update();
                delta--;
                frames++;
                needsRender = true;
            }

            if (needsRender) {
                render();
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
            // Fase 1: render por capas.
            gameState.draw(frame);

            // Fase 2: componer capas sobre el framebuffer.
            frame.flushLayers();

            // Fase 3: presentación a pantalla.
            // present() implementa el protocolo Oracle completo con do-while
            // anidado: inner loop para contentsRestored, outer loop para contentsLost.
            frame.present();

        } finally {
            renderGateway.releaseFrame(frame);

            if (frame.isContentLost()) {
                renderGateway.notifyContentLost();
            }
        }
    }
}
