package Game.Engine.Systems;

import Game.Engine.GameObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Colisions.CollisionManager;
import Game.Engine.Colisions.CollisionsPair;
import Game.Engine.Components.Collisions.SweptAABB;
import Game.Fisics.PhysicsStepper;

import java.awt.Rectangle;
import java.util.*;

public class CollisionsSystem {

    public void update(List<GameObjects> objects) {

        Set<CollisionsPair> sweptPairs = new HashSet<>();

        // ==========================================
        // 1️⃣ MOVIMIENTO CONTINUO (SweptAABB)
        // ==========================================

        for (GameObjects obj : objects) {

            PhysicsComponent physics =
                    obj.getComponent(PhysicsComponent.class);

            ColliderComponent colA =
                    obj.getComponent(ColliderComponent.class);

            if (physics == null || colA == null) continue;

            if (colA.getType() == ColliderComponent.ColliderType.TRIGGER)
                continue;

            var body = physics.getPhysics();

            body.applyGravity(body.getOnGround());

            body.setOnGround(false);

            double velX = body.getVelocity().getX();
            double velY = body.getVelocity().getY();

            Rectangle movingBounds = colA.getBounds();

            Rectangle broadphase = new Rectangle(
                    (int) (velX > 0 ? movingBounds.x : movingBounds.x + velX),
                    (int) (velY > 0 ? movingBounds.y : movingBounds.y + velY),
                    (int) (movingBounds.width + Math.abs(velX)),
                    (int) (movingBounds.height + Math.abs(velY))
            );

            double earliestTime = 1.0;
            int hitNormalX = 0;
            int hitNormalY = 0;

            GameObjects hitObject = null;

            for (GameObjects other : objects) {

                if (obj == other) continue;

                ColliderComponent colB =
                        other.getComponent(ColliderComponent.class);

                if (colB == null) continue;

                if (colB.getType() == ColliderComponent.ColliderType.TRIGGER)
                    continue;

                if ((colA.getMask() & colB.getLayer()) == 0 ||
                        (colB.getMask() & colA.getLayer()) == 0)
                    continue;

                Rectangle targetBounds = colB.getBounds();

                if (!broadphase.intersects(targetBounds))
                    continue;

                SweptAABB.Result result =
                        SweptAABB.calculate(
                                movingBounds,
                                targetBounds,
                                velX,
                                velY
                        );

                if (result.hasCollision() && result.time < earliestTime) {

                    earliestTime = result.time;
                    hitNormalX = result.normalX;
                    hitNormalY = result.normalY;
                    hitObject = other;
                }
            }

            double moveX = velX * earliestTime;
            double moveY = velY * earliestTime;

            PhysicsStepper.moveWith(obj, moveX, moveY);

            if (earliestTime < 1.0 && hitObject != null) {

                sweptPairs.add(new CollisionsPair(obj, hitObject, false));

                double remainingTime = 1.0 - earliestTime;

                double remainingX = velX * remainingTime;
                double remainingY = velY * remainingTime;

                if (hitNormalX != 0) {

                    remainingX = 0;
                    body.getVelocity().setX(0);
                }

                if (hitNormalY != 0) {

                    remainingY = 0;
                    body.getVelocity().setY(0);

                    if (hitNormalY == -1) {
                        body.setOnGround(true);
                    }
                }

                PhysicsStepper.moveWith(obj, remainingX, remainingY);
            }
        }

        // ==========================================
        // 2️⃣ DETECCIÓN FINAL (AABB NORMAL)
        // ==========================================

        List<CollisionsPair> pairs =
                CollisionManager.detect(objects);

        pairs.addAll(sweptPairs);

        Map<GameObjects, Set<GameObjects>> collisionMap =
                new HashMap<>();

        Map<GameObjects, Set<GameObjects>> triggerMap =
                new HashMap<>();

        for (CollisionsPair pair : pairs) {

            if (pair.trigger()) {

                triggerMap
                        .computeIfAbsent(pair.a(), k -> new HashSet<>())
                        .add(pair.b());

                triggerMap
                        .computeIfAbsent(pair.b(), k -> new HashSet<>())
                        .add(pair.a());

            } else {

                collisionMap
                        .computeIfAbsent(pair.a(), k -> new HashSet<>())
                        .add(pair.b());

                collisionMap
                        .computeIfAbsent(pair.b(), k -> new HashSet<>())
                        .add(pair.a());
            }
        }

        // ==========================================
        // 3️⃣ EVENTOS ENTER/STAY/EXIT
        // ==========================================

        for (GameObjects obj : objects) {

            Set<GameObjects> col =
                    collisionMap.getOrDefault(obj, Set.of());

            Set<GameObjects> trg =
                    triggerMap.getOrDefault(obj, Set.of());

            for (GameObjects other : col)
                obj.handleCollision(other, false);

            for (GameObjects other : trg)
                obj.handleCollision(other, true);

            obj.resolveExits(col, false);
            obj.resolveExits(trg, true);
        }
    }
}