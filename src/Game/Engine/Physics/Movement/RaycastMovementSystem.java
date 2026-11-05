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
 *   7. CollisionSystem detecta contacto con bounds.intersectsInclusive()
 *      Calcula normal con SweptAABB
 *      Propaga: ContactState, normals, eventos
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
            
            // DEBUG: Log velocity del Player
            if (obj instanceof Game.Player.Player) {
                System.out.printf("[DEBUG] Player velocity: vx=%.2f vy=%.2f → dx=%.2f dy=%.2f%n", 
                    velocity.getX(), velocity.getY(), dx, dy);
            }

            // Skip si no hay movimiento
            if (dx == 0.0 && dy == 0.0) continue;

            // Obtener o crear estado de raycast para este objeto
            RaycastState state = raycastStates.computeIfAbsent(obj, k -> new RaycastState());

            // ── RAYCAST EN EJE X ──────────────────────────────────────────
            // ✅ FIX: Evaluar raycast si:
            //   1. NO hay contacto (!state.isContactX()), O
            //   2. HAY contacto PERO velocidad en dirección OPUESTA (alejándose)
            boolean shouldEvaluateRaycastX = !state.isContactX();
            if (!shouldEvaluateRaycastX && state.hasObstacleX()) {
                // Hay contacto - verificar si velocity se aleja del obstáculo
                int contactNormal = state.getNormalX();
                // Si contactNormal = -1 (muro izquierdo), velocity negativa = alejándose
                // Si contactNormal = 1 (muro derecho), velocity positiva = alejándose
                boolean movingAwayFromContact = (dx < 0 && contactNormal == -1) || (dx > 0 && contactNormal == 1);
                shouldEvaluateRaycastX = movingAwayFromContact;
            }
            
            if (Math.abs(dx) >= VELOCITY_THRESHOLD && shouldEvaluateRaycastX) {
                GameObjects obstacleX = findFirstObstacleX(obj, collider, dx, objects);
                
                double distanceX = Math.abs(dx); // Distancia por defecto (sin obstáculo)
                int normalX = (dx > 0) ? -1 : 1;
                
                if (obstacleX != null) {
                    distanceX = calculateDistanceX(collider.getBounds(), 
                                                  obstacleX.getComponent(ColliderComponent.class).getBounds(), 
                                                  dx);
                    
                    // Limitar al punto EXACTO de contacto
                    if (Math.abs(distanceX) < Math.abs(dx)) {
                        dx = distanceX;
                        state.setObstacleX(obstacleX, normalX);
                    }
                }
                
                // ✅ CORRECCIÓN: Registrar visualización SIEMPRE que raycast se evalúe
                // Independientemente de si hay obstáculo o no
                if (VelocityRaycast.SHOW_RAYCASTS) {
                    registerVisualization(obj, collider, dx, dy, obstacleX, distanceX, normalX, 0);
                }
            } else if (state.hasObstacleX() && VelocityRaycast.SHOW_RAYCASTS) {
                // Visualización de contacto persistente
                GameObjects obstacleX = state.getObstacleX();
                int normalX = state.getNormalX();
                
                ColliderComponent obstacleCol = obstacleX.getComponent(ColliderComponent.class);
                if (obstacleCol != null) {
                    double distanceX = calculateDistanceX(collider.getBounds(), 
                                                         obstacleCol.getBounds(), 
                                                         dx);
                    registerVisualization(obj, collider, 0, dy, obstacleX, distanceX, normalX, 0);
                }
            }

            // ── RAYCAST EN EJE Y ──────────────────────────────────────────
            // ✅ FIX: Evaluar raycast si:
            //   1. NO hay contacto (!state.isContactY()), O
            //   2. HAY contacto PERO velocidad en dirección OPUESTA (alejándose)
            boolean shouldEvaluateRaycastY = !state.isContactY();
            if (!shouldEvaluateRaycastY && state.hasObstacleY()) {
                // Hay contacto - verificar si velocity se aleja del obstáculo
                int contactNormal = state.getNormalY();
                // Si contactNormal = -1 (suelo), velocity negativa (hacia arriba) = alejándose
                // Si contactNormal = 1 (techo), velocity positiva (hacia abajo) = alejándose
                boolean movingAwayFromContact = (dy < 0 && contactNormal == -1) || (dy > 0 && contactNormal == 1);
                shouldEvaluateRaycastY = movingAwayFromContact;
            }
            
            if (Math.abs(dy) >= VELOCITY_THRESHOLD && shouldEvaluateRaycastY) {
                GameObjects obstacleY = findFirstObstacleY(obj, collider, dy, objects);
                
                double distanceY = Math.abs(dy); // Distancia por defecto (sin obstáculo)
                int normalY = (dy > 0) ? -1 : 1;
                
                if (obstacleY != null) {
                    distanceY = calculateDistanceY(collider.getBounds(), 
                                                  obstacleY.getComponent(ColliderComponent.class).getBounds(), 
                                                  dy);
                    
                    // DEBUG: Log cuando se limita
                    boolean wasLimited = Math.abs(distanceY) < Math.abs(dy);
                    if (wasLimited && obj instanceof Game.Player.Player) {
                        System.out.printf("[RAYCAST-Y] Player dy=%.2f limited to=%.2f (obstacle detected)%n", dy, distanceY);
                    }
                    
                    // Limitar al punto EXACTO de contacto
                    if (wasLimited) {
                        dy = distanceY;
                        state.setObstacleY(obstacleY, normalY);
                    }
                } else if (obj instanceof Game.Player.Player) {
                    System.out.printf("[RAYCAST-Y] Player dy=%.2f NO OBSTACLE (free movement)%n", dy);
                }
                
                // ✅ CORRECCIÓN: Registrar visualización SIEMPRE que raycast se evalúe
                // Independientemente de si hay obstáculo o no
                if (VelocityRaycast.SHOW_RAYCASTS) {
                    registerVisualization(obj, collider, dx, dy, obstacleY, distanceY, 0, normalY);
                }
            } else if (state.hasObstacleY() && VelocityRaycast.SHOW_RAYCASTS) {
                // Visualización de contacto persistente en Y
                GameObjects obstacleY = state.getObstacleY();
                int normalY = state.getNormalY();
                
                ColliderComponent obstacleCol = obstacleY.getComponent(ColliderComponent.class);
                if (obstacleCol != null) {
                    double distanceY = calculateDistanceY(collider.getBounds(), 
                                                         obstacleCol.getBounds(), 
                                                         dy);
                    registerVisualization(obj, collider, dx, 0, obstacleY, distanceY, 0, normalY);
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
     * 
     * ✅ CORRECCIÓN ESQUINAS: También considera obstáculos con los que YA está
     * en contacto (de state) para prevenir tunneling al "rodear" esquinas.
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
            
            // ✅ NUEVO: También considerar si YA está en contacto (intersectsInclusive)
            // Esto previene tunneling al cambiar dirección en esquinas
            boolean inPath = reusableSwept.intersects(otherBounds);
            boolean alreadyTouching = bounds.intersectsInclusive(otherBounds);
            
            if (!inPath && !alreadyTouching) continue;

            double dist = calculateDistanceX(bounds, otherBounds, dx);
            if (Math.abs(dist) < nearestDist) {
                nearestDist = Math.abs(dist);
                nearest = other;
            }
        }

        return nearest;
    }

    /**
     * Busca el primer obstáculo en el eje Y.
     * 
     * ✅ CORRECCIÓN ESQUINAS: También considera obstáculos con los que YA está
     * en contacto (de state) para prevenir tunneling al "rodear" esquinas.
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
            
            // ✅ NUEVO: También considerar si YA está en contacto (intersectsInclusive)
            // Esto previene tunneling al cambiar dirección en esquinas
            boolean inPath = reusableSwept.intersects(otherBounds);
            boolean alreadyTouching = bounds.intersectsInclusive(otherBounds);
            
            if (!inPath && !alreadyTouching) continue;

            double dist = calculateDistanceY(bounds, otherBounds, dy);
            if (Math.abs(dist) < nearestDist) {
                nearestDist = Math.abs(dist);
                nearest = other;
            }
        }

        return nearest;
    }

    /**
     * Calcula distancia al obstáculo en eje X PRESERVANDO SIGNO de velocity.
     */
    private double calculateDistanceX(RectangleH bounds, RectangleH obstacle, double dx) {
        double dist;
        if (dx > 0) {
            // Moviéndose a la derecha
            dist = obstacle.getMinX() - bounds.getMaxX();
        } else {
            // Moviéndose a la izquierda  
            dist = obstacle.getMaxX() - bounds.getMinX();
        }
        // Mantener signo de dx: si dx negativo, dist debe ser negativo también
        return (dx < 0) ? -Math.abs(dist) : Math.abs(dist);
    }

    /**
     * Calcula distancia al obstáculo en eje Y PRESERVANDO SIGNO de velocity.
     */
    private double calculateDistanceY(RectangleH bounds, RectangleH obstacle, double dy) {
        double dist;
        if (dy > 0) {
            // Moviéndose hacia abajo
            dist = obstacle.getMinY() - bounds.getMaxY();
        } else {
            // Moviéndose hacia arriba
            dist = obstacle.getMaxY() - bounds.getMinY();
        }
        // Mantener signo de dy: si dy negativo, dist debe ser negativo también
        return (dy < 0) ? -Math.abs(dist) : Math.abs(dist);
    }

    /**
     * Registra el raycast para visualización debug.
     * @param obstacle puede ser null si no hay obstáculo detectado
     */
    private void registerVisualization(GameObjects obj, ColliderComponent collider,
                                      double dx, double dy, GameObjects obstacle, 
                                      double distance, int normalX, int normalY) {
        double objX = obj.getTransform().getPosition().getX();
        double objY = obj.getTransform().getPosition().getY();
        
        // Calcular raycast para visualización
        VelocityRaycast.RaycastResult raycast = VelocityRaycast.calculateRaycast(dx, dy);
        
        // Crear detectionResult según si hay obstáculo o no
        RaycastCollisionDetector.DetectionResult detection;
        if (obstacle != null) {
            double limitFactor = distance / Math.max(Math.abs(dx), Math.abs(dy));
            detection = RaycastCollisionDetector.DetectionResult.obstacle(distance, obstacle, limitFactor);
        } else {
            // Sin obstáculo - raycast libre
            detection = RaycastCollisionDetector.DetectionResult.noObstacle();
        }
        
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
            
            // LIMPIAR estado SI:
            // 1. Obstáculo fue destruido
            // 2. Obstáculo perdió su collider
            // 3. Ya NO hay overlap físico
            
            // ✅ CORRECCIÓN: NO limpiar solo por tener velocity
            // El raycast se reevaluará de todas formas si !isContactX()
            // Mantener el tracking del obstáculo mientras haya overlap
            
            if (obstacleX instanceof Destroyable d && d.isPendingDestruction()) {
                state.clearX();
            } else {
                ColliderComponent obstacleCol = obstacleX.getComponent(ColliderComponent.class);
                
                if (obstacleCol == null) {
                    state.clearX();
                } else {
                    RectangleH obstacleBounds = obstacleCol.getBounds();
                    
                    if (!bounds.intersectsInclusive(obstacleBounds)) {
                        // Ya NO hay overlap → LIMPIAR
                        state.clearX();
                    } else {
                        // Sigue en overlap → MANTENER tracking
                        // isContactX se maneja en el loop principal según velocity
                        boolean hasVelocityX = Math.abs(vx) >= VELOCITY_THRESHOLD;
                        state.setContactX(!hasVelocityX);
                    }
                }
            }
        }

        // ── Verificar estado en Y ─────────────────────────────────────────
        if (state.hasObstacleY()) {
            GameObjects obstacleY = state.getObstacleY();
            
            if (obstacleY instanceof Destroyable d && d.isPendingDestruction()) {
                state.clearY();
            } else {
                ColliderComponent obstacleCol = obstacleY.getComponent(ColliderComponent.class);
                
                if (obstacleCol == null) {
                    state.clearY();
                } else {
                    RectangleH obstacleBounds = obstacleCol.getBounds();
                    
                    if (!bounds.intersectsInclusive(obstacleBounds)) {
                        state.clearY();
                    } else {
                        boolean hasVelocityY = Math.abs(vy) >= VELOCITY_THRESHOLD;
                        state.setContactY(!hasVelocityY);
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
