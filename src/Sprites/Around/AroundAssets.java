package Sprites.Around;

import Sprites.Around.BackGrounds.BackGroundAssets;
import Sprites.Around.Blocks.BlocksAssets;

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
