package Game.Enemys.Types.HybridFyG;

import Game.Enemys.AI.EnemyComport;
import Game.Enemys.Enemy;
import Game.Enemys.EnemyPhysics;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import java.awt.image.BufferedImage;

/**
 * Base para enemigos híbridos tierra/vuelo.
 *
 * MIGRACIÓN: eliminado el constructor @Deprecated con Player.
 * Consistente con la migración de GroundTypeEnemy y FlyingTypeEnemy.
 *
 * FIX 1-3 del original conservados:
 *   - getPhysicsComponent() en lugar de getPhysics() inexistente.
 *   - Sincronización de enElSuelo hacia EnemyState antes de cada frame de física.
 *   - Null-check en getPhysicsComponent().
 *
 * BUG-15: applyGravity eliminado de updateTypePhysics(). Ahora lo aplica
 * CollisionsSystem en FASE 0.5, después de actualizar onGround en FASE 0.
 */
public abstract class HybridFlyGroundTypeEnemy extends Enemy {

    protected boolean flyingMode = false;

    public HybridFlyGroundTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            EnemyPhysics physics
    ) {
        super(position, texture, hp, comport, physics);
    }

    @Override
    protected void updateTypePhysics() {
        if (flyingMode) return; // Volando: steering puro, sin gravedad

        var pc = getPhysicsComponent();
        if (pc == null) return;

        // Sincronizar enElSuelo hacia EnemyState (necesario para MoveCommand.moveX()).
        // applyGravity() ya NO se llama aquí — CollisionsSystem la aplica en
        // FASE 0.5, después de actualizar onGround en FASE 0. Ver BUG-15.
        getState().setEnElSuelo(pc.getPhysics().getOnGround());
    }

    public void setFlyingMode(boolean flying) {
        this.flyingMode = flying;
        getState().setFlying(flying);
    }

    public boolean isFlyingMode() { return flyingMode; }
}
