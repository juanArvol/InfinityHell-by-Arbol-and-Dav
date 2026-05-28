package Main;

import Display.Managers.DisplayManager;
import Display.Settings.DisplaySettings;
import Display.Settings.ScalingMode;
import Entradas.KeyBoard;
import Entradas.MouseInput;
import Game.Settings.GameSettings;
import Graficos.Assets;
import States.GameState;

/**
 * Orquestador del juego.
 *
 * ─── CAMBIOS ──────────────────────────────────────────────────────────────────
 *
 * REGISTRO DE KeyBoard COMO FocusListener (BUG-06)
 *   KeyBoard ahora implementa FocusListener además de KeyListener.
 *   Para que el listener de foco funcione (limpiar teclas al perder foco),
 *   hay que registrar keyboard también como FocusListener en el Canvas.
 *
 *   ANTES: display.init(keyboard, mouse, mouse, mouse)
 *   AHORA: display.init(keyboard, mouse, mouse, mouse, keyboard)
 *
 * TOGGLE FPS COUNTER (NUEVO)
 *   Se registra un KeyActionListener adicional para la tecla F3 (o la que
 *   implementes en KeyActionListener.onToggleFps()) que alterna el contador
 *   de FPS en pantalla. El contador se controla desde GameSettings, por lo que
 *   puede activarse/desactivarse en cualquier momento sin tocar el GameLoop.
 *
 *   Si KeyActionListener no tiene aún el método onToggleFps(), añádelo
 *   como default method vacío para no romper implementaciones existentes:
 *
 *       default void onToggleFps() {}
 *
 *   Y en KeyBoard.update() dispara onToggleFps() cuando se pulsa F3.
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
            .uiScale(1.0f)
            .build();

        display = new DisplayManager(settings);

        // ── 4. Conectar viewport al mouse ─────────────────────────────────────
        display.addResizeListener((realW, realH, viewport) -> {
            mouse.setViewport(viewport);
        });

        // ── 5. Inicializar display ────────────────────────────────────────────
        //
        // BUG-06 FIX: keyboard se pasa también como FocusListener (5º parámetro).
        display.init(keyboard, mouse, mouse, mouse);

        // Viewport inicial
        mouse.setViewport(display.getViewport());

        // ── 6. Listeners de teclado ───────────────────────────────────────────
        keyboard.addKeyActionListener(new Entradas.Listeners.KeyActionListener() {

            @Override
            public void onToggleFullscreen() {
                display.toggleFullscreen();
            }

            /**
             * Toggle FPS counter (F3).
             * Activa o desactiva el contador de FPS en pantalla.
             * El estado se guarda en GameSettings; el GameLoop lo lee cada frame.
             *
             * Para que esto funcione debes:
             *   1. Añadir `default void onToggleFps() {}` en KeyActionListener.java
             *   2. Llamar listener.onToggleFps() en KeyBoard.update() cuando se pulse F3.
             */
            @Override
            public void onToggleFps() {
                GameSettings.getInstance().toggleFps();
            }
        });

        // ── 7. Game State ─────────────────────────────────────────────────────
        GameState state = new GameState(display);

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
