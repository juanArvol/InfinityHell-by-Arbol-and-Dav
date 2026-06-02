package Display.Pipeline;

import Display.Background.DisplayBackground;
import Display.Commands.DisplayCommand;
import Display.Commands.DisplayCommandQueue;
import Display.Managers.FullscreenManager;
import Display.Managers.ViewportManager;
import Display.Managers.WindowManager;
import Display.State.DisplayMode;
import Display.State.DisplayState;
import Display.State.Resolution;
import Display.State.SurfaceState;
import Display.Surface.RenderSurface;
import Display.Surface.SurfaceBuilder;
import Display.Surface.SurfacePublisher;
import Display.Transition.DisplayTransitionMachine;
import Display.Transition.DisplayTransitionState;
import Display.ViewportInfo;

import java.awt.Dimension;
import java.awt.Window;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Pipeline unificado para toda reconfiguración del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: ÚNICA FUENTE DE VERDAD PARA currentState
 *
 * Problema anterior:
 *   DisplayManager tenía su propio campo currentState y su propio método
 *   publishFullState() que construía y publicaba un DisplayState
 *   independientemente del pipeline. El pipeline también tenía su propio
 *   currentState inicializado con el initialState del constructor.
 *   Cualquier llamada a publishState() en el pipeline usaba toBuilder()
 *   sobre ese initialState obsoleto, produciendo snapshots con datos del
 *   estado inicial mezclados con datos del estado actual.
 *
 *   DisplayManager.init() construía y publicaba la superficie fuera del
 *   pipeline, por lo que el pipeline.currentState nunca se enteraba del
 *   estado real post-init. El primer ResizeCanvas publicaba un DisplayState
 *   con viewport=null y surfaceState=LOST aunque la superficie ya existía.
 *
 * Solución:
 *   El pipeline es la ÚNICA fuente de currentState. DisplayManager no
 *   tiene lógica de construcción de estado propia; lee el último valor
 *   publicado por el statePublisher.
 *
 *   El método initializeState() permite al pipeline publicar el estado
 *   correcto post-init sin necesidad de ejecutar un comando completo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: DPI INDEPENDENCE EN LECTURA DE DIMENSIONES
 *
 *   El pipeline ya no llama canvas.getWidth() / canvas.getHeight()
 *   directamente. Usa fullscreenManager.getPhysicalCanvasSize(canvas),
 *   que aplica el fallback a device.getDefaultConfiguration().getBounds()
 *   si el canvas reporta dimensiones degeneradas. Esto garantiza que el
 *   viewport se calcula siempre sobre el tamaño físico real, independiente
 *   del DPI scaling del sistema operativo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FASES DEL PIPELINE (todas las reconfiguraciones excepto ResizeCanvas)
 *
 *   FASE 1 — Adquirir transición (TransitionMachine).
 *   FASE 2 — Suprimir resize espurios (WindowManager).
 *   FASE 3 — Publicar null (retirar superficie activa del GameLoop).
 *   FASE 4 — Ejecutar la operación específica del comando.
 *   FASE 5 — Recalcular viewport (con getPhysicalCanvasSize).
 *   FASE 6 — Construir nueva RenderSurface (SurfaceBuilder.build).
 *   FASE 7 — Publicar nueva RenderSurface (SurfacePublisher.publish).
 *   FASE 8 — Publicar nuevo DisplayState (única fuente de verdad).
 *   FASE 9 — Reanudar resize.
 *   FASE 10 — Liberar transición (siempre en finally).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   execute() / initializeState() → solo EDT.
 */
public final class DisplayReconfigurationPipeline
        implements DisplayCommandQueue.CommandExecutor {

    private static final Logger LOG =
        Logger.getLogger(DisplayReconfigurationPipeline.class.getName());

    private final WindowManager            windowManager;
    private final FullscreenManager        fullscreenManager;
    private final ViewportManager          viewportManager;
    private final SurfaceBuilder           surfaceBuilder;
    private final SurfacePublisher         surfacePublisher;
    private final DisplayTransitionMachine transitionMachine;
    private final DisplayBackground        background;
    private final Consumer<DisplayState>   statePublisher;

    /**
     * Estado canónico del subsistema Display.
     * Solo se modifica en publishState() / publishFailedState() / initializeState().
     * Es la única fuente de verdad; DisplayManager lee el valor publicado.
     */
    private DisplayState currentState;

    public DisplayReconfigurationPipeline(
            WindowManager windowManager,
            FullscreenManager fullscreenManager,
            ViewportManager viewportManager,
            SurfaceBuilder surfaceBuilder,
            SurfacePublisher surfacePublisher,
            DisplayBackground background,
            DisplayTransitionMachine transitionMachine,
            DisplayState initialState,
            Consumer<DisplayState> statePublisher) {

        this.windowManager     = windowManager;
        this.fullscreenManager = fullscreenManager;
        this.viewportManager   = viewportManager;
        this.surfaceBuilder    = surfaceBuilder;
        this.surfacePublisher  = surfacePublisher;
        this.background        = background;
        this.transitionMachine = transitionMachine;
        this.statePublisher    = statePublisher;
        this.currentState      = initialState;
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    /**
     * Publica el estado completo post-init sin ejecutar un comando completo.
     *
     * Llamar desde DisplayManager.init() después de que la superficie inicial
     * fue construida y publicada por SurfacePublisher. Esto garantiza que
     * el currentState del pipeline refleja el estado real del sistema desde
     * el primer frame, sin esperar al primer ResizeCanvas.
     *
     * EDT únicamente.
     */
    public void initializeState() {
        assertEDT();
        Dimension size = fullscreenManager.getPhysicalCanvasSize(windowManager.getCanvas());
        int w = size.width;
        int h = size.height;
        if (w > 0 && h > 0) {
            viewportManager.onResize(w, h);
        }
        publishState(w, h, DisplayTransitionState.IDLE);
        LOG.info("Pipeline: state initialized — " + currentState);
    }

    // ── CommandExecutor ───────────────────────────────────────────────────────

    @Override
    public void execute(DisplayCommand command) {
        assertEDT();

        if (command instanceof DisplayCommand.ResizeCanvas rc) {
            executeResize(rc);
            return;
        }

        DisplayTransitionState transition = resolveTransition(command);

        // FASE 1: Adquirir la transición
        if (!transitionMachine.tryBegin(transition)) {
            LOG.fine("Pipeline: " + command.getClass().getSimpleName()
                     + " rejected — " + transitionMachine.getState() + " in progress");
            return;
        }

        windowManager.suppressResize(true);
        try {
            // FASE 3: Retirar superficie activa
            surfacePublisher.unpublish();

            // FASE 4: Operación específica
            applyCommand(command);

            // FASE 5: Recalcular viewport con dimensiones físicas
            Dimension size = fullscreenManager.getPhysicalCanvasSize(windowManager.getCanvas());
            int w = size.width;
            int h = size.height;
            if (w > 0 && h > 0) {
                viewportManager.onResize(w, h);
            }

            // FASE 6 + 7: Construir y publicar nueva superficie
            buildAndPublish();

            // FASE 8: Publicar estado
            publishState(w, h, transition);

            windowManager.requestCanvasFocus();
            LOG.info("Pipeline: completed " + command.getClass().getSimpleName()
                     + " → " + fullscreenManager.getCurrentMode());

        } catch (Exception e) {
            LOG.warning("Pipeline: exception during " + command.getClass().getSimpleName()
                        + ": " + e.getMessage());
            publishFailedState();
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);

        } finally {
            // FASE 9: Reanudar resize
            windowManager.suppressResize(false);
            // FASE 10: Liberar transición SIEMPRE
            transitionMachine.end(transition);
        }
    }

    // ── ResizeCanvas: pipeline optimizado ─────────────────────────────────────

    /**
     * Ejecuta un ResizeCanvas con idempotencia y sin transición de modo.
     *
     * suppressResize durante la ejecución rompe el bucle:
     * createBS → componentResized → ResizeCanvas → createBS.
     */
    private void executeResize(DisplayCommand.ResizeCanvas cmd) {
        int newW = cmd.width();
        int newH = cmd.height();

        // Idempotencia: si el viewport ya refleja este tamaño, descartar.
        ViewportInfo existing = viewportManager.getViewport();
        if (existing != null
                && existing.realWidth  == newW
                && existing.realHeight == newH) {
            LOG.fine("Pipeline: ResizeCanvas " + newW + "x" + newH + " — same as current, discarded");
            return;
        }

        if (!transitionMachine.tryBegin(DisplayTransitionState.RECONFIGURING_DISPLAY)) {
            LOG.fine("Pipeline: ResizeCanvas rejected — transition in progress");
            return;
        }

        windowManager.suppressResize(true);
        try {
            boolean viewportChanged = viewportManager.onResize(newW, newH);

            if (viewportChanged) {
                surfacePublisher.unpublish();
                buildAndPublish();
                publishState(newW, newH, DisplayTransitionState.RECONFIGURING_DISPLAY);
                LOG.fine("Pipeline: ResizeCanvas " + newW + "x" + newH + " — surface rebuilt");
            } else {
                LOG.fine("Pipeline: ResizeCanvas " + newW + "x" + newH + " — viewport unchanged");
            }

        } catch (Exception e) {
            LOG.warning("Pipeline: exception during ResizeCanvas: " + e.getMessage());
            publishFailedState();

        } finally {
            windowManager.suppressResize(false);
            transitionMachine.end(DisplayTransitionState.RECONFIGURING_DISPLAY);
        }
    }

    // ── Construcción y publicación de superficie ──────────────────────────────

    private void buildAndPublish() {
        ViewportInfo viewport = viewportManager.getViewport();
        RenderSurface newSurface = surfaceBuilder.build(viewport, background);
        surfacePublisher.publish(newSurface);
    }

    // ── FASE 4: operación específica por tipo de comando ─────────────────────

    private void applyCommand(DisplayCommand command) {
        Window frame = windowManager.getFrame();

        switch (command) {
            case DisplayCommand.ToggleFullscreen ignored ->
                fullscreenManager.toggle(frame);

            case DisplayCommand.EnterFullscreen cmd -> {
                if (cmd.targetMode() == DisplayMode.FULLSCREEN_EXCLUSIVE) {
                    fullscreenManager.enterFullscreen(frame);
                } else {
                    fullscreenManager.enterBorderless(frame);
                }
            }

            case DisplayCommand.ExitFullscreen ignored ->
                fullscreenManager.exitFullscreen(frame);

            case DisplayCommand.SetDisplayMode cmd ->
                applySetDisplayMode(cmd.mode(), frame);

            case DisplayCommand.ChangeResolution cmd -> {
                surfaceBuilder.onVirtualResolutionChanged(
                    cmd.resolution().width, cmd.resolution().height);
                viewportManager.onVirtualResolutionChanged(
                    cmd.resolution().width, cmd.resolution().height);
                LOG.info("Pipeline: virtual resolution changed to " + cmd.resolution());
            }

            case DisplayCommand.ChangeMonitor cmd -> {
                fullscreenManager.setMonitor(cmd.monitorIndex());
                if (fullscreenManager.getCurrentMode().isFullscreen()) {
                    DisplayMode currentMode = fullscreenManager.getCurrentMode();
                    fullscreenManager.exitFullscreen(frame);
                    if (currentMode == DisplayMode.FULLSCREEN_EXCLUSIVE) {
                        fullscreenManager.enterFullscreen(frame);
                    } else {
                        fullscreenManager.enterBorderless(frame);
                    }
                }
            }

            case DisplayCommand.RestoreWindow ignored ->
                fullscreenManager.exitFullscreen(frame);

            case DisplayCommand.RecreateBufferStrategy ignored ->
                LOG.fine("Pipeline: explicit surface rebuild (unpublish in phase 3, rebuild in 6+7)");

            case DisplayCommand.ResizeCanvas ignored ->
                throw new IllegalStateException("ResizeCanvas should not reach applyCommand()");
        }
    }

    private void applySetDisplayMode(DisplayMode target, Window frame) {
        DisplayMode current = fullscreenManager.getCurrentMode();
        if (current == target) return;
        switch (target) {
            case DisplayMode.WINDOWED -> fullscreenManager.exitFullscreen(frame);
            case DisplayMode.FULLSCREEN_EXCLUSIVE -> {
                if (current.isFullscreen()) fullscreenManager.exitFullscreen(frame);
                fullscreenManager.enterFullscreen(frame);
            }
            case DisplayMode.BORDERLESS_FULLSCREEN -> {
                if (current.isFullscreen()) fullscreenManager.exitFullscreen(frame);
                fullscreenManager.enterBorderless(frame);
            }
        }
    }

    // ── Publicación de DisplayState ───────────────────────────────────────────

    private void publishState(int realW, int realH, DisplayTransitionState completedTransition) {
        ViewportInfo vp = viewportManager.getViewport();

        SurfaceState ss = surfacePublisher.hasPublishedSurface()
            ? SurfaceState.READY
            : SurfaceState.LOST;

        DisplayState next = currentState.toBuilder()
            .mode(fullscreenManager.getCurrentMode())
            .realSize(realW, realH)
            .viewport(vp)
            .surfaceState(ss)
            .transitionState(DisplayTransitionState.IDLE)
            .activeMonitorIndex(fullscreenManager.getActiveMonitorIndex())
            .build();

        currentState = next;
        statePublisher.accept(next);
    }

    private void publishFailedState() {
        DisplayState failed = currentState.toBuilder()
            .surfaceState(SurfaceState.FAILED)
            .transitionState(DisplayTransitionState.IDLE)
            .build();
        currentState = failed;
        statePublisher.accept(failed);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Estado canónico actual. EDT únicamente para coherencia con publishState. */
    public DisplayState getCurrentState() { return currentState; }

    // ── Resolución de tipo de transición ─────────────────────────────────────

    private DisplayTransitionState resolveTransition(DisplayCommand command) {
        return switch (command) {
            case DisplayCommand.ToggleFullscreen ignored ->
                fullscreenManager.isFullscreen()
                    ? transitionForLeave(fullscreenManager.getCurrentMode())
                    : DisplayTransitionState.ENTERING_FULLSCREEN;

            case DisplayCommand.EnterFullscreen cmd ->
                cmd.targetMode() == DisplayMode.FULLSCREEN_EXCLUSIVE
                    ? DisplayTransitionState.ENTERING_FULLSCREEN
                    : DisplayTransitionState.ENTERING_BORDERLESS;

            case DisplayCommand.ExitFullscreen ignored ->
                transitionForLeave(fullscreenManager.getCurrentMode());

            case DisplayCommand.SetDisplayMode cmd ->
                resolveSetModeTransition(cmd.mode());

            case DisplayCommand.ChangeResolution ignored ->
                DisplayTransitionState.CHANGING_RESOLUTION;

            case DisplayCommand.ChangeMonitor ignored ->
                DisplayTransitionState.CHANGING_MONITOR;

            case DisplayCommand.RestoreWindow ignored ->
                DisplayTransitionState.RECONFIGURING_DISPLAY;

            case DisplayCommand.RecreateBufferStrategy ignored ->
                DisplayTransitionState.RECONFIGURING_DISPLAY;

            case DisplayCommand.ResizeCanvas ignored ->
                DisplayTransitionState.RECONFIGURING_DISPLAY;
        };
    }

    private DisplayTransitionState transitionForLeave(DisplayMode mode) {
        return mode == DisplayMode.FULLSCREEN_EXCLUSIVE
            ? DisplayTransitionState.LEAVING_FULLSCREEN
            : DisplayTransitionState.LEAVING_BORDERLESS;
    }

    private DisplayTransitionState resolveSetModeTransition(DisplayMode target) {
        DisplayMode current = fullscreenManager.getCurrentMode();
        if (target == DisplayMode.WINDOWED)              return transitionForLeave(current);
        if (target == DisplayMode.FULLSCREEN_EXCLUSIVE)  return DisplayTransitionState.ENTERING_FULLSCREEN;
        if (target == DisplayMode.BORDERLESS_FULLSCREEN) return DisplayTransitionState.ENTERING_BORDERLESS;
        return DisplayTransitionState.RECONFIGURING_DISPLAY;
    }

    private static void assertEDT() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "DisplayReconfigurationPipeline must be called from the EDT");
        }
    }
}
