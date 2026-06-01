package Game.Items.Types.Weapons.WeaponType.Shoot;

import java.util.List;

import Game.Items.Types.Bullets.Bullet;

public record ShootResult(List<Bullet> bullets, String sound) {}
