package Sprites;

import Sprites.Core.AssetLoader;
import Sprites.Entity.Bullets.BulletAssets;
import Sprites.Entity.Enemys.Boss.sans.sansAssets;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;
import Sprites.Entity.Player.PlayerAssets;
import Sprites.Enviroment.Around.AroundAssets;
import Sprites.Enviroment.Obstacles.ObstaclesAssets;
import Sprites.Source.SoundManager;

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
 *     → SoundManager.init()     → Pre-carga sonidos frecuentes
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
 *   Al cerrar:
 *     Assets.shutdown()         → Libera recursos de audio e imágenes
 *
 * ── GARANTÍA ─────────────────────────────────────────────────────────────
 * Assets.init() se llama UNA SOLA VEZ al inicio, antes de crear cualquier
 * entidad del juego. El AssetLoader cachea cada imagen solo una vez aunque
 * múltiples assets compartan el mismo archivo físico.
 *
 * ── OPTIMIZACIONES IMPLEMENTADAS ─────────────────────────────────────────
 * 1. Audio pooling: Los clips de sonido se reutilizan (evita fugas memoria)
 * 2. Optimización de imágenes: Conversión a TYPE_INT_ARGB (mejor rendimiento)
 * 3. Pre-carga de sonidos: Sonidos frecuentes listos sin lag inicial
 * 4. Shutdown limpio: Liberación correcta de recursos al cerrar
 */
public final class Assets {

    private Assets() {}

    public static void init() {
        // Inicializar sistema de audio optimizado
        SoundManager.init();
        
        // Cargar assets visuales
        PlayerAssets.init();
        EnemyAssets.init();
        BulletAssets.init();
        AroundAssets.init();
        ObstaclesAssets.init();
        sansAssets.init();
    }

    /**
     * Libera todos los recursos (audio e imágenes).
     * Llamar al cerrar el juego para evitar fugas de memoria.
     */
    public static void shutdown() {
        SoundManager.shutdown();
        AssetLoader.shutdown();
        System.out.println("[Assets] Todos los recursos liberados");
    }
}
