package Game.Enemys.Types.HybridFyG;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyComport;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

/**
 * Enemigo híbrido tierra/vuelo.
 *
 * FIX 1: Se reemplazó getPhysics() (método inexistente en Enemy) por
 *         getPhysicsComponent().getPhysics(), alineándolo con la API
 *         real de MovingObjects / Enemy.
 *
 * FIX 2: En modo terrestre se sincroniza enElSuelo desde la física
 *         (igual que GroundTypeEnemy) para que applyGravity() reciba
 *         el valor correcto y no acumule vy infinitamente en el suelo.
 *
 * FIX 3: Null-check en getPhysicsComponent() para evitar NPE si el
 *         componente no está inicializado aún.
 */
public abstract class HybridFlyGroundTypeEnemy extends Enemy {

    protected boolean flyingMode;

    public HybridFlyGroundTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            Player player,
            EnemyPhysics physics
    ) {
        super(
            position,
            texture,
            hp,
            comport,
            player,
            physics
        );
    }

    @Override
    protected void updateTypePhysics() {
        if (!flyingMode) {
            var pc = getPhysicsComponent();      // FIX 1: era getPhysics() — no existe en Enemy
            if (pc == null) return;

            // FIX 2: sincronizar enElSuelo desde la física antes de applyGravity
            // (igual que GroundTypeEnemy), para evitar acumulación infinita de vy.
            getState().setEnElSuelo(pc.getPhysics().getOnGround());

            pc.getPhysics().applyGravity(getState().isEnElSuelo());
        }
    }

    public void setFlyingMode(boolean f) {
        flyingMode = f;
    }
}
