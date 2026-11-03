package Game.Engine.Entity.Components.Physics;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Physics.Predictive.RaycastCollisionDetector;
import Game.Engine.Physics.Predictive.RaycastDebugRenderer;
import Game.Engine.Physics.Predictive.VelocityRaycast;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * RaycastVisualizerComponent — Componente para Visualizar Raycasts
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PROPÓSITO:
 *   Permite a cualquier GameObject con física visualizar su raycast predictivo.
 *   
 *   Se añade automáticamente a proyectiles y otros objetos rápidos.
 *   Solo renderiza cuando VelocityRaycast.SHOW_RAYCASTS = true.
 *
 * NOTA IMPORTANTE:
 *   Implementa Renderable (NO DebugRenderable) para renderizarse siempre,
 *   independientemente del estado del DebugRenderSystem. El flag interno
 *   VelocityRaycast.SHOW_RAYCASTS controla la visibilidad.
 *
 * USO:
 *   gameObject.addComponent(new RaycastVisualizerComponent());
 *
 * INTEGRACIÓN:
 *   - Lee el Physics2DComponent para obtener velocidad actual
 *   - Calcula raycast en tiempo real
 *   - Renderiza cuando SHOW_RAYCASTS = true
 */
public class RaycastVisualizerComponent extends Component implements Renderable {

    // Caché de último raycast y detección para evitar recalcular en cada frame
    private VelocityRaycast.RaycastResult lastRaycast = null;
    private RaycastCollisionDetector.DetectionResult lastDetection = null;

    /**
     * Constructor por defecto.
     */
    public RaycastVisualizerComponent() {
        // Componente ligero, no necesita inicialización
    }

    /**
     * Actualiza el caché de raycast y detección.
     * Debe ser llamado por el sistema de colisiones después de calcular.
     */
    public void updateRaycastData(VelocityRaycast.RaycastResult raycast,
                                 RaycastCollisionDetector.DetectionResult detection) {
        this.lastRaycast = raycast;
        this.lastDetection = detection;
    }

    /**
     * Renderiza el raycast.
     * Implementa Renderable en lugar de DebugRenderable para renderizarse
     * independientemente del estado del DebugRenderSystem.
     */
    @Override
    public void render(RenderContext ctx, RenderCamera camera) {
        // Solo renderizar si el flag global está activo
        if (!VelocityRaycast.SHOW_RAYCASTS) {
            return;
        }
        
        // DEBUG: Verificar que render() se está llamando
        System.out.println("[RaycastVisualizer] render() CALLED!");

        // Obtener posición del gameObject
        double x = gameObject.getTransform().getPosition().getX();
        double y = gameObject.getTransform().getPosition().getY();
        
        System.out.println("[RaycastVisualizer] Position: (" + x + ", " + y + ")");

        // Si no hay caché, calcular en tiempo real
        if (lastRaycast == null) {
            Physics2DComponent physComp = gameObject.getComponent(Physics2DComponent.class);
            if (physComp == null) {
                System.out.println("[RaycastVisualizer] ERROR: No Physics2DComponent!");
                return; // No hay física, no hay raycast
            }

            double vx = physComp.getPhysics().getVelocity().getX() * 0.016; // Aproximar deltaTime
            double vy = physComp.getPhysics().getVelocity().getY() * 0.016;
            
            System.out.println("[RaycastVisualizer] Velocity: (" + vx + ", " + vy + ")");

            lastRaycast = VelocityRaycast.calculateRaycast(vx, vy);
            lastDetection = RaycastCollisionDetector.DetectionResult.noObstacle();
        }

        // Renderizar el raycast
        if (lastRaycast != null && lastRaycast.shouldCast) {
            System.out.println("[RaycastVisualizer] Rendering raycast! Length: " + lastRaycast.rayLength);
            RaycastDebugRenderer.render(ctx, camera, x, y, lastRaycast, 
                                       lastDetection != null ? lastDetection 
                                       : RaycastCollisionDetector.DetectionResult.noObstacle());
        } else {
            System.out.println("[RaycastVisualizer] No raycast to render (shouldCast=false)");
        }
    }

    /**
     * Limpia el caché de raycast.
     * Llamar al inicio de cada frame o cuando el objeto cambie de estado.
     */
    public void clearCache() {
        this.lastRaycast = null;
        this.lastDetection = null;
    }
}
