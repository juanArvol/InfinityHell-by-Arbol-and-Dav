package Graficos.Enemys;

import Graficos.Enemys.Animations.*;;

public class EnemyAssets {
    public static EnemyNormal Enormal;
    public static EnemyFlying Eflying;

    public static void init() {
        Enormal = new EnemyNormal();
        Eflying = new EnemyFlying();
    }
}