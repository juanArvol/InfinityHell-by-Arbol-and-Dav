package Display.Pipeline;

import Display.Commands.DisplayCommand;
import Display.Commands.DisplayCommandQueue;
import Display.Managers.FullscreenManager;
import Display.Managers.RenderSurfaceManager;
import Display.Managers.ViewportManager;
import Display.Managers.WindowManager;
import Display.State.DisplayMode;
import Display.State.DisplayState;
import Display.State.Resolution;
import Display.State.SurfaceState;
import Display.Transition.DisplayTransitionMachine;
import Display.Transition.DisplayTransitionState;
import Display.ViewportInfo;

import java.awt.Window;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Pipeline unificado para toda reconfiguración del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIOS EN ESTA VERSIÓN
 *
 * 1. ResizeCanvas COMO COMANDO DE PRIMERA CLASE
 *    El resize del canvas (por arrastre del usuario) pasa ahora por este
 *    pipeline como DisplayCommand.ResizeCanvas.
 *
 *    Antes: onCanvasResized() en DisplayManager ejecutaba directamente
 *    viewportManager.onResize + destroyBS + createBS + publishState.
 *    Esto no pasaba por el TransitionMachine ni por suppressResize.
 *
 *    Ahora: ResizeCanvas sigue exactamente las mismas 9 fases que cualquier
 *    otro comando. suppressResize se activa durante la ejecución, lo que
 *    rompe el posible bucle: createBS → componentResized → onCanvasResized.
 *
 * 2. IDEMPOTENCIA EN RESIZE
 *    ResizeCanvas es idempotente: si las dimensiones son idénticas al
 *    estado actual del viewport (mismo realWidth × realHeight), el comando
 *    se descarta sin recalcular nada. No ocurre destroyBS ni createBS.
 *
 * 3. BS SOLO SE RECREA EN RECONFIGURACIONES REALES
 *    Para ResizeCanvas: si el viewport no cambió (idempotencia), NO se
 *    destruye/recrea el BufferStrategy. Solo se recrea si algo cambió.
 *    Esto elimina la recreación innecesaria de BS durante resizes iguales.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FASES DEL PIPELINE (sin cambios salvo la adición de ResizeCanvas)
 *
 *   FASE 1 — Adquirir transición (TransitionMachine).
 *   FASE 2 — Suprimir resize espurios (WindowManager).
 *   FASE 3 — Destruir BufferStrategy (si se requiere para este comando).
 *   FASE 4 — Ejecutar la operación específica del comando.
 *   FASE 5 — Recalcular viewport.
 *   FASE 6 — Recrear BufferStrategy (si se destruyó en FASE 3).
 *   FASE 7 — Publicar nuevo DisplayState.
 *   FASE 8 — Reanudar resize.
 *   FASE 9 — Liberar transición (siempre en finally).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   execute() → solo EDT.
 */
public final class DisplayReconfigurationPipeline
        implements DisplayCommandQueue.CommandExecutor {

    private static final Logger LOG =
        Logger.getLogger(DisplayReconfigurationPipeline.class.getName());

    private final WindowManager           windowManager;
    private final FullscreenManager       fullscreenManager;
    private final ViewportManager         viewportManager;
    private final RenderSurfaceManager    surfaceManager;
    private final DisplayTransitionMachine transitionMachine;

    private final Consumer<DisplayState> statePublisher;
    private volatile DisplayState currentState;

    public DisplayReconfigurationPipeline(
            WindowManager windowManager,
            FullscreenManager fullscreenManager,
            ViewportManager viewportManager,
            RenderSurfaceManager surfaceManager,
            DisplayTransitionMachine transitionMachine,
            DisplayState initialState,
            Consumer<DisplayState> statePublisher) {

        this.windowManager     = windowManager;
        this.fullscreenManager = fullscreenManager;
        this.viewportManager   = viewportManager;
        this.surfaceManager    = surfaceManager;
        this.transitionMachine = transitionMachine;
        this.statePublisher    = statePublisher;
        this.currentState      = initialState;
    }

    // ── CommandExecutor ───────────────────────────────────────────────────────

    @Override
    public void execute(DisplayCommand command) {
        assertEDT();

        // CASO ESPECIAL: ResizeCanvas tiene idempotencia propia.
        // Si las dimensiones no cambiaron, descartar sin transición.
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
            // FASE 3: Destruir BufferStrategy
            surfaceManager.destroyBufferStrategy();

            // FASE 4: Operación específica
            applyCommand(command);

            // FASE 5: Recalcular viewport con el nuevo tamaño real
            int w = windowManager.getCanvas().getWidth();
            int h = windowManager.getCanvas().getHeight();
            if (w > 0 && h > 0) {
                viewportManager.onResize(w, h);
            }

            // FASE 6: Recrear BufferStrategy
            surfaceManager.createBufferStrategy();

            // FASE 7: Publicar estado
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
            // FASE 8: Reanudar resize
            windowManager.suppressResize(false);
            // FASE 9: Liberar transición SIEMPRE
            transitionMachine.end(transition);
        }
    }

    // ── ResizeCanvas: pipeline optimizado ────────────────────────────────────

    /**
     * Ejecuta un ResizeCanvas con idempotencia y sin transición de modo.
     *
     * IDEMPOTENCIA: si las dimensiones son idénticas al viewport actual,
     * descarta sin recalcular ni recrear BS.
     *
     * BS SOLO SE RECREA SI EL VIEWPORT CAMBIÓ: evita destroy+create
     * innecesarios que podían generar eventos AWT adicionales.
     *
     * suppressResize durante la ejecución: rompe el bucle
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

        // Adquirir transición (RECONFIGURING_DISPLAY para resize)
        if (!transitionMachine.tryBegin(DisplayTransitionState.RECONFIGURING_DISPLAY)) {
            LOG.fine("Pipeline: ResizeCanvas rejected — transition in progress");
            return;
        }

        windowManager.suppressResize(true);
        try {
            // Recalcular viewport
            boolean viewportChanged = viewportManager.onResize(newW, newH);

            if (viewportChanged) {
                // Solo recrear BS si el viewport realmente cambió
                surfaceManager.destroyBufferStrategy();
                surfaceManager.createBufferStrategy();
                publishState(newW, newH, DisplayTransitionState.RECONFIGURING_DISPLAY);
                LOG.fine("Pipeline: ResizeCanvas " + newW + "x" + newH + " — viewport updated");
            } else {
                // No cambió: actualizar solo el estado (sin recrear BS)
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
                surfaceManager.onVirtualResolutionChanged(
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
                LOG.fine("Pipeline: explicit BufferStrategy recreation (destroy+create in phases 3+6)");

            // ResizeCanvas se maneja en executeResize(), nunca llega aquí
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

    // ── Publicación de estado ─────────────────────────────────────────────────

    private void publishState(int realW, int realH, DisplayTransitionState completedTransition) {
        ViewportInfo vp = viewportManager.getViewport();
        SurfaceState ss = surfaceManager.getSurfaceState();

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
                DisplayTransitionState.RECONFIGURING_DISPLAY; // manejado por executeResize
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
                "DisplayReconfigurationPipeline.execute() must be called from the EDT");
        }
    }
}
