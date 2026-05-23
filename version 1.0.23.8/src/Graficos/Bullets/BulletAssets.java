package Graficos.Bullets;

import Graficos.Bullets.SingleSprites.*;

public class BulletAssets {
    public static Bala bala;
    public static CometaBala cometa;

    public static void init() {
        bala = new Bala();
        cometa = new CometaBala();
    }
}