package Game.Items.Types.Weapons.WeaponType.Shoot;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletType;

public interface Shoot {
    ShootResult shoot(
        double spawnX,
        double spawnY,
        boolean right,
        Vector2D direction,
        BulletType type,
        double damageFireModeMultiplier,
        double speedFireModeMultiplier
    );
} 