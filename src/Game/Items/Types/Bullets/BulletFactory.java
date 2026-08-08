package Game.Items.Types.Bullets;

import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletStats;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileEvents;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweight;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweightCache;
import Game.Items.Types.Bullets.Movement.CompositeMovement;
import Game.Items.Types.Bullets.Movement.GravityMovement;

/**
 * Factory de proyectiles — única autoridad de construcción de instancias Bullet.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   1. Resolver el BulletFlyweight del cache (recursos compartidos del tipo).
 *   2. Calcular velocidad X/Y desde speed escalar y dirección normalizada.
 *   3. Construir la instancia Bullet con el Flyweight ya resuelto.
 *   4. Emitir OnProjectileSpawn cuando corresponde.
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   × No decide si una instancia debe reutilizarse (eso es ProjectilePool).
 *   × No gestiona disponibilidad ni reciclaje de instancias.
 *   × No resuelve assets directamente (delega a BulletFlyweightCache).
 *   × No conoce Player, Enemy ni ninguna entidad concreta.
 *   × No contiene if/switch por tipo de proyectil.
 *
 * ── DOS RUTAS DE CONSTRUCCIÓN ─────────────────────────────────────────────
 *
 * build()        — ruta pública. Construye y emite OnProjectileSpawn.
 *                  Usar desde ModifiedWeapon y cualquier sistema que construya
 *                  proyectiles sin pool.
 *
 * buildForPool() — ruta del pool (package-private). Construye SIN emitir
 *                  OnProjectileSpawn. El pool controla cuándo y con qué owner
 *                  se emite el evento, tanto para instancias nuevas como para
 *                  instancias reutilizadas. Un único punto de emisión en acquire().
 *
 * ── RELACIÓN FACTORY ↔ POOL ───────────────────────────────────────────────
 *
 *   Pool.acquire()
 *       ├── instancia compatible → resetState()
 *       └── no disponible → BulletFactory.buildForPool() → Bullet nueva
 *                               → pool.assignPool(bullet)
 *                               → pool.emit OnProjectileSpawn(bullet, owner)
 *
 *   La Factory no sabe si la Bullet irá a un pool. El Pool no sabe cómo
 *   construir una Bullet. Responsabilidades inequívocas.
 *
 * ── FLYWEIGHT ─────────────────────────────────────────────────────────────
 *
 *   BulletFlyweightCache.INSTANCE.get(blueprint) devuelve el Flyweight
 *   compartido para este tipo (assetKey + profile + dimensiones).
 *   La resolución del asset ocurre solo la primera vez para cada tipo.
 *   Con N Bullets del mismo tipo activas: 1 BulletFlyweight, no N.
 */
public final class BulletFactory {

    private BulletFactory() {}

    // ── Ruta pública — construcción con evento ────────────────────────────

    /**
     * Construye un Bullet y emite OnProjectileSpawn con owner = null.
     *
     * Ruta estándar para proyectiles que no pasan por un pool.
     *
     * @param blueprint definición completa y resuelta del proyectil
     * @param position  posición de spawn en coordenadas del mundo
     * @param direction dirección normalizada de vuelo
     * @return Bullet listo para añadir al mundo
     */
    public static Bullet build(ProjectileBlueprint blueprint,
                               Vector2D position,
                               Vector2D direction) {
        return build(blueprint, position, direction, null);
    }

    /**
     * Construye un Bullet, emite OnProjectileSpawn propagando el owner.
     *
     * owner es Object — la Factory es ignorante de la jerarquía de entidades.
     * owner puede ser null: proyectil sin dueño rastreable.
     *
     * @param blueprint definición completa y resuelta del proyectil
     * @param position  posición de spawn en coordenadas del mundo
     * @param direction dirección normalizada de vuelo
     * @param owner     el objeto que originó el disparo (puede ser null)
     * @return Bullet listo para añadir al mundo
     */
    public static Bullet build(ProjectileBlueprint blueprint,
                               Vector2D position,
                               Vector2D direction,
                               Object owner) {
        Bullet bullet = construct(blueprint, position, direction);
        emitSpawn(bullet, owner);
        return bullet;
    }

    // ── Ruta del pool — construcción sin evento ───────────────────────────

    /**
     * Construye un Bullet SIN emitir OnProjectileSpawn.
     *
     * Usar exclusivamente desde ProjectilePool.acquire() cuando no hay
     * instancia compatible disponible. El pool emite el evento después de
     * inyectar el ownerPool y el projectileContext, con el owner correcto
     * y en el mismo punto que las reutilizaciones — un único punto de emisión.
     *
     * El nombre "buildForPool" documenta explícitamente que esta ruta omite
     * el evento. No llamar desde código que no sea ProjectilePool.
     *
     * @param blueprint definición completa y resuelta del proyectil
     * @param position  posición de spawn en coordenadas del mundo
     * @param direction dirección normalizada de vuelo
     * @return Bullet nueva, sin evento emitido, sin ownerPool asignado
     */
    public static Bullet buildForPool(ProjectileBlueprint blueprint,
                                      Vector2D position,
                                      Vector2D direction) {
        return construct(blueprint, position, direction);
    }

    // ── Stats para UI (sin instanciar Bullet) ────────────────────────────

    /**
     * Calcula BulletStats desde un ProjectileBlueprint sin crear un Bullet.
     * Usado por CrossHairHUD y cualquier sistema de preview.
     *
     * hasGravity se infiere inspeccionando recursivamente el árbol de movimiento.
     *
     * @param blueprint definición del proyectil
     * @return BulletStats para presentación en UI
     */
    public static BulletStats statsFrom(ProjectileBlueprint blueprint) {
        boolean hasGravity = containsGravity(blueprint.movement());
        return new BulletStats(
                blueprint.speed(),
                blueprint.damage(),
                blueprint.lifeTime(),
                hasGravity
        );
    }

    // ── Construcción interna compartida ──────────────────────────────────

    /**
     * Núcleo de construcción compartido entre build() y buildForPool().
     *
     * Resuelve el Flyweight, calcula la velocidad vectorial y construye
     * la instancia Bullet. No emite eventos — eso es responsabilidad del
     * caller según si usa la ruta pública o la ruta del pool.
     */
    private static Bullet construct(ProjectileBlueprint blueprint,
                                    Vector2D position,
                                    Vector2D direction) {
        BulletFlyweight flyweight = BulletFlyweightCache.INSTANCE.get(blueprint);

        double xSpeed = direction.getX() * blueprint.speed();
        double ySpeed = direction.getY() * blueprint.speed();

        return new Bullet(
                position,
                flyweight,
                blueprint.behavior(),
                blueprint.movement(),
                xSpeed,
                ySpeed,
                blueprint.lifeTime(),
                blueprint.damage()
        );
    }

    // ── Emisión de evento (separada para reutilizar desde el pool) ────────

    /**
     * Emite OnProjectileSpawn si hay suscriptores.
     *
     * Separado de construct() para que ProjectilePool pueda emitir el evento
     * una vez, en el mismo punto, tanto para instancias nuevas como para
     * instancias reutilizadas — garantizando que el owner es siempre el correcto.
     *
     * Llamar desde ProjectilePool.acquire() después de inyectar ownerPool
     * y projectileContext. No llamar en otros contextos.
     */
    public static void emitSpawn(Bullet bullet, Object owner) {
        if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileSpawn.class)) {
            GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileSpawn(bullet, owner));
        }
    }

    // ── Utilidad interna ──────────────────────────────────────────────────

    /**
     * Determina si un ProjectileMovement incluye gravedad real.
     *
     * Inspección recursiva:
     *   - GravityMovement directo → true.
     *   - CompositeMovement → inspecciona cada componente recursivamente.
     *   - Cualquier otro movement → false.
     */
    static boolean containsGravity(ProjectileMovement movement) {
        if (movement instanceof GravityMovement) return true;
        if (movement instanceof CompositeMovement composite) {
            for (ProjectileMovement component : composite.getComponents()) {
                if (containsGravity(component)) return true;
            }
        }
        return false;
    }
}
