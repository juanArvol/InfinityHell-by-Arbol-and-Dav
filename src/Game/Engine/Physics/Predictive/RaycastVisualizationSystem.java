package Game.Engine.Physics.Predictive;

import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * RaycastVisualizationSystem — Sistema Global de Visualización de Raycasts
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PROPÓSITO:
 *   Sistema centralizado que captura y renderiza todos los raycasts activos
 *   del juego, independientemente del tipo de objeto.
 *
 * FILOSOFÍA:
 *   - CollisionsSystem registra los raycasts de objetos en movimiento
 *   - Este sistema los renderiza en una pasada dedicada
 *   - No depende de componentes individuales
 *
 * USO:
 *   // En CollisionsSystem o donde se calcule el raycast:
 *   RaycastVisualizationSystem.registerRaycast(obj, raycast, detection);
 *   
 *   // En el loop de render:
 *   RaycastVisualizationSystem.render(ctx, camera);
 *   
 *   // Al inicio de cada frame:
 *   RaycastVisualizationSystem.clearFrame();
 */
public class RaycastVisualizationSystem {

    /**
     * Registro de raycasts del frame actual.
     * Thread-safe para permitir registro desde múltiples sistemas.
     */
    private static final Map<GameObjects, RaycastData> activeRaycasts = 
        new ConcurrentHashMap<>();

    /**
     * Datos de un raycast registrado.
     */
    public static class RaycastData {
        public final double x, y;
        public final VelocityRaycast.RaycastResult raycast;
        public final RaycastCollisionDetector.DetectionResult detection;

        public RaycastData(double x, double y,
                          VelocityRaycast.RaycastResult raycast,
                          RaycastCollisionDetector.DetectionResult detection) {
            this.x = x;
            this.y = y;
            this.raycast = raycast;
            this.detection = detection;
        }
    }

    /**
     * Registra un raycast para ser renderizado en este frame.
     * 
     * @param obj objeto que genera el raycast
     * @param x posición X del objeto
     * @param y posición Y del objeto
     * @param raycast resultado del cálculo de raycast
     * @param detection resultado de la detección de colisión
     */
    public static void registerRaycast(GameObjects obj, double x, double y,
                                      VelocityRaycast.RaycastResult raycast,
                                      RaycastCollisionDetector.DetectionResult detection) {
        if (!VelocityRaycast.SHOW_RAYCASTS) {
            return; // No registrar si no está activo
        }

        if (raycast == null || !raycast.shouldCast) {
            return; // No hay raycast válido
        }

        activeRaycasts.put(obj, new RaycastData(x, y, raycast, detection));
    }

    /**
     * Renderiza todos los raycasts registrados.
     * Debe llamarse en la fase de render del GameLoop.
     *
     * @param ctx contexto de render
     * @param camera cámara para transformar coordenadas
     */
    public static void render(RenderContext ctx, RenderCamera camera) {
        if (!VelocityRaycast.SHOW_RAYCASTS) {
            return;
        }

        if (activeRaycasts.isEmpty()) {
            return;
        }

        // Renderizar cada raycast registrado
        for (RaycastData data : activeRaycasts.values()) {
            RaycastDebugRenderer.render(ctx, camera, 
                                       data.x, data.y, 
                                       data.raycast, data.detection);
        }
    }

    /**
     * Limpia los raycasts del frame anterior.
     * Debe llamarse al inicio de cada frame ANTES de CollisionsSystem.
     */
    public static void clearFrame() {
        activeRaycasts.clear();
    }

    /**
     * Obtiene el número de raycasts activos (para debug/stats).
     */
    public static int getActiveCount() {
        return activeRaycasts.size();
    }
}
