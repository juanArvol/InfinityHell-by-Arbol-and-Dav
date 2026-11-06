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
 * RaycastMovementSystem — Sistema DUAL con Caché de Colisiones
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * RAYCAST 1 (Wide Ray): Grueso, cubre toda la cara, detecta y limita
 * RAYCAST 2 (Thin Ray): Delgado, cachea colisiones, reduce trabajo de Ray 1
 *
 * OPTIMIZACIÓN:
 * - Ray 2 recuerda obstáculos y bloquea movimiento mientras colisión activa
 * - Ray 1 SOLO se ejecuta cuando NO hay caché
 * - Ambos visibles con tecla P (debug)
 */
public final class RaycastMovementSystem {

    private static final double VELOCITY_THRESHOLD = 0.1;

    private final IdentityHashMap<GameObjects, RaycastState> raycastStates = 
        new IdentityHashMap<>();

    private final RectangleH reusableSwept = new RectangleH();

    public void update(List<GameObjects> objects, double deltaTime) {
        for (GameObjects obj : objects) {
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            ColliderComponent collider = obj.getComponent(ColliderComponent.class);
            if (collider == null) continue;
            if (collider.blocksMovement()) continue;

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;

            Physics2D physics = physComp.getPhysics();
            Vector2D velocity = physics.getVelocity();

            double dx = velocity.getX() * deltaTime;
            double dy = velocity.getY() * deltaTime;

            if (dx == 0.0 && dy == 0.0) continue;

            RaycastState state = raycastStates.computeIfAbsent(obj, k -> new RaycastState());
            RectangleH bounds = collider.getBounds();

            // ══════════════════════════════════════════════════════════════
            // RAYCAST X CON CACHÉ
            // ══════════════════════════════════════════════════════════════

            if (Math.abs(dx) >= VELOCITY_THRESHOLD) {
                double currentVelocityX = Math.abs(velocity.getX());
                
                // RAY 2: Verificar caché primero (solo si velocity <= impactVelocity)
                if (state.hasObstacleX() && state.isContactX()) {
                    // ✅ VALIDACIÓN DE VELOCIDAD: solo mantener caché si velocity actual ≤ velocity de impacto
                    if (currentVelocityX <= state.getImpactVelocityX()) {
                        GameObjects cachedObs = state.getObstacleX();
                        ColliderComponent cachedCol = cachedObs.getComponent(ColliderComponent.class);
                        
                        if (cachedCol != null && bounds.intersectsInclusive(cachedCol.getBounds())) {
                            // CACHÉ ACTIVO: bloquear movimiento
                            dx = 0;
                            
                            // Visualizar Ray 2 (thin)
                            if (VelocityRaycast.SHOW_RAYCASTS) {
                                registerThinRay(obj, collider, cachedObs, state.getNormalX(), 0);
                            }
                        } else {
                            // Ya no colisiona → limpiar caché
                            state.clearX();
                        }
                    } else {
                        // Velocity superó el límite de impacto → limpiar caché y reactivar Ray 1
                        state.clearX();
                    }
                }
                
                // RAY 1: Solo si NO hay caché activo
                if (!state.isContactX() && dx != 0) {
                    GameObjects obstacleX = findWideObstacleX(obj, collider, dx, objects);
                    
                    if (obstacleX != null) {
                        RectangleH obsBounds = obstacleX.getComponent(ColliderComponent.class).getBounds();
                        double distToObs = calculateDistanceX(bounds, obsBounds, dx);
                        
                        // ✅ FIX PENETRACIÓN: Si distToObs es 0, ya estamos penetrando → no mover
                        if (distToObs == 0) {
                            dx = 0;
                            int normalX = (velocity.getX() > 0) ? -1 : 1;
                            state.setObstacleX(obstacleX, normalX, currentVelocityX);
                            state.setContactX(true);
                        } else if (Math.abs(dx) > Math.abs(distToObs)) {
                            double originalDx = dx; // Guardar para visualización
                            dx = distToObs;
                            
                            // ✅ Marcar para cachear CON VELOCIDAD REAL
                            int normalX = (dx > 0) ? -1 : 1;
                            state.setObstacleX(obstacleX, normalX, currentVelocityX);
                            
                            // ✅ Visualizar Ray 1 (wide) CON LONGITUD ORIGINAL, NO LIMITADA
                            if (VelocityRaycast.SHOW_RAYCASTS) {
                                registerWideRay(obj, collider, originalDx, 0, obstacleX, normalX, 0);
                            }
                        } else {
                            // No hay limitación, solo visualizar
                            if (VelocityRaycast.SHOW_RAYCASTS) {
                                registerWideRay(obj, collider, dx, 0, null, 0, 0);
                            }
                        }
                    } else {
                        // No hay obstáculo, visualizar ray libre
                        if (VelocityRaycast.SHOW_RAYCASTS) {
                            registerWideRay(obj, collider, dx, 0, null, 0, 0);
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // RAYCAST Y CON CACHÉ
            // ══════════════════════════════════════════════════════════════

            if (Math.abs(dy) >= VELOCITY_THRESHOLD) {
                double currentVelocityY = Math.abs(velocity.getY());
                
                // RAY 2: Verificar caché primero (solo si velocity <= impactVelocity)
                if (state.hasObstacleY() && state.isContactY()) {
                    // ✅ VALIDACIÓN DE VELOCIDAD: solo mantener caché si velocity actual ≤ velocity de impacto
                    if (currentVelocityY <= state.getImpactVelocityY()) {
                        GameObjects cachedObs = state.getObstacleY();
                        ColliderComponent cachedCol = cachedObs.getComponent(ColliderComponent.class);
                        
                        if (cachedCol != null && bounds.intersectsInclusive(cachedCol.getBounds())) {
                            // CACHÉ ACTIVO: bloquear movimiento
                            dy = 0;
                            
                            // Visualizar Ray 2 (thin)
                            if (VelocityRaycast.SHOW_RAYCASTS) {
                                registerThinRay(obj, collider, cachedObs, 0, state.getNormalY());
                            }
                        } else {
                            // Ya no colisiona → limpiar caché
                            state.clearY();
                        }
                    } else {
                        // Velocity superó el límite de impacto → limpiar caché y reactivar Ray 1
                        state.clearY();
                    }
                }
                
                // RAY 1: Solo si NO hay caché activo
                if (!state.isContactY() && dy != 0) {
                    GameObjects obstacleY = findWideObstacleY(obj, collider, dy, objects);
                    
                    if (obstacleY != null) {
                        RectangleH obsBounds = obstacleY.getComponent(ColliderComponent.class).getBounds();
                        double distToObs = calculateDistanceY(bounds, obsBounds, dy);
                        
                        // ✅ FIX PENETRACIÓN: Si distToObs es 0, ya estamos penetrando → no mover
                        if (distToObs == 0) {
                            dy = 0;
                            int normalY = (velocity.getY() > 0) ? -1 : 1;
                            state.setObstacleY(obstacleY, normalY, currentVelocityY);
                            state.setContactY(true);
                        } else if (Math.abs(dy) > Math.abs(distToObs)) {
                            double originalDy = dy; // Guardar para visualización
                            dy = distToObs;
                            
                            // ✅ Marcar para cachear CON VELOCIDAD REAL
                            int normalY = (dy > 0) ? -1 : 1;
                            state.setObstacleY(obstacleY, normalY, currentVelocityY);
                            
                            // ✅ Visualizar Ray 1 (wide) CON LONGITUD ORIGINAL, NO LIMITADA
                            if (VelocityRaycast.SHOW_RAYCASTS) {
                                registerWideRay(obj, collider, 0, originalDy, obstacleY, 0, normalY);
                            }
                        } else {
                            // No hay limitación, solo visualizar
                            if (VelocityRaycast.SHOW_RAYCASTS) {
                                registerWideRay(obj, collider, 0, dy, null, 0, 0);
                            }
                        }
                    } else {
                        // No hay obstáculo, visualizar ray libre
                        if (VelocityRaycast.SHOW_RAYCASTS) {
                            registerWideRay(obj, collider, 0, dy, null, 0, 0);
                        }
                    }
                }
            }

            // MOVER
            PhysicsStepper.moveWith(obj, dx, dy);

            // POST-MOVE: Activar caché si hay colisión real
            updateCachePostMove(obj, collider, state);
        }
    }

    private GameObjects findWideObstacleX(GameObjects obj, ColliderComponent collider,
                                         double dx, List<GameObjects> allObjects) {
        RectangleH bounds = collider.getBounds();
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
            
            boolean inPath = reusableSwept.intersects(otherBounds);
            boolean touching = bounds.intersectsInclusive(otherBounds);
            
            if (!inPath && !touching) continue;

            boolean verticalOverlap = (otherBounds.getMaxY() >= bounds.getMinY() && 
                                      otherBounds.getMinY() <= bounds.getMaxY());
            
            if (!verticalOverlap) continue;

            double dist = calculateDistanceX(bounds, otherBounds, dx);
            if (Math.abs(dist) < nearestDist) {
                nearestDist = Math.abs(dist);
                nearest = other;
            }
        }

        return nearest;
    }

    private GameObjects findWideObstacleY(GameObjects obj, ColliderComponent collider,
                                         double dy, List<GameObjects> allObjects) {
        RectangleH bounds = collider.getBounds();
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
            
            boolean inPath = reusableSwept.intersects(otherBounds);
            boolean touching = bounds.intersectsInclusive(otherBounds);
            
            if (!inPath && !touching) continue;

            boolean horizontalOverlap = (otherBounds.getMaxX() >= bounds.getMinX() && 
                                        otherBounds.getMinX() <= bounds.getMaxX());
            
            if (!horizontalOverlap) continue;

            double dist = calculateDistanceY(bounds, otherBounds, dy);
            if (Math.abs(dist) < nearestDist) {
                nearestDist = Math.abs(dist);
                nearest = other;
            }
        }

        return nearest;
    }

    private void updateCachePostMove(GameObjects obj, ColliderComponent collider, RaycastState state) {
        RectangleH bounds = collider.getBounds();

        if (state.hasObstacleX() && !state.isContactX()) {
            GameObjects obs = state.getObstacleX();
            ColliderComponent obsCol = obs.getComponent(ColliderComponent.class);
            
            if (obsCol != null && bounds.intersectsInclusive(obsCol.getBounds())) {
                state.setContactX(true); // Activar caché
            } else {
                state.clearX();
            }
        }

        if (state.hasObstacleY() && !state.isContactY()) {
            GameObjects obs = state.getObstacleY();
            ColliderComponent obsCol = obs.getComponent(ColliderComponent.class);
            
            if (obsCol != null && bounds.intersectsInclusive(obsCol.getBounds())) {
                state.setContactY(true); // Activar caché
            } else {
                state.clearY();
            }
        }
    }

    private void registerWideRay(GameObjects obj, ColliderComponent collider,
                                 double dx, double dy, GameObjects obstacle,
                                 int normalX, int normalY) {
        double objX = obj.getTransform().getPosition().getX();
        double objY = obj.getTransform().getPosition().getY();
        
        VelocityRaycast.RaycastResult raycast = VelocityRaycast.calculateRaycast(dx, dy);
        
        RaycastCollisionDetector.DetectionResult detection;
        if (obstacle != null) {
            double dist = Math.max(Math.abs(dx), Math.abs(dy));
            detection = RaycastCollisionDetector.DetectionResult.obstacle(dist, obstacle, 1.0);
        } else {
            detection = RaycastCollisionDetector.DetectionResult.noObstacle();
        }
        
        RaycastVisualizationSystem.registerRaycast(obj, objX, objY, raycast, detection);
    }

    private void registerThinRay(GameObjects obj, ColliderComponent collider,
                                GameObjects obstacle, int normalX, int normalY) {
        double objX = obj.getTransform().getPosition().getX();
        double objY = obj.getTransform().getPosition().getY();
        
        ColliderComponent obsCol = obstacle.getComponent(ColliderComponent.class);
        if (obsCol == null) return;
        
        RectangleH obsBounds = obsCol.getBounds();
        double dx = (obsBounds.getMinX() + obsBounds.getMaxX()) / 2.0 - objX;
        double dy = (obsBounds.getMinY() + obsBounds.getMaxY()) / 2.0 - objY;
        
        VelocityRaycast.RaycastResult raycast = VelocityRaycast.calculateRaycast(dx, dy);
        
        double dist = Math.sqrt(dx * dx + dy * dy);
        RaycastCollisionDetector.DetectionResult detection = 
            RaycastCollisionDetector.DetectionResult.obstacle(dist, obstacle, 0.0);
        
        RaycastVisualizationSystem.registerRaycast(obj, objX, objY, raycast, detection);
    }

    private double calculateDistanceX(RectangleH bounds, RectangleH obstacle, double dx) {
        double dist;
        if (dx > 0) {
            dist = obstacle.getMinX() - bounds.getMaxX();
        } else {
            dist = obstacle.getMaxX() - bounds.getMinX();
        }
        
        if (dist < 0) return 0;
        
        return (dx < 0) ? -Math.abs(dist) : Math.abs(dist);
    }

    private double calculateDistanceY(RectangleH bounds, RectangleH obstacle, double dy) {
        double dist;
        if (dy > 0) {
            dist = obstacle.getMinY() - bounds.getMaxY();
        } else {
            dist = obstacle.getMaxY() - bounds.getMinY();
        }
        
        if (dist < 0) return 0;
        
        return (dy < 0) ? -Math.abs(dist) : Math.abs(dist);
    }

    public void clear() {
        raycastStates.clear();
    }
}
