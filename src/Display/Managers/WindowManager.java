package Display.Managers;

import Display.Settings.DisplaySettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Responsable del ciclo de vida físico de la ventana.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIO: RESIZE COMO EVENTO, NO COMO EJECUCIÓN DIRECTA
 *
 * Problema anterior:
 *   El ComponentListener llamaba directamente al listener registrado, que
 *   ejecutaba destroyBS + createBS de forma síncrona en cada pixel de resize.
 *   Esto era costoso y podía crear un bucle si createBS disparaba otro
 *   componentResized desde el peer AWT nativo.
 *
 * Solución:
 *   El ComponentListener ya no ejecuta nada directamente.
 *   Notifica al CanvasResizeListener que a su vez ENCOLA un ResizeCanvas
 *   en la CommandQueue. La cola colapsa ráfagas de resize al último valor.
 *   Solo se procesa el tamaño final de una ráfaga de arrastre.
 *
 *   Además, el ComponentListener tiene ahora su propio guard de re-entrada:
 *   si el mismo tamaño se notifica dos veces consecutivas (puede ocurrir
 *   en algunas JVM durante validación de layout), la segunda se descarta.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   show(), dispose(), suppressResize() → EDT únicamente.
 *   getCanvas(), getFrame() → inmutables post-construcción → thread-safe.
 *   suppressResize → volatile; escrito y leído desde EDT.
 *   Los listeners de resize se invocan siempre desde el EDT.
 */
public final class WindowManager {

    private final JFrame frame;
    private final Canvas canvas;
    private final Dimension minimumSize;

    private final List<CanvasResizeListener> resizeListeners = new CopyOnWriteArrayList<>();

    private volatile boolean suppressResize = false;
    private boolean visible = false;

    /** Último tamaño notificado. Evita notificaciones duplicadas del mismo tamaño. */
    private int lastNotifiedWidth  = -1;
    private int lastNotifiedHeight = -1;

    public WindowManager(DisplaySettings settings) {
        this.minimumSize = settings.minimumWindowSize;
        this.frame  = buildFrame(settings);
        this.canvas = buildCanvas(settings);

        frame.add(canvas, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);

        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!visible || suppressResize) return;

                int w = canvas.getWidth();
                int h = canvas.getHeight();

                // Descartar dimensiones degeneradas o por debajo del mínimo
                if (w <= 0 || h <= 0) return;
                if (minimumSize != null
                        && (w < minimumSize.width || h < minimumSize.height)) {
                    return;
                }

                // GUARD DE RE-ENTRADA: descartar si el tamaño no cambió.
                // Esto evita notificaciones redundantes que ocurren en algunas JVM
                // cuando el LayoutManager re-valida sin cambiar las dimensiones.
                if (w == lastNotifiedWidth && h == lastNotifiedHeight) return;
                lastNotifiedWidth  = w;
                lastNotifiedHeight = h;

                for (CanvasResizeListener l : resizeListeners) {
                    l.onCanvasResized(w, h);
                }
            }
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void show() {
        frame.setVisible(true);
        visible = true;
        canvas.requestFocusInWindow();
    }

    public void dispose() {
        frame.dispose();
    }

    // ── Control de resize ─────────────────────────────────────────────────────

    /**
     * Habilita o deshabilita la propagación de eventos de resize.
     *
     * Llamar con true ANTES de una transición.
     * Llamar con false DESPUÉS de que la transición haya terminado.
     * EDT only.
     */
    public void suppressResize(boolean suppress) {
        this.suppressResize = suppress;
        if (!suppress) {
            // Al reanudar, resetear el último tamaño notificado para que el próximo
            // resize genuino siempre se propague, aunque coincida con el anterior.
            lastNotifiedWidth  = -1;
            lastNotifiedHeight = -1;
        }
    }

    // ── Input listeners ───────────────────────────────────────────────────────

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

    public void addCanvasResizeListener(CanvasResizeListener l)    { resizeListeners.add(l);    }
    public void removeCanvasResizeListener(CanvasResizeListener l) { resizeListeners.remove(l); }

    public void requestCanvasFocus() {
        SwingUtilities.invokeLater(canvas::requestFocusInWindow);
    }

    public JFrame getFrame()  { return frame;  }
    public Canvas getCanvas() { return canvas; }

    // ── Builders ──────────────────────────────────────────────────────────────

    private static JFrame buildFrame(DisplaySettings s) {
        JFrame f = new JFrame(s.windowTitle);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(s.windowResizable);
        if (!s.windowDecorated) f.setUndecorated(true);
        return f;
    }

    private static Canvas buildCanvas(DisplaySettings s) {
        Canvas c = new Canvas();
        c.setPreferredSize(new Dimension(s.windowedWidth, s.windowedHeight));
        c.setMinimumSize(s.minimumWindowSize);
        if (s.maximumWindowSize != null) c.setMaximumSize(s.maximumWindowSize);
        if (s.cursor != null) c.setCursor(s.cursor);
        c.setFocusable(true);
        c.setIgnoreRepaint(true);
        return c;
    }

    @FunctionalInterface
    public interface CanvasResizeListener {
        void onCanvasResized(int width, int height);
    }
}
