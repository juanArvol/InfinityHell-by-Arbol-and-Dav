package Game.Enemys.Types.Ground;

import Game.Enemys.AI.EnemyComport;
import Game.Enemys.Enemy;
import Game.Enemys.EnemyPhysics;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import java.awt.image.BufferedImage;

/**
 * Base para enemigos terrestres — aplica gravedad y sincroniza enElSuelo.
 *
 * MIGRACIÓN: eliminado el constructor legacy con Player.
 * Toda la cadena EnemyNormal → GroundTypeEnemy → Enemy usa el flujo limpio.
 */
public abstract class GroundTypeEnemy extends Enemy {

    public GroundTypeEnemy(
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
        var pc = getPhysicsComponent();
        if (pc == null) return;

        // Sincronizar enElSuelo desde física hacia EnemyState.
        // MoveCommand.moveX() lo lee para calcular aceleración correctamente.
        // applyGravity() ya NO se llama aquí — CollisionsSystem la aplica en
        // FASE 0.5, después de que FASE 0 actualizó onGround. Ver BUG-15.
        getState().setEnElSuelo(pc.getPhysics().getOnGround());
    }
}
