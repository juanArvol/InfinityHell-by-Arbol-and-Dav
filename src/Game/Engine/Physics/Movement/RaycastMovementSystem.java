package Game.Engine.Physics.Movement;

import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Engine.Physics.Predictive.RaycastCollisionDetector;
import Game.Engine.Physics.Predictive.RaycastVisualizationSystem;
import Game.Engine.Physics.Predictive.VelocityRaycast;
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
 *   3. IMPACTAR: Limitar desplazamiento si hay obstáculo en el trayecto
 *   4. Aplicar desplazamiento con PhysicsStepper
 *   5. Mantener estado de raycast por objeto y eje (tracking interno)
 *
 * NO RESPONSABILIDADES:
 *   - Detectar overlaps → CollisionSystem
 *   - Calcular normales de colisión → CollisionSystem + SweptAABB
 *   - Resolver penetraciones → CollisionSystem
 *   - Modificar velocidad → CollisionSystem
 *   - Ejecutar eventos → CollisionSystem
 *
 * ── DIFERENCIA CLAVE: IMPACTO vs COLISIÓN ────────────────────────────────
 *
 * IMPACTO (Este sistema):
 *   - Ocurre ANTES de mover
 *   - Limita DISTANCIA para prevenir tunneling
 *   - Una evaluación por EJE (X e Y independientes)
 *   - Normal simple (dirección del eje)
 *   - NO ejecuta física de colisión
 *
 * COLISIÓN (CollisionsSystem):
 *   - Ocurre DESPUÉS de mover
 *   - Detecta overlaps y calcula física completa
 *   - Usa SweptAABB para geometría precisa
 *   - Normal compleja (TOI, eje de entrada)
 *   - Ejecuta eventos y modifica velocidad
 *
 * ── ARQUITECTURA: UNIVERSAL ───────────────────────────────────────────────
 *
 * A diferencia del diseño anterior donde:
 *   - MovementSystem procesaba bullets (DOD)
 *   - CollisionsSystem procesaba enemies/player (OOP)
 *
 * Ahora:
 *   - RaycastMovementSystem procesa TDO
 *   - CollisionSystem solo detecta y resuelve (no mueve)
 *
 * ── ALGORITMO DE RAYCAST POR EJE ──────────────────────────────────────────
 *
 * Para cada objeto con velocity != 0:
 *
 *   1. Calcular desplazamiento: dx = vx × dt, dy = vy × dt
 *
 *   2. Para cada eje (X, Y) INDEPENDIENTEMENTE:
 *      
 *      a. ¿Velocidad ≥ 1 en ese eje?
 *      b. ¿NO está en contacto previo en ese eje? (state.isContactX/Y == false)
 *      
 *      SÍ → Raycast en ese eje:
 *         - Buscar primer obstáculo en el trayecto
 *         - Limitar distancia al punto EXACTO de contacto
 *         - Marcar raycast activo: state.setObstacleX/Y(obstacle, normal)
 *         - Objeto llega TOCANDO el obstáculo
 *      
 *      NO → Usar desplazamiento completo:
 *         - Si contactX/Y == true: CollisionsSystem maneja el contacto
 *         - Si velocidad < 1: No hay riesgo de tunneling
 *
 *   3. PhysicsStepper.moveWith(dx_final, dy_final)
 *
 *   4. DESPUÉS de mover → updateContactState():
 *      
 *      Para cada eje con obstáculo detectado:
 *      
 *      - ¿Hay velocidad en ese eje (cualquier dirección)?
 *        → SÍ: clearX/Y() → REACTIVAR raycast (necesita evaluar próximo frame)
 *      
 *      - ¿Obstáculo fue destruido o perdió collider?
 *        → SÍ: clearX/Y() → REACTIVAR raycast
 *      
 *      - ¿Ya NO hay overlap?
 *        → SÍ: clearX/Y() → REACTIVAR raycast
 *      
 *      - ¿Sin velocidad Y sigue en contacto?
 *        → SÍ: setContactX/Y(true) → Raycast INACTIVO
 *             CollisionsSystem mantiene el contacto
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
 * Frame N (Primera aproximación):
 *   Bullet en X=50, velocidad=800 px/s, deltaTime=0.016s
 *   Muro en X=60
 *
 *   1. Desplazamiento previsto: dx = 800 × 0.016 = 12.8 px
 *   2. |dx| >= 1 → Evaluar raycast en X
 *   3. Raycast detecta muro a distancia 10.0 px
 *   4. Limitar: dx = 10.0 (punto exacto de contacto)
 *   5. Mover: X = 50 + 10.0 = 60.0 (TOCANDO el muro)
 *   6. Raycast dice: "Ya impactaste, me desactivo"
 *      state.setObstacleX(muro, -1)
 *      state.setContactX(true) → raycast INACTIVO
 *   7. CollisionSystem detecta overlap con bounds.intersects
 *      Calcula normal con SweptAABB
 *      Ejecuta física: vSetX(0), onWall=true, eventos
 *
 * Frame N+1 (Sigue contra el muro):
 *   Bullet en X=60.0, velocidad=800 px/s (IA vuelve a aplicar fuerza)
 *
 *   1. updateContactState() detecta: vx >= 1 → HAY velocidad en X
 *   2. state.clearX() → Raycast se REACTIVA
 *   3. Raycast evalúa nuevamente y limita distancia
 *   4. CollisionsSystem detecta overlap y maneja física
 *
 * Frame N+2 (Sin velocidad):
 *   Bullet en X=60.0, velocidad=0 px/s (behavior detuvo la velocidad)
 *
 *   1. updateContactState() detecta: vx == 0 → SIN velocidad
 *   2. state.setContactX(true) → Raycast INACTIVO
 *   3. Bullet permanece tocando el muro sin evaluar raycast
 *
 * Frame N+3 (Velocity en cualquier dirección):
 *   Bullet en X=60.0, velocidad=-100 px/s (alejándose) o +50 px/s (hacia muro)
 *
 *   1. updateContactState() detecta: |vx| >= 1 → HAY velocidad
 *   2. state.clearX() → Raycast se REACTIVA
 *   3. Próximo frame evalúa raycast nuevamente
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
 *   - Hay velocidad en ese eje (|v| >= 1)
 *   - El obstáculo es destruido o pierde collider
 *   - Ya no hay overlap físico
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
                    
                    // Calcular normal: dirección desde la que viene el impacto
                    int normalX = (dx > 0) ? -1 : 1;
                    
                    // Limitar al punto EXACTO de contacto (sin overlap artificial)
                    // RectangleH.intersects() usa <= por lo que detectará contacto borde-a-borde
                    if (Math.abs(distanceX) < Math.abs(dx)) {
                        dx = distanceX;
                        state.setObstacleX(obstacleX, normalX);
                        
                        // Registrar visualización si está activa
                        if (VelocityRaycast.SHOW_RAYCASTS) {
                            registerVisualization(obj, collider, dx, dy, obstacleX, distanceX, normalX, 0);
                        }
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
                    
                    // Calcular normal: dirección desde la que viene el impacto
                    int normalY = (dy > 0) ? -1 : 1;
                    
                    // Limitar al punto EXACTO de contacto (sin overlap artificial)
                    // RectangleH.intersects() usa <= por lo que detectará contacto borde-a-borde
                    if (Math.abs(distanceY) < Math.abs(dy)) {
                        dy = distanceY;
                        state.setObstacleY(obstacleY, normalY);
                        
                        // Registrar visualización si está activa
                        if (VelocityRaycast.SHOW_RAYCASTS) {
                            registerVisualization(obj, collider, dx, dy, obstacleY, distanceY, 0, normalY);
                        }
                    }
                }
            }

            // ── APLICAR MOVIMIENTO ────────────────────────────────────────
            // Movemos con los valores limitados por raycast (punto EXACTO de contacto).
            // 
            // SEPARACIÓN DE RESPONSABILIDADES:
            //   - Raycast IMPACTA: Limita dx/dy para prevenir tunneling
            //   - CollisionsSystem COLISIONA: Detecta overlap y calcula física completa
            //   
            // CollisionsSystem calculará sus propias normales con SweptAABB retrospectivo.
            // Las normales de RaycastState son para tracking interno, NO para física.
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
     * Registra el raycast para visualización debug.
     */
    private void registerVisualization(GameObjects obj, ColliderComponent collider,
                                      double dx, double dy, GameObjects obstacle, 
                                      double distance, int normalX, int normalY) {
        double objX = obj.getTransform().getPosition().getX();
        double objY = obj.getTransform().getPosition().getY();
        
        // Calcular raycast para visualización
        VelocityRaycast.RaycastResult raycast = VelocityRaycast.calculateRaycast(dx, dy);
        
        // Crear detectionResult para visualización
        double limitFactor = distance / Math.max(Math.abs(dx), Math.abs(dy));
        RaycastCollisionDetector.DetectionResult detection = 
            RaycastCollisionDetector.DetectionResult.obstacle(distance, obstacle, limitFactor);
        
        // Registrar en el sistema de visualización
        RaycastVisualizationSystem.registerRaycast(obj, objX, objY, raycast, detection);
    }

    /**
     * Actualiza el estado de contacto basado en overlaps actuales.
     * 
     * FLUJO CORRECTO:
     * 1. Raycast IMPACTA → objeto llega hasta obstáculo → raycast se DESACTIVA
     * 2. CollisionsSystem COLISIONA → mantiene contacto, anula velocity
     * 3. Raycast solo se REACTIVA si velocity cambia de dirección (alejándose)
     * 
     * NO se reactiva simplemente porque hay velocity hacia el obstáculo.
     * CollisionsSystem maneja eso.
     */
    private void updateContactState(GameObjects obj, ColliderComponent collider, RaycastState state) {
        RectangleH bounds = collider.getBounds();
        
        // Obtener velocity actual para detectar cambio de dirección
        Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
        if (physComp == null) return;
        
        double vx = physComp.getPhysics().getVelocity().getX();
        double vy = physComp.getPhysics().getVelocity().getY();

        // ── Verificar estado en X ─────────────────────────────────────────
        if (state.hasObstacleX()) {
            GameObjects obstacleX = state.getObstacleX();
            
            // REACTIVAR raycast SI:
            // 1. Hay velocidad en X (cualquier dirección) → necesita evaluar raycast
            // 2. Obstáculo fue destruido
            // 3. Obstáculo perdió su collider
            // 4. Ya no hay overlap físico
            
            boolean hasVelocityX = Math.abs(vx) >= VELOCITY_THRESHOLD;
            
            if (hasVelocityX) {
                // Hay velocidad → REACTIVAR raycast para próximo frame
                state.clearX();
            } else if (obstacleX instanceof Destroyable d && d.isPendingDestruction()) {
                state.clearX();
            } else {
                ColliderComponent obstacleCol = obstacleX.getComponent(ColliderComponent.class);
                
                if (obstacleCol == null) {
                    state.clearX();
                } else {
                    RectangleH obstacleBounds = obstacleCol.getBounds();
                    
                    if (!bounds.intersects(obstacleBounds)) {
                        state.clearX();
                    } else {
                        // Sin velocidad Y sigue en contacto → raycast INACTIVO
                        state.setContactX(true);
                    }
                }
            }
        }

        // ── Verificar estado en Y ─────────────────────────────────────────
        if (state.hasObstacleY()) {
            GameObjects obstacleY = state.getObstacleY();
            
            boolean hasVelocityY = Math.abs(vy) >= VELOCITY_THRESHOLD;
            
            if (hasVelocityY) {
                state.clearY();
            } else if (obstacleY instanceof Destroyable d && d.isPendingDestruction()) {
                state.clearY();
            } else {
                ColliderComponent obstacleCol = obstacleY.getComponent(ColliderComponent.class);
                
                if (obstacleCol == null) {
                    state.clearY();
                } else {
                    RectangleH obstacleBounds = obstacleCol.getBounds();
                    
                    if (!bounds.intersects(obstacleBounds)) {
                        state.clearY();
                    } else {
                        state.setContactY(true);
                    }
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
     * Limpia TDO  el estado. Llamar al cambiar de mundo/escena.
     */
    public void clear() {
        raycastStates.clear();
    }
    
    /**
     * Obtiene el estado de raycast para un objeto.
     * Retorna null si el objeto no tiene estado (nunca usó raycast).
     * 
     * USADO POR: CollisionsSystem para obtener normales calculadas por raycast.
     */
    public RaycastState getState(GameObjects obj) {
        return raycastStates.get(obj);
    }
}
