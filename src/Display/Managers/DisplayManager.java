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
import Display.Surface.RenderGateway;
import Display.Surface.SurfaceBuilder;
import Display.Surface.SurfacePublisher;
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
 * CORRECCIÓN: ELIMINACIÓN DE publishFullState() Y DOBLE currentState
 *
 * Problema anterior:
 *   DisplayManager tenía su propio campo currentState y su propio método
 *   publishFullState() que construía DisplayState independientemente del
 *   pipeline. Esto creaba dos fuentes de verdad para el mismo dato:
 *
 *     - DisplayManager.currentState: construido en init() y por publishFullState()
 *     - DisplayReconfigurationPipeline.currentState: construido en publishState()
 *
 *   Los dos divergían desde el primer frame. Cualquier getState() que llamara
 *   código externo obtenía el de DisplayManager, que podía diferir del que
 *   usaba el pipeline como base para toBuilder() en la próxima transición.
 *
 * Solución:
 *   DisplayManager ya no tiene lógica de construcción de estado propia.
 *   currentState es simplemente el último valor publicado por el pipeline
 *   a través del statePublisher. El pipeline llama a pipeline.initializeState()
 *   al final de init() para publicar el estado correcto post-init.
 *   getState() retorna el valor más reciente publicado por el pipeline.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: init() COMPLETAMENTE ATÓMICO
 *
 * Problema anterior:
 *   init() usaba dos invokeAndWait separados. Entre ellos el EDT podía
 *   procesar eventos encolados durante show(), incluyendo componentResized.
 *   El segundo bloque podía recibir un resize antes de haber completado
 *   la inicialización.
 *
 * Solución:
 *   Todo el cuerpo de init() — show(), construcción inicial, publicación de
 *   superficie, publicación de estado — ocurre dentro de un único
 *   invokeAndWait, con suppressResize activo desde el inicio. Al final,
 *   suppressResize(false) se ejecuta dentro del mismo bloque atómico, justo
 *   después de que el estado es consistente.
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
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   init()                    → thread principal; usa invokeAndWait.
 *   enqueue(command)          → cualquier thread.
 *   requestToggleFullscreen() → cualquier thread.
 *   getRenderGateway()        → cualquier thread (referencia inmutable).
 *   addResizeListener()       → thread-safe (CopyOnWriteArrayList).
 *   getState()                → volatile read; thread-safe.
 */
public final class DisplayManager {

    private static final Logger LOG = Logger.getLogger(DisplayManager.class.getName());

    private final DisplaySettings              settings;
    private final WindowManager                windowManager;
    private final FullscreenManager            fullscreenManager;
    private final ViewportManager              viewportManager;
    private final SurfaceBuilder               surfaceBuilder;
    private final SurfacePublisher             surfacePublisher;
    private final DisplayTransitionMachine     transitionMachine  = new DisplayTransitionMachine();
    private final DisplayCommandQueue          commandQueue       = new DisplayCommandQueue();
    private       DisplayReconfigurationPipeline pipeline;

    private final int virtualWidth;
    private final int virtualHeight;

    /**
     * Último DisplayState publicado por el pipeline.
     * Solo se escribe desde el statePublisher (EDT). Volatile para lectura
     * thread-safe desde GameLoop u otros threads.
     */
    private volatile DisplayState currentState;

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

        surfaceBuilder = new SurfaceBuilder(
            windowManager.getCanvas(),
            settings.bufferCount,
            settings.virtualWidth,
            settings.virtualHeight
        );

        surfacePublisher = new SurfacePublisher(
            settings.scalingMode,
            settings.useInterpolation
        );

        // Estado inicial: SurfaceState.LOST hasta que init() publique el real.
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
            windowManager, fullscreenManager, viewportManager,
            surfaceBuilder, surfacePublisher, settings.background,
            transitionMachine, currentState,
            state -> {
                // statePublisher: actualiza currentState y notifica listeners.
                // Esta lambda se ejecuta en el EDT (desde publishState del pipeline).
                currentState = state;
                notifyResizeListenersIfNeeded(state);
            }
        );

        // El ComponentListener solo encola ResizeCanvas.
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

    /**
     * Inicializa el subsistema Display.
     *
     * Todo el proceso ocurre en un único invokeAndWait para garantizar
     * atomicidad: desde show() hasta la publicación del estado inicial.
     * suppressResize permanece activo durante todo el bloque y se libera
     * solo cuando el estado es completamente consistente.
     */
    public void init(java.awt.event.KeyListener kl,
                     java.awt.event.MouseListener ml,
                     java.awt.event.MouseMotionListener mml,
                     java.awt.event.MouseWheelListener mwl,
                     FocusListener fl) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                // suppressResize activo desde el primer momento.
                windowManager.suppressResize(true);
                windowManager.addInputListeners(kl, ml, mml, mwl, fl);
                windowManager.show();

                // Fullscreen inicial si está configurado.
                if (settings.startFullscreen) {
                    fullscreenManager.enterFullscreen(windowManager.getFrame());
                }

                // Construir y publicar la superficie inicial.
                // getPhysicalCanvasSize usa fallback a device bounds si el canvas
                // no reporta dimensiones válidas todavía.
                Dimension size = fullscreenManager.getPhysicalCanvasSize(windowManager.getCanvas());
                int w = size.width;
                int h = size.height;

                if (w > 0 && h > 0) {
                    viewportManager.onResize(w, h);
                }

                var surface = surfaceBuilder.build(viewportManager.getViewport(), settings.background);
                surfacePublisher.publish(surface);

                // Publicar el estado real post-init a través del pipeline.
                // Esto sincroniza pipeline.currentState con el estado real,
                // eliminando la divergencia que causaba snapshots incorrectos
                // en el primer ResizeCanvas tras el arranque.
                pipeline.initializeState();

                // suppressResize se libera DESPUÉS de que el estado es consistente.
                windowManager.suppressResize(false);
                windowManager.requestCanvasFocus();
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException("DisplayManager.init() failed", e.getCause());
        }
    }

    // ── API de render (solo para el GameLoop) ─────────────────────────────────

    /**
     * Retorna el gateway de render. La referencia es inmutable: puede
     * obtenerse una vez en construcción y reutilizarse durante toda la sesión.
     */
    public RenderGateway getRenderGateway() {
        return surfacePublisher;
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
     * Encola un comando. Thread-safe. El drain se programa automáticamente
     * en el EDT mediante invokeLater.
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

    /**
     * Cambia el fondo del framebuffer virtual.
     * El cambio se aplica en la próxima reconfiguración de superficie.
     */
    public void setBackground(DisplayBackground bg) {
        enqueue(new DisplayCommand.RecreateBufferStrategy());
    }

    // ── Estado y viewport ─────────────────────────────────────────────────────

    /** Estado publicado más reciente. Thread-safe (volatile read). */
    public DisplayState getState()      { return currentState; }
    /** Viewport calculado actual. Thread-safe (volatile en ViewportManager). */
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
