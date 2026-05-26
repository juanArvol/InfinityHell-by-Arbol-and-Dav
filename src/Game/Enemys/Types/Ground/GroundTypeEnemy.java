package Game.Enemys.Types.Ground;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyComport;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

public abstract class GroundTypeEnemy extends Enemy {

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

        // Sincronizar enElSuelo desde la fisica (CollisionsSystem lo actualiza via setOnGround).
        // Sin esta sincronizacion, applyGravity siempre recibe false y el enemigo cae infinitamente
        // incluso cuando esta parado sobre el suelo.
        getState().setEnElSuelo(pc.getPhysics().getOnGround());

        // Aplicar gravedad (modifica velocidad Y). CollisionsSystem luego resuelve el movimiento.
        pc.getPhysics().applyGravity(getState().isEnElSuelo());
    }
}
