package Main;

import Display.Background.SolidColorBackground;
import Display.Managers.DisplayManager;
import Display.Settings.DisplaySettings;
import Display.Settings.ScalingMode;
import Graficos.Assets;
import Inputs.KeyBoard;
import Inputs.MouseInput;
import Inputs.Listeners.KeyActionListener;
import Main.Debug.DebugGameSettings;
import Main.States.GameState;

import java.awt.Color;

/**
 * Orquestador del juego.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIO RESPECTO A LA VERSIÓN ANTERIOR
 *
 * El GameLoop ya no recibe DisplayManager como dependencia.
 * Recibe RenderGateway, que es la única interfaz que necesita para render.
 *
 * Esto completa la separación de responsabilidades:
 *   - DisplayManager: gestiona el ciclo de vida del Display (solo EDT).
 *   - RenderGateway: punto de acceso del GameLoop a superficies publicadas.
 *   - GameLoop: consume frames a través de RenderGateway sin conocer nada más.
 *
 * La línea de cambio en este archivo es mínima (paso 8):
 *   antes: new GameLoop(display, state, ...)
 *   ahora: new GameLoop(display.getRenderGateway(), state, ...)
 * ──────────────────────────────────────────────────────────────────────────
 */
public class GameOrquester {

    private DisplayManager display;
    private GameLoop       loop;

    public void start() {

        // ── 1. Assets ─────────────────────────────────────────────────────────
        Assets.init();

        // ── 2. Input ──────────────────────────────────────────────────────────
        KeyBoard   keyboard = new KeyBoard();
        MouseInput mouse    = new MouseInput();

        // ── 3. Display ────────────────────────────────────────────────────────
        DisplaySettings settings = DisplaySettings.builder()
            .virtualResolution(1280, 720)
            .windowTitle("Infinity Hell")
            .windowedSize(1280, 720)
            .startFullscreen(true)
            .monitorIndex(0)
            .scalingMode(ScalingMode.FIT)
            .useInterpolation(false)
            .targetFps(30)
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
        GameState state = new GameState(
            display.getVirtualWidth(),
            display.getVirtualHeight()
        );

        // ── 8. Game Loop ──────────────────────────────────────────────────────
        // El GameLoop recibe RenderGateway, no DisplayManager.
        // No tiene conocimiento del ciclo de vida gráfico; solo adquiere frames.
        loop = new GameLoop(
            display.getRenderGateway(),   // único punto de acceso al display
            state,
            keyboard,
            mouse,
            settings.targetFps
        );

        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }
}
