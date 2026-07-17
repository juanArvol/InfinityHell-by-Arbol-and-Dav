package Sprites;

import Sprites.Entity.Bullets.BulletAssets;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;
import Sprites.Entity.Player.PlayerAssets;
import Sprites.Enviroment.Around.AroundAssets;
import Sprites.Enviroment.Obstacles.ObstaclesAssets;

/**
 * Assets — punto de entrada único del sistema de recursos gráficos.
 *
 * ── CAMBIO ARQUITECTÓNICO ────────────────────────────────────────────────
 * ANTES:  init() llamaba a managers que instanciaban clases wrapper
 *         (WalkD, WalkIzq, PlayerIdle, EnemyNormal, etc.) que cargaban
 *         BufferedImages de forma independiente y sin caché compartida.
 *
 * AHORA:  init() llama a los managers que usan AssetLoader (con caché
 *         ConcurrentHashMap) y registran SpriteHandles en AssetRegistry.
 *         El resto del sistema obtiene assets vía AssetRegistry.get(id).
 *
 * ── FLUJO COMPLETO ────────────────────────────────────────────────────────
 *
 *   Assets.init()
 *     → PlayerAssets.init()     → AssetRegistry.register("player", handle)
 *     → EnemyAssets.init()      → AssetRegistry.register("enemy.normal", ...)
 *                                 AssetRegistry.register("enemy.flying", ...)
 *     → BulletAssets.init()     → AssetRegistry.register("bullet.bala", ...)
 *                                 AssetRegistry.register("bullet.cometa", ...)
 *     → AroundAssets.init()
 *         → BlocksAssets.init() → AssetRegistry.register("world.suelo", ...)
 *         → BackGroundAssets.init()
 *     → ObstaclesAssets.init()  → AssetRegistry.register("obstacle.mondongo", ...)
 *
 *   Gameplay:
 *     AssetRegistry.find("player")        → SpriteHandle
 *     AssetRegistry.find("enemy.normal")  → SpriteHandle
 *
 * ── GARANTÍA ─────────────────────────────────────────────────────────────
 * Assets.init() se llama UNA SOLA VEZ al inicio, antes de crear cualquier
 * entidad del juego. El AssetLoader cachea cada imagen solo una vez aunque
 * múltiples assets compartan el mismo archivo físico.
 */
public final class Assets {

    private Assets() {}

    public static void init() {
        PlayerAssets.init();
        EnemyAssets.init();
        BulletAssets.init();
        AroundAssets.init();
        ObstaclesAssets.init();
    }
}
