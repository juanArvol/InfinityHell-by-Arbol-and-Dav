package Display.Backend;

import Display.State.DisplayMode;
import java.awt.GraphicsConfiguration;

/**
 * Fotografía inmutable del estado del subsistema Display observado directamente
 * desde AWT en un instante preciso.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FILOSOFÍA — AWT COMO FUENTE DE VERDAD
 *
 * El Engine no controla el ciclo de vida de la ventana: AWT lo controla.
 * DisplaySnapshot captura lo que AWT realmente reporta en el momento de la
 * lectura. Ningún campo proviene de suposiciones internas del Engine sobre
 * qué debería haber ocurrido.
 *
 * Invariante central:
 *   Cada campo de este record se obtiene directamente de un objeto AWT.
 *   Ningún campo se deriva de lo que el Pipeline solicitó.
 *   El Pipeline publica DisplayState basándose en este snapshot,
 *   no en lo que creyó que pasaría.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMPOS Y SUS FUENTES AWT
 *
 * confirmedMode
 *   Fuente: GraphicsDevice.getFullScreenWindow(), JFrame.isUndecorated(),
 *           JFrame.getExtendedState().
 *   Semántica: modo de presentación que AWT confirma actualmente.
 *     FULLSCREEN_EXCLUSIVE  → device.getFullScreenWindow() == frame
 *     BORDERLESS_FULLSCREEN → frame.isUndecorated() &&
 *                             (extendedState & MAXIMIZED_BOTH) == MAXIMIZED_BOTH
 *     WINDOWED              → cualquier otro caso
 *   Nunca deriva del campo currentMode interno de FullscreenManager.
 *
 * canvasWidth / canvasHeight
 *   Fuente: Canvas.getWidth() / Canvas.getHeight().
 *   Semántica: dimensiones físicas del canvas según el LayoutManager AWT.
 *   Pueden ser 0 si el peer no existe todavía o si el canvas no está
 *   correctamente inicializado.
 *
 * canvasDisplayable
 *   Fuente: Canvas.isDisplayable().
 *   Semántica: el peer nativo del canvas existe y puede recibir operaciones
 *   gráficas. Es la condición previa necesaria para createBufferStrategy().
 *
 * canvasVisible
 *   Fuente: Canvas.isVisible().
 *   Semántica: el canvas es visible en el árbol de componentes. No implica
 *   que la ventana esté en pantalla (ver windowVisible).
 *
 * windowVisible
 *   Fuente: JFrame.isVisible().
 *   Semántica: la ventana está actualmente visible en pantalla.
 *
 * windowActive
 *   Fuente: JFrame.isActive().
 *   Semántica: la ventana tiene el foco de activación del OS.
 *   False durante Alt+Tab, minimización o cuando otra app está al frente.
 *
 * graphicsConfig
 *   Fuente: Canvas.getGraphicsConfiguration().
 *   Semántica: configuración gráfica actual del dispositivo donde está
 *   renderizando el canvas. Puede ser null si el canvas no está anclado
 *   a ningún dispositivo todavía.
 *
 * bufferStrategyPresent
 *   Fuente: Canvas.getBufferStrategy() != null.
 *   Semántica: existe una BufferStrategy activa en el canvas.
 *   No implica que sea válida (ver bufferStrategyContentsLost).
 *
 * bufferStrategyContentsLost
 *   Fuente: BufferStrategy.contentsLost().
 *   Semántica: la BS existe pero perdió su contenido y necesita reconstrucción.
 *   False si no hay BS (no se puede preguntar contentsLost a null).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * USO
 *
 * El snapshot se obtiene llamando AwtWindowBackend.readSnapshot().
 * Se valida con SnapshotValidator antes de usarlo.
 * El Pipeline publica DisplayState derivado de sus campos.
 *
 * DisplaySnapshot es permanente: no existe solo durante transiciones.
 * AwtWindowBackend mantiene el último snapshot leído como estado observable
 * actual del subsistema, consultable en cualquier momento.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 * Record inmutable. Seguro compartir entre threads sin sincronización.
 * El campo graphicsConfig es una referencia a un objeto AWT; su contenido
 * puede cambiar si el device cambia, pero la referencia del record no.
 */
public record DisplaySnapshot(

    /**
     * Modo de presentación confirmado por AWT.
     * Derivado de device.getFullScreenWindow() y estado del JFrame.
     * Nunca del campo currentMode interno de FullscreenManager.
     */
    DisplayMode confirmedMode,

    /**
     * Ancho físico del canvas en píxeles, según Canvas.getWidth().
     * 0 si el peer no existe o el canvas no está inicializado.
     */
    int canvasWidth,

    /**
     * Alto físico del canvas en píxeles, según Canvas.getHeight().
     * 0 si el peer no existe o el canvas no está inicializado.
     */
    int canvasHeight,

    /**
     * True si Canvas.isDisplayable() — el peer nativo AWT existe.
     * Condición necesaria para createBufferStrategy().
     */
    boolean canvasDisplayable,

    /**
     * True si Canvas.isVisible() — el canvas es visible en el árbol Swing.
     */
    boolean canvasVisible,

    /**
     * True si JFrame.isVisible() — la ventana está en pantalla.
     */
    boolean windowVisible,

    /**
     * True si JFrame.isActive() — la ventana tiene activación del OS.
     */
    boolean windowActive,

    /**
     * GraphicsConfiguration del canvas en el momento de la lectura.
     * Null si el canvas no está asociado a ningún device todavía.
     */
    GraphicsConfiguration graphicsConfig,

    /**
     * True si Canvas.getBufferStrategy() != null.
     * No implica que la BS sea válida.
     */
    boolean bufferStrategyPresent,

    /**
     * True si la BS existe y BufferStrategy.contentsLost() == true.
     * False si no hay BS o si la BS no ha perdido contenido.
     */
    boolean bufferStrategyContentsLost

) {

    // ── Conveniencias semánticas ──────────────────────────────────────────────

    /**
     * True si el canvas tiene dimensiones válidas (ambas > 0).
     * Condición necesaria para calcular el viewport.
     */
    public boolean hasValidDimensions() {
        return canvasWidth > 0 && canvasHeight > 0;
    }

    /**
     * True si la ventana está en modo fullscreen según AWT.
     */
    public boolean isFullscreen() {
        return confirmedMode.isFullscreen();
    }

    /**
     * True si la BS existe y no ha perdido contenido.
     * Equivale a: bufferStrategyPresent && !bufferStrategyContentsLost.
     */
    public boolean isBufferStrategyHealthy() {
        return bufferStrategyPresent && !bufferStrategyContentsLost;
    }

    /**
     * Descripción legible para logging y diagnóstico.
     */
    @Override
    public String toString() {
        return String.format(
            "DisplaySnapshot[mode=%s canvas=%dx%d displayable=%b visible=%b/%b active=%b gc=%s bs=%b/%s]",
            confirmedMode, canvasWidth, canvasHeight,
            canvasDisplayable, canvasVisible, windowVisible, windowActive,
            graphicsConfig != null ? graphicsConfig.getDevice().getIDstring() : "null",
            bufferStrategyPresent,
            bufferStrategyContentsLost ? "lost" : "ok"
        );
    }
}
