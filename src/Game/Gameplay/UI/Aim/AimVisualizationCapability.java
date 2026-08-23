package Game.Gameplay.UI.Aim;

import Game.Player.Player;
import java.awt.Graphics2D;

/**
 * Capability de visualización de apuntado — define una capacidad visual que puede
 * añadirse aditivamente al HUD sin modificar CrossHairHUD.
 *
 * ── DISEÑO ADITIVO ────────────────────────────────────────────────────────
 *
 * Este sistema permite que amuletos, items o power-ups concedan capacidades
 * visuales de forma modular:
 *
 *   Amuleto A → muestra trayectoria
 *   Amuleto B → muestra alcance
 *   Amuleto C → muestra colisiones estimadas
 *   Amuleto D → muestra trayectoria de homing
 *   Amuleto E → muestra información completa
 *
 * Sin convertir AimUI en un monstruo de if/else.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   AimVisualizationCapability (interfaz)
 *          │
 *          ├── BasicCrosshairCapability (crosshair simple)
 *          ├── TrajectoryVisualizationCapability (trayectoria proyectada)
 *          ├── RangeIndicatorCapability (círculo de alcance)
 *          ├── CollisionPredictionCapability (puntos de impacto estimados)
 *          └── HomingPathCapability (curva de homing)
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   CrossHairHUD → coordina capabilities activas, convierte coordenadas
 *   Capability   → renderiza su aspecto visual específico
 *   Amuleto      → concede la capability al Player
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // En CrossHairHUD.draw():
 *   for (AimVisualizationCapability cap : activeCapabilities) {
 *       cap.render(g, player, camera);
 *   }
 *
 *   // Al obtener amuleto:
 *   player.getAimCapabilities().add(new TrajectoryVisualizationCapability());
 */
public interface AimVisualizationCapability {

    /**
     * Renderiza el aspecto visual de esta capability.
     *
     * @param g      Graphics2D del framebuffer virtual
     * @param player Player para consultar estado (arma, proyectil, posición)
     * @param camX   offset X de cámara para conversión mundo→pantalla
     * @param camY   offset Y de cámara para conversión mundo→pantalla
     */
    void render(Graphics2D g, Player player, double camX, double camY);

    /**
     * Prioridad de renderizado — menor número = renderiza primero.
     * Útil para controlar el orden de capas (crosshair base antes de trayectoria).
     *
     * @return prioridad (0-100, default 50)
     */
    default int getRenderPriority() {
        return 50;
    }

    /**
     * True si esta capability requiere que el botón derecho esté presionado.
     * False = siempre visible (ej: indicador pasivo de alcance).
     *
     * @return true si requiere apuntado activo
     */
    default boolean requiresAiming() {
        return true;
    }

    /**
     * Nombre descriptivo de la capability para debugging/UI.
     *
     * @return nombre legible (ej: "Trajectory Visualization")
     */
    String getName();
}
