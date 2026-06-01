package Display;

/**
 * Callback para notificaciones de resize.
 *
 * Implementar en cualquier sistema que necesite reaccionar
 * cuando cambia el tamaño del canvas:
 *  - Camera (ajustar bounds)
 *  - UIManager (reposicionar anchors)
 *  - BufferStrategyManager (recrear BufferStrategy)
 *  - Cualquier render target que dependa del tamaño real
 */
@FunctionalInterface
public interface ResizeListener {

    /**
     * Llamado cuando el canvas cambia de tamaño.
     *
     * @param realWidth   nuevo ancho real del canvas
     * @param realHeight  nuevo alto real del canvas
     * @param viewport    viewport ya recalculado por ViewportManager
     */
    void onResize(int realWidth, int realHeight, ViewportInfo viewport);
}
