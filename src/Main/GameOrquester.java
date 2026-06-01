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
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR
 *
 * 1. toggleFullscreen(keyboard) → requestToggleFullscreen()
 *    El método anterior recibía un KeyBoard que no usaba, y ejecutaba la
 *    transición vía invokeLater sin protección contra llamadas concurrentes.
 *    El nuevo método es seguro ante key repeat por diseño (TransitionLock).
 *    El KeyBoard ya no es necesario como parámetro.
 *
 * 2. display.toggleFullscreen() eliminado de la API pública.
 *    La única forma de solicitar un toggle es requestToggleFullscreen(),
 *    que garantiza:
 *      - Ejecución en el EDT.
 *      - Exclusión mutua entre transiciones.
 *      - Supresión de resize durante la transición.
 *      - Restauración de estado windowed correcta.
 *
 * 3. DisplaySettings sin cambios en el contrato básico.
 *    fillColor y background siguen siendo configurables independientemente.
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
            .background(SolidColorBackground.WHITE) // fondo transparente, sin limpiar (ideal para debugging)
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
                    // requestToggleFullscreen() es seguro desde cualquier thread:
                    // - Protegido por TransitionLock contra key repeat y pulsaciones rápidas.
                    // - Despacha al EDT automáticamente.
                    // - Gestiona supresión de resize, restauración de estado y BS.
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
        loop = new GameLoop(
            display,
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
