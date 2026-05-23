package Game.Engine.Systems;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Collisions.SweptAABB;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Colisions.CollisionDetector;
import Game.Engine.Colisions.CollisionDispatcher;
import Game.Engine.Colisions.CollisionResult;
import Game.Fisics.PhysicsStepper;

import java.awt.Rectangle;
import java.util.*;

/**
 * Sistema de colisiones. Se ejecuta una vez por frame en WorldObjectsContainer.
 *
 * ── Las 3 fases, claramente separadas ────────────────────────────────────
 *
 *  FASE 1 — MOVIMIENTO CONTINUO (SweptAABB)
 *    Solo para objetos SOLID con física (jugador, enemigos).
 *    Mueve cada objeto hasta el primer obstáculo que encuentre.
 *    Cancela la velocidad en el eje del impacto.
 *    Marca onGround si el impacto fue desde arriba.
 *
 *  FASE 2 — DETECCIÓN (AABB)
 *    Detecta todos los pares que se solapan con spatial hash.
 *    Incluye triggers y pares bullet/enemy que no pasaron por SweptAABB.
 *
 *  FASE 3 — DESPACHO DE EVENTOS
 *    Para cada par detectado, llama onCollisionWith() en ambos objetos.
 *
 * ── Por qué ya no hay gravedad aquí ─────────────────────────────────────
 * La gravedad se aplica en la física de cada objeto (Physics.applyGravity),
 * no aquí. El CollisionsSystem no toca la física — solo resuelve colisiones.
 * Antes se aplicaba gravedad en el loop de SweptAABB, mezclando dos
 * responsabilidades en el mismo lugar.
 *
 * ── Qué objetos pasan por SweptAABB ─────────────────────────────────────
 * Solo objetos SOLID con PhysicsComponent. Los TRIGGER y los estáticos
 * (sin física, como BlockWorld) solo participan como targets.
 */
public class CollisionsSystem {

    public void update(List<GameObjects> objects) {

        // ──────────────────────────────────────────────────────────────────
        // FASE 1: Movimiento continuo (SweptAABB) para objetos con física
        // ──────────────────────────────────────────────────────────────────

        for (GameObjects obj : objects) {

            PhysicsComponent physComp = obj.getComponent(PhysicsComponent.class);
            ColliderComponent colA    = obj.getComponent(ColliderComponent.class);

            // Solo objetos con física y collider sólido
            if (physComp == null || colA == null || colA.isTrigger()) continue;

            var physics = physComp.getPhysics();
            double vx   = physics.getVelocity().getX();
            double vy   = physics.getVelocity().getY();

            // Sin movimiento → nada que resolver
            if (vx == 0.0 && vy == 0.0) continue;

            physics.setOnGround(false);

            Rectangle myBounds = colA.getBounds();

            // Broadphase: caja que cubre el movimiento completo del frame
            Rectangle broadphase = new Rectangle(
                    (int)(vx < 0 ? myBounds.x + vx : myBounds.x),
                    (int)(vy < 0 ? myBounds.y + vy : myBounds.y),
                    (int)(myBounds.width  + Math.abs(vx)),
                    (int)(myBounds.height + Math.abs(vy))
            );

            // Buscar el obstáculo más cercano en la trayectoria
            double   nearestTime    = 1.0;
            int      hitNormalX     = 0;
            int      hitNormalY     = 0;
            GameObjects hitTarget   = null;

            for (GameObjects other : objects) {
                if (other == obj) continue;

                ColliderComponent colB = other.getComponent(ColliderComponent.class);
                if (colB == null || colB.isTrigger()) continue;

                // Filtro de capas: ¿pueden estos dos chocar?
                if (!colA.canCollideWith(colB)) continue;

                Rectangle otherBounds = colB.getBounds();

                // Broadphase descarta la mayoría sin SweptAABB
                if (!broadphase.intersects(otherBounds)) continue;

                SweptAABB.Result result = SweptAABB.calculate(myBounds, otherBounds, vx, vy);

                if (result.hasCollision() && result.time < nearestTime) {
                    nearestTime = result.time;
                    hitNormalX  = result.normalX;
                    hitNormalY  = result.normalY;
                    hitTarget   = other;
                }
            }

            // Mover hasta el punto de contacto
            PhysicsStepper.moveWith(obj, vx * nearestTime, vy * nearestTime);

            // Si hubo impacto: cancelar velocidad en el eje golpeado
            if (hitTarget != null) {
                if (hitNormalX != 0) physics.getVelocity().setX(0);
                if (hitNormalY != 0) {
                    physics.getVelocity().setY(0);
                    // Normal Y = -1 significa que el impacto fue desde arriba → suelo
                    if (hitNormalY == -1) physics.setOnGround(true);
                }
                // Notificar la colisión física (bala mata enemigo, jugador recibe daño, etc.)
                CollisionDispatcher.dispatch(obj, hitTarget);
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // FASE 2: Detección AABB de todos los pares restantes
        // (triggers, balas, objetos sin física que igual se tocan)
        // ──────────────────────────────────────────────────────────────────

        List<CollisionResult> pairs = CollisionDetector.detect(objects);

        // ──────────────────────────────────────────────────────────────────
        // FASE 3: Despacho de eventos a los objetos
        // ──────────────────────────────────────────────────────────────────

        for (CollisionResult pair : pairs) {
            CollisionDispatcher.dispatch(pair.a, pair.b);
        }
    }
}
