package Game.Engine.Physics.KineticPhysics;

import Game.Engine.GameMath.Logic2D.AABB;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameObjects;
import java.util.List;
import java.util.ArrayList;

/**
 * Resolución de física usando Axis Clipping estilo Minecraft.
 *
 * ── Arquitectura ────────────────────────────────────────────────────────────
 *
 * Este helper encapsula el algoritmo completo de resolución de movimiento
 * tipo Minecraft:
 *
 *   1. Crear broadphase box con AABB.expandTowards()
 *   2. Recolectar bloques sólidos que intersecten broadphase
 *   3. FASE Y: iterar bloques, recortar vy, aplicar move(0, vy)
 *   4. FASE X: iterar bloques, recortar vx, aplicar move(vx, 0)
 *   5. Detectar estado de colisión (onGround, collidedHorizontally)
 *
 * ── Integración con ColliderComponent ──────────────────────────────────────
 *
 * Para mantener compatibilidad con el sistema existente, este helper:
 *   - Lee bounds desde ColliderComponent.getBounds()
 *   - Convierte RectangleH → AABB para procesamiento
 *   - Usa ColliderComponent.canCollideWith() para filtrado
 *   - Actualiza posición vía Transform después de resolver
 *
 * ── Uso en Entity.update() ─────────────────────────────────────────────────
 *
 *   Physics2D physics = getComponent(Physics2DComponent.class).getPhysics();
 *   ColliderComponent collider = getComponent(ColliderComponent.class);
 *
 *   // Guardar velocidad original para detectar colisiones
 *   double originalVx = physics.getVelocity().getX();
 *   double originalVy = physics.getVelocity().getY();
 *
 *   // Convertir velocidad a desplazamiento (velocity × deltaTime)
 *   double vx = originalVx * deltaTime;
 *   double vy = originalVy * deltaTime;
 *
 *   // Resolver movimiento con axis clipping
 *   AABBPhysicsResolver.Result result = AABBPhysicsResolver.resolve(
 *       this, collider, vx, vy, worldObjects
 *   );
 *
 *   // Actualizar velocidad según colisiones
 *   if (result.collidedY) {
 *       physics.getVelocity().setY(0);
 *   }
 *   if (result.collidedX) {
 *       physics.getVelocity().setX(0);
 *   }
 *
 *   // Actualizar flags de contacto
 *   if (result.onGround) {
 *       physics.setOnGround(true);
 *   }
 *   if (result.collidedHorizontally) {
 *       physics.setOnWall(true);
 *   }
 *
 * ── Ventajas sobre Swept AABB ──────────────────────────────────────────────
 *
 * - Más simple: sin cálculo de tiempos de contacto
 * - Más robusto: sin divisiones por velocidad (evita NaN/Inf)
 * - Más eficiente: una sola pasada por eje
 * - Determinista: orden fijo Y→X produce resultados consistentes
 * - Deslizamiento natural: si X colisiona, Y sigue moviéndose
 *
 * ── Cuándo usar esto vs SweptAABB ──────────────────────────────────────────
 *
 * AABBPhysicsResolver (Axis Clipping):
 *   - Entidades tipo platformer (Player, Enemies con gravedad)
 *   - Objetos que necesitan deslizamiento por paredes
 *   - Física determinista frame-by-frame
 *
 * SweptAABB (Continuous):
 *   - Proyectiles de alta velocidad (bullets)
 *   - Objetos que solo necesitan detectar primer contacto
 *   - Sensores que no aplican resolución física
 */
public final class AABBPhysicsResolver {

    private AABBPhysicsResolver() {}

    /**
     * Resultado de la resolución de movimiento.
     */
    public static final class Result {
        /** Desplazamiento final aplicado en X (ya recortado) */
        public final double finalDx;
        
        /** Desplazamiento final aplicado en Y (ya recortado) */
        public final double finalDy;
        
        /** True si hubo colisión vertical (piso o techo) */
        public final boolean collidedY;
        
        /** True si hubo colisión horizontal (pared) */
        public final boolean collidedX;
        
        /** True si la colisión vertical fue con el piso (cayendo) */
        public final boolean onGround;
        
        /** True si la colisión horizontal ocurrió */
        public final boolean collidedHorizontally;

        public Result(double finalDx, double finalDy, 
                     boolean collidedX, boolean collidedY,
                     boolean onGround, boolean collidedHorizontally) {
            this.finalDx = finalDx;
            this.finalDy = finalDy;
            this.collidedX = collidedX;
            this.collidedY = collidedY;
            this.onGround = onGround;
            this.collidedHorizontally = collidedHorizontally;
        }
    }

    /**
     * Resuelve el movimiento de un objeto usando Axis Clipping estilo Minecraft.
     *
     * ── Mini-HRFC 1.5: Parámetros como displacement ────────────────────────
     *
     * @param entity    el objeto que se está moviendo
     * @param collider  componente de colisión del objeto
     * @param vx        desplazamiento horizontal deseado [units], NO velocidad
     * @param vy        desplazamiento vertical deseado [units], NO velocidad
     * @param allObjects todos los objetos del mundo (se filtran internamente)
     * @return Result con desplazamientos finales y flags de colisión
     *
     * IMPORTANTE: vx y vy deben calcularse como velocity × deltaTime antes
     * de llamar a este método.
     *
     * EFECTO SECUNDARIO: Este método modifica directamente la posición del
     * Transform del entity. NO modifica la velocidad - eso debe hacerse
     * externamente basándose en los flags del Result.
     */
    public static Result resolve(
            GameObjects entity,
            ColliderComponent collider,
            double vx,
            double vy,
            List<GameObjects> allObjects) {

        // Guardar velocidad original para detectar colisiones
        double originalVx = vx;
        double originalVy = vy;

        // Convertir bounds actuales a AABB
        Game.Engine.GameMath.Logic2D.RectangleH currentBounds = collider.getBounds();
        AABB entityBox = AABB.fromRectangleH(currentBounds);

        // PASO 1: Crear broadphase box
        AABB broadphase = entityBox.expandTowards(vx, vy);

        // PASO 2: Recolectar bloques sólidos que intersecten broadphase
        List<AABB> obstacles = new ArrayList<>();
        for (GameObjects other : allObjects) {
            if (other == entity) continue;
            
            ColliderComponent otherCollider = other.getComponent(ColliderComponent.class);
            if (otherCollider == null) continue;
            if (otherCollider.isSensor()) continue;  // Sensores no bloquean movimiento
            if (!collider.canCollideWith(otherCollider)) continue;

            Game.Engine.GameMath.Logic2D.RectangleH otherBounds = otherCollider.getBounds();
            AABB otherBox = AABB.fromRectangleH(otherBounds);

            if (broadphase.intersects(otherBox)) {
                obstacles.add(otherBox);
            }
        }

        // PASO 3: FASE Y - Resolver movimiento vertical primero
        for (AABB obstacle : obstacles) {
            vy = entityBox.calculateYOffset(obstacle, vy);
        }
        entityBox.move(0, vy);

        // PASO 4: FASE X - Resolver movimiento horizontal (con Y ya aplicado)
        for (AABB obstacle : obstacles) {
            vx = entityBox.calculateXOffset(obstacle, vx);
        }
        entityBox.move(vx, 0);

        // PASO 5: Actualizar posición del Transform
        entity.getTransform().getPosition().setX(entityBox.minX - collider.getOffsetX());
        entity.getTransform().getPosition().setY(entityBox.minY - collider.getOffsetY());

        // PASO 6: Detectar estado de colisión
        boolean collidedY = Math.abs(vy - originalVy) > 1e-6;
        boolean collidedX = Math.abs(vx - originalVx) > 1e-6;
        boolean onGround = collidedY && originalVy > 0.0;  // Cayendo y colisionó
        boolean collidedHorizontally = collidedX;

        return new Result(vx, vy, collidedX, collidedY, onGround, collidedHorizontally);
    }

    /**
     * Versión simplificada que solo retorna los desplazamientos recortados
     * sin modificar el Transform (útil para previsualizaciones).
     */
    public static Result resolveWithoutApplying(
            ColliderComponent collider,
            double vx,
            double vy,
            List<AABB> obstacles) {

        double originalVx = vx;
        double originalVy = vy;

        Game.Engine.GameMath.Logic2D.RectangleH currentBounds = collider.getBounds();
        AABB entityBox = AABB.fromRectangleH(currentBounds);

        // FASE Y
        for (AABB obstacle : obstacles) {
            vy = entityBox.calculateYOffset(obstacle, vy);
        }

        // Simular move en Y (sin modificar el box original)
        AABB tempBox = entityBox.offset(0, vy);

        // FASE X
        for (AABB obstacle : obstacles) {
            vx = tempBox.calculateXOffset(obstacle, vx);
        }

        boolean collidedY = Math.abs(vy - originalVy) > 1e-6;
        boolean collidedX = Math.abs(vx - originalVx) > 1e-6;
        boolean onGround = collidedY && originalVy > 0.0;
        boolean collidedHorizontally = collidedX;

        return new Result(vx, vy, collidedX, collidedY, onGround, collidedHorizontally);
    }

    /**
     * Versión para obtener solo los obstáculos relevantes sin resolver movimiento.
     * Útil para optimización cuando necesitas reutilizar la lista de obstáculos.
     */
    public static List<AABB> collectObstacles(
            GameObjects entity,
            ColliderComponent collider,
            double vx,
            double vy,
            List<GameObjects> allObjects) {

        Game.Engine.GameMath.Logic2D.RectangleH currentBounds = collider.getBounds();
        AABB entityBox = AABB.fromRectangleH(currentBounds);
        AABB broadphase = entityBox.expandTowards(vx, vy);

        List<AABB> obstacles = new ArrayList<>();
        for (GameObjects other : allObjects) {
            if (other == entity) continue;
            
            ColliderComponent otherCollider = other.getComponent(ColliderComponent.class);
            if (otherCollider == null) continue;
            if (otherCollider.isSensor()) continue;
            if (!collider.canCollideWith(otherCollider)) continue;

            Game.Engine.GameMath.Logic2D.RectangleH otherBounds = otherCollider.getBounds();
            AABB otherBox = AABB.fromRectangleH(otherBounds);

            if (broadphase.intersects(otherBox)) {
                obstacles.add(otherBox);
            }
        }

        return obstacles;
    }
}
