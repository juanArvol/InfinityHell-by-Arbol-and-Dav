package Main;

import Display.Background.SolidColorBackground;
import Display.Managers.DisplayManager;
import Display.Settings.DisplaySettings;
import Display.Settings.ScalingMode;
import Inputs.KeyBoard;
import Inputs.Listeners.KeyActionListener;
import Inputs.MouseInput;
import Main.Debug.DebugGameSettings;
import Main.States.GameState;
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
 *   - GameState:      estado del juego y gameplay.
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
 *   GameState.shutdown() (que para el bgExecutor con awaitTermination).
 *   Solo entonces se llama frame.dispose(), que cierra la ventana y
 *   permite que la JVM termine limpiamente.
 *
 *   El WindowAdapter se registra DESPUÉS de init() para evitar que se
 *   active durante la inicialización del Display.
 */
public class GameOrquester {

    private DisplayManager display;
    private GameLoop       loop;
    private GameState      state;

    public void start() {

        // ── 1. Assets ─────────────────────────────────────────────────────────
        Assets.init();

        // ── 2. Input ──────────────────────────────────────────────────────────
        KeyBoard   keyboard = new KeyBoard();
        MouseInput mouse    = new MouseInput();

        KeyBoard.setActiveInstance(keyboard);
        MouseInput.setActiveInstance(mouse);

        // ── 3. Display ────────────────────────────────────────────────────────
        DisplaySettings settings = DisplaySettings.builder()
            .virtualResolution(1280, 720)
            .windowTitle("Infinity Hell")
            .windowedSize(1280, 720)
            .startFullscreen(true)
            .monitorIndex(0)
            .scalingMode(ScalingMode.FIT)
            .useInterpolation(false)
            .targetFps(60)
            .fillColor(Color.BLACK)
            .background(SolidColorBackground.WHITE)
            .build();

        display = new DisplayManager(settings);

        // ── 4. Conectar viewport al mouse ─────────────────────────────────────
        display.addResizeListener((realW, realH, viewport) ->
            mouse.setViewport(viewport)
        );

        // ── 5. Inicializar display ────────────────────────────────────────────
        display.init(keyboard, mouse, mouse, mouse, keyboard);

        // Viewport inicial
        mouse.setViewport(display.getViewport());

        // ── 6. Listeners de teclado ───────────────────────────────────────────
        keyboard.addKeyActionListener((KeyActionListener) action -> {
            switch (action) {
                case "toggleFullscreen" ->
                    display.requestToggleFullscreen();
                case "toggleFps" ->
                    DebugGameSettings.getInstance().toggleFps();
            }
        });

        // ── 7. Game State ─────────────────────────────────────────────────────
        state = new GameState(
            display.getVirtualWidth(),
            display.getVirtualHeight()
        );

        // Conectar cambios de resolución virtual al GameState.
        // Si se envía DisplayCommand.ChangeResolution, GameState, WorldManager
        // y GameCamera se actualizan con las nuevas dimensiones virtuales.
        display.addVirtualResolutionListener((w, h) ->
            state.onVirtualDimensionsChanged(w, h)
        );

        // ── 8. Game Loop ──────────────────────────────────────────────────────
        state.registerMouseInput(mouse);

        loop = new GameLoop(
            display.getRenderGateway(),
            state,
            keyboard,
            mouse,
            settings.targetFps
        );

        loop.start();

        // ── 9. Shutdown al cerrar la ventana ──────────────────────────────────
        // addWindowCloseListener encapsula el registro dentro del subsistema
        // Display, sin exponer el JFrame hacia GameOrquester.
        // El callback para el GameLoop y para disposeWindow() son la
        // única comunicación necesaria con el Display en este punto.
        display.addWindowCloseListener(() -> {
            stop();
            display.disposeWindow();
        });
    }

    public void stop() {
        if (loop  != null) loop.stop();
        if (state != null) state.shutdown();
    }
}
