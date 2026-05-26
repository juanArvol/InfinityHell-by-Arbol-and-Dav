package Main;

import Entradas.KeyBoard;
import Entradas.MouseInput;
import Graficos.Assets;
import States.GameState;

import java.awt.Canvas;

public class GameOrquester {

    private GameWindow window;
    private GameLoop loop;

    public void start() {

        Assets.init();

        KeyBoard keyboard = new KeyBoard();
        MouseInput mouse = new MouseInput();

        window = new GameWindow(
                "Infinity Hell",
                1200,
                600,
                keyboard,
                mouse,
                true
        );

        Canvas canvas = window.getCanvas();

        // ❌ YA NO inicializamos WorldManager aquí
        // ❌ NO usamos canvas.getSize() todavía

        GameState state = new GameState(); // ← sin width/height

        loop = new GameLoop(
                canvas,
                state,
                keyboard,
                window,
                30
        );

        loop.start();
    }
}