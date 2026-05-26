package Main;

import javax.swing.*;
import java.awt.*;

import Entradas.KeyBoard;
import Entradas.MouseInput;

public class GameWindow {

    private final JFrame frame;
    private final Canvas canvas;
    private boolean fullscreenState;

    public GameWindow(String title, int width, int height,
                      KeyBoard keyboard, MouseInput mouse,
                      boolean fullscreen) {

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame = new JFrame(title);

        this.fullscreenState = fullscreen;

        if (fullscreenState) {
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);
        } else {
            frame.setSize(width, height);
            frame.setLocationRelativeTo(null);
        }

        canvas = new Canvas();

        if (!fullscreen) {
            canvas.setPreferredSize(new Dimension(width, height));
            frame.pack();
        }

        canvas.setMinimumSize(new Dimension(width, height));
        canvas.setMaximumSize(new Dimension(screenSize.width, screenSize.height));

        canvas.addKeyListener(keyboard);
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        canvas.addMouseWheelListener(mouse);

        frame.add(canvas);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.setVisible(true);

        canvas.requestFocus();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void toggleFullscreen() {
        fullscreenState = !fullscreenState;

        frame.dispose();

        if (fullscreenState) {
            frame.setUndecorated(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            frame.setUndecorated(false);
            frame.setExtendedState(JFrame.NORMAL);
            frame.setSize(1200, 600);
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
        canvas.requestFocus();
    }
}