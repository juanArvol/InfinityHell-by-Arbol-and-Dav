package Graficos.Player;

import Graficos.Player.Animations.WalkD;
import Graficos.Player.Animations.WalkIzq;
import Graficos.Player.SingleSprites.PlayerIdle;

public class PlayerAssets {
    public static PlayerIdle idle;
    public static WalkD walkDere;
    public static WalkIzq walkHiz;

    public static void init() {
        idle = new PlayerIdle();
        walkDere = new WalkD();
        walkHiz = new WalkIzq();
    }
}