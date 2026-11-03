package Game.Engine.Physics.Predictive;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * RaycastConfig — Configuración Global del Sistema de Raycast Predictivo
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PROPÓSITO:
 *   Centralizar toda la configuración del sistema de raycast en un solo lugar.
 *   Permite ajustar parámetros y activar debug sin modificar código.
 *
 * USO EN CÓDIGO:
 *   // Activar visualización
 *   RaycastConfig.SHOW_RAYCASTS = true;
 *   
 *   // Ajustar factor de predicción
 *   RaycastConfig.RAYCAST_FACTOR = 2.5;
 *
 * USO CON TECLAS (ejemplo en GameState):
 *   if (keyboard.isKeyPressed(KeyEvent.VK_F3)) {
 *       RaycastConfig.toggle();
 *   }
 */
public class RaycastConfig {

    // ═════════════════════════════════════════════════════════════════════
    // DEBUG VISUALIZATION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Activar/desactivar visualización de raycasts.
     * 
     * TRUE  = Renderizar raycasts en pantalla (verde = libre, rojo = bloqueado)
     * FALSE = No renderizar (modo producción)
     *
     * NOTA: También controla VelocityRaycast.SHOW_RAYCASTS automáticamente.
     */
    public static boolean SHOW_RAYCASTS = false;

    // ═════════════════════════════════════════════════════════════════════
    // RAYCAST PARAMETERS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Velocidad mínima para activar raycast (px/frame).
     * Por debajo de este umbral, no se hace raycast (optimización).
     *
     * DEFAULT: 3.0
     * RANGO RECOMENDADO: 1.0 - 10.0
     */
    public static double MIN_VELOCITY_FOR_RAYCAST = 3.0;

    /**
     * Factor de proporcionalidad del raycast.
     * 
     * raycast_length = velocity_magnitude × RAYCAST_FACTOR
     *
     * Valores típicos:
     *   1.0 = raycast igual al desplazamiento del frame (ajustado)
     *   1.5 = 50% más de anticipación (seguro)
     *   2.0 = doble anticipación (muy seguro)
     *   3.0 = triple anticipación (ultra seguro, más costoso)
     *
     * DEFAULT: 2.0
     * RANGO RECOMENDADO: 1.0 - 3.0
     */
    public static double RAYCAST_FACTOR = 2.0;

    /**
     * Longitud mínima del raycast cuando se activa (px).
     * Garantiza detección mínima incluso en velocidades bajas.
     *
     * DEFAULT: 8.0
     * RANGO RECOMENDADO: 4.0 - 16.0
     */
    public static double MIN_RAYCAST_LENGTH = 8.0;

    /**
     * Longitud máxima del raycast (px).
     * Limita el raycast para evitar cálculos excesivos en velocidades extremas.
     *
     * DEFAULT: 200.0
     * RANGO RECOMENDADO: 100.0 - 500.0
     */
    public static double MAX_RAYCAST_LENGTH = 200.0;

    /**
     * Gap de seguridad al frenar (px).
     * El objeto se detiene a esta distancia del obstáculo.
     *
     * DEFAULT: 0.5
     * RANGO RECOMENDADO: 0.1 - 2.0
     */
    public static double SAFETY_GAP = 0.5;

    // ═════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Toggle de visualización (activar/desactivar).
     * Útil para binding a una tecla.
     */
    public static void toggle() {
        SHOW_RAYCASTS = !SHOW_RAYCASTS;
        VelocityRaycast.SHOW_RAYCASTS = SHOW_RAYCASTS;
        System.out.println("========================================");
        System.out.println("[RaycastConfig] Visualization: " + 
                          (SHOW_RAYCASTS ? "ON ✓" : "OFF ✗"));
        System.out.println("========================================");
    }

    /**
     * Activar visualización explícitamente.
     */
    public static void enable() {
        SHOW_RAYCASTS = true;
        VelocityRaycast.SHOW_RAYCASTS = true;
        System.out.println("[RaycastConfig] Visualization ENABLED");
    }

    /**
     * Desactivar visualización explícitamente.
     */
    public static void disable() {
        SHOW_RAYCASTS = false;
        VelocityRaycast.SHOW_RAYCASTS = false;
        System.out.println("[RaycastConfig] Visualization DISABLED");
    }

    /**
     * Resetear todos los parámetros a valores por defecto.
     */
    public static void resetToDefaults() {
        SHOW_RAYCASTS = false;
        MIN_VELOCITY_FOR_RAYCAST = 3.0;
        RAYCAST_FACTOR = 2.0;
        MIN_RAYCAST_LENGTH = 8.0;
        MAX_RAYCAST_LENGTH = 200.0;
        SAFETY_GAP = 0.5;
        
        VelocityRaycast.SHOW_RAYCASTS = false;
        
        System.out.println("[RaycastConfig] Reset to defaults");
    }

    /**
     * Obtener información actual de configuración.
     */
    public static String getConfigInfo() {
        return String.format(
            "=== Raycast Config ===\n" +
            "Visualization: %s\n" +
            "Min Velocity: %.1f px/frame\n" +
            "Raycast Factor: %.2f\n" +
            "Min Length: %.1f px\n" +
            "Max Length: %.1f px\n" +
            "Safety Gap: %.2f px",
            SHOW_RAYCASTS ? "ON" : "OFF",
            MIN_VELOCITY_FOR_RAYCAST,
            RAYCAST_FACTOR,
            MIN_RAYCAST_LENGTH,
            MAX_RAYCAST_LENGTH,
            SAFETY_GAP
        );
    }

    /**
     * Imprimir configuración actual en consola.
     */
    public static void printConfig() {
        System.out.println(getConfigInfo());
    }

    // Constructor privado para evitar instanciación
    private RaycastConfig() {}
}
