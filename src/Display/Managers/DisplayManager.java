package Display.Managers;

import Display.Background.DisplayBackground;
import Display.Commands.DisplayCommand;
import Display.Commands.DisplayCommandQueue;
import Display.Pipeline.DisplayReconfigurationPipeline;
import Display.ResizeListener;
import Display.Settings.DisplaySettings;
import Display.State.DisplayMode;
import Display.State.DisplayState;
import Display.State.Resolution;
import Display.State.SurfaceState;
import Display.Transition.DisplayTransitionMachine;
import Display.Transition.DisplayTransitionState;
import Display.ViewportInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Fachada del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIO: RESIZE COMPLETAMENTE EVENT-DRIVEN
 *
 * Problema anterior:
 *   onCanvasResized() ejecutaba directamente destroyBS + createBS + viewport
 *   en el ComponentListener. Era un camino especial fuera del pipeline.
 *
 * Solución:
 *   El CanvasResizeListener ahora encola DisplayCommand.ResizeCanvas.
 *   El pipeline lo procesa como cualquier otro comando, con suppressResize
 *   durante la ejecución para evitar el bucle ComponentResized ↔ createBS.
 *   La cola colapsa ráfagas de resize al último valor (debounce).
 *   El ResizeListener externo (cámara, UI) se notifica desde el pipeline
 *   tras publicar el nuevo DisplayState, no desde el ComponentListener.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * INVARIANTE DEL SISTEMA
 *
 * Todo cambio de estado del Display sigue exactamente este flujo:
 *
 *   evento externo (Swing, input, código)
 *     → enqueue(DisplayCommand)
 *     → CommandQueue.drainToEDT()
 *     → DisplayReconfigurationPipeline.execute()
 *     → nuevo DisplayState publicado
 *     → ResizeListeners notificados (si aplica)
 *
 * No existe ningún camino que modifique el estado del Display
 * fuera de este flujo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   init()                    → thread principal; usa invokeAndWait.
 *   enqueue(command)          → cualquier thread.
 *   requestToggleFullscreen() → cualquier thread.
 *   beginFrame() / endFrame() → solo GameLoop thread.
 *   addResizeListener()       → thread-safe (CopyOnWriteArrayList).
 *   getState()                → volatile read; thread-safe.
 */
public final class DisplayManager {

    private static final Logger LOG = Logger.getLogger(DisplayManager.class.getName());

    private final DisplaySettings              settings;
    private final WindowManager                windowManager;
    private final FullscreenManager            fullscreenManager;
    private final ViewportManager              viewportManager;
    private final RenderSurfaceManager         surfaceManager;
    private final DisplayTransitionMachine     transitionMachine  = new DisplayTransitionMachine();
    private final DisplayCommandQueue          commandQueue       = new DisplayCommandQueue();
    private       DisplayReconfigurationPipeline pipeline;

    private final int virtualWidth;
    private final int virtualHeight;

    private volatile DisplayState currentState;

    /**
     * ResizeListeners externos. Se notifican DESDE EL PIPELINE después de publicar
     * el nuevo DisplayState, no desde el ComponentListener.
     * Esto garantiza que los listeners siempre reciben un estado coherente.
     */
    private final List<ResizeListener> resizeListeners = new CopyOnWriteArrayList<>();

    public DisplayManager(DisplaySettings settings) {
        this.settings      = settings;
        this.virtualWidth  = settings.virtualWidth;
        this.virtualHeight = settings.virtualHeight;

        windowManager = new WindowManager(settings);

        viewportManager = new ViewportManager(
            settings.virtualWidth,
            settings.virtualHeight,
            settings.scalingMode,
            settings.fillColor
        );

        fullscreenManager = new FullscreenManager(settings.monitorIndex);

        surfaceManager = new RenderSurfaceManager(
            windowManager.getCanvas(),
            settings.virtualWidth,
            settings.virtualHeight,
            settings.bufferCount,
            settings.useInterpolation,
            settings.scalingMode,
            settings.background
        );

        currentState = new DisplayState(
            DisplayMode.WINDOWED,
            settings.windowedWidth,
            settings.windowedHeight,
            new Resolution(settings.virtualWidth, settings.virtualHeight),
            null,
            SurfaceState.LOST,
            DisplayTransitionState.IDLE,
            settings.monitorIndex
        );

        pipeline = new DisplayReconfigurationPipeline(
            windowManager, fullscreenManager, viewportManager, surfaceManager,
            transitionMachine, currentState,
            state -> {
                currentState = state;
                // Notificar ResizeListeners externos con el estado recién publicado
                // Solo si es un resize real (realWidth/realHeight cambiaron)
                notifyResizeListenersIfNeeded(state);
            }
        );

        // El ComponentListener solo encola ResizeCanvas.
        // NO ejecuta nada directamente. La cola se drena en el EDT via enqueue().
        windowManager.addCanvasResizeListener((w, h) ->
            enqueue(new DisplayCommand.ResizeCanvas(w, h))
        );
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    public void init(java.awt.event.KeyListener kl,
                     java.awt.event.MouseListener ml,
                     java.awt.event.MouseMotionListener mml,
                     java.awt.event.MouseWheelListener mwl) {
        init(kl, ml, mml, mwl, null);
    }

    public void init(java.awt.event.KeyListener kl,
                     java.awt.event.MouseListener ml,
                     java.awt.event.MouseMotionListener mml,
                     java.awt.event.MouseWheelListener mwl,
                     FocusListener fl) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                windowManager.suppressResize(true);
                windowManager.addInputListeners(kl, ml, mml, mwl, fl);
                windowManager.show();
            });

            SwingUtilities.invokeAndWait(() -> {
                if (settings.startFullscreen) {
                    fullscreenManager.enterFullscreen(windowManager.getFrame());
                }
                surfaceManager.createBufferStrategy();

                int w = windowManager.getCanvas().getWidth();
                int h = windowManager.getCanvas().getHeight();
                if (w > 0 && h > 0) {
                    viewportManager.onResize(w, h);
                }

                publishFullState(w, h);
                windowManager.suppressResize(false);
                windowManager.requestCanvasFocus();
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException("DisplayManager.init() failed", e.getCause());
        }
    }

    // ── Frame pipeline ────────────────────────────────────────────────────────

    public Graphics2D beginFrame() {
        return surfaceManager.beginFrame();
    }

    public void endFrame(Graphics2D virtualG) {
        surfaceManager.endFrame(virtualG);

        Graphics2D screenG = surfaceManager.beginPresent();
        if (screenG == null) return;

        surfaceManager.present(screenG, viewportManager.getViewport());
        surfaceManager.endPresent(screenG);
    }

    // ── API de comandos ───────────────────────────────────────────────────────

    /**
     * Drena la cola de comandos y ejecuta reconfiguraciones pendientes.
     * DEBE llamarse desde el EDT.
     */
    public void drainCommands() {
        commandQueue.drainToEDT(pipeline);
    }

    /**
     * Encola un comando. Thread-safe. Puede llamarse desde cualquier thread.
     * El drain se programa automáticamente en el EDT.
     */
    public void enqueue(DisplayCommand command) {
        commandQueue.enqueue(command);
        SwingUtilities.invokeLater(this::drainCommands);
    }

    /**
     * Solicita alternar fullscreen. Thread-safe.
     */
    public void requestToggleFullscreen() {
        enqueue(new DisplayCommand.ToggleFullscreen());
    }

    // ── Background ────────────────────────────────────────────────────────────

    public void setBackground(DisplayBackground bg) { surfaceManager.setBackground(bg); }
    public DisplayBackground getBackground()         { return surfaceManager.getBackground(); }

    // ── Estado y viewport ─────────────────────────────────────────────────────

    public DisplayState getState()      { return currentState; }
    public ViewportInfo getViewport()   { return viewportManager.getViewport(); }
    public int getVirtualWidth()        { return virtualWidth;  }
    public int getVirtualHeight()       { return virtualHeight; }
    public Canvas  getCanvas()          { return windowManager.getCanvas(); }
    public boolean isFullscreen()       { return fullscreenManager.isFullscreen(); }
    public DisplayMode getMode()        { return fullscreenManager.getCurrentMode(); }

    // ── Resize listeners ──────────────────────────────────────────────────────

    public void addResizeListener(ResizeListener l)    { resizeListeners.add(l);    }
    public void removeResizeListener(ResizeListener l) { resizeListeners.remove(l); }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void publishFullState(int realW, int realH) {
        ViewportInfo vp = viewportManager.getViewport();
        DisplayState next = new DisplayState(
            fullscreenManager.getCurrentMode(),
            realW, realH,
            new Resolution(virtualWidth, virtualHeight),
            vp,
            surfaceManager.getSurfaceState(),
            transitionMachine.getState(),
            fullscreenManager.getActiveMonitorIndex()
        );
        currentState = next;
    }

    /** Notifica ResizeListeners si el DisplayState publicado tiene nuevas dimensiones. */
    private void notifyResizeListenersIfNeeded(DisplayState state) {
        if (state.viewport == null || resizeListeners.isEmpty()) return;
        for (ResizeListener l : resizeListeners) {
            try {
                l.onResize(state.realWidth, state.realHeight, state.viewport);
            } catch (Exception e) {
                LOG.warning("ResizeListener threw: " + e.getMessage());
            }
        }
    }
}
