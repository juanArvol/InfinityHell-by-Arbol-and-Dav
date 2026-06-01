package Game.Enemys.Types.Ground;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyComport;
import Game.Engine.GameMath.Physics.Implementation.EnemyPhysics;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Player.Player;

import java.awt.image.BufferedImage;

/**
 * Base para enemigos terrestres — aplica gravedad y sincroniza enElSuelo.
 *
 * Sin cambios en la física respecto al original.
 * CAMBIO: constructor primario ya no requiere Player (lo recibe Enemy base).
 * Constructor legacy con Player mantenido para EnemyFactory sin modificar.
 */
public abstract class GroundTypeEnemy extends Enemy {

    // Constructor sin Player (preferido)
    public GroundTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            EnemyPhysics physics
    ) {
        super(position, texture, hp, comport, physics);
    }

    // Constructor legacy con Player (retrocompatibilidad con EnemyFactory)
    @Deprecated
    public GroundTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            Player player,
            EnemyPhysics physics
    ) {
        super(position, texture, hp, comport, player, physics);
    }

    @Override
    protected void updateTypePhysics() {
        var pc = getPhysicsComponent();
        if (pc == null) return;

        // Sincronizar enElSuelo desde física antes de aplicar gravedad
        getState().setEnElSuelo(pc.getPhysics().getOnGround());
        pc.getPhysics().applyGravity(getState().isEnElSuelo());
    }
}
