package Main;

import States.GameState;
import Entradas.KeyBoard;

import java.awt.*;
import java.awt.image.BufferStrategy;

public class GameLoop implements Runnable {

    private final Canvas canvas;
    private final GameState gameState;
    private final GameWindow window;
    private final KeyBoard keyboard;

    private double targetTime;

    private Thread thread;
    private boolean running = false;

    private int lastWidth;
    private int lastHeight;

    private int fpsPorSegundo = 0;

    public GameLoop(Canvas canvas,
                    GameState gameState,
                    KeyBoard keyboard,
                    GameWindow window,
                    int fps) {

        this.canvas = canvas;
        this.gameState = gameState;
        this.keyboard = keyboard;
        this.window = window;

        this.lastWidth = 0;
        this.lastHeight = 0;
        this.targetTime = 1_000_000_000.0 / fps;
    }

    public void start() {
        if (running) return;

        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running = false;

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        canvas.createBufferStrategy(3);
        BufferStrategy bs = canvas.getBufferStrategy();

        long lastTime = System.nanoTime();
        long timer = 0;
        double delta = 0;
        int frames = 0;

        while (running) {

            long now = System.nanoTime();
            delta += (now - lastTime) / targetTime;
            timer += (now - lastTime);
            lastTime = now;

            if (delta >= 1) {
                update();
                render(bs);
                delta--;
                frames++;
            }

            long frameTime = System.nanoTime() - now;
            long sleepTime = (long) (targetTime - frameTime) / 1_000_000;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (timer >= 1_000_000_000) {
                fpsPorSegundo = frames;
                frames = 0;
                timer = 0;
            }
        }
    }

    private void update() {
        keyboard.update();

        if (KeyBoard.f11) window.toggleFullscreen();

        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();

        if (screenWidth != lastWidth || screenHeight != lastHeight) {
            lastWidth = screenWidth;
            lastHeight = screenHeight;
        }

        gameState.update(screenWidth, screenHeight);
    }

    private void render(BufferStrategy bs) {

        if (bs == null) {
            canvas.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        try {
            g.setColor(Color.WHITE);
            g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            gameState.draw(g);

            g.setColor(Color.BLACK);
            g.drawString("FPS: " + fpsPorSegundo, 10, 15);

        } finally {
            g.dispose();
        }

        bs.show();
    }
}