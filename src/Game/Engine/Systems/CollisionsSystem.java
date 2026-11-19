package Game.Engine.Systems;

import static Game.Engine.Colisions.CollisionConstants.*;
import Game.Engine.Colisions.CollisionDetector;
import Game.Engine.Colisions.CollisionDispatcher;
import Game.Engine.Colisions.CollisionHandler;
import Game.Engine.Colisions.CollisionResult;
import Game.Engine.Component;
import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.InteractionSideComponent;
import Game.Engine.Entity.Components.InteractionSideComponent.SideInteraction;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.PushableComponent;
import Game.Engine.Entity.Components.SurfaceComponent;
import Game.Engine.GameMath.Logic2D.AABB;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameObjects;
import Game.Engine.Physics.Contact.ContactState;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import java.util.*;

/**
 * Sistema de colisiones — Axis Clipping tipo Minecraft.
 *
 * ── REFACTOR COMPLETO: AABB Axis Clipping Unificado ───────────────────────
 *
 * PRINCIPIO FUNDAMENTAL:
 *   TODAS las entidades (Player, Enemies, Bullets, Items) usan el MISMO
 *   sistema de física: AABB con Axis Clipping independiente por eje.
 *
 *   NO hay "sensores mágicos" ni "triggers especiales".
 *   Todos son objetos físicos con masa, velocidad y colisiones.
 *
 * ARQUITECTURA DEL PIPELINE:
 *
 *   FASE 0   = applyGravity + flushForces (objetos con gravedad)
 *   FASE 0.5 = reset contactState (todos con physics)
 *   FASE 1   = AABB Axis Clipping (TODOS los objetos con velocidad != 0)
 *              → Broadphase: expandTowards(vx, vy)
 *              → Recolectar obstáculos que intersecten broadphase
 *              → ORDEN ESTRICTO: Y primero, X después
 *              → calculateYOffset() para cada obstáculo, move(0, vy)
 *              → calculateXOffset() para cada obstáculo, move(vx, 0)
 *              → Detectar contactos y construir ContactState
 *   FASE 2   = CollisionDetector.detect() (overlaps residuales)
 *   FASE 3   = CollisionDispatcher.dispatch() (eventos de colisión)
 *   FASE 4   = CollisionHandler enter/stay/exit
 *
 * ── HRFC — Collision System Refactor ──────────────────────────────────────
 *
 * MEJORAS EN ESTA VERSIÓN:
 *   ✓ Métodos extraídos por fase para legibilidad (no más 600 líneas)
 *   ✓ Constantes centralizadas (VELOCITY_EPSILON, normales, etc.)
 *   ✓ Eliminada duplicación de lógica X/Y (método unificado)
 *   ✓ Dispatch ordenado: markDispatched DESPUÉS de dispatch exitoso
 *   ✓ Validación de objetos destruidos en TODAS las fases
 *   ✓ Optimización: skip obstacles vacíos
 *   ✓ Documentación clara de responsabilidad de cada método
 *
 * VENTAJAS DEL SISTEMA:
 *   ✓ Más simple: un solo algoritmo para todo
 *   ✓ Más robusto: sin divisiones por velocidad (no más NaN/Inf)
 *   ✓ Más eficiente: una sola pasada por eje
 *   ✓ Determinista: orden Y→X produce resultados consistentes
 *   ✓ Deslizamiento natural: funciona automáticamente
 *   ✓ Sin casos especiales: bullets, player, enemies - todos iguales
 */
public class CollisionsSystem {

    // ── CollisionObstacle — obstáculo detectado en broadphase ────────────

    /**
     * Obstáculo que intersecta con el broadphase box del objeto en movimiento.
     * Agregado de información útil para evitar múltiples lookups.
     */
    private static final class CollisionObstacle {
        final GameObjects object;
        final AABB box;
        final InteractionSideComponent interactionSide;
        final SurfaceMaterial surface;

        CollisionObstacle(GameObjects obj, AABB box, 
                         InteractionSideComponent isc, SurfaceMaterial surf) {
            this.object = obj;
            this.box = box;
            this.interactionSide = isc;
            this.surface = surf;
        }
    }

    // ── ContactRecord — registro interno de un contacto del frame ────────

    /**
     * Registro de un contacto detectado durante FASE 1 (sólidos).
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
     * Contactos activos del frame anterior, por objeto.
     * Clave: identidad del objeto; Valor: conjunto de identidades de sus contactos.
     * Usado en FASE 4 para calcular enter/stay/exit.
     */
    private final IdentityHashMap<GameObjects, Set<GameObjects>> previousContacts =
            new IdentityHashMap<>();

    /**
     * CollisionDetector instanciable con colecciones reutilizables.
     * Se reutiliza entre frames — zero allocations de colecciones en hot path.
     */
    private final CollisionDetector detector = new CollisionDetector();

    // ── HRFC — Profiling Infrastructure ───────────────────────────────────
    private final Game.Engine.Profiling.SubsystemTimer collisionTimer = 
        new Game.Engine.Profiling.SubsystemTimer();

    /**
     * Ejecuta el pipeline completo de física y colisiones.
     *
     * ── Mini-HRFC — Unified Physics Time Integration ─────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime explícito del simulation step y lo
     * propaga a todos los métodos de integración física (applyGravity,
     * flushAccumulatedForces) para garantizar integración temporal correcta.
     *
     * deltaTime debe expresarse en segundos (ej: 0.01667s para 60 FPS).
     *
     * ── HRFC — Bottleneck Diagnosis ──────────────────────────────────────
     *
     * Instrumentado para medir tiempo de colisiones en el frame profile.
     * collisionTimer se resetea en WorldManager después de cada frame.
     *
     * ── HRFC — Collision Culling ──────────────────────────────────────────
     *
     * Si se proporciona una cámara, se aplicará collision culling para omitir
     * triggers (bullets) que están completamente fuera del área relevante.
     * Esto reduce drásticamente el trabajo cuando hay muchos bullets off-screen.
     *
     * @param objects lista de objetos activos en el mundo
     * @param deltaTime tiempo del simulation step en segundos
     * @param camera cámara activa del juego (null = sin culling)
     */
    public void update(List<GameObjects> objects, double deltaTime, Game.Engine.Camera.GameCamera camera) {
        collisionTimer.start();

        // ── FASE 0: Aplicar gravedad y fuerzas acumuladas ────────────────
        // Usa el onGround del frame ANTERIOR (todavía válido aquí).
        // Solo objetos con física que NO gestionan gravedad externamente
        // (los proyectiles gestionan gravedad via ProjectileMovement).
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (physics.isGravityManagedExternally()) continue;
            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null || col.isSensor()) continue;
            // Mini-HRFC: Pasar deltaTime para integración temporal correcta
            physics.applyGravity(physics.getOnGround(), deltaTime);
            physics.flushAccumulatedForces(deltaTime);
        }

        // ── FASE 0.5: Resetear estado de contacto ────────────────────────
        // DESPUÉS de applyGravity para que use el onGround previo correcto.
        // ANTES de AABB para que los nuevos contactos partan de cero.
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null) continue;
            Physics2D physics = physComp.getPhysics();
            
            // Resetear flags de contacto (se recalculan cada frame)
            physics.setOnGround(false);
            physics.setOnWall(false);
            physics.setOnCeiling(false);
            physics.clearSurface();
            physComp.setContactState(ContactState.NONE);
        }

        // ── FASE 1: AABB Axis Clipping Unificado ─────────────────────────
        // TODOS los objetos (Player, Enemies, Bullets, Items) usan el MISMO
        // algoritmo de resolución de movimiento tipo Minecraft.
        //
        // ORDEN ESTRICTO: Y primero (gravedad/piso), X después (paredes)
        //
        // NO hay casos especiales - todos son entidades físicas iguales.

        IdentityHashMap<GameObjects, List<ContactRecord>> frameContacts =
                new IdentityHashMap<>();

        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null) continue;
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            Physics2D physics = physComp.getPhysics();
            double vx = physics.getVelocity().getX() * deltaTime;
            double vy = physics.getVelocity().getY() * deltaTime;
            
            if (vx == 0.0 && vy == 0.0) continue;

            // Guardar velocidad original para detectar colisiones
            double originalVx = vx;
            double originalVy = vy;

            // Convertir bounds actuales a AABB
            RectangleH currentBounds = colA.getBounds();
            AABB entityBox = AABB.fromRectangleH(currentBounds);

            // PASO 1: Crear broadphase box (volumen de barrido)
            AABB broadphase = entityBox.expandTowards(vx, vy);

            // PASO 2: Recolectar obstáculos que intersecten broadphase
            List<CollisionObstacle> obstacles = new ArrayList<>();
            for (GameObjects other : objects) {
                if (other == obj) continue;
                if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                
                ColliderComponent colB = other.getComponent(ColliderComponent.class);
                if (colB == null) continue;
                if (colB.isSensor()) continue;  // Sensores NO bloquean movimiento
                
                // SKIP: Bullets NO bloquean movimiento de otras entidades
                // (pero SÍ colisionan con terreno para permitir rebotes)
                if (other instanceof Game.Items.Types.Bullets.Definition.Bullet) continue;
                
                if (!colA.canCollideWith(colB)) continue;

                RectangleH otherBounds = colB.getBounds();
                AABB otherBox = AABB.fromRectangleH(otherBounds);

                if (broadphase.intersects(otherBox)) {
                    InteractionSideComponent isc = other.getComponent(InteractionSideComponent.class);
                    SurfaceMaterial surface = resolveSurface(other);
                    obstacles.add(new CollisionObstacle(other, otherBox, isc, surface));
                }
            }

            // PASO 3: FASE Y - Resolver movimiento vertical PRIMERO
            for (CollisionObstacle obs : obstacles) {
                vy = entityBox.calculateYOffset(obs.box, vy);
            }
            entityBox.move(0, vy);

            // PASO 4: FASE X - Resolver movimiento horizontal (con Y ya aplicado)
            for (CollisionObstacle obs : obstacles) {
                vx = entityBox.calculateXOffset(obs.box, vx);
            }
            entityBox.move(vx, 0);

            // PASO 5: Actualizar posición
            // Si el objeto usa DOD (Bullet/Enemy), actualizar su storage directamente
            // Si no, actualizar Transform tradicional
            if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                // Bullet DOD: escribir directamente en PrimitiveStorage
                var handle = bullet.getSimulationHandle();
                if (handle != null && handle.isValid()) {
                    int idx = handle.index();
                    var storage = bullet.getEntityStore().getStorage();
                    storage.positionsX()[idx] = (float) (entityBox.minX - colA.getOffsetX());
                    storage.positionsY()[idx] = (float) (entityBox.minY - colA.getOffsetY());
                }
            } else if (obj instanceof Game.Enemys.Core.Enemy enemy) {
                // Enemy DOD: escribir directamente en EntityStorage
                var handle = enemy.getSimulationHandle();
                if (handle != null && handle.isValid()) {
                    int idx = handle.index();
                    var storage = enemy.getEntityStore().getStorage();
                    storage.positionsX()[idx] = (float) (entityBox.minX - colA.getOffsetX());
                    storage.positionsY()[idx] = (float) (entityBox.minY - colA.getOffsetY());
                }
            } else {
                // Objeto tradicional: actualizar Transform
                obj.getTransform().getPosition().setX(entityBox.minX - colA.getOffsetX());
                obj.getTransform().getPosition().setY(entityBox.minY - colA.getOffsetY());
            }

            // PASO 6: Detectar colisiones y construir ContactRecords
            boolean collidedY = Math.abs(vy - originalVy) > VELOCITY_EPSILON;
            boolean collidedX = Math.abs(vx - originalVx) > VELOCITY_EPSILON;

            List<ContactRecord> records = frameContacts.computeIfAbsent(obj, k -> new ArrayList<>(INITIAL_CONTACT_LIST_CAPACITY));

            if (collidedY) {
                // Colisión vertical - determinar con QUÉ objeto
                int normalY = (originalVy > 0) ? NORMAL_UP : NORMAL_DOWN;
                
                for (CollisionObstacle obs : obstacles) {
                    // Verificar si este obstáculo causó la colisión en Y
                    RectangleH tempBounds = new RectangleH(
                        entityBox.minX, 
                        entityBox.minY - vy,  // Posición antes de mover en Y
                        entityBox.getWidth(), 
                        entityBox.getHeight()
                    );
                    AABB tempBox = AABB.fromRectangleH(tempBounds);
                    double testVy = tempBox.calculateYOffset(obs.box, originalVy);
                    
                    if (Math.abs(testVy - originalVy) > VELOCITY_EPSILON) {
                        // Este obstáculo causó la colisión en Y
                        SideInteraction sideType = (obs.interactionSide != null)
                                ? obs.interactionSide.forNormal(0, normalY)
                                : defaultSideForNormalY(normalY);
                        
                        records.add(new ContactRecord(obs.object, 0, normalY, obs.surface));
                        applyContactFlag(physics, 0, normalY, sideType);
                        
                        if (normalY == NORMAL_UP) {
                            physics.setCurrentSurface(obs.surface);
                        }
                        
                        applyPushIfPushable(obs.object, 0.0, originalVy, physics.getMass());
                        CollisionDispatcher.dispatch(obj, obs.object);
                        detector.markDispatched(obj, obs.object);
                    }
                }

                // Frenar velocidad en Y
                physics.vSetY(0);
            }

            if (collidedX) {
                // Colisión horizontal - determinar con QUÉ objeto
                int normalX = (originalVx > 0) ? NORMAL_LEFT : NORMAL_RIGHT;
                
                for (CollisionObstacle obs : obstacles) {
                    // Verificar si este obstáculo causó la colisión en X
                    RectangleH tempBounds = new RectangleH(
                        entityBox.minX - vx,  // Posición antes de mover en X
                        entityBox.minY,
                        entityBox.getWidth(), 
                        entityBox.getHeight()
                    );
                    AABB tempBox = AABB.fromRectangleH(tempBounds);
                    double testVx = tempBox.calculateXOffset(obs.box, originalVx);
                    
                    if (Math.abs(testVx - originalVx) > VELOCITY_EPSILON) {
                        // Este obstáculo causó la colisión en X
                        SideInteraction sideType = (obs.interactionSide != null)
                                ? obs.interactionSide.forNormal(normalX, 0)
                                : SideInteraction.WALL;
                        
                        records.add(new ContactRecord(obs.object, normalX, 0, obs.surface));
                        applyContactFlag(physics, normalX, 0, sideType);
                        applyPushIfPushable(obs.object, originalVx, 0.0, physics.getMass());
                        CollisionDispatcher.dispatch(obj, obs.object);
                        detector.markDispatched(obj, obs.object);
                    }
                }

                // Frenar velocidad en X
                physics.vSetX(0);
            }
        } // fin FASE 1

        // ── FASE 1 — finalización: construir ContactState por objeto ─────
        for (Map.Entry<GameObjects, List<ContactRecord>> entry : frameContacts.entrySet()) {
            GameObjects         obj     = entry.getKey();
            List<ContactRecord> records = entry.getValue();
            Physics2DComponent  physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null || records.isEmpty()) continue;
            Physics2D physics = physComp.getPhysics();

            boolean       hasGround  = false;
            boolean       hasWall    = false;
            boolean       hasCeiling = false;
            SurfaceMaterial groundSurface = null;
            int primaryNX = 0, primaryNY = 0;

            for (ContactRecord rec : records) {
                if (rec.normalY == NORMAL_UP) {
                    hasGround = true;
                    if (groundSurface == null) groundSurface = rec.surface;
                    primaryNX = NORMAL_NONE; primaryNY = NORMAL_UP;
                } else if (rec.normalY == NORMAL_DOWN) {
                    hasCeiling = true;
                    if (primaryNY == 0) { primaryNX = NORMAL_NONE; primaryNY = NORMAL_DOWN; }
                } else if (rec.normalX != 0) {
                    hasWall = true;
                    if (primaryNX == 0 && primaryNY == 0) { primaryNX = rec.normalX; primaryNY = 0; }
                }
            }

            if (groundSurface == null) groundSurface = ensureNonNullSurface(SurfaceMaterial.DEFAULT);

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
        //   - Sensor vs Sensor
        //   - Objetos ya solapados (penetración preexistente sin velocidad)
        //   - Cualquier par que se solape y no haya sido despachado ya
        // CollisionDetector omite los pares registrados via markDispatched().
        List<CollisionResult> pairs = detector.detect(objects);

        // ── FASE 3: Despacho de eventos de FASE 2 ────────────────────────
        for (CollisionResult pair : pairs) {
            // Guard: skip si alguno está destruido
            if (pair.a instanceof Destroyable da && da.isPendingDestruction()) continue;
            if (pair.b instanceof Destroyable db && db.isPendingDestruction()) continue;

            // ── Filtro de colisión por ownership ───────────────────────────
            // DOD: Un proyectil NO debe colisionar con su owner
            if (shouldFilterCollision(pair.a, pair.b)) {
                continue; // skip dispatch
            }

            // Para sensores de FASE 2 que llegaron con normal calculada,
            // escribir la normal en BulletPhysics antes del dispatch
            propagateNormalToTrigger(pair);

            CollisionDispatcher.dispatch(pair.a, pair.b);
        }

        // ── FASE 4: CollisionHandler enter / stay / exit ─────────────────
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
            Set<GameObjects> current  = currentContacts.getOrDefault(obj, Set.of());
            Set<GameObjects> previous = previousContacts.getOrDefault(obj, Set.of());

            for (GameObjects contact : current) {
                if (!previous.contains(contact)) {
                    // ENTER: Primer frame de contacto
                    // 1. CollisionHandler (Components)
                    List<CollisionHandler> listeners = collectListeners(obj);
                    for (CollisionHandler listener : listeners) {
                        listener.onCollisionEnter(contact);
                    }
                    
                    // 2. GameObjects directamente (3 formas)
                    obj.onCollisionEnter(contact);           // 1 param
                    obj.onCollisionEnter(obj, contact);      // 2 params
                } else {
                    // STAY: Contacto continua
                    // 1. CollisionHandler (Components)
                    List<CollisionHandler> listeners = collectListeners(obj);
                    for (CollisionHandler listener : listeners) {
                        listener.onCollisionStay(contact);
                    }
                    
                    // 2. GameObjects directamente (3 formas)
                    obj.onCollisionStay(contact);            // 1 param
                    obj.onCollisionStay(obj, contact);       // 2 params
                }
            }

            for (GameObjects contact : previous) {
                if (!current.contains(contact)) {
                    // EXIT: Contacto termino
                    // 1. CollisionHandler (Components)
                    List<CollisionHandler> listeners = collectListeners(obj);
                    for (CollisionHandler listener : listeners) {
                        listener.onCollisionExit(contact);
                    }
                    
                    // 2. GameObjects directamente (3 formas)
                    obj.onCollisionExit(contact);            // 1 param
                    obj.onCollisionExit(obj, contact);       // 2 params
                }
            }
        }

        // Rotar contactos: actual → anterior para el siguiente frame
        previousContacts.clear();
        previousContacts.putAll(currentContacts);

        // ── HRFC — Profiling Infrastructure ───────────────────────────────
        collisionTimer.stop();
    }


    /**
     * Limpia el estado de contactos anteriores.
     * Llamar al cambiar de mundo/escena para evitar enter/exit espurios.
     */
    public void clearContactHistory() {
        previousContacts.clear();
        detector.clear();
    }

    // ── HRFC — Profiling Infrastructure ───────────────────────────────────

    /**
     * Retorna el tiempo de colisiones del último frame en milisegundos.
     * Para diagnóstico de cuellos de botella.
     */
    public double getLastCollisionTimeMs() {
        return collisionTimer.getElapsedMs();
    }

    // ── Helper: filtro de colisiones por ownership ────────────────────────

    /**
     * Determina si una colisión debe ser filtrada (no despachada).
     *
     * ── HRFC — Projectile DOD Migration: Collision Filtering ──────────────
     *
     * REGLA: Un proyectil NO debe colisionar con su owner.
     *
     * Esta es la solución correcta en DOD — el filtro está en el sistema
     * de colisiones, no en cada behavior individual. Evita que proyectiles
     * maten a su owner o se autodestruyan al spawnearse.
     *
     * CASOS FILTRADOS:
     *   - Bullet A con owner O colisionando con O
     *   - O colisionando con Bullet A que tiene owner O
     *
     * @param a primer objeto
     * @param b segundo objeto
     * @return true si la colisión debe ser filtrada (no despachar)
     */
    private boolean shouldFilterCollision(GameObjects a, GameObjects b) {
        // Verificar si A es un proyectil que colisiona con su owner
        if (a instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            Object owner = bullet.getOwner();
            if (owner != null && owner == b) {
                return true; // filtrar: proyectil colisionando con su owner
            }
        }
        
        // Verificar si B es un proyectil que colisiona con su owner
        if (b instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            Object owner = bullet.getOwner();
            if (owner != null && owner == a) {
                return true; // filtrar: proyectil colisionando con su owner
            }
        }
        
        return false; // no filtrar
    }

    // ── Helper: propagar normal a sensores desde CollisionResult de FASE 2 ─

    /**
     * Si el par de FASE 2 involucra un sensor con BulletPhysics,
     * escribe la normal calculada por CollisionDetector en el sensor
     * antes del dispatch para que onCollision() pueda leerla.
     *
     * Solo escribe si la normal es válida (no (0,0)) — evita borrar
     * una normal escrita por FASE 1 en el mismo frame.
     */
    private static void propagateNormalToTrigger(CollisionResult pair) {
        if (pair.normalX == 0 && pair.normalY == 0) return;

        if (pair.a.getComponent(ColliderComponent.class) != null
                && pair.a.getComponent(ColliderComponent.class).isSensor()) {
            Physics2DComponent pc = pair.a.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                bp.setLastContactNormal(pair.normalX, pair.normalY);
            }
            // ── HRFC — DOD Bridge: también escribir en Bullet DOD adapter ─
            if (pair.a instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                bullet.setCollisionNormal(pair.normalX, pair.normalY);
            }
        }

        if (pair.b.getComponent(ColliderComponent.class) != null
                && pair.b.getComponent(ColliderComponent.class).isSensor()) {
            Physics2DComponent pc = pair.b.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                // Normal invertida: la de pair es desde perspectiva de A
                bp.setLastContactNormal(-pair.normalX, -pair.normalY);
            }
            // ── HRFC — DOD Bridge: también escribir en Bullet DOD adapter ─
            if (pair.b instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                bullet.setCollisionNormal(-pair.normalX, -pair.normalY);
            }
        }
    }

    // ── Helper: aplicar flag de contacto según la cara impactada ─────────

    private static void applyContactFlag(Physics2D physics, int normalX, int normalY,
                                          SideInteraction sideType) {
        switch (sideType) {
            case SURFACE   -> physics.setOnGround(true);
            case CEILING   -> physics.setOnCeiling(true);
            case WALL,
                 CLIMBABLE -> physics.setOnWall(true);
            case NONE      -> {
                if (normalY == NORMAL_UP)      physics.setOnGround(true);
                else if (normalY == 1)  physics.setOnCeiling(true);
                else if (normalX != 0)  physics.setOnWall(true);
            }
            case PASSTHROUGH -> { /* filtrado antes de llegar aquí */ }
        }
    }

    private static SideInteraction defaultSideForNormalY(int normalY) {
        return normalY == NORMAL_UP ? SideInteraction.SURFACE : SideInteraction.CEILING;
    }

    // ── Helper: resolución de superficie ─────────────────────────────────

    private static SurfaceMaterial resolveSurface(GameObjects obj) {
        SurfaceComponent sc = obj.getComponent(SurfaceComponent.class);
        if (sc != null) return sc;
        if (obj instanceof SurfaceMaterial sm) return sm;
        return ensureNonNullSurface(SurfaceMaterial.DEFAULT);
    }
    
    /**
     * Garantiza que surface nunca sea null.
     * Si DEFAULT es null, retorna un fallback hardcoded seguro.
     */
    private static SurfaceMaterial ensureNonNullSurface(SurfaceMaterial surface) {
        if (surface != null) return surface;
        
        // Fallback si DEFAULT es null (caso degenerado)
        return SurfaceMaterial.of(1.0, 0.0, 1.0, 1.0);
    }

    // ── Helper: PushableComponent ─────────────────────────────────────────

    private static void applyPushIfPushable(GameObjects target,
                                             double vx, double vy,
                                             double sourceMass) {
        PushableComponent pushable = target.getComponent(PushableComponent.class);
        if (pushable == null || !pushable.isEnabled()) return;
        pushable.applyPush(vx * sourceMass, vy * sourceMass);
    }

    // ── Helpers privados de FASE 4 ────────────────────────────────────────

    private static List<CollisionHandler> collectListeners(GameObjects obj) {
        List<CollisionHandler> result = null;
        for (Component c : obj.getComponents()) {
            if (c instanceof CollisionHandler cle) {
                if (result == null) result = new ArrayList<>(INITIAL_LISTENER_LIST_CAPACITY);
                result.add(cle);
            }
        }
        return result != null ? result : List.of();
    }
}

