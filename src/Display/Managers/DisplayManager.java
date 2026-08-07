package Display.Managers;

import Display.Backend.AwtWindowBackend;
import Display.Backend.DisplaySnapshot;
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
import java.awt.event.FocusListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Fachada del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-003: ENSAMBLAJE CON AwtWindowBackend
 *
 * DisplayManager construye y conecta todos los componentes del subsistema:
 *
 *   AwtWindowBackend  — único punto de contacto con AWT.
 *   FullscreenManager — coordina transiciones de modo (delega en Backend).
 *   WindowManager     — registra listeners AWT, convierte en comandos.
 *   ViewportManager   — calcula viewport.
 *   SurfaceBuilder    — construye RenderSurface (BS via Backend).
 *   SurfacePublisher  — publica surfaces al GameLoop via RenderGateway.
 *   Pipeline          — coordina transiciones, deriva estado del snapshot.
 *
 * getSnapshot() expone el último DisplaySnapshot leído por el Backend.
 * Cualquier componente autorizado puede consultar el estado observado
 * del Display sin pasar por el Pipeline.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   init()                    → thread principal; usa invokeAndWait.
 *   enqueue(command)          → cualquier thread.
 *   requestToggleFullscreen() → cualquier thread.
 *   getRenderGateway()        → cualquier thread (referencia inmutable).
 *   getSnapshot()             → thread-safe (volatile en Backend).
 *   getState()                → volatile read; thread-safe.
 *   addResizeListener()       → thread-safe (CopyOnWriteArrayList).
 */
public final class DisplayManager {

    private static final Logger LOG =
        Logger.getLogger(DisplayManager.class.getName());

    private final DisplaySettings              settings;
    private final AwtWindowBackend             backend;
    private final FullscreenManager            fullscreenManager;
    private final WindowManager                windowManager;
    private final ViewportManager              viewportManager;
    private final SurfaceBuilder               surfaceBuilder;
    private final SurfacePublisher             surfacePublisher;
    private final DisplayTransitionMachine     transitionMachine = new DisplayTransitionMachine();
    private final DisplayCommandQueue          commandQueue      = new DisplayCommandQueue();
    private final DisplayReconfigurationPipeline pipeline;

    private final int virtualWidth;
    private final int virtualHeight;

    private volatile DisplayState currentState;

    /**
     * True una vez que disposeWindow() ha sido invocado.
     *
     * Protege enqueue() para que ningún comando nuevo sea encolado después
     * de que el shutdown definitivo haya comenzado. Esto impide que retries
     * del pipeline (RecreateBufferStrategy) o callbacks de recuperación del
     * SurfacePublisher (onContentLost, onRecoveryNeeded) puedan iniciar un
     * ciclo infinito de reintentos sobre un Canvas cuyo peer ya fue destruido
     * por backend.dispose().
     *
     * Volatile: enqueue() puede ser llamado desde cualquier thread; la
     * escritura en disposeWindow() debe ser visible inmediatamente.
     */
    private volatile boolean disposed = false;

    private final List<ResizeListener> resizeListeners = new CopyOnWriteArrayList<>();

    public DisplayManager(DisplaySettings settings) {
        this.settings     = settings;
        this.virtualWidth  = settings.virtualWidth;
        this.virtualHeight = settings.virtualHeight;

        // ── Backend (sole AWT contact) ─────────────────────────────────────
        backend = new AwtWindowBackend(settings);

        // ── Managers ───────────────────────────────────────────────────────
        fullscreenManager = new FullscreenManager(backend, settings.monitorIndex);

        windowManager = new WindowManager(backend, settings);

        viewportManager = new ViewportManager(
            settings.virtualWidth,
            settings.virtualHeight,
            settings.scalingMode,
            settings.fillColor
        );

        // ── Surface layer ──────────────────────────────────────────────────
        surfaceBuilder = new SurfaceBuilder(
            backend,
            settings.bufferCount,
            settings.virtualWidth,
            settings.virtualHeight
        );

        surfacePublisher = new SurfacePublisher(
            settings.scalingMode,
            settings.useInterpolation,
            () -> enqueue(new DisplayCommand.RecreateBufferStrategy()),
            () -> enqueue(new DisplayCommand.RecreateBufferStrategy())
        );

        // ── Initial state (LOST until init() confirms otherwise) ──────────
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

        // ── Pipeline ───────────────────────────────────────────────────────
        pipeline = new DisplayReconfigurationPipeline(
            backend,
            fullscreenManager,
            viewportManager,
            surfaceBuilder,
            surfacePublisher,
            windowManager,
            settings.background,
            transitionMachine,
            currentState,
            state -> {
                currentState = state;
                notifyResizeListenersIfNeeded(state);
            },
            () -> enqueue(new DisplayCommand.RecreateBufferStrategy()),
            this::notifyVirtualResolutionChanged
        );

        // ── Window event wiring ────────────────────────────────────────────
        windowManager.addCanvasResizeListener((w, h) ->
            enqueue(new DisplayCommand.ResizeCanvas(w, h))
        );

        windowManager.addWindowLifecycleListener(
            new WindowManager.WindowLifecycleListener() {
                @Override
                public void onWindowSuspended() {
                    enqueue(new DisplayCommand.SuspendRendering());
                }
                @Override
                public void onWindowResumed(boolean requiresRebuild) {
                    enqueue(new DisplayCommand.ResumeRendering(requiresRebuild));
                }
            }
        );
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    /**
     * Inicializa el subsistema Display.
     *
     * ── Protocolo de dos fases ────────────────────────────────────────────
     *
     * FASE A — invokeAndWait (hilo principal bloquea hasta que EDT termina):
     *   1. Construir JFrame + Canvas.
     *   2. Registrar listeners AWT e input.
     *   3. Mostrar ventana en modo WINDOWED.
     *   4. Construir surface inicial sobre el canvas windowed.
     *   5. Publicar estado inicial y abrir gate.
     *
     * FASE B — si startFullscreen=true:
     *   Encolar EnterFullscreen como comando normal.
     *   El pipeline lo procesará en el siguiente ciclo del EDT,
     *   siguiendo exactamente el mismo protocolo que cualquier transición
     *   posterior: solicitud → readSnapshot → isBootstrapReady → build → openGate.
     *
     * ── Por qué no se hace fullscreen dentro de invokeAndWait ─────────────
     *
     * La transición fullscreen (especialmente FULLSCREEN_EXCLUSIVE) requiere
     * que el OS complete la negociación del device antes de que la BS sea
     * útil para presentar. Esa negociación es asíncrona respecto al EDT:
     * frame.validate() retorna antes de que el compositor la complete.
     *
     * Si la BS se construye en el mismo ciclo EDT que la solicitud fullscreen,
     * bs.show() escribe sobre un buffer que el compositor aún no está usando.
     * El primer frame no aparece hasta que un evento posterior (resize, etc.)
     * fuerza una nueva transición con el pipeline ya estable.
     *
     * Encolando EnterFullscreen como comando, el pipeline se ejecuta en el
     * siguiente ciclo del EDT — cuando el OS ya completó la transición — y
     * el protocolo completo (snapshot → validate → build → gate) garantiza
     * que la BS se construye sobre un canvas completamente sincronizado.
     *
     * El GameLoop adquiere frames normalmente durante la transición (gate
     * cerrada → drops silenciosos). La pantalla se activa cuando el pipeline
     * completa la transición y abre la gate, igual que en cualquier F11
     * posterior.
     */
    public void init(java.awt.event.KeyListener kl,
                     java.awt.event.MouseListener ml,
                     java.awt.event.MouseMotionListener mml,
                     java.awt.event.MouseWheelListener mwl,
                     FocusListener fl) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                // 1. Construir la ventana física.
                backend.init();

                // 2. Registrar listeners AWT.
                windowManager.registerListeners();
                windowManager.addInputListeners(kl, ml, mml, mwl, fl);

                // 3. Suprimir resize durante init.
                windowManager.suppressResize(true);

                // 4. Mostrar la ventana en modo WINDOWED.
                //    Independientemente de startFullscreen, la ventana siempre
                //    aparece primero en windowed: el canvas tiene dimensiones
                //    conocidas, el peer AWT está completamente sincronizado y
                //    la BS funciona desde el primer frame.
                backend.show();
                windowManager.onWindowShown();

                // 5. Construir la surface inicial sobre el canvas windowed.
                DisplaySnapshot initSnapshot = backend.readSnapshot();
                if (initSnapshot.hasValidDimensions()) {
                    viewportManager.onResize(
                        initSnapshot.canvasWidth(), initSnapshot.canvasHeight());
                }
                var surface = surfaceBuilder.build(
                    viewportManager.getViewport(), settings.background);
                surfacePublisher.publish(surface);

                // 6. Publicar estado inicial y abrir gate.
                pipeline.initializeState();

                windowManager.suppressResize(false);
                backend.requestCanvasFocus();
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException("DisplayManager.init() failed", e.getCause());
        }

        // FASE B: si se solicitó arranque en fullscreen, encolarlo ahora como
        // comando normal. El pipeline lo ejecutará en el EDT cuando esté libre,
        // con el protocolo completo: snapshot → isBootstrapReady → build → gate.
        // No hay caso especial: es exactamente el mismo camino que un F11.
        // El Backend es la única autoridad sobre qué modo fullscreen usar.
        if (settings.startFullscreen) {
            enqueue(new DisplayCommand.EnterFullscreen(
                backend.getPreferredFullscreenMode()
            ));
        }
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void addWindowCloseListener(Runnable onClose) {
        if (onClose == null) throw new IllegalArgumentException("onClose cannot be null");
        backend.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try { onClose.run(); }
                catch (Exception ex) {
                    LOG.warning("DisplayManager: windowClosing callback threw: " + ex.getMessage());
                }
            }
        });
    }

    public void disposeWindow() {
        disposed = true;
        if (SwingUtilities.isEventDispatchThread()) {
            backend.dispose();
        } else {
            SwingUtilities.invokeLater(backend::dispose);
        }
    }

    // ── API de render ─────────────────────────────────────────────────────────

    public RenderGateway getRenderGateway() { return surfacePublisher; }

    // ── API de comandos ───────────────────────────────────────────────────────

    void drainCommands() { commandQueue.drainToEDT(pipeline); }

    public void enqueue(DisplayCommand command) {
        if (disposed) {
            LOG.fine("DisplayManager: ignoring command after dispose: "
                     + command.getClass().getSimpleName());
            return;
        }
        commandQueue.enqueue(command);
        SwingUtilities.invokeLater(this::drainCommands);
    }

    public void requestToggleFullscreen() {
        enqueue(new DisplayCommand.ToggleFullscreen());
    }

    // ── Background ────────────────────────────────────────────────────────────

    public void setBackground(DisplayBackground bg) {
        if (bg == null) throw new IllegalArgumentException("background cannot be null");
        enqueue(new DisplayCommand.ChangeBackground(bg));
    }

    // ── Estado y snapshot ─────────────────────────────────────────────────────

    /** Último DisplayState publicado por el pipeline. Thread-safe (volatile). */
    public DisplayState getState() { return currentState; }

    /**
     * Último DisplaySnapshot leído por el Backend.
     *
     * Representa el estado real observado de AWT en el momento de la última
     * lectura. Thread-safe (volatile en Backend). Puede ser null antes de
     * que init() complete su primera lectura.
     *
     * Cualquier módulo autorizado puede consultar este snapshot directamente
     * sin pasar por el Pipeline ni por una transición activa.
     */
    public DisplaySnapshot getSnapshot() { return backend.getLastSnapshot(); }

    public ViewportInfo getViewport()   { return viewportManager.getViewport(); }
    public int getVirtualWidth()        { return virtualWidth;  }
    public int getVirtualHeight()       { return virtualHeight; }
    public boolean isFullscreen()       { return fullscreenManager.isFullscreen(); }
    public DisplayMode getMode()        { return fullscreenManager.getCurrentMode(); }

    /**
     * @deprecated Preferir {@link #addWindowCloseListener(Runnable)}.
     */
    @Deprecated(since = "hrfc-001", forRemoval = false)
    public JFrame getFrame() { return backend.getFrame(); }

    // ── Resize listeners ──────────────────────────────────────────────────────

    public void addResizeListener(ResizeListener l)    { resizeListeners.add(l);    }
    public void removeResizeListener(ResizeListener l) { resizeListeners.remove(l); }

    // ── Virtual resolution listeners ──────────────────────────────────────────

    /**
     * Listener para cambios de resolución virtual (DisplayCommand.ChangeResolution).
     *
     * A diferencia de ResizeListener (que notifica cambios del canvas real),
     * este listener se invoca únicamente cuando el framebuffer virtual cambia
     * de tamaño. Los sistemas que dependen de las dimensiones virtuales
     * (GameCamera, WorldManager, GameState) deben registrarse aquí.
     *
     * Se invoca desde el EDT, justo después de que el pipeline aplica el cambio.
     */
    @FunctionalInterface
    public interface VirtualResolutionListener {
        void onVirtualResolutionChanged(int newVirtualWidth, int newVirtualHeight);
    }

    private final List<VirtualResolutionListener> virtualResolutionListeners =
        new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addVirtualResolutionListener(VirtualResolutionListener l) {
        virtualResolutionListeners.add(l);
    }

    public void removeVirtualResolutionListener(VirtualResolutionListener l) {
        virtualResolutionListeners.remove(l);
    }

    /**
     * Invocado por el pipeline cuando se procesa un ChangeResolution.
     * Notifica a todos los listeners registrados de la nueva resolución virtual.
     * EDT únicamente.
     */
    public void notifyVirtualResolutionChanged(int newW, int newH) {
        for (VirtualResolutionListener l : virtualResolutionListeners) {
            try { l.onVirtualResolutionChanged(newW, newH); }
            catch (Exception e) {
                LOG.warning("VirtualResolutionListener threw: " + e.getMessage());
            }
        }
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void notifyResizeListenersIfNeeded(DisplayState state) {
        if (state.viewport == null || resizeListeners.isEmpty()) return;
        for (ResizeListener l : resizeListeners) {
            try { l.onResize(state.realWidth, state.realHeight, state.viewport); }
            catch (Exception e) {
                LOG.warning("ResizeListener threw: " + e.getMessage());
            }
        }
    }
}
