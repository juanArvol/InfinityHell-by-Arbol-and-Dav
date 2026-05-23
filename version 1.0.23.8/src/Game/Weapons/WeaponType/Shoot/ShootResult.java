package Game.Weapons.WeaponType.Shoot;

import java.util.List;
import Game.Bullets.Bullet;

public record ShootResult(List<Bullet> bullets, String sound) {}
