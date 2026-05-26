package Display.Managers;

import Display.Settings.DisplaySettings;
import Display.ResizeListener;
import Display.Managers.ViewportManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona el JFrame y Canvas del juego.
 *
 * RESPONSABILIDADES:
 *  - Crear y configurar JFrame y Canvas
 *  - Registrar listeners de input
 *  - Detectar resize y notificar al ViewportManager
 *  - NO gestionar fullscreen (eso es FullscreenManager)
 *  - NO gestionar BufferStrategy (eso es BufferStrategyManager)
 *
 * El Canvas NO cambia de tamaño lógico: el juego siempre es virtual.
 * Cuando el Canvas se redimensiona, solo se recalcula el viewport.
 */
public class WindowManager {

    private final JFrame frame;
    private final Canvas canvas;
    private final DisplaySettings settings;
    private final ViewportManager viewportManager;

    /** Listeners a notificar cuando el canvas cambie de tamaño. */
    private final List<ResizeListener> resizeListeners = new ArrayList<>();

    public WindowManager(DisplaySettings settings, ViewportManager viewportManager) {
        this.settings        = settings;
        this.viewportManager = viewportManager;

        frame  = buildFrame();
        canvas = buildCanvas();

        setupFrameContent();
        setupResizeDetection();
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    public JFrame  getFrame()  { return frame;  }
    public Canvas  getCanvas() { return canvas; }

    /** Registra un listener para cambios de tamaño del canvas. */
    public void addResizeListener(ResizeListener l) {
        resizeListeners.add(l);
    }

    public void removeResizeListener(ResizeListener l) {
        resizeListeners.remove(l);
    }

    /**
     * Registra keyboard y mouse en el canvas.
     * El canvas tiene el foco, por eso los listeners van en él.
     */
    public void addInputListeners(KeyListener kl,
                                  MouseListener ml,
                                  MouseMotionListener mml,
                                  MouseWheelListener mwl) {
        if (kl  != null) canvas.addKeyListener(kl);
        if (ml  != null) canvas.addMouseListener(ml);
        if (mml != null) canvas.addMouseMotionListener(mml);
        if (mwl != null) canvas.addMouseWheelListener(mwl);
    }

    /** Hace la ventana visible y solicita foco al canvas. */
    public void show() {
        frame.setVisible(true);
        canvas.requestFocusInWindow();
    }

    // ─── Construcción ─────────────────────────────────────────────────────────

    private JFrame buildFrame() {
        JFrame f = new JFrame(settings.windowTitle);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(true);
        // NO setUndecorated aquí — FullscreenManager lo gestiona
        return f;
    }

    private Canvas buildCanvas() {
        Canvas c = new Canvas();
        // El Canvas arranca con el tamaño de ventana windowed
        c.setPreferredSize(new Dimension(settings.windowedWidth, settings.windowedHeight));
        c.setMinimumSize(new Dimension(320, 180));
        // Sin cursor personalizado por defecto
        c.setFocusable(true);
        return c;
    }

    private void setupFrameContent() {
        frame.add(canvas, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /**
     * Detecta resize del Canvas y notifica al ViewportManager + listeners.
     *
     * ComponentListener.componentResized() es el hook correcto para esto.
     * NO detectar resize en el game loop (sucio y tardío).
     */
    private void setupResizeDetection() {
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = canvas.getWidth();
                int h = canvas.getHeight();

                if (w <= 0 || h <= 0) return;

                // 1. Actualizar viewport primero
                viewportManager.onResize(w, h);

                // 2. Notificar a todos los listeners (Game, UI, Camera, etc.)
                for (ResizeListener l : resizeListeners) {
                    l.onResize(w, h, viewportManager.getViewport());
                }
            }
        });
    }
}
