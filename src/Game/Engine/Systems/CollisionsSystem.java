package Game.Engine.Systems;

import Game.Engine.Colisions.CollisionDetector;
import Game.Engine.Colisions.CollisionDispatcher;
import Game.Engine.Colisions.CollisionResult;
import Game.Engine.Colisions.SweptAABB;
import Game.Engine.Component;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Events.CollisionListenerEvent;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Engine.GameObjects;
import java.awt.Rectangle;
import java.util.*;

/**
 * Sistema de colisiones. Cinco fases por frame.
 *
 * FASE 0 — Revalidación de contacto vertical
 *   Detecta si hay suelo debajo (2px margin). Actualiza onGround y surface.
 *
 * FASE 0.5 — Aplicar gravedad + fuerzas acumuladas
 *   Aplica gravedad a objetos con física propia. Integra fuerzas continuas
 *   registradas por sistemas de zona (Physics2D.accumulate()) mediante
 *   flushAccumulatedForces(). Esto permite viento, campos magnéticos y
 *   cualquier fuerza continua sin modificar Physics ni sus subclases.
 *
 * FASE 1 — Movimiento continuo (SweptAABB) eje por eje
 *   Eje X primero, luego Y.
 *
 * FASE 2 + 3 — Detección AABB + Despacho de eventos
 *
 * FASE 4 — CollisionListener enter / stay / exit  (HRFC-014 — GAP-1)
 *   Activa. Compara los contactos del frame actual con los del anterior
 *   y llama onCollisionEnter / onCollisionStay / onCollisionExit (y sus
 *   variantes onTrigger*) en los componentes del objeto que implementan
 *   {@link CollisionListenerEvent}.
 *
 *   Esto habilita:
 *     - Zonas de daño continuo (lava, gas, campo de fuerza).
 *     - Triggers de área que activan/desactivan efectos al entrar/salir.
 *     - Detectores de suelo/pared refinados.
 *     - Cualquier lógica que dependa del ciclo de vida de una colisión,
 *       no solo del instante de impacto.
 *
 *   Uso desde un Component:
 *     public class DamageZone extends Component implements CollisionListenerEvent {
 *         {@literal @}Override
 *         public void onTriggerStay(GameObjects other) {
 *             if (other instanceof AbstractEntity e) e.damage(1);
 *         }
 *     }
 */
public class CollisionsSystem {

    private static final int GROUND_CHECK_MARGIN = 2;

    /**
     * Contactos activos del frame anterior, por objeto.
     * Clave: identidad del objeto; Valor: conjunto de identidades de sus contactos.
     * Usado en FASE 4 para calcular enter/stay/exit.
     */
    private final IdentityHashMap<GameObjects, Set<GameObjects>> previousContacts =
            new IdentityHashMap<>();

    public void update(List<GameObjects> objects) {

        // ── FASE 0: Revalidar contacto vertical ──────────────────────────

        for (GameObjects obj : objects) {

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;

            Physics2D physics = physComp.getPhysics();
            Rectangle bounds  = colA.getBounds();

            Rectangle groundCheck = new Rectangle(
                    bounds.x + 1,
                    bounds.y + bounds.height,
                    Math.max(1, bounds.width - 2),
                    GROUND_CHECK_MARGIN
            );

            boolean         foundGround  = false;
            SurfaceMaterial foundSurface = null;

            for (GameObjects other : objects) {
                if (other == obj) continue;
                ColliderComponent colB = other.getComponent(ColliderComponent.class);
                if (colB == null || colB.isTrigger()) continue;
                if (!colA.canCollideWith(colB)) continue;

                if (groundCheck.intersects(colB.getBounds())) {
                    foundGround  = true;
                    foundSurface = (other instanceof SurfaceMaterial sm)
                                   ? sm : SurfaceMaterial.DEFAULT;
                    break;
                }
            }

            if (foundGround) {
                physics.setOnGround(true);
                physics.setCurrentSurface(foundSurface);
            } else {
                physics.setOnGround(false);
                physics.clearSurface();
            }
        }

        // ── FASE 0.5: Aplicar gravedad y fuerzas acumuladas ──────────────
        // onGround ya es el valor correcto para este frame (FASE 0 lo actualizó).
        // Solo aplicar a objetos que NO gestionan su propia gravedad.
        for (GameObjects obj : objects) {
            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            if (physComp == null) continue;
            Physics2D physics = physComp.getPhysics();
            if (physics.isGravityManagedExternally()) continue;
            physics.applyGravity(physics.getOnGround());
            // Integrar fuerzas continuas declaradas por sistemas de zona
            // (viento, campos magnéticos, gravedad personalizada).
            physics.flushAccumulatedForces();
        }

        // ── FASE 1: SweptAABB eje por eje ────────────────────────────────

        for (GameObjects obj : objects) {

            Physics2DComponent physComp = obj.getComponent(Physics2DComponent.class);
            ColliderComponent  colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;

            Physics2D physics = physComp.getPhysics();
            double    vx      = physics.getVelocity().getX();
            double    vy      = physics.getVelocity().getY();

            if (vx == 0.0 && vy == 0.0) continue;

            // ── Eje X ─────────────────────────────────────────────────────
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

                    Rectangle ob = colB.getBounds();
                    if (ob.y >= bounds.y + bounds.height) continue;
                    if (ob.y + ob.height <= bounds.y) continue;
                    if (!broadX.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, vx, 0.0);
                    if (r.hasCollision() && r.time < nearestTimeX) {
                        nearestTimeX = r.time;
                        hitNormalX   = r.normalX;
                        hitX         = other;
                    }
                }

                PhysicsStepper.moveWith(obj, vx * nearestTimeX, 0);

                if (hitX != null && hitNormalX != 0) {
                    physics.getVelocity().setX(0);
                    CollisionDispatcher.dispatch(obj, hitX);
                }
            }

            // ── Eje Y ─────────────────────────────────────────────────────
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
                    ColliderComponent colB = other.getComponent(ColliderComponent.class);
                    if (colB == null || colB.isTrigger()) continue;
                    if (!colA.canCollideWith(colB)) continue;

                    Rectangle ob = colB.getBounds();
                    if (!broadY.intersects(ob)) continue;

                    SweptAABB.Result r = SweptAABB.calculate(bounds, ob, 0.0, vy);
                    if (r.hasCollision() && r.time < nearestTimeY) {
                        nearestTimeY = r.time;
                        hitNormalY   = r.normalY;
                        hitY         = other;
                    }
                }

                PhysicsStepper.moveWith(obj, 0, vy * nearestTimeY);

                if (hitY != null && hitNormalY != 0) {
                    physics.getVelocity().setY(0);
                    CollisionDispatcher.dispatch(obj, hitY);
                }
            }
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

        // Construir contactos actuales
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

        // Despachar enter / stay / exit a componentes CollisionListenerEvent
        for (GameObjects obj : objects) {
            List<CollisionListenerEvent> listeners = collectListeners(obj);
            if (listeners.isEmpty()) continue;

            Set<GameObjects> current  = currentContacts.getOrDefault(obj, Set.of());
            Set<GameObjects> previous = previousContacts.getOrDefault(obj, Set.of());
            boolean          isTrigger = isTrigger(obj);

            for (GameObjects contact : current) {
                if (!previous.contains(contact)) {
                    // enter
                    for (CollisionListenerEvent listener : listeners) {
                        if (isTrigger) listener.onTriggerEnter(contact);
                        else           listener.onCollisionEnter(contact);
                    }
                } else {
                    // stay
                    for (CollisionListenerEvent listener : listeners) {
                        if (isTrigger) listener.onTriggerStay(contact);
                        else           listener.onCollisionStay(contact);
                    }
                }
            }

            for (GameObjects contact : previous) {
                if (!current.contains(contact)) {
                    // exit
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

    // ── Helpers privados ──────────────────────────────────────────────────

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
