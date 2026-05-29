package Main;

import Display.Managers.DisplayManager;
import Display.Settings.DisplaySettings;
import Display.Settings.ScalingMode;
import Entradas.KeyBoard;
import Entradas.Listeners.KeyActionListener;
import Entradas.MouseInput;
import Game.Settings.GameSettings;
import Graficos.Assets;
import States.GameState;

/**
 * Orquestador del juego.
 *
 * ─── CAMBIOS (refactor Entradas v2) ───────────────────────────────────────────
 *
 *  · KeyActionListener ya NO tiene métodos por tecla (onToggleFullscreen,
 *    onToggleFps…). Ahora expone un único punto de entrada:
 *
 *        onKeyAction(String action)
 *
 *    donde "action" es el string declarado en KeyBinding.edgeAction.
 *
 *  · BUG-06 FIX: keyboard se pasa también como FocusListener (5º parámetro
 *    de display.init) para que focusLost() limpie las teclas al perder foco.
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
        display.init(keyboard, mouse, mouse, mouse, keyboard);

        // Viewport inicial
        mouse.setViewport(display.getViewport());

        // ── 6. Listeners de teclado ───────────────────────────────────────────
        //
        // onKeyAction recibe el edgeAction declarado en KeyBoard.BINDINGS.
        // Añadir soporte para una tecla nueva = nuevo case aquí, sin tocar
        // KeyActionListener ni KeyBoard.
        keyboard.addKeyActionListener((KeyActionListener) action -> {
            switch (action) {
                // toggleFullscreen(keyboard): despacha al EDT vía invokeLater() y,
                // una vez que el toggle + requestFocus terminan, llama
                // keyboard.clearFsTogglePending() desde el EDT.
                // NO llamar clearFsTogglePending() aquí: hacerlo desde el GameLoop
                // thread libera el guard antes de que el toggle haya terminado,
                // permitiendo que F11 dispare un segundo toggle y entre en loop
                // infinito de resize → crash.
                case "toggleFullscreen" ->
                    display.toggleFullscreen(keyboard);
                case "toggleFps" ->
                    GameSettings.getInstance().toggleFps();
                // "pause", "jump", "reload", etc. son gestionados por
                // otros suscriptores (p.ej. GameState, PlayerController).
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
