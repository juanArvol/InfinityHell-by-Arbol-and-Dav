package Sprites.Enviroment.Around;

import Sprites.Enviroment.Around.BackGrounds.BackGroundAssets;
import Sprites.Enviroment.Around.Blocks.BlocksAssets;

/**
 * AroundAssets — inicializa los recursos visuales del entorno del mundo.
 */
public final class AroundAssets {

    private AroundAssets() {}

    public static void init() {
        BlocksAssets.init();
        BackGroundAssets.init();
    }
}
