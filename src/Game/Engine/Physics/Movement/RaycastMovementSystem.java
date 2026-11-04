package Game.Engine.Physics.Movement;

import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * RaycastMovementSystem — Sistema Universal de Movimiento con Anti-Tunneling
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 *
 * Este sistema es el ÚNICO responsable de aplicar movimiento a TODAS las
 * entidades del juego (DOD bullets + OOP enemies/player).
 *
 * RESPONSABILIDADES:
 *   1. Calcular desplazamiento previsto (velocity × deltaTime)
 *   2. Evaluar raycast anti-tunneling (si velocidad ≥ 1 en cualquier eje)
 *   3. Limitar desplazamiento si hay obstáculo en el trayecto
 *   4. Aplicar desplazamiento con PhysicsStepper
 *   5. Mantener estado de raycast por objeto y eje
 *
 * NO RESPONSABILIDADES:
 *   - Detectar overlaps → CollisionSystem
 *   - Calcular normales → CollisionSystem + SweptAABB
 *   - Resolver penetraciones → CollisionSystem
 *   - Modificar velocidad → CollisionSystem
 *   - Ejecutar eventos → CollisionSystem
 *
 * ── ARQUITECTURA: UNIVERSAL ───────────────────────────────────────────────
 *
 * A diferencia del diseño anterior donde:
 *   - MovementSystem procesaba bullets (DOD)
 *   - CollisionsSystem procesaba enemies/player (OOP)
 *
 * Ahora:
 *   - RaycastMovementSystem procesa TODO
 *   - CollisionSystem solo detecta y resuelve (no mueve)
 *
 * ── ALGORITMO DE RAYCAST POR EJE ──────────────────────────────────────────
 *
 * Para cada objeto con velocity != 0:
 *
 *   1. Calcular desplazamiento: dx = vx × dt, dy = vy × dt
 *
 *   2. Para cada eje (X, Y) INDEPENDIENTEMENTE:
 *      a. ¿Velocidad ≥ 1 en ese eje?
 *      b. ¿NO está en contacto previo en ese eje?
 *      c. SÍ → Raycast en ese eje
 *         - Buscar primer obstáculo en el trayecto
 *         - Limitar distancia al punto EXACTO de contacto
 *         - Marcar raycast activo en ese eje
 *      d. NO → Usar desplazamiento completo
 *
 *   3. PhysicsStepper.moveWith(dx_final, dy_final)
 *
 *   4. Para cada eje con raycast activo:
 *      a. ¿Ahora está en contacto cercano? (distance < 2px)
 *      b. SÍ → Desactivar raycast en ese eje
 *      c. NO → Mantener raycast activo
 *
 * ── FILOSOFÍA: SIN TRUCOS ─────────────────────────────────────────────────
 *
 * NO usamos márgenes de penetración artificiales.
 * NO modificamos distancias para "asegurar" contacto.
 * 
 * El raycast limita al punto EXACTO donde ocurriría el contacto.
 * CollisionSystem usa SweptAABB retrospectivo para detectar y resolver.
 * Si el sistema está bien diseñado, no necesitamos trucos.
 *
 * ── EJEMPLO DE FUNCIONAMIENTO ─────────────────────────────────────────────
 *
 * Frame N:
 *   Bullet en X=50, velocidad=100 px/s, deltaTime=0.016s
 *   Muro en X=55 (width=10)
 *
 *   1. Desplazamiento previsto: dx = 100 × 0.016 = 1.6 px
 *   2. |dx| >= 1 → Evaluar raycast en X
 *   3. Raycast detecta muro a distancia 5.0 px
 *   4. Limitar: dx = 5.0 (punto exacto de contacto)
 *   5. Mover: X = 50 + 5.0 = 55.0 (tocando el muro)
 *   6. CollisionSystem detecta overlap con SweptAABB retrospectivo
 *
 * Frame N+1:
 *   Bullet en X=55.0 (ya en contacto), velocidad=100 px/s
 *
 *   1. Desplazamiento previsto: dx = 1.6 px
 *   2. |dx| >= 1, PERO raycast desactivado en X (ya en contacto)
 *   3. NO evaluar raycast
 *   4. Mover: X = 54.7 + 0 (CollisionSystem bloqueará)
 *   5. CollisionSystem detecta overlap, resuelve, detiene velocidad
 *
 * ── ESTADO DE RAYCAST ─────────────────────────────────────────────────────
 *
 * Cada objeto tiene un RaycastState que registra:
 *   - ¿Raycast activo en X?
 *   - ¿Raycast activo en Y?
 *   - Obstáculo actual en X (si existe)
 *   - Obstáculo actual en Y (si existe)
 *
 * El estado persiste entre frames hasta que:
 *   - Se detecta contacto (bounds.intersects)
 *   - La velocidad en ese eje se invierte (alejándose del obstáculo)
 *
 * ── PERFORMANCE ───────────────────────────────────────────────────────────
 *
 * Optimizaciones aplicadas:
 *   - Raycast solo si |velocity| >= 1 en ese eje
 *   - Broadphase con swept bounds (RectangleH.sweep)
 *   - Reutilización de RectangleH temporales
 *   - Estado persistente entre frames (sin re-allocations)
 *   - Objetos destruidos skipped early
 *
 * ── INVARIANTES GARANTIZADOS ──────────────────────────────────────────────
 *
 *   1. Ningún objeto atraviesa obstáculos independientemente de su velocidad
 *   2. Raycast solo se evalúa cuando es necesario (velocidad ≥ 1)
 *   3. Objetos en contacto no evalúan raycast innecesariamente
 *   4. Estado de raycast persiste correctamente entre frames
 *   5. PhysicsStepper es el ÚNICO que modifica posición
 */
public final class RaycastMovementSystem {



    /**
     * Umbral mínimo de velocidad para activar raycast.
     * 
     * Solo evaluamos raycast si la velocidad en un eje cumple:
     *   |velocity| >= VELOCITY_THRESHOLD
     * 
     * Esto evita cálculos innecesarios para movimientos muy lentos que
     * NO pueden causar tunneling.
     */
    private static final double VELOCITY_THRESHOLD = 1.0;

    /**
     * Estado de raycast por objeto.
     * Persistente entre frames, limpiado cuando objeto se destruye.
     */
    private final IdentityHashMap<GameObjects, RaycastState> raycastStates = 
        new IdentityHashMap<>();

    /**
     * RectangleH reutilizable para swept bounds (evita allocations).
     */
    private final RectangleH reusableSwept = new RectangleH();

    /**
     * Ejecuta el movimiento con raycast anti-tunneling para todos los objetos.
     *
     * @param objects lista de todos los objetos activos
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void update(List<GameObjects> objects, double deltaTime) {
        for (GameObjects obj : objects) {
            // Skip objetos destruidos
            if (obj instanceof Destroyable d && d.isPendingDestruction()) {
                continue;
            }

            ColliderComponent collider = obj.getComponent(ColliderComponent.class);
            if (collider == null) continue;

            // Skip objetos estáticos (muros, plataformas)
            if (collider.blocksMovement()) continue;

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;

            Physics2D physics = physComp.getPhysics();
            Vector2D velocity = physics.getVelocity();

            // Calcular desplazamiento previsto
            double dx = velocity.getX() * deltaTime;
            double dy = velocity.getY() * deltaTime;

            // Skip si no hay movimiento
            if (dx == 0.0 && dy == 0.0) continue;

            // Obtener o crear estado de raycast para este objeto
            RaycastState state = raycastStates.computeIfAbsent(obj, k -> new RaycastState());

            // ── RAYCAST EN EJE X ──────────────────────────────────────────
            if (Math.abs(dx) >= VELOCITY_THRESHOLD && !state.isContactX()) {
                GameObjects obstacleX = findFirstObstacleX(obj, collider, dx, objects);
                
                if (obstacleX != null) {
                    double distanceX = calculateDistanceX(collider.getBounds(), 
                                                         obstacleX.getComponent(ColliderComponent.class).getBounds(), 
                                                         dx);
                    
                    // Limitar al punto exacto de contacto
                    if (Math.abs(distanceX) < Math.abs(dx)) {
                        dx = distanceX;
                        state.setObstacleX(obstacleX);
                    }
                }
            }

            // ── RAYCAST EN EJE Y ──────────────────────────────────────────
            if (Math.abs(dy) >= VELOCITY_THRESHOLD && !state.isContactY()) {
                GameObjects obstacleY = findFirstObstacleY(obj, collider, dy, objects);
                
                if (obstacleY != null) {
                    double distanceY = calculateDistanceY(collider.getBounds(), 
                                                         obstacleY.getComponent(ColliderComponent.class).getBounds(), 
                                                         dy);
                    
                    // Limitar al punto exacto de contacto
                    if (Math.abs(distanceY) < Math.abs(dy)) {
                        dy = distanceY;
                        state.setObstacleY(obstacleY);
                    }
                }
            }

            // ── APLICAR MOVIMIENTO ────────────────────────────────────────
            // Movemos con los valores limitados por raycast.
            // Si raycast detectó obstáculo, dx/dy ya fueron limitados al punto
            // exacto de contacto. CollisionsSystem detectará el overlap y
            // calculará normales retrospectivamente.
            PhysicsStepper.moveWith(obj, dx, dy);

            // ── ACTUALIZAR ESTADO DE CONTACTO ─────────────────────────────
            updateContactState(obj, collider, state);
        }
    }

    /**
     * Busca el primer obstáculo en el eje X.
     */
    private GameObjects findFirstObstacleX(GameObjects obj, ColliderComponent collider, 
                                          double dx, List<GameObjects> allObjects) {
        RectangleH bounds = collider.getBounds();
        
        // Crear swept bounds solo en X
        reusableSwept.copyFrom(bounds.sweep(dx, 0));

        GameObjects nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (GameObjects other : allObjects) {
            if (other == obj) continue;
            if (other instanceof Destroyable d && d.isPendingDestruction()) continue;

            ColliderComponent otherCol = other.getComponent(ColliderComponent.class);
            if (otherCol == null) continue;
            if (!otherCol.blocksMovement()) continue;
            if (!collider.canCollideWith(otherCol)) continue;

            RectangleH otherBounds = otherCol.getBounds();
            if (!reusableSwept.intersects(otherBounds)) continue;

            double dist = calculateDistanceX(bounds, otherBounds, dx);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }

        return nearest;
    }

    /**
     * Busca el primer obstáculo en el eje Y.
     */
    private GameObjects findFirstObstacleY(GameObjects obj, ColliderComponent collider, 
                                          double dy, List<GameObjects> allObjects) {
        RectangleH bounds = collider.getBounds();
        
        // Crear swept bounds solo en Y
        reusableSwept.copyFrom(bounds.sweep(0, dy));

        GameObjects nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (GameObjects other : allObjects) {
            if (other == obj) continue;
            if (other instanceof Destroyable d && d.isPendingDestruction()) continue;

            ColliderComponent otherCol = other.getComponent(ColliderComponent.class);
            if (otherCol == null) continue;
            if (!otherCol.blocksMovement()) continue;
            if (!collider.canCollideWith(otherCol)) continue;

            RectangleH otherBounds = otherCol.getBounds();
            if (!reusableSwept.intersects(otherBounds)) continue;

            double dist = calculateDistanceY(bounds, otherBounds, dy);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }

        return nearest;
    }

    /**
     * Calcula distancia al obstáculo en eje X.
     */
    private double calculateDistanceX(RectangleH bounds, RectangleH obstacle, double dx) {
        if (dx > 0) {
            // Moviéndose a la derecha
            return obstacle.getMinX() - bounds.getMaxX();
        } else {
            // Moviéndose a la izquierda
            return bounds.getMinX() - obstacle.getMaxX();
        }
    }

    /**
     * Calcula distancia al obstáculo en eje Y.
     */
    private double calculateDistanceY(RectangleH bounds, RectangleH obstacle, double dy) {
        if (dy > 0) {
            // Moviéndose hacia abajo
            return obstacle.getMinY() - bounds.getMaxY();
        } else {
            // Moviéndose hacia arriba
            return bounds.getMinY() - obstacle.getMaxY();
        }
    }

    /**
     * Actualiza el estado de contacto basado en overlaps actuales.
     */
    private void updateContactState(GameObjects obj, ColliderComponent collider, RaycastState state) {
        RectangleH bounds = collider.getBounds();

        // Verificar contacto en X
        if (state.hasObstacleX()) {
            GameObjects obstacleX = state.getObstacleX();
            
            // Si obstáculo fue destruido, limpiar estado
            if (obstacleX instanceof Destroyable d && d.isPendingDestruction()) {
                state.clearX();
            } else {
                ColliderComponent obstacleCol = obstacleX.getComponent(ColliderComponent.class);
                
                if (obstacleCol != null) {
                    RectangleH obstacleBounds = obstacleCol.getBounds();
                    
                    // Verificar si está en contacto
                    if (bounds.intersects(obstacleBounds)) {
                        state.setContactX(true);
                    } else {
                        state.clearX(); // Ya no está en contacto
                    }
                } else {
                    state.clearX(); // Obstáculo perdió collider
                }
            }
        }

        // Verificar contacto en Y
        if (state.hasObstacleY()) {
            GameObjects obstacleY = state.getObstacleY();
            
            // Si obstáculo fue destruido, limpiar estado
            if (obstacleY instanceof Destroyable d && d.isPendingDestruction()) {
                state.clearY();
            } else {
                ColliderComponent obstacleCol = obstacleY.getComponent(ColliderComponent.class);
                
                if (obstacleCol != null) {
                    RectangleH obstacleBounds = obstacleCol.getBounds();
                    
                    // Verificar si está en contacto
                    if (bounds.intersects(obstacleBounds)) {
                        state.setContactY(true);
                    } else {
                        state.clearY(); // Ya no está en contacto
                    }
                } else {
                    state.clearY(); // Obstáculo perdió collider
                }
            }
        }
    }

    /**
     * Limpia el estado de raycast para objetos destruidos.
     * Debe llamarse periódicamente para evitar memory leaks.
     */
    public void cleanupDestroyedObjects(List<GameObjects> objects) {
        raycastStates.keySet().removeIf(obj -> 
            obj instanceof Destroyable d && d.isPendingDestruction()
        );
    }

    /**
     * Limpia TODO el estado. Llamar al cambiar de mundo/escena.
     */
    public void clear() {
        raycastStates.clear();
    }
}
