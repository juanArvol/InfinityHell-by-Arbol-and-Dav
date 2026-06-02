package Display.Managers;

import Display.Settings.ScalingMode;
import Display.ViewportCalculator;
import Display.ViewportInfo;

import java.awt.Color;

/**
 * Mantiene el viewport actual y lo recalcula cuando cambia el canvas
 * o la resolución virtual.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: USO DEL ViewportCalculator UNIFICADO
 *
 * Problema anterior:
 *   ViewportManager importaba Display.Managers.ViewportCalculator, que usaba
 *   Math.round() para calcular vpW y vpH. Esto divergía del ViewportCalculator
 *   en el package Display (que usaba truncamiento correcto), causando que
 *   los FillAreas calculados por ViewportManager fueran diferentes de los
 *   que se necesitaban, produciendo oscilación de 1px en la barra lateral.
 *
 * Solución:
 *   ViewportManager ahora usa Display.ViewportCalculator (el unificado).
 *   Display.Managers.ViewportCalculator ha sido eliminado.
 *   Existe un único calculador con lógica de truncamiento correcta.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   onResize() / onVirtualResolutionChanged() → EDT únicamente.
 *   getViewport()                             → volatile; seguro desde GameLoop.
 *   setFillColor()                            → EDT únicamente.
 */
public final class ViewportManager {

    private int       virtualWidth;
    private int       virtualHeight;
    private final ScalingMode scalingMode;
    private Color     fillColor;

    /** Últimas dimensiones reales conocidas. Inicializadas con la resolución virtual. */
    private int lastRealWidth;
    private int lastRealHeight;

    private volatile ViewportInfo currentViewport;

    public ViewportManager(int virtualWidth, int virtualHeight,
                           ScalingMode scalingMode, Color fillColor) {
        this.virtualWidth   = virtualWidth;
        this.virtualHeight  = virtualHeight;
        this.scalingMode    = scalingMode;
        this.fillColor      = fillColor != null ? fillColor : Color.BLACK;
        this.lastRealWidth  = virtualWidth;
        this.lastRealHeight = virtualHeight;
        this.currentViewport = ViewportCalculator.calculate(
            virtualWidth, virtualHeight,
            virtualWidth, virtualHeight,
            scalingMode, this.fillColor
        );
    }

    public ViewportManager(int virtualWidth, int virtualHeight, ScalingMode scalingMode) {
        this(virtualWidth, virtualHeight, scalingMode, Color.BLACK);
    }

    // ── Operaciones ───────────────────────────────────────────────────────────

    /**
     * Recalcula el viewport cuando cambia el tamaño del canvas.
     *
     * IDEMPOTENTE: no recalcula si w y h son idénticos al estado anterior.
     * EDT únicamente.
     *
     * @return true si el viewport cambió efectivamente; false si era igual.
     */
    public boolean onResize(int realWidth, int realHeight) {
        if (realWidth <= 0 || realHeight <= 0) return false;
        if (realWidth == lastRealWidth && realHeight == lastRealHeight) {
            return false;
        }
        this.lastRealWidth  = realWidth;
        this.lastRealHeight = realHeight;
        currentViewport = ViewportCalculator.calculate(
            virtualWidth, virtualHeight,
            realWidth, realHeight,
            scalingMode, fillColor
        );
        return true;
    }

    /**
     * Recalcula el viewport cuando cambia la resolución virtual.
     *
     * IDEMPOTENTE: no recalcula si las dimensiones virtuales son iguales.
     * EDT únicamente.
     *
     * @return true si el viewport cambió efectivamente.
     */
    public boolean onVirtualResolutionChanged(int newVirtualWidth, int newVirtualHeight) {
        if (newVirtualWidth <= 0 || newVirtualHeight <= 0) return false;
        if (newVirtualWidth == virtualWidth && newVirtualHeight == virtualHeight) {
            return false;
        }
        this.virtualWidth  = newVirtualWidth;
        this.virtualHeight = newVirtualHeight;
        currentViewport = ViewportCalculator.calculate(
            newVirtualWidth, newVirtualHeight,
            lastRealWidth, lastRealHeight,
            scalingMode, fillColor
        );
        return true;
    }

    /**
     * Cambia el color de relleno y fuerza recálculo.
     * EDT únicamente.
     */
    public void setFillColor(Color color) {
        Color newColor = color != null ? color : Color.BLACK;
        if (newColor.equals(this.fillColor)) return;
        this.fillColor = newColor;
        currentViewport = ViewportCalculator.calculate(
            virtualWidth, virtualHeight,
            lastRealWidth, lastRealHeight,
            scalingMode, fillColor
        );
    }

    /** Viewport actual. Inmutable, seguro compartir entre threads. */
    public ViewportInfo getViewport() { return currentViewport; }

    /** Ancho virtual actual. */
    public int getVirtualWidth()  { return virtualWidth;  }
    /** Alto virtual actual. */
    public int getVirtualHeight() { return virtualHeight; }
}
