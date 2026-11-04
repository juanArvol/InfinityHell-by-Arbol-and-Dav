package Game.Engine.Physics.KineticPhysics;

import Game.Enemys.Core.Enemy;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Utilidad para mover objetos paso a paso.
 *
 * ── Mini-HRFC 1.5 — Canonical Physics Units ──────────────────────────────
 *
 * CONTRATO DIMENSIONAL:
 *   Los parámetros moveX y moveY representan DESPLAZAMIENTO (displacement)
 *   en unidades espaciales, NO velocidad.
 *
 *   CORRECTO: PhysicsStepper.moveWith(obj, velocity × deltaTime, ...)
 *   INCORRECTO: PhysicsStepper.moveWith(obj, velocity, ...)
 *
 *   Este contrato es aplicado por CollisionsSystem y MovingObjects:
 *     - CollisionsSystem: displacement = velocity × deltaTime × collisionFraction
 *     - MovingObjects.moveByPhysics(): displacement = velocity × deltaTime
 *
 * Separa la lógica de "mover la posición" de la lógica de física,
 * permitiendo que CollisionsSystem aplique sub-steps o swept AABB
 * sin acoplarse a Physics directamente.
 *
 * ── HRFC — Movement Unification ──────────────────────────────────────────
 *
 * ACTUALIZACIÓN: Ahora sincroniza position para TODOS los modelos de almacenamiento:
 *   - Enemy: DOD storage + Transform OOP (sincronización bidireccional)
 *   - Bullet: DOD storage (PrimitiveStorage)
 *   - Otros: Transform OOP
 *
 * Este es el ÚNICO punto de escritura de posición durante movimiento físico.
 */
public class PhysicsStepper {

    public static void moveX(GameObjects obj, double moveX) {
        if (obj instanceof Enemy enemy) {
            enemy.moveDOD(moveX, 0);
            return;
        }
        if (obj instanceof Bullet bullet) {
            int idx = bullet.getSimulationHandle().index();
            var storage = bullet.getEntityStore().getStorage();
            storage.positionsX()[idx] += (float) moveX;
            return;
        }
        var pos = obj.getTransform().getPosition();
        pos.setX(pos.getX() + moveX);
    }

    public static void moveY(GameObjects obj, double moveY) {
        if (obj instanceof Enemy enemy) {
            enemy.moveDOD(0, moveY);
            return;
        }
        if (obj instanceof Bullet bullet) {
            int idx = bullet.getSimulationHandle().index();
            var storage = bullet.getEntityStore().getStorage();
            storage.positionsY()[idx] += (float) moveY;
            return;
        }
        var pos = obj.getTransform().getPosition();
        pos.setY(pos.getY() + moveY);
    }

    /** Movimiento combinado X+Y (compatible con swept AABB diagonal). */
    public static void moveWith(GameObjects obj, double moveX, double moveY) {
        if (obj instanceof Enemy enemy) {
            enemy.moveDOD(moveX, moveY);
            return;
        }
        if (obj instanceof Bullet bullet) {
            int idx = bullet.getSimulationHandle().index();
            var storage = bullet.getEntityStore().getStorage();
            storage.positionsX()[idx] += (float) moveX;
            storage.positionsY()[idx] += (float) moveY;
            return;
        }
        var pos = obj.getTransform().getPosition();
        pos.setX(pos.getX() + moveX);
        pos.setY(pos.getY() + moveY);
    }
}
