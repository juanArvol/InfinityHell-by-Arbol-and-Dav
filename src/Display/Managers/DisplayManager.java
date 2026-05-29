package Display.Managers;

import Display.ResizeListener;
import Display.Settings.DisplaySettings;
import Display.ViewportInfo;
import Entradas.KeyBoard;

import java.awt.*;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Fachada principal del sistema de display.
 *
 * Coordina todos los subsistemas:
 *   DisplayManager
 *    ├── WindowManager        — JFrame + Canvas + resize detection
 *    ├── FullscreenManager    — fullscreen real sin dispose()
 *    ├── ViewportManager      — cálculo de viewport/scale
 *    ├── ScalingManager       — presentación del framebuffer virtual
 *    └── RenderSurfaceManager — framebuffer virtual + BufferStrategy
 *
 * USO TÍPICO EN EL GAME LOOP:
 *
 *   // Inicio de frame:
 *   Graphics2D virtualG = displayManager.beginFrame();
 *   if (virtualG == null) return; // frame saltado (contentsLost recovery)
 *
 *   // Render del juego completo en coordenadas virtuales:
 *   gameState.draw(virtualG, displayManager.getViewport());
 *
 *   // Presentar a pantalla:
 *   displayManager.endFrame(virtualG);
 *
 * REGLA: el juego NO conoce la resolución real del monitor.
 *        Solo usa VIRTUAL_WIDTH / VIRTUAL_HEIGHT y ViewportInfo para input.
 *
 * ─── BUGS CORREGIDOS ──────────────────────────────────────────────────────────
 *
 * BUG-INIT-FIRMA · init() no aceptaba FocusListener (5º parámetro)
 *   CAUSA: GameOrquester llama display.init(keyboard, mouse, mouse, mouse, keyboard)
 *          con 5 argumentos. El método original solo tenía 4 parámetros,
 *          por lo que el código no compilaba o el FocusListener de teclado
 *          (necesario para limpiar teclas al perder foco) nunca se registraba.
 *   SOLUCIÓN: añadir overload init() con FocusListener como 5º parámetro opcional.
 *             El overload de 4 params delega al de 5 con null, manteniendo
 *             compatibilidad total con código existente.
 *   RIESGO: ninguno. Cambio aditivo.
 *
 * BUG-EDT-FULLSCREEN · toggleFullscreen() llamado desde GameLoop thread sin EDT
 *   CAUSA: el flujo es GameLoop thread → keyboard.update() → KeyActionListener
 *          → onToggleFullscreen() → display.toggleFullscreen() → fullscreenManager
 *          que llama setVisible/setUndecorated/setFullScreenWindow. Estas son
 *          operaciones de Swing/AWT que DEBEN ejecutarse en el EDT.
 *          Llamarlas desde el GameLoop thread puede causar:
 *            · DeadLock si el EDT está esperando un lock que el GameLoop tiene
 *            · Corrupcón del estado de la ventana
 *            · Crashes al modificar el peer nativo desde el thread equivocado
 *            · Pantalla negra al salir de fullscreen
 *   SOLUCIÓN: toggleFullscreen() envuelve la operación en SwingUtilities.invokeLater()
 *             para despacharla al EDT. La operación es asíncrona (se ejecuta en el
 *             siguiente ciclo del EDT), pero esto es correcto: el user no nota el
 *             delay de 1 frame, y las operaciones de ventana son inherentemente
 *             asíncronas de todas formas.
 *   RIESGO: mínimo. invokeLater() es el mecanismo estándar y seguro para esto.
 *           Si toggleFullscreen() ya se llama desde el EDT (caso raro),
 *           invokeLater() lo encola igualmente — no causa doble ejecución.
 *   NOTA: isFullscreen() puede devolver el estado "anterior" durante 1 frame
 *         mientras el EDT procesa el toggle. Esto es aceptable y es el
 *         comportamiento correcto para UI asíncrona.
 *
 * BUG-FOCUS-LOST-AFTER-TOGGLE · el Canvas pierde el foco tras el toggle y no se recupera
 *   CAUSA: FullscreenManager llama setVisible(false) durante el toggle, disparando
 *          focusLost() en KeyBoard que limpia rawKeys. Al volver a setVisible(true),
 *          nadie llama requestFocusInWindow() → el teclado queda muerto.
 *   SOLUCIÓN: toggleFullscreen(keyboard) recibe el KeyBoard y tras completar el toggle
 *             llama requestFocusInWindow() en un invokeLater() anidado (para que el WM
 *             haya terminado de procesar setVisible antes de pedir el foco), y luego
 *             llama keyboard.clearFsTogglePending() para liberar el guard anti-doble-toggle.
 *
 * BUG-F11-DOBLE-TOGGLE · F11 puede dispararse dos veces durante el toggle
 *   CAUSA: durante el toggle, setVisible(false) → focusLost() limpia rawKeys[F11].
 *          En hardware lento (o Windows con DWM), update() corre varios frames durante
 *          el toggle. En esos frames rawKeys[F11]=false → lastKeys[F11] se pone false.
 *          Cuando el foco regresa, el OS re-envía keyPressed(F11) si la tecla sigue
 *          pulsada → rawKeys[F11]=true → edge detectado → SEGUNDO onToggleFullscreen().
 *   SOLUCIÓN: flag fsTogglePending en KeyBoard, activado al detectar el primer edge
 *             de F11 y desactivado desde el EDT cuando el toggle + requestFocus terminan.
 *             Mientras está activo, el edge de F11 se suprime.
 *
 * BUG-VIEWPORT-INICIAL · getCanvas().getWidth() puede ser 0 justo tras show()
 *   CAUSA: frame.setVisible(true) es asíncrono en Swing. El Canvas puede no
 *          tener sus dimensiones finales en el mismo tick. Si viewportManager
 *          .onResize(0, 0) se llama, el viewport queda en estado inválido.
 *   SOLUCIÓN: validar que w > 0 && h > 0 antes de llamar onResize(). Si las
 *             dimensiones aún no están disponibles, el ComponentListener las
 *             capturará cuando el Canvas termine de dimensionarse.
 *   RIESGO: ninguno. La validación es defensiva y no cambia el flujo normal.
 */
public class DisplayManager {

    private static final Logger LOG = Logger.getLogger(DisplayManager.class.getName());

    // ─── Subsistemas ──────────────────────────────────────────────────────────
    private final DisplaySettings       settings;
    private final ViewportManager       viewportManager;
    private final WindowManager         windowManager;
    private final FullscreenManager     fullscreenManager;
    private final RenderSurfaceManager  surfaceManager;
    private final ScalingManager        scalingManager;
    private final BufferStrategyManager bsManager;

    // ─── Estado ───────────────────────────────────────────────────────────────
    /** Graphics2D del framebuffer virtual, válido entre beginFrame/endFrame. */
    private Graphics2D currentVirtualG = null;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public DisplayManager(DisplaySettings settings) {
        this.settings = settings;

        // 1. ViewportManager primero (WindowManager lo necesita para notificar)
        viewportManager   = new ViewportManager(settings);

        // 2. WindowManager (crea JFrame + Canvas, registra resize → viewportManager)
        windowManager     = new WindowManager(settings, viewportManager);

        // 3. Managers independientes
        fullscreenManager = new FullscreenManager(settings.monitorIndex);
        surfaceManager    = new RenderSurfaceManager(settings);
        scalingManager    = new ScalingManager(settings);
        bsManager         = new BufferStrategyManager(windowManager.getCanvas());

        // 4. Registrar resize listener del bsManager (recrear BS al resize)
        //    componentResized() se llama en EDT → bsManager.recreate() también
        //    en EDT → canvas.createBufferStrategy() es correcto en EDT.
        windowManager.addResizeListener((rw, rh, vp) -> {
            bsManager.recreate();
        });

        LOG.info("DisplayManager inicializado. Virtual: " +
                 settings.virtualWidth + "x" + settings.virtualHeight);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Overload de compatibilidad sin FocusListener.
     */
    public void init(java.awt.event.KeyListener keyListener,
                     java.awt.event.MouseListener mouseListener,
                     java.awt.event.MouseMotionListener motionListener,
                     java.awt.event.MouseWheelListener wheelListener) {
        init(keyListener, mouseListener, motionListener, wheelListener, null);
    }

    /**
     * BUG-INIT-FIRMA FIX: overload completo con FocusListener opcional.
     *
     * Inicializa la ventana y el BufferStrategy.
     * Llamar ANTES de iniciar el game loop.
     *
     * @param keyListener    listener de teclado (puede ser null)
     * @param mouseListener  listener de mouse (puede ser null)
     * @param motionListener listener de movimiento mouse (puede ser null)
     * @param wheelListener  listener de rueda mouse (puede ser null)
     * @param focusListener  listener de foco (puede ser null; normalmente KeyBoard)
     */
    public void init(java.awt.event.KeyListener keyListener,
                     java.awt.event.MouseListener mouseListener,
                     java.awt.event.MouseMotionListener motionListener,
                     java.awt.event.MouseWheelListener wheelListener,
                     FocusListener focusListener) {

        windowManager.addInputListeners(keyListener, mouseListener,
                                        motionListener, wheelListener,
                                        focusListener);
        windowManager.show();

        // DESPUÉS de show() para que el Canvas tenga peer nativo
        bsManager.init();

        // Arrancar en fullscreen si settings lo pide.
        // init() se llama desde el hilo principal (pre-GameLoop), antes de que
        // el EDT tome control exclusivo, así que esta llamada directa es segura.
        if (settings.startFullscreen) {
            fullscreenManager.enterFullscreen(windowManager.getFrame());
        }

        // BUG-VIEWPORT-INICIAL FIX: solo llamar onResize si las dimensiones
        // ya están disponibles. El ComponentListener las capturará de todas formas.
        Canvas c = windowManager.getCanvas();
        int w = c.getWidth();
        int h = c.getHeight();
        if (w > 0 && h > 0) {
            viewportManager.onResize(w, h);
        }

        LOG.info("Display iniciado. Canvas: " + w + "x" + h);
    }

    // ─── Frame pipeline ───────────────────────────────────────────────────────

    /**
     * Inicia un frame de render.
     *
     * @return Graphics2D del framebuffer virtual en coordenadas virtuales,
     *         o null si hay que saltar este frame (recovery de contentsLost).
     *
     * Si devuelve no-null, SIEMPRE llamar endFrame() después.
     */
    public Graphics2D beginFrame() {
        currentVirtualG = surfaceManager.beginFrame();
        return currentVirtualG;
    }

    /**
     * Termina el frame: escala el framebuffer virtual a pantalla y presenta.
     *
     * @param virtualG el Graphics2D devuelto por beginFrame() (será disposed)
     */
    public void endFrame(Graphics2D virtualG) {
        // Cerrar el Graphics del framebuffer virtual
        surfaceManager.endFrame(virtualG);
        currentVirtualG = null;

        // Adquirir Graphics2D del canvas real (BufferStrategy)
        Graphics2D screenG = bsManager.acquireGraphics();
        if (screenG == null) return; // Frame saltado, recovery en progreso

        try {
            scalingManager.present(screenG, surfaceManager.getFramebuffer(),
                                   viewportManager.getViewport());
        } finally {
            bsManager.present(screenG); // dispose + show()
        }
    }

    // ─── Fullscreen toggle ────────────────────────────────────────────────────

    /**
     * Alterna fullscreen ↔ windowed de forma segura.
     *
     * BUG-EDT-FULLSCREEN FIX: la operación se despacha al EDT via invokeLater().
     * BUG-FOCUS-LOST-AFTER-TOGGLE FIX: se pide el foco del Canvas al terminar.
     * BUG-F11-DOBLE-TOGGLE FIX: keyboard.clearFsTogglePending() al terminar libera
     *   el guard que suprime edges duplicados de F11 durante el toggle.
     *
     * @param keyboard el KeyBoard del juego (para restaurar foco y limpiar guard).
     */
    public void toggleFullscreen(KeyBoard keyboard) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            fullscreenManager.toggle(
                windowManager.getFrame(),
                settings.windowedWidth,
                settings.windowedHeight
            );
            // BUG-FOCUS-LOST-AFTER-TOGGLE FIX + BUG-F11-DOBLE-TOGGLE FIX:
            // pedir foco en el siguiente ciclo EDT (tras setVisible(true)),
            // y luego liberar el guard para que F11 vuelva a responder.
            javax.swing.SwingUtilities.invokeLater(() -> {
                windowManager.getCanvas().requestFocusInWindow();
                keyboard.clearFsTogglePending();
            });
        });
    }

    /**
     * Overload de compatibilidad sin referencia a KeyBoard.
     * Úsalo solo si no tienes acceso al KeyBoard desde este punto.
     * NO corrige BUG-F11-DOBLE-TOGGLE ni BUG-FOCUS-LOST-AFTER-TOGGLE.
     */
    public void toggleFullscreen() {
        javax.swing.SwingUtilities.invokeLater(() ->
            fullscreenManager.toggle(
                windowManager.getFrame(),
                settings.windowedWidth,
                settings.windowedHeight
            )
        );
    }

    public boolean isFullscreen() {
        return fullscreenManager.isFullscreen();
    }

    // ─── Acceso a datos del viewport ─────────────────────────────────────────

    /**
     * Viewport actual. Usar para:
     *  - Transformar coordenadas de mouse a coordenadas virtuales
     *  - Saber los límites virtuales visibles
     *
     * Referencia inmutable — seguro cachear por frame.
     */
    public ViewportInfo getViewport() {
        return viewportManager.getViewport();
    }

    /** Ancho virtual fijo (constante de settings). */
    public int getVirtualWidth() {
        return settings.virtualWidth;
    }

    /** Alto virtual fijo (constante de settings). */
    public int getVirtualHeight() {
        return settings.virtualHeight;
    }

    // ─── Registro de listeners externos ──────────────────────────────────────

    /**
     * Registra un listener para cuando el canvas cambia de tamaño.
     * Útil para que Camera, UIManager y otros sistemas se adapten.
     */
    public void addResizeListener(ResizeListener l) {
        windowManager.addResizeListener(l);
    }

    public void removeResizeListener(ResizeListener l) {
        windowManager.removeResizeListener(l);
    }

    // ─── Acceso a subsistemas (para casos avanzados) ─────────────────────────

    public WindowManager     getWindowManager()     { return windowManager;     }
    public ViewportManager   getViewportManager()   { return viewportManager;   }
    public FullscreenManager getFullscreenManager() { return fullscreenManager; }
    public Canvas            getCanvas()            { return windowManager.getCanvas(); }
}
