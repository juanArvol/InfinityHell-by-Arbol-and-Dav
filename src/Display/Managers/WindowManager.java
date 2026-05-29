package Display.Managers;

import Display.Settings.DisplaySettings;
import Display.ResizeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Gestiona el JFrame y Canvas del juego.
 *
 * RESPONSABILIDADES:
 *  - Crear y configurar JFrame y Canvas
 *  - Registrar listeners de input en el Canvas
 *  - Detectar resize del Canvas y notificar al ViewportManager + ResizeListeners
 *
 * NO gestiona:
 *  - Fullscreen (FullscreenManager)
 *  - BufferStrategy (BufferStrategyManager)
 *  - Supresión de resize durante toggle (DisplayManager vía ResizeListener guard)
 *
 * ─── SOBRE componentResized DURANTE TOGGLE ───────────────────────────────────
 *
 *  Durante un toggle fullscreen, el Canvas recibe 2-4 eventos componentResized
 *  del WM (por setVisible(false/true), setFullScreenWindow, setSize).
 *  WindowManager los propaga normalmente — es responsabilidad de los listeners
 *  (particularmente el que llama bsManager.recreate en DisplayManager) decidir
 *  si actuar o no según toggleInProgress.
 *
 *  WindowManager filtra dimensiones w=0 o h=0 (canvas aún sin peer o transitorio)
 *  pero NO filtra resizes "de toggle" — eso lo hace DisplayManager.
 *
 * ─── THREAD SAFETY ────────────────────────────────────────────────────────────
 *
 *  resizeListeners: CopyOnWriteArrayList → iteración segura en EDT con posible
 *  adición concurrente desde otros threads (p.ej. inicialización paralela).
 */
public class WindowManager {

    private final JFrame frame;
    private final Canvas canvas;
    private final DisplaySettings settings;
    private final ViewportManager viewportManager;

    private final List<ResizeListener> resizeListeners = new CopyOnWriteArrayList<>();

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

    public void addResizeListener(ResizeListener l)    { resizeListeners.add(l);    }
    public void removeResizeListener(ResizeListener l) { resizeListeners.remove(l); }

    /**
     * Registra listeners de input en el Canvas.
     * Overload de compatibilidad sin FocusListener.
     */
    public void addInputListeners(KeyListener kl,
                                  MouseListener ml,
                                  MouseMotionListener mml,
                                  MouseWheelListener mwl) {
        addInputListeners(kl, ml, mml, mwl, null);
    }

    /**
     * Registra todos los listeners de input en el Canvas.
     * Todos los parámetros son opcionales (null = ignorar).
     */
    public void addInputListeners(KeyListener kl,
                                  MouseListener ml,
                                  MouseMotionListener mml,
                                  MouseWheelListener mwl,
                                  FocusListener fl) {
        if (kl  != null) canvas.addKeyListener(kl);
        if (ml  != null) canvas.addMouseListener(ml);
        if (mml != null) canvas.addMouseMotionListener(mml);
        if (mwl != null) canvas.addMouseWheelListener(mwl);
        if (fl  != null) canvas.addFocusListener(fl);
    }

    /** Hace la ventana visible y solicita foco al Canvas. */
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
        c.setPreferredSize(new Dimension(settings.windowedWidth, settings.windowedHeight));
        c.setMinimumSize(new Dimension(320, 180));
        c.setFocusable(true);
        return c;
    }

    private void setupFrameContent() {
        frame.add(canvas, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /**
     * Instala el detector de resize del Canvas.
     *
     * componentResized() se llama siempre en el EDT.
     * Filtra dimensiones inválidas (w=0 o h=0) pero NO filtra resizes de toggle
     * — esa responsabilidad es de los ResizeListeners individuales (ver DisplayManager).
     */
    private void setupResizeDetection() {
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = canvas.getWidth();
                int h = canvas.getHeight();

                // Filtrar dimensiones inválidas (canvas sin peer o transitorio)
                if (w <= 0 || h <= 0) return;

                // 1. Actualizar viewport primero (siempre, independiente del toggle)
                viewportManager.onResize(w, h);

                // 2. Notificar a los listeners (cada uno decide si actuar)
                for (ResizeListener l : resizeListeners) {
                    l.onResize(w, h, viewportManager.getViewport());
                }
            }
        });
    }
}
