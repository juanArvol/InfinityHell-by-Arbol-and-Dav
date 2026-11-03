package Game.Engine.Physics.Predictive;

import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.GameMath.Logic2D.Vector2D;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * RaycastDebugRenderer — Visualización de Raycasts en Pantalla
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PROPÓSITO:
 *   Renderizar visualmente los raycasts para debug y ajuste de parámetros.
 *   
 *   Como en Unity/Unreal/Godot, permite ver:
 *   - Dirección del raycast (línea)
 *   - Longitud proporcional a velocidad
 *   - Punto de impacto (si hay)
 *   - Color cambia según estado (libre/bloqueado)
 *
 * ACTIVACIÓN:
 *   VelocityRaycast.SHOW_RAYCASTS = true/false
 *
 * COLORES:
 *   - VERDE: Path libre, no hay obstáculos
 *   - ROJO: Obstáculo detectado, movimiento limitado
 *   - AMARILLO: Raycast activo pero movimiento permitido
 */
public class RaycastDebugRenderer {

    // ═════════════════════════════════════════════════════════════════════
    // COLORES DE DEBUG
    // ═════════════════════════════════════════════════════════════════════

    /** Path libre, sin obstáculos */
    private static final Color COLOR_FREE = new Color(0, 255, 0, 180);
    
    /** Obstáculo detectado, movimiento limitado */
    private static final Color COLOR_BLOCKED = new Color(255, 0, 0, 180);
    
    /** Punto de impacto */
    private static final Color COLOR_HIT_POINT = new Color(255, 255, 0, 255);

    // ═════════════════════════════════════════════════════════════════════
    // RENDERING
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Renderiza un raycast en pantalla.
     *
     * @param ctx contexto de render
     * @param camera cámara para transformar coordenadas
     * @param startX posición inicial X (world space)
     * @param startY posición inicial Y (world space)
     * @param raycast resultado del cálculo de raycast
     * @param detection resultado de la detección de colisión
     */
    public static void render(RenderContext ctx, RenderCamera camera,
                             double startX, double startY,
                             VelocityRaycast.RaycastResult raycast,
                             RaycastCollisionDetector.DetectionResult detection) {
        // Verificar si el debug está activado
        if (!VelocityRaycast.SHOW_RAYCASTS) {
            return;
        }

        // Verificar si hay raycast que renderizar
        if (!raycast.shouldCast) {
            return;
        }

        // Obtener Graphics2D del contexto
        Graphics2D g = ctx.getGraphics2D();
        if (g == null) {
            return;
        }

        // Transformar coordenadas a screen space
        int screenStartX = (int) (startX - camera.getX());
        int screenStartY = (int) (startY - camera.getY());

        // Calcular punto final del raycast
        double endX = startX + raycast.direction.getX() * raycast.rayLength;
        double endY = startY + raycast.direction.getY() * raycast.rayLength;
        int screenEndX = (int) (endX - camera.getX());
        int screenEndY = (int) (endY - camera.getY());

        // Determinar color según estado
        Color lineColor = detection.hasObstacle ? COLOR_BLOCKED : COLOR_FREE;

        // Dibujar línea del raycast
        g.setColor(lineColor);
        g.drawLine(screenStartX, screenStartY, screenEndX, screenEndY);

        // Si hay obstáculo, dibujar punto de impacto
        if (detection.hasObstacle && detection.distanceToObstacle < Double.MAX_VALUE) {
            // Calcular posición del punto de impacto
            double hitDistance = Math.min(detection.distanceToObstacle, raycast.rayLength);
            double hitX = startX + raycast.direction.getX() * hitDistance;
            double hitY = startY + raycast.direction.getY() * hitDistance;
            int screenHitX = (int) (hitX - camera.getX());
            int screenHitY = (int) (hitY - camera.getY());

            // Dibujar círculo en el punto de impacto
            g.setColor(COLOR_HIT_POINT);
            g.fillOval(screenHitX - 3, screenHitY - 3, 6, 6);

            // Opcional: dibujar línea hasta el punto real de impacto
            g.setColor(COLOR_HIT_POINT);
            g.drawLine(screenStartX, screenStartY, screenHitX, screenHitY);
        }

        // Opcional: dibujar punto de origen
        g.setColor(Color.WHITE);
        g.fillOval(screenStartX - 2, screenStartY - 2, 4, 4);
    }

    /**
     * Versión simplificada para renderizar sin detección.
     * Útil cuando solo se quiere visualizar el raycast sin verificar colisiones.
     */
    public static void renderSimple(RenderContext ctx, RenderCamera camera,
                                   double startX, double startY,
                                   VelocityRaycast.RaycastResult raycast) {
        render(ctx, camera, startX, startY, raycast, 
               RaycastCollisionDetector.DetectionResult.noObstacle());
    }

    /**
     * Renderiza información de debug textual sobre el raycast.
     * Muestra datos en pantalla para tuning de parámetros.
     */
    public static void renderInfo(Graphics2D g, int x, int y,
                                  VelocityRaycast.RaycastResult raycast,
                                  RaycastCollisionDetector.DetectionResult detection) {
        if (!VelocityRaycast.SHOW_RAYCASTS || !raycast.shouldCast) {
            return;
        }

        g.setColor(Color.WHITE);
        g.drawString(String.format("Velocity: %.1f px/f", raycast.velocityMagnitude), x, y);
        g.drawString(String.format("Ray Length: %.1f px", raycast.rayLength), x, y + 12);
        
        if (detection.hasObstacle) {
            g.setColor(Color.RED);
            g.drawString(String.format("Obstacle: %.1f px", detection.distanceToObstacle), x, y + 24);
            g.drawString(String.format("Limit: %.2f%%", detection.movementLimitFactor * 100), x, y + 36);
        } else {
            g.setColor(Color.GREEN);
            g.drawString("Path: CLEAR", x, y + 24);
        }
    }
}
