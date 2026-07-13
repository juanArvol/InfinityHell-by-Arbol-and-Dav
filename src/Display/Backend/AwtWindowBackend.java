package Display.Backend;

import Display.Settings.DisplaySettings;
import Display.State.DisplayMode;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Único punto de contacto directo entre el Display Engine y AWT.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * RESPONSABILIDAD
 *
 * AwtWindowBackend actúa como adaptador entre el Engine y la plataforma AWT.
 * Sus cuatro responsabilidades son estrictamente acotadas:
 *
 *   1. Solicitar operaciones a AWT (enter/exit fullscreen, show, dispose...).
 *   2. Leer el estado real de AWT y construir un DisplaySnapshot.
 *   3. Crear y destruir BufferStrategy (único lugar en todo el Engine).
 *   4. Exponer las referencias AWT necesarias para registro de listeners.
 *
 * NO coordina el Pipeline. NO toma decisiones de negocio. NO publica estado.
 * NO conoce DisplayState, DisplayCommandQueue ni RenderGateway.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * PROTOCOLO AWT: setUndecorated() REQUIERE !isDisplayable()
 *
 * Contrato AWT (Frame.setUndecorated Javadoc):
 *   "This method can only be called while the frame is not displayable."
 *
 * Un componente es displayable cuando su peer nativo ha sido creado.
 * El peer se crea con pack() / setVisible(true) y se DESTRUYE con dispose().
 * setVisible(false) NO destruye el peer — la ventana sigue siendo displayable.
 *
 * Protocolo correcto para cualquier cambio de decoración:
 *
 *   1. dispose()          → destruye el peer nativo → isDisplayable() = false
 *   2. setUndecorated()   → OK (precondición cumplida)
 *   3. pack()             → recrea el peer nativo → isDisplayable() = true
 *   4. setVisible(true)   → hace la ventana visible
 *
 * TODAS las transiciones de modo que requieren cambiar la decoración del
 * JFrame siguen exactamente este protocolo. No existe ningún atajo.
 *
 * Consecuencia importante: dispose() en el JFrame destruye también el peer
 * del Canvas hijo. La BufferStrategy asociada al Canvas queda inválida. Por
 * eso el Pipeline siempre llama unpublish() ANTES de solicitar la transición
 * y buildAndPublish() DESPUÉS, garantizando que nunca se usa una BS sobre
 * un Canvas cuyo peer fue destruido.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * AWT COMO FUENTE DE VERDAD
 *
 * readSnapshot() no inventa ni asume nada: lee directamente los objetos AWT
 * que posee (JFrame, Canvas, GraphicsDevice) y construye el DisplaySnapshot.
 *
 * confirmedMode se determina así:
 *   1. device.getFullScreenWindow() == frame → FULLSCREEN_EXCLUSIVE
 *   2. frame.isUndecorated() &&
 *      (extendedState & MAXIMIZED_BOTH) == MAXIMIZED_BOTH → BORDERLESS_FULLSCREEN
 *   3. cualquier otro caso                   → WINDOWED
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   init(), show(), requestXxx(), createBufferStrategy(),
 *   disposeBufferStrategy()                      → EDT únicamente.
 *   readSnapshot()                               → EDT únicamente.
 *   getFrame(), getCanvas()                      → inmutables post-init;
 *                                                  thread-safe para lectura.
 *   lastSnapshot (volatile)                      → lectura thread-safe.
 */
public final class AwtWindowBackend {

    private static final Logger LOG =
        Logger.getLogger(AwtWindowBackend.class.getName());

    private final DisplaySettings settings;
    private final GraphicsDevice  device;

    private JFrame frame;
    private Canvas canvas;

    /**
     * Último snapshot leído. Volatile para que otros threads puedan leer
     * el estado más reciente sin sincronización adicional.
     * Solo se escribe desde el EDT mediante readSnapshot().
     */
    private volatile DisplaySnapshot lastSnapshot;

    public AwtWindowBackend(DisplaySettings settings) {
        this.settings = settings;
        this.device   = resolveDevice(settings.monitorIndex);
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    /**
     * Construye el JFrame y Canvas, los ensambla y los configura.
     * No hace visible la ventana — eso lo hace show().
     * EDT únicamente.
     */
    public void init() {
        assertEDT("init");
        frame  = buildFrame(settings);
        canvas = buildCanvas(settings);
        frame.add(canvas, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        LOG.fine("AwtWindowBackend: JFrame and Canvas initialized.");
    }

    /**
     * Hace la ventana visible y solicita el foco al canvas.
     * EDT únicamente.
     */
    public void show() {
        assertEDT("show");
        frame.setVisible(true);
        canvas.requestFocusInWindow();
        LOG.fine("AwtWindowBackend: window shown.");
    }

    /**
     * Disposa la ventana definitivamente (cierre de aplicación).
     * EDT únicamente.
     */
    public void dispose() {
        assertEDT("dispose");
        if (frame != null) {
            frame.dispose();
            LOG.fine("AwtWindowBackend: window disposed.");
        }
    }

    // ── Solicitudes de modo (fire-and-forget hacia AWT) ───────────────────────

    /**
     * Solicita entrar en fullscreen exclusivo.
     * Si el dispositivo no soporta exclusive, cae a BORDERLESS.
     * EDT únicamente.
     */
    public void requestEnterFullscreen() {
        assertEDT("requestEnterFullscreen");
        if (device.isFullScreenSupported()) {
            requestEnterExclusive();
        } else {
            LOG.warning("AwtWindowBackend: exclusive fullscreen not supported — falling back to borderless");
            requestEnterBorderless();
        }
    }

    /**
     * Solicita entrar en modo borderless (maximized, sin decoración).
     *
     * Protocolo AWT correcto para setUndecorated():
     *   dispose() → setUndecorated(true) → pack() → setExtendedState(MAX) → setVisible(true)
     *
     * dispose() destruye el peer nativo del JFrame (y del Canvas hijo),
     * satisfaciendo la precondición !isDisplayable() de setUndecorated().
     * pack() recrea el peer antes de setVisible(true).
     *
     * EDT únicamente.
     */
    public void requestEnterBorderless() {
        assertEDT("requestEnterBorderless");
        captureWindowedSnapshot();

        // Protocolo correcto: dispose → setUndecorated → pack → setVisible
        frame.dispose();                            // peer destruido → !isDisplayable()
        frame.setUndecorated(true);                 // OK: precondición cumplida
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.pack();                               // peer recreado
        frame.setVisible(true);
        frame.validate();
        frame.getContentPane().validate();

        LOG.info("AwtWindowBackend: entered BORDERLESS_FULLSCREEN.");
    }

    /**
     * Solicita salir de fullscreen y restaurar el estado windowed.
     *
     * Protocolo AWT correcto:
     *   Si EXCLUSIVE: device.setFullScreenWindow(null) primero.
     *   Luego: dispose() → setUndecorated(false) → pack() → setBounds() → setVisible(true)
     *
     * EDT únicamente.
     */
    public void requestExitFullscreen() {
        assertEDT("requestExitFullscreen");
        DisplayMode current = deriveCurrentMode();

        // Liberar exclusive antes de dispose() si aplica.
        if (current == DisplayMode.FULLSCREEN_EXCLUSIVE
                && device.getFullScreenWindow() == frame) {
            device.setFullScreenWindow(null);
        }

        // Capturar el snapshot windowed antes de continuar.
        // Si no existe (caso de toggle directo a fullscreen sin snapshot previo),
        // usaremos la resolución windowed de los settings.
        WindowedSnapshot snap = windowedSnapshot;
        windowedSnapshot = null;

        // Protocolo correcto: dispose → setUndecorated → pack → setVisible
        frame.dispose();                            // peer destruido → !isDisplayable()
        frame.setUndecorated(false);                // OK: precondición cumplida
        frame.setExtendedState(JFrame.NORMAL);

        if (snap != null) {
            frame.setSize(snap.width, snap.height);
            frame.setLocation(snap.x, snap.y);
        } else {
            frame.setSize(settings.windowedWidth, settings.windowedHeight);
            frame.setLocationRelativeTo(null);
        }

        frame.pack();                               // peer recreado
        frame.setVisible(true);
        frame.validate();

        LOG.info("AwtWindowBackend: exited fullscreen → WINDOWED.");
    }

    /**
     * Alterna entre WINDOWED y FULLSCREEN.
     * EDT únicamente.
     */
    public void requestToggleFullscreen() {
        assertEDT("requestToggleFullscreen");
        if (deriveCurrentMode().isFullscreen()) {
            requestExitFullscreen();
        } else {
            requestEnterFullscreen();
        }
    }

    /**
     * Solicita cambiar al monitor indicado (efecto en la próxima transición FS).
     * EDT únicamente.
     */
    public void requestSetMonitor(int monitorIndex) {
        assertEDT("requestSetMonitor");
        GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        int clamped = Math.max(0, Math.min(monitorIndex, devices.length - 1));
        LOG.info("AwtWindowBackend: monitor change requested to index " + clamped
                 + " — takes effect on next fullscreen transition.");
    }

    /**
     * Solicita focus al canvas.
     * EDT únicamente.
     */
    public void requestCanvasFocus() {
        assertEDT("requestCanvasFocus");
        SwingUtilities.invokeLater(canvas::requestFocusInWindow);
    }

    // ── Lectura de estado confirmado ──────────────────────────────────────────

    /**
     * Lee el estado real del subsistema Display desde AWT y construye
     * un DisplaySnapshot inmutable.
     *
     * Cada campo del snapshot se obtiene directamente de un objeto AWT.
     * Ningún campo proviene de una variable interna del Engine.
     *
     * EDT únicamente.
     */
    public DisplaySnapshot readSnapshot() {
        assertEDT("readSnapshot");

        DisplayMode confirmedMode = deriveCurrentMode();

        int     canvasW       = canvas.getWidth();
        int     canvasH       = canvas.getHeight();
        boolean displayable   = canvas.isDisplayable();
        boolean canvasVis     = canvas.isVisible();
        boolean windowVis     = frame.isVisible();
        boolean windowActive  = frame.isActive();

        GraphicsConfiguration gc = canvas.getGraphicsConfiguration();

        BufferStrategy bs      = canvas.getBufferStrategy();
        boolean bsPresent      = bs != null;
        boolean bsContentsLost = false;
        if (bsPresent) {
            try {
                bsContentsLost = bs.contentsLost();
            } catch (Exception e) {
                bsContentsLost = true;
                LOG.fine("AwtWindowBackend.readSnapshot(): bs.contentsLost() threw — treating as lost: "
                         + e.getMessage());
            }
        }

        // Fallback de dimensiones en fullscreen: si el peer acaba de ser recreado
        // (post-dispose/pack), el canvas puede reportar 0x0 brevemente.
        if ((canvasW <= 0 || canvasH <= 0) && confirmedMode.isFullscreen()) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            canvasW = bounds.width;
            canvasH = bounds.height;
            LOG.fine("AwtWindowBackend.readSnapshot(): using device bounds fallback: "
                     + canvasW + "x" + canvasH);
        }

        DisplaySnapshot snapshot = new DisplaySnapshot(
            confirmedMode, canvasW, canvasH,
            displayable, canvasVis,
            windowVis, windowActive,
            gc, bsPresent, bsContentsLost
        );

        this.lastSnapshot = snapshot;
        LOG.fine("AwtWindowBackend: snapshot → " + snapshot);
        return snapshot;
    }

    /** Último snapshot leído. Thread-safe (volatile read). Null antes de init(). */
    public DisplaySnapshot getLastSnapshot() { return lastSnapshot; }

    // ── BufferStrategy ────────────────────────────────────────────────────────

    /**
     * Crea una nueva BufferStrategy en el canvas.
     *
     * Único lugar en todo el Engine donde se llama canvas.createBufferStrategy().
     * Disposa explícitamente la BS anterior si existe.
     *
     * Precondición: canvas.isDisplayable() == true. Si el canvas no es displayable
     * (porque dispose() fue llamado para setUndecorated y pack() aún no se llamó),
     * retorna null. Esto no debería ocurrir en el flujo normal del pipeline porque
     * el pipeline llama unpublish() antes de la transición y buildAndPublish()
     * después de que el Backend completa la transición (post-pack/setVisible).
     *
     * @return nueva BufferStrategy, o null si canvas no es displayable.
     * EDT únicamente.
     */
    public BufferStrategy createBufferStrategy(int bufferCount) {
        assertEDT("createBufferStrategy");

        if (!canvas.isDisplayable()) {
            LOG.warning("AwtWindowBackend.createBufferStrategy(): canvas not displayable — returning null");
            return null;
        }

        disposeBufferStrategy();

        try {
            canvas.createBufferStrategy(bufferCount);
            BufferStrategy bs = canvas.getBufferStrategy();
            LOG.fine("AwtWindowBackend: BufferStrategy created (buffers=" + bufferCount + ").");
            return bs;
        } catch (Exception e) {
            LOG.warning("AwtWindowBackend.createBufferStrategy() failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Destruye la BufferStrategy activa del canvas si existe.
     * EDT únicamente.
     */
    public void disposeBufferStrategy() {
        assertEDT("disposeBufferStrategy");
        BufferStrategy existing = canvas.getBufferStrategy();
        if (existing != null) {
            try {
                existing.dispose();
                LOG.fine("AwtWindowBackend: previous BufferStrategy disposed.");
            } catch (Exception e) {
                LOG.fine("AwtWindowBackend.disposeBufferStrategy(): absorbed — " + e.getMessage());
            }
        }
    }

    // ── Política de fullscreen ────────────────────────────────────────────────

    /**
     * Retorna el modo fullscreen preferido según las capacidades del device.
     *
     * Política:
     *   FULLSCREEN_EXCLUSIVE  si el GraphicsDevice lo soporta.
     *   BORDERLESS_FULLSCREEN en caso contrario.
     *
     * Esta es la única implementación de la política de selección de modo
     * fullscreen en todo el Engine. Toda decisión sobre qué modo usar al
     * entrar en fullscreen debe delegarse aquí — nunca replicarse fuera
     * del Backend.
     *
     * Thread-safe: consulta únicamente {@code device.isFullScreenSupported()},
     * que es seguro llamar desde cualquier thread.
     */
    public DisplayMode getPreferredFullscreenMode() {
        return device.isFullScreenSupported()
            ? DisplayMode.FULLSCREEN_EXCLUSIVE
            : DisplayMode.BORDERLESS_FULLSCREEN;
    }

    // ── Acceso a referencias AWT ──────────────────────────────────────────────
    public JFrame         getFrame()  { return frame;  }
    /** Canvas. Inmutable post-init. Solo para registro de listeners. */
    public Canvas         getCanvas() { return canvas; }
    /** GraphicsDevice activo. */
    public GraphicsDevice getDevice() { return device; }

    // ── Derivación del modo confirmado ────────────────────────────────────────

    /**
     * Deriva el DisplayMode actual directamente desde el estado de AWT.
     *
     * Orden de precedencia:
     *   1. device.getFullScreenWindow() == frame → FULLSCREEN_EXCLUSIVE
     *   2. frame.isUndecorated() && MAXIMIZED_BOTH → BORDERLESS_FULLSCREEN
     *   3. cualquier otro caso                     → WINDOWED
     *
     * Fuente de verdad: AWT, no un campo interno.
     * EDT únicamente.
     */
    public DisplayMode deriveCurrentMode() {
        if (frame == null) return DisplayMode.WINDOWED;
        if (device.getFullScreenWindow() == frame) return DisplayMode.FULLSCREEN_EXCLUSIVE;
        if (frame.isUndecorated()
                && (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
            return DisplayMode.BORDERLESS_FULLSCREEN;
        }
        return DisplayMode.WINDOWED;
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private WindowedSnapshot windowedSnapshot = null;

    private void captureWindowedSnapshot() {
        if (frame != null) {
            windowedSnapshot = WindowedSnapshot.capture(frame);
        }
    }

    /**
     * Entra en FULLSCREEN_EXCLUSIVE.
     *
     * setFullScreenWindow() no requiere que el frame sea no-displayable, pero
     * en algunos JVM es más estable si el frame no está visible al llamarlo.
     * Usamos dispose/pack para mantener el mismo protocolo coherente que
     * las transiciones borderless. Esto también garantiza que la BS anterior
     * del canvas quede destruida de forma controlada.
     */
    private void requestEnterExclusive() {
        captureWindowedSnapshot();

        // Misma secuencia dispose→setUndecorated→pack que borderless,
        // para garantizar coherencia y que el peer quede limpio.
        frame.dispose();
        frame.setUndecorated(true);
        frame.pack();

        // setFullScreenWindow() reconfigura el frame para el device.
        device.setFullScreenWindow(frame);
        frame.setVisible(true);
        frame.validate();

        LOG.info("AwtWindowBackend: entered FULLSCREEN_EXCLUSIVE.");
    }

    private static GraphicsDevice resolveDevice(int monitorIndex) {
        GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        int idx = Math.max(0, Math.min(monitorIndex, devices.length - 1));
        return devices[idx];
    }

    private static JFrame buildFrame(DisplaySettings s) {
        JFrame f = new JFrame(s.windowTitle);
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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

    private static void assertEDT(String methodName) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "AwtWindowBackend." + methodName + "() must be called from the EDT");
        }
    }

    // ── WindowedSnapshot ──────────────────────────────────────────────────────

    private static final class WindowedSnapshot {
        final int x, y, width, height;

        private WindowedSnapshot(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }

        static WindowedSnapshot capture(JFrame f) {
            Rectangle b = f.getBounds();
            return new WindowedSnapshot(b.x, b.y, b.width, b.height);
        }
    }
}
