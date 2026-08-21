package Main;

import Display.Surface.RenderFrame;
import Display.Surface.RenderGateway;
import Inputs.KeyBoard;
import Inputs.MouseInput;
import Main.Debug.TemporalDiagnostics;
import Main.States.GameState;
import Main.States.StateManager;

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
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * CONTRATO TEMPORAL CANÓNICO:
 *
 * GameLoop es la ÚNICA FUENTE DE VERDAD para el tiempo del simulation step.
 *
 *   deltaTime → segundos REALES transcurridos en el simulation step
 *
 * IMPLEMENTACIÓN — REAL DELTATIME (no fixed timestep):
 *
 *   long now = System.nanoTime();
 *   long elapsed = now - lastTime;
 *   double deltaTime = elapsed / 1_000_000_000.0;  // tiempo REAL en segundos
 *   lastTime = now;
 *
 *   // Clamp para estabilidad (NO es fixed timestep)
 *   if (deltaTime > MAX_DELTA) deltaTime = MAX_DELTA;
 *
 *   update(deltaTime);  // propagar tiempo real a toda la jerarquía
 *
 * DIFERENCIA vs FIXED TIMESTEP:
 *   Fixed timestep:  deltaTime = targetTime / 1e9 (siempre 1/60 para 60 FPS)
 *   Real deltaTime:  deltaTime = elapsed / 1e9 (variable según tiempo real)
 *
 * EJEMPLOS (60 FPS target):
 *   Frame rápido (8ms):   deltaTime = 0.008s
 *   Frame normal (16ms):  deltaTime = 0.016s  
 *   Frame lento (50ms):   deltaTime = 0.050s
 *   Lag spike (200ms):    deltaTime = 0.083s (clamped a ~5 frames)
 *
 * UNIDADES CANÓNICAS:
 *   Velocity       → unidades / segundo (u/s)
 *   Acceleration   → unidades / segundo² (u/s²)
 *   Force          → masa × unidades / segundo²
 *   Impulse        → masa × unidades / segundo
 *   Position       → unidades espaciales
 *   Time           → segundos (REALES, no ticks ni frames)
 *
 * INTEGRACIÓN:
 *   Δv = a × dt
 *   Δv = (F / m) × dt
 *   Δv = J / m           (impulsos NO usan deltaTime)
 *   Δx = v × dt
 *
 * INVARIANTE FUNDAMENTAL:
 *   El comportamiento del juego debe ser independiente del framerate.
 *   30 FPS, 60 FPS, 120 FPS, 144 FPS deben producir aproximadamente
 *   el mismo estado físico tras el mismo tiempo real simulado.
 *
 * PROHIBIDO:
 *   - Obtener tiempo independientemente (System.nanoTime(), currentTimeMillis())
 *   - Usar constantes temporales hardcodeadas (1/60, 0.016, magic multipliers)
 *   - Mantener contadores frame-based (counter++, ticks--) para fenómenos temporales
 *
 * DISTRIBUCIÓN:
 *   GameLoop.update(deltaTime)
 *     → StateManager.update(temporalContext)
 *       → State.update(temporalContext)
 *         → [si GameState] WorldManager.update(temporalContext)
 *           → CollisionsSystem.update(objects, temporalContext)
 *             → Physics2D.applyGravity(onGround, deltaTime)
 *             → Physics2D.flushAccumulatedForces(deltaTime)
 *             → Physics2D.updateMoves(position, deltaTime)
 *
 * Ningún subsistema debe calcular su propio deltaTime. Todos reciben
 * el valor calculado aquí y lo propagan hacia abajo.
 *
 * ── HRFC-DT-003 — Temporal State Pipeline ────────────────────────────────
 *
 * EVOLUCIÓN ARQUITECTÓNICA:
 *
 * GameLoop ya no conoce GameState directamente. En su lugar delega
 * a StateManager, que distribuye el TemporalContext al estado activo:
 *
 *   GameLoop (calcula TemporalContext)
 *     ↓
 *   StateManager (distribuye a estado activo)
 *     ↓
 *   State (GameState, MenuState, PauseState, etc.)
 *
 * DESACOPLAMIENTO:
 *
 * GameLoop NO conoce:
 *   - Physics2D, Player, Bullet, Weapon, World, Enemy
 *   - Qué estado está activo
 *   - Si el estado ejecuta simulación física o no
 *
 * GameLoop SOLO conoce:
 *   - RenderGateway (para render)
 *   - StateManager (para distribuir tiempo)
 *   - Input (para polling)
 *
 * GARANTÍA TEMPORAL:
 *
 * El TemporalContext fluye consistentemente sin importar qué estado
 * esté activo. La transición entre estados no rompe el flujo temporal.
 *
 * ── Threading ────────────────────────────────────────────────────────────
 *
 * run() y render() → GameLoop thread únicamente.
 * stop() → puede llamarse desde cualquier thread.
 */
public final class GameLoop implements Runnable {

    private final RenderGateway  renderGateway;
    private final StateManager   stateManager;
    private final KeyBoard       keyboard;
    private final MouseInput     mouse;

    private final double  targetTime;
    private Thread        thread;
    private volatile boolean running = false;

    /**
     * Contador monótono de simulation steps.
     *
     * ── HRFC-DT-002 — Unified Temporal Context ───────────────────────────
     *
     * Incrementa en cada simulation step para generar stepIds únicos.
     * Permite verificar que todos los sistemas utilizan el mismo
     * TemporalContext durante un step mediante comparación de stepId.
     */
    private long simulationStepCounter = 0;

    /**
     * Multiplicador para el clamp de deltaTime durante lag spikes.
     *
     * MAX_DELTA_CATCH_UP = 5.0 significa que el deltaTime máximo permitido
     * es 5 veces el targetTime (5 frames de margen).
     *
     * Para 60 FPS (targetTime = 16.67ms):
     *   maxDelta = 5 × 16.67ms = 83.35ms (~12 FPS mínimo)
     *
     * Para 30 FPS (targetTime = 33.33ms):
     *   maxDelta = 5 × 33.33ms = 166.65ms (~6 FPS mínimo)
     *
     * Lag spikes mayores (GC pause de 500ms, debugger break, OS suspend)
     * se clampan a este valor máximo. El juego "pierde" ese tiempo en lugar
     * de intentar recuperarlo con múltiples updates, que es el comportamiento
     * correcto para un juego de acción:
     *   - Preferir continuidad sobre precisión absoluta
     *   - Evitar sprints de lógica sin renders intercalados
     *   - Prevenir físicas inestables con dt extremos (tunneling, overflow)
     *
     * Esto NO convierte el sistema en fixed timestep — deltaTime sigue siendo
     * variable y representa tiempo real, solo con un límite superior de seguridad.
     */
    private static final double MAX_DELTA_CATCH_UP = 5.0;

    public GameLoop(RenderGateway renderGateway,
                    StateManager stateManager,
                    KeyBoard keyboard,
                    MouseInput mouse,
                    int fps) {
        this.renderGateway = renderGateway;
        this.stateManager  = stateManager;
        this.keyboard      = keyboard;
        this.mouse         = mouse;
        this.targetTime    = 1_000_000_000.0 / fps;

        // Configurar diagnóstico temporal
        TemporalDiagnostics.configure(fps, MAX_DELTA_CATCH_UP);
        TemporalDiagnostics.enable();
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
        long lastTime         = System.nanoTime();
        long timer            = 0;
        int  simulationSteps  = 0;  // UPS counter
        int  renderedFrames   = 0;  // FPS counter

        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastTime;
            timer += elapsed;
            lastTime = now;

            // ── HRFC — REAL DELTATIME CALCULATION ─────────────────────────
            // deltaTime representa el tiempo REAL transcurrido entre este
            // simulation step y el anterior, medido en segundos.
            //
            // ANTES: deltaTime = targetTime / 1e9 (fixed timestep)
            // AHORA: deltaTime = elapsed / 1e9 (tiempo real)
            //
            // Ejemplos:
            //   Frame rápido (8ms):   deltaTime = 0.008s
            //   Frame normal (16ms):  deltaTime = 0.016s
            //   Frame lento (50ms):   deltaTime = 0.050s
            //   Lag spike (200ms):    deltaTime = 0.200s → clamped
            double deltaTimeSeconds = elapsed / 1_000_000_000.0;

            // ── HRFC — DIAGNOSTIC INSTRUMENTATION ─────────────────────────
            TemporalDiagnostics.recordRawDelta(deltaTimeSeconds);

            // Clamp deltaTime para evitar valores extremos durante lag spikes.
            // Esto NO convierte el sistema en fixed timestep — simplemente
            // previene que un GC pause de 500ms cause físicas inestables.
            //
            // MAX_DELTA_CATCH_UP = 5.0 representa ~5 frames a 60 FPS.
            // Equivale a ~83ms máximo por simulation step (12 FPS mínimo).
            // Durante un lag spike mayor, el juego "pierde" ese tiempo en
            // lugar de intentar recuperarlo, que es correcto para un juego
            // de acción (preferir continuidad sobre precisión absoluta).
            double maxDeltaSeconds = MAX_DELTA_CATCH_UP * (targetTime / 1_000_000_000.0);
            if (deltaTimeSeconds > maxDeltaSeconds) {
                deltaTimeSeconds = maxDeltaSeconds;
            }

            // ── HRFC — DIAGNOSTIC INSTRUMENTATION ─────────────────────────
            TemporalDiagnostics.recordEffectiveDelta(deltaTimeSeconds);

            // ── HRFC-DT-002 — Unified Temporal Context ───────────────────
            // Crear el contexto temporal único para este simulation step.
            // Este contexto será propagado inmutablemente a través de toda
            // la jerarquía de sistemas. Ningún sistema debe calcular su
            // propio deltaTime — todos derivan su comportamiento temporal
            // de este contexto.
            TemporalContext temporalContext = TemporalContext.of(
                simulationStepCounter,
                deltaTimeSeconds
            );
            simulationStepCounter++;

            // Ejecutar simulation step con el contexto temporal canónico
            update(temporalContext);
            simulationSteps++;

            // ── HRFC — DIAGNOSTIC INSTRUMENTATION ─────────────────────────
            TemporalDiagnostics.recordUpdate();

            // Renderizar el estado actualizado
            render();
            renderedFrames++;

            // ── HRFC — DIAGNOSTIC INSTRUMENTATION ─────────────────────────
            TemporalDiagnostics.recordRender();

            // Sleep para respetar el target framerate
            long frameTime = System.nanoTime() - now;
            long sleepMs   = (long)(targetTime - frameTime) / 1_000_000;
            if (sleepMs > 0) {
                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            // Actualizar contadores cada segundo
            if (timer >= 1_000_000_000L) {
                // ── HRFC-DT-003: FPS/UPS a través de StateManager ────────
                // Si el estado activo es GameState, actualizar contadores.
                // Otros estados pueden no tener esta funcionalidad.
                if (stateManager.getActiveState() instanceof GameState) {
                    GameState gameState = (GameState) stateManager.getActiveState();
                    gameState.setFps(renderedFrames);   // renders reales
                    gameState.setUps(simulationSteps);  // updates ejecutados
                }

                // ── HRFC — DIAGNOSTIC REPORT ──────────────────────────────
                TemporalDiagnostics.checkAndReport();

                renderedFrames   = 0;
                simulationSteps  = 0;
                timer            = 0;
            }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Actualiza la lógica del juego.
     *
     * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────
     *
     * AUTORIDAD TEMPORAL:
     *
     * Este método es el punto de entrada del contrato temporal canónico.
     * deltaTime se calcula UNA ÚNICA VEZ por simulation step en run()
     * midiendo el tiempo REAL transcurrido desde el frame anterior:
     *
     *   elapsed = System.nanoTime() - lastTime
     *   deltaTime = elapsed / 1_000_000_000.0  (segundos REALES)
     *
     * Este valor se propaga inmutablemente a través de toda la jerarquía
     * de sistemas sin ser recalculado ni modificado.
     *
     * ── HRFC-DT-002 — Unified Temporal Context ───────────────────────────
     *
     * EVOLUCIÓN:
     *
     * El deltaTime primitivo (double) fue reemplazado por TemporalContext,
     * una abstracción que:
     *   1. Transporta simulationDeltaTime calculado por GameLoop
     *   2. Tiene un stepId único verificable por identidad
     *   3. Es inmutable — no puede ser modificado por sistemas
     *   4. Es package-private en construcción — solo GameLoop lo crea
     *
     * DISTRIBUCIÓN:
     *   update(context) → stateManager.update(context)
     *                  → state.update(context)
     *                  → [si GameState] worldManager.update(context)
     *                  → subsystems...
     *
     * INVARIANTE:
     *   Ningún subsistema modifica el contexto.
     *   Ningún subsistema calcula su propio deltaTime.
     *   Todos los sistemas temporales derivan su comportamiento de este contexto.
     *   Todos los sistemas reciben LA MISMA INSTANCIA de TemporalContext.
     *
     * ── HRFC-DT-003 — Temporal State Pipeline ────────────────────────────
     *
     * DESACOPLAMIENTO:
     *
     * GameLoop delega a StateManager, que distribuye el contexto al estado
     * activo. GameLoop NO conoce si el estado ejecuta física, renderiza UI,
     * o simplemente ignora el tiempo.
     *
     * @param temporalContext contexto temporal del simulation step (autoridad única)
     */
    private void update(TemporalContext temporalContext) {
        keyboard.update();
        mouse.flushEvents();
        stateManager.update(temporalContext);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Renderiza el estado activo.
     *
     * ── HRFC-DT-003: Render a través de StateManager ─────────────────────
     *
     * GameLoop no conoce qué estado está activo ni qué dibuja cada estado.
     * Delega a StateManager, que delega al estado activo.
     */
    private void render() {
        RenderFrame frame = renderGateway.acquireFrame();
        if (frame == null) return; // sin superficie: drop silencioso

        try {
            // Fase 1: render por capas (estado decide qué dibuja).
            stateManager.draw(frame);

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
