package Graficos;

import Graficos.Player.PlayerAssets;
import Graficos.Obstacles.ObstaclesAssets;
import Graficos.Around.AroundAssets;
import Graficos.Bullets.BulletAssets;
import Graficos.Enemys.EnemyAssets;

public class Assets {
    public static void init() {
        PlayerAssets.init();
        AroundAssets.init();
        BulletAssets.init();
        EnemyAssets.init();
        ObstaclesAssets.init();
    }
}