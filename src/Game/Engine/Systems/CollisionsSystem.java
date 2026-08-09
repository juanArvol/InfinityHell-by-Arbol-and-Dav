package Game.Engine.Systems;

import Game.Engine.Colisions.CollisionDetector;
import Game.Engine.Colisions.CollisionDispatcher;
import Game.Engine.Colisions.CollisionResult;
import Game.Engine.Colisions.SweptAABB;
import Game.Engine.Component;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.InteractionSideComponent;
import Game.Engine.Entity.Components.InteractionSideComponent.SideInteraction;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.PushableComponent;
import Game.Engine.Entity.Components.SurfaceComponent;
import Game.Engine.Events.CollisionListenerEvent;
import Game.Engine.GameObjects;
import Game.Engine.Physics.Contact.ContactState;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import java.awt.Rectangle;
import java.util.*;

/**
 * Sistema de colisiones. Pipeline de cinco fases por frame.
 *
 * ── HRFC — Generalización del sistema de Collision ────────────────────────
 *
 * CAMBIOS ARQUITECTÓNICOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   ANTES:
 *     FASE 0  = groundCheck con rect de 2px (PRE-movimiento) → bug del salto
 *     FASE 0.5= applyGravity (usaba onGround del groundCheck previo)
 *     FASE 1  = SweptAABB solo frenaba velocity, sin actualizar contactState
 *     Sin onWall, sin onCeiling, sin ContactState activo
 *     InteractionSideComponent inerte, PushableComponent no integrado
 *
 *   AHORA:
 *     FASE 0  = applyGravity + flushForces (usa onGround del frame anterior)
 *     FASE 0.5= RESET de onGround/onWall/onCeiling/surface a false/AIR
 *               (DESPUÉS de applyGravity para que use el valor correcto)
 *     FASE 1  = SweptAABB eje por eje + acumulación de ContactRecords +
 *               integración de InteractionSideComponent + PushableComponent
 *               Al finalizar: construye ContactState y lo asigna via
 *               Physics2DComponent.setContactState()
 *     FASE 2  = Detección AABB (sin cambios)
 *     FASE 3  = Despacho de eventos (sin cambios)
 *     FASE 4  = CollisionListener enter/stay/exit (sin cambios)
 *
 * FIX DEL BUG DEL SALTO:
 *   La FASE 0 anterior detectaba onGround ANTES del movimiento usando un
 *   rect de 2px. Cuando el Player saltaba, PlayerController ponía
 *   onGround=false, pero FASE 0 del mismo frame lo reponía a true porque
 *   el player aún no se había movido. El siguiente frame leía onGround=true
 *   e impedía volver a saltar.
 *
 *   La solución es eliminar ese groundCheck separado. onGround ahora se
 *   determina exclusivamente por la normal del contacto en SweptAABB:
 *     normalY == -1 → onGround = true  (viene de abajo → superficie inferior del bloque)
 *     normalY == +1 → onCeiling = true (viene de arriba → superficie superior del bloque)
 *     normalX != 0  → onWall = true
 *
 *   El reseteo (FASE 0.5) ocurre DESPUÉS de applyGravity (FASE 0), garantizando
 *   que la gravedad usa el onGround correcto del frame anterior y que el nuevo
 *   onGround solo lo establece el SweptAABB del frame actual.
 *
 * INTEGRACIÓN DE InteractionSideComponent:
 *   Si el objeto B tiene InteractionSideComponent, se consulta
 *   isc.forNormal(normalX, normalY) antes de aplicar la resolución.
 *   SideInteraction.PASSTHROUGH → ignorar completamente la colisión.
 *   Los demás valores determinan qué flag de contacto se activa.
 *
 * INTEGRACIÓN DE PushableComponent:
 *   Si el objeto B tiene PushableComponent habilitado, después de frenar
 *   al objeto A, se transfiere un impulso a B proporcional a la velocidad
 *   de A, la masa de A y la receptividad de B.
 *
 * MÚLTIPLES CONTACTOS:
 *   ContactRecords se acumulan durante FASE 1. Al combinarlos:
 *     onGround  = OR de todos los contactos con normalY == -1
 *     onWall    = OR de todos los contactos con normalX != 0
 *     onCeiling = OR de todos los contactos con normalY == +1
 *   La surface del ContactState proviene del primer contacto inferior
 *   encontrado (nearest time en eje Y).
 *   La normal del ContactState proviene del contacto más significativo:
 *   primero inferior, luego superior, luego lateral.
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
     * Contactos activos del frame anterior, por objeto.
     * Clave: identidad del objeto; Valor: conjunto de identidades de sus contactos.
     * Usado en FASE 4 para calcular enter/stay/exit.
     */
    private final IdentityHashMap<GameObjects, Set<GameObjects>> previousContacts =
            new IdentityHashMap<>();

    public void update(List<GameObjects> objects) {

        // ── FASE 0: Aplicar gravedad y fuerzas acumuladas ────────────────
        //
        // Usa el onGround del frame ANTERIOR (todavía válido aquí).
        // isGravityManagedExternally() permite que subclases como los
        // patrones de boss gestionen su propia gravedad sin interferencia.
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (physics.isGravityManagedExternally()) continue;
            physics.applyGravity(physics.getOnGround());
            physics.flushAccumulatedForces();
        }

        // ── FASE 0.5: Resetear estado de contacto ────────────────────────
        //
        // DESPUÉS de applyGravity para que use el onGround previo correcto.
        // ANTES de SweptAABB para que los nuevos contactos partan de cero.
        // El nuevo onGround/onWall/onCeiling solo lo establece FASE 1.
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;
            Physics2D physics = physComp.getPhysics();
            physics.setOnGround(false);
            physics.setOnWall(false);
            physics.setOnCeiling(false);
            physics.clearSurface();
            physComp.setContactState(ContactState.NONE);
        }

        // ── FASE 1: SweptAABB eje por eje + acumulación de contactos ─────
        //
        // Para cada objeto dinámico, resuelve colisiones continuas en X e Y.
        // Acumula ContactRecords para construir el ContactState al final.

        // Mapa de acumulación: objeto → lista de contactos del frame
        IdentityHashMap<GameObjects, List<ContactRecord>> frameContacts =
                new IdentityHashMap<>();

        for (GameObjects obj : objects) {

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;

            Physics2D physics = physComp.getPhysics();
            double    vx      = physics.getVelocity().getX();
            double    vy      = physics.getVelocity().getY();

            if (vx == 0.0 && vy == 0.0) continue;

            List<ContactRecord> records = frameContacts.computeIfAbsent(
                    obj, k -> new ArrayList<>(4));

            // ── Eje X ──────────────────────────────────────────────────────
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
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;

                    // Comprobar InteractionSideComponent antes del SweptAABB
                    // para descartar PASSTHROUGH sin coste de cálculo.
                    InteractionSideComponent iscB = other.getComponent(InteractionSideComponent.class);

                    Rectangle ob = colB.getBounds();
                    // Filtro de bandas verticales para evitar falsos positivos
                    if (ob.y >= bounds.y + bounds.height) continue;
                    if (ob.y + ob.height <= bounds.y) continue;
                    if (!broadX.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, vx, 0.0);
                    if (!r.hasCollision() || r.time >= nearestTimeX) continue;

                    // Consultar InteractionSideComponent para PASSTHROUGH
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
                    // Resolver la velocidad según la cara impactada
                    resolveVelocityX(physics, hitX, vx);

                    // Determinar el tipo de contacto según ISC
                    InteractionSideComponent iscHit = hitX.getComponent(InteractionSideComponent.class);
                    SideInteraction sideType = (iscHit != null)
                            ? iscHit.forNormal(hitNormalX, 0)
                            : SideInteraction.WALL;

                    SurfaceMaterial surface = resolveSurface(hitX);
                    records.add(new ContactRecord(hitX, hitNormalX, 0, surface));

                    // Actualizar flags inmediatos de Physics2D
                    applyContactFlag(physics, hitNormalX, 0, sideType);

                    // PushableComponent en el objeto impactado
                    applyPushIfPushable(hitX, vx, 0.0, physics.getMass());

                    CollisionDispatcher.dispatch(obj, hitX);
                }
            }

            // ── Eje Y ──────────────────────────────────────────────────────
            if (vy != 0.0) {
                Rectangle bounds = colA.getBounds(); // rebounds post-moveX

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
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;

                    Rectangle ob = colB.getBounds();
                    if (!broadY.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, 0.0, vy);
                    if (!r.hasCollision() || r.time >= nearestTimeY) continue;

                    // Consultar InteractionSideComponent para PASSTHROUGH
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
                    // Resolver la velocidad según la cara impactada
                    resolveVelocityY(physics, hitY, vy);

                    // Determinar tipo de contacto según ISC
                    InteractionSideComponent iscHit = hitY.getComponent(InteractionSideComponent.class);
                    SideInteraction sideType = (iscHit != null)
                            ? iscHit.forNormal(0, hitNormalY)
                            : defaultSideForNormalY(hitNormalY);

                    SurfaceMaterial surface = resolveSurface(hitY);
                    records.add(new ContactRecord(hitY, 0, hitNormalY, surface));

                    // Actualizar flags inmediatos de Physics2D
                    applyContactFlag(physics, 0, hitNormalY, sideType);

                    // Surface: asignar ya si es contacto inferior (aterrizando)
                    if (hitNormalY == -1) {
                        physics.setCurrentSurface(surface);
                    }

                    // PushableComponent en el objeto impactado
                    applyPushIfPushable(hitY, 0.0, vy, physics.getMass());

                    CollisionDispatcher.dispatch(obj, hitY);
                }
            }
        } // fin loop FASE 1

        // ── FASE 1 — finalización: construir ContactState por objeto ─────
        //
        // Para cada objeto que tuvo al menos un contacto en FASE 1,
        // combinar todos sus ContactRecords en un único ContactState.
        // Los flags ya fueron escritos en Physics2D directamente durante
        // el loop anterior; aquí se construye el snapshot inmutable.
        for (Map.Entry<GameObjects, List<ContactRecord>> entry : frameContacts.entrySet()) {
            GameObjects         obj     = entry.getKey();
            List<ContactRecord> records = entry.getValue();

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null || records.isEmpty()) continue;

            Physics2D physics = physComp.getPhysics();

            // Combinar contactos: OR de booleans, primera superficie inferior
            boolean       hasGround  = false;
            boolean       hasWall    = false;
            boolean       hasCeiling = false;
            SurfaceMaterial groundSurface = null;
            int primaryNX = 0, primaryNY = 0;

            for (ContactRecord rec : records) {
                if (rec.normalY == -1) {
                    hasGround = true;
                    if (groundSurface == null) groundSurface = rec.surface;
                    // Normal primaria: preferir contacto inferior
                    primaryNX = 0;
                    primaryNY = -1;
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

        // ── FASE 2: Detección AABB (triggers, balas, pares restantes) ────
        List<CollisionResult> pairs = CollisionDetector.detect(objects);

        // ── FASE 3: Despacho de eventos ───────────────────────────────────
        for (CollisionResult pair : pairs) {
            CollisionDispatcher.dispatch(pair.a, pair.b);
        }

        // ── FASE 4: CollisionListener enter / stay / exit ─────────────────
        //
        // Construye el mapa de contactos actuales a partir de los pares
        // detectados en FASE 2. Luego, para cada objeto que tenga algún
        // componente que implemente CollisionListenerEvent, calcula:
        //
        //   enter = actuales - anteriores  → onCollisionEnter / onTriggerEnter
        //   stay  = actuales ∩ anteriores  → onCollisionStay  / onTriggerStay
        //   exit  = anteriores - actuales  → onCollisionExit  / onTriggerExit

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
            List<CollisionListenerEvent> listeners = collectListeners(obj);
            if (listeners.isEmpty()) continue;

            Set<GameObjects> current  = currentContacts.getOrDefault(obj, Set.of());
            Set<GameObjects> previous = previousContacts.getOrDefault(obj, Set.of());
            boolean          isTrigger = isTrigger(obj);

            for (GameObjects contact : current) {
                if (!previous.contains(contact)) {
                    for (CollisionListenerEvent listener : listeners) {
                        if (isTrigger) listener.onTriggerEnter(contact);
                        else           listener.onCollisionEnter(contact);
                    }
                } else {
                    for (CollisionListenerEvent listener : listeners) {
                        if (isTrigger) listener.onTriggerStay(contact);
                        else           listener.onCollisionStay(contact);
                    }
                }
            }

            for (GameObjects contact : previous) {
                if (!current.contains(contact)) {
                    for (CollisionListenerEvent listener : listeners) {
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
     * Llamar al cambiar de mundo/escena para evitar enter/exit espurios
     * en el primer frame del nuevo mundo.
     */
    public void clearContactHistory() {
        previousContacts.clear();
    }

    // ── Helpers de resolución de velocidad ───────────────────────────────

    /**
     * Frena la velocidad horizontal de obj al impactar con other.
     * Si other tiene PushableComponent y Physics2DComponent, la velocidad
     * restante se transfiere proporcionalmente (conservación de momentum).
     */
    private static void resolveVelocityX(Physics2D physics, GameObjects other, double vx) {
        physics.getVelocity().setX(0);
    }

    /**
     * Frena la velocidad vertical de obj al impactar con other.
     */
    private static void resolveVelocityY(Physics2D physics, GameObjects other, double vy) {
        physics.getVelocity().setY(0);
    }

    // ── Helper: aplicar flag de contacto según la cara impactada ─────────

    /**
     * Actualiza onGround / onWall / onCeiling en Physics2D según la
     * normal del impacto y el tipo de interacción declarado por ISC.
     *
     * Convención de normales (igual que SweptAABB):
     *   normalY == -1 → objeto A venía de abajo → impacta la cara TOP de B
     *                   → onGround para A
     *   normalY == +1 → objeto A venía de arriba → impacta la cara BOTTOM de B
     *                   → onCeiling para A
     *   normalX != 0  → contacto lateral → onWall para A
     *
     * Si InteractionSideComponent declara un tipo explícito, tiene precedencia.
     */
    private static void applyContactFlag(Physics2D physics,
                                          int normalX, int normalY,
                                          SideInteraction sideType) {
        switch (sideType) {
            case SURFACE   -> physics.setOnGround(true);
            case CEILING   -> physics.setOnCeiling(true);
            case WALL,
                 CLIMBABLE -> physics.setOnWall(true);
            case NONE      -> {
                // Sin interacción especial: usar la normal geométrica
                if (normalY == -1)      physics.setOnGround(true);
                else if (normalY == 1)  physics.setOnCeiling(true);
                else if (normalX != 0)  physics.setOnWall(true);
            }
            case PASSTHROUGH -> {
                // No debería llegar aquí — PASSTHROUGH se filtra antes
            }
        }
    }

    /**
     * Devuelve el SideInteraction por defecto para normalY cuando no hay ISC.
     * normalY == -1 → SURFACE (contacto inferior del bloque = suelo)
     * normalY == +1 → CEILING (contacto superior del bloque = techo)
     */
    private static SideInteraction defaultSideForNormalY(int normalY) {
        return normalY == -1 ? SideInteraction.SURFACE : SideInteraction.CEILING;
    }

    // ── Helper: resolución de superficie ─────────────────────────────────

    /**
     * Resuelve las propiedades de superficie de un objeto de colisión.
     * Prioriza SurfaceComponent. Fallback a SurfaceMaterial directo (legacy).
     * Default: SurfaceMaterial.DEFAULT.
     */
    private static SurfaceMaterial resolveSurface(GameObjects obj) {
        SurfaceComponent sc = obj.getComponent(SurfaceComponent.class);
        if (sc != null) return sc;
        if (obj instanceof SurfaceMaterial sm) return sm;
        return SurfaceMaterial.DEFAULT;
    }

    // ── Helper: PushableComponent ─────────────────────────────────────────

    /**
     * Si el objeto tiene PushableComponent habilitado, transfiere un impulso
     * proporcional a la velocidad del objeto impactante.
     *
     * Fórmula: impulso = velocidad × masa_fuente × pushReceptivity
     * (pushReceptivity se aplica internamente en applyPush())
     *
     * @param target     objeto que podría ser empujado
     * @param vx         velocidad horizontal del objeto que empuja
     * @param vy         velocidad vertical del objeto que empuja
     * @param sourceMass masa del objeto que empuja
     */
    private static void applyPushIfPushable(GameObjects target,
                                             double vx, double vy,
                                             double sourceMass) {
        PushableComponent pushable = target.getComponent(PushableComponent.class);
        if (pushable == null || !pushable.isEnabled()) return;

        // El impulso transmitido es la fuerza del impacto (F = m × v)
        double fx = vx * sourceMass;
        double fy = vy * sourceMass;
        pushable.applyPush(fx, fy);
    }

    // ── Helpers privados de FASE 4 ────────────────────────────────────────

    /**
     * Recoge todos los componentes del objeto que implementan CollisionListenerEvent.
     * Retorna lista vacía si ninguno lo implementa (caso más común — sin coste).
     */
    private static List<CollisionListenerEvent> collectListeners(GameObjects obj) {
        List<CollisionListenerEvent> result = null;
        for (Component c : obj.getComponents()) {
            if (c instanceof CollisionListenerEvent cle) {
                if (result == null) result = new ArrayList<>(2);
                result.add(cle);
            }
        }
        return result != null ? result : List.of();
    }

    /** True si el objeto tiene un ColliderComponent de tipo TRIGGER. */
    private static boolean isTrigger(GameObjects obj) {
        ColliderComponent col = obj.getComponent(ColliderComponent.class);
        return col != null && col.isTrigger();
    }
}
