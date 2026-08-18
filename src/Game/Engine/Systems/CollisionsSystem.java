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
import Game.Engine.GameObjects;
import Game.Engine.Physics.Contact.ContactState;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import java.awt.Rectangle;
import java.util.*;

/**
 * Sistema de colisiones — pipeline unificado por frame.
 *
 * ── HRFC — Unificación del pipeline de colisiones ─────────────────────────
 *
 * PRINCIPIO FUNDAMENTAL (Diseño B):
 *   La matemática de detección geométrica es ÚNICA para todos los objetos.
 *   La diferencia entre SÓLIDO y TRIGGER está en la POLÍTICA DE RESPUESTA,
 *   no en la detección.
 *
 *   SÓLIDO  → SweptAABB eje-por-eje → resolución de velocidad + ContactState
 *   TRIGGER → SweptAABB 2D completo → setLastContactNormal + dispatch (sin resolución)
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   ANTES:
 *     FASE 1   = SweptAABB solo para SÓLIDOS
 *     FASE 2   = AABB estática para TODOS (incluidos TRIGGERS)
 *     Bullets nunca recibían normal de colisión
 *     Bullets no participaban en CCD
 *     moveByPhysics() en Bullet.update() antes de CollisionsSystem
 *
 *   AHORA:
 *     FASE 0   = applyGravity + flushForces          [SÓLIDOS]
 *     FASE 0.5 = reset contactState + normal trigger  [SÓLIDOS + TRIGGERS]
 *     FASE 1   = SweptAABB eje X, luego eje Y         [SÓLIDOS no-trigger]
 *                 → política sólida: resolveVelocity + ContactState
 *     FASE 1B  = SweptAABB 2D simultáneo              [TRIGGERS con velocity != 0]
 *                 → política trigger: setLastContactNormal + dispatch
 *                 → integra el movimiento del trigger (mover hasta contacto)
 *                 → CollisionDetector.markDispatched() para evitar duplicado
 *     FASE 2   = CollisionDetector.detect()           [overlaps residuales]
 *                 (con colecciones reutilizables, con normal calculada)
 *     FASE 3   = dispatch pares FASE 2
 *                 → guard: omitir si algún objeto está destruido
 *     FASE 4   = CollisionListener enter/stay/exit
 *
 * ORDEN DEL PIPELINE:
 *   Bullet.update() llama movement.tick() + behavior.onUpdate() pero YA NO
 *   llama moveByPhysics(). CollisionsSystem integra la posición del trigger
 *   en FASE 1B con CCD, garantizando que la detección es swept y que la
 *   normal está disponible cuando onCollision() se ejecuta.
 *
 * INVARIANTES GARANTIZADOS:
 *   1. Todo objeto con velocity != 0 recibe detección swept (no AABB snapshot).
 *   2. La normal del impacto siempre está disponible en onCollision().
 *   3. Un par no se despacha más de una vez por frame.
 *   4. Objetos destruidos durante dispatch no reciben eventos adicionales.
 *   5. Bullets mueven su posición dentro de CollisionsSystem con CCD.
 */
public class CollisionsSystem {

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
     * Epsilon de sub-pixel aplicado al trigger en contactos CCD limpios (time > 0).
     *
     * ── Justificación ─────────────────────────────────────────────────────
     * ColliderComponent.getBounds() hace (int) pos.getX() — truncado hacia cero.
     * Con un time exactamente calculado por SweptAABB, la posición double queda
     * en el borde geométrico real. El truncado (int) puede reportar al objeto
     * 0–0.999px dentro del collider aunque la posición double esté en el borde
     * o apenas fuera.
     *
     * Para contactos CCD limpios (time > 0) basta un epsilon de 0.5:
     *   - Es sub-pixel: no desplaza visiblemente el proyectil.
     *   - Sobrevive al truncado (int): pos_double = borde + 0.5 → (int) = borde,
     *     que es exactamente el borde del collider, no dentro de él.
     *   - No produce overshoot: 0.5 < 1 unidad, por lo que nunca puede
     *     penetrar un collider adyacente (tamaño mínimo 4px en bullets).
     *
     * ── Por qué NO 1.0 (el valor anterior) ──────────────────────────────
     * 1.0 producía oscilación en contactos tangenciales:
     *   frame N  → bullet en borde exacto → separar 1.0 en dirección normal
     *   frame N+1 → bullet 1.0px fuera → velocidad reflejada la devuelve al borde
     *   frame N+2 → contacto de nuevo con time ≈ 0 → separar 1.0 otra vez → loop
     * Con 0.5 la separación es sub-pixel, la velocidad reflejada aleja la bullet
     * más que 0.5px por frame (velocidad mínima de bullets > 1px/frame),
     * así que el ciclo de re-contacto no ocurre.
     *
     * ── Para time = 0 (ya solapado) ──────────────────────────────────────
     * Se usa computePenetrationSeparation() para calcular el desplazamiento
     * exacto necesario para salir del solapamiento + CONTACT_EPSILON.
     */
    private static final double CONTACT_EPSILON = 0.5;

    /**
     * Separación adicional sobre la penetración real para contactos time=0.
     * Se suma a la penetración calculada para garantizar que el objeto quede
     * fuera del collider después de la separación.
     * Mismo valor que CONTACT_EPSILON — coherencia arquitectural.
     */
    private static final double PENETRATION_EPSILON = 0.5;

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
     * @param objects lista de objetos activos en el mundo
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void update(List<GameObjects> objects, double deltaTime) {

        // ── FASE 0: Aplicar gravedad y fuerzas acumuladas ────────────────
        // Usa el onGround del frame ANTERIOR (todavía válido aquí).
        // Solo objetos SÓLIDOS — los triggers gestionan su gravedad
        // externamente via ProjectileMovement (isGravityManagedExternally).
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (physics.isGravityManagedExternally()) continue;
            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null || col.isTrigger()) continue;
            // Mini-HRFC: Pasar deltaTime para integración temporal correcta
            physics.applyGravity(physics.getOnGround(), deltaTime);
            physics.flushAccumulatedForces(deltaTime);
        }

        // ── FASE 0.5: Resetear estado de contacto ────────────────────────
        // DESPUÉS de applyGravity para que use el onGround previo correcto.
        // ANTES de SweptAABB para que los nuevos contactos partan de cero.
        // Para TRIGGERS: limpia lastContactNormal (escrito en el frame anterior).
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (!colA.isTrigger()) {
                // Sólido: resetear flags de contacto
                physics.setOnGround(false);
                physics.setOnWall(false);
                physics.setOnCeiling(false);
                physics.clearSurface();
                physComp.setContactState(ContactState.NONE);
            } else {
                // Trigger: resetear normal de contacto
                if (physics instanceof BulletPhysics bp) {
                    bp.clearLastContactNormal();
                }
            }
        }


        // ── FASE 1: SweptAABB eje-por-eje para SÓLIDOS ───────────────────
        // Solo objetos no-trigger con Physics2DComponent y velocity != 0.
        // Política sólida: resuelve movimiento, escribe ContactState.

        IdentityHashMap<GameObjects, List<ContactRecord>> frameContacts =
                new IdentityHashMap<>();

        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            Physics2D physics = physComp.getPhysics();
            // ── Mini-HRFC 1.5: Temporal integration correction ────────────
            // velocity está en units/second. Debe multiplicarse por deltaTime
            // para obtener displacement en units.
            // ANTES: vx = velocity.x  (tratado como px/frame)
            // AHORA: vx = velocity.x × deltaTime  (displacement correcto)
            double    vx      = physics.getVelocity().getX() * deltaTime;
            double    vy      = physics.getVelocity().getY() * deltaTime;
            if (vx == 0.0 && vy == 0.0) continue;

            List<ContactRecord> records = frameContacts.computeIfAbsent(
                    obj, k -> new ArrayList<>(4));

            // ── Eje X ──────────────────────────────────────────────────
            if (vx != 0.0) {
                Rectangle bounds = colA.getBounds();
                Rectangle broadX = new Rectangle(
                        (int)(vx < 0 ? bounds.x + vx : bounds.x + bounds.width),
                        bounds.y + 1,
                        (int)(Math.abs(vx) + 1),
                        bounds.height - 2
                );

                double      nearestTimeX = 1.0;
                int         hitNormalX   = 0;
                GameObjects hitX         = null;

                for (GameObjects other : objects) {
                    if (other == obj) continue;
                    if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;

                    InteractionSideComponent iscB = other.getComponent(InteractionSideComponent.class);
                    Rectangle ob = colB.getBounds();
                    if (ob.y >= bounds.y + bounds.height) continue;
                    if (ob.y + ob.height <= bounds.y) continue;
                    if (!broadX.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, vx, 0.0);
                    if (!r.hasCollision() || r.time >= nearestTimeX) continue;

                    if (iscB != null) {
                        SideInteraction interaction = iscB.forNormal(r.normalX, r.normalY);
                        if (interaction == SideInteraction.PASSTHROUGH) continue;
                    }

                    nearestTimeX = r.time;
                    hitNormalX   = r.normalX;
                    hitX         = other;
                }

                PhysicsStepper.moveWith(obj, vx * nearestTimeX, 0);

                if (hitX != null && hitNormalX != 0) {
                    resolveVelocityX(physics, hitX, vx);
                    InteractionSideComponent iscHit = hitX.getComponent(InteractionSideComponent.class);
                    SideInteraction sideType = (iscHit != null)
                            ? iscHit.forNormal(hitNormalX, 0) : SideInteraction.WALL;
                    SurfaceMaterial surface = resolveSurface(hitX);
                    records.add(new ContactRecord(hitX, hitNormalX, 0, surface));
                    applyContactFlag(physics, hitNormalX, 0, sideType);
                    applyPushIfPushable(hitX, vx, 0.0, physics.getMass());
                    detector.markDispatched(obj, hitX);
                    CollisionDispatcher.dispatch(obj, hitX);
                }
            }


            // ── Eje Y ──────────────────────────────────────────────────
            if (vy != 0.0) {
                Rectangle bounds = colA.getBounds();
                Rectangle broadY = new Rectangle(
                        bounds.x,
                        (int)(vy < 0 ? bounds.y + vy : bounds.y),
                        bounds.width,
                        (int)(bounds.height + Math.abs(vy))
                );

                double      nearestTimeY = 1.0;
                int         hitNormalY   = 0;
                GameObjects hitY         = null;

                for (GameObjects other : objects) {
                    if (other == obj) continue;
                    if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;

                    Rectangle ob = colB.getBounds();
                    if (!broadY.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, 0.0, vy);
                    if (!r.hasCollision() || r.time >= nearestTimeY) continue;

                    InteractionSideComponent iscB = other.getComponent(InteractionSideComponent.class);
                    if (iscB != null) {
                        SideInteraction interaction = iscB.forNormal(r.normalX, r.normalY);
                        if (interaction == SideInteraction.PASSTHROUGH) continue;
                    }

                    nearestTimeY = r.time;
                    hitNormalY   = r.normalY;
                    hitY         = other;
                }

                PhysicsStepper.moveWith(obj, 0, vy * nearestTimeY);

                if (hitY != null && hitNormalY != 0) {
                    resolveVelocityY(physics, hitY, vy);
                    InteractionSideComponent iscHit = hitY.getComponent(InteractionSideComponent.class);
                    SideInteraction sideType = (iscHit != null)
                            ? iscHit.forNormal(0, hitNormalY)
                            : defaultSideForNormalY(hitNormalY);
                    SurfaceMaterial surface = resolveSurface(hitY);
                    records.add(new ContactRecord(hitY, 0, hitNormalY, surface));
                    applyContactFlag(physics, 0, hitNormalY, sideType);
                    if (hitNormalY == -1) physics.setCurrentSurface(surface);
                    applyPushIfPushable(hitY, 0.0, vy, physics.getMass());
                    detector.markDispatched(obj, hitY);
                    CollisionDispatcher.dispatch(obj, hitY);
                }
            }
        } // fin loop FASE 1


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


        // ── FASE 1B: SweptAABB 2D para TRIGGERS en vuelo ─────────────────
        //
        // Política trigger: detecta impacto con CCD completo (ambos ejes),
        // escribe la normal en BulletPhysics.setLastContactNormal(),
        // mueve el trigger hasta el punto de contacto, luego dispatch.
        //
        // Los triggers NO reciben resolución de velocidad — su behavior
        // (BulletJump, etc.) decide qué hacer con la velocidad en onCollision.
        //
        // Solo se comparan triggers contra NO-triggers (objetos sólidos del mundo).
        // Trigger vs Trigger se maneja en FASE 2 si es necesario.

        for (GameObjects obj : objects) {
            ColliderComponent colA = obj.getComponent(ColliderComponent.class);
            if (colA == null || !colA.isTrigger()) continue;
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;

            Physics2D physics = physComp.getPhysics();
            // ── Mini-HRFC 1.5: Temporal integration correction ────────────
            // velocity está en units/second. Debe multiplicarse por deltaTime
            // para obtener displacement en units.
            double vx = physics.getVelocity().getX() * deltaTime;
            double vy = physics.getVelocity().getY() * deltaTime;
            if (vx == 0.0 && vy == 0.0) {
                // Sin velocidad — mover completo (no debería ocurrir para bullets)
                PhysicsStepper.moveWith(obj, 0, 0);
                continue;
            }

            Rectangle bounds = colA.getBounds();

            // Broad phase: swept bounds del path completo del frame
            // Cualquier obstáculo que no intersecte con este rect es imposible de impactar
            Rectangle sweptB = SweptAABB.sweptBounds(bounds, vx, vy);

            double      nearestTime  = 1.0;
            int         hitNormalX   = 0;
            int         hitNormalY   = 0;
            GameObjects hitObj       = null;

            for (GameObjects other : objects) {
                if (other == obj) continue;
                if (other instanceof Destroyable d && d.isPendingDestruction()) continue;
                ColliderComponent colB = other.getComponent(ColliderComponent.class);
                if (colB == null || colB.isTrigger()) continue;  // trigger vs solo sólidos aquí
                if (!colA.canCollideWith(colB)) continue;

                Rectangle ob = colB.getBounds();

                // Broad phase: descartar rápidamente sin swept
                if (!sweptB.intersects(ob)) continue;

                // Narrow phase: swept 2D completo
                SweptAABB.Result r = SweptAABB.calculate2D(bounds, ob, vx, vy);
                if (!r.hasCollision()) continue;
                if (r.time >= nearestTime) continue;

                nearestTime = r.time;
                hitNormalX  = r.normalX;
                hitNormalY  = r.normalY;
                hitObj      = other;
            }

            // Mover el trigger hasta el punto de contacto (o completo si no hay hit)
            PhysicsStepper.moveWith(obj, vx * nearestTime, vy * nearestTime);

            if (hitObj != null) {
                // ── Separación de contacto — corrección geométrica ────────────
                //
                // CAUSA DE LA OSCILACIÓN (diagnosticada en Mini-HRFC):
                //
                // El parche anterior aplicaba una separación fija de 1.0px en
                // dirección de la normal en TODOS los casos (time > 0 y time = 0).
                // Eso producía oscilación por dos razones distintas:
                //
                //   CASO time > 0 (contacto CCD limpio):
                //     moveWith(vx*t, vy*t) deja la bullet en el borde geométrico.
                //     Separar 1.0px en dirección normal → bullet demasiado lejos.
                //     La velocidad reflejada (ej: JUMP_BOOST = -14) devuelve la
                //     bullet al borde en el siguiente frame → re-contacto → 1.0px
                //     de nuevo → loop de oscilación a 1px de la superficie.
                //
                //   CASO time = 0 (penetración preexistente por truncado int):
                //     moveWith(0, 0) → posición sin cambio.
                //     Separar 1.0px fijo: puede ser demasiado (si la penetración
                //     real es 0.1px, 1.0px lo mete en el collider adyacente) o
                //     insuficiente (si la penetración real es 1.5px).
                //
                // SOLUCIÓN CORRECTA:
                //
                //   CASO time > 0 → solo aplicar CONTACT_EPSILON (0.5px sub-pixel).
                //     0.5px sobrevive al truncado (int) sin desplazar visiblemente
                //     el proyectil. La velocidad reflejada siempre aleja la bullet
                //     más de 0.5px/frame (velocidad mínima de bullets > 1px/frame).
                //
                //   CASO time = 0 → calcular la penetración real y desplazar
                //     exactamente pen + PENETRATION_EPSILON. Esto garantiza salida
                //     mínima del overlap sin overshoot hacia colliders adyacentes.
                //
                // La separación solo se aplica en el eje de la normal para no
                // desplazar lateralmente la bullet (el eje perpendicular es libre).
                Rectangle hitBounds = hitObj.getComponent(ColliderComponent.class).getBounds();
                double sepX, sepY;
                if (nearestTime > 0.0) {
                    // Contacto CCD limpio: separación sub-pixel
                    sepX = hitNormalX * CONTACT_EPSILON;
                    sepY = hitNormalY * CONTACT_EPSILON;
                } else {
                    // time = 0: penetración preexistente — calcular profundidad real
                    Rectangle trigBounds = colA.getBounds();
                    double pen = computePenetrationDepth(trigBounds, hitBounds, hitNormalX, hitNormalY);
                    sepX = hitNormalX * (pen + PENETRATION_EPSILON);
                    sepY = hitNormalY * (pen + PENETRATION_EPSILON);
                }
                PhysicsStepper.moveWith(obj, sepX, sepY);

                // Escribir normal en BulletPhysics ANTES del dispatch
                // para que onCollision() pueda leerla
                if (physics instanceof BulletPhysics bp) {
                    bp.setLastContactNormal(hitNormalX, hitNormalY);
                }

                // Evitar que FASE 2 despache este par de nuevo
                detector.markDispatched(obj, hitObj);

                CollisionDispatcher.dispatch(obj, hitObj);

                // ── Guard post-contacto: V·N debe ser positivo ────────────────
                //
                // Después del dispatch, el behavior (BulletJump, etc.) ha modificado
                // la velocidad. Verificar que la componente de velocidad en la
                // dirección de la normal sea positiva (alejándose del collider).
                //
                // Si la velocidad resultante apunta HACIA el collider (V·N < 0),
                // forzarla a cero en esa componente. Esto corta el ciclo de
                // re-contacto inmediato sin modificar la reflexión del behavior.
                //
                // Casos que activan el guard:
                //   1. El behavior no modificó la velocidad (bug en el behavior).
                //   2. Doble dispatch por FASE 2 que invirtió la velocidad ya corregida.
                //   3. Contacto de esquina con normal inconsistente.
                //
                // El guard es un safety net del sistema, no un reemplazo de la
                // reflexión correcta en el behavior. No debe activarse en condiciones
                // normales si el behavior está bien implementado.
                enforcePostContactVelocity(physics, hitNormalX, hitNormalY);
            }
        } // fin loop FASE 1B


        // ── FASE 2: Detección AABB (overlaps residuales) ─────────────────
        // Detecta pares que no fueron procesados en FASE 1 ni FASE 1B:
        //   - Trigger vs Trigger
        //   - Sólido vs Sólido ya solapados (penetración preexistente sin velocidad)
        //   - Cualquier par que se solape y no haya sido despachado ya
        // CollisionDetector omite los pares registrados via markDispatched().
        List<CollisionResult> pairs = detector.detect(objects);

        // ── FASE 3: Despacho de eventos de FASE 2 ────────────────────────
        for (CollisionResult pair : pairs) {
            // Guard: skip si alguno está destruido
            if (pair.a instanceof Destroyable da && da.isPendingDestruction()) continue;
            if (pair.b instanceof Destroyable db && db.isPendingDestruction()) continue;

            // Para triggers de FASE 2 que llegaron con normal calculada,
            // escribir la normal en BulletPhysics antes del dispatch
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
    }


    /**
     * Limpia el estado de contactos anteriores.
     * Llamar al cambiar de mundo/escena para evitar enter/exit espurios.
     */
    public void clearContactHistory() {
        previousContacts.clear();
        detector.clear();
    }

    // ── Helpers de resolución de velocidad (política sólida) ──────────────

    private static void resolveVelocityX(Physics2D physics, GameObjects other, double vx) {
        physics.getVelocity().setX(0);
    }

    private static void resolveVelocityY(Physics2D physics, GameObjects other, double vy) {
        physics.getVelocity().setY(0);
    }

    // ── Helper: propagar normal a trigger desde CollisionResult de FASE 2 ─

    /**
     * Si el par de FASE 2 involucra un trigger con BulletPhysics,
     * escribe la normal calculada por CollisionDetector en el trigger
     * antes del dispatch para que onCollision() pueda leerla.
     *
     * Solo escribe si la normal es válida (no (0,0)) — evita borrar
     * una normal escrita por FASE 1B en el mismo frame.
     */
    private static void propagateNormalToTrigger(CollisionResult pair) {
        if (pair.normalX == 0 && pair.normalY == 0) return;

        if (pair.a.getComponent(ColliderComponent.class) != null
                && pair.a.getComponent(ColliderComponent.class).isTrigger()) {
            Physics2DComponent pc = pair.a.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                bp.setLastContactNormal(pair.normalX, pair.normalY);
            }
        }

        if (pair.b.getComponent(ColliderComponent.class) != null
                && pair.b.getComponent(ColliderComponent.class).isTrigger()) {
            Physics2DComponent pc = pair.b.getComponent(Physics2DComponent.class);
            if (pc != null && pc.getPhysics() instanceof BulletPhysics bp
                    && !bp.hasContactNormal()) {
                // Normal invertida: la de pair es desde perspectiva de A
                bp.setLastContactNormal(-pair.normalX, -pair.normalY);
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
                if (normalY == -1)      physics.setOnGround(true);
                else if (normalY == 1)  physics.setOnCeiling(true);
                else if (normalX != 0)  physics.setOnWall(true);
            }
            case PASSTHROUGH -> { /* filtrado antes de llegar aquí */ }
        }
    }

    private static SideInteraction defaultSideForNormalY(int normalY) {
        return normalY == -1 ? SideInteraction.SURFACE : SideInteraction.CEILING;
    }

    // ── Helper: resolución de superficie ─────────────────────────────────

    private static SurfaceMaterial resolveSurface(GameObjects obj) {
        SurfaceComponent sc = obj.getComponent(SurfaceComponent.class);
        if (sc != null) return sc;
        if (obj instanceof SurfaceMaterial sm) return sm;
        return SurfaceMaterial.DEFAULT;
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
                if (result == null) result = new ArrayList<>(2);
                result.add(cle);
            }
        }
        return result != null ? result : List.of();
    }

    private static boolean isTriggerObj(GameObjects obj) {
        ColliderComponent col = obj.getComponent(ColliderComponent.class);
        return col != null && col.isTrigger();
    }

    // ── Helpers de separación de contacto ────────────────────────────────

    /**
     * Calcula la profundidad de penetración del trigger dentro del hit en el
     * eje de la normal del impacto.
     *
     * Se usa exclusivamente para contactos con time=0 (penetración preexistente),
     * donde SweptAABB ya no puede calcular el desplazamiento correcto porque el
     * objeto ya está dentro del obstáculo.
     *
     * ── Cálculo ───────────────────────────────────────────────────────────
     * Para normal (nx=1, ny=0) — pared derecha:
     *   La bullet viene desde la derecha y penetró la pared.
     *   pen = (hit.x + hit.width) - trig.x  → cuánto solapan en X desde la izquierda.
     *   Pero la normal es +X (bullet saldrá hacia la derecha), así que pen es:
     *   pen = (hit.x + hit.width) - trig.x
     *
     * La fórmula general es el overlap en el eje activo de la normal:
     *   Si nx != 0: pen = solapamiento en X = min(trig.x+trig.w, hit.x+hit.w) - max(trig.x, hit.x)
     *   Si ny != 0: pen = solapamiento en Y = min(trig.y+trig.h, hit.y+hit.h) - max(trig.y, hit.y)
     *
     * Retorna 0.0 si no hay overlap real (caso defensivo — no debería ocurrir
     * si el caller verificó time=0 correctamente).
     *
     * @param trig      bounds del trigger (ya puede estar solapando)
     * @param hit       bounds del collider impactado
     * @param normalX   normal en X del impacto (-1, 0, +1)
     * @param normalY   normal en Y del impacto (-1, 0, +1)
     * @return profundidad de penetración en el eje de la normal (>= 0)
     */
    private static double computePenetrationDepth(Rectangle trig, Rectangle hit,
                                                   int normalX, int normalY) {
        if (normalX != 0) {
            // Eje X: calcular overlap horizontal
            double overlapLeft  = (trig.x + trig.width) - hit.x;
            double overlapRight = (hit.x + hit.width) - trig.x;
            double pen = Math.min(overlapLeft, overlapRight);
            return Math.max(0.0, pen);
        } else {
            // Eje Y: calcular overlap vertical
            double overlapTop    = (trig.y + trig.height) - hit.y;
            double overlapBottom = (hit.y + hit.height) - trig.y;
            double pen = Math.min(overlapTop, overlapBottom);
            return Math.max(0.0, pen);
        }
    }

    /**
     * Garantiza que la velocidad del trigger apunte FUERA del collider tras el
     * dispatch de contacto.
     *
     * ── Por qué existe este guard ─────────────────────────────────────────
     * Después del dispatch, el behavior puede haber modificado la velocidad
     * incorrectamente (inversión doble por doble-dispatch, fallback incorrecto,
     * velocidad sin cambios por un behavior que no maneja el caso). Si la
     * velocidad resultante apunta hacia el collider — es decir, V·N < 0 —
     * el siguiente frame detectará inmediatamente otro contacto: oscilación.
     *
     * El guard asegura que la componente de velocidad en la dirección de la
     * normal sea >= 0 (el objeto no está penetrando la superficie).
     * Solo actúa en el eje de la normal, sin tocar la componente perpendicular.
     *
     * ── Semántica ─────────────────────────────────────────────────────────
     * V·N > 0 significa que el objeto se aleja del collider → correcto.
     * V·N = 0 significa que el objeto se mueve paralelo → neutro, aceptable.
     * V·N < 0 significa que el objeto penetra → forzar a 0 en ese eje.
     *
     * Forzar a 0 es más conservador que invertir la velocidad (que podría
     * producir rebotes no deseados). El behavior ya aplicó la reflexión que
     * consideró correcta; el guard solo interviene si esa reflexión falló.
     *
     * @param physics   física del trigger (se modificará si V·N < 0)
     * @param normalX   normal del contacto en X
     * @param normalY   normal del contacto en Y
     */
    private static void enforcePostContactVelocity(Physics2D physics,
                                                    int normalX, int normalY) {
        double vx = physics.getVelocity().getX();
        double vy = physics.getVelocity().getY();

        // Producto punto V·N
        double dot = vx * normalX + vy * normalY;

        if (dot < 0.0) {
            // La velocidad sigue apuntando hacia el collider — neutralizar esa componente.
            // Solo se toca el eje de la normal (el perpendicular no cambia).
            if (normalX != 0) physics.getVelocity().setX(0.0);
            if (normalY != 0) physics.getVelocity().setY(0.0);
        }
    }
}
