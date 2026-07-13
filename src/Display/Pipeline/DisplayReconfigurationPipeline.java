package Display.Pipeline;

import Display.Backend.AwtWindowBackend;
import Display.Backend.DisplaySnapshot;
import Display.Backend.SnapshotValidator;
import Display.Background.DisplayBackground;
import Display.Commands.DisplayCommand;
import Display.Commands.DisplayCommandQueue;
import Display.Managers.FullscreenManager;
import Display.Managers.ViewportManager;
import Display.Managers.WindowManager;
import Display.State.DisplayMode;
import Display.State.DisplayState;
import Display.State.SurfaceState;
import Display.Surface.RenderSurface;
import Display.Surface.SurfaceBuilder;
import Display.Surface.SurfacePublisher;
import Display.Transition.DisplayTransitionMachine;
import Display.Transition.DisplayTransitionState;
import Display.ViewportInfo;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Pipeline unificado para toda reconfiguración del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-003: AWT COMO FUENTE DE VERDAD
 *
 * El Pipeline ya no es la autoridad sobre el estado del Display.
 * El Pipeline coordina transiciones y publica estados derivados de lo que
 * AWT confirma — nunca de lo que el Engine supuso que ocurriría.
 *
 * Cambio central respecto a HRFC-002:
 *
 *   Antes (HRFC-002):
 *     applyCommand() ejecuta la operación.
 *     El Pipeline asume que tuvo éxito.
 *     publishState() usa fullscreenManager.getCurrentMode() —
 *       que era un campo interno, no confirmado por AWT.
 *
 *   Ahora (HRFC-003):
 *     backend.requestXxx() ejecuta la solicitud.
 *     backend.readSnapshot() lee lo que AWT reporta realmente.
 *     SnapshotValidator.isUsable() verifica condiciones mínimas.
 *     Si la validación pasa, se construye la surface y se valida
 *     con isRenderReady().
 *     publishState() se construye enteramente desde el snapshot.
 *     La gate se abre solo si isRenderReady() pasa.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FASES DEL PIPELINE (todos los comandos excepto Resize/Suspend/Resume)
 *
 *   FASE 1  — tryBegin(transition)
 *   FASE 2  — closeGate() + publishTransientState(RECREATING)
 *   FASE 3  — suppressResize(true)
 *   FASE 4  — unpublish()
 *   FASE 5  — backend.requestXxx()              ← solicitud a AWT
 *   FASE 6  — snapshot = backend.readSnapshot() ← estado observado post-solicitud
 *   FASE 7  — SnapshotValidator.isUsable(snapshot) — ¿tiene sentido construir?
 *               Si falla: publishLost + scheduleRetry → return
 *   FASE 8  — viewportManager.onResize(snapshot dims)
 *   FASE 9  — buildAndPublish(snapshot)
 *   FASE 10 — snapshot2 = backend.readSnapshot() ← re-leer tras build
 *             SnapshotValidator.isRenderReady(snapshot2)
 *               Si pasa:  publishState(snapshot2, READY) + openGate()
 *               Si falla: publishState(snapshot2, LOST)  + scheduleRetry
 *   FASE 11 — suppressResize(false)             [finally]
 *   FASE 12 — transitionMachine.end()           [finally]
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

    private final AwtWindowBackend         backend;
    private final FullscreenManager        fullscreenManager;
    private final ViewportManager          viewportManager;
    private final SurfaceBuilder           surfaceBuilder;
    private final SurfacePublisher         surfacePublisher;
    private final WindowManager            windowManager;
    private final DisplayTransitionMachine transitionMachine;
    private final Consumer<DisplayState>   statePublisher;
    private final Runnable                 onBuildFailed;

    /** Fondo activo. EDT únicamente. */
    private DisplayBackground background;

    /** Estado canónico. Única fuente de verdad dentro del Pipeline. */
    private DisplayState currentState;

    public DisplayReconfigurationPipeline(
            AwtWindowBackend backend,
            FullscreenManager fullscreenManager,
            ViewportManager viewportManager,
            SurfaceBuilder surfaceBuilder,
            SurfacePublisher surfacePublisher,
            WindowManager windowManager,
            DisplayBackground background,
            DisplayTransitionMachine transitionMachine,
            DisplayState initialState,
            Consumer<DisplayState> statePublisher,
            Runnable onBuildFailed) {

        this.backend           = backend;
        this.fullscreenManager = fullscreenManager;
        this.viewportManager   = viewportManager;
        this.surfaceBuilder    = surfaceBuilder;
        this.surfacePublisher  = surfacePublisher;
        this.windowManager     = windowManager;
        this.background        = background;
        this.transitionMachine = transitionMachine;
        this.statePublisher    = statePublisher;
        this.onBuildFailed     = onBuildFailed != null ? onBuildFailed : () -> {};
        this.currentState      = initialState;
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    /**
     * Publica el estado post-init derivado del snapshot real y abre la gate.
     *
     * Llama readSnapshot() para confirmar el estado AWT antes de decidir si
     * la gate puede abrirse. No asume nada sobre lo que ocurrió en init().
     *
     * EDT únicamente.
     */
    public void initializeState() {
        assertEDT();
        DisplaySnapshot snapshot = backend.readSnapshot();

        // isBootstrapReady() verifica usabilidad + BS presente, pero NO contentsLost.
        // contentsLost() no tiene semántica definida antes del primer getDrawGraphics(),
        // por lo que no puede usarse para decidir si abrir la gate en el arranque.
        SnapshotValidator.ValidationResult ready =
            SnapshotValidator.isBootstrapReady(snapshot);

        if (ready.passed) {
            viewportManager.onResize(snapshot.canvasWidth(), snapshot.canvasHeight());
            publishStateFromSnapshot(snapshot, SurfaceState.READY);
            surfacePublisher.openGate();
            LOG.info("Pipeline: initialized from snapshot — gate opened. " + snapshot);
        } else {
            publishTransientState(SurfaceState.LOST);
            scheduleBuildRetry();
            LOG.warning("Pipeline: initializeState — isBootstrapReady failed: "
                        + ready.summary() + " — retry scheduled");
        }
    }

    // ── CommandExecutor ───────────────────────────────────────────────────────

    @Override
    public void execute(DisplayCommand command) {
        assertEDT();
        switch (command) {
            case DisplayCommand.ResizeCanvas rc    -> { executeResize(rc);  return; }
            case DisplayCommand.SuspendRendering s -> { executeSuspend(s);  return; }
            case DisplayCommand.ResumeRendering r  -> { executeResume(r);   return; }
            default -> {}
        }
        executeFullPipeline(command);
    }

    // ── SuspendRendering ──────────────────────────────────────────────────────

    private void executeSuspend(DisplayCommand.SuspendRendering cmd) {
        LOG.fine("Pipeline: SuspendRendering — closing gate");
        surfacePublisher.closeGate();
        publishTransientState(SurfaceState.SUSPENDED);
    }

    // ── ResumeRendering ───────────────────────────────────────────────────────

    private void executeResume(DisplayCommand.ResumeRendering cmd) {
        LOG.fine("Pipeline: ResumeRendering(rebuild=" + cmd.requiresRebuild() + ")");

        if (cmd.requiresRebuild()) {
            if (!transitionMachine.tryBegin(DisplayTransitionState.RECONFIGURING_DISPLAY)) {
                LOG.fine("Pipeline: ResumeRendering(rebuild=true) rejected — transition in progress");
                return;
            }
            // Cerrar gate ANTES de unpublish: evita la ventana donde
            // gate=open pero publishedRef=null (dispararía onRecoveryNeeded).
            surfacePublisher.closeGate();
            windowManager.suppressResize(true);
            try {
                surfacePublisher.unpublish();

                DisplaySnapshot snapshot = backend.readSnapshot();
                SnapshotValidator.ValidationResult usable =
                    SnapshotValidator.isUsable(snapshot);
                if (usable.failed()) {
                    LOG.warning("Pipeline: ResumeRendering — snapshot not usable: "
                                + usable.summary() + " — retry scheduled");
                    publishTransientState(SurfaceState.LOST);
                    scheduleBuildRetry();
                    return;
                }

                viewportManager.onResize(snapshot.canvasWidth(), snapshot.canvasHeight());
                boolean built = buildAndPublish();
                DisplaySnapshot snapshot2 = backend.readSnapshot();
                // isBootstrapReady(): BS recién creada, contentsLost no es fiable.
                SnapshotValidator.ValidationResult ready =
                    SnapshotValidator.isBootstrapReady(snapshot2);

                if (built && ready.passed) {
                    publishStateFromSnapshot(snapshot2, SurfaceState.READY);
                    surfacePublisher.openGate();
                    backend.requestCanvasFocus();
                    LOG.info("Pipeline: ResumeRendering(rebuild=true) — gate opened.");
                } else {
                    publishStateFromSnapshot(snapshot2, SurfaceState.LOST);
                    scheduleBuildRetry();
                    LOG.warning("Pipeline: ResumeRendering(rebuild=true) — not bootstrap-ready: "
                                + ready.summary());
                }
            } finally {
                windowManager.suppressResize(false);
                transitionMachine.end(DisplayTransitionState.RECONFIGURING_DISPLAY);
            }
            return;
        }

        // rebuild=false: verificar estado AWT y reabrir gate si el canvas es usable
        // y la BS existe (aunque tenga contentsLost transitorio — el do-while de
        // RenderFrame.present() lo maneja sin necesidad de rebuild completo).
        if (!surfacePublisher.hasPublishedSurface()) {
            LOG.fine("Pipeline: ResumeRendering(rebuild=false) — no surface, forcing rebuild");
            executeResume(new DisplayCommand.ResumeRendering(true));
            return;
        }

        DisplaySnapshot snapshot = backend.readSnapshot();

        // Verificar condiciones básicas de usabilidad (canvas displayable, visible, dims válidas).
        SnapshotValidator.ValidationResult usable = SnapshotValidator.isUsable(snapshot);
        if (usable.failed()) {
            LOG.fine("Pipeline: ResumeRendering(rebuild=false) — canvas not usable ("
                     + usable.summary() + "), escalating to rebuild");
            executeResume(new DisplayCommand.ResumeRendering(true));
            return;
        }

        // Verificar que la BS existe. Si no existe, necesitamos rebuild completo.
        if (!snapshot.bufferStrategyPresent()) {
            LOG.fine("Pipeline: ResumeRendering(rebuild=false) — no BufferStrategy, escalating to rebuild");
            executeResume(new DisplayCommand.ResumeRendering(true));
            return;
        }

        // La BS puede tener contentsLost transitorio — NO escalar a rebuild.
        // El loop do-while en RenderFrame.present() lo resolverá en el próximo frame.
        // Simplemente reabrir la gate; el GameLoop retomará el render.
        publishStateFromSnapshot(snapshot, SurfaceState.READY);
        surfacePublisher.openGate();
        backend.requestCanvasFocus();
        LOG.info("Pipeline: ResumeRendering(rebuild=false) — gate reopened"
                 + (snapshot.bufferStrategyContentsLost() ? " (contentsLost transient, will resolve in render loop)" : "")
                 + ".");
    }

    // ── ResizeCanvas ──────────────────────────────────────────────────────────

    private void executeResize(DisplayCommand.ResizeCanvas cmd) {
        int newW = cmd.width();
        int newH = cmd.height();

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

        surfacePublisher.closeGate();
        windowManager.suppressResize(true);
        try {
            boolean viewportChanged = viewportManager.onResize(newW, newH);
            if (!viewportChanged) {
                surfacePublisher.openGate();
                LOG.fine("Pipeline: ResizeCanvas — viewport unchanged, gate reopened");
                return;
            }

            surfacePublisher.unpublish();

            // Confirm AWT state before building.
            DisplaySnapshot snapshot = backend.readSnapshot();
            SnapshotValidator.ValidationResult usable = SnapshotValidator.isUsable(snapshot);
            if (usable.failed()) {
                publishStateFromSnapshot(snapshot, SurfaceState.LOST);
                scheduleBuildRetry();
                LOG.warning("Pipeline: ResizeCanvas — snapshot not usable: " + usable.summary());
                return;
            }

            boolean built = buildAndPublish();
            DisplaySnapshot snapshot2 = backend.readSnapshot();
            // isBootstrapReady(): no verifica contentsLost — BS recién creada.
            SnapshotValidator.ValidationResult ready = SnapshotValidator.isBootstrapReady(snapshot2);

            if (built && ready.passed) {
                publishStateFromSnapshot(snapshot2, SurfaceState.READY);
                surfacePublisher.openGate();
                LOG.fine("Pipeline: ResizeCanvas " + newW + "x" + newH + " — surface rebuilt, gate opened.");
            } else {
                publishStateFromSnapshot(snapshot2, SurfaceState.LOST);
                scheduleBuildRetry();
                LOG.warning("Pipeline: ResizeCanvas — not bootstrap-ready: " + ready.summary());
            }

        } catch (Exception e) {
            LOG.warning("Pipeline: exception during ResizeCanvas: " + e.getMessage());
            publishTransientState(SurfaceState.LOST);
            scheduleBuildRetry();
        } finally {
            windowManager.suppressResize(false);
            transitionMachine.end(DisplayTransitionState.RECONFIGURING_DISPLAY);
        }
    }

    // ── Pipeline completo ─────────────────────────────────────────────────────

    private void executeFullPipeline(DisplayCommand command) {
        DisplayTransitionState transition = resolveTransition(command);

        // FASE 1
        if (!transitionMachine.tryBegin(transition)) {
            LOG.fine("Pipeline: " + command.getClass().getSimpleName()
                     + " rejected — " + transitionMachine.getState() + " in progress");
            return;
        }

        // FASE 2
        surfacePublisher.closeGate();
        publishTransientState(SurfaceState.RECREATING);

        // FASE 3
        windowManager.suppressResize(true);

        try {
            // FASE 4
            surfacePublisher.unpublish();

            // FASE 5: solicitud a AWT
            applyRequest(command);

            // FASE 6: leer estado observado post-solicitud
            DisplaySnapshot snapshot = backend.readSnapshot();
            LOG.fine("Pipeline: post-request snapshot → " + snapshot);

            // FASE 7: validar usabilidad
            SnapshotValidator.ValidationResult usable = SnapshotValidator.isUsable(snapshot);
            if (usable.failed()) {
                LOG.warning("Pipeline: " + command.getClass().getSimpleName()
                            + " — snapshot not usable after request: " + usable.summary()
                            + " — attempting emergency path");
                // No hacer return inmediato: intentar recuperación de emergencia.
                attemptEmergencyRecovery(command, snapshot);
                return;
            }

            // FASE 8: recalcular viewport desde dimensiones confirmadas
            viewportManager.onResize(snapshot.canvasWidth(), snapshot.canvasHeight());

            // FASE 9: construir surface
            boolean built = buildAndPublish();

            // FASE 10: re-leer snapshot y validar bootstrap-readiness post-build.
            // isBootstrapReady() no verifica contentsLost — correcto aquí porque
            // la BS acaba de ser creada y nunca se llamó getDrawGraphics() todavía.
            DisplaySnapshot snapshot2 = backend.readSnapshot();
            SnapshotValidator.ValidationResult ready = SnapshotValidator.isBootstrapReady(snapshot2);

            if (built && ready.passed) {
                publishStateFromSnapshot(snapshot2, SurfaceState.READY);
                surfacePublisher.openGate();
                backend.requestCanvasFocus();
                LOG.info("Pipeline: completed " + command.getClass().getSimpleName()
                         + " → mode=" + snapshot2.confirmedMode());
            } else {
                publishStateFromSnapshot(snapshot2, SurfaceState.LOST);
                scheduleBuildRetry();
                LOG.warning("Pipeline: " + command.getClass().getSimpleName()
                            + " — not bootstrap-ready after build: " + ready.summary());
            }

        } catch (Exception e) {
            LOG.warning("Pipeline: exception during "
                        + command.getClass().getSimpleName() + ": " + e.getMessage());
            attemptEmergencyRecovery(command, backend.readSnapshot());

        } finally {
            // FASE 11 + 12: siempre
            windowManager.suppressResize(false);
            transitionMachine.end(transition);
        }
    }

    // ── Recuperación de emergencia ────────────────────────────────────────────

    /**
     * Cuando la transición principal falla, intenta construir una surface sobre
     * el estado real actual de AWT (sea cual sea). Si incluso eso falla,
     * publica LOST y programa un reintento.
     *
     * El estado publicado siempre proviene del snapshot leído en ese instante,
     * nunca de una suposición sobre lo que debería haber ocurrido.
     */
    private void attemptEmergencyRecovery(DisplayCommand failedCommand,
                                          DisplaySnapshot contextSnapshot) {
        LOG.warning("Pipeline: emergency recovery after failed "
                    + failedCommand.getClass().getSimpleName());
        try {
            DisplaySnapshot current = backend.readSnapshot();
            SnapshotValidator.ValidationResult usable = SnapshotValidator.isUsable(current);
            if (usable.failed()) {
                LOG.warning("Pipeline: emergency recovery — canvas not usable: "
                            + usable.summary() + " — scheduling retry");
                publishTransientState(SurfaceState.LOST);
                scheduleBuildRetry();
                return;
            }

            viewportManager.onResize(current.canvasWidth(), current.canvasHeight());
            boolean built = buildAndPublish();
            DisplaySnapshot snapshot2 = backend.readSnapshot();
            SnapshotValidator.ValidationResult ready = SnapshotValidator.isBootstrapReady(snapshot2);

            if (built && ready.passed) {
                publishStateFromSnapshot(snapshot2, SurfaceState.READY);
                surfacePublisher.openGate();
                LOG.info("Pipeline: emergency recovery succeeded.");
            } else {
                publishStateFromSnapshot(snapshot2, SurfaceState.LOST);
                scheduleBuildRetry();
                LOG.warning("Pipeline: emergency recovery also failed: " + ready.summary());
            }
        } catch (Exception e) {
            LOG.warning("Pipeline: emergency recovery threw: " + e.getMessage());
            publishTransientState(SurfaceState.LOST);
            scheduleBuildRetry();
        }
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    /**
     * Construye y publica una nueva RenderSurface.
     * Retorna true si la surface fue construida correctamente; false si build() devolvió null.
     */
    private boolean buildAndPublish() {
        ViewportInfo vp = viewportManager.getViewport();
        RenderSurface surface = surfaceBuilder.build(vp, background);
        if (surface == null) {
            LOG.warning("Pipeline: buildAndPublish() — surfaceBuilder.build() returned null");
            return false;
        }
        surfacePublisher.publish(surface);
        return true;
    }

    // ── FASE 5: solicitudes al Backend por tipo de comando ────────────────────

    private void applyRequest(DisplayCommand command) {
        switch (command) {
            case DisplayCommand.ToggleFullscreen ignored ->
                fullscreenManager.toggle();

            case DisplayCommand.EnterFullscreen cmd -> {
                if (cmd.targetMode() == DisplayMode.FULLSCREEN_EXCLUSIVE) {
                    fullscreenManager.enterFullscreen();
                } else {
                    fullscreenManager.enterBorderless();
                }
            }

            case DisplayCommand.ExitFullscreen ignored ->
                fullscreenManager.exitFullscreen();

            case DisplayCommand.SetDisplayMode cmd ->
                applySetDisplayMode(cmd.mode());

            case DisplayCommand.ChangeResolution cmd -> {
                surfaceBuilder.onVirtualResolutionChanged(
                    cmd.resolution().width, cmd.resolution().height);
                viewportManager.onVirtualResolutionChanged(
                    cmd.resolution().width, cmd.resolution().height);
                LOG.info("Pipeline: virtual resolution changed to " + cmd.resolution());
            }

            case DisplayCommand.ChangeMonitor cmd ->
                fullscreenManager.setMonitor(cmd.monitorIndex());

            case DisplayCommand.RestoreWindow ignored ->
                fullscreenManager.exitFullscreen();

            case DisplayCommand.RecreateBufferStrategy ignored ->
                LOG.fine("Pipeline: explicit surface rebuild (unpublish phase 4, rebuild phase 9)");

            case DisplayCommand.ChangeBackground cmd -> {
                this.background = cmd.background();
                LOG.info("Pipeline: background changed to " + cmd.background());
            }

            case DisplayCommand.ResizeCanvas ignored ->
                throw new IllegalStateException("ResizeCanvas should not reach applyRequest()");
            case DisplayCommand.SuspendRendering ignored ->
                throw new IllegalStateException("SuspendRendering should not reach applyRequest()");
            case DisplayCommand.ResumeRendering ignored ->
                throw new IllegalStateException("ResumeRendering should not reach applyRequest()");
        }
    }

    private void applySetDisplayMode(DisplayMode target) {
        DisplayMode current = fullscreenManager.getCurrentMode();
        if (current == target) return;
        switch (target) {
            case WINDOWED              -> fullscreenManager.exitFullscreen();
            case FULLSCREEN_EXCLUSIVE  -> {
                if (current.isFullscreen()) fullscreenManager.exitFullscreen();
                fullscreenManager.enterFullscreen();
            }
            case BORDERLESS_FULLSCREEN -> {
                if (current.isFullscreen()) fullscreenManager.exitFullscreen();
                fullscreenManager.enterBorderless();
            }
        }
    }

    // ── Publicación de DisplayState ───────────────────────────────────────────

    /**
     * Publica un DisplayState derivado enteramente del snapshot confirmado.
     * Ningún campo proviene de una suposición interna del Pipeline.
     */
    private void publishStateFromSnapshot(DisplaySnapshot snapshot, SurfaceState surfaceState) {
        ViewportInfo vp = viewportManager.getViewport();

        DisplayState next = currentState.toBuilder()
            .mode(snapshot.confirmedMode())
            .realSize(snapshot.canvasWidth(), snapshot.canvasHeight())
            .viewport(vp)
            .surfaceState(surfaceState)
            .transitionState(DisplayTransitionState.IDLE)
            .activeMonitorIndex(fullscreenManager.getActiveMonitorIndex())
            .build();

        currentState = next;
        statePublisher.accept(next);
    }

    /**
     * Publica un estado transitorio (RECREATING / SUSPENDED / LOST).
     * Los campos de modo y dimensiones mantienen el último valor confirmado.
     */
    private void publishTransientState(SurfaceState transientState) {
        DisplayTransitionState active = transitionMachine.getState();
        DisplayState transient_ = currentState.toBuilder()
            .surfaceState(transientState)
            .transitionState(active.isActive()
                ? active
                : DisplayTransitionState.RECONFIGURING_DISPLAY)
            .build();
        currentState = transient_;
        statePublisher.accept(transient_);
    }

    // ── Recovery ─────────────────────────────────────────────────────────────

    private void scheduleBuildRetry() {
        LOG.warning("Pipeline: scheduling build retry");
        try { onBuildFailed.run(); }
        catch (Exception e) {
            LOG.warning("Pipeline: onBuildFailed callback threw: " + e.getMessage());
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

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

            case DisplayCommand.ChangeBackground ignored ->
                DisplayTransitionState.RECONFIGURING_DISPLAY;

            case DisplayCommand.ResizeCanvas ignored ->
                throw new AssertionError("ResizeCanvas should not reach resolveTransition()");
            case DisplayCommand.SuspendRendering ignored ->
                throw new AssertionError("SuspendRendering should not reach resolveTransition()");
            case DisplayCommand.ResumeRendering ignored ->
                throw new AssertionError("ResumeRendering should not reach resolveTransition()");
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
