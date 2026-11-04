package Game.Engine.Physics.Predictive;

import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PredictiveMovementLimiter — Coordinador de Limitación Predictiva de Movimiento
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ── ARQUITECTURA: SEPARACIÓN DE RESPONSABILIDADES ────────────────────────
 *
 * Esta clase es el **COORDINADOR** entre la detección predictiva y el sistema
 * de movimiento. Proporciona una interfaz limpia para consultar limitaciones
 * de movimiento sin exponer detalles de implementación.
 *
 * RESPONSABILIDADES:
 *   1. Calcular desplazamiento previsto desde velocidad + deltaTime
 *   2. Invocar detección predictiva (RaycastCollisionDetector)
 *   3. Aplicar factor de limitación al desplazamiento
 *   4. Proporcionar resultado estructurado para MovementSystem
 *
 * NO RESPONSABILIDADES:
 *   - Aplicar movimiento → MovementSystem / PhysicsStepper
 *   - Buscar obstáculos → RaycastCollisionDetector
 *   - Cálculos geométricos → RectangleH / SweptAABB
 *   - Resolver colisiones → CollisionSystem
 *   - Modificar velocidad → CollisionSystem
 *
 * ── FLUJO DE USO ──────────────────────────────────────────────────────────
 *
 * CollisionsSystem (FASE 1):
 *   1. Obtiene velocidad de Physics2DComponent
 *   2. Llama: limitMovement(obj, velocity, deltaTime, worldObjects)
 *   3. Recibe LimitedMovement con desplazamiento ajustado
 *   4. Aplica el desplazamiento con PhysicsStepper.moveWith()
 *   5. Continúa con detección swept convencional
 *
 * EJEMPLO:
 *   Physics2D physics = obj.getComponent(Physics2DComponent.class).getPhysics();
 *   Vector2D velocity = physics.getVelocity();
 *   
 *   LimitedMovement limited = PredictiveMovementLimiter.limitMovement(
 *       obj, velocity, deltaTime, allObjects
 *   );
 *   
 *   PhysicsStepper.moveWith(obj, limited.displacementX, limited.displacementY);
 *
 * ── CONVIVENCIA CON SWEPTAABB ─────────────────────────────────────────────
 *
 * PredictiveMovementLimiter y CollisionSystem (SweptAABB) son COMPLEMENTARIOS:
 *
 * FASE 0.6 — Limitación Predictiva (ESTE COMPONENTE):
 *   - Propósito: evitar tunneling por desplazamientos extremos
 *   - Timing: ANTES de mover el objeto
 *   - Resultado: desplazamiento limitado
 *   - No modifica estado del mundo
 *
 * FASE 1 — Detección y Resolución Swept (CollisionSystem):
 *   - Propósito: detectar contactos, resolver físicamente
 *   - Timing: DESPUÉS de mover el objeto (con desplazamiento limitado)
 *   - Resultado: normales, eventos, modificación de velocidad
 *   - Modifica estado físico del mundo
 *
 * ── GARANTÍA MATEMÁTICA ───────────────────────────────────────────────────
 *
 * Si limitFactor < 1.0:
 *   - El objeto NO alcanzará el obstáculo (se detiene antes)
 *   - SweptAABB en CollisionSystem PUEDE o NO detectar contacto
 *   - Si detecta, es para resolver penetraciones residuales
 *
 * Si limitFactor == 1.0:
 *   - No hay obstáculo en el path previsto
 *   - SweptAABB operará normalmente
 *
 * ── PRECISION NUMÉRICA ────────────────────────────────────────────────────
 *
 * Soporta velocidades extremadamente altas (tested hasta 10^11 unidades).
 * Los cálculos usan double precision y RectangleH.
 * Las limitaciones están en la representación de double (~1e15 precision).
 *
 * Si las coordenadas exceden Double.MAX_VALUE, el comportamiento es undefined
 * (esto requeriría objetos a distancias astronómicas del origen).
 */
public final class PredictiveMovementLimiter {

    /**
     * Resultado de limitación de movimiento.
     * 
     * Contiene el desplazamiento final (posiblemente limitado) que debe
     * aplicarse al objeto, junto con información sobre el obstáculo detectado.
     */
    public static final class LimitedMovement {
        /** Desplazamiento horizontal final a aplicar (unidades de mundo) */
        public final double displacementX;
        
        /** Desplazamiento vertical final a aplicar (unidades de mundo) */
        public final double displacementY;
        
        /** ¿Se detectó un obstáculo que limitó el movimiento? */
        public final boolean wasLimited;
        
        /** Factor de limitación aplicado (0.0 = detenido, 1.0 = sin limitación) */
        public final double limitFactor;
        
        /** Objeto obstáculo que causó la limitación (null si no hay) */
        public final GameObjects obstacle;
        
        /** Distancia al obstáculo (unidades de mundo, Double.MAX_VALUE si no hay) */
        public final double distanceToObstacle;

        private LimitedMovement(double dx, double dy, boolean limited, 
                               double factor, GameObjects obstacle, double distance) {
            this.displacementX = dx;
            this.displacementY = dy;
            this.wasLimited = limited;
            this.limitFactor = factor;
            this.obstacle = obstacle;
            this.distanceToObstacle = distance;
        }

        /**
         * Crea un resultado sin limitación (movimiento completo permitido).
         */
        public static LimitedMovement unlimited(double dx, double dy) {
            return new LimitedMovement(dx, dy, false, 1.0, null, Double.MAX_VALUE);
        }

        /**
         * Crea un resultado con limitación.
         */
        public static LimitedMovement limited(double dx, double dy, double factor, 
                                             GameObjects obstacle, double distance) {
            return new LimitedMovement(dx, dy, true, factor, obstacle, distance);
        }
    }

    // Constructor privado — clase de utilidades
    private PredictiveMovementLimiter() {}

    /**
     * Calcula el desplazamiento permitido para un objeto considerando obstáculos.
     *
     * Este es el punto de entrada principal para la limitación predictiva.
     * Calcula el desplazamiento previsto, consulta RaycastCollisionDetector,
     * y retorna el desplazamiento ajustado.
     *
     * CONTRATO DIMENSIONAL:
     *   velocity   → px/s (unidades por segundo)
     *   deltaTime  → s (segundos)
     *   displacement → px (unidades espaciales)
     *
     * GARANTÍA:
     *   El desplazamiento retornado NUNCA excede el previsto original.
     *   0.0 ≤ limitFactor ≤ 1.0
     *
     * @param obj objeto que se va a mover
     * @param velocity velocidad actual del objeto (px/s)
     * @param deltaTime tiempo del simulation step (s)
     * @param allObjects lista de todos los objetos del mundo
     * @return LimitedMovement con desplazamiento ajustado
     */
    public static LimitedMovement limitMovement(GameObjects obj,
                                               Vector2D velocity,
                                               double deltaTime,
                                               List<GameObjects> allObjects) {
        
        // Calcular desplazamiento previsto
        double plannedDX = velocity.getX() * deltaTime;
        double plannedDY = velocity.getY() * deltaTime;
        
        // Magnitud del desplazamiento previsto
        double plannedMagnitude = Math.sqrt(plannedDX * plannedDX + plannedDY * plannedDY);
        
        // Si no hay movimiento, retornar sin limitación
        if (plannedMagnitude < 1e-9) {
            return LimitedMovement.unlimited(0.0, 0.0);
        }

        // Consultar detección predictiva
        RaycastCollisionDetector.DetectionResult detection = 
            RaycastCollisionDetector.detectAndLimitMovement(
                obj, plannedDX, plannedDY, allObjects
            );

        // Si no hay obstáculo o el obstáculo está muy lejos, permitir movimiento completo
        if (!detection.hasObstacle || detection.movementLimitFactor >= 1.0) {
            return LimitedMovement.unlimited(plannedDX, plannedDY);
        }

        // Aplicar limitación
        double limitedDX = plannedDX * detection.movementLimitFactor;
        double limitedDY = plannedDY * detection.movementLimitFactor;

        return LimitedMovement.limited(
            limitedDX, 
            limitedDY, 
            detection.movementLimitFactor,
            detection.nearestObject,
            detection.distanceToObstacle
        );
    }

    /**
     * Variante que acepta Physics2DComponent directamente.
     *
     * Conveniencia para llamadores que ya tienen el componente de física.
     */
    public static LimitedMovement limitMovement(GameObjects obj,
                                               Physics2DComponent physicsComponent,
                                               double deltaTime,
                                               List<GameObjects> allObjects) {
        Physics2D physics = physicsComponent.getPhysics();
        return limitMovement(obj, physics.getVelocity(), deltaTime, allObjects);
    }

    /**
     * Verifica si un objeto debería usar limitación predictiva.
     *
     * CRITERIOS:
     *   - Debe tener ColliderComponent
     *   - No debe estar pendiente de destrucción
     *   - La velocidad debe justificar consulta (configurable en RaycastConfig)
     *
     * @param obj objeto a verificar
     * @param velocity velocidad actual
     * @param deltaTime tiempo del step
     * @return true si debe usar limitación predictiva
     */
    public static boolean shouldLimit(GameObjects obj, Vector2D velocity, double deltaTime) {
        // Objetos destruyéndose no necesitan limitación
        if (obj instanceof Destroyable d && d.isPendingDestruction()) {
            return false;
        }

        // Debe tener collider
        ColliderComponent col = obj.getComponent(ColliderComponent.class);
        if (col == null) {
            return false;
        }

        // Calcular desplazamiento previsto
        double dx = velocity.getX() * deltaTime;
        double dy = velocity.getY() * deltaTime;

        // Verificar si la velocidad justifica raycast
        VelocityRaycast.RaycastResult raycast = VelocityRaycast.calculateRaycast(dx, dy);
        return raycast.shouldCast;
    }

    /**
     * Variante que acepta Physics2DComponent.
     */
    public static boolean shouldLimit(GameObjects obj, Physics2DComponent physicsComponent, 
                                     double deltaTime) {
        Physics2D physics = physicsComponent.getPhysics();
        return shouldLimit(obj, physics.getVelocity(), deltaTime);
    }
}
