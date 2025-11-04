package Game.Bullets.BulletCharger;

import Game.Bullets.BulletCharger.BulletClass.BulletJump;
import Game.Bullets.BulletCharger.BulletClass.BulletNormal;
import Game.Bullets.BulletCharger.BulletClass.MetheorBullet;
import Game.Bullets.BulletCharger.BulletClass.ThunderBullet;

public class BulletComportFactory {
    public static BulletComport getComportamiento(int tipo) {
        return switch (tipo) {
            case 1 -> new BulletNormal();
            case 2 -> new BulletJump();
            //case 3 -> new BulletTierra();
            case 4 -> new MetheorBullet();
            //case 5 -> new BulletSummon();
            case 6 -> new ThunderBullet();
            default -> new BulletNormal(); // fallback
        };
    }
}
