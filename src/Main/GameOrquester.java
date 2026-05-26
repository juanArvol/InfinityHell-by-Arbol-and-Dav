package Main;

import Display.Managers.DisplayManager;
import Display.Settings.DisplaySettings;
import Display.Settings.ScalingMode;
import Entradas.KeyBoard;
import Entradas.MouseInput;
import Graficos.Assets;
import States.GameState;

/**
 * Orquestador del juego — actualizado para el sistema de listeners de input.
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *  1. mouse.setViewport() se conecta al ResizeListener del display para
 *     que la transformación de coordenadas de mouse sea siempre correcta.
 *
 *  2. Los eventos edge de teclado (fullscreen, pausa, reload…) ya NO se
 *     leen como campos estáticos en GameLoop. Se suscriben directamente
 *     aquí (o en los sistemas que los necesitan) via KeyActionListener.
 *
 *  3. GameLoop ya NO necesita recibir keyboard para leer KeyBoard.f11.
 *     El toggle de fullscreen es ahora un listener en el orquestador.
 *
 *  4. mouse.flushEvents() se llama en GameLoop.update() al inicio de cada
 *     frame para despachar los eventos acumulados desde el EDT.
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

        // ── 4. Conectar viewport al mouse (transformación de coordenadas) ─────
        //
        // Cada vez que el canvas cambia de tamaño, el ViewportInfo nuevo
        // se propaga al MouseInput para que mouseVirtualX/Y sean siempre correctos.
        display.addResizeListener((realW, realH, viewport) -> {
            mouse.setViewport(viewport);
        });

        // Inicializar display (crea ventana y BufferStrategy)
        display.init(keyboard, mouse, mouse, mouse);

        // Viewport inicial (antes del primer resize por ComponentListener)
        mouse.setViewport(display.getViewport());

        // ── 5. Listeners de teclado (eventos edge desacoplados) ───────────────
        //
        // Toggle fullscreen: antes estaba hardcodeado en GameLoop leyendo
        // KeyBoard.f11 estático. Ahora es un listener explícito y declarativo.
        keyboard.addKeyActionListener(new Entradas.Listeners.KeyActionListener() {
            @Override
            public void onToggleFullscreen() {
                display.toggleFullscreen();
            }
            // onPause(), onReload(), etc. se pueden agregar aquí o en
            // los sistemas que los necesiten (PlayerCombat, UIManager…)
        });

        // ── 6. Game State ─────────────────────────────────────────────────────
        GameState state = new GameState(display);

        // ── 7. Game Loop ──────────────────────────────────────────────────────
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
