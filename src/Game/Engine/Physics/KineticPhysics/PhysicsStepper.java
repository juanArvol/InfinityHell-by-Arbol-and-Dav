package Game.Engine.Physics.KineticPhysics;

import Game.Enemys.Core.Enemy;
import Game.Engine.GameObjects;

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
 * ── Enemy DOD Migration ──────────────────────────────────────────────────
 *
 * ACTUALIZACIÓN: Ahora sincroniza position a DOD storage para enemies.
 * CollisionsSystem mueve el Transform OOP, PhysicsStepper sincroniza a DOD.
 */
public class PhysicsStepper {

    public static void moveX(GameObjects obj, double moveX) {
        if (obj instanceof Enemy enemy) {
            enemy.moveDOD(moveX, 0);
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
        var pos = obj.getTransform().getPosition();
        pos.setY(pos.getY() + moveY);
    }

    /** Movimiento combinado X+Y (compatible con swept AABB diagonal). */
    public static void moveWith(GameObjects obj, double moveX, double moveY) {
        if (obj instanceof Enemy enemy) {
            enemy.moveDOD(moveX, moveY);
            return;
        }
        var pos = obj.getTransform().getPosition();
        pos.setX(pos.getX() + moveX);
        pos.setY(pos.getY() + moveY);
    }
}
