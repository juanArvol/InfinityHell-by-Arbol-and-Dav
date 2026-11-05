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
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import java.util.*;

/**
 * Sistema de colisiones — pipeline de detección y resolución.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * REFACTORIZACIÓN ARQUITECTÓNICA — Separación Movimiento/Colisión
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ── NUEVO PARADIGMA ───────────────────────────────────────────────────────
 *
 * ANTES (TODO en CollisionsSystem):
 *   FASE 0   → Gravedad
 *   FASE 0.5 → Reset contactState
 *   FASE 0.6 → Raycast predictivo
 *   FASE 1   → SweptAABB + MOVER objetos
 *   FASE 2   → Overlaps residuales
 *   FASE 3-4 → Eventos
 *
 * AHORA (Separación limpia):
 *   RaycastMovementSystem:
 *     • Evalúa raycast anti-tunneling (velocidad >= 1px, por eje independiente)
 *     • IMPACTA: Limita desplazamiento para prevenir tunneling
 *     • MUEVE objetos con PhysicsStepper
 *     • Mantiene estado de contacto por eje (tracking interno)
 *
 *   CollisionsSystem (ESTE):
 *     FASE 0   → Gravedad + fuerzas
 *     FASE 0.5 → Reset contactState
 *     FASE 1   → COLISIONA: Detectar overlaps (objetos YA movidos)
 *     FASE 2   → Calcular normales con SweptAABB retrospectivo (SIEMPRE)
 *     FASE 3   → Resolver física (velocidad, ContactState)
 *     FASE 4   → Ejecutar eventos de colisión
 *     FASE 5   → CollisionListener enter/stay/exit
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 * CollisionsSystem SOLO:
 *   ✅ Aplica gravedad (objetos con gravedad habilitada)
 *   ✅ Resetea contactState antes de detección
 *   ✅ Detecta overlaps (objetos ya movidos por RaycastMovementSystem)
 *   ✅ Calcula normales precisas con SweptAABB (SIEMPRE, no reutiliza raycast)
 *   ✅ Actualiza ContactState (onGround, onWall, onCeiling)
 *   ✅ Propaga normales a BulletPhysics/Bullet
 *   ✅ Ejecuta eventos de colisión (CollisionDispatcher.dispatch)
 *   ✅ Aplica empuje si objeto es pushable
 *
 * CollisionsSystem NO:
 *   ❌ Mueve objetos (lo hace RaycastMovementSystem)
 *   ❌ Evalúa raycast (lo hace RaycastMovementSystem)
 *   ❌ Limita desplazamiento (lo hace RaycastMovementSystem)
 *   ❌ Reutiliza normales de raycast (calcula sus propias normales con SweptAABB)
 *   ❌ Anula velocity directamente (la lógica de behaviors decide qué hacer)
 *
 * ── DIFERENCIA CLAVE: IMPACTO vs COLISIÓN ────────────────────────────────
 *
 * IMPACTO (Raycast):
 *   - Ocurre ANTES de mover
 *   - Limita DISTANCIA (no velocidad)
 *   - Previene tunneling
 *   - Normal simple (eje dominante)
 *   - NO calcula física completa
 *
 * COLISIÓN (CollisionsSystem):
 *   - Ocurre DESPUÉS de mover
 *   - Detecta OVERLAP
 *   - Calcula física completa (normales precisas, TOI, ContactState)
 *   - Normal calculada con SweptAABB (geometría completa)
 *   - Ejecuta eventos y modifica velocidad
 *
 * ── SWEPTAABB: USO RETROSPECTIVO ──────────────────────────────────────────
 *
 * SweptAABB ya NO se usa para MOVER objetos.
 * Se usa RETROSPECTIVAMENTE para calcular normales precisas:
 *
 *   1. RaycastMovementSystem movió objeto de A → B
 *   2. CollisionsSystem detecta overlap en B
 *   3. SweptAABB calcula qué normal habría tenido si hubiera movido A → B
 *   4. Usa esa normal para resolver físicamente
 *
 * Esto garantiza normales correctas sin duplicar movimiento.
 *
 * ── ORDEN DE EJECUCIÓN ────────────────────────────────────────────────────
 *
 * WorldManager.update():
 *   1. RaycastMovementSystem.update()  → MUEVE todo
 *   2. CollisionsSystem.update()       → DETECTA y RESUELVE
 *
 * ── INVARIANTES GARANTIZADOS ──────────────────────────────────────────────
 *
 *   1. PhysicsStepper SOLO es llamado por RaycastMovementSystem
 *   2. Normales siempre disponibles en onCollision()
 *   3. Sin movimiento duplicado
 *   4. Raycast evita tunneling ANTES de mover
 *   5. CollisionSystem resuelve contactos DESPUÉS de mover
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
    private static final double ROBUST_SEPARATION_MINIMUM = 1;

    /**
     * Umbral de velocidad para activar RAYCAST ANTI-TUNNELING.
     * Velocidades >50 px/frame requieren raycast predictivo para
     * garantizar que no atraviesen colliders pequeños.
     * 
     * SweptAABB SIEMPRE se ejecuta después del raycast (si aplica)
     * para calcular la geometría precisa del contacto.
     */
    private static final double SUBSTEP_VELOCITY_THRESHOLD = 1.0;

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
    
    /**
     * RaycastMovementSystem instance para acceder al estado de raycast.
     * Usado para obtener normales calculadas por raycast en vez de recalcular.
     */
    private Game.Engine.Physics.Movement.RaycastMovementSystem raycastMovementSystem;

    /**
     * Configura la referencia a RaycastMovementSystem.
     * DEBE llamarse desde WorldManager después de crear ambos sistemas.
     * 
     * @param raycastSystem sistema de raycast movement
     */
    public void setRaycastMovementSystem(Game.Engine.Physics.Movement.RaycastMovementSystem raycastSystem) {
        this.raycastMovementSystem = raycastSystem;
    }

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

        // ══════════════════════════════════════════════════════════════════
        // FASE 1: Sistema de Colisiones Continuas (CCD)
        // ══════════════════════════════════════════════════════════════════
        //
        // ARQUITECTURA LIMPIA:
        //   1. Raycast + PhysicsStepper → Anti-tunneling (solo velocidades >50px)
        //   2. SweptAABB → Continuous Collision Detection (SIEMPRE)
        //
        // RESPONSABILIDADES:
        //   - Raycast: Detectar y limitar movimiento peligroso
        //   - PhysicsStepper: Aplicar movimiento seguro
        //   - SweptAABB: Calcular TOI y normales precisas de contacto
        //
        // NO HAY DUPLICACIÓN: Raycast NO calcula colisiones, solo limita.
        // SweptAABB es el ÚNICO que calcula geometría de contacto.
        // ══════════════════════════════════════════════════════════════════

        IdentityHashMap<GameObjects, List<ContactRecord>> frameContacts =
                new IdentityHashMap<>();

        for (GameObjects obj : objects) {
            ColliderComponent colA = obj.getComponent(ColliderComponent.class);
            if (colA == null) continue;
            
            // Skip objetos estáticos (no se mueven)
            if (colA.blocksMovement()) continue;
            
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;

            Physics2D physics = physComp.getPhysics();
            double vx = physics.getVelocity().getX() * deltaTime;
            double vy = physics.getVelocity().getY() * deltaTime;
            
            if (vx == 0.0 && vy == 0.0) continue;

            // ── Collision Culling (bullets off-screen) ────────────────────
            // NOTA: El movimiento ahora es responsabilidad exclusiva de RaycastMovementSystem
            // CollisionSystem SOLO detecta y resuelve overlaps, NO mueve objetos
            if (camera != null && CollisionCulling.shouldSkipCollision(obj, camera, deltaTime)) {
                continue;
            }

            double velocityMag = Math.sqrt(vx * vx + vy * vy);
            
            // ══════════════════════════════════════════════════════════════
            // DETECCIÓN DE OVERLAPS EN POSICIÓN ACTUAL
            // ══════════════════════════════════════════════════════════════
            // ARQUITECTURA CORRECTA:
            //   RaycastMovementSystem ya movió el objeto.
            //   CollisionsSystem detecta overlaps en la posición ACTUAL.
            //   NO intenta reconstruir el trayecto (no lo conoce sin coupling).
            //   
            // ESTRATEGIA:
            //   1. Detectar si currentBounds intersecta con obstacles
            //   2. Calcular normal desde dirección de velocity
            //   3. Resolver overlap y ejecutar eventos
            // ══════════════════════════════════════════════════════════════
            
            RectangleH currentBounds = colA.getBounds();
            GameObjects hitObj = null;
            int hitNormalX = 0;
            int hitNormalY = 0;

            // Buscar obstáculo con el que estamos overlapping
            for (GameObjects other : objects) {
                if (other == obj) continue;
                if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                
                ColliderComponent colB = other.getComponent(ColliderComponent.class);
                if (colB == null) continue;
                
                // Solo colisionar contra objetos que bloquean
                if (!colB.blocksMovement()) continue;
                
                if (!colA.canCollideWith(colB)) continue;
                
                RectangleH ob = colB.getBounds();
                
                // ¿Hay overlap en posición actual?
                if (currentBounds.intersects(ob)) {
                    hitObj = other;
                    
                    // ✅ CORRECCIÓN: SIEMPRE calcular normal con SweptAABB
                    // El raycast solo IMPACTA (limita distancia).
                    // CollisionsSystem debe COLISIONAR (calcular física completa).
                    // NO reutilizar normales de raycast - calcular geometría precisa.
                    SweptAABB.Result result = SweptAABB.calculate2D(
                        currentBounds,
                        ob,
                        vx, vy
                    );
                    
                    hitNormalX = result.normalX;
                    hitNormalY = result.normalY;
                    
                    break; // Procesar solo el primer overlap
                }
            }

            // Si hubo colisión, procesar eventos y contactState
            if (hitObj != null) {
                processCollision(obj, hitObj, physics, hitNormalX, hitNormalY, 
                               vx, vy, frameContacts);
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
     * Propaga normal, actualiza ContactState, despacha evento, aplica empuje.
     * 
     * NO anula velocity directamente - la lógica de colisión (GroundMovement,
     * BulletBehaviors, etc.) decide qué hacer con la velocity basándose en
     * ContactState y la normal.
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

        // ✅ CORRECCIÓN: NO anular velocity aquí
        // La lógica de colisión (behaviors, movement systems) decide qué hacer
        // basándose en ContactState y normal.
        // Anular velocity aquí duplicaría lógica y rompería saltos/rebotes.

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
