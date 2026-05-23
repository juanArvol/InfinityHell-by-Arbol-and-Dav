package Game.Weapons.WeaponType.Shoot;

import Game.Bullets.BulletType;
import GameMath.Vector2D;

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