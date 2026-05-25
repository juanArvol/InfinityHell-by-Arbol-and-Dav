package Game.Engine.Systems;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Collisions.SweptAABB;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Colisions.CollisionDetector;
import Game.Engine.Colisions.CollisionDispatcher;
import Game.Engine.Colisions.CollisionResult;
import Game.Fisics.Physics;
import Game.Fisics.PhysicsStepper;
import Game.World.Surface.SurfaceMaterial;

import java.awt.Rectangle;
import java.util.*;

/**
 * Sistema de colisiones. Tres fases por frame.
 *
 * FASE 0 — Revalidación de contacto vertical
 *   Detecta si hay suelo debajo (2px margin). Actualiza onGround y surface.
 *   Sin esto, al caminar sobre el borde de un bloque onGround nunca se resetea.
 *
 * FASE 1 — Movimiento continuo (SweptAABB) eje por eje
 *   Eje X primero, luego Y con bounds ya actualizados.
 *   FIX: el broadphase en X excluye objetos que están SOLO debajo del objeto
 *   (sin solapamiento vertical en la trayectoria horizontal). Esto evita que
 *   el suelo bloquee el movimiento lateral cuando el jugador está parado encima.
 *
 * FASE 2 + 3 — Detección AABB + Despacho de eventos
 */
public class CollisionsSystem {

    private static final int GROUND_CHECK_MARGIN = 2;

    public void update(List<GameObjects> objects) {

        // ── FASE 0: Revalidar contacto vertical ──────────────────────────

        for (GameObjects obj : objects) {

            PhysicsComponent  physComp = obj.getComponent(PhysicsComponent.class);
            ColliderComponent colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;

            Physics   physics = physComp.getPhysics();
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

        // ── FASE 1: SweptAABB eje por eje ────────────────────────────────

        for (GameObjects obj : objects) {

            PhysicsComponent  physComp = obj.getComponent(PhysicsComponent.class);
            ColliderComponent colA     = obj.getComponent(ColliderComponent.class);
            if (physComp == null || colA == null || colA.isTrigger()) continue;

            Physics physics = physComp.getPhysics();
            double  vx      = physics.getVelocity().getX();
            double  vy      = physics.getVelocity().getY();

            if (vx == 0.0 && vy == 0.0) continue;

            // ── Eje X ─────────────────────────────────────────────────────
            if (vx != 0.0) {
                Rectangle bounds = colA.getBounds();

                // Broadphase horizontal: solo la franja lateral en la dirección del movimiento
                Rectangle broadX = new Rectangle(
                        (int)(vx < 0 ? bounds.x + vx : bounds.x + bounds.width),
                        bounds.y + 1,                          // +1: excluye el pixel del suelo
                        (int)(Math.abs(vx) + 1),
                        bounds.height - 2                      // -2: margen superior e inferior
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

                    // FIX CLAVE: ignorar objetos que están COMPLETAMENTE por debajo
                    // del objeto en movimiento. Sin esto, el bloque del suelo bloquea
                    // el movimiento horizontal cuando el jugador está parado encima.
                    if (ob.y >= bounds.y + bounds.height) continue;
                    // Ignorar objetos completamente por encima también
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
                Rectangle bounds = colA.getBounds(); // re-leer: X ya fue actualizado

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
    }
}
