package Game.Items.Types.Bullets.Flyweight;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Items.Types.Bullets.BulletAssetResolver;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache central de BulletFlyweights.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * Por cada combinación única de (assetKey, collisionProfile, width, height),
 * existe exactamente UN BulletFlyweight. El cache lo crea la primera vez que
 * se solicita y lo reutiliza en todas las llamadas posteriores.
 *
 * Esto garantiza que N proyectiles del mismo tipo compartan el MISMO objeto
 * BulletFlyweight en memoria, sin duplicar la resolución del asset ni los
 * parámetros de configuración del collider.
 *
 * ── CLAVE DEL CACHE ───────────────────────────────────────────────────────
 *
 * La clave agrupa los cuatro campos que definen un tipo de proyectil a nivel
 * de recursos:
 *
 *   - assetKey          → determina la textura (null = "bullet.bala")
 *   - collisionProfile  → determina con qué capas colisiona (null = PLAYER_BULLET)
 *   - width, height     → dimensiones del collider
 *
 * Dos blueprints con los mismos valores en estos cuatro campos producirán el
 * mismo BulletFlyweight sin importar cuánto difieran en damage, speed, behavior
 * o movement.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 *
 * Usa ConcurrentHashMap con computeIfAbsent — seguro para acceso desde
 * múltiples threads (aunque el GameLoop suele ser single-thread).
 *
 * ── SINGLETON DE APLICACIÓN ───────────────────────────────────────────────
 *
 * INSTANCE es el punto de acceso global. El cache vive durante toda la
 * sesión del juego. clear() permite limpiar el cache entre runs si los
 * assets cambian (poco probable en producción, útil en tests).
 *
 * ── GARANTÍAS DE NO-RETENCIÓN ─────────────────────────────────────────────
 *
 * El Flyweight solo contiene:
 *   - BufferedImage  → gestionada por AssetRegistry, no hay riesgo de leak
 *   - CollisionProfile → enum/value object, sin referencias a entidades
 *   - int width/height → primitivos en record
 *
 * NUNCA almacena: AbstractEntity, Player, Enemy, Bullet, World, BulletBehavior,
 * ProjectileMovement ni ningún objeto con lifecycle de runtime.
 */
public final class BulletFlyweightCache {

    /** Punto de acceso global al cache. */
    public static final BulletFlyweightCache INSTANCE = new BulletFlyweightCache();

    private final Map<FlyweightKey, BulletFlyweight> cache = new ConcurrentHashMap<>();

    private BulletFlyweightCache() {}

    // ── Acceso principal ──────────────────────────────────────────────────

    /**
     * Obtiene el BulletFlyweight correspondiente al Blueprint dado.
     *
     * Si ya existe un Flyweight para la misma combinación (assetKey, profile,
     * width, height), lo retorna directamente. Si no, lo crea y lo almacena.
     *
     * Resolución del asset ocurre solo en la primera creación de cada tipo.
     *
     * @param blueprint Blueprint del proyectil a resolver
     * @return BulletFlyweight inmutable y compartido para este tipo
     */
    public BulletFlyweight get(ProjectileBlueprint blueprint) {
        FlyweightKey key = FlyweightKey.from(blueprint);
        return cache.computeIfAbsent(key, this::create);
    }

    /**
     * Sobrecarga directa para cuando ya se tienen los parámetros individuales.
     * Útil en tests o en sistemas que construyen Bullets sin Blueprint completo.
     */
    public BulletFlyweight get(String assetKey, CollisionProfile profile,
                               int width, int height) {
        FlyweightKey key = new FlyweightKey(assetKey, profile, width, height);
        return cache.computeIfAbsent(key, this::create);
    }

    // ── Estadísticas ──────────────────────────────────────────────────────

    /** @return número de tipos de Flyweight distintos en el cache. */
    public int size() { return cache.size(); }

    /**
     * Limpia el cache.
     *
     * Llamar si los assets cambian (recarga en caliente, tests).
     * En uso normal de producción no es necesario.
     */
    public void clear() { cache.clear(); }

    // ── Construcción interna ──────────────────────────────────────────────

    private BulletFlyweight create(FlyweightKey key) {
        BufferedImage texture = BulletAssetResolver.resolve(key.assetKey());
        return new BulletFlyweight(texture, key.profile(), key.width(), key.height());
    }

    // ── Clave del cache ───────────────────────────────────────────────────

    /**
     * Clave inmutable del cache — define qué hace único a un tipo de Flyweight.
     *
     * Cuatro campos determinan si dos blueprints producen el mismo Flyweight:
     *   assetKey         — sprite a usar (null = default)
     *   profile          — con qué capas colisiona (null = PLAYER_BULLET)
     *   width, height    — dimensiones del collider
     *
     * CollisionProfile puede ser null (el Blueprint puede no especificarlo).
     * assetKey puede ser null (usa el default "bullet.bala").
     * equals/hashCode usan Objects.equals para manejar nulls correctamente.
     */
    private record FlyweightKey(
            String           assetKey,
            CollisionProfile profile,
            int              width,
            int              height
    ) {
        static FlyweightKey from(ProjectileBlueprint blueprint) {
            return new FlyweightKey(
                    blueprint.assetKey(),
                    blueprint.collisionProfile(),
                    blueprint.width(),
                    blueprint.height()
            );
        }

        // record genera equals/hashCode automáticamente incluyendo null-safety
        // para assetKey y profile — no es necesario sobreescribir.
    }
}
