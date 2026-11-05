package Game.Engine.Physics.Predictive;

import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * RaycastCollisionDetector — Detector de Limitación Predictiva de Movimiento
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ── RESPONSABILIDAD ÚNICA: ANTI-TUNNELING ─────────────────────────────────
 *
 * Este componente tiene UNA SOLA responsabilidad: detectar obstáculos en el
 * path de movimiento y LIMITAR el desplazamiento para prevenir tunneling.
 *
 * LO QUE HACE:
 *   ✓ Detecta si hay obstáculos en el path de movimiento
 *   ✓ Calcula distancia al obstáculo más cercano
 *   ✓ Calcula factor de limitación del movimiento (0.0-1.0)
 *   ✓ Retorna información para que el caller limite el movimiento
 *
 * LO QUE NO HACE (delegado a SweptAABB):
 *   ✗ NO calcula geometría precisa de contacto
 *   ✗ NO calcula normales de impacto
 *   ✗ NO mueve objetos (PhysicsStepper lo hace)
 *   ✗ NO despacha eventos (CollisionSystem lo hace)
 *   ✗ NO resuelve física (CollisionSystem lo hace)
 *
 * ── ALGORITMO DE LIMITACIÓN ───────────────────────────────────────────────
 *
 * 1. Calcular desplazamiento previsto: displacement = velocity × deltaTime
 * 2. Crear consulta volumétrica (swept bounds + raycast geometry)
 * 3. Broadphase: filtrar candidatos con bounds extendidos
 * 4. Narrowphase: calcular TOI preciso con SweptAABB.calculate2D()
 * 5. Identificar el obstáculo con TOI mínimo
 * 6. Calcular factor de limitación: limitFactor = distanceToObstacle / plannedMovement
 * 7. Retornar DetectionResult con limitFactor
 *
 * EJEMPLO:
 *   - Velocidad: 500 px/s, deltaTime: 0.016s
 *   - Desplazamiento previsto: 8 px
 *   - Obstáculo detectado a: 5 px
 *   - Factor de limitación: 5 / 8 = 0.625
 *   - Desplazamiento real aplicado: 8 × 0.625 = 5 px
 *
 * ── GEOMETRÍA DE CONSULTA ─────────────────────────────────────────────────
 *
 * NO es un raycast punto-a-punto tradicional.
 * Es una CONSULTA VOLUMÉTRICA que cubre toda la cara frontal del objeto:
 *
 * Para Entity de 32×48 moviéndose hacia la derecha:
 *   - Swept bounds cubre todo el volumen del recorrido
 *   - SweptAABB.calculate2D() detecta contacto de CUALQUIER parte del bounds
 *   - Garantiza que esquinas y bordes no atraviesen obstáculos
 *
 * ── SEPARACIÓN CLARA CON SWEPTAABB ────────────────────────────────────────
 *
 * RaycastCollisionDetector (Sub-Fase 1A):
 *   • Propósito: LIMITAR movimiento peligroso (anti-tunneling)
 *   • Timing: ANTES de mover objetos
 *   • Output: Factor de limitación (0.0-1.0)
 *   • Acción: Ninguna (solo informa)
 *
 * SweptAABB (Sub-Fase 1B):
 *   • Propósito: DETECTAR colisiones con precisión geométrica
 *   • Timing: DESPUÉS de raycast (si aplicó)
 *   • Output: TOI preciso, normales, eventos
 *   • Acción: Mueve objetos (PhysicsStepper), resuelve física
 *
 * NO SON REDUNDANTES:
 *   - Raycast es una BARRERA PREVENTIVA (velocidades >50px/frame)
 *   - SweptAABB es el SISTEMA DE COLISIONES COMPLETO (siempre)
 *
 * ── OPTIMIZACIÓN ──────────────────────────────────────────────────────────
 *
 * - Broadphase con bounds extendidos (VelocityRaycast.createRaycastBounds)
 * - Solo objetos con blockMovement=true se consideran obstáculos
 * - Ignora objetos pendientes de destrucción
 * - Usa SweptAABB.calculate2D() para cálculo preciso (sin duplicar código)
 * - Sin allocations innecesarias en hot path
 */
public class RaycastCollisionDetector {

    /**
     * Resultado de la detección predictiva.
     */
    public static class DetectionResult {
        /** ¿Se detectó obstáculo en el path? */
        public final boolean hasObstacle;
        
        /** Distancia al obstáculo más cercano (en unidades de movimiento, 0.0-1.0+) */
        public final double distanceToObstacle;
        
        /** Objeto más cercano detectado (null si no hay) */
        public final GameObjects nearestObject;
        
        /** 
         * Factor de limitación del movimiento (0.0 = no mover, 1.0 = movimiento completo)
         * Este es el valor CRÍTICO que limita el desplazamiento.
         */
        public final double movementLimitFactor;

        private DetectionResult(boolean hasObstacle, double distance, 
                               GameObjects nearest, double limitFactor) {
            this.hasObstacle = hasObstacle;
            this.distanceToObstacle = distance;
            this.nearestObject = nearest;
            this.movementLimitFactor = limitFactor;
        }

        public static DetectionResult noObstacle() {
            return new DetectionResult(false, Double.MAX_VALUE, null, 1.0);
        }

        public static DetectionResult obstacle(double distance, GameObjects obj, double limitFactor) {
            return new DetectionResult(true, distance, obj, limitFactor);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: DETECCIÓN PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Detecta si hay obstáculos en el path de movimiento predicho y calcula
     * EL FACTOR DE LIMITACIÓN DEL MOVIMIENTO.
     *
     * NUEVO ALGORITMO:
     *   1. Crear raycast basado en velocidad
     *   2. Encontrar obstáculo más cercano en el path
     *   3. Si hay obstáculo:
     *      a. Calcular distancia REAL al obstáculo
     *      b. Calcular factor de limitación: distance / plannedMovement
     *      c. Movimiento se limitará a este factor
     *   4. Si NO hay obstáculo: factor = 1.0 (movimiento completo)
     *
     * EJEMPLO:
     *   - Velocidad planeada: 50px
     *   - Obstáculo detectado a: 30px
     *   - Factor de limitación: 30/50 = 0.6
     *   - Movimiento real: 50 * 0.6 = 30px (SE DETIENE justo antes del muro)
     *
     * @param obj objeto que se va a mover
     * @param vx velocidad planeada X (px/frame)
     * @param vy velocidad planeada Y (px/frame)
     * @param allObjects lista de todos los objetos en la escena
     * @return DetectionResult con factor de limitación del movimiento
     */
    public static DetectionResult detectAndLimitMovement(GameObjects obj,
                                                        double vx, double vy,
                                                        List<GameObjects> allObjects) {
        // Calcular magnitud del movimiento planeado
        double plannedMovement = Math.sqrt(vx * vx + vy * vy);
        
        // Si no hay movimiento, no hay limitación
        if (plannedMovement < 0.001) {
            return DetectionResult.noObstacle();
        }

        // Calcular raycast
        VelocityRaycast.RaycastResult raycast = 
            VelocityRaycast.calculateRaycast(vx, vy);
        
        // Si velocidad muy baja, no hacer raycast
        if (!raycast.shouldCast) {
            return DetectionResult.noObstacle();
        }

        ColliderComponent colA = obj.getComponent(ColliderComponent.class);
        if (colA == null) {
            return DetectionResult.noObstacle();
        }

        // BROADPHASE: crear bounds extendido
        RectangleH currentBounds = colA.getBounds();
        RectangleH extendedBounds = VelocityRaycast.createRaycastBounds(
            currentBounds, raycast);

        // Variables para tracking del obstáculo más cercano
        double nearestDistance = Double.MAX_VALUE;
        GameObjects nearestObj = null;

        // NARROWPHASE: verificar cada objeto candidato
        for (GameObjects other : allObjects) {
            // Filtros básicos
            if (other == obj) continue;
            if (other instanceof Game.Engine.Destroyable d && d.isPendingDestruction()) continue;

            ColliderComponent colB = other.getComponent(ColliderComponent.class);
            if (colB == null) continue;
            
            // ── Refactorización Arquitectónica ────────────────────────────
            // Solo objetos que BLOQUEAN movimiento cuentan como obstáculos.
            // Bullets, player, enemies NO bloquean (blockMovement = false).
            // Muros, suelo, plataformas SÍ bloquean (blockMovement = true).
            if (!colB.blocksMovement()) continue;
            
            // Verificar colisión de capas
            if (!colA.canCollideWith(colB)) continue;

            // Broadphase check
            RectangleH otherBounds = colB.getBounds();
            if (!extendedBounds.intersects(otherBounds)) continue;

            // Calcular distancia PRECISA al obstáculo
            double distance = calculatePreciseDistance(currentBounds, otherBounds, 
                                                      raycast.direction, vx, vy);

            // Actualizar si es el más cercano
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestObj = other;
            }
        }

        // CALCULAR FACTOR DE LIMITACIÓN
        if (nearestObj != null && nearestDistance < plannedMovement) {
            // Hay obstáculo ANTES del destino → limitar movimiento al punto EXACTO
            double limitFactor = nearestDistance / plannedMovement;
            
            // Clampear entre 0.0 y 1.0
            limitFactor = Math.max(0.0, Math.min(1.0, limitFactor));
            
            return DetectionResult.obstacle(nearestDistance, nearestObj, limitFactor);
        } else {
            // No hay obstáculo o está muy lejos → movimiento completo
            return DetectionResult.noObstacle();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: CÁLCULO DE DISTANCIA
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Calcula distancia PRECISA entre dos bounds en la dirección del movimiento.
     *
     * MÉTODO MEJORADO:
     *   Usa SweptAABB simplificado para obtener la distancia REAL al punto de contacto.
     *
     * @param source bounds del objeto que se mueve
     * @param target bounds del obstáculo
     * @param direction dirección normalizada del movimiento
     * @param vx velocidad X completa (no normalizada)
     * @param vy velocidad Y completa (no normalizada)
     * @return distancia precisa al punto de contacto
     */
    private static double calculatePreciseDistance(RectangleH source, 
                                                  RectangleH target, 
                                                  Vector2D direction,
                                                  double vx, double vy) {
        // Usar SweptAABB para calcular tiempo de colisión
        Game.Engine.Colisions.SweptAABB.Result result = 
            Game.Engine.Colisions.SweptAABB.calculate2D(source, target, vx, vy);
        
        if (!result.hasCollision()) {
            return Double.MAX_VALUE;
        }
        
        // Calcular distancia basada en el tiempo de colisión
        // time = 0.0 means ya solapan, time = 1.0 means colisión al final del movimiento
        double movementMagnitude = Math.sqrt(vx * vx + vy * vy);
        double distance = result.time * movementMagnitude;
        
        return distance;
    }

    /**
     * MÉTODO LEGACY - Mantener por compatibilidad
     */
    @Deprecated
    private static double approximateDistance(RectangleH source, 
                                             RectangleH target, 
                                             Vector2D direction) {
        // Centros de los bounds
        double srcCenterX = source.x + source.width * 0.5;
        double srcCenterY = source.y + source.height * 0.5;
        double tgtCenterX = target.x + target.width * 0.5;
        double tgtCenterY = target.y + target.height * 0.5;

        // Vector de separación
        double dx = tgtCenterX - srcCenterX;
        double dy = tgtCenterY - srcCenterY;

        // Proyectar en la dirección del movimiento
        double projectedDistance = dx * direction.getX() + dy * direction.getY();

        // Restar los half-extents para obtener distancia de superficie
        double srcHalfExtent = Math.abs(source.width * direction.getX()) * 0.5 
                             + Math.abs(source.height * direction.getY()) * 0.5;
        double tgtHalfExtent = Math.abs(target.width * direction.getX()) * 0.5 
                             + Math.abs(target.height * direction.getY()) * 0.5;

        return projectedDistance - srcHalfExtent - tgtHalfExtent;
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 3: ANÁLISIS DE RESULTADO
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Determina si se necesitan sub-steps extra basado en la detección.
     *
     * LÓGICA:
     *   - Sin obstáculo → no ajustar
     *   - Obstáculo lejano (> rayLength * 0.5) → no ajustar
     *   - Obstáculo cercano (< rayLength * 0.5) → aumentar sub-steps
     *   - Obstáculo inmediato (< rayLength * 0.25) → máximo sub-steps
     *
     * @param detection resultado de detectInPath()
     * @param raycast raycast usado para la detección
     * @param baseSubsteps sub-steps base del sistema
     * @param maxSubsteps máximo de sub-steps permitido
     * @return número ajustado de sub-steps
     */
    public static int calculateRequiredSubsteps(DetectionResult detection,
                                               VelocityRaycast.RaycastResult raycast,
                                               int baseSubsteps,
                                               int maxSubsteps) {
        if (!detection.hasObstacle) {
            return baseSubsteps;
        }

        double rayLength = raycast.rayLength;
        double distance = detection.distanceToObstacle;

        // Obstáculo lejano → no ajustar
        if (distance > rayLength * 0.5) {
            return baseSubsteps;
        }

        // Obstáculo cercano → escalar sub-steps
        if (distance > rayLength * 0.25) {
            // Interpolar entre base y max
            double factor = 1.0 - (distance / (rayLength * 0.5));
            int extraSteps = (int) ((maxSubsteps - baseSubsteps) * factor);
            return baseSubsteps + extraSteps;
        }

        // Obstáculo inmediato → máximo sub-steps
        return maxSubsteps;
    }

    /**
     * Verifica si un objeto debe usar raycast predictivo.
     *
     * CRITERIO:
     *   - Debe tener ColliderComponent
     *   - Debe estar en movimiento (velocidad > umbral)
     *   - No debe estar pendiente de destrucción
     *
     * @param obj objeto a verificar
     * @param vx velocidad X
     * @param vy velocidad Y
     * @return true si debe usar raycast
     */
    public static boolean shouldUseRaycast(GameObjects obj, double vx, double vy) {
        if (obj instanceof Destroyable d && d.isPendingDestruction()) {
            return false;
        }

        ColliderComponent col = obj.getComponent(ColliderComponent.class);
        if (col == null) {
            return false;
        }

        // Calcular si la velocidad justifica raycast
        VelocityRaycast.RaycastResult raycast = 
            VelocityRaycast.calculateRaycast(vx, vy);
        
        return raycast.shouldCast;
    }
}
