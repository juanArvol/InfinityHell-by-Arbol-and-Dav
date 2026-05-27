package Game.Enemys.Types.HybridFyG;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyComport;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

/**
 * Base para enemigos híbridos tierra/vuelo.
 *
 * FIX 1-3 del original conservados:
 *   - getPhysicsComponent() en lugar de getPhysics() inexistente.
 *   - Sincronización de enElSuelo antes de applyGravity en modo terrestre.
 *   - Null-check en getPhysicsComponent().
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

    @Deprecated
    public HybridFlyGroundTypeEnemy(
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
        if (flyingMode) return; // Volando: steering puro, sin gravedad

        var pc = getPhysicsComponent();
        if (pc == null) return;

        getState().setEnElSuelo(pc.getPhysics().getOnGround());
        pc.getPhysics().applyGravity(getState().isEnElSuelo());
    }

    public void setFlyingMode(boolean flying) {
        this.flyingMode = flying;
        getState().setFlying(flying);
    }

    public boolean isFlyingMode() { return flyingMode; }
}
