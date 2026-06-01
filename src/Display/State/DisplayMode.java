package Display.State;

/**
 * Modo de presentación de la ventana.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * Problema original:
 *   El estado fullscreen/windowed era un boolean volátil en FullscreenManager.
 *   No había distinción entre FULLSCREEN_EXCLUSIVE y BORDERLESS_FULLSCREEN.
 *   Añadir un tercer modo (borderless) requería modificar FullscreenManager
 *   y todos los puntos que consultaban isFullscreen().
 *
 * Causa raíz:
 *   Un boolean no puede representar más de dos estados. El diseño cerraba la
 *   extensibilidad por construcción.
 *
 * Solución:
 *   Enum con tres modos. Cada modo tiene semántica clara y es independiente.
 *   FullscreenManager opera sobre DisplayMode, no sobre un boolean.
 *
 * Beneficios:
 *   - Agregar BORDERLESS_FULLSCREEN no requiere cambios en DisplayManager.
 *   - El código que consulta el modo puede hacer switch exhaustivo.
 *   - isFullscreen() sigue siendo válido como conveniencia pero no oculta
 *     la distinción real entre modos.
 * ──────────────────────────────────────────────────────────────────────────
 */
public enum DisplayMode {

    /**
     * Ventana normal con decoración del sistema operativo.
     * El usuario puede redimensionar (si windowResizable = true).
     */
    WINDOWED,

    /**
     * Fullscreen exclusivo del sistema operativo.
     * La ventana toma control exclusivo del GraphicsDevice.
     * Menor latencia en algunos sistemas; puede cambiar la resolución de pantalla.
     */
    FULLSCREEN_EXCLUSIVE,

    /**
     * Ventana maximizada sin decoración (borderless windowed).
     * Cubre toda la pantalla visualmente pero sin tomar control exclusivo.
     * Permite Alt+Tab sin overhead de restaurar resolución.
     * Alternativa cuando FULLSCREEN_EXCLUSIVE no está soportado.
     */
    BORDERLESS_FULLSCREEN;

    /** True si este modo es cualquier forma de fullscreen. */
    public boolean isFullscreen() {
        return this == FULLSCREEN_EXCLUSIVE || this == BORDERLESS_FULLSCREEN;
    }
}
