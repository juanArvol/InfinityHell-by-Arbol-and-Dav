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
 *
 * ─── BUGS CORREGIDOS ──────────────────────────────────────────────────────────
 *
 * BUG-FIRMA · addInputListeners() no aceptaba FocusListener
 *   CAUSA: GameOrquester llama display.init(..., keyboard) donde keyboard es
 *          FocusListener (para limpiar teclas al perder foco, BUG-06 fix).
 *          DisplayManager.init() pasa ese 5º parámetro a
 *          windowManager.addInputListeners(), pero el método solo tenía 4
 *          parámetros (sin FocusListener). El código no compilaba o el
 *          FocusListener nunca se registraba en el Canvas.
 *   SOLUCIÓN: añadir overload addInputListeners(..., FocusListener) que
 *             registra también el FocusListener en el Canvas.
 *   RIESGO: ninguno. Cambio aditivo, el overload de 4 params se mantiene
 *           para compatibilidad con código existente que no pase FocusListener.
 *   IMPACTO FUTURO 2D/3D: sin impacto. FocusListener es AWT puro.
 *
 * BUG-RESIZE-EDT · componentResized() notificaba listeners sin protección
 *   CAUSA: la lista resizeListeners se itera en EDT (componentResized),
 *          pero puede ser modificada concurrentemente si addResizeListener()
 *          se llama desde otro thread.
 *   SOLUCIÓN: cambiar List<ResizeListener> a CopyOnWriteArrayList.
 *             Iteración en EDT es segura; adición concurrente también.
 *   RIESGO: mínimo. CopyOnWriteArrayList tiene overhead en escritura pero
 *           resizeListeners rara vez se modifica en runtime.
 */
public class WindowManager {

    private final JFrame frame;
    private final Canvas canvas;
    private final DisplaySettings settings;
    private final ViewportManager viewportManager;

    /**
     * BUG-RESIZE-EDT FIX: CopyOnWriteArrayList para iteración segura en EDT
     * con posible adición concurrente desde otros threads.
     */
    private final List<ResizeListener> resizeListeners =
        new java.util.concurrent.CopyOnWriteArrayList<>();

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
     * Overload de compatibilidad sin FocusListener.
     */
    public void addInputListeners(KeyListener kl,
                                  MouseListener ml,
                                  MouseMotionListener mml,
                                  MouseWheelListener mwl) {
        addInputListeners(kl, ml, mml, mwl, null);
    }

    /**
     * BUG-FIRMA FIX: overload completo que acepta FocusListener adicional.
     *
     * El FocusListener (normalmente KeyBoard) se registra en el Canvas para
     * recibir notificación de pérdida de foco y limpiar las teclas presionadas.
     * Sin esto, las teclas quedan "pegadas" al hacer alt-tab o perder foco.
     *
     * @param kl  KeyListener (puede ser null)
     * @param ml  MouseListener (puede ser null)
     * @param mml MouseMotionListener (puede ser null)
     * @param mwl MouseWheelListener (puede ser null)
     * @param fl  FocusListener (puede ser null)
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
     * Detecta resize del Canvas y notifica al ViewportManager + listeners.
     *
     * ComponentListener.componentResized() se llama en el EDT — correcto.
     * La lista usa CopyOnWriteArrayList para iteración segura.
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
