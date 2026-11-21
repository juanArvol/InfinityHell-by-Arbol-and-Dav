package Main;

import Display.Background.SolidColorBackground;
import Display.Managers.DisplayManager;
import Display.Settings.DisplaySettings;
import Display.Settings.ResolutionManager;
import Display.Settings.ScalingMode;
import Display.State.Resolution;
import Inputs.KeyBoard;
import Inputs.Listeners.KeyActionListener;
import Inputs.MouseInput;
import Main.Debug.DebugGameSettings;
import Main.States.GameState;
import Main.States.IdleState;
import Main.States.SettingsState;
import Main.States.StateManager;
import Sprites.Assets;
import java.awt.Color;

/**
 * Orquestador del juego.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * RESPONSABILIDADES
 *
 * GameOrquester ensambla todos los subsistemas y los conecta:
 *   - DisplayManager: ciclo de vida del Display (EDT).
 *   - RenderGateway:  acceso del GameLoop a superficies publicadas.
 *   - GameLoop:       consume frames sin conocer DisplayManager.
 *   - StateManager:   gestiona estados del juego (GameState, MenuState, etc.).
 *   - GameState:      estado inicial del juego y gameplay.
 *   - Input:          teclado y ratón conectados al canvas.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: SHUTDOWN LIMPIO EN windowClosing
 *
 * Problema anterior:
 *   WindowManager usaba JFrame.EXIT_ON_CLOSE, que llama System.exit()
 *   inmediatamente al cerrar la ventana. El GameLoop thread quedaba en
 *   ejecución hasta que la JVM lo mataba. WorldManager.bgExecutor nunca
 *   recibía shutdown() limpio. No había oportunidad de liberar recursos.
 *
 * Solución:
 *   WindowManager usa JFrame.DO_NOTHING_ON_CLOSE. GameOrquester registra
 *   un WindowAdapter en el JFrame después de init() que llama stop() y
 *   luego dispone la ventana. stop() para el GameLoop y llama
 *   StateManager.shutdown() (que para el estado activo con awaitTermination).
 *   Solo entonces se llama frame.dispose(), que cierra la ventana y
 *   permite que la JVM termine limpiamente.
 *
 *   El WindowAdapter se registra DESPUÉS de init() para evitar que se
 *   active durante la inicialización del Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-DT-003 — Temporal State Pipeline
 *
 * EVOLUCIÓN:
 *
 * GameOrquester ahora crea StateManager y lo configura con GameState como
 * estado inicial. GameLoop recibe StateManager en lugar de GameState.
 *
 * FLUJO DE CONSTRUCCIÓN:
 *   1. Crear GameState
 *   2. Crear StateManager
 *   3. Inyectar MouseInput en GameState
 *   4. Establecer GameState como estado inicial (activa onEnter)
 *   5. Crear GameLoop con StateManager
 *
 * SHUTDOWN:
 *   1. Detener GameLoop
 *   2. StateManager.shutdown() → onExit() + shutdown() en estado activo
 */
public class GameOrquester {

    private DisplayManager     display;
    private GameLoop           loop;
    private StateManager       stateManager;
    private ResolutionManager  resolutionManager;
    private KeyBoard           keyBoard;    // For SettingsState
    private MouseInput         mouseInput;  // For SettingsState
    private GameState          gameState;  // Referencia para volver del SettingsState

    public void start() {

        // ── 1. Assets ─────────────────────────────────────────────────────────
        Assets.init();

        // ── 2. Input ──────────────────────────────────────────────────────────
        KeyBoard   keyboard = new KeyBoard();
        MouseInput mouse    = new MouseInput();
        this.keyBoard = keyboard;  // Store for later use
        this.mouseInput = mouse;   // Store for later use

        KeyBoard.setActiveInstance(keyboard);
        MouseInput.setActiveInstance(mouse);

        // ── 3. Resolution Manager ─────────────────────────────────────────────
        // Crear ANTES del DisplayManager para pasarlo en el constructor
        ResolutionManager resolutionManager = new ResolutionManager(
            new Resolution(1280, 720),  // Resolución inicial fullscreen
            new Resolution(1280, 720)   // Resolución inicial windowed
        );

        // ── 4. Display ────────────────────────────────────────────────────────
        DisplaySettings settings = DisplaySettings.builder()
            .virtualResolution(1280, 720)
            .windowTitle("Infinity Hell")
            .windowedSize(1280, 720)
            .startFullscreen(true)
            .monitorIndex(0)
            // ScalingMode.FIT: Escala el juego para que quepa completamente en la ventana
            // manteniendo el aspect ratio. Agrega barras de letterbox/pillarbox si es necesario.
            // Cuando la ventana es más pequeña que la resolución virtual, el juego se escala
            // proporcionalmente para mostrarse completo, no se recorta.
            .scalingMode(ScalingMode.FIT)
            .useInterpolation(false)
            .targetFps(0)
            .fillColor(Color.BLACK)
            .background(SolidColorBackground.WHITE)
            .build();

        display = new DisplayManager(settings, resolutionManager);
        this.resolutionManager = resolutionManager;

        // ── 5. Conectar viewport al mouse ─────────────────────────────────────
        display.addResizeListener((realW, realH, viewport) ->
            mouse.setViewport(viewport)
        );

        // ── 6. Inicializar display ────────────────────────────────────────────
        display.init(keyboard, mouse, mouse, mouse, keyboard);

        // Viewport inicial
        mouse.setViewport(display.getViewport());

        // ── 7. Game State ─────────────────────────────────────────────────────
        gameState = new GameState(
            display.getVirtualWidth(),
            display.getVirtualHeight()
        );

        // ── 8. State Manager ──────────────────────────────────────────────────
        // ── HRFC-DT-003: StateManager gestiona estados del juego ──────────────
        stateManager = new StateManager(mouse);

        // Inyectar MouseInput en GameState para lifecycle (onEnter/onExit)
        gameState.setMouseInput(mouse);

        // Establecer GameState como estado inicial (activa onEnter)
        stateManager.setState(gameState);

        // Conectar cambios de resolución virtual al StateManager.
        // Si se envía DisplayCommand.ChangeResolution, StateManager propagará
        // el cambio al estado activo (GameState, MenuState, etc.).
        display.addVirtualResolutionListener((w, h) ->
            stateManager.onVirtualDimensionsChanged(w, h)
        );

        // ── 9. Listeners de teclado ───────────────────────────────────────────
        keyboard.addKeyActionListener((KeyActionListener) action -> {
            switch (action) {
                case "toggleFullscreen" ->
                    display.requestToggleFullscreen();
                case "toggleFps" ->
                    DebugGameSettings.getInstance().toggleFps();
                // ── Settings State con F4 (TOGGLE) ───────────────────────────
                case "openSettings" -> {
                    // F4 ahora es TOGGLE: abre O cierra el menú
                    if (stateManager.getActiveState() instanceof SettingsState) {
                        closeSettingsMenu();
                    } else {
                        openSettingsMenu();
                    }
                }
                // ── ESC también es TOGGLE para settings ───────────────────────
                case "pause" -> {
                    // ESC también abre/cierra el menú de settings (igual que F4)
                    if (stateManager.getActiveState() instanceof SettingsState) {
                        closeSettingsMenu();
                    } else if (stateManager.getActiveState() instanceof GameState) {
                        openSettingsMenu();
                    }
                    // En otros estados (IdleState), no hacer nada
                }
                // ── HRFC-DT-003: Prueba arquitectónica de cambio de estado ───
                case "toggleIdleState" -> {
                    // Demostración de transición entre estados
                    if (stateManager.getActiveState() instanceof GameState) {
                        // Cambiar a IdleState
                        IdleState idleState = new IdleState(
                            display.getVirtualWidth(),
                            display.getVirtualHeight()
                        );
                        stateManager.setState(idleState);
                    } else {
                        // Volver a GameState
                        stateManager.setState(gameState);
                    }
                }
            }
        });

        // ── 10. Game Loop ─────────────────────────────────────────────────────
        // ── HRFC-DT-003: GameLoop recibe StateManager, no GameState ──────────
        loop = new GameLoop(
            display.getRenderGateway(),
            stateManager,
            keyboard,
            mouse,
            settings.targetFps
        );

        loop.start();

        // ── 11. Shutdown al cerrar la ventana ─────────────────────────────────
        // addWindowCloseListener encapsula el registro dentro del subsistema
        // Display, sin exponer el JFrame hacia GameOrquester.
        // El callback para el GameLoop y para disposeWindow() son la
        // única comunicación necesaria con el Display en este punto.
        //
        // HRFC-004: Ejecutar shutdown en thread separado para evitar deadlock.
        // 
        // Problema:
        //   WindowAdapter.windowClosing() se ejecuta en el EDT.
        //   loop.stop() llama thread.join(5000), bloqueando el EDT.
        //   Si el GameLoop necesita que el EDT procese comandos (via invokeLater),
        //   ocurre deadlock: EDT esperando GameLoop, GameLoop esperando EDT.
        // 
        // Solución:
        //   Ejecutar stop() + disposeWindow() en thread dedicado de shutdown.
        //   El EDT retorna inmediatamente de windowClosing().
        //   El ShutdownThread ejecuta el protocolo completo sin bloquear el EDT.
        display.addWindowCloseListener(() -> {
            Thread shutdownThread = new Thread(() -> {
                stop();
                display.disposeWindow();
            }, "GameShutdownThread");
            shutdownThread.setDaemon(false); // Asegurar que la JVM espera a que termine
            shutdownThread.start();
        });
    }

    public void stop() {
        if (loop         != null) loop.stop();
        if (stateManager != null) stateManager.shutdown();
        
        // Liberar recursos de assets (audio e imágenes)
        Assets.shutdown();
    }
    
    // ── Settings Menu ─────────────────────────────────────────────────────
    
    private void openSettingsMenu() {
        SettingsState settingsState = new SettingsState(
            display,
            resolutionManager,
            keyBoard,
            mouseInput,
            this::closeSettingsMenu
        );
        
        stateManager.setState(settingsState);
    }
    
    private void closeSettingsMenu() {
        // Volver al GameState
        stateManager.setState(gameState);
    }
}
