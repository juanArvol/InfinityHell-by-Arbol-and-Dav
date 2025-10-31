package Game.Bullets.BulletType;

import Game.Bullets.BulletType.BulletClass.BulletJump;
import Game.Bullets.BulletType.BulletClass.BulletNormal;

public class BulletComportFactory {
    public static BulletComport getComportamiento(int tipo) {
        return switch (tipo) {
            case 1 -> new BulletNormal();
            case 2 -> new BulletJump();
            //case 3 -> new BulletTierra();
            //case 4 -> new BulletMeteoro();
            //case 5 -> new BulletSummon();
            default -> new BulletNormal(); // fallback
        };
    }
}
