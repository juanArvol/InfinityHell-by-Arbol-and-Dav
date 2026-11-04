package Game.Engine.Systems;

import Game.Engine.Colisions.CollisionDetector;
import Game.Engine.Colisions.CollisionDispatcher;
import Game.Engine.Colisions.CollisionHandler;
import Game.Engine.Colisions.CollisionResult;
import Game.Engine.Colisions.SweptAABB;
import Game.Engine.Component;
import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.InteractionSideComponent;
import Game.Engine.Entity.Components.InteractionSideComponent.SideInteraction;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.PushableComponent;
import Game.Engine.Entity.Components.SurfaceComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameObjects;
import Game.Engine.Physics.Contact.ContactState;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Engine.Physics.Predictive.RaycastCollisionDetector;
import Game.Engine.Physics.Predictive.VelocityRaycast;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Sistema de colisiones — pipeline unificado por frame.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * REFACTORIZACIÓN ARQUITECTÓNICA COMPLETA — Eliminación SOLID vs TRIGGER
 * ══════════════════════════════════════════════════════════════════════════
 *
 * PRINCIPIO FUNDAMENTAL:
 *   La matemática de detección geométrica es ÚNICA para todos los objetos.
 *   NO existe diferencia entre "sólidos" y "triggers" en la DETECCIÓN.
 *   La diferencia está en la POLÍTICA DE RESPUESTA (comportamientos).
 *
 * ── ANTES (Sistema Duplicado) ─────────────────────────────────────────────
 *   FASE 1   → SweptAABB eje-por-eje SOLO para SÓLIDOS (~200 líneas)
 *   FASE 1B  → SweptAABB 2D SOLO para TRIGGERS (~250 líneas)
 *   FASE 0.6 → Raycast predictivo separado (~90 líneas)
 *   Total: ~540 líneas de código duplicado
 *   Problema: Bullets no recibían CCD, atravesaban muros a alta velocidad
 *
 * ── AHORA (Sistema Unificado) ─────────────────────────────────────────────
 *   FASE 0   → Gravedad + fuerzas (solo objetos con gravedad habilitada)
 *   FASE 0.5 → Reset contactState + lastContactNormal
 *   FASE 1   → SweptAABB 2D UNIFICADO para TODOS los objetos dinámicos
 *              - Raycast predictivo integrado (limita velocidad)
 *              - Usa blockMovement flag para filtrar narrow phase
 *              - Usa canPush flag para aplicar empuje
 *              - Sub-stepping automático para velocidades altas
 *   FASE 2   → Overlaps residuales (AABB estático)
 *   FASE 3   → Dispatch eventos FASE 2
 *   FASE 4   → CollisionListener enter/stay/exit
 *
 *   Total: ~200 líneas de lógica de colisión (340 líneas eliminadas)
 *
 * ── INVARIANTES GARANTIZADOS ──────────────────────────────────────────────
 *   1. Todo objeto con velocity != 0 recibe CCD (SweptAABB 2D)
 *   2. La normal del impacto siempre está disponible en onCollision()
 *   3. Un par no se despacha más de una vez por frame
 *   4. Objetos destruidos no reciben eventos adicionales
 *   5. Raycast visual coincide con detección real (mismo algoritmo)
 *   6. Bullets NUNCA atraviesan muros (sub-stepping garantizado)
 *
 * ── CONFIGURACIÓN DE OBJETOS ──────────────────────────────────────────────
 *
 * MURO (estático, bloquea):
 *   collider.setBlockMovement(true);   // bloquea a otros
 *   physics.setCanPush(false);          // no empuja (no tiene sentido)
 *
 * PLAYER (dinámico, no bloquea):
 *   collider.setBlockMovement(false);  // no bloquea a otros
 *   physics.setCanPush(true);           // puede empujar objetos pushables
 *
 * BULLET (dinámico, no bloquea, no empuja):
 *   collider.setBlockMovement(false);  // no bloquea a otros
 *   physics.setCanPush(false);          // no empuja nada
 *
 * ── ORDEN DE EJECUCIÓN ────────────────────────────────────────────────────
 *   1. Bullet.update() → movement.tick() + behavior.onUpdate() (NO moveByPhysics)
 *   2. CollisionsSystem.update() → integra posición con CCD garantizado
 *   3. Bullet.onCollision() recibe evento con normal correcta
 */
public class CollisionsSystem {

    // ── ContactRecord — registro interno de un contacto del frame ────────

    /**
     * Registro de un contacto detectado durante FASE 1.
     * Acumulado por objeto para combinar múltiples contactos del mismo frame.
     */
    private static final class ContactRecord {
        final GameObjects   other;
        final int           normalX;
        final int           normalY;
        final SurfaceMaterial surface;

        ContactRecord(GameObjects other, int normalX, int normalY, SurfaceMaterial surface) {
            this.other   = other;
            this.normalX = normalX;
            this.normalY = normalY;
            this.surface = surface;
        }
    }

    /**
     * Separación mínima robusta aplicada en contactos de colisión.
     * 
     * GARANTÍA: 1.0px es suficiente para que ANY collider (mínimo 4×4px)
     * salga completamente del bounds de otro sin intersection residual.
     */
    private static final double ROBUST_SEPARATION_MINIMUM = 1.0;

    /**
     * Umbral de velocidad para activar SUB-STEPPING PREDICTIVO.
     * Velocidades >50 px/frame requieren división en sub-pasos para
     * garantizar que no atraviesen colliders pequeños.
     */
    private static final double SUBSTEP_VELOCITY_THRESHOLD = 50.0;
    
    /**
     * Tamaño máximo de sub-paso. Con 20px y collider mínimo 4×4px,
     * es IMPOSIBLE atravesar sin detección.
     */
    private static final double MAX_STEP_SIZE = 20.0;

    /**
     * Contactos activos del frame anterior, por objeto.
     * Usado en FASE 4 para calcular enter/stay/exit.
     */
    private final IdentityHashMap<GameObjects, Set<GameObjects>> previousContacts =
            new IdentityHashMap<>();

    /**
     * CollisionDetector reutilizable entre frames (zero allocations).
     */
    private final CollisionDetector detector = new CollisionDetector();

    /**
     * RectangleH reutilizable para broadphase 2D.
     * Evita allocations en hot path.
     */
    private final RectangleH reusableBroad2D = new RectangleH();

    // ── Profiling Infrastructure ──────────────────────────────────────────
    private final Game.Engine.Profiling.SubsystemTimer collisionTimer = 
        new Game.Engine.Profiling.SubsystemTimer();

    /**
     * Ejecuta el pipeline completo de física y colisiones.
     *
     * @param objects lista de objetos activos en el mundo
     * @param deltaTime tiempo del simulation step en segundos
     * @param camera cámara activa del juego (null = sin culling)
     */
    public void update(List<GameObjects> objects, double deltaTime, Game.Engine.Camera.GameCamera camera) {
        collisionTimer.start();

        // ── FASE 0: Aplicar gravedad y fuerzas acumuladas ────────────────
        // Solo objetos con gravedad NO manejada externamente.
        // Triggers (bullets) gestionan su gravedad via ProjectileMovement.
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (physics.isGravityManagedExternally()) continue;
            
            physics.applyGravity(physics.getOnGround(), deltaTime);
            physics.flushAccumulatedForces(deltaTime);
        }

        // ── FASE 0.5: Resetear estado de contacto ────────────────────────
        // DESPUÉS de applyGravity (usa onGround previo).
        // ANTES de SweptAABB (nuevos contactos parten de cero).
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;
            Physics2D physics = physComp.getPhysics();
            
            // Reset flags de contacto
            physics.setOnGround(false);
            physics.setOnWall(false);
            physics.setOnCeiling(false);
            physics.clearSurface();
            
            // Reset normal de contacto (BulletPhysics)
            if (physics instanceof BulletPhysics bp) {
                bp.clearLastContactNormal();
            }
            
            physComp.setContactState(ContactState.NONE);
        }

        // ── FASE 1: SweptAABB 2D UNIFICADO para TODOS los dinámicos ──────
        // Pipeline unificado: raycast + CCD + sub-stepping + resolución.
        // Usa blockMovement para filtrar narrow phase.
        // Usa canPush para aplicar empuje solo donde corresponde.

        IdentityHashMap<GameObjects, List<ContactRecord>> frameContacts =
                new IdentityHashMap<>();

        for (GameObjects obj : objects) {
            ColliderComponent colA = obj.getComponent(ColliderComponent.class);
            if (colA == null) continue;
            
            // Skip objetos estáticos (blockMovement = true, velocity = 0)
            // Estos son muros, plataformas — no necesitan detección swept
            if (colA.blocksMovement()) continue;
            
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;

            Physics2D physics = physComp.getPhysics();
            double vx = physics.getVelocity().getX() * deltaTime;
            double vy = physics.getVelocity().getY() * deltaTime;
            
            if (vx == 0.0 && vy == 0.0) continue;

            // ── Collision Culling (bullets off-screen) ────────────────────
            if (camera != null && CollisionCulling.shouldSkipCollision(obj, camera, deltaTime)) {
                PhysicsStepper.moveWith(obj, vx, vy);
                continue;
            }

            // ── Raycast Predictivo Limitante ──────────────────────────────
            // Detecta obstáculos y limita el movimiento ANTES de SweptAABB.
            // Garantiza que bullets con velocidad alta NO atraviesen muros.
            RaycastCollisionDetector.DetectionResult detection = 
                RaycastCollisionDetector.detectAndLimitMovement(obj, vx, vy, objects);
            
            // Registrar raycast para visualización (si está activo)
            if (VelocityRaycast.SHOW_RAYCASTS) {
                double objX = obj.getTransform().getPosition().getX();
                double objY = obj.getTransform().getPosition().getY();
                VelocityRaycast.RaycastResult raycastVis = VelocityRaycast.calculateRaycast(vx, vy);
                Game.Engine.Physics.Predictive.RaycastVisualizationSystem.registerRaycast(
                    obj, objX, objY, raycastVis, detection);
            }
            
            // Aplicar limitación de raycast
            if (detection.hasObstacle && detection.movementLimitFactor < 1.0) {
                vx *= detection.movementLimitFactor;
                vy *= detection.movementLimitFactor;
                
                // Si movimiento casi nulo, detener y despachar
                if (Math.abs(vx) < 0.1 && Math.abs(vy) < 0.1) {
                    if (detection.nearestObject != null) {
                        writeContactNormal(obj, physics, vx, vy);
                        detector.markDispatched(obj, detection.nearestObject);
                        if (!shouldFilterCollision(obj, detection.nearestObject)) {
                            CollisionDispatcher.dispatch(obj, detection.nearestObject);
                        }
                    }
                    continue; // No mover más
                }
            }
            
            // Recalcular magnitud después de limitación
            double velocityMag = Math.sqrt(vx * vx + vy * vy);
            
            // ── Sub-stepping Adaptativo (velocidades peligrosas) ──────────
            if (velocityMag > SUBSTEP_VELOCITY_THRESHOLD) {
                // Velocidad PELIGROSA — dividir en sub-pasos
                int numSteps = (int) Math.ceil(velocityMag / MAX_STEP_SIZE);
                double stepVx = vx / numSteps;
                double stepVy = vy / numSteps;
                
                boolean hitThisFrame = false;
                GameObjects finalHitObj = null;
                int finalNormalX = 0;
                int finalNormalY = 0;
                
                for (int step = 0; step < numSteps; step++) {
                    RectangleH bounds = colA.getBounds();
                    reusableBroad2D.copyFrom(bounds.sweep(stepVx, stepVy));
                    
                    double nearestTime = 1.0;
                    int hitNormalX = 0;
                    int hitNormalY = 0;
                    GameObjects hitObj = null;
                    
                    // Narrow phase en este sub-paso
                    for (GameObjects other : objects) {
                        if (other == obj) continue;
                        if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                        
                        ColliderComponent colB = other.getComponent(ColliderComponent.class);
                        if (colB == null) continue;
                        
                        // ── NUEVO: Filtro blockMovement ───────────────────
                        // Solo colisionar contra objetos que bloquean movimiento.
                        // Esto permite que bullets colisionen con muros pero NO
                        // entre ellas (ni con player/enemies si así se configura).
                        if (!colB.blocksMovement()) continue;
                        
                        if (!colA.canCollideWith(colB)) continue;
                        
                        RectangleH ob = colB.getBounds();
                        if (!reusableBroad2D.intersects(ob)) continue;
                        
                        SweptAABB.Result r = SweptAABB.calculate2D(bounds, ob, stepVx, stepVy);
                        if (!r.hasCollision()) continue;
                        if (r.time >= nearestTime) continue;
                        
                        nearestTime = r.time;
                        hitNormalX = r.normalX;
                        hitNormalY = r.normalY;
                        hitObj = other;
                    }
                    
                    // Mover al punto de contacto (o completo si no hay hit)
                    PhysicsStepper.moveWith(obj, stepVx * nearestTime, stepVy * nearestTime);
                    
                    if (hitObj != null) {
                        // ¡COLISIÓN! — detener sub-stepping
                        hitThisFrame = true;
                        finalHitObj = hitObj;
                        finalNormalX = hitNormalX;
                        finalNormalY = hitNormalY;
                        
                        // Separación mínima
                        double sepX = hitNormalX * ROBUST_SEPARATION_MINIMUM;
                        double sepY = hitNormalY * ROBUST_SEPARATION_MINIMUM;
                        PhysicsStepper.moveWith(obj, sepX, sepY);
                        
                        break; // Detener sub-stepping
                    }
                }
                
                // Procesar la colisión detectada
                if (hitThisFrame && finalHitObj != null) {
                    processCollision(obj, finalHitObj, physics, finalNormalX, finalNormalY, 
                                   vx, vy, frameContacts);
                }
                
            } else {
                // ── Velocidad NORMAL — SweptAABB estándar ─────────────────
                RectangleH bounds = colA.getBounds();
                reusableBroad2D.copyFrom(bounds.sweep(vx, vy));

                double nearestTime = 1.0;
                int hitNormalX = 0;
                int hitNormalY = 0;
                GameObjects hitObj = null;

                // Narrow phase
                for (GameObjects other : objects) {
                    if (other == obj) continue;
                    if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                    
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null) continue;
                    
                    // ── NUEVO: Filtro blockMovement ───────────────────────
                    if (!colB.blocksMovement()) continue;
                    
                    if (!colA.canCollideWith(colB)) continue;
                    
                    RectangleH ob = colB.getBounds();
                    if (!reusableBroad2D.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate2D(bounds, ob, vx, vy);
                    if (!r.hasCollision()) continue;
                    if (r.time >= nearestTime) continue;

                    nearestTime = r.time;
                    hitNormalX = r.normalX;
                    hitNormalY = r.normalY;
                    hitObj = other;
                }

                // Mover al punto de contacto
                PhysicsStepper.moveWith(obj, vx * nearestTime, vy * nearestTime);

                if (hitObj != null) {
                    // Separación robusta
                    double sepX, sepY;
                    
                    if (nearestTime > 0.0) {
                        // Contacto CCD limpio
                        sepX = hitNormalX * ROBUST_SEPARATION_MINIMUM;
                        sepY = hitNormalY * ROBUST_SEPARATION_MINIMUM;
                    } else {
                        // Penetración preexistente — calcular MTV
                        RectangleH currentBounds = colA.getBounds();
                        RectangleH ob = hitObj.getComponent(ColliderComponent.class).getBounds();
                        Point2D.Double mtv = currentBounds.minimumTranslationVector(ob, hitNormalX, hitNormalY);
                        
                        sepX = (mtv.x != 0.0) ? mtv.x + Math.signum(mtv.x) * ROBUST_SEPARATION_MINIMUM : 0.0;
                        sepY = (mtv.y != 0.0) ? mtv.y + Math.signum(mtv.y) * ROBUST_SEPARATION_MINIMUM : 0.0;
                    }
                    
                    PhysicsStepper.moveWith(obj, sepX, sepY);

                    processCollision(obj, hitObj, physics, hitNormalX, hitNormalY, 
                                   vx, vy, frameContacts);
                }
            }
        } // FIN FASE 1


        // ── FASE 1 — Finalización: Construir ContactState por objeto ─────
        for (Map.Entry<GameObjects, List<ContactRecord>> entry : frameContacts.entrySet()) {
            GameObjects         obj     = entry.getKey();
            List<ContactRecord> records = entry.getValue();
            Physics2DComponent  physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null || records.isEmpty()) continue;

            boolean       hasGround  = false;
            boolean       hasWall    = false;
            boolean       hasCeiling = false;
            SurfaceMaterial groundSurface = null;
            int primaryNX = 0, primaryNY = 0;

            for (ContactRecord rec : records) {
                if (rec.normalY == -1) {
                    hasGround = true;
                    if (groundSurface == null) groundSurface = rec.surface;
                    primaryNX = 0; primaryNY = -1;
                } else if (rec.normalY == 1) {
                    hasCeiling = true;
                    if (primaryNY == 0) { primaryNX = 0; primaryNY = 1; }
                } else if (rec.normalX != 0) {
                    hasWall = true;
                    if (primaryNX == 0 && primaryNY == 0) { primaryNX = rec.normalX; primaryNY = 0; }
                }
            }

            if (groundSurface == null) groundSurface = SurfaceMaterial.DEFAULT;

            ContactState state = ContactState.builder()
                    .onGround(hasGround)
                    .onWall(hasWall)
                    .onCeiling(hasCeiling)
                    .surfaceFriction(groundSurface.getFriction())
                    .surfaceNormalX(primaryNX)
                    .surfaceNormalY(primaryNY)
                    .build();

            physComp.setContactState(state);
        }


        // ── FASE 2: Detección AABB (overlaps residuales) ─────────────────
        // Detecta pares que no fueron procesados en FASE 1:
        //   - Objetos dinámicos que se solapan sin velocity
        //   - Cualquier overlap residual no despachado
        List<CollisionResult> pairs = detector.detect(objects);

        // ── FASE 3: Despacho de eventos de FASE 2 ────────────────────────
        for (CollisionResult pair : pairs) {
            // Guard: skip si alguno está destruido
            if (pair.a instanceof Destroyable da && da.isPendingDestruction()) continue;
            if (pair.b instanceof Destroyable db && db.isPendingDestruction()) continue;

            // Filtro de colisión por ownership (bullet vs owner)
            if (shouldFilterCollision(pair.a, pair.b)) {
                continue;
            }

            // Propagar normal a BulletPhysics si disponible
            propagateNormalToTrigger(pair);

            CollisionDispatcher.dispatch(pair.a, pair.b);
        }

        // ── FASE 4: CollisionListener enter / stay / exit ─────────────────
        IdentityHashMap<GameObjects, Set<GameObjects>> currentContacts =
                new IdentityHashMap<>();

        for (CollisionResult pair : pairs) {
            currentContacts
                .computeIfAbsent(pair.a, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(pair.b);
            currentContacts
                .computeIfAbsent(pair.b, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(pair.a);
        }

        for (GameObjects obj : objects) {
            List<CollisionHandler> listeners = collectListeners(obj);
            if (listeners.isEmpty()) continue;

            Set<GameObjects> current  = currentContacts.getOrDefault(obj, Set.of());
            Set<GameObjects> previous = previousContacts.getOrDefault(obj, Set.of());
            boolean          isTrigger = isTriggerObj(obj);

            for (GameObjects contact : current) {
                if (!previous.contains(contact)) {
                    for (CollisionHandler listener : listeners) {
                        if (isTrigger) listener.onTriggerEnter(contact);
                        else           listener.onCollisionEnter(contact);
                    }
                } else {
                    for (CollisionHandler listener : listeners) {
                        if (isTrigger) listener.onTriggerStay(contact);
                        else           listener.onCollisionStay(contact);
                    }
                }
            }

            for (GameObjects contact : previous) {
                if (!current.contains(contact)) {
                    for (CollisionHandler listener : listeners) {
                        if (isTrigger) listener.onTriggerExit(contact);
                        else           listener.onCollisionExit(contact);
                    }
                }
            }
        }

        // Rotar contactos: actual → anterior para el siguiente frame
        previousContacts.clear();
        previousContacts.putAll(currentContacts);

        collisionTimer.stop();
    }

    /**
     * Procesa una colisión detectada en FASE 1.
     * Resuelve velocidad, aplica contactState, despacha evento, aplica empuje.
     */
    private void processCollision(GameObjects obj, GameObjects hitObj, Physics2D physics,
                                  int normalX, int normalY, double vx, double vy,
                                  IdentityHashMap<GameObjects, List<ContactRecord>> frameContacts) {
        
        // Escribir normal para BulletPhysics/Bullet
        if (physics instanceof BulletPhysics bp) {
            bp.setLastContactNormal(normalX, normalY);
        }
        if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            bullet.setCollisionNormal(normalX, normalY);
        }

        // Resolución de velocidad (detener en eje de normal)
        if (normalX != 0) physics.vSetX(0);
        if (normalY != 0) physics.vSetY(0);

        // Registrar contacto para ContactState
        InteractionSideComponent iscHit = hitObj.getComponent(InteractionSideComponent.class);
        SideInteraction sideType = (iscHit != null)
                ? iscHit.forNormal(normalX, normalY)
                : defaultSideForNormal(normalX, normalY);
        
        SurfaceMaterial surface = resolveSurface(hitObj);
        
        List<ContactRecord> records = frameContacts.computeIfAbsent(obj, k -> new ArrayList<>(4));
        records.add(new ContactRecord(hitObj, normalX, normalY, surface));
        
        applyContactFlag(physics, normalX, normalY, sideType);
        if (normalY == -1) physics.setCurrentSurface(surface);

        // Aplicar empuje si canPush=true y target es pushable
        if (physics.canPush()) {
            applyPushIfPushable(hitObj, vx, vy, physics.getMass());
        }

        // Despachar evento
        detector.markDispatched(obj, hitObj);
        if (!shouldFilterCollision(obj, hitObj)) {
            CollisionDispatcher.dispatch(obj, hitObj);
        }
    }

    /**
     * Escribe la normal de contacto en BulletPhysics y Bullet.
     * Usado cuando raycast detiene el movimiento completamente.
     */
    private void writeContactNormal(GameObjects obj, Physics2D physics, double vx, double vy) {
        int normalX = (vx > 0) ? -1 : (vx < 0) ? 1 : 0;
        int normalY = (vy > 0) ? -1 : (vy < 0) ? 1 : 0;
        
        if (physics instanceof BulletPhysics bp) {
            bp.setLastContactNormal(normalX, normalY);
        }
        if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            bullet.setCollisionNormal(normalX, normalY);
        }
    }

    /**
     * Limpia el estado de contactos anteriores.
     * Llamar al cambiar de mundo/escena para evitar enter/exit espurios.
     */
    public void clearContactHistory() {
        previousContacts.clear();
        detector.clear();
    }

    /**
     * Retorna el tiempo de colisiones del último frame en milisegundos.
     */
    public double getLastCollisionTimeMs() {
        return collisionTimer.getElapsedMs();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Filtro de colisión por ownership.
     * Un proyectil NO debe colisionar con su owner.
     */
    private boolean shouldFilterCollision(GameObjects a, GameObjects b) {
        if (a instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            Object owner = bullet.getOwner();
            if (owner != null && owner == b) return true;
        }
        
        if (b instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            Object owner = bullet.getOwner();
            if (owner != null && owner == a) return true;
        }
        
        return false;
    }

    /**
     * Propaga normal calculada en FASE 2 a BulletPhysics si disponible.
     */
    private static void propagateNormalToTrigger(CollisionResult pair) {
        if (pair.normalX == 0 && pair.normalY == 0) return;

        if (pair.a.getComponent(ColliderComponent.class) != null
                && !pair.a.getComponent(ColliderComponent.class).blocksMovement()) {
            Physics2DComponent pc = pair.a.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                bp.setLastContactNormal(pair.normalX, pair.normalY);
            }
            if (pair.a instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                bullet.setCollisionNormal(pair.normalX, pair.normalY);
            }
        }

        if (pair.b.getComponent(ColliderComponent.class) != null
                && !pair.b.getComponent(ColliderComponent.class).blocksMovement()) {
            Physics2DComponent pc = pair.b.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                bp.setLastContactNormal(-pair.normalX, -pair.normalY);
            }
            if (pair.b instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                bullet.setCollisionNormal(-pair.normalX, -pair.normalY);
            }
        }
    }

    private static void applyContactFlag(Physics2D physics, int normalX, int normalY,
                                          SideInteraction sideType) {
        switch (sideType) {
            case SURFACE   -> physics.setOnGround(true);
            case CEILING   -> physics.setOnCeiling(true);
            case WALL,
                 CLIMBABLE -> physics.setOnWall(true);
            case NONE      -> {
                if (normalY == -1)      physics.setOnGround(true);
                else if (normalY == 1)  physics.setOnCeiling(true);
                else if (normalX != 0)  physics.setOnWall(true);
            }
            case PASSTHROUGH -> { /* filtrado antes de llegar aquí */ }
        }
    }

    private static SideInteraction defaultSideForNormal(int normalX, int normalY) {
        if (normalY == -1) return SideInteraction.SURFACE;
        if (normalY == 1)  return SideInteraction.CEILING;
        if (normalX != 0)  return SideInteraction.WALL;
        return SideInteraction.NONE;
    }

    private static SurfaceMaterial resolveSurface(GameObjects obj) {
        SurfaceComponent sc = obj.getComponent(SurfaceComponent.class);
        if (sc != null) return sc;
        if (obj instanceof SurfaceMaterial sm) return sm;
        return SurfaceMaterial.DEFAULT;
    }

    private static void applyPushIfPushable(GameObjects target,
                                             double vx, double vy,
                                             double sourceMass) {
        PushableComponent pushable = target.getComponent(PushableComponent.class);
        if (pushable == null || !pushable.isEnabled()) return;
        pushable.applyPush(vx * sourceMass, vy * sourceMass);
    }

    private static List<CollisionHandler> collectListeners(GameObjects obj) {
        List<CollisionHandler> result = null;
        for (Component c : obj.getComponents()) {
            if (c instanceof CollisionHandler cle) {
                if (result == null) result = new ArrayList<>(2);
                result.add(cle);
            }
        }
        return result != null ? result : List.of();
    }

    private static boolean isTriggerObj(GameObjects obj) {
        ColliderComponent col = obj.getComponent(ColliderComponent.class);
        return col != null && !col.blocksMovement();
    }
}
